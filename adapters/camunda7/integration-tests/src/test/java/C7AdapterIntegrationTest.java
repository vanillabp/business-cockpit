import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import io.vanillabp.cockpit.gui.api.v1.UserTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class C7AdapterIntegrationTest {

	private static final String DOCKER_NETWORK_MONGO = "mongo";
	private static final String DOCKER_NETWORK_SIMULATOR = "simulator";

	static MongoDBContainer mongoDB;
	static GenericContainer<MongoDBContainer> mongoDBSetUp;
	static GenericContainer<?> simulator;
	private static String mongoConnectionString;
	private static String simulatorUrl;

	@BeforeAll
	static void beforeAll() {

		mongoDB = new MongoDBContainer("mongo:4.4")
				.withCommand("/usr/bin/mongod", "--replSet", "rs-business-cockpit", "--bind_ip_all")
				.withNetwork(Network.SHARED)
				.withNetworkAliases(DOCKER_NETWORK_MONGO);
		mongoDB.start();

		String host = mongoDB.getHost();
		Integer port = mongoDB.getMappedPort(27017);
		String name = mongoDB.getContainerName();
		mongoConnectionString = String.format("mongodb://%s:%d", host, port);


		ImageFromDockerfile mongoDBSetUpImage = new ImageFromDockerfile()
				.withFileFromPath(".", Paths.get("../../../development/mongo"));

		mongoDBSetUp = new GenericContainer<MongoDBContainer>(mongoDBSetUpImage)
				.withCommand("bash", "-c", "chmod a+x /config/*.sh && /config/wait-for-it.sh " + name + ":27017 -- /config/mongo-setup.sh");

		mongoDBSetUp.start();

		Path targetDir = Paths.get("../../../development/simulator/target");
		ImageFromDockerfile simulatorImage = new ImageFromDockerfile()
				.withFileFromPath(".", targetDir)
				.withFileFromString("Dockerfile", """
				 FROM amazoncorretto:17
				 COPY *-runnable.jar /simulator.jar
				 ENTRYPOINT ["java", "-Dspring.profiles.active=local", "--add-opens=java.base/java.lang=ALL-UNNAMED", "-jar", "simulator.jar"]
				""");

		simulator = new GenericContainer<>(simulatorImage)
				.withNetwork(Network.SHARED)
				.withNetworkAliases(DOCKER_NETWORK_SIMULATOR).withExposedPorts(8079).withReuse(true)
				.withEnv("spring.datasource.url", mongoConnectionString)
				.withEnv("spring.datasource.username", "business-cockpit")
				.withEnv("spring.datasource.password", "business-cockpit")
				.waitingFor(Wait.forLogMessage(".*Started BusinessCockpitSimulator.*", 1));

		simulator.start();

		simulatorUrl = "http://%s:%d"
				.formatted(simulator.getHost(), simulator.getMappedPort(8079));
	}

	@AfterAll
	static void afterAll() {
		mongoDB.stop();
		mongoDBSetUp.stop();
		simulator.stop();
	}

	@Test
	public void testMongoDBAvailable() {
		try (MongoClient client = MongoClients.create(mongoConnectionString)) {
			MongoDatabase db = client.getDatabase("test");
			db.createCollection("test");
			assertThat(db.listCollectionNames()).contains("test");
		}
	}

	@Test
	public void sanityTest() {
		assertTrue(true, "This should always pass");
		System.out.println(">>> C7AdapterIntegrationTest.sanityTest() ran – integration tests are wired up correctly! <<<");
	}

	@Test
	public void getUserFromSimulator() {
		final var restClient = RestClient.create();
		final var result = restClient.get().uri(getSimulatorUrl("/official-api/v1/usertask/123"))
				.retrieve().body(UserTask.class);
		assertThat(result.getId()).isEqualTo("123");
	}

	public String getSimulatorUrl(final String path) {
		return path.startsWith("/") ? simulatorUrl + path : simulatorUrl + "/" + path;
	}

}

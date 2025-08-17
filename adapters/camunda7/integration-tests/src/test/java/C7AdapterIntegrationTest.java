import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class C7AdapterIntegrationTest {

	static MongoDBContainer mongoDB;
	static GenericContainer<MongoDBContainer> mongoDBSetUp;
	private static String connectionString;

	@BeforeAll
	static void beforeAll() {

		mongoDB = new MongoDBContainer("mongo:4.4")
				.withCommand("/usr/bin/mongod", "--replSet", "rs-business-cockpit", "--bind_ip_all");;
		mongoDB.start();

		String host = mongoDB.getHost();
		Integer port = mongoDB.getMappedPort(27017);
		String name = mongoDB.getContainerName();
		connectionString = String.format("mongodb://%s:%d", host, port);


		ImageFromDockerfile mongoDBSetUpImage = new ImageFromDockerfile()
				.withFileFromPath(".", Paths.get("../../../development/mongo"));

		mongoDBSetUp = new GenericContainer<MongoDBContainer>(mongoDBSetUpImage)
				.withCommand("bash", "-c", "chmod a+x /config/*.sh && /config/wait-for-it.sh " + name + ":27017 -- /config/mongo-setup.sh");
	}

	@AfterAll
	static void afterAll() {
		mongoDB.stop();
	}

	@Test
	public void testMongoDBAvailable() {
		try (MongoClient client = MongoClients.create(connectionString)) {
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
}

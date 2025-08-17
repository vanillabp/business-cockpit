import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SanityTests {

	@Test
	public void sanityTest() {
		assertTrue(true, "This should always pass");
		System.out.println(">>> SanityTests.sanityTest() ran – integration tests are wired up correctly! <<<");
	}

	@Test
	void dockerInfoShouldBeNonNullAndValid() {
		Info info;
		DockerClient client = DockerClientFactory.lazyClient();
		info = client.infoCmd().exec();
		assertThat(info).isNotNull();
	}

	@Test
	void dockerVersionShouldBeNonEmpty() {
		String version;
		DockerClient client = DockerClientFactory.lazyClient();
		version = client.versionCmd().exec().getVersion();
		assertThat(version).isNotNull().isNotBlank();
		System.out.println("> Docker daemon version: " + version);
	}

}

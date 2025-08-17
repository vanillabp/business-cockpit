import java.io.IOException;

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
		try (DockerClient client = DockerClientFactory.lazyClient()) {
			info = client.infoCmd().exec();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		assertThat(info).isNotNull();
	}

	@Test
	void dockerVersionShouldBeNonEmpty() {
		String version;
		try (DockerClient client = DockerClientFactory.lazyClient()) {
			version = client.versionCmd().exec().getVersion();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		assertThat(version)
				.isNotNull()
				.isNotBlank();
		System.out.println("> Docker daemon version: " + version);
	}

}

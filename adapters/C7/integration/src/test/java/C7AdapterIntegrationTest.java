import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class C7AdapterIntegrationTest {

	@Test
	public void sanityTest() {
		assertTrue(true, "This should always pass");

		System.out.println(">>> C7AdapterIntegrationTest.sanityTest() ran – integration tests are wired up correctly! <<<");
	}
}

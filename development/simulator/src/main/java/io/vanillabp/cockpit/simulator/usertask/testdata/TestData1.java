package io.vanillabp.cockpit.simulator.usertask.testdata;

/**
 * Random test data which the generator puts into the details of a user task, under the key
 * {@code test1}. The workflow generator uses {@code TestData1} too.
 */
public class TestData1 {

    /**
     * Creates empty test data.
     */
    public TestData1() {
    }

    private String testId1;
    
    private Integer testId2;

    /**
     * A test value, a random number from 0 to 4 as a string.
     *
     * @return The value
     */
    public String getTestId1() {
        return testId1;
    }

    /**
     * Sets the test value.
     *
     * @param testId1 The value
     */
    public void setTestId1(String testId1) {
        this.testId1 = testId1;
    }

    /**
     * A test value, a random number below 10000.
     *
     * @return The value
     */
    public Integer getTestId2() {
        return testId2;
    }

    /**
     * Sets the test value.
     *
     * @param testId2 The value
     */
    public void setTestId2(Integer testId2) {
        this.testId2 = testId2;
    }
    
}

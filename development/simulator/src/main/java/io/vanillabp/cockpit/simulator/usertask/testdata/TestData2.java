package io.vanillabp.cockpit.simulator.usertask.testdata;

/**
 * Random test data which the generator puts into the details of every second user task, under
 * the key {@code test2}.
 */
public class TestData2 {

    /**
     * Creates empty test data.
     */
    public TestData2() {
    }

    private Integer testId2;

    private String testId3;    

    /**
     * A test value, a random number from 0 to 4 as a string.
     *
     * @return The value
     */
    public String getTestId3() {
        return testId3;
    }

    /**
     * Sets the test value.
     *
     * @param testId3 The value
     */
    public void setTestId3(String testId3) {
        this.testId3 = testId3;
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

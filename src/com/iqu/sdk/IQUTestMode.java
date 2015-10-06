package com.iqu.sdk;

/**
 * Possible test modes available for the SDK.
 */
public enum IQUTestMode {
    /**
     * Normal operation mode.
     */
    NONE(0),

    /**
     * Don't perform any network IO, simulate that every transaction is successful.
     */
    SIMULATE_SERVER(1),

    /**
     * Simulate that the server is offline.
     */
    SIMULATE_OFFLINE(2);
    
    //
    // PRIVATE VARS
    //

    /**
     * Store value
     */
    private final int m_value;

    //
    // PRIVATE METHODS
    //

    /**
     * Creates a new instance.
     * 
     * @param aValue
     *            integer test mode value
     */
    private IQUTestMode(int aValue) {
        this.m_value = aValue;
    }

    //
    // PUBLIC PROPERTIES
    //

    /**
     * Returns the test mode as integer.
     * 
     * @return test mode as integer
     */
    public int getValue() {
        return this.m_value;
    }

    //
    // PUBLIC METHODS
    //

    /**
     * Find a specific test mode for an integer value.
     * 
     * @param aTestMode
     *            integer value to find test mode for
     * 
     * @return the found test mode or null if none could be found for aSource
     */
    public static IQUTestMode find(int aTestMode) {
        for (IQUTestMode testMode : IQUTestMode.values()) {
            if (testMode.getValue() == aTestMode) {
                return testMode;
            }
        }
        return null;
    }
}

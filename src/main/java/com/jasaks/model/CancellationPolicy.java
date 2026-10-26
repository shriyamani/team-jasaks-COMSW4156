package com.jasaks.model;


/**
 * Defines the refund policy applied when a reservation is cancelled.
 */
public enum CancellationPolicy {

    /**
     * Full refund if cancelled at least 24 hours before the start time;
     * otherwise, a 50% refund.
     */
    FLEXIBLE,

    /**
     * Full refund if cancelled at least 7 days before the start time;
     * otherwise, a 50% refund.
     */
    MODERATE,
    
    /**
     * Full refund if cancelled at least 14 days before the start time;
     * otherwise, a 50% refund.
     */
    STRICT
}

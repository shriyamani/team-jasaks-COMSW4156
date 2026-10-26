package com.jasaks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;

/**
 * Booker: a client who books spaces.
 */
@Entity
@Table(name = "bookers")
public class Booker {
    /** Unique identifier of the booker. */
    @Id
    @Column(nullable = false)
    private String bookerId;

    /** Display name of the booker. */
    private String name;

    /** No-arg constructor required by JPA. */
    protected Booker() { }

    /**
     * Creates a booker.
     *
     * @param bookerId unique identifier of the booker
     * @param name display name of the booker
     */
    public Booker(final String bookerId, final String name) {
        this.bookerId = bookerId;
        this.name = name;
    }

    /**
     * Returns the booker id.
     *
     * @return the booker id
     */
    public String getBookerId() {
        return bookerId;
    }

    /**
     * Returns the booker name.
     *
     * @return the booker name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the booker name.
     *
     * @param name the new name
     */
    public void setName(final String name) {
        this.name = name;
    }
}

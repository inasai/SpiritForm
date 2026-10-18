package com.spiritform;

/**
 * Три гілки прокачки форми Привида.
 */
public enum SpiritBranch {
    HUNGER("hunger"),
    MOVEMENT("movement"),
    DARKNESS("darkness");

    public final String id;

    SpiritBranch(String id) {
        this.id = id;
    }
}

package com.tih.app.model;

/**
 * Ordered seniority levels used to tag interview questions.
 * Ordinal order (L1=0, L2=1, …) defines the cumulative filter: selecting L2 includes L1 and L2.
 */
public enum QuestionLevel {

    L1("Junior", "L1"),
    L2("Middle", "L2"),
    L3("Senior", "L3"),
    L4("Lead", "L4");

    private final String label;
    private final String tag;

    QuestionLevel(String label, String tag) {
        this.label = label;
        this.tag = tag;
    }

    public String getLabel() {
        return label;
    }

    public String getTag() {
        return tag;
    }
}

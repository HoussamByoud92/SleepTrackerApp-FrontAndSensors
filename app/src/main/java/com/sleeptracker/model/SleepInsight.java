package com.sleeptracker.model;

public class SleepInsight {
    private String title;
    private String description;

    public SleepInsight(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
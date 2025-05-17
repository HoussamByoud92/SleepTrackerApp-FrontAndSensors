package com.sleeptracker.model;

import com.google.gson.annotations.SerializedName;

public class SleepSession {
    @SerializedName("user_id")
    private int userId;

    @SerializedName("start")
    private String start;

    @SerializedName("stop")
    private String stop;

    private String events;

    public SleepSession(int userId, String start, String stop, String events) {
        this.userId = userId;
        this.start = start;
        this.stop = stop;
        this.events = events;
    }

    // Getters and setters
    public int getUserId() {
        return userId;
    }

    public String getStart() {
        return start;
    }

    public String getStop() {
        return stop;
    }

    public String getEvents() {
        return events;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public void setStop(String stop) {
        this.stop = stop;
    }

    public void setEvents(String events) {
        this.events = events;
    }
}

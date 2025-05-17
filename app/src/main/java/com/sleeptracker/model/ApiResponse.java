package com.sleeptracker.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("received")
    private SleepSession received;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public SleepSession getReceived() {
        return received;
    }
}

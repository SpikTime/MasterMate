package com.example.mastermate.network;

import com.google.gson.annotations.SerializedName;

public class PresignedUrlResponse {
    @SerializedName("status")
    public String status;
    @SerializedName("url")
    public String presignedUrl;
    @SerializedName("message")
    public String message;

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
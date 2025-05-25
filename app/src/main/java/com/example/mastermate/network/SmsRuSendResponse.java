package com.example.mastermate.network;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class SmsRuSendResponse {

    @SerializedName("status")
    private String status;

    @SerializedName("status_code")
    private int statusCode;

    @SerializedName("sms")
    private Map<String, SmsStatus> smsDetails;

    @SerializedName("status_text")
    private String statusText;

    // Getters
    public String getStatus() { return status; }
    public int getStatusCode() { return statusCode; }
    public Map<String, SmsStatus> getSmsDetails() { return smsDetails; }
    public String getStatusText() { return statusText; }

    public boolean isSuccess() {
        return "OK".equalsIgnoreCase(status) && statusCode == 100;
    }

    public static class SmsStatus {
        @SerializedName("status")
        private String status;

        @SerializedName("status_code")
        private int statusCode;

        @SerializedName("sms_id")
        private String smsId;

        @SerializedName("status_text")
        private String statusText;

        // Getters
        public String getStatus() { return status; }
        public int getStatusCode() { return statusCode; }
        public String getSmsId() { return smsId; }
        public String getStatusText() { return statusText; }

        public boolean isSuccessPerNumber() {
            return "OK".equalsIgnoreCase(status) && statusCode == 100;
        }
    }
}
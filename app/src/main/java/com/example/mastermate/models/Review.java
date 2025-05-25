package com.example.mastermate.models;

import com.google.firebase.database.ServerValue;

public class Review {
    private String userId;
    private String userName;
    private String text;
    private float rating;
    private Object timestamp;
    private String orderId;

    public Review() {
    }
    public Review(String userId, String userName, String text, float rating, String orderId) {
        this.userId = userId;
        this.userName = userName;
        this.text = text;
        this.rating = rating;
        this.orderId = orderId;
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public Object getTimestamp() { return timestamp; }
    public void setTimestamp(Object timestamp) { this.timestamp = timestamp; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public long getTimestampLong() {
        if (timestamp instanceof Long) {
            return (Long) timestamp;
        }
        return 0;
    }
}
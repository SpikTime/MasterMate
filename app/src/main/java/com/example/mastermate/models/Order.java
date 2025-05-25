package com.example.mastermate.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;


public class Order {
    public static final String STATUS_NEW = "new";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_IN_PROGRESS = "in_progress";
    public static final String STATUS_COMPLETED_MASTER = "completed_master";
    public static final String STATUS_CONFIRMED_CLIENT = "confirmed_client";
    public static final String STATUS_REJECTED_MASTER = "rejected_master";
    public static final String STATUS_CANCELLED_CLIENT = "cancelled_client";
    public static final String STATUS_DISPUTED = "disputed";
    private String orderId;
    private String clientId;

    private String masterPhoneNumber;
    private String clientName;
    private String clientPhoneNumber;
    private String masterId;
    private String masterName;
    private String masterSpecialization;
    private String problemDescription;
    private String clientAddress;
    private Double clientLatitude;
    private Double clientLongitude;
    private String status;
    private float masterRatingForClient;
    private String masterCommentForClient;
    private Object masterRatedClientTimestamp;

    private Object creationTimestamp;
    private Object acceptedTimestamp;
    private Object masterCompletionTimestamp;

    private Object clientConfirmationTimestamp;
    private Object rejectionTimestamp;

    private boolean clientReviewLeft;
    private double finalPrice;
    private String masterComment;
    private String clientComment;

    private float clientRatingForMaster;




    public Order() {
        this.creationTimestamp = ServerValue.TIMESTAMP;
        this.status = STATUS_NEW;
        this.clientLatitude = 0.0;
        this.clientLongitude = 0.0;
        this.finalPrice = 0.0;
        this.masterRatingForClient = 0f;
        this.masterCommentForClient = "";
        this.clientReviewLeft = false;
        this.clientRatingForMaster = 0f;
        this.masterRatingForClient = 0f;
        this.masterPhoneNumber = "";
    }

    public Order(String orderId, String clientId, String clientName, String clientPhoneNumber,
                 String masterId, String masterName, String masterSpecialization, String masterPhoneNumber,
                 String problemDescription, String clientAddress,
                 Double clientLatitude, Double clientLongitude) {
        this();
        this.orderId = orderId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientPhoneNumber = clientPhoneNumber;
        this.masterId = masterId;
        this.masterName = masterName;
        this.masterSpecialization = masterSpecialization;
        this.masterPhoneNumber = masterPhoneNumber;
        this.problemDescription = problemDescription;
        this.clientAddress = clientAddress;
        this.clientLatitude = clientLatitude;
        this.clientLongitude = clientLongitude;
    }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public boolean isClientReviewLeft() { // Обычно для boolean геттер начинается с "is"
        return clientReviewLeft;
    }
    public void setClientReviewLeft(boolean clientReviewLeft) {
        this.clientReviewLeft = clientReviewLeft;
    }

    public String getMasterCommentForClient() { return masterCommentForClient; }
    public void setMasterCommentForClient(String masterCommentForClient) { this.masterCommentForClient = masterCommentForClient; }

    public Object getMasterRatedClientTimestamp() { return masterRatedClientTimestamp; }
    public void setMasterRatedClientTimestamp(Object masterRatedClientTimestamp) { this.masterRatedClientTimestamp = masterRatedClientTimestamp; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public Object getClientConfirmationTimestamp() {
        return clientConfirmationTimestamp;
    }
    public void setClientConfirmationTimestamp(Object clientConfirmationTimestamp) {
        this.clientConfirmationTimestamp = clientConfirmationTimestamp;
    }

    @Exclude
    public long getClientConfirmationTimestampLong() {
        if (clientConfirmationTimestamp instanceof Long) {
            return (Long) clientConfirmationTimestamp;
        }
        return 0;
    }
    @Exclude
    public long getCreationTimestampLong() {
        if (creationTimestamp instanceof Long) {
            return (Long) creationTimestamp;
        }
        return 0;
    }


    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getMasterPhoneNumber() {
        return masterPhoneNumber;
    }

    public void setMasterPhoneNumber(String masterPhoneNumber) {
        this.masterPhoneNumber = masterPhoneNumber;
    }
    public String getClientPhoneNumber() { return clientPhoneNumber; }
    public void setClientPhoneNumber(String clientPhoneNumber) { this.clientPhoneNumber = clientPhoneNumber; }

    public String getMasterId() { return masterId; }
    public void setMasterId(String masterId) { this.masterId = masterId; }

    public String getMasterName() { return masterName; }
    public void setMasterName(String masterName) { this.masterName = masterName; }

    public String getMasterSpecialization() { return masterSpecialization; }
    public void setMasterSpecialization(String masterSpecialization) { this.masterSpecialization = masterSpecialization; }

    public String getProblemDescription() { return problemDescription; }
    public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }

    public String getClientAddress() { return clientAddress; }
    public void setClientAddress(String clientAddress) { this.clientAddress = clientAddress; }

    public Double getClientLatitude() { return clientLatitude; }
    public void setClientLatitude(Double clientLatitude) { this.clientLatitude = clientLatitude; }

    public Double getClientLongitude() { return clientLongitude; }
    public void setClientLongitude(Double clientLongitude) { this.clientLongitude = clientLongitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Object getCreationTimestamp() { return creationTimestamp; }
    public void setCreationTimestamp(Object creationTimestamp) { this.creationTimestamp = creationTimestamp; }

    public Object getAcceptedTimestamp() { return acceptedTimestamp; }
    public void setAcceptedTimestamp(Object acceptedTimestamp) { this.acceptedTimestamp = acceptedTimestamp; }

    public Object getMasterCompletionTimestamp() { return masterCompletionTimestamp; }
    public void setMasterCompletionTimestamp(Object masterCompletionTimestamp) { this.masterCompletionTimestamp = masterCompletionTimestamp; }



    public Object getRejectionTimestamp() { return rejectionTimestamp; }
    public void setRejectionTimestamp(Object rejectionTimestamp) { this.rejectionTimestamp = rejectionTimestamp; }

    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }

    public String getMasterComment() { return masterComment; }
    public void setMasterComment(String masterComment) { this.masterComment = masterComment; }

    public String getClientComment() { return clientComment; }
    public void setClientComment(String clientComment) { this.clientComment = clientComment; }

    public float getClientRatingForMaster() { return clientRatingForMaster; }
    public void setClientRatingForMaster(float clientRatingForMaster) { this.clientRatingForMaster = clientRatingForMaster; }

    public float getMasterRatingForClient() { return masterRatingForClient; }
    public void setMasterRatingForClient(float masterRatingForClient) { this.masterRatingForClient = masterRatingForClient; }



}
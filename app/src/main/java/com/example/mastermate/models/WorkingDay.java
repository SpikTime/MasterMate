package com.example.mastermate.models;

public class WorkingDay {
    private boolean isWorking;
    private String startTime;
    private String endTime;

    public WorkingDay() {
        this.isWorking = false;
        this.startTime = "";
        this.endTime = "";
    }

    public WorkingDay(boolean isWorking, String startTime, String endTime) {
        this.isWorking = isWorking;
        this.startTime = startTime != null ? startTime : "";
        this.endTime = endTime != null ? endTime : "";
    }

    public boolean isWorking() {
        return isWorking;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setWorking(boolean working) {
        isWorking = working;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime != null ? startTime : "";
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime != null ? endTime : "";
    }

    @Override
    public String toString() {
        if (isWorking && startTime != null && !startTime.isEmpty() && endTime != null && !endTime.isEmpty()) {
            return startTime + " - " + endTime;
        } else {
            return "Выходной";
        }
    }
}
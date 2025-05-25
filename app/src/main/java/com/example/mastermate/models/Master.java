package com.example.mastermate.models;

import com.google.firebase.database.Exclude;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class Master {
    private String id;
    private Map<String, ServiceItem> services;
    private String name;
    private double clientAverageRating;
    private double clientRatingSum;
    private long clientRatedByMastersCount;
    private String email;
    private String role;
    private List<String> specializations;
    private String specialization;
    private String imageUrl;
    private String description;
    private String phoneNumber;
    private boolean phoneVerified;
    private String address;
    private Double latitude;
    private Double longitude;
    private String city;
    private String experience;
    private Map<String, WorkingDay> workingHours;

    private double rating;
    private long reviewCount;
    private double ratingSum;

    private long completedOrdersCount;
    @Exclude
    public Map<String, Boolean> orders;
    @Exclude
    public Map<String, Object> reviews;

    public Master() {
        this.specializations = new ArrayList<>();
        this.workingHours = new HashMap<>();
        this.phoneVerified = false;
        this.rating = 0.0;
        this.reviewCount = 0L;
        this.ratingSum = 0.0;
        this.completedOrdersCount = 0L;
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.id = "";
        this.name = "";
        this.email = "";
        this.role = "master";
        this.specialization = "";
        this.imageUrl = "";
        this.description = "";
        this.phoneNumber = "";
        this.address = "";
        this.city = "";
        this.experience = "";
        this.clientAverageRating = 0.0;
        this.clientRatingSum = 0.0;
        this.clientRatedByMastersCount = 0L;
        this.services = new HashMap<>();
    }

    public double getClientAverageRating() {
        return clientAverageRating;
    }

    public Map<String, ServiceItem> getServices() {
        return services != null ? new HashMap<>(services) : new HashMap<>();
    }

    public void setServices(Map<String, ServiceItem> services) {
        this.services = services;
    }

    public void setClientAverageRating(double clientAverageRating) {
        this.clientAverageRating = clientAverageRating;
    }

    public double getClientRatingSum() {
        return clientRatingSum;
    }

    public void setClientRatingSum(double clientRatingSum) {
        this.clientRatingSum = clientRatingSum;
    }



    public long getClientRatedByMastersCount() {
        return clientRatedByMastersCount;
    }

    public void setClientRatedByMastersCount(long clientRatedByMastersCount) {
        this.clientRatedByMastersCount = clientRatedByMastersCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }



    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }


    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<String> getSpecializations() {
        return specializations != null ? new ArrayList<>(specializations) : new ArrayList<>();
    }
    public void setSpecializations(List<String> specializations) { this.specializations = specializations; }


    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = (latitude != null) ? latitude : 0.0; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = (longitude != null) ? longitude : 0.0; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public Map<String, WorkingDay> getWorkingHours() {
        return workingHours != null ? new HashMap<>(workingHours) : new HashMap<>();
    }
    public void setWorkingHours(Map<String, WorkingDay> workingHours) { this.workingHours = workingHours; }

    public double getRating() {
        return rating;
    }
    public void setRating(double rating) { this.rating = rating; }

    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }

    public double getRatingSum() { return ratingSum; }
    public void setRatingSum(double ratingSum) { this.ratingSum = ratingSum; }

    public long getCompletedOrdersCount() { return completedOrdersCount; }
    public void setCompletedOrdersCount(long completedOrdersCount) { this.completedOrdersCount = completedOrdersCount; }

    @Exclude
    public String getSpecializationsString() {
        if (specializations != null && !specializations.isEmpty()) {
            return android.text.TextUtils.join(", ", specializations);
        } else if (specialization != null && !specialization.isEmpty()) {
            return specialization;
        }
        return "Не указана";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Master master = (Master) o;
        return Objects.equals(id, master.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
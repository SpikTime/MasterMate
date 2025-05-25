package com.example.mastermate.models;

import com.google.firebase.database.Exclude;

public class ServiceItem {
    private String serviceId;
    private String serviceName;
    private String description;
    private double priceMin;
    private double priceMax;
    private String priceUnit;

    public ServiceItem() {
        this.priceMin = 0.0;
        this.priceMax = 0.0;
        this.serviceName = "";
        this.description = "";
        this.priceUnit = "за услугу";
    }

    public ServiceItem(String serviceName, String description, double priceMin, double priceMax, String priceUnit) {
        this.serviceName = serviceName;
        this.description = description;
        this.priceMin = priceMin;
        this.priceMax = priceMax;
        this.priceUnit = priceUnit;
    }

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPriceMin() { return priceMin; }
    public void setPriceMin(double priceMin) { this.priceMin = priceMin; }

    public double getPriceMax() { return priceMax; }
    public void setPriceMax(double priceMax) { this.priceMax = priceMax; }

    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    @Exclude
    public String getPriceDisplayString() {
        if (priceMin == priceMax && priceMin > 0) {
            return String.format(java.util.Locale.getDefault(), "%.2f %s", priceMin, priceUnit);
        } else if (priceMin > 0 && priceMax > priceMin) {
            return String.format(java.util.Locale.getDefault(), "от %.2f до %.2f %s", priceMin, priceMax, priceUnit);
        } else if (priceMin > 0) {
            return String.format(java.util.Locale.getDefault(), "от %.2f %s", priceMin, priceUnit);
        } else if (priceMax > 0) {
            return String.format(java.util.Locale.getDefault(), "%.2f %s", priceMax, priceUnit);
        }
        return "Цена по договоренности";
    }
}
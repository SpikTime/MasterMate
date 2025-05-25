package com.example.mastermate.models;

import androidx.annotation.ColorRes;

public class StatusInfo {
    private final String statusText;
    @ColorRes
    private final int statusColorRes;
    @ColorRes
    private final int iconTintRes;
    public StatusInfo(String statusText, @ColorRes int statusColorRes, @ColorRes int iconTintRes) {
        this.statusText = statusText;
        this.statusColorRes = statusColorRes;
        this.iconTintRes = iconTintRes;
    }

    public String getStatusText() {
        return statusText;
    }

    @ColorRes
    public int getStatusColorRes() {
        return statusColorRes;
    }

    @ColorRes
    public int getIconTintRes() {
        return iconTintRes;
    }
}
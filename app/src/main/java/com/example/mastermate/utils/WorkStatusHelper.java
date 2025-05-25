package com.example.mastermate.utils;

import android.util.Log;

import com.example.mastermate.R;
import com.example.mastermate.models.StatusInfo;
import com.example.mastermate.models.WorkingDay;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class WorkStatusHelper {

    private static final String TAG = "WorkStatusHelper";
    private static String getCurrentDayKey(int calendarDayOfWeek) {
        switch (calendarDayOfWeek) {
            case Calendar.MONDAY:    return "monday";
            case Calendar.TUESDAY:   return "tuesday";
            case Calendar.WEDNESDAY: return "wednesday";
            case Calendar.THURSDAY:  return "thursday";
            case Calendar.FRIDAY:    return "friday";
            case Calendar.SATURDAY:  return "saturday";
            case Calendar.SUNDAY:    return "sunday";
            default: return null;
        }
    }

    public static StatusInfo getCurrentStatus(Map<String, WorkingDay> workingHours) {
        if (workingHours == null || workingHours.isEmpty()) {
            return null;
        }

        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String currentDayKey = getCurrentDayKey(dayOfWeek);

        if (currentDayKey == null) {
            return null;
        }

        WorkingDay todayWorkingData = workingHours.get(currentDayKey);
        String statusText;
        int statusColorRes;
        int iconTintRes;

        if (todayWorkingData != null && todayWorkingData.isWorking()) {
            String startTimeStr = todayWorkingData.getStartTime();
            String endTimeStr = todayWorkingData.getEndTime();

            if (startTimeStr != null && !startTimeStr.isEmpty() && !startTimeStr.equals("--:--")
                    && endTimeStr != null && !endTimeStr.isEmpty() && !endTimeStr.equals("--:--")) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                try {
                    Date startTime = sdf.parse(startTimeStr);
                    Date endTime = sdf.parse(endTimeStr);
                    Date currentTime = sdf.parse(sdf.format(calendar.getTime()));

                    Calendar calStart = Calendar.getInstance(); calStart.setTime(startTime);
                    calStart.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

                    Calendar calEnd = Calendar.getInstance(); calEnd.setTime(endTime);
                    calEnd.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                    if (calEnd.before(calStart) || calEnd.equals(calStart)) {
                        calEnd.add(Calendar.DATE, 1);
                    }

                    Calendar calNow = Calendar.getInstance();
                    Calendar calNowTimeOnly = Calendar.getInstance();
                    calNowTimeOnly.setTime(currentTime);
                    calNowTimeOnly.set(Calendar.YEAR, 0, 1);

                    Calendar calStartTimeOnly = Calendar.getInstance();
                    calStartTimeOnly.setTime(startTime);
                    calStartTimeOnly.set(Calendar.YEAR, 0, 1);

                    Calendar calEndTimeOnly = Calendar.getInstance();
                    calEndTimeOnly.setTime(endTime);
                    calEndTimeOnly.set(Calendar.YEAR, 0, 1);

                    boolean isWorkingNow;
                    if (calEndTimeOnly.before(calStartTimeOnly) || calEndTimeOnly.equals(calStartTimeOnly)) {
                        isWorkingNow = !calNowTimeOnly.before(calStartTimeOnly) || !calNowTimeOnly.after(calEndTimeOnly);
                    } else {
                        isWorkingNow = !calNowTimeOnly.before(calStartTimeOnly) && calNowTimeOnly.before(calEndTimeOnly);
                    }


                    if (isWorkingNow) {
                        statusText = "Работает";
                        statusColorRes = R.color.colorPrimaryBlue;
                        iconTintRes = R.color.colorPrimaryBlue;
                    } else {
                        statusText = String.format("Сегодня %s - %s", startTimeStr, endTimeStr);
                        statusColorRes = R.color.textColorSecondaryLight;
                        iconTintRes = R.color.textColorSecondaryLight;
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing working time: " + startTimeStr + " - " + endTimeStr, e);
                    statusText = "Сегодня работает";
                    statusColorRes = R.color.textColorSecondaryLight;
                    iconTintRes = R.color.textColorSecondaryLight;
                }
            } else {
                statusText = "Сегодня работает";
                statusColorRes = R.color.textColorSecondaryLight;
                iconTintRes = R.color.textColorSecondaryLight;
            }
        } else {
            statusText = "Сегодня выходной";
            statusColorRes = R.color.colorErrorLight;
            iconTintRes = R.color.colorErrorLight;
        }

        return new StatusInfo(statusText, statusColorRes, iconTintRes);
    }
}
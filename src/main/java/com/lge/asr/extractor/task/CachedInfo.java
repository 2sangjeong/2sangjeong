package com.lge.asr.extractor.task;

import com.lge.asr.common.utils.TextUtils;

public class CachedInfo {

    private String mLogId;
    private String mLoggedDate = "";
    private String mLoggedTime = "";
    private String mDeviceId = "";
    private String mAppName = "";
    private String mLanguage = "";

    public void setLogId(String logId) {
        mLogId = logId;
    }

    public String getLogId() {
        return mLogId;
    }

    public String getLoggedDate() {
        return mLoggedDate;
    }

    public void setLoggedDate(String date) {
        this.mLoggedDate = date;
    }

    public String getLoggedTime() {
        return mLoggedTime;
    }

    public void setLoggedTime(String time) {
        mLoggedTime = time;
    }

    public String getDeviceId() {
        return mDeviceId;
    }

    public void setDeviceId(String deviceId) {
        mDeviceId = deviceId;
    }

    public String getAppName() {
        return TextUtils.isEmpty(mAppName) ? "logs" : mAppName;
    }

    public void setAppName(String appName) {
        this.mAppName = appName;
    }

    public String getLanguage() {
        return TextUtils.isEmpty(mLanguage) ? "none" : mLanguage;
    }

    public void setLanguage(String language) {
        if (TextUtils.isEmpty(language)) {
            language = "none";
        }
        this.mLanguage = language;
    }
}

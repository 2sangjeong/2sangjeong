package com.lge.asr.classifier.parser;

import java.sql.Array;

public class MetaCacheInfo {

    private String mDay;
    private String mHour;
    private String mMinute;
    private String mMonth;
    private String mSecond;
    private String mYear;

    private String mAppName;
    private String mAux;
    private String mDeviceId;
    private String mEngineType;
    private String mLogId;
    private String mPcmData;
    private String mPcmDataLength;
    private String mRequestUrl;
    private String mServerIP;
    private String mServerPort;
    private String mUserAgent;
    private String mVersion;

    private String mAsrResult;
    private String mFinalResult;
    private String mFeedback;
    private String mDevelopStage;
    private String mLocale;
    private String mModel;
    private String mOs;
    private String mPcmSource;
    private String mProduct;

    // JSON Array
    private Array mResultText;
    private Array mSpeechList;

    public String getDay() {
        return mDay;
    }
    public void setDay(String day) {
        this.mDay = day;
    }
    public String getHour() {
        return mHour;
    }
    public void setHour(String hour) {
        this.mHour = hour;
    }
    public String getMinute() {
        return mMinute;
    }
    public void setMinute(String minute) {
        mMinute = minute;
    }
    public String getMonth() {
        return mMonth;
    }
    public void setMonth(String month) {
        mMonth = month;
    }
    public String getSecond() {
        return mSecond;
    }
    public void setSecond(String second) {
        mSecond = second;
    }
    public String getYear() {
        return mYear;
    }
    public void setYear(String year) {
        mYear = year;
    }
    public String getAppName() {
        return mAppName;
    }
    public void setAppName(String appName) {
        mAppName = appName;
    }
    public String getAux() {
        return mAux;
    }
    public void setAux(String aux) {
        mAux = aux;
    }
    public String getDeviceId() {
        return mDeviceId;
    }
    public void setDeviceId(String deviceId) {
        mDeviceId = deviceId;
    }
    public String getEngineType() {
        return mEngineType;
    }
    public void setEngineType(String engineType) {
        mEngineType = engineType;
    }
    public String getLogId() {
        return mLogId;
    }
    public void setLogId(String logId) {
        mLogId = logId;
    }
    public String getPcmData() {
        return mPcmData;
    }
    public void setPcmData(String pcmData) {
        mPcmData = pcmData;
    }
    public String getPcmDataLength() {
        return mPcmDataLength;
    }
    public void setPcmDataLength(String pcmDataLength) {
        mPcmDataLength = pcmDataLength;
    }
    public String getRequestUrl() {
        return mRequestUrl;
    }
    public void setRequestUrl(String requestUrl) {
        mRequestUrl = requestUrl;
    }
    public String getServerIP() {
        return mServerIP;
    }
    public void setServerIP(String serverIP) {
        mServerIP = serverIP;
    }
    public String getServerPort() {
        return mServerPort;
    }
    public void setServerPort(String serverPort) {
        mServerPort = serverPort;
    }
    public String getUserAgent() {
        return mUserAgent;
    }
    public void setUserAgent(String userAgent) {
        mUserAgent = userAgent;
    }
    public String getVersion() {
        return mVersion;
    }
    public void setVersion(String version) {
        mVersion = version;
    }
    public String getAsrResult() {
        return mAsrResult;
    }
    public void setAsrResult(String asrResult) {
        mAsrResult = asrResult;
    }
    public String getFinalResult() {
        return mFinalResult;
    }
    public void setFinalResult(String finalResult) {
        mFinalResult = finalResult;
    }
    public String getFeedback() {
        return mFeedback;
    }
    public void setFeedback(String feedback) {
        mFeedback = feedback;
    }
    public String getDevelopStage() {
        return mDevelopStage;
    }
    public void setDevelopStage(String developStage) {
        mDevelopStage = developStage;
    }
    public String getLocale() {
        return mLocale;
    }
    public void setLocale(String locale) {
        mLocale = locale;
    }
    public String getModel() {
        return mModel;
    }
    public void setModel(String model) {
        mModel = model;
    }
    public String getOs() {
        return mOs;
    }
    public void setOs(String os) {
        mOs = os;
    }
    public String getPcmSource() {
        return mPcmSource;
    }
    public void setPcmSource(String pcmSource) {
        mPcmSource = pcmSource;
    }
    public String getProduct() {
        return mProduct;
    }
    public void setProduct(String product) {
        mProduct = product;
    }
    public Array getResultText() {
        return mResultText;
    }
    public void setResultText(Array resultText) {
        mResultText = resultText;
    }
    public Array getSpeechList() {
        return mSpeechList;
    }
    public void setSpeechList(Array speechList) {
        mSpeechList = speechList;
    }

}

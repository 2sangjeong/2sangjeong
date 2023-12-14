package com.lge.asr.extractor.vo;

public class ResultVo {
    private String logId        = ""; // server : unique log identifier
    private String deviceId     = ""; // client : device id (coded)
    private String appName      = ""; // client : application name
    private String userAgent    = ""; // client : user agent
    private String serverIp     = ""; // server : ip address(or host name)
    private long serverPort     = 0; // server : port number
    private String version      = ""; // client : version
    private String aux          = ""; // client : aux
    private String requestUrl   = ""; // server : requested url
    private String engineType   = ""; // client : engine type
    private long pcmDataLength  = 0; // client : pcm data length
    private String pcmData      = ""; // client : pcm data
    private String tagging      = "";
    private String product      = "";
    private String pcmSource    = "";
    private String country      = "";
    private String resultText   = "";
    private String feadback     = "";
    private String savetime     = "";

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public long getServerPort() {
        return serverPort;
    }

    public void setServerPort(long serverPort) {
        this.serverPort = serverPort;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAux() {
        return aux;
    }

    public void setAux(String aux) {
        this.aux = aux;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public long getPcmDataLength() {
        return pcmDataLength;
    }

    public void setPcmDataLength(long pcmDataLength) {
        this.pcmDataLength = pcmDataLength;
    }

    public String getPcmData() {
        return pcmData;
    }

    public void setPcmData(String pcmData) {
        this.pcmData = pcmData;
    }

    public String getTagging() {
        return tagging;
    }

    public void setTagging(String tagging) {
        this.tagging = tagging;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getPcmSource() {
        return pcmSource;
    }

    public void setPcmSource(String pcmSource) {
        this.pcmSource = pcmSource;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }

    public String getFeadback() {
        return feadback;
    }

    public void setFeadback(String feadback) {
        this.feadback = feadback;
    }

    public String getSavetime() {
        return savetime;
    }

    public void setSavetime(String savetime) {
        this.savetime = savetime;
    }
}

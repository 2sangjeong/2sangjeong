package com.lge.asr.extractor.vo;

public class ResultTextVo {
    private String engineType = ""; // client : engine type
    private String resultText = ""; // client : result text of speech recognition
    private String feedback = ""; // client : feedback text of speech recognition

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public String getResultText() {
        return resultText;
    }

    public void setResultText(String resultText) {
        this.resultText = resultText;
    }
}

/*
 * Mobile Communication Company, LG ELECTRONICS INC., SEOUL, KOREA
 * Copyright(c) 2019 by LG Electronics Inc.
 *
 * All rights reserved. No part of this work may be reproduced, stored in a retrieval system,
 * or transmitted by any means without prior written
 * Permission of LG Electronics Inc.
 */

package com.lge.asr.extractor.task;

import com.lge.asr.classifier.constant.MetaInformationConts;
import com.lge.asr.classifier.parser.JSONParser;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.ListPathUtil;
import com.lge.asr.common.utils.TextUtils;
import com.lge.asr.extractor.dao.ExtractorDao;
import com.lge.asr.extractor.utils.LogCryptor;
import com.lge.asr.extractor.utils.NLPCryptoUtil;
import com.lge.asr.extractor.vo.ResultVo;
import com.lge.asr.extractor.vo.UserDataVo;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.boon.json.ObjectMapper;

import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.lge.asr.common.constants.CommonConsts.OUTPUT_PATH;

/**
 * @author jerome.kim
 * Extractor :: meta 를 json형태로 가공 하여 저장 및 pcm data를 decode 하여 저장 한다.
 *
 */
public class DefaultWorkThread implements Runnable {

    protected Logger mLogger;

    protected ObjectMapper mMapper;
    protected ExtractorDao mExtractorDao;

    protected CachedInfo mCachedInfo;
    protected ResultVo mResultVo;
    protected String mOutputPath;
    protected String mTargetRegion;
    protected String mTargetDate;

    public DefaultWorkThread(Logger logger, ExtractorDao extractDAO, ObjectMapper mapper, String outputPath, String targetRegion,
            String targetDate) {
        super();

        mLogger = logger;

        mExtractorDao = extractDAO;
        mMapper = mapper;

        mOutputPath = CommonUtils.removeSlash(outputPath);
        mTargetRegion = targetRegion;
        mTargetDate = targetDate;

        mCachedInfo = new CachedInfo();
    }

    @Override
    public void run() {
        // run on child class
    }

    public void composeUserData(JSONObject jsonObject) {
        try {
            String saveTime = (String) jsonObject.get("savetime");
            long save = Long.parseLong(saveTime) * 1000;
            
            Timestamp stamp = new Timestamp(save);
            Date date = new Date(stamp.getTime());

            jsonObject.put("_year", (new SimpleDateFormat("yyyy")).format(date));
            jsonObject.put("_month", (new SimpleDateFormat("MM")).format(date));
            jsonObject.put("_day", (new SimpleDateFormat("dd")).format(date));
            jsonObject.put("_hour", (new SimpleDateFormat("HH")).format(date));
            jsonObject.put("_minute", (new SimpleDateFormat("mm")).format(date));
            jsonObject.put("_second", (new SimpleDateFormat("ss")).format(date));

            mCachedInfo.setLoggedDate((new SimpleDateFormat("yyyyMMdd")).format(date));
            mCachedInfo.setLoggedTime((new SimpleDateFormat("HHmmss")).format(date));
            mCachedInfo.setDeviceId(((String)jsonObject.get("deviceId")).substring(0, 10));

            JSONObject userAgentObject = null;
            Object userAgent = jsonObject.get("userAgent");
            if (userAgent instanceof String) {
                String userAgentString = (String) userAgent;
                if (!TextUtils.isEmpty(userAgentString)) {
                    Object userAgentDecoded = mMapper.fromJson(userAgentString);
                    userAgentObject = JSONObject.fromObject(userAgentDecoded);
                }
            } else if (userAgent instanceof JSONObject) {
                userAgentObject = (JSONObject) userAgent;
            }

            if (userAgentObject != null) {
                JSONObject asr_config = userAgentObject.getJSONObject("asr_config");

                String language = "";
                if (asr_config != null && !asr_config.isEmpty()) {
                    language = (String) asr_config.get("language");
                }
                if (TextUtils.isEmpty(language)) {
                    language = (String) userAgentObject.get("locale");
                }
                if ("ko_KR".equals(language) || "ko-kr".equals(language)) {
                    language = "ko-KR";
                }
                mCachedInfo.setLanguage(language);
            }

            // 개인정보로 오해 할 수 있는 항목 초기화 :: 사용하지 않는 쓰레기값이 채워지고 있음.
            putMetaData(jsonObject, "serverIp", "empty", false);
            putMetaData(jsonObject, "serverPort", "empty", false);
        } catch (JSONException e) {
            mLogger.error("error json decode. " + e.getMessage());
        }

        // 2015.06.18. JIYEON, 각 logId 별 contact 정보 가져오기
        UserDataVo userData = mExtractorDao.selectUserData(mCachedInfo.getLogId());
        if (userData != null) {
            putMetaData(jsonObject, "userContacts", userData.getContactData(), true);
            putMetaData(jsonObject, "userAdditionalData", userData.getAdditionalData(), true);
        }
    }

    public void putMetaData(JSONObject object, String key, Object data, boolean decrypt) {
        if (decrypt) {
            if (data instanceof String) {
                object.put(key, getDecryptText((String)data));
            }
        } else {
            if (data != null) {
                object.put(key, data);
            }
        }
    }

    public String getDecryptText(String data) {
        String dec_text = null;
        if (!TextUtils.isEmpty(data)) {
            try {
                byte[] decrypted = LogCryptor.decrypt((String)data);
                if (decrypted != null && decrypted.length > 0) {
                    dec_text = new String(decrypted, CommonConsts.CHARSET_UTF_8);
                }
            } catch (NullPointerException e) {
                mLogger.error("getDecryptText :: " + e.getMessage());
            }
        }
        return dec_text;
    }

    public boolean composeResultText(JSONObject jsonObject) {
        org.json.simple.JSONArray resultTextArr = new org.json.simple.JSONArray();

        String feedback = (String) jsonObject.get("feedback");
        String resultText = (String) jsonObject.get("resultText");

        JSONObject resultTextJsonObject = JSONParser.getInstance(mLogger).getJsonObjectFromString(resultText);

        if (resultTextJsonObject != null) {
            Iterator<String> keySet = resultTextJsonObject.keySet().iterator();

            while (keySet.hasNext()) {
                boolean isSuccessDecryptResultText = false;

                Map<String, Object> map = new HashMap<>();
                String key = keySet.next();
                if (resultTextJsonObject.containsKey(key)) {
                    String encodedTxt = (String) resultTextJsonObject.get(key);
                    if (!TextUtils.isEmpty(encodedTxt)) {
                        String dec_text = getDecryptText(encodedTxt);
                        if (!TextUtils.isEmpty(dec_text)) {
                            isSuccessDecryptResultText = true;
                            map.put("engineType", key);
                            map.put("resultText", dec_text);
                        }
                    }
                }

                if (isSuccessDecryptResultText) {
                    map.put("feedback", feedback);
                }

                if (!map.isEmpty()) {
                    decodeMap(map);
                    resultTextArr.add(JSONParser.getInstance(mLogger).getJsonStringFromMap(map));
                }
            }
        } else {
            Map<String, Object> map = new HashMap<>();
            String dec_text = getDecryptText(resultText);
            mLogger.error("[jerome] retry dec_text :: " + dec_text);
            if (!TextUtils.isEmpty(dec_text)) {
                map.put("engineType", jsonObject.get("engineType"));
                map.put("resultText", dec_text);
                map.put("feedback", feedback);
                decodeMap(map);
                resultTextArr.add(JSONParser.getInstance(mLogger).getJsonStringFromMap(map));
            }
        }

        if (!resultTextArr.isEmpty()) {
            jsonObject.put("resultText", resultTextArr);
        } else {
            jsonObject.put("resultText", "EMPTY");
        }

        return !resultTextArr.isEmpty();
    }

    @SuppressWarnings("unchecked")
    private void decodeMap(Map map) {

        String jsonEncoded = (String)map.get("resultText");
        JSONObject awsJson = null;
        String asrResult = "";
        String asrFinalResult = "";
        try {
            awsJson = JSONObject.fromObject(jsonEncoded);
            asrResult = awsJson.get("ASRResult").toString();
            asrFinalResult = awsJson.get("ASRFinalResult").toString();
        } catch (JSONException e) {
            mLogger.error("JSONException - " + (map.containsKey("engineType") ? "engineType : " + map.get("engineType") : "jsonEncoded : " + jsonEncoded));
        } catch (NullPointerException e) {
            mLogger.debug(String.format("Is awsJson null? %s || asrResult empty ? %s || asrFinalResult empty ? %s", awsJson == null, TextUtils.isEmpty(asrResult), TextUtils.isEmpty(asrFinalResult)));
        }

        if (!TextUtils.isEmpty(asrFinalResult)) {
            if (asrFinalResult.matches("^\\{.+\\}$|^\\[.+\\]$")) {
                Object decoded = mMapper.fromJson(asrFinalResult);
                map.put("ASRFinalResult", decoded);
            } else if (asrFinalResult.matches("^[0-9]+\\$.+$")) {
                String[] decoded = asrFinalResult.substring(asrFinalResult.indexOf('$') + 1, jsonEncoded.length()).split("\\$");
                map.put("ASRFinalResult", decoded);
            } else {
                map.put("ASRFinalResult", asrFinalResult);
            }
        }

        if (!TextUtils.isEmpty(asrResult)) {
            if (asrResult.matches("^\\{.+\\}$|^\\[.+\\]$")) {
                Object decoded = mMapper.fromJson(asrResult);
                map.put("ASRResult", decoded);
            } else if (asrResult.matches("^[0-9]+\\$.+$")) {
                String[] decoded = asrResult.substring(asrResult.indexOf('$') + 1, jsonEncoded.length()).split("\\$");
                map.put("ASRResult", decoded);
            } else {
                map.put("ASRResult", asrResult);
            }

            String finalResult = getFinalResult(awsJson);
            if (finalResult == null) {
                finalResult = asrResult;
            }
            map.put("FinalResult", finalResult);

        } else {
            if (jsonEncoded.matches("^\\{.+\\}$|^\\[.+\\]$")) {
                // 2015.01.26, JIYEON, Regx pattern update json 형태인지 확인 후 디코드 한다.
                Object decoded = mMapper.fromJson(jsonEncoded);
                map.put("ASRResult", decoded);
                map.put("FinalResult", decoded);
            } else if (jsonEncoded.matches("^[0-9]+\\$.+$")) {
                // 개수N$n-best1$...$n-bestN 포맷일 때 배열로 변환
                String[] decoded = jsonEncoded.substring(jsonEncoded.indexOf('$') + 1, jsonEncoded.length()).split("\\$");
                map.put("ASRResult", decoded);
                map.put("FinalResult", decoded);
            } else {
                // plain text 라면 그냥 둔다
                map.put("ASRResult", jsonEncoded);
                map.put("FinalResult", jsonEncoded);
            }
        }
        map.remove("resultText");
    }

    private String getFinalResult(JSONObject awsJson) {
        String finalResult = null;
        if (awsJson != null) {
            try {
                Object finalResultObj = awsJson.get("FinalResult");
                if (finalResultObj != null && !TextUtils.isEmpty(finalResultObj.toString())) {
                    finalResult = finalResultObj.toString();

                    JSONObject finalResultJson = JSONObject.fromObject(finalResult);

                    if (finalResultJson.containsKey("debug")) {
                        mLogger.info("getFinalResult >> remove debug from FinalResult." );
                        finalResultJson.remove("debug");
                        finalResult = finalResultJson.toString();
                    }

                    JSONArray layoutList = finalResultJson.getJSONArray("layoutList");
                    for (int i = 0; i < layoutList.size(); i++) {
                        JSONObject layout = layoutList.getJSONObject(i);
                        if (layout.containsKey("action")) {
                            JSONObject action = layout.getJSONObject("action");
                            if (action.containsKey("paramList")) {
                                JSONArray paramList = action.getJSONArray("paramList");
                                for (int j = 0; j < paramList.size(); j++) {
                                    JSONObject param = paramList.getJSONObject(j);
                                    if (param.containsKey("name") && param.get("name").toString().equalsIgnoreCase("deviceId")) {
                                        mLogger.info("getFinalResult >> remove deviceId from FinalResult." );
                                        paramList.remove(j);
                                        finalResult = finalResultJson.toString();
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (JSONException | NullPointerException | UnsupportedOperationException | IndexOutOfBoundsException e) {
                mLogger.error("[getFinalResult]" + e.getMessage());
                for (StackTraceElement traceElement : e.getStackTrace()) {
                    if (traceElement.getMethodName().equals("getFinalResult")) {
                        mLogger.error("[getFinalResult]" + traceElement.toString());
                    }
                }
            }
        }
        return finalResult;
    }

    public void saveMeta(JSONObject jsonObject, String result) {
        String outputResultPath = getOutputPath(result);
        String outMetaPath = outputResultPath + "/" + getFileName("meta");

        FileWriter fw = null;
        BufferedWriter bw = null;

        try {
            String metadata = jsonObject.toString();
            fw = new FileWriter(outMetaPath, false);
            bw = new BufferedWriter(fw);
            bw.write(metadata);

            mLogger.info("saveMeta : " + outMetaPath);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bw != null)
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            if (fw != null)
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    protected static final String EXTRACT_RESULT_FAIL = "fail";
    protected static final String EXTRACT_RESULT_SUCCESS = "success";
    protected static final String EXTRACT_RESULT_REJECTED_VOICE_TRIGGER = "rejected_voice_trigger";

    public String savePcm(JSONObject jsonObject, boolean isSuccessDecryptResultText, byte[] pcm) {
        String result = EXTRACT_RESULT_FAIL;
        if (pcm == null || pcm.length == 0) {
            mLogger.info("[JEROME] savePcm :: pcm is empty. >> " + pcm == null);
        }
        if (isSuccessDecryptResultText && pcm != null) {
            byte[] s3pcm = pcm;
            int s3pcmlength = s3pcm.length;

            pcm = LogCryptor.decrypt(new String(pcm, CommonConsts.CHARSET_UTF_8));
            byte[] encodepcm = LogCryptor.encode(pcm);
            int pcmlength = encodepcm.length;

            String log_id = getServiceId(jsonObject);
            result = (pcmlength == s3pcmlength) ? EXTRACT_RESULT_SUCCESS : EXTRACT_RESULT_FAIL;
            if (result.equals(EXTRACT_RESULT_FAIL)) {
                if (mCachedInfo.getAppName().compareToIgnoreCase("com.webos.app.voice") == 0) {
                    result = EXTRACT_RESULT_SUCCESS;
                } else {
                    mLogger.debug(String.format("[JEROME] savePcm (1) <%s> :: s3_pcm_length [%d] , encode_pcm [%d]", log_id, s3pcmlength, pcmlength));
                    pcm = NLPCryptoUtil.decode(s3pcm);
                    encodepcm = NLPCryptoUtil.encode(pcm);
                    pcmlength = encodepcm.length;
                    result = (pcmlength == s3pcmlength) ? EXTRACT_RESULT_SUCCESS : EXTRACT_RESULT_FAIL;
                    mLogger.debug(String.format("[JEROME] savePcm (2) <%s> :: s3_pcm_length [%d] , nlp_encode_pcm [%d]", log_id, s3pcmlength, pcmlength));
                }
            }

            if (result.equals(EXTRACT_RESULT_SUCCESS) && isRejectedVoiceTrigger(jsonObject)) {
                result = EXTRACT_RESULT_REJECTED_VOICE_TRIGGER;
            }

            String outPcmPath = getOutputPath(result);
            outPcmPath = outPcmPath + "/" + getFileName("pcm");
            mLogger.info("outPcmPath : " + outPcmPath);

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(outPcmPath);
                fileOutputStream.write(pcm);
                fileOutputStream.flush();
                fileOutputStream.close();
                fileOutputStream = null;

                mLogger.info("savePcm : " + outPcmPath);
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            } finally {
                if (fileOutputStream != null)
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
        return result;
    }

    public String getOutputPath(String result) {
        String outputResultPath = String.format("%s/%s/%s/%s/%s", mOutputPath, result, mCachedInfo.getAppName(),
                mCachedInfo.getLanguage(), mCachedInfo.getLoggedDate());
        boolean created = CommonUtils.makeDirectory(mLogger, outputResultPath);
        if (created) {
            if (outputResultPath.toLowerCase().contains("/success/")) {
                writeList(ListPathUtil.getSplitterPathList(mTargetRegion, mTargetDate), outputResultPath);
            }
            writeFinalList(result, mCachedInfo); // save all created paths
        }

        // create final destination dir.
        CommonUtils.makeDirectory(mLogger, String.format("%s/%s/%s/%s/%s/%s", CommonUtils.removeSlash(OUTPUT_PATH), mTargetRegion, result,
                mCachedInfo.getAppName(),mCachedInfo.getLanguage(), mCachedInfo.getLoggedDate()));

        return outputResultPath;
    }

    public String getFileName(String ext) {
        String fileName = String.format("%s_%s_%s_%s.%s", mCachedInfo.getLoggedDate(), mCachedInfo.getLoggedTime(),
                mCachedInfo.getDeviceId(), mCachedInfo.getLogId(), ext);
        return fileName;
    }

    public void writeList(String filePath, String content) {
        File listFile = new File(filePath);
        BufferedWriter bw = null;
        try {
            if (listFile.createNewFile()) {
                mLogger.info("Create SplitterPathList :: " + filePath);
            }
            bw = new BufferedWriter(new FileWriter(filePath, true));
            bw.write(content);
            bw.newLine();
            mLogger.info("[Splitter] writeList  :: " + content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.flush();
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void writeFinalList(String result, CachedInfo cachedInfo) {
        String finalContent = String.format("%s/%s/%s/%s/%s/%s", CommonUtils.removeSlash(OUTPUT_PATH), mTargetRegion, result, cachedInfo.getAppName(),
                cachedInfo.getLanguage(), cachedInfo.getLoggedDate());

        if (!cachedInfo.getLoggedDate().equals(mTargetDate)) {
            mLogger.info(String.format("[JEROME] writeFinalList. LoggedDate(%s) is not equal with targetDate(%s).", cachedInfo.getLoggedDate(), mTargetDate));
            return;
        }

        String finalListPath = ListPathUtil.getSplitterPathList(mTargetRegion, mTargetDate, false);
        File finalList = new File(finalListPath);

        BufferedReader bufferdReader = null;
        BufferedWriter bufferedWriter = null;
        try {
            if (finalList.exists() && finalList.isFile()) {
                bufferdReader = new BufferedReader(new FileReader(finalList));
                String path = "";
                while ((path = bufferdReader.readLine()) != null) {
                    if (finalContent.equals(path) || finalContent.contains(path)) {
                        return;
                    }
                }
            } else {
                if (finalList.createNewFile()) {
                    mLogger.info("Create FinalCreatedPathList :: " + finalListPath);
                }
            }

            bufferedWriter = new BufferedWriter(new FileWriter(finalListPath, true));
            bufferedWriter.write(finalContent);
            bufferedWriter.newLine();
            mLogger.info("[FinalCreatedPathList] writeList  :: " + finalContent);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferdReader != null) {
                    bufferdReader.close();
                }
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private boolean isRejectedVoiceTrigger(JSONObject jsonObject) {
        if (jsonObject != null) {
            String asrFinalResult = "";
            String finalResult = "";

            JSONObject resultText = getSubJsonObject(jsonObject.get(MetaInformationConts.RESULT_TEXT));
            if (resultText != null) {
                asrFinalResult = getJsonText(resultText.get(MetaInformationConts.ASR_FINAL_RESULT));
                finalResult = getJsonText(resultText.get(MetaInformationConts.FINAL_RESULT));
            }
            if ((!TextUtils.isEmpty(asrFinalResult) && asrFinalResult.contains("REJECTED_VOICETRIGGER"))
                    || (!TextUtils.isEmpty(finalResult) && finalResult.contains("REJECTED_VOICETRIGGER"))) {
                mLogger.debug("isRejectedVoiceTrigger - asrFinalResult : " + asrFinalResult);
                return true;
            }
        }
        return false;
    }

    protected String getServiceId(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        String serviceId = getJsonText(jsonObject.get(MetaInformationConts.LOG_ID));
        return serviceId;
    }

    protected void saveDeviceInfo(DeviceInfoSaver saver) {
        saver.saveMetaInfo(mCachedInfo);
    }

    private JSONObject getSubJsonObject(Object object) {
        if (object instanceof JSONArray) {
            JSONArray array = (JSONArray)object;
            if (array.get(0) instanceof JSONObject) {
                return (JSONObject)array.get(0);
            }
        }
        return null;
    }

    private String getJsonText(Object json) {
        if (json instanceof String) {
            return (String)json;
        }
        return null;
    }

    protected boolean isIgnoredProduct(String appName) {
        return appName.equalsIgnoreCase("mobile") || appName.equalsIgnoreCase("smarttv");
    }

    protected boolean isSkippedSavingPcm(String appName, String locale) {
        return appName.equalsIgnoreCase("com.webos.app.voice") && !(locale.equalsIgnoreCase("ko-KR") || locale.equalsIgnoreCase("en-US"));
    }
}

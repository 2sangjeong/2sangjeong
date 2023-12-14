package com.lge.asr.test;

import com.lge.asr.classifier.parser.JSONParser;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.TextUtils;
import com.lge.asr.extractor.task.CachedInfo;
import com.lge.asr.extractor.utils.LogCryptor;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TestFailedMeta {

    private static final String TEST_META_PATH = "/home/jerome/Workspace/temp/20191001_003904_c6b239c1b8_acc29f4b-d334-4002-8927-e0d3a4ce3d6c.meta";

    private ObjectMapper mMapper = JsonFactory.create();
//    private Logger mLogger = CommonUtils.getLogger(CommonConsts.LOGGER_COMMON, "", "");
//    protected CachedInfo mCachedInfo;

    public void run() {
        File meta = new File(TEST_META_PATH);
        JSONObject jsonObject = null;
        if (meta.exists()) {
            try {
                InputStream is = new FileInputStream(TEST_META_PATH);
                String jsonTxt = IOUtils.toString(is, "UTF-8");
                jsonObject = (JSONObject) JSONSerializer.toJSON(jsonTxt);
                System.out.println(jsonObject.toString());

//                mCachedInfo = mMapper.readValue(jsonTxt, CachedInfo.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        composeUserData(jsonObject);
        boolean isSuccessDecryptResultText = composeResultText(jsonObject);
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

            /*mCachedInfo.setLoggedDate((new SimpleDateFormat("yyyyMMdd")).format(date));
            mCachedInfo.setLoggedTime((new SimpleDateFormat("HHmmss")).format(date));
            mCachedInfo.setDeviceId(((String) jsonObject.get("deviceId")).substring(0, 10));*/

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
            }

            // 개인정보로 오해 할 수 있는 항목 초기화 :: 사용하지 않는 쓰레기값이 채워지고 있음.
            putMetaData(jsonObject, "serverIp", "empty", false);
            putMetaData(jsonObject, "serverPort", "empty", false);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 2015.06.18. JIYEON, 각 logId 별 contact 정보 가져오기
        /*UserDataVo userData = mExtractorDao.selectUserData(mCachedInfo.getLogId());
        if (userData != null) {
            putMetaData(jsonObject, "userContacts", userData.getContactData(), true);
            putMetaData(jsonObject, "userAdditionalData", userData.getAdditionalData(), true);
        }*/
    }

    public boolean composeResultText(JSONObject jsonObject) {
        org.json.simple.JSONArray resultTextArr = new org.json.simple.JSONArray();

        String feedback = (String) jsonObject.get("feedback");
        String resultText = (String) jsonObject.get("resultText");

        JSONObject resultTextJsonObject = JSONParser.getInstance(null).getJsonObjectFromString(resultText);

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
                    resultTextArr.add(JSONParser.getInstance(null).getJsonStringFromMap(map));
                }
            }
        } else {
            Map<String, Object> map = new HashMap<>();
            String dec_text = getDecryptText(resultText);
            if (!TextUtils.isEmpty(dec_text)) {
                map.put("engineType", jsonObject.get("engineType"));
                map.put("resultText", dec_text);
                map.put("feedback", feedback);
                decodeMap(map);
                resultTextArr.add(JSONParser.getInstance(null).getJsonStringFromMap(map));
            }
        }

        if (!resultTextArr.isEmpty()) {
            jsonObject.put("resultText", resultTextArr);
        } else {
            jsonObject.put("resultText", "EMPTY");
        }

        return !resultTextArr.isEmpty();
    }

    public String getDecryptText(String data) {
        String dec_text = null;
        if (!TextUtils.isEmpty(data)) {
            try {
                byte[] decrypted = LogCryptor.decrypt((String) data);
                if (decrypted != null && decrypted.length > 0) {
                    dec_text = new String(decrypted, CommonConsts.CHARSET_UTF_8);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        return dec_text;
    }

    @SuppressWarnings("unchecked")
    private void decodeMap(Map map) {

        String jsonEncoded = (String) map.get("resultText");
        JSONObject awsJson = null;
        String asrResult = "";
        String asrFinalResult = "";
        try {
            awsJson = JSONObject.fromObject(jsonEncoded);
            asrResult = awsJson.get("ASRResult").toString();
            asrFinalResult = awsJson.get("ASRFinalResult").toString();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
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

            Object finalResult = null;
            try {
                finalResult = awsJson.get("FinalResult");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (finalResult != null && !TextUtils.isEmpty(finalResult.toString())) {
                map.put("FinalResult", awsJson.get("FinalResult").toString());
            } else {
                map.put("FinalResult", asrResult);
            }
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

    public void putMetaData(JSONObject object, String key, Object data, boolean decrypt) {
        if (decrypt) {
            if (data instanceof String) {
                object.put(key, getDecryptText((String) data));
            }
        } else {
            if (data != null) {
                object.put(key, data);
            }
        }
    }

    public String getJsonText(Object json) {
        if (json instanceof String) {
            return (String)json;
        } else if (json instanceof JSONObject) {
            return ((JSONObject) json).toString();
        }
        return null;
    }
}

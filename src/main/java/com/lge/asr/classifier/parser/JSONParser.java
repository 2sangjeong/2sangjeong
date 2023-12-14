package com.lge.asr.classifier.parser;

import com.lge.asr.classifier.constant.MetaInformationConts;
import com.lge.asr.common.utils.TextUtils;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class JSONParser extends org.json.simple.parser.JSONParser {

    private static JSONParser sInstance;

    private static Logger mLogger;

    private MetaCacheInfo mCacheInfo;

    public static JSONParser getInstance(Logger logger) {
        mLogger = logger;
        if (sInstance == null) {
            sInstance = new JSONParser();
        }
        return sInstance;
    }

    public JSONObject getJsonObjectFromFilePath(String path) {
        try {
            Object obj = this.parse(new FileReader(path));
            JSONObject jsonObject = (JSONObject)obj;
            return jsonObject;
        } catch (Exception e) {
            mLogger.error(e.getMessage());
            mLogger.error("[Exception] : " + path);
        }
        return null;
    }

    public net.sf.json.JSONObject getJsonObjectFromString(String jsonText) {
        try {
            net.sf.json.JSONObject object = net.sf.json.JSONObject.fromObject(JSONSerializer.toJSON(jsonText));
            return object;
        } catch (Exception e) {
            mLogger.error("getJsonObjectFromString - jsonText :: " + jsonText);
            mLogger.error(e.getLocalizedMessage());
        }
        return null;
    }

    public JSONObject getJsonStringFromMap(Map<String, Object> map) {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            jsonObject.put(key, value);
        }
        return jsonObject;
    }

    public MetaCacheInfo ParsingFromFilePath(String jsonPath) {
        initPcmMetaCacheInfo();
        try {
            Object obj = this.parse(new FileReader(jsonPath));
            JSONObject jsonObject = (JSONObject) obj;

            if (jsonObject != null && !jsonObject.isEmpty()) {
                doParse(jsonObject);
            }

        } catch (IOException | ParseException e) {
            mLogger.error(e.getMessage());
        }
        return mCacheInfo;
    }

    public MetaCacheInfo ParsingFromJsonString(String jsonString) {
        initPcmMetaCacheInfo();
        try {
            Map json = (Map)this.parse(jsonString);
            json.entrySet().iterator();

            doParse(json);

        } catch (ParseException e) {
            mLogger.error(e.getMessage());
        }
        return mCacheInfo;
    }

    private void initPcmMetaCacheInfo() {
        mCacheInfo = new MetaCacheInfo();
    }

    private void doParse(JSONObject json) {
        setAppName(json.get(MetaInformationConts.APP_NAME));
    }

    private void doParse(Map json) {
        setAppName(json.get(MetaInformationConts.APP_NAME));
    }

    private void setAppName(Object obj_appName) {
        if (obj_appName instanceof String) {
            mCacheInfo.setAppName((String)obj_appName);
        }
    }

    public String getFinalResult(String jsonString) {
        JSONObject metaObject = getJsonObjectFromFilePath(jsonString);
        if (metaObject != null) {
            JSONObject resultText = getSubJsonObject(metaObject.get(MetaInformationConts.RESULT_TEXT));
            if (resultText != null) {
                String finalResultText = getJsonText(resultText.get(MetaInformationConts.FINAL_RESULT));
                return finalResultText;
            }
        }
        return null;
    }

    public String getLanguage(JSONObject metaObject) {
        if (metaObject != null) {
            String language = "";
            JSONObject userAgent = getSubJsonObject(metaObject.get(MetaInformationConts.USER_AGENT));
            if (userAgent != null) {
                JSONObject asrConfig = getSubJsonObject(metaObject.get(MetaInformationConts.ASR_CONFIG));
                if (asrConfig != null) {
                    language = getJsonText(userAgent.get(MetaInformationConts.LANGUAGE));
                }
                if (TextUtils.isEmpty(language)) {
                    language = getJsonText(userAgent.get(MetaInformationConts.LOCALE));
                }
                if (language.equals("ko_KR") || language.equals("ko-kr")) {
                    language = "ko-KR";
                }
                return language;
            }
        }
        return null;
    }

    public String getJsonText(Object json) {
        if (json instanceof String) {
            return (String)json;
        }
        return null;
    }

    public JSONObject getSubJsonObject(Object object) {
        if (object instanceof JSONArray) {
            JSONArray array = (JSONArray)object;
            if (array.get(0) instanceof JSONObject) {
                return (JSONObject)array.get(0);
            }
        }
        return null;
    }
}

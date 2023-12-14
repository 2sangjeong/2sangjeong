package com.lge.asr.reporter.parser;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.SlackWebHook;
import com.lge.asr.crnn_classifier.CRNNClassifier;
import com.lge.asr.crnn_classifier.SelectWithPreds;
import com.lge.asr.reporter.utils.ReporterUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

public class CRNNParser extends org.json.simple.parser.JSONParser {

    private static CRNNParser sInstance;
    private HashMap<String, HashMap<String, Integer>> mCRNNResultMap = new HashMap<>();
    private String mResultFileName = "";

    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_STATISTICS_REPORTER);

    public static CRNNParser getInstance() {
        if (sInstance == null) {
            sInstance = new CRNNParser();
        }
        return sInstance;
    }

    public void parsingReport(ArrayList<String> mDateList) {
        for (String region : CommonConsts.REGIONS) {
            for (String day : mDateList) {
                File jsonFile = new File(ReporterUtils.getJsonPath(logger, region, day, CommonConsts.CRNN_CLASSIFIER_RESULT_JSON, CRNNClassifier.CRNN_CLASSIFIER_RESULT_FILE_NAME));
                if (jsonFile.exists()) {
                    ParsingFromFilePath(jsonFile.getAbsolutePath());
                }
            }
        }
        reportResult(mDateList);
    }

    public void ParsingFromFilePath(String jsonPath) {
        try {
            Object obj = this.parse(new FileReader(jsonPath));
            JSONObject jsonObject = (JSONObject) obj;

            if (jsonObject != null && !jsonObject.isEmpty()) {
                doParseByDate(jsonObject);
            }

        } catch (IOException | ParseException e) {
            logger.error(e.getMessage());
        }
    }

    private void doParseByDate(JSONObject object) {
        Iterator<String> keys = object.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            JSONArray value = (JSONArray) object.get(key);
            if (value != null && value.size() > 0) {
                for (int i = 0; i < value.size(); i++) {
                    JSONObject data = (JSONObject) value.get(i);
                    sumValue(key, data);
                }
            }
        }
    }

    private void sumValue(String key, JSONObject object) {
        int totalCnt = 0;
        int normalCnt = 0;
        if (object.containsKey(SelectWithPreds.CRNN_KEY_TOTAL_SPEECH_CNT)) {
            totalCnt = (int) ((long) object.get(SelectWithPreds.CRNN_KEY_TOTAL_SPEECH_CNT));
        }
        if (object.containsKey(SelectWithPreds.CRNN_KEY_NORMAL_SPEECH_CNT)) {
            normalCnt = (int) ((long) object.get(SelectWithPreds.CRNN_KEY_NORMAL_SPEECH_CNT));
        }

        HashMap<String, Integer> data = new HashMap<>();

        if (mCRNNResultMap.containsKey(key)) {
            data = mCRNNResultMap.get(key);
            totalCnt += data.get(SelectWithPreds.CRNN_KEY_TOTAL_SPEECH_CNT);
            normalCnt += data.get(SelectWithPreds.CRNN_KEY_NORMAL_SPEECH_CNT);
        }
        data.put(SelectWithPreds.CRNN_KEY_TOTAL_SPEECH_CNT, totalCnt);
        data.put(SelectWithPreds.CRNN_KEY_NORMAL_SPEECH_CNT, normalCnt);

        mCRNNResultMap.put(key, data);

        logger.debug(String.format("[update value] %-45s >> total[%6s], normal[%6s]", key, totalCnt, normalCnt));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void reportResult(ArrayList<String> dateList) {
        String date = dateList.get(0);
        if (dateList.size() > 1) {
            date = date + "-" + dateList.get(dateList.size() - 1);
        }

        String regions = "";
        for (String region : CommonConsts.REGIONS) {
            regions = regions + region + ".";
        }
        if (regions.endsWith(".")) {
            regions = regions.substring(0, regions.length() - 1);
        }

        mResultFileName = String.format("%s/%s_crnn_result_[%s].txt", CommonConsts.CRNN_CLASSIFIER_RESULT_REPORT, date, regions);
        initResultFile(mResultFileName);

        HashMap<String, StringBuilder> resultMap = new HashMap<>();
        try {
            Set<String> keySet = mCRNNResultMap.keySet();
            Vector vector = new Vector(keySet);
            Collections.sort(vector);

            ArrayList<String> results = new ArrayList<>();
            Iterator<String> keys = vector.iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                HashMap<String, Integer> value = mCRNNResultMap.get(key);
                if (value != null) {

                    int totalCnt = value.get(SelectWithPreds.CRNN_KEY_TOTAL_SPEECH_CNT);
                    int normalCnt = value.get(SelectWithPreds.CRNN_KEY_NORMAL_SPEECH_CNT);
                    String percentage = String.format("%.2f%%", (double) normalCnt / (double) totalCnt * 100.0);

                    /*results.add(String.format("%s %-30s :: TotalSpeech[%6d]  NormalSpeech[%6d]    >>  %s",
                            getInfoFromKey(key, POSITION_REGION), getInfoFromKey(key, POSITION_APPNAME), totalCnt, normalCnt, percentage));*/

                    results.add(String.format("%s %-30s :: %d / %d    >>  %s",
                            getInfoFromKey(key, POSITION_REGION), getInfoFromKey(key, POSITION_APPNAME), normalCnt, totalCnt, percentage));
                }
            }

            Collections.sort(results);
            BufferedWriter br = new BufferedWriter(new FileWriter(mResultFileName, true));
            String regionTag = "none";
            for (String result : results) {

                if (!result.startsWith(regionTag)) {
                    regionTag = result.split(" ")[0];
                    br.write(String.format("[%s]", regionTag));
                    br.write(CommonUtils.getLineSeparator());
                }

                result = result.replace(regionTag, "    *");

                br.write(result);
                br.write(CommonUtils.getLineSeparator());
            }

            br.flush();
            br.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        reportResultToSlack(date);
    }

    private final int POSITION_APPNAME = 1;
    private final int POSITION_REGION = 2;

    private String getInfoFromKey(String key, int position) {
        String[] splittedKey = key.split("_");
        if (position == POSITION_APPNAME && splittedKey[position].startsWith("com")) {
            key = splittedKey[position];
        } else if (position == POSITION_REGION) {
            key = splittedKey[position] + "_" + splittedKey[position + 1];
        }
        return key;
    }

    private void initResultFile(String filePath) {
        File file = new File(filePath);
        CommonUtils.makeDirectory(logger, file.getParentFile().getAbsolutePath());
        if (file.exists()) {
            boolean result = file.delete();
            logger.debug("initResultFile >> " + (result ? "success" : "failed"));
        }
    }

    private void reportResultToSlack(String date) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 이상발성 분류 결과 :: " + date + " =====");
        sb.append(CommonUtils.getLineSeparator());
        sb.append("한국어(ko-KR)에 대한 이상발성 Tagging 결과로, 오차가 있을 수 있습니다.\n< 정상발화 / 전체발화 >");
        sb.append(CommonUtils.getLineSeparator());

        File resultFile = new File(mResultFileName);
        if (resultFile.exists()) {
            try {
                FileReader filereader = new FileReader(resultFile);
                BufferedReader bufReader = new BufferedReader(filereader);

                String line = "";
                while ((line = bufReader.readLine()) != null) {
                    sb.append(line);
                    sb.append(CommonUtils.getLineSeparator());
                }

                bufReader.close();
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        String finalResult = sb.toString();
        SlackWebHook slack = new SlackWebHook(finalResult);
        slack.sendSlack(logger, SlackWebHook.WEBHOOK_LISTENALL_LOGGINGSTAT); // LISTENALL
        slack.sendSlack(logger, SlackWebHook.WEBHOOK_VAP_VI_LOGGING_STATUS); // for VAP
        slack.sendSlack(logger, SlackWebHook.WEBHOOK_PM_CHANNEL);
    }
}

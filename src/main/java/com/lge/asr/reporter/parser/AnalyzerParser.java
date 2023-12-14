package com.lge.asr.reporter.parser;

import com.lge.asr.classifier.parser.JSONParser;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.ListPathUtil;
import com.lge.asr.common.utils.SlackWebHook;
import com.lge.asr.common.utils.TextUtils;
import com.lge.asr.reporter.task.Analyzer;
import com.lge.asr.reporter.utils.ReporterUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

public class AnalyzerParser extends org.json.simple.parser.JSONParser {

    private static AnalyzerParser sInstance;
    private ArrayList<HashMap<String, Integer>> mDepth_map = new ArrayList<>();

    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_STATISTICS_REPORTER);

    public static AnalyzerParser getInstance() {
        if (sInstance == null) {
            sInstance = new AnalyzerParser();
        }
        return sInstance;
    }

    public void parsingReport(ArrayList<String> mDateList) {
        for (String region : CommonConsts.REGIONS) {
            for (String day : mDateList) {
                File jsonFile = new File(ReporterUtils.getJsonPath(logger, region, day));
                if (jsonFile.exists()) {
                    ParsingFromFilePath(jsonFile.getAbsolutePath());
                }
            }
        }
        reportResult(mDateList, mDepth_map);
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
        JSONArray pathArray = (JSONArray) object.get(Analyzer.JSON_KEY_COUNT);
        if (pathArray != null && pathArray.size() > 0) {
            for (int i = 0; i < pathArray.size(); i++) {
                JSONObject data = (JSONObject) pathArray.get(i);
                Iterator<String> keys = data.keySet().iterator();
                while (keys.hasNext()) {
                    String key = keys.next();
                    if (!key.endsWith("/kws") && !key.endsWith("/asr")) {
                        String value = (String) data.get(key);
                        sumValues(key, value);
                    }
                }
            }
        }
    }

    private void sumValues(String key, String value) {
        //key = key.replace("/data001/VILogData/output/", "");
        key = key.replace(CommonConsts.OUTPUT_PATH, "");
        String[] steps = key.split("/");
        for (int i = 0; i < steps.length - 1; i++) {
            try {
                sumValue(getMap(i), getMapKey(steps, i), Integer.parseInt(value));
            } catch (NumberFormatException e) {
                logger.error("NumberFormatException :: " + value);
            }
        }
    }

    private HashMap<String, Integer> getMap(int step) {
        if (mDepth_map.isEmpty() || mDepth_map.size() <= step || mDepth_map.get(step) == null) {
            mDepth_map.add(step, new HashMap<String, Integer>());
        }
        return mDepth_map.get(step);
    }

    private String getMapKey(String[] path, int step) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= step; i++) {
            sb.append(path[i]);
            if (!TextUtils.isEmpty(path[i]) && i < step) {
                sb.append("/");
            }
        }
        return sb.toString();
    }

    private void sumValue(HashMap<String, Integer> map, String key, int value) {
        if (map.containsKey(key)) {
            value += map.get(key);
        }
        map.put(key, value);
        logger.debug(String.format("update value >> for %s :: %s", key, value));
    }

    private void initResultFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            boolean result = file.delete();
            logger.debug("initResultFile >> " + (result ? "success" : "failed"));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void reportResult(ArrayList<String> dateList, ArrayList<HashMap<String, Integer>> depth_map) {
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

        String fileName = String.format("%s/%s_result_[%s].txt", CommonConsts.REPORT_PATH, date, regions);
        initResultFile(fileName);

        HashMap<String, StringBuilder> resultMap = new HashMap<>();
        try {
            BufferedWriter br = new BufferedWriter(new FileWriter(fileName, true));

            for (int i = 0; i < depth_map.size(); i++) {
                HashMap<String, Integer> map = depth_map.get(i);
                logger.info(String.format("=========== Depth[%s] level ===========", i));
                br.write(String.format("=========== Depth[%s] level ===========", i) + System.getProperty("line.separator"));
                Vector vector = new Vector(map.keySet());
                Collections.sort(vector);

                Iterator<String> keys = vector.iterator();
                while (keys.hasNext()) {
                    String key = keys.next();
                    int value = (int) map.get(key);
                    if (value > 0) {
                        String result;
                        if (i < 2) {
                            result = String.format("[%s][ %s ] %s", i, key, String.format("%,d", value));
                        } else {
                            String deviceCount = "";
                            if (i == 2 && dateList.size() == 1) {
                                String[] paths = key.split("/");
                                String appName = paths[i];
                                String region = paths[0];
                                deviceCount = getDeviceCount(region, date, appName);
                            }

                            String appName = "";
                            if (!TextUtils.isEmpty(deviceCount)) {
                                appName = String.format("%s (%s devices)", key, deviceCount);
                            } else {
                                appName = key;
                            }

                            result = String.format("[%s]     * %s::%s", i, appName, String.format("%,d", value));
                        }

                        logger.info(result);
                        appendResult(resultMap, key, result);
                        br.write(result);
                        br.write(System.getProperty("line.separator"));
                    }
                }
                br.write(System.getProperty("line.separator"));
            }

            br.flush();
            br.close();

            reportResultToSlack(date, resultMap);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void appendResult(HashMap<String, StringBuilder> resultMap, String path, String line) {
        String[] steps = path.split("/");
        String key = steps[0];

        if (!TextUtils.isEmpty(key) && !resultMap.containsKey(key)) {
            resultMap.put(key, new StringBuilder());
        }

        if (line.contains(key) && resultMap.containsKey(key)) {
            StringBuilder sb = resultMap.get(key);
            sb.append(line);
            sb.append(CommonUtils.getLineSeparator());
        }
    }

    private void reportResultToSlack(String date, HashMap<String, StringBuilder> resultMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Logging Status :: " + date + " =====");
        sb.append(CommonUtils.getLineSeparator());

        for (String region : CommonConsts.REGIONS) {
            if (region.equals(CommonConsts.REGION_SEOUL_DEV)) {
                continue;
            }
            try {
                String body = getFormedBody(region, resultMap.get(region).toString());
                sb.append(body);
                sb.append(CommonUtils.getLineSeparator());
            } catch (NullPointerException e) {
                logger.error(e.getMessage());
            }
        }

        String finalResult = sb.toString();
        SlackWebHook slack = new SlackWebHook(finalResult);
        slack.sendSlack(logger, SlackWebHook.WEBHOOK_LISTENALL_LOGGINGSTAT); // LISTENALL
        slack.sendSlack(logger, SlackWebHook.WEBHOOK_VAP_VI_LOGGING_STATUS); // for VAP
        slack.sendSlack(logger, SlackWebHook.WEBHOOK_PM_CHANNEL);
    }

    private String getFormedBody(String region, String body) {
        String[] lines = body.split(CommonUtils.getLineSeparator());
        ArrayList<String> firstBodyList = new ArrayList<>();
        ArrayList<String> secondBodyList = new ArrayList<>();
        for (String line : lines) {
            if (/*line.startsWith("[0]")
                    ||*/ line.contains("/rejected_voice_trigger/")
                    || line.contains("fail")
                    || (line.startsWith("[3]") && !line.toLowerCase().contains("ko-") && !line.toLowerCase().contains("en-"))) {
                continue;
            }

            line = line.replaceAll(region + "/success/", "");
            if (line.startsWith("[0]") || line.startsWith("[1]")) {
                line = line.substring(3);
                firstBodyList.add(line);
            } else {
                if (line.startsWith("[3]")) {
                    line = line + "[3]";
                }
                line = line.substring(3);
                secondBodyList.add(line);
            }
        }
        Collections.sort(secondBodyList);

        StringBuilder sb = new StringBuilder();
        for (String line : firstBodyList) {
            sb.append(line);
            sb.append(CommonUtils.getLineSeparator());
        }
        for (String line : secondBodyList) {
            if (line.endsWith("[3]")) {
                line = line.replace("[3]", "");
                line = line.replace("*", "  ");
            }
            String[] contents = line.split("::");
            line = String.format("%-50s :: %s", contents[0], contents[1]);

            sb.append(line);
            sb.append(CommonUtils.getLineSeparator());
        }

        return sb.toString();
    }

    private String getDeviceCount(String region, String date, String appName) {
        String count = "";

        String deviceInfoJson = ListPathUtil.getDeviceInfoJson(region, date);
        logger.debug("[jerome] getDeviceCount :: deviceInfoJson = " + deviceInfoJson);
        File jsonFile = new File(deviceInfoJson);
        if (jsonFile.exists()) {
            JSONObject jsonObj = JSONParser.getInstance(logger).getJsonObjectFromFilePath(deviceInfoJson);
            if (jsonObj != null && !jsonObj.isEmpty()) {
                if (jsonObj.containsKey(appName)) {
                    HashMap<String, String> value = (HashMap<String, String>) jsonObj.get(appName);
                    count = String.valueOf(value.size());
                }
            }
        }

        return count;
    }
}

package com.lge.asr.reporter.task;

import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.TextUtils;
import com.lge.asr.reporter.utils.ReporterUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class Analyzer implements Runnable {

    private Logger mLogger;
    private String mTargetRegion;
    private String mTargetDate;

    public static final String JSON_KEY_REQ_DATA = "REQ_DATA";
    public static final String JSON_KEY_DATE = "DATE";
    public static final String JSON_KEY_COUNT = "COUNT";

    public Analyzer(String region, String date) {
        mTargetRegion = region;
        mTargetDate = date;
        mLogger = CommonUtils.getLogger(CommonConsts.LOGGER_STATISTICS_REPORTER, region, date);
    }

    public Analyzer(String region, String date, Logger logger) {
        mTargetRegion = region;
        mTargetDate = date;
        mLogger = logger;
    }

    @Override
    public void run() {
        ProgressNotifier.getInstance().setStatus(mLogger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_BASIC_REPORTER, ProgressNotifier.START);
        mLogger.debug("[Start] Analyzer - " + mTargetRegion);
        File jsonFile = new File(ReporterUtils.getJsonPath(mLogger, mTargetRegion, mTargetDate));
        if (!jsonFile.exists()) {
            ArrayList<String> listForTargetDate = getDateList(mTargetRegion, mTargetDate);
            if (!listForTargetDate.isEmpty()) {
                JSONObject object = getDataCountByPath(listForTargetDate);
                saveJsonFile(mTargetRegion, mTargetDate, object);
            }
        }
        mLogger.debug("[End] Analyzer - " + mTargetRegion);
        ProgressNotifier.getInstance().setStatus(mLogger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_BASIC_REPORTER, ProgressNotifier.FINISH);
    }

    @SuppressWarnings("unchecked")
    private JSONObject getDataCountByPath(ArrayList<String> listForTheDay) {
        JSONObject dailyData = new JSONObject();
        dailyData.put(JSON_KEY_DATE, mTargetDate);

        JSONObject data = new JSONObject();
        for (String path : listForTheDay) {
            /*String command = String.format("ls %s |grep pcm |wc -l", path);
            String count = CommonUtils.shellCommand(mLogger, command);
            mLogger.debug("ListPath :: " + path + " has [" + count + "] data.");
            data.put(path, count);*/

            putDataCount(data, path);
            // putDataCount(data, path + "/kws");  // 기동어 count
            // putDataCount(data, path + "/asr");  // 연속어 count
        }

        JSONArray pathArray = new JSONArray();
        pathArray.add(data);
        dailyData.put(JSON_KEY_COUNT, pathArray);

        mLogger.debug(System.getProperty("line.separator"));
        return dailyData;
    }

    private void putDataCount(JSONObject data, String path) {
        String command = String.format("ls %s |grep meta |wc -l", path);
        String count = CommonUtils.shellCommand(mLogger, command);
        mLogger.debug("ListPath :: " + path + " has [" + count + "] data.");
        data.put(path, count);
    }

    public ArrayList<String> getDateList(String targetRegion, String targetDate) {
        mLogger.info("start checking for the date " + targetDate);

        String basePath = CommonConsts.OUTPUT_PATH + targetRegion;
        ArrayList<String> list = searchList(basePath, targetDate, new ArrayList<String>());

        mLogger.debug(String.format("DateList for %s has %s paths", targetDate, list.size()));
        mLogger.info("finish checking for the date " + targetDate);

        return list;
    }

    private ArrayList<String> searchList(String path, String date, ArrayList<String> list) {
        File dir = new File(path);
        File[] fileList = dir.listFiles();
        if (fileList == null || fileList.length == 0) {
            return new ArrayList<>();
        }
        Arrays.sort(fileList);
        try {
            for (int i = 0; i < fileList.length; i++) {
                File file = fileList[i];
                if (file.isDirectory()) {
                    String directoryName = file.getName();
                    if (!TextUtils.isEmpty(directoryName) && directoryName.equals(date)) {
                        list.add(file.getAbsolutePath());
                        return list;
                    } else if (!directoryName.startsWith("20")) {
                        list = searchList(file.getCanonicalPath().toString(), date, list);
                    }
                }
            }
        } catch (IOException e) {
            mLogger.error(e.getMessage());
        }
        return list;
    }

    public String saveJsonFile(String region, String day, JSONObject object) {
        String fileName = ReporterUtils.getJsonPath(mLogger, region, day);
        try {
            FileWriter fileWriter = new FileWriter(fileName);
            fileWriter.write(object.toString());
            fileWriter.close();
            System.out.println("Successfully saved JSON Object to File...");
        } catch (IOException e) {
            mLogger.error(e.getMessage());
        }

        File created = new File(fileName);
        if (created.exists()) {
            return created.getAbsolutePath();
        } else {
            return null;
        }
    }

}

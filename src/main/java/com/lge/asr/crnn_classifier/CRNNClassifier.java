package com.lge.asr.crnn_classifier;

import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.ListPathUtil;
import com.lge.asr.reporter.CRNNReporter;
import com.lge.asr.reporter.utils.ReporterUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CRNNClassifier extends Thread {
    private Logger mLogger;

    private String mTargetRegion;
    private String mTargetDate;
    private String mBasePath;
    private ArrayList<String> mPathList = new ArrayList<>();

    private ListPathUtil mListPathUtil;

    public static final String CRNN_CLASSIFIER_RESULT_FILE_NAME = "crnn_classifer_result";

    public CRNNClassifier(String region, String date) {
        this.mTargetRegion = region;
        this.mTargetDate = date;
        mLogger = CommonUtils.getLogger(CommonConsts.LOGGER_CLASSIFIER, region, date);

        mListPathUtil = new ListPathUtil(mLogger, mTargetRegion, mTargetDate);
    }

    public CRNNClassifier(String region, String date, Logger logger) {
        this.mTargetRegion = region;
        this.mTargetDate = date;
        mLogger = logger;

        mListPathUtil = new ListPathUtil(mLogger, mTargetRegion, mTargetDate);
    }

    @Override
    public void run() {
        String startMessage = String.format("[CRNNClassifier][Start] [%s]-%s-", mTargetRegion, mTargetDate);
        mLogger.info(startMessage);
        ProgressNotifier.getInstance().sendCRNNClassifierNoti(mLogger, mTargetDate, mTargetRegion, ProgressNotifier.START);

        if (!mListPathUtil.readTargetList(mPathList, false, true)) {
            mBasePath = String.format("%s%s/success", CommonConsts.OUTPUT_PATH, mTargetRegion);
            mListPathUtil.setTargetPathForKoKr(mBasePath, mPathList);
        }
        System.out.println("====== CRNN Classifier :: " + mPathList.size() + " paths ======");

        JSONObject resultJsonObj = new JSONObject();

        ExecutorService executor = Executors.newFixedThreadPool(1);
        for (String path : mPathList) {
            if (!isKwsOnlyClient(path) && !isWebOs(path)) {
                Runnable splitter = new ClassifierRunnable(mLogger, path, resultJsonObj);
                executor.execute(splitter);
            }
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        saveJsonFile(resultJsonObj);

        String finishMessage = String.format("[CRNNClassifier][Finish]. [%s]-%s-", mTargetRegion, mTargetDate);
        mLogger.info(finishMessage);
        ProgressNotifier.getInstance().sendCRNNClassifierNoti(mLogger, mTargetDate, mTargetRegion, ProgressNotifier.FINISH);

        //CRNNReporter.getInstance(mTargetDate).reportCRNNResult(mTargetRegion);
    }

    public String saveJsonFile(JSONObject object) {
        String fileName = ReporterUtils.getJsonPath(mLogger, mTargetRegion, mTargetDate, CommonConsts.CRNN_CLASSIFIER_RESULT_JSON, CRNN_CLASSIFIER_RESULT_FILE_NAME);
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

    public String getRegion() {
        return mTargetRegion;
    }

    private boolean isKwsOnlyClient(String path) {
        if (path.contains("com.lge.robot.homebot")) {
            return true;
        }
        return false;
    }

    private boolean isWebOs(String path) {
        if (path.contains("com.webos")) {
            return true;
        }
        return false;
    }
}
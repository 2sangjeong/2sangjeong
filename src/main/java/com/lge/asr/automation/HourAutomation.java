/*
 * Mobile Communication Company, LG ELECTRONICS INC., SEOUL, KOREA
 * Copyright(c) 2019 by LG Electronics Inc.
 *
 * All rights reserved. No part of this work may be reproduced, stored in a retrieval system,
 * or transmitted by any means without prior written
 * Permission of LG Electronics Inc.
 */

package com.lge.asr.automation;

import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.crnn_classifier.CRNNClassifier;
import com.lge.asr.crnn_classifier.CRNNClassifierManager;
import com.lge.asr.csv_saver.CsvSaver;
import com.lge.asr.extractor.task.LogDataProcessorByHour;
import com.lge.asr.reporter.CRNNReporter;
import com.lge.asr.reporter.StatisticsReporter;
import com.lge.asr.reporter.task.Analyzer;
import com.lge.asr.splitter.PcmSplitter;
import org.apache.log4j.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.lge.asr.common.constants.CommonConsts.AWS_LOG_DATA_PATH;
import static com.lge.asr.common.constants.CommonConsts.DATA_MANAGEMENT;

public class HourAutomation extends Thread {

    private static Logger logger;

    private String mTargetRegion;
    private String mTargetDate;
    private String mHour;
    private String mBasePath;

    private ArrayList<String> mTarFiles = new ArrayList<>();

    private boolean shellLogging = true;
    private boolean isManualMode = false;

    public HourAutomation(String region) {
        setTime();
        mTargetRegion = region;
        init();
    }

    public void setManualMode(String date, String time) {
        isManualMode = true;
        mHour = time;
        mTargetDate = date;
        init();
    }

    private void init() {
        mBasePath = String.format("%s/%s/%s/%s", CommonConsts.DATA_LAKE_WORKSPACE, mTargetDate, mHour, mTargetRegion);
        logger = CommonUtils.getLogger(CommonConsts.LOGGER_HOUR, mTargetRegion, mTargetDate + "-" + mHour);
        ProgressNotifier.getInstance().setHourAutiomationTime(mHour);
        mTarFiles.clear();
    }

    /**
     * @author jerome.kim
     * DataProcessing :: 매 시간 (-1 hour)를 기준으로 수행한다.
     */
    @Override
    public void run() {
        runReporter(); // run 08:00

        logger.info("### HourAutomation [" + mTargetRegion + "] [ " + mTargetDate + "-" + mHour + " ] ###");
        ProgressNotifier.getInstance().setFirstStep(ProgressNotifier.PROCESS_EXTRACTOR);

        notifyProcess(ProgressNotifier.PROCESS_EXTRACTOR, ProgressNotifier.START);
        logDataProcessorByHour();

//        runSplitter();

        notifyProcess(ProgressNotifier.PROCESS_COPY_N_ADJUST, ProgressNotifier.START);
        copyToOutputPath();

        removeFailedData();

        adjustSuccessPath();

        notifyProcess(ProgressNotifier.PROCESS_ZIP_N_RELEASE, ProgressNotifier.START);

//        exportMeta();
        /* == Hold by DataLake ==
        compressTarZip();
        uploadTar();
        */

        removeTempData();

//        runAfterProcess();
        notifyProcess(ProgressNotifier.PROCESS_HOUR_AUTOMATION, ProgressNotifier.FINISH);

        if (mHour.equals("23") && ProgressNotifier.getInstance().finishAllRegions()) {
            runCsvSaver(); // region에 상관없이 한번만 수행되어야 함!!!
            removeWorkspaceData();
        }

        if (ProgressNotifier.getInstance().finishAllRegions()) {
            copyBgFullLog();
        }
    }

    private void logDataProcessorByHour() {
        logger.info(CommonUtils.getLineSeparator());
        logger.info("=========================================================================");
        logger.info(String.format("-start- [%s] logDataProcessorByHour", mTargetRegion));
        LogDataProcessorByHour extractor = new LogDataProcessorByHour(mTargetRegion, mTargetDate, mHour);
        extractor.run();
        try {
            extractor.join();
        } catch (InterruptedException e) {
            logger.error("InterruptedException");
            CommonUtils.terminateSystem();
        }
        logger.info(String.format("-completed- [%s] logDataProcessorByHour", mTargetRegion));
    }

    private void runSplitter() {
        logger.info(CommonUtils.getLineSeparator());
        logger.info("=========================================================================");
        logger.info(String.format("-start- [%s] PCM Splitter", mTargetRegion));
        PcmSplitter splitter = new PcmSplitter(mTargetRegion, mTargetDate);
        splitter.run();
        try {
            splitter.join();
        } catch (InterruptedException e) {
            logger.error("InterruptedException");
            CommonUtils.terminateSystem();
        }
        logger.info(String.format("-completed- [%s] PCM Splitter", mTargetRegion));
    }

    private void copyToOutputPath() {
        logger.info(CommonUtils.getLineSeparator());
        logger.info("=========================================================================");
        logger.info(String.format("-start- [%s] copyToOutputPath", mTargetRegion));
        String output = CommonConsts.OUTPUT_PATH + mTargetRegion;
        CommonUtils.makeDirectory(logger, output + "/success");
        CommonUtils.makeDirectory(logger, output + "/fail");
        CommonUtils.makeDirectory(logger, output + "/rejected_voice_trigger");

        logger.info("DEV_CHECK :: " + output);
        File pathDir = new File(String.format("%s/success", mBasePath));
        if (pathDir.exists() && pathDir.isDirectory()) {
            CommonUtils.shellCommand(logger, String.format("cp -ruav %s/success/* %s/success/", mBasePath, output), shellLogging, false);
        } else {
            logger.error("CHECK : " + pathDir.toString());
        }

        pathDir = new File(String.format("%s/fail", mBasePath));
        if (pathDir.exists() && pathDir.isDirectory()) {
            CommonUtils.shellCommand(logger, String.format("cp -ruav %s/fail/* %s/fail/", mBasePath, output), shellLogging, false);
        } else {
            logger.error("CHECK : " + pathDir.toString());
        }

        pathDir = new File(String.format("%s/rejected_voice_trigger", mBasePath));
        if (pathDir.exists() && pathDir.isDirectory()) {
            CommonUtils.shellCommand(logger, String.format("cp -ruav %s/rejected_voice_trigger/* %s/rejected_voice_trigger/", mBasePath, output), shellLogging, false);
        } else {
            logger.error("CHECK : " + pathDir.toString());
        }

        logger.info(String.format("-completed- [%s] copyToOutputPath", mTargetRegion));
    }

    private void removeFailedData() {
        logger.info(CommonUtils.getLineSeparator());
        logger.info("=========================================================================");
        logger.info(String.format("-start- [%s] removeFailedData", mTargetRegion));

        String failPath = mBasePath + "/" + "fail";
        CommonUtils.shellCommand(logger, String.format("rm -rf %s", failPath), shellLogging, false);

        String rejectedVoiceTriggerPath = mBasePath + "/" + "rejected_voice_trigger";
        CommonUtils.shellCommand(logger, String.format("rm -rf %s", rejectedVoiceTriggerPath), shellLogging, false);

        logger.info(String.format("-completed- [%s] removeFailedData", mTargetRegion));
    }

    private void adjustSuccessPath() {
        logger.info(CommonUtils.getLineSeparator());
        logger.info("=========================================================================");
        logger.info(String.format("-start- [%s] adjustSuccessPath", mTargetRegion));
        String successPath = mBasePath + "/" + "success";
        String adjusted = mBasePath + "/";
        CommonUtils.shellCommand(logger, String.format("mv %s/* %s", successPath, adjusted), shellLogging, false);
        CommonUtils.shellCommand(logger, String.format("rm -rf %s", successPath), shellLogging, false);
        logger.info(String.format("-completed- [%s] adjustSuccessPath", mTargetRegion));
    }

    private void compressTarZip() {
        logger.info(CommonUtils.getLineSeparator());
        logger.info("=========================================================================");
        logger.info(String.format("-start- [%s] tarResult", mTargetRegion));

        String year = mTargetDate.substring(0, 4);
        String month = mTargetDate.substring(4, 6);
        String day = mTargetDate.substring(6, 8);

        String path = String.format("%s/%s", CommonConsts.DATA_LAKE, mTargetRegion);

        File baseDir = new File(mBasePath);
        if (baseDir.isDirectory()) {
            File[] appList = baseDir.listFiles();
            Arrays.sort(appList);
            for (File appName : appList) {
                if (appName.isDirectory()) {
                    // Language 분류
                    File[] languages = appName.listFiles();
                    for (File language : languages) {
                        if (language.isDirectory()) {
                            // {%각_리전별_버킷이름%}/{%APP_NAME%}/{%LANGUAGE%}/YYYY/mm/DD/HH/{%FILE_NAME%}.tgz
                            String finalPath = String.format("%s/%s/%s/%s/%s/%s/%s", path, appName.getName(), language.getName(), year, month, day, mHour);
                            CommonUtils.makeDirectory(logger, finalPath);

                            String tarName = String.format("%s_%s_%s_%s_%s.tgz", mTargetRegion, mTargetDate, mHour, appName.getName(), language.getName());
                            tarName = finalPath + "/" + tarName;
                            String command = String.format("cd %s && tar -cvzf %s ./%s/%s --exclude=*.meta --exclude=asr --exclude=kws",
                                    mBasePath, tarName, appName.getName(), language.getName());
                            CommonUtils.shellCommand(logger, command, true, false);

                            mTarFiles.add(tarName);
                        }
                    }
                }
            }
        }
        logger.info(String.format("-completed- [%s] tarResult", mTargetRegion));
    }

    private void exportMeta() {
        String metaList = CommonUtils.getMetaListFilePath(logger, mTargetRegion, mTargetDate, mHour);
        File file = new File(metaList);
        if (file.exists()) {
            uploadMeta(metaList);
        }
    }

    private void uploadMeta(String filePath) {
        if (mTargetRegion.contains("_dev")) { // seoul_dev
            return;
        }
        String year = mTargetDate.substring(0, 4);
        String month = mTargetDate.substring(4, 6);
        String day = mTargetDate.substring(6, 8);

        String basePath = String.format("%s/%s/%s/%s/%s/%s/", CommonUtils.removeSlash(CommonConsts.DATALAKE_BUCKET), mTargetRegion, year,
                month, day, mHour);

        logger.debug("-uploadMeta- :: " + basePath);
        CommonUtils.shellCommand(logger, String.format("aws s3 cp %s %s", filePath, basePath), true, false);
    }

    private void uploadTar() {
        logger.info(CommonUtils.getLineSeparator());
        logger.info("=========================================================================");
        logger.info(String.format("-start- [%s] uploadTar", mTargetRegion));
        for (String tarFile : mTarFiles) {
            try {
                String[] tarFileName = tarFile.split("/");
                String[] tarFileInfo = (tarFileName[tarFileName.length - 1].replace(".tgz", "")).split("_");
                String appName = tarFileInfo[4];
                String language = tarFileInfo[5];
                String date = tarFileInfo[2];

                String year = date.substring(0, 4);
                String month = date.substring(4, 6);
                String day = date.substring(6, 8);
                String hour = tarFileInfo[3];

                CommonUtils.shellCommand(logger, String.format("aws s3 cp %s %s%s/%s/%s/%s/%s/%s/%s/", tarFile,
                        CommonConsts.DATALAKE_BUCKET, mTargetRegion, appName, language, year, month, day, hour), true, false);
            } catch (NullPointerException | ArrayIndexOutOfBoundsException e) {
                logger.error(e.getMessage());
            }
        }
        logger.info(String.format("-completed- [%s] uploadTar", mTargetRegion));
    }

    private void removeTempData() {
        logger.info(CommonUtils.getLineSeparator());
        logger.info("=========================================================================");
        logger.info(String.format("-start- [%s] removeTempData", mTargetRegion));

        CommonUtils.shellCommand(logger, String.format("rm -rf %s", mBasePath), shellLogging, false);

        if (mHour.equals("23")) {
            String removePath = String.format("%s/%s/logs/%s", CommonUtils.removeSlash(AWS_LOG_DATA_PATH), mTargetRegion, mTargetDate);
            CommonUtils.shellCommand(logger, String.format("rm -rf %s", removePath), shellLogging, false);
        }

        logger.info(String.format("-completed- [%s] removeTempData", mTargetRegion));
    }

    private void removeWorkspaceData() {
        String removePath = String.format("%s/%s", CommonConsts.DATA_LAKE_WORKSPACE, mTargetDate);
        CommonUtils.shellCommand(logger, String.format("rm -rf %s", removePath), false, false);

        removePath = String.format("%s/%s/%s", AWS_LOG_DATA_PATH, mTargetRegion, mTargetDate);
        CommonUtils.shellCommand(logger, String.format("rm -rf %s", removePath), false, false);
    }

    private void runDailyAnalyzer() {
        logger.info(CommonUtils.getLineSeparator());
        logger.info("=========================================================================");
        logger.info(String.format("-start- [%s] DailyAnalyzer", mTargetRegion));
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Runnable analyzer = new Analyzer(mTargetRegion, mTargetDate);
        executor.execute(analyzer);
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        logger.info(String.format("-completed- [%s] DailyAnalyzer", mTargetRegion));
    }

    private void runCRNNClassifier() {
        CRNNClassifier crnnClassifier = new CRNNClassifier(mTargetRegion, mTargetDate);
        CRNNClassifierManager.getInstance(mTargetRegion, mTargetDate).runClassifier(crnnClassifier);
    }

    private void runCsvSaver() {
        logger.info(CommonUtils.getLineSeparator());
        logger.info("=========================================================================");
        logger.info(String.format("-start- [%s] CSV Saver", mTargetRegion));
        CsvSaver csvSaver = new CsvSaver(mTargetDate);
        csvSaver.run();
        try {
            csvSaver.join();
        } catch (InterruptedException e) {
            logger.error("InterruptedException");
            CommonUtils.terminateSystem();
        }
        logger.info(String.format("-completed- [%s] CSV Saver", mTargetRegion));
    }

    private void runAfterProcess() {
        if (mHour.equals("23")) {
            runDailyAnalyzer();
            runCRNNClassifier();
        }
    }

    private void notifyProcess(String step, int status) {
        ProgressNotifier.getInstance().setStatus(logger, mTargetDate, mTargetRegion, step, status);
    }

    private void setTime() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1);
        SimpleDateFormat hour = new SimpleDateFormat("HH");

        mHour = hour.format(cal.getTime());
        mTargetDate = CommonUtils.getDate(CommonConsts.DATE_UNIT.DAY, cal.getTime());
    }

    private void runReporter() {
        if (mHour.equals("07")) { // Report KST 08:00
            // NO MORE USED!!
            // This function has been moved to LoggingDataManager2.0

            // String yesterday = CommonUtils.getYesterday(mTargetDate);
            // logger.info("### runReporter [" + mTargetRegion + "] [ " + yesterday + " ] ###");

            // StatisticsReporter.getInstance(yesterday).reportAnalyzedResult(mTargetRegion);
            // CRNNReporter.getInstance(yesterday).reportCRNNResult(mTargetRegion);
        }
    }

    private void copyBgFullLog() {
        String year = mTargetDate.substring(0, 4);
        String month = mTargetDate.substring(4, 6);
        String day = mTargetDate.substring(6, 8);

        String destPath = String.format("%s/%s/%s/%s/%s", CommonUtils.removeSlash(DATA_MANAGEMENT), "logs/full-bg-log", year, month, day);
        CommonUtils.makeDirectory(logger, destPath);

        String tempLog = String.format("%s/%s_%s_runtime.log", CommonUtils.removeSlash(DATA_MANAGEMENT), mTargetDate, mHour);
        String cmd = String.format("mv %s %s/", tempLog, destPath);
        File runtimeLogFile = new File(tempLog);
        if (isManualMode || !runtimeLogFile.exists()) {
            tempLog = String.format("%s/runtime.log", CommonUtils.removeSlash(DATA_MANAGEMENT));
            cmd = String.format("cp %s %s/%s_%s_runtime.log", tempLog, destPath, mTargetDate, mHour);
        }

        CommonUtils.shellCommand(logger, cmd, false, false);
    }

}

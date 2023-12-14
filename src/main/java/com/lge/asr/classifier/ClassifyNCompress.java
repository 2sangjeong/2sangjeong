package com.lge.asr.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.lge.asr.classifier.task.ClassifyRunnable;
import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.ListPathUtil;

public class ClassifyNCompress extends Thread {

    private Logger mLogger;

    private String mTargetRegion;
    private String mTargetDate;
    private String mBasePath;

    private ArrayList<String> mPathList = new ArrayList<>();
    private ListPathUtil mListPathUtil;

    public ClassifyNCompress(String region, String date) {
        this.mTargetRegion = region;
        this.mTargetDate = date;

        mLogger = CommonUtils.getLogger(CommonConsts.LOGGER_CLASSIFIER, region, date);
        mListPathUtil = new ListPathUtil(mLogger, mTargetRegion, mTargetDate);
    }

    @Override
    public void run() {
        String startMessage = String.format("[ClassifyRejectedVoiceTrigger][Start] [%s]-%s-", mTargetRegion, mTargetDate);
        mLogger.info(startMessage);
        ProgressNotifier.getInstance().setStatus(mLogger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_CLASSIFIER,
                ProgressNotifier.START);

        mBasePath = String.format("%s%s/success", CommonConsts.OUTPUT_PATH, mTargetRegion);
        mListPathUtil.setTargetPathForAllLanguage(mBasePath, mPathList);
        System.out.println("====== ClassifyRejectedVoiceTrigger :: " + mPathList.size() + " paths ======");

        //ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (String path : mPathList) {
                Thread classifier = new ClassifyRunnable(mLogger, mTargetRegion, mTargetDate, path);
                classifier.start();
                
                try {
                    classifier.join();
                } catch (InterruptedException e) {
                    mLogger.error("InterruptedException");
                    CommonUtils.terminateSystem();
                }
                //executor.execute(classifier);
            }
            /*executor.shutdown();
            while (!executor.isTerminated()) {
            }*/
        } finally {
            String finishMessage = String.format("[ClassifyRejectedVoiceTrigger][Finish]. [%s]-%s-", mTargetRegion, mTargetDate);
            mLogger.info(finishMessage);
            ProgressNotifier.getInstance().setStatus(mLogger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_CLASSIFIER,
                    ProgressNotifier.FINISH);
        }

        //compressTarZip();
    }
    
    private void compressTarZip() {
        String year = mTargetDate.substring(0, 4);
        String month = mTargetDate.substring(4, 6);
        String day = mTargetDate.substring(6, 8);

        String datalake = String.format("%s/%s", CommonConsts.DATA_LAKE, mTargetRegion);
        String path1st = String.format("%s/%s", CommonConsts.DATA_LAKE_WORKSPACE, mTargetDate);
        File pathDir = new File(path1st);
        if (pathDir != null && pathDir.isDirectory()) {
            File[] times = pathDir.listFiles();
            for (File time : times) {
                if (!time.isDirectory()) {
                    continue;
                }
                String hour = time.getName();
                String target = String.format("%s/%s/%s", path1st, hour, mTargetRegion);
                File baseDir = new File(target);
                if (baseDir.isDirectory()) {
                    File[] appList = baseDir.listFiles();
                    Arrays.sort(appList);
                    for (File appName : appList) {
                        if (appName.isDirectory()) {
                            File[] languages = appName.listFiles();
                            for (File language : languages) {
                                if (language.isDirectory()) {
                                    // {%각_리전별_버킷이름%}/{%APP_NAME%}/{%LANGUAGE%}/YYYY/mm/DD/HH/{%FILE_NAME%}.tgz
                                    String finalPath = String.format("%s/%s/%s/%s/%s/%s/%s", datalake, appName.getName(),
                                            language.getName(), year, month, day, hour);
                                    CommonUtils.makeDirectory(mLogger, finalPath);

                                    String tarName = String.format("%s_%s_%s_%s_%s.tgz", mTargetRegion, mTargetDate, hour,
                                            appName.getName(), language.getName());
                                    tarName = finalPath + "/" + tarName;

                                    String command = String.format(
                                            "cd %s && tar -cvzf %s ./%s/%s --exclude=*.meta --exclude=asr --exclude=kws", target, tarName,
                                            appName.getName(), language.getName());
                                    CommonUtils.shellCommand(mLogger, command, true, false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

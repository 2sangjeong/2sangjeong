package com.lge.asr.classifier.task;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.lge.asr.classifier.constant.MetaInformationConts;
import com.lge.asr.classifier.parser.JSONParser;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.TextUtils;
import com.lge.asr.extractor.task.MetaSaver;

public class ClassifyRunnable extends Thread {

    private Logger mLogger;

    private String mBasePath;
    private String mTargetRegion;
    private String mTargetDate;

    private int mCount;

    private MetaSaverManager mMetaSaverManager;

    public ClassifyRunnable(Logger logger, String region, String date, String basePath) {
        mLogger = logger;
        mTargetRegion = region;
        mTargetDate = date;
        mBasePath = basePath;
        mCount = 0;
        mMetaSaverManager = new MetaSaverManager(mLogger, region, date);
    }

    @Override
    public void run() {
        File pathDir = new File(mBasePath);
        if (pathDir.exists() && pathDir.isDirectory()) {
            File[] fileList = pathDir.listFiles();
            mLogger.info("ClassifyRunnable :: " + mBasePath + "[" + fileList.length / 2 + "]");
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isFile() && fileList[i].getName().endsWith(".meta")) {
                    File meta = fileList[i];
                    if (isRejectedVoiceTrigger(meta.getAbsolutePath())) {
                        String serviceId = meta.getName().replace(".meta", "");
                        moveRejectedLoggingData(serviceId);
                        mCount++;
                    } else {
                        copyToDataLakeWorkspaceByHour(meta);
                    }
                }
            }
        }

        /*ArrayList<File> metaList = listingMetaFiles();
        mLogger.info("ClassifyRunnable :: " + mBasePath + "[" + metaList.size() + "]");
        if (metaList != null && metaList.size() > 0) {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            for (File meta : metaList) {
                if (isRejectedVoiceTrigger(meta.getAbsolutePath())) {
                    String serviceId = meta.getName().replace(".meta", "");
                    moveRejectedLoggingData(serviceId);
                    mCount++;
                } else {
                    copyToDataLakeWorkspaceByHour(meta);
                }
                try {
                    if (meta.isFile()) {
                        Runnable innerRunnable = new ProcessInnerRunner(meta);
                        executor.execute(innerRunnable);
                    }
                    executor.shutdown();
                    while (!executor.isTerminated()) {
                    }
                } finally {
                    mLogger.info("ClassifyRunnable-finally-" + mBasePath);
                }
            }
        }*/
        finishAllMetaSavers();
        mLogger.info(String.format("ClassifyRejectedRunnable :: moved %s serviceId from %s", mCount, mBasePath));
    }

    class ProcessInnerRunner implements Runnable {
        private File meta;

        public ProcessInnerRunner(File meta) {
            this.meta = meta;
        }

        @Override
        public void run() {
            if (isRejectedVoiceTrigger(meta.getAbsolutePath())) {
                String serviceId = meta.getName().replace(".meta", "");
                moveRejectedLoggingData(serviceId);
                mCount++;
            } else {
                copyToDataLakeWorkspaceByHour(meta);
            }
        }
    }

    private ArrayList<File> listingMetaFiles() {
        File pathDir = new File(mBasePath);
        if (pathDir.exists() && pathDir.isDirectory()) {
            File[] fileList = pathDir.listFiles();
            ArrayList<File> metaList = new ArrayList<>();
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isFile() && fileList[i].getName().endsWith(".meta")) {
                    metaList.add(fileList[i]);
                }
            }
            return metaList;
        }
        return null;
    }

    private boolean isRejectedVoiceTrigger(String metaPath) {
        if (!TextUtils.isEmpty(metaPath)) {
            String finalResult = JSONParser.getInstance(mLogger).getFinalResult(metaPath);
            if (!TextUtils.isEmpty(finalResult) && (finalResult.contains("REJECTED_VOICETRIGGER") || finalResult.contains("$FALSE$"))) {
                return true;
            }
        }
        return false;
    }

    private void moveRejectedLoggingData(String serviceId) {
        String rejectedTargetPath = mBasePath.replace("/success/", "/rejected_voice_trigger/");
        CommonUtils.makeDirectory(mLogger, rejectedTargetPath);
        String moveCommand = String.format("mv %s/%s.* %s/", mBasePath, serviceId, rejectedTargetPath);
        CommonUtils.shellCommand(mLogger, moveCommand, true, false);
    }

    private void copyToDataLakeWorkspaceByHour(File meta) {
        String metaPath = meta.getAbsolutePath();
        if (!TextUtils.isEmpty(metaPath)) {
            JSONObject metaObject = JSONParser.getInstance(mLogger).getJsonObjectFromFilePath(metaPath);
            /*
             * if (metaObject != null) { String hour =
             * JSONParser.getInstance().getJsonText(metaObject.get(MetaInformationConts.HOUR
             * )); String replaced = String.format("%s%s/%s", CommonConsts.OUTPUT_PATH,
             * mTargetRegion, "success"); String datalakeWorkspace =
             * String.format("%s/%s/%s/%s", CommonConsts.DATA_LAKE_WORKSPACE, mTargetDate,
             * hour, mTargetRegion); datalakeWorkspace = mBasePath.replace(replaced,
             * datalakeWorkspace); CommonUtils.makeDirectory(mLogger, datalakeWorkspace);
             * 
             * String serviceId = meta.getName().replace(".meta", ""); String cpCommand =
             * String.format("cp %s/%s.* %s/", mBasePath, serviceId, datalakeWorkspace);
             * saveMetaListByHour(metaObject, hour); CommonUtils.shellCommand(mLogger,
             * cpCommand, true, false); }
             */
            if (metaObject != null) {
                String hour = JSONParser.getInstance(mLogger).getJsonText(metaObject.get(MetaInformationConts.HOUR));
                saveMetaListByHour(metaObject, hour);
            }
        }
    }

    private void saveMetaListByHour(JSONObject meta, String hour) {
        // mLogger.debug("saveMetaListByHour() - " + hour);
        MetaSaver saver = mMetaSaverManager.getMetaSaver(hour);
        saver.addMeta(meta.toString());
    }

    private void finishAllMetaSavers() {
        mMetaSaverManager.closeAllSavers();
    }
}

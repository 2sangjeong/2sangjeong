package com.lge.asr.splitter;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.ListPathUtil;

public class PcmSplitter extends Thread {

    private Logger mLogger;

    private String mTargetRegion;
    private String mTargetDate;
    private String mBasePath;
    private ArrayList<String> mPathList = new ArrayList<>();

    private ListPathUtil mListPathUtil;

    public PcmSplitter(String region, String date, Logger logger) {
        this.mTargetRegion = region;
        this.mTargetDate = date;
        this.mLogger = logger;
        mListPathUtil = new ListPathUtil(mLogger, mTargetRegion, mTargetDate);
    }

    public PcmSplitter(String region, String date) {
        this.mTargetRegion = region;
        this.mTargetDate = date;
        mLogger = CommonUtils.getLogger(CommonConsts.LOGGER_SPLITTER, region, date);

        mListPathUtil = new ListPathUtil(mLogger, mTargetRegion, mTargetDate);
    }

    @Override
    public void run() {
        String startMessage = String.format("[PcmSplitter][Start] [%s]-%s-", mTargetRegion, mTargetDate);
        mLogger.info(startMessage);
        ProgressNotifier.getInstance().setStatus(mLogger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_SPLITTER,
                ProgressNotifier.START);

        if (!mListPathUtil.readTargetList(mPathList)) {
            mBasePath = String.format("%s%s/success", CommonConsts.OUTPUT_PATH, mTargetRegion);
            mListPathUtil.setTargetPathForAllLanguage(mBasePath, mPathList);
        }
        System.out.println("====== PCM splitter :: " + mPathList.size() + " paths ======");

        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            for (String path : mPathList) {
                Runnable splitter = new SplitterRunnable(mLogger, path);
                executor.execute(splitter);
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
        } finally {
            String finishMessage = String.format("[PcmSplitter][Finish]. [%s]-%s-", mTargetRegion, mTargetDate);
            mLogger.info(finishMessage);
            ProgressNotifier.getInstance().setStatus(mLogger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_SPLITTER,
                    ProgressNotifier.FINISH);
        }
    }
}

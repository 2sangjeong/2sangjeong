package com.lge.asr.cleaner;

import com.lge.asr.cleaner.task.CleanerTaskCRNNFeature;
import com.lge.asr.cleaner.task.CleanerTaskOutput;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import org.apache.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Cleaner extends Thread {

    private Logger logger = CommonUtils.getCommonLogger();
    private String mBaseDate;

    public Cleaner(String baseDate) {
        this.mBaseDate = baseDate;
    }

    @Override
    public void run() {
        logger.info("[START] LoggingData Cleaner :: " + mBaseDate);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        Runnable cleanOutput = new CleanerTaskOutput(mBaseDate);
        Runnable cleanCrnnFeature = new CleanerTaskCRNNFeature(mBaseDate);

        executor.execute(cleanOutput);
        executor.execute(cleanCrnnFeature);

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        copyBgFullLog();
        logger.info("[FINISH] LoggingData Cleaner :: " + mBaseDate);
    }

    private void copyBgFullLog() {
        logger.info("copy background full log.");

        String year = mBaseDate.substring(0, 4);
        String month = mBaseDate.substring(4, 6);
        String day = mBaseDate.substring(6, 8);

        String destPath = String.format("%s/%s/%s/%s", CommonConsts.DATA_MANAGEMENT + "/logs/full-bg-log", year, month, day);
        CommonUtils.makeDirectory(logger, destPath);

        String cleanerLog = String.format("%s/cleaner.log", CommonConsts.DATA_MANAGEMENT);

        String cmd = String.format("cp %s %s/%s_cleaner.log", cleanerLog, destPath, mBaseDate);
        CommonUtils.shellCommand(logger, cmd, false, false);
    }
}

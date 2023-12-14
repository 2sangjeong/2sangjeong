package com.lge.asr.automation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.downloader.task.Downloader;
import com.lge.asr.downloader.utils.DownloaderUtils;
import com.lge.asr.extractor.task.LogDataProcessor;
import com.lge.asr.list_creator.task.LogDataListCreator;
import com.lge.asr.reporter.StatisticsReporter;
import com.lge.asr.reporter.task.Analyzer;
import com.lge.asr.splitter.PcmSplitter;

public class DailyAutomation extends Thread {

    private static Logger logger;

    private StatisticsReporter mReporter;
    private String mTargetRegion;
    private String mTargetDate;

    public DailyAutomation(String region, String date, StatisticsReporter reporter) {
        mTargetRegion = region;
        mTargetDate = date;
        mReporter = reporter;
        logger = CommonUtils.getLogger(CommonConsts.LOGGER_DAILY, mTargetRegion, mTargetDate);
    }

    @Override
    public void run() {
        startAutomation();
    }

    public void startAutomation() {
        logger.info("[0] start DailyAutomation");
        // STEP #1. Download
        if (DownloaderUtils.hasTargetDate(mTargetRegion, mTargetDate)) {
            logger.info("[1] Start Download");
            Downloader downloader = new Downloader(mTargetRegion, mTargetDate, CommonConsts.AWS_LOG_DATA_PATH);
            downloader.run();
            try {
                downloader.join();
            } catch (InterruptedException e) {
                logger.error("InterruptedException");
                CommonUtils.terminateSystem();
            }
            logger.info("[1-1] Finish Download");
        } else {
            logger.error(String.format("No Data for %s of the region %s", mTargetDate, mTargetRegion));
        }

        if (DownloaderUtils.hasTargetDateOnLocal(mTargetRegion, mTargetDate)) {

            // STEP #2. Create data list
            logger.info("[2] Start ListCreator");
            LogDataListCreator listCreator = new LogDataListCreator(mTargetDate, mTargetRegion);
            listCreator.run();
            try {
                listCreator.join();
            } catch (InterruptedException e) {
                logger.error("InterruptedException");
                CommonUtils.terminateSystem();
            }
            logger.info("[2-1] Finish ListCreator");

            // STEP #3. Extract
            logger.info("[3] Start Extractor");
            LogDataProcessor extractor = new LogDataProcessor(mTargetRegion, mTargetDate);
            extractor.run();
            try {
                extractor.join();
            } catch (InterruptedException e) {
                logger.error("InterruptedException");
                CommonUtils.terminateSystem();
            }
            logger.info("[3-1] Finish Extractor");

            // STEP #4. Statistics Analyzer
            logger.info("[4] Start DailyAnalyzer");
            ExecutorService executor = Executors.newFixedThreadPool(1);
            Runnable analyzer = new Analyzer(mTargetRegion, mTargetDate);
            executor.execute(analyzer);
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
            mReporter.reportAnalyzedResult(mTargetRegion);
            logger.info("[4-1] Finish DailyAnalyzer");

            logger.info("[5] Start PCM Splitter");
            PcmSplitter splitter = new PcmSplitter(mTargetRegion, mTargetDate);
            splitter.run();
            try {
                splitter.join();
            } catch (InterruptedException e) {
                logger.error("InterruptedException");
                CommonUtils.terminateSystem();
            }
            logger.info("[5] Finish PCM Splitter");
        } else {
            logger.error(String.format("No Data for %s of the region %s", mTargetDate, mTargetRegion));
        }
    }

}

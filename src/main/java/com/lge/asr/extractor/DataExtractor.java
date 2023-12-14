package com.lge.asr.extractor;

import org.apache.log4j.Logger;

import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.extractor.task.LogDataProcessor;

public class DataExtractor {

    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_COMMON);

    private String mTargetDate;

    public DataExtractor(String date) {
        mTargetDate = date;
        CommonUtils.setLogFileName(logger, CommonConsts.LOGGER_COMMON, "common", mTargetDate);
        ProgressNotifier.getInstance().setFinalStep(ProgressNotifier.PROCESS_EXTRACTOR);
    }

    public void runExtractor() {
        for (String region : CommonConsts.REGIONS) {
            logger.info("[START] Extractor for " + region);
            LogDataProcessor extractor = new LogDataProcessor(region, mTargetDate);
            extractor.start();
        }
    }
}

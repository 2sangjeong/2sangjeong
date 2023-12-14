package com.lge.asr.list_creator;

import org.apache.log4j.Logger;

import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.downloader.utils.DownloaderUtils;
import com.lge.asr.list_creator.task.LogDataListCreator;

public class ListCreator {

    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_COMMON);

    private String mTargetDate;

    public ListCreator(String date) {
        super();
        mTargetDate = date;
        CommonUtils.setLogFileName(logger, CommonConsts.LOGGER_COMMON, "common", mTargetDate);
        ProgressNotifier.getInstance().setFinalStep(ProgressNotifier.PROCESS_LIST_CREATOR);
    }

    public void createDataList() {
        for (String region : CommonConsts.REGIONS) {
            if (DownloaderUtils.hasTargetDateOnLocal(region, mTargetDate)) {
                LogDataListCreator listCreator = new LogDataListCreator(mTargetDate, region);
                listCreator.start();

                try {
                    listCreator.join();
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }

            } else {
                logger.info(String.format("No Data for %s of the region %s", mTargetDate, region));
            }
        }
    }
}

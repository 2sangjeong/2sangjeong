package com.lge.asr.downloader;

import org.apache.log4j.Logger;

import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.downloader.task.Downloader;
import com.lge.asr.downloader.utils.DownloaderUtils;

public class AwsDownloader {

    private final Logger mLogger = Logger.getLogger(CommonConsts.LOGGER_COMMON);

    private String mTargetDate;
    private String mDownloadPath;

    public AwsDownloader(final String date) {
        super();
        mTargetDate = date;
        mDownloadPath = CommonConsts.AWS_LOG_DATA_PATH;
        CommonUtils.setLogFileName(mLogger, CommonConsts.LOGGER_COMMON, "common", mTargetDate);
        ProgressNotifier.getInstance().setFinalStep(ProgressNotifier.PROCESS_DOWNLOADER);
    }

    public void downloadData() {
        for (String region : CommonConsts.REGIONS) {
            if (DownloaderUtils.hasTargetDate(region, mTargetDate)) {
                Downloader downloader = new Downloader(region, mTargetDate, mDownloadPath);
                downloader.start();
            } else {
                mLogger.info(String.format("No Data for %s of the region %s", mTargetDate, region));
            }
        }
    }
}

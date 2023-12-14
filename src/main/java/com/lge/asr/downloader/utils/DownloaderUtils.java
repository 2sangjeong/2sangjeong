package com.lge.asr.downloader.utils;

import org.apache.log4j.Logger;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.TextUtils;

public class DownloaderUtils {

    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_DOWNLOADER);

    private static final String mLoggingServerBucketPath_Seoul_Dev = "s3://an2-speech-dev";
    private static final String mLoggingServerBucketPath_Seoul_Prd = "s3://an2-speech-prd";
    private static final String mLoggingServerBucketPath_Oregon_Prd = "s3://uw2-speech-prd";
    private static final String mLoggingServerBucketPath_Airbot_Prd = "s3://apbva-logging-prd";

    public static String getAwsBucketAddr(String region) {
        String bucket = null;
        switch (region) {
            case CommonConsts.REGION_SEOUL_DEV:
                bucket = mLoggingServerBucketPath_Seoul_Dev;
                break;
            case CommonConsts.REGION_SEOUL_PRD:
                bucket = mLoggingServerBucketPath_Seoul_Prd;
                break;
            case CommonConsts.REGION_OREGON_PRD:
                bucket = mLoggingServerBucketPath_Oregon_Prd;
                break;
            case CommonConsts.REGION_AIRBOT_PRD:
                bucket = mLoggingServerBucketPath_Airbot_Prd;
                break;
            default:
                logger.error("Check arguments.");
                break;
        }
        return bucket;
    }

    public static boolean hasTargetDate(String region, String date) {
        // check s3 bucket.
        String bucketAddr = getAwsBucketAddr(region);
        if (!TextUtils.isEmpty(bucketAddr)) {
            String commandFormat = "aws s3 ls %s/logs/ | grep %s";
            if (region.equals("airbot_prd")) {
                commandFormat = "aws s3 ls %s/logs/ --profile apbva-logging-prd | grep %s";
            }

            String command = String.format(commandFormat, bucketAddr, date);
            String result = CommonUtils.shellCommand(logger, command);
            if (!TextUtils.isEmpty(result) && result.contains(date)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasTargetDateOnLocal(String region, String date) {
        // check VI Infra.
        String command = String.format("ls %s/%s/logs/ | grep %s", CommonUtils.removeSlash(CommonConsts.AWS_LOG_DATA_PATH), region, date);
        String result = CommonUtils.shellCommand(logger, command);

        logger.info(String.format("hasTargetDateOnLocal(), [%s][%s] result : ", region, date, result));

        if (!TextUtils.isEmpty(result) && result.contains(date)) {
            return true;
        }
        return false;
    }
}

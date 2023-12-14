package com.lge.asr.downloader.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.TextUtils;
import com.lge.asr.downloader.utils.DownloaderUtils;

public class Downloader extends Thread {

    private static Logger logger;

    private String mTargetRegion;
    private String mTargetDate;
    private String mDownloadPath;

    public Downloader(String region, String date, String downloadPath) {
        this.mTargetRegion = region;
        this.mTargetDate = date;
        this.mDownloadPath = String.format("%s/%s/logs/%s", downloadPath, region, date);
        logger = CommonUtils.getLogger(CommonConsts.LOGGER_DOWNLOADER, region, mTargetDate);
    }

    @Override
    public void run() {
        String startMessage = String.format("[Download][Start] [%s]-%s-", mTargetRegion, mTargetDate);
        logger.info(startMessage);
        ProgressNotifier.getInstance().setStatus(logger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_DOWNLOADER,
                ProgressNotifier.START);

        CommonUtils.makeDirectory(logger, mDownloadPath);

        String bucket = DownloaderUtils.getAwsBucketAddr(mTargetRegion);
        String commandFormat = "aws s3 sync %s/logs/%s %s";
        if (mTargetRegion.equals("airbot_prd")) {
            commandFormat = "aws s3 sync %s/logs/%s --profile apbva-logging-prd %s";
        }
        if (!TextUtils.isEmpty(bucket)) {
            try {
                shellCommand(logger, String.format(commandFormat, bucket, mTargetDate, mDownloadPath), true);
            } finally {
                String finishMessage = String.format("[Download][Finish]. [%s]-%s-", mTargetRegion, mTargetDate);
                logger.info(finishMessage);
                ProgressNotifier.getInstance().setStatus(logger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_DOWNLOADER,
                        ProgressNotifier.FINISH);
            }
        }
    }

    @SuppressWarnings("finally")
    public static String shellCommand(Logger logger, String command, boolean logging) {
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        BufferedReader successBufferReader = null;
        String msg = null;
        String result = null;

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("/bin/sh");
        cmdList.add("-c");
        cmdList.add(command);

        String[] array = cmdList.toArray(new String[cmdList.size()]);
        try {
            process = runtime.exec(array);
            if (logging) {
                successBufferReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((msg = successBufferReader.readLine()) != null) {
                    if(msg.startsWith("download")) {
                        logger.info(msg);
                    }
                }
            }
            // wait for the process
            process.waitFor();

            if (process.exitValue() == 0) {
                logger.info("Success!");
            } else {
                logger.error("Unexpected quit.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("IOException");
        } catch (InterruptedException e) {
            e.printStackTrace();
            logger.error("InterruptedException");
        } finally {
            try {
                process.destroy();
                if (successBufferReader != null) {
                    successBufferReader.close();
                }
                result = "FINISH [" + command + "]";
            } catch (IOException e) {
                logger.error(e.getMessage());
                result = null;
            }
        }
        return result;
    }

}

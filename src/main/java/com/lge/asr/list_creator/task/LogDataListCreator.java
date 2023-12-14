package com.lge.asr.list_creator.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;

public class LogDataListCreator extends Thread {

    private String mTargetDate;
    private String mTargetRegion;
    String fileListPath;
    final String pattern = ".pcm";

    private static Logger logger;

    public LogDataListCreator(String date, String region) {
        this.mTargetDate = date;
        this.mTargetRegion = region;
        this.fileListPath = createFileListPath();
        logger = CommonUtils.getLogger(CommonConsts.LOGGER_LIST_CREATOR, mTargetRegion, mTargetDate);
        initListFile();
    }

    @Override
    public void run() {
        String startMessage = String.format("[ListCreator][Start] [%s]-%s-", mTargetRegion, mTargetDate);
        logger.info(startMessage);
        writeContent(startMessage);
        ProgressNotifier.getInstance().setStatus(logger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_LIST_CREATOR,
                ProgressNotifier.START);

        String resourcePath = String.format("%s/%s/logs/%s", CommonConsts.AWS_LOG_DATA_PATH, mTargetRegion, mTargetDate);
        subDirList(resourcePath);

        String finishMessage = String.format("[ListCreator][Finish] [%s]-%s-", mTargetRegion, mTargetDate);
        logger.info(finishMessage);
        writeContent(finishMessage);
        ProgressNotifier.getInstance().setStatus(logger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_LIST_CREATOR,
                ProgressNotifier.FINISH);
    }

    private void initListFile() {
        File file = new File(fileListPath);
        if (file.exists()) {
            boolean result = file.delete();
            logger.debug("initListFile >> " + (result ? "success" : "failed"));
        }
    }

    private String createFileListPath() {
        StringBuilder sb = new StringBuilder();
        sb.append(CommonConsts.LOG_DATA_LIST_PATH);
        sb.append("/");
        sb.append(mTargetRegion);
        CommonUtils.makeDirectory(logger, sb.toString());

        sb.append("/");
        sb.append(mTargetDate);
        sb.append(".list");
        return sb.toString();
    }

    public String getFileListPath() {
        return this.fileListPath;
    }

    private void subDirList(String source) {
        File dir = new File(source);
        File[] fileList = dir.listFiles();
        try {
            for (int i = 0; i < fileList.length; i++) {
                File file = fileList[i];
                if (file.isFile()) {
                    String file_name = file.getName();
                    if (file_name.endsWith(pattern)) {
                        logger.debug("Write path :: " + file.getAbsolutePath());
                        writeContent(file.getAbsolutePath());
                    }
                } else if (file.isDirectory()) {
                    logger.debug("sub dir :: " + file.getName());
                    subDirList(file.getCanonicalPath().toString());
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void writeContent(String content) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileListPath, true));

            bw.write(content);
            bw.newLine();

            bw.flush();
            bw.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}

package com.lge.asr.classifier.utils;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.TextUtils;


public class ClassifierUtils {

    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_CLASSIFIER);

    public static String createListFileDirectory(String outputPath) {
        String path = outputPath + "/list_file";
        CommonUtils.makeDirectory(logger, path);
        return path;
    }
    
    public static void setLogFileName(String fileName) {
        StringBuilder sb = new StringBuilder();
        sb.append("./logs/");
        sb = getOutputPathSB(fileName, sb);
        sb.append(CommonUtils.getDate(CommonConsts.DATE_UNIT.MIN, CommonUtils.today()));
        sb.append(".log");
        String logFileName = sb.toString();

        System.out.println("setLogFileName" + logFileName);

        Logger rootLogger = Logger.getRootLogger();
        Enumeration appenders = rootLogger.getAllAppenders();
        FileAppender fa = null;
        while (appenders.hasMoreElements()) {
            Appender currAppender = (Appender) appenders.nextElement();
            if (currAppender instanceof FileAppender) {
                fa = (FileAppender) currAppender;
            }
        }
        if (fa != null) {
            fa.setFile(logFileName);
            fa.activateOptions();
        } else { 
            logger.info("No File Appender found");
        }
    }

    public static String getDayListPath(String source) {
        File dir = new File(source);
        File[] fileList = dir.listFiles();
        String finalPath = "";
        try {
            for (int i = 0; i < fileList.length; i++) {
                File file = fileList[i];
                if (file.isDirectory()) {
                    logger.debug("sub dir :: " + file.getAbsolutePath());
                    if (!file.getAbsolutePath().endsWith("logs")) {
                        finalPath = getDayListPath(file.getCanonicalPath().toString());
                        break;
                    } else {
                        finalPath = file.getCanonicalPath().toString();
                        logger.debug("return day list path :: " + finalPath);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return finalPath;
    }
    
    public static StringBuilder getOutputPathSB(String outputPath, StringBuilder sb) {
        String[] path = outputPath.split("/");

        for (int i = 0; i < path.length; i++) {
            if (!TextUtils.isEmpty(path[i])) {
                sb.append(path[i]);
                if (i < path.length - 1) {
                    sb.append(".");
                }
            }
        }

        sb.append("_");
        return sb;
    }

    public static void cleanUpLogFile() {
        String logPath = "./logs/output.log";
        File logfile = new File(logPath);
        if (logfile.exists()) {
            String logfileToSave = logPath + "." + CommonUtils.getDate(CommonConsts.DATE_UNIT.MIN, CommonUtils.today());
            String command = String.format("mv %s %s", logPath, logfileToSave);
            logger.info("cleanUpLogFile :: " + logfileToSave);
            CommonUtils.shellCommand(logger, command);
        }
    }

    public static boolean hasValidData(String path) {
        String countPcm = CommonUtils.shellCommand(logger, String.format("ls %s | grep pcm | wc -l", path)).replaceAll("[^0-9]", "");
        String countMeta = CommonUtils.shellCommand(logger, String.format("ls %s | grep meta | wc -l", path)).replaceAll("[^0-9]", "");

        logger.info(String.format("hasValidData. PCM : %s , META : %s", countPcm, countMeta));

        boolean isValidForPcm = false;
        boolean isValidForMeta = false;
        try {
            isValidForPcm = Integer.parseInt(countPcm) > 0;
            isValidForMeta = Integer.parseInt(countMeta) > 0;
            logger.info(String.format("hasValidData. isValidForPcm : %s  , isValidForMeta : %s", isValidForPcm, isValidForMeta));
        } catch (NumberFormatException e) {
            // NOTHING TO DO
            return false;
        }
        return isValidForPcm && isValidForMeta;
    }
}

package com.lge.asr.classifier.task;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.lge.asr.classifier.parser.JSONParser;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.TextUtils;

public class ClassifierTask extends Thread {

    String listFile;
    String outputPath;
    private JSONParser mParser;

    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_CLASSIFIER);

    public ClassifierTask(String listFile, String outputPath) {
        this.listFile = listFile;
        this.outputPath = outputPath;
        mParser = JSONParser.getInstance(logger);
    }

    @Override
    public void run() {
        logger.info("[START] Classifier");

        BufferedReader bufferdReader = null;
        try {
            bufferdReader = new BufferedReader(new FileReader(listFile));

            String file;
            while ((file = bufferdReader.readLine()) != null) {
                if (file.endsWith(CommonConsts.EXTENSION_META)) {
                    readJSON(file);
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        } finally {
            if (bufferdReader != null) {
                try {
                    bufferdReader.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        logger.info("[END] Classifier");
    }

    private void readJSON(String file) {
        BufferedReader bufferdReader = null;
        try {
            bufferdReader = new BufferedReader(new FileReader(file));

            StringBuffer sb = new StringBuffer();
            String jsonBody = "";
            while ((jsonBody = bufferdReader.readLine()) != null) {
                sb.append(jsonBody);
            }

            jsonBody = sb.toString();
            if (!TextUtils.isEmpty(jsonBody)) {
                String appName = mParser.ParsingFromJsonString(jsonBody).getAppName();
                classify(file, appName);
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
        } finally {
            if (bufferdReader != null) {
                try {
                    bufferdReader.close();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
    }

    private void classify(String file, String appName) {
        if (TextUtils.isEmpty(appName)) {
            return;
        }

        int step = 0;

        String[] filePath = file.split("/");
        String finalDirectory = null;

        StringBuilder sb = new StringBuilder();
        sb.append(outputPath);

        for (int i = 0; i < filePath.length; i++) {
            if (step == 1 && filePath[i].equals(CommonConsts.LOGS)) {
                filePath[i] = appName;
                step++;
            }

            if (filePath[i].equals("seoul") || filePath[i].equals("tokyo") || filePath[i].equals("sydney")
                    || filePath[i].equals("oregon")) {
                step++;
            } else if (step == 0) {
                filePath[i] = "";
            }

            if (filePath[i].length() > 0) {
                sb.append("/");
                sb.append(filePath[i]);
            }

            if (i == filePath.length - 2) {
                finalDirectory = sb.toString();
            }
        }

        if (finalDirectory != null && finalDirectory.length() > 0) {
            CommonUtils.makeDirectory(logger, finalDirectory);
        }

        logger.debug(System.getProperty("line.separator") + "Classify. [To] " + appName);

        String command = "mv " + file + " " + finalDirectory + "/";
        CommonUtils.shellCommand(logger, command);

        if (file.endsWith(CommonConsts.EXTENSION_META)) {
            file = file.replace(CommonConsts.EXTENSION_META, ".pcm");
        }

        command = "mv " + file + " " + finalDirectory + "/";
        CommonUtils.shellCommand(logger, command);
    }
}

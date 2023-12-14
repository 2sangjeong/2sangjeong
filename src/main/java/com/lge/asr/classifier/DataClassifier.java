package com.lge.asr.classifier;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;

import com.lge.asr.classifier.task.ClassifierTask;
import com.lge.asr.classifier.task.MetaFileListExtractor;
import com.lge.asr.classifier.utils.ClassifierUtils;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.TextUtils;


public class DataClassifier {

    private static String rootDirectory;
    private static String outputPath;

    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_CLASSIFIER);

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Check arguments.");
            System.out.println("java -jar DataClassifier_v.xx.jar [input path] [output path]");
            CommonUtils.terminateSystem();
        }

        rootDirectory = args[0];
        outputPath = args[1];

        if (rootDirectory == null || rootDirectory.equals("") || outputPath == null || outputPath.equals("")) {
            CommonUtils.terminateSystem();
        }

        start();
    }
    
    private static void start() {
        if (!TextUtils.isEmpty(rootDirectory)) {
            String[] path = rootDirectory.split("/");
            for (int i = 0; i < path.length; i++) {
                if ("logs".equals(path[i]) && i == (path.length - 2)) {
                    runClassifier(rootDirectory);
                    return;
                }
            }
            runClassifierDayByDay();
        }
    }

    private static void runClassifierDayByDay() {
        String loglistpath = ClassifierUtils.getDayListPath(rootDirectory);
        if (!TextUtils.isEmpty(loglistpath)) {
            File path = new File(loglistpath);
            File[] dayList = path.listFiles();

            Arrays.sort(dayList);
            System.out.println("dayList length : " + dayList.length);

            for (File day : dayList) {
                String dayPath = day.getAbsolutePath();
                System.out.println();
                logger.info("runClassifierDayByDay >> " + dayPath);
                if (ClassifierUtils.hasValidData(dayPath)) {
                    runClassifier(dayPath);
                }
            }
        } else {
            System.out.println("LogListPath is empty");
        }
    }

    private static void runClassifier(String source) {
        ClassifierUtils.setLogFileName(source);
        CommonUtils.stampKoreanStandardTime(logger);

        logger.info(String.format("[START] [Classifier] java -jar DataClassifier_ver%s.jar %s %s",
                CommonConsts.VERSION, source, outputPath + System.getProperty("line.separator")));

        logger.info("STEP #0 :: init the output folder");
        String fileListOutoutPath = ClassifierUtils.createListFileDirectory(outputPath);

        logger.info("STEP #1 :: [READY] extracting file list");
        MetaFileListExtractor extractor = new MetaFileListExtractor(source, fileListOutoutPath);
        String fileListPath = extractor.getFileListPath();
        extractor.start();

        try {
            extractor.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

        logger.info("STEP #2 :: Start Classifier");
        ClassifierTask classifier = new ClassifierTask(fileListPath, outputPath);
        classifier.start();

        try {
            classifier.join();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

        logger.info("[FINISHED] Classifier");
        CommonUtils.stampKoreanStandardTime(logger);
    }
}

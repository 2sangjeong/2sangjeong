/*
 * Mobile Communication Company, LG ELECTRONICS INC., SEOUL, KOREA
 * Copyright(c) 2019 by LG Electronics Inc.
 *
 * All rights reserved. No part of this work may be reproduced, stored in a retrieval system,
 * or transmitted by any means without prior written
 * Permission of LG Electronics Inc.
 */

package com.lge.asr;

import com.lge.asr.automation.DailyAutomation;
import com.lge.asr.automation.HourAutomation;
import com.lge.asr.classifier.ClassifyNCompress;
import com.lge.asr.cleaner.Cleaner;
import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.TextUtils;
import com.lge.asr.crnn_classifier.CRNNClassifier;
import com.lge.asr.crnn_classifier.CRNNClassifierManager;
import com.lge.asr.csv_saver.CsvSaver;
import com.lge.asr.downloader.AwsDownloader;
import com.lge.asr.extractor.DataExtractor;
import com.lge.asr.list_creator.ListCreator;
import com.lge.asr.notUsed.MetaCsvExporter;
import com.lge.asr.reporter.StatisticsReporter;
import com.lge.asr.splitter.PcmSplitter;

import java.util.ArrayList;
import java.util.Scanner;

public class DataManagement {

    private static String mMode;
    private static String mTargetRegion;
    private static String mStart;
    private static String mEnd;
    private static String mManualTime;
    private static ArrayList<String> mDateList;

    public static void main(final String[] args) {
        if (args == null || args.length < 1) {
            getArguments();
        } else if (args.length == 1) {
            mMode = args[0];
            setYesterDay();
        } else if (args.length == 2) {
            mMode = args[0];
            mTargetRegion = args[1];
            setYesterDay();
        } else if (args.length >= 4) {
            mMode = args[0];
            mTargetRegion = args[1];
            mStart = args[2];
            mEnd = args[3];
            if (args.length >= 5) {
                if (args[4].length() == 2) {
                    mManualTime = args[4];
                } else {
                    CommonConsts.AWS_LOG_DATA_PATH = args[4];
                }
                if (args.length >= 6) {
                    CommonConsts.VI_LOG_DATA = args[5];
                }
            }
        }
        if (TextUtils.isEmpty(mMode) || TextUtils.isEmpty(mStart) || TextUtils.isEmpty(mEnd)) {
            System.out.println("Check arguments.");
            CommonUtils.terminateSystem();
        }

        CommonConsts.initTargetRegion(mTargetRegion);
        CommonConsts.initPath();
        mDateList = CommonUtils.getDateList(mStart, mEnd);
        System.out.println("Target Region :: " + CommonConsts.REGIONS);

        ProgressNotifier.getInstance().initStatus();
        startDataManagement();
    }

    private static void getArguments() {
        System.out.println("===== DataManageMentTool. v16:56 ====");
        System.out.println("[1] SELECT MODE");
        System.out.println("  1. Daily Automation   2. Manually");
        System.out.print("    >> ");

        Scanner scan = new Scanner(System.in);
        String input = scan.nextLine();

        if (!TextUtils.isEmpty(input)) {
            if (input.equals("1")) {
                mMode = input;
                setYesterDay();
            } else if (input.equals("2")) {
                System.out.println("Select Job");
                System.out.println("  1. Full   2. Downloader   3. ListCreator   4. Extractor   5. StatisticsAnalyzer   6. Splitter");
                System.out.print("    >> ");
                mMode = scan.nextLine();

                System.out.println("Select a Region");
                System.out.println("  all or all_without_oregon or [ seoul_dev | seoul_prd | oregon_prd | airbot_prd ]");
                System.out.print("    >> ");
                mTargetRegion = scan.nextLine();

                System.out.print("INPUT START DATE (yyyyMMdd) : ");
                mStart = scan.nextLine();
                System.out.print("INPUT END DATE (yyyyMMdd) : ");
                mEnd = scan.nextLine();
            }
        } else {
            System.out.println("Check arguments.");
            CommonUtils.terminateSystem();
        }

        scan.close();
    }

    private static void setYesterDay() {
        mStart = CommonUtils.getDate(CommonConsts.DATE_UNIT.DAY, CommonUtils.yesterday());
        mEnd = CommonUtils.getDate(CommonConsts.DATE_UNIT.DAY, CommonUtils.yesterday());
    }

    private static void startDataManagement() {
        if ("hour_automation".equals(mMode)) {
            runHourAutomation();
        } else if ("1".equals(mMode) || "full".equals(mMode)) {
            StatisticsReporter reporter = new StatisticsReporter(mDateList);
            for (String date : mDateList) {
                for (String region : CommonConsts.REGIONS) {
                    runDailyAutomation(region, date, reporter);
                }
            }
        } else if ("2".equals(mMode) || "downloader".equals(mMode)) {
            for (String date : mDateList) {
                runDownloader(date);
            }
        } else if ("3".equals(mMode) || "list_creator".equals(mMode)) {
            for (String date : mDateList) {
                runListCreator(date);
            }
        } else if ("4".equals(mMode) || "extractor".equals(mMode)) {
            // runExtractor(); // for manual mode
            for (String date : mDateList) {
                runExtractor(date);
            }
        } else if ("5".equals(mMode) || "reporter".equals(mMode)) {
            runReporter(mDateList);
        } else if ("6".equals(mMode) || "splitter".equals(mMode)) {
            runSplitter();
        } else if ("classify".equals(mMode)) {
            runClassification();
        } else if ("crnn_classifier".equals(mMode)) {
            runCRNNClassifier();
        } else if ("csv_saver".equals(mMode)) {
            runCsvSaver();
        } else if ("cleaner".equals(mMode)) {
            runCleaner();
        }

        else if ("test".equals(mMode)) {
        }
    }

    private static void runDailyAutomation(final String targetRegion, final String targetDate, final StatisticsReporter reporter) {
        DailyAutomation dailyAutomation = new DailyAutomation(targetRegion, targetDate, reporter);
        dailyAutomation.start();
    }

    private static void runHourAutomation() {
        ProgressNotifier.getInstance().setFinalStep(ProgressNotifier.PROCESS_HOUR_AUTOMATION);
        for (String region : CommonConsts.REGIONS) {
            HourAutomation hourAutomation = new HourAutomation(region);
            if (!TextUtils.isEmpty(mManualTime)) {
                for (String date : mDateList) {
                    hourAutomation.setManualMode(date, mManualTime);
                }
            }
            hourAutomation.start();
        }
    }

    private static void runDownloader(final String targetDate) {
        AwsDownloader downloader = new AwsDownloader(targetDate);
        downloader.downloadData();
    }

    private static void runListCreator(final String targetDate) {
        ListCreator listCreator = new ListCreator(targetDate);
        listCreator.createDataList();
    }

    private static void runExtractor(final String targetDate) {
    //private static void runExtractor() { // for manual mode
        DataExtractor extractor = new DataExtractor(targetDate);
        extractor.runExtractor();

        /* for manual mode
         * 
         * for (String date : mDateList) { DataExtractorDevManual extractor = new
         * DataExtractorDevManual(date); extractor.start(); try { extractor.join(); }
         * catch (InterruptedException e) { CommonUtils.terminateSystem(); } }
         */
    }

    private static void runReporter(final ArrayList<String> targetDate) {
        StatisticsReporter reporter = new StatisticsReporter(targetDate);
        reporter.runAnalyzer();
    }

    private static void runSplitter() {
        for (String date : mDateList) {
            for (String region : CommonConsts.REGIONS) {
                PcmSplitter splitter = new PcmSplitter(region, date);
                splitter.start();
            }
        }
    }

    private static void runCsvSaver() {
        for (String date : mDateList) {
            CsvSaver csvSaver = new CsvSaver(date);
            csvSaver.run();
            try {
                csvSaver.join();
            } catch (InterruptedException e) {
                CommonUtils.terminateSystem();
            }
        }
    }

    private static void runMetaCsvExporter() {
        for (String date : mDateList) {
            for (String region : CommonConsts.REGIONS) {
                MetaCsvExporter metaCsvExporter = new MetaCsvExporter(date, region);
                metaCsvExporter.start();
                try {
                    metaCsvExporter.join();
                } catch (InterruptedException e) {
                    CommonUtils.terminateSystem();
                }
            }
        }
    }

    private static void runClassification() {
        for (String date : mDateList) {
            for (String region : CommonConsts.REGIONS) {
                ClassifyNCompress classifier = new ClassifyNCompress(region, date);
                classifier.start();
                try {
                    classifier.join();
                } catch (InterruptedException e) {
                    CommonUtils.terminateSystem();
                }
            }
        }
    }

    private static void runCRNNClassifier() {
        ProgressNotifier.getInstance().setFirstStep(ProgressNotifier.PROCESS_CLASSIFIER);
        ProgressNotifier.getInstance().setFinalStep(ProgressNotifier.PROCESS_CLASSIFIER);
        for (String date : mDateList) {
            for (String region : CommonConsts.REGIONS) {
                CRNNClassifier crnnClassifier = new CRNNClassifier(region, date);
                CRNNClassifierManager.getInstance(region, date).runClassifier(crnnClassifier);
            }
        }
    }

    private static void runCleaner() {
        if (mDateList == null || mDateList.isEmpty()) {
            setYesterDay();
        }
        for (String date : mDateList) {
            Cleaner cleaner = new Cleaner(date);
            cleaner.run();
            try {
                cleaner.join();
            } catch (InterruptedException e) {
                CommonUtils.terminateSystem();
            }
        }
    }

}

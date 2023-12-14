package com.lge.asr.reporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.reporter.parser.AnalyzerParser;
import com.lge.asr.reporter.task.Analyzer;

public class StatisticsReporter {

    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_STATISTICS_REPORTER);

    private static StatisticsReporter sInstance;

    private static ArrayList<String> sDateList = new ArrayList<>();
    private static HashMap<String, Boolean> sDailyAutomationStatus = new HashMap<>();

    public StatisticsReporter(ArrayList<String> dateList) {
        sDateList = dateList;
        String date = dateList.get(0);
        if (dateList.size() > 1) {
            date = date + "-" + dateList.get(dateList.size() - 1);
        }
        init(date);
    }

    public StatisticsReporter(String date) {
        sDateList.add(date);
        init(date);
    }

    public static synchronized StatisticsReporter getInstance(String date) {
        if (sInstance == null) {
            sInstance = new StatisticsReporter(date);
        }
        return sInstance;
    }

    private void init(String date) {
        CommonUtils.setLogFileName(logger, CommonConsts.LOGGER_STATISTICS_REPORTER, "Reporter", date);
        CommonUtils.makeDirectory(logger, CommonConsts.REPORT_PATH);
        CommonUtils.makeDirectory(logger, String.format("%s/%s", CommonConsts.REPORT_JSON_PATH, date.substring(0, 4)));
    }

    public void runAnalyzer() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (String date : sDateList) {
            for (String region : CommonConsts.REGIONS) {
                Runnable analyzer = new Analyzer(region, date);
                executor.execute(analyzer);
            }
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        AnalyzerParser.getInstance().parsingReport(sDateList);
    }

    public void runDailyAnalyzer(String region) {
        sDailyAutomationStatus.put(region, true);
        if (isFinishAllRegions()) {
            runAnalyzer();
        }
    }

    private boolean isFinishAllRegions() {
        for (String region : CommonConsts.REGIONS) {
            if (!sDailyAutomationStatus.containsKey(region) || !sDailyAutomationStatus.get(region)) {
                return false;
            }
        }
        return true;
    }

    public void reportAnalyzedResult(String region) {
        sDailyAutomationStatus.put(region, true);
        if (isFinishAllRegions()) {
            AnalyzerParser.getInstance().parsingReport(sDateList);
        }
    }

    public void setManualDate(String date) {
        sDateList.clear();
        sDateList.add(date);
        init(date);
    }
}

package com.lge.asr.reporter;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.reporter.parser.CRNNParser;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public class CRNNReporter {

    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_STATISTICS_REPORTER);

    private static CRNNReporter sInstance;

    private static ArrayList<String> mDateList;
    private HashMap<String, Boolean> mDailyAutomationStatus;

    public CRNNReporter(ArrayList<String> dateList) {
        mDateList = dateList;
        String date = dateList.get(0);
        if (dateList.size() > 1) {
            date = date + "-" + dateList.get(dateList.size() - 1);
        }
        init(date);
    }

    public CRNNReporter(String date) {
        mDateList = new ArrayList<>();
        mDateList.add(date);
        init(date);
    }

    public static synchronized CRNNReporter getInstance(String date) {
        if (sInstance == null) {
            sInstance = new CRNNReporter(date);
        }
        return sInstance;
    }

    private void init(String date) {
        CommonUtils.setLogFileName(logger, CommonConsts.LOGGER_CLASSIFIER, "Reporter", date);

        mDailyAutomationStatus = new HashMap<>();
        for (String region : CommonConsts.REGIONS) {
            mDailyAutomationStatus.put(region, false);
        }
    }

    public boolean isFinishAllRegions() {
        for (String region : CommonConsts.REGIONS) {
            if (!mDailyAutomationStatus.get(region)) {
                return false;
            }
        }
        return true;
    }

    public void reportCRNNResult(String region) {
        mDailyAutomationStatus.put(region, true);
        if (isFinishAllRegions()) {
            CRNNParser.getInstance().parsingReport(mDateList);
        }
    }

    public void reportCRNNResult() {
        CRNNParser.getInstance().parsingReport(mDateList);
    }
}

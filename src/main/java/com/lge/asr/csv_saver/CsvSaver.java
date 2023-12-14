package com.lge.asr.csv_saver;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;

public class CsvSaver extends Thread {
    private String mTargetDate;

    public CsvSaver(String targetDate) {
        mTargetDate = targetDate;
    }

    @Override
    public void run() {
        String command = String.format("python3 %s %s --date=%s", CommonConsts.CSV_SAVER_PY, "csv_saver", mTargetDate);
        CommonUtils.shellCommand(CommonUtils.getLogger(CommonConsts.LOGGER_CSV_SAVER, "", mTargetDate), command, true, false);
    }
}

package com.lge.asr.notUsed;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.log4j.Logger;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.extractor.utils.MetaExporter;

public class MetaCsvExporter extends Thread {

    private String mTargetRegion;
    private String mTargetDate;

    private String mYear;
    private String mMonth;
    private String mDay;
    private String mTime;

    private Calendar mCalendar;
    private ArrayList<String> mTarFiles = new ArrayList<>();

    Logger logger = Logger.getLogger(CommonConsts.LOGGER_COMMON);

    public MetaCsvExporter(String date, String region) {
        mTargetDate = date;
        mTargetRegion = region;

        mTarFiles.clear();
        initTime();
    }

    @Override
    public void run() {
        for (int i = 0; i < 24; i++) {
            mTime = getTime(mCalendar);
            exportMeta(mTargetRegion, mTargetDate, mTime);
            mCalendar.add(Calendar.HOUR, 1);
        }
    }

    private void initTime() {
        mYear = mTargetDate.substring(0, 4);
        mMonth = mTargetDate.substring(4, 6);
        mDay = mTargetDate.substring(6, 8);
        mTime = "00";

        mCalendar = Calendar.getInstance();
        mCalendar.set(Calendar.YEAR, Integer.parseInt(mYear));
        mCalendar.set(Calendar.MONTH, Integer.parseInt(mMonth));
        mCalendar.set(Calendar.DATE, Integer.parseInt(mDay));
        mCalendar.set(Calendar.HOUR, Integer.parseInt(mTime));
    }

    private String getTime(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        return sdf.format(cal.getTime());
    }

    private void exportMeta(final String region, final String date, final String time) {
        System.out.println(String.format("exportMeta :: %s_%s_%s", region, date, time));
        Thread worker = new Thread() {
            @Override
            public void run() {
                MetaExporter exporter = new MetaExporter(region, date, time);
                exporter.exportMetaScv();
                uploadMeta(time, exporter.getCsvFileAbsolutePath());
            }
        };
        worker.start();
    }

    private void uploadMeta(String time, String filePath) {
        String basePath = String.format("%s/%s/%s/%s/%s/%s/", CommonUtils.removeSlash(CommonConsts.DATALAKE_BUCKET), mTargetRegion, mYear,
                mMonth, mDay, time);

        logger.debug("-uploadMeta- :: " + basePath);
        CommonUtils.shellCommand(logger, String.format("aws s3 cp %s %s", filePath, basePath), true, false);
    }
}

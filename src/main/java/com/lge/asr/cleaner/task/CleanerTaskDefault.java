package com.lge.asr.cleaner.task;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CleanerTaskDefault implements Runnable {
    Logger mLogger;

    String mBasePath;
    String mBaseDate;

    int mTargetPosition;
    List<String> mTargetPath = new ArrayList<>();

    CleanerTaskDefault(String baseDate) {
        mBaseDate = baseDate;
        mLogger = CommonUtils.getLogger(CommonConsts.LOGGER_CLEANER, "", mBaseDate);
    }

    @Override
    public void run() {
    }

    boolean isTargetDate(String baseDate, String targetDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        try {
            Date base = sdf.parse(baseDate);

            Calendar cal = Calendar.getInstance();
            cal.setTime(base);
            cal.add(Calendar.YEAR, -1);
            base = cal.getTime();

            Date target = sdf.parse(targetDate);

            return target.before(base);
        } catch (ParseException e) {
            e.getMessage();
        }
        return false;
    }

    boolean maybeDatePath(String[] path) {
        String dir = path[path.length - 1];
        return (dir.startsWith("201") || dir.startsWith("202"));
    }

    void deleteTargetPath(List<String> targetPath) {
        mLogger.info(String.format("deleteTargetPath :: start Cleaner for %d paths.", targetPath.size()));

        ExecutorService executor = Executors.newFixedThreadPool(15);
        for (String path : targetPath) {
            Runnable worker = new RunnableCleaner(mLogger, path);
            executor.execute(worker);
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }
    }

}

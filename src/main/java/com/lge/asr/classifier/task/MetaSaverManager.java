package com.lge.asr.classifier.task;

import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.lge.asr.common.utils.TextUtils;
import com.lge.asr.extractor.task.MetaSaver;

public class MetaSaverManager {

    private Logger mLogger;

    private String mTargetRegion;
    private String mTargetDate;

    private HashMap<String, MetaSaver> mMetaSaverMap;

    public MetaSaverManager(Logger logger, String region, String date) {
        mLogger = logger;
        mTargetRegion = region;
        mTargetDate = date;
        mMetaSaverMap = new HashMap<String, MetaSaver>();
    }

    public MetaSaver getMetaSaver(String hour) {
        if (TextUtils.isEmpty(hour)) {
            return null;
        }

        if (!mMetaSaverMap.containsKey(hour)) {
            createMetaSaver(hour);
        }

        return mMetaSaverMap.get(hour);
    }

    private void createMetaSaver(String hour) {
        MetaSaver metaSaver = new MetaSaver(mLogger, mTargetRegion, mTargetDate, hour);
        mMetaSaverMap.put(hour, metaSaver);
        metaSaver.startWithAppend(true);
    }

    public void closeAllSavers() {
        Iterator<String> keys = mMetaSaverMap.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            mMetaSaverMap.get(key).finish();
        }
    }

}

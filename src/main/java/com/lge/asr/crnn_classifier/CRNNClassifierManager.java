package com.lge.asr.crnn_classifier;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import org.apache.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

public class CRNNClassifierManager {

    private static CRNNClassifierManager sInstance;

    private static Logger logger;
    private LinkedBlockingQueue<CRNNClassifier> mClassifierQueue = new LinkedBlockingQueue<>();
    private boolean mIsOnRunning = false;

    public static synchronized CRNNClassifierManager getInstance(String date, String hour) {
        if (sInstance == null) {
            sInstance = new CRNNClassifierManager();
            logger = CommonUtils.getLogger(CommonConsts.LOGGER_CRNN_MGR, null, date + "-" + hour);
        }
        return sInstance;
    }

    public void runClassifier(CRNNClassifier classifier) {
        if (mIsOnRunning) {
            logger.info(String.format("runClassifier is running other classifier. %s is saved on next queue.", classifier.getRegion()));
            mClassifierQueue.add(classifier);
        } else {
            startClassifier(classifier);
        }
    }

    private void startClassifier(CRNNClassifier classifier) {
        if (classifier == null) {
            logger.error("classier is null!!");
            return;
        }
        mIsOnRunning = true;
        logger.info(String.format("runClassifier - %s start", classifier.getRegion()));

        classifier.start();
        try {
            classifier.join();
        } catch (InterruptedException e) {
            CommonUtils.terminateSystem();
        }

        logger.info(String.format("runClassifier - %s end", classifier.getRegion()));
        mIsOnRunning = false;

        runNext();
    }

    private void runNext() {
        CRNNClassifier nextClassifier = mClassifierQueue.poll();
        if (nextClassifier != null) {
            startClassifier(nextClassifier);
        }
    }

}

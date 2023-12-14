package com.lge.asr.extractor.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.TextUtils;

public class MetaSaver {

    private Logger mLogger;

    private String mTargetRegion;
    private String mTargetDate;
    private String mHour;
    private String mMetaListFile;

    private boolean isFinishTagged = false;

    private LinkedBlockingQueue<String> mMetaSaveQueue = new LinkedBlockingQueue<String>();
    private Thread mMetaSaveThread = null;
    private BufferedWriter mBufferWriter;

    protected static final String FINAL = "FINAL";

    public MetaSaver(Logger logger, String region, String date, String hour) {
        mLogger = logger;
        mTargetRegion = region;
        mTargetDate = date;
        mHour = hour;
    }

    public void start() {
        startWithAppend(false);
    }

    public void startWithAppend(boolean append) {
        mLogger.debug("onStart");
        isFinishTagged = false;
        initMetaList(append);
        startMetaSaver();
    }

    public void addMeta(String metaJson) {
        // mLogger.debug("addMeta [Queue] : " + mMetaSaveQueue + " [meta] : " + metaJson);
        mMetaSaveQueue.add(metaJson);
    }

    public void finish() {
        mLogger.debug("onFinish");
        mMetaSaveQueue.add(FINAL);
    }

    private void startMetaSaver() {
        clear();
        mLogger.debug("startMetaSaver");

        mMetaSaveThread = new Thread(new Runnable() {
            public void run() {

                boolean isExitCondition = false;
                while (!isExitCondition) {
                    String message = null;
                    message = mMetaSaveQueue.poll();
                    isExitCondition = isOkToFinish(message);
                    if (!isExitCondition && !TextUtils.isEmpty(message)) {
                        writeMeta(message);
                    }
                }
                closeBufferWriter();
                clear();

            }
        });
        mMetaSaveThread.start();
    }

    private boolean isOkToFinish(String message) {
        if (FINAL.equals(message) || isFinishTagged) {
            if (mMetaSaveQueue.isEmpty()) {
                mLogger.debug("mMetaSaveQueue.isEmpty(). :: " + message);
                return true;
            } else {
                isFinishTagged = true;
            }
        }
        return false;
    }

    private void initMetaList(boolean append) {
        mMetaListFile = CommonUtils.getMetaListFilePath(mLogger, mTargetRegion, mTargetDate, mHour);
        File file = new File(mMetaListFile);
        if (!append && file.exists()) {
            boolean result = file.delete();
            mLogger.debug("initListFile >> " + (result ? "success" : "failed"));
        }

        try {
            mBufferWriter = new BufferedWriter(new FileWriter(mMetaListFile, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeMeta(String metaJson) {
        if (mBufferWriter != null) {
            try {
                //mLogger.debug("writeMeta()");
                mBufferWriter.write(metaJson);
                mBufferWriter.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeBufferWriter() {
        if (mBufferWriter != null) {
            try {
                mLogger.debug("closeBufferWriter()");
                mBufferWriter.flush();
                mBufferWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void clear() {
        if (mMetaSaveThread != null) {
            mMetaSaveThread.interrupt();
            mMetaSaveThread = null;
        }
        if (mMetaSaveQueue != null) {
            mMetaSaveQueue.clear();
        }
    }
}

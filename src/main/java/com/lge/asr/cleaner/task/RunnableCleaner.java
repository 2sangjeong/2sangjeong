package com.lge.asr.cleaner.task;

import com.lge.asr.common.utils.CommonUtils;
import org.apache.log4j.Logger;

public class RunnableCleaner implements Runnable {

    private Logger logger;
    private String targetPath;

    public RunnableCleaner(Logger logger, String path) {
        this.logger = logger;
        targetPath = path;
    }

    @Override
    public void run() {
        String command = String.format("rm -rf %s", targetPath);
        CommonUtils.shellCommand(logger, command, true, false);
    }
}

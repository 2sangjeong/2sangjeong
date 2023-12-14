package com.lge.asr.splitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.lge.asr.common.constants.CommonConsts;

public class SplitterRunnable implements Runnable {

    private Logger mLogger;
    private String mBasePath;

    public SplitterRunnable(Logger logger, String basePath) {
        mLogger = logger;
        mBasePath = basePath;
    }

    @Override
    public void run() {
        String command = String.format("python3 %s %s", CommonConsts.SPLITTER, mBasePath);
        mLogger.info("[Start] " + command);
        try {
            shellCommand(command);
        } finally {
            mLogger.info("[Finish] " + command);
        }
    }

    public void shellCommand(String command) {
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        BufferedReader successBufferReader = null;
        String msg = null;

        List<String> cmdList = new ArrayList<String>();
        cmdList.add("/bin/sh");
        cmdList.add("-c");
        cmdList.add(command);

        String[] array = cmdList.toArray(new String[cmdList.size()]);
        try {
            process = runtime.exec(array);
            successBufferReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((msg = successBufferReader.readLine()) != null) {
                mLogger.info(msg);
            }

            // wait for the process
            process.waitFor();

            if (process.exitValue() == 0) {
                mLogger.info("Success!");
            } else {
                mLogger.error("Unexpected quit.");
            }
        } catch (IOException e) {
            mLogger.error("IOException");
        } catch (InterruptedException e) {
            mLogger.error("InterruptedException");
        } finally {
            try {
                process.destroy();
                if (successBufferReader != null) {
                    successBufferReader.close();
                }
            } catch (IOException e) {
                mLogger.error(e.getMessage());
            }
        }
    }
}

package com.lge.asr.extractor.task;

import com.lge.asr.classifier.parser.JSONParser;
import com.lge.asr.common.utils.ListPathUtil;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import static com.lge.asr.extractor.task.MetaSaver.FINAL;

public class DeviceInfoSaver {

    private Logger mLogger;

    private String mTargetRegion;
    private String mTargetDate;
    private String mHour;

    private String mDeviceInfoJsonFile;

    private boolean isFinishTagged = false;

    private LinkedBlockingQueue<CachedInfo> mSaveQueue = new LinkedBlockingQueue<>();
    private HashMap<String, HashMap<String, String>> mDeviceInfoMap = new HashMap<>();

    private Thread mSaveThread = null;

    public DeviceInfoSaver(Logger logger, String region, String date, String hour) {
        mLogger = logger;
        mTargetRegion = region;
        mTargetDate = date;
        mHour = hour;
    }

    public void start() {
        mLogger.debug("onStart");
        isFinishTagged = false;
        readyToStart();
        startMetaSaver();
    }

    public void finish() {
        CachedInfo cachedInfo = new CachedInfo();
        cachedInfo.setDeviceId(FINAL);
        cachedInfo.setAppName(FINAL);
        mSaveQueue.add(cachedInfo);
    }

    private void readyToStart() {
        mDeviceInfoJsonFile = ListPathUtil.getDeviceInfoJson(mTargetRegion, mTargetDate);
        File file = new File(mDeviceInfoJsonFile);

        if (file.exists()) {
            if ("00".equals(mHour)) {
                boolean result = file.delete();
                mLogger.debug("initListFile >> " + (result ? "success" : "failed"));
            } /*else {
                JSONObject jsonObj = JSONParser.getInstance().getJsonObjectFromFilePath(mDeviceInfoJsonFile);
                if (jsonObj != null && !jsonObj.isEmpty()) {
                    Iterator<String> keySet = jsonObj.keySet().iterator();
                    while (keySet.hasNext()) {
                        String key = keySet.next();
                        if (jsonObj.get(key) instanceof HashMap) {
                            HashMap<String, String> value = (HashMap<String, String>) jsonObj.get(key);
                            mDeviceInfoMap.put(key, value);
                        }
                    }
                }
            }*/
        }
    }

    private void startMetaSaver() {
        clear();
        mLogger.debug("startMetaSaver");

        mSaveThread = new Thread(new Runnable() {
            public void run() {

                boolean isExitCondition = false;
                while (!isExitCondition) {
                    CachedInfo cachedInfo = mSaveQueue.poll();
                    if (cachedInfo != null) {
                        isExitCondition = isOkToFinish(cachedInfo);
                        if (!isExitCondition) {
                            addDeviceInfo(cachedInfo);
                        }
                    }
                }

                if (isExitCondition) {
                    mergeWithPreviousFile();
                    writeDeviceInfo();
                }

            }
        });
        mSaveThread.start();
    }

    private boolean isOkToFinish(CachedInfo cachedInfo) {
        if (FINAL.equals(cachedInfo.getAppName()) || isFinishTagged) {
            if (mSaveQueue.isEmpty()) {
                mLogger.debug("mSaveQueue is Empty.");
                return true;
            } else {
                isFinishTagged = true;
            }
        }
        return false;
    }

    public void saveMetaInfo(CachedInfo cachedInfo) {
        mSaveQueue.add(cachedInfo);
    }

    private void addDeviceInfo(CachedInfo cachedInfo) {
        String appName = cachedInfo.getAppName();
        String deviceId = cachedInfo.getDeviceId();

        int deviceCnt = 0;
        HashMap<String, String> devices = new HashMap<>();
        if (mDeviceInfoMap.containsKey(appName)) {
            devices = mDeviceInfoMap.get(appName);
            if (devices.containsKey(deviceId)) {
                deviceCnt = Integer.parseInt(devices.get(deviceId));
            }
        }
        deviceCnt++;
        devices.put(deviceId, String.valueOf(deviceCnt));

        mDeviceInfoMap.put(appName, devices);
    }

    private void mergeWithPreviousFile() {
        File file = new File(mDeviceInfoJsonFile);
        if (!file.exists()) {
            return;
        }

        mLogger.debug("mergeWithPreviousFile - start");

        HashMap<String, HashMap<String, String>> previousDevicesInfoMap = new HashMap<>();

        // #1 기존 파일의 device 정보를 parsing
        JSONObject jsonObj = JSONParser.getInstance(mLogger).getJsonObjectFromFilePath(mDeviceInfoJsonFile);
        if (jsonObj != null && !jsonObj.isEmpty()) {
            Iterator<String> keySet = jsonObj.keySet().iterator();
            while (keySet.hasNext()) {
                String key = keySet.next();
                if (jsonObj.get(key) instanceof HashMap) {
                    HashMap<String, String> value = (HashMap<String, String>) jsonObj.get(key);
                    previousDevicesInfoMap.put(key, value);
                }
            }
        }

        // #2 기존 device 정보에 새로 logging된 device 정보를 update
        Set<String> appNames = mDeviceInfoMap.keySet();
        for (String appName : appNames) {
            HashMap<String, String> newInformation = mDeviceInfoMap.get(appName);

            if (previousDevicesInfoMap.containsKey(appName)) {
                HashMap<String, String> previous = previousDevicesInfoMap.get(appName);

                Set<String> deviceIDs = newInformation.keySet();
                for (String deviceId : deviceIDs) {
                    if (previous.containsKey(deviceId)) {
                        int deviceCnt = Integer.parseInt(previous.get(deviceId)) + Integer.parseInt(newInformation.get(deviceId));
                        previous.put(deviceId, String.valueOf(deviceCnt));
                    } else {
                        previous.put(deviceId, newInformation.get(deviceId));
                    }
                }

                previousDevicesInfoMap.put(appName, previous);
            } else {
                previousDevicesInfoMap.put(appName, newInformation);
            }

        }

        mDeviceInfoMap = previousDevicesInfoMap;

        mLogger.debug("mergeWithPreviousFile - end");
    }

    private void writeDeviceInfo() {
        JSONObject jsonObject = new JSONObject();
        for (HashMap.Entry<String, HashMap<String, String>> entry : mDeviceInfoMap.entrySet()) {
            String key = entry.getKey();
            Object sub = entry.getValue();

            JSONObject subObject = new JSONObject();
            if (sub instanceof HashMap) {
                HashMap<String, String> subMap = (HashMap<String, String>) sub;
                for (HashMap.Entry<String, String> subEntry : subMap.entrySet()) {
                    String subEntryKey = subEntry.getKey();
                    Object subEntryObject = subEntry.getValue();
                    subObject.put(subEntryKey, subEntryObject);
                }
            }

            jsonObject.put(key, subObject);
        }

        File file = new File(mDeviceInfoJsonFile);
        if (file.exists()) {
            file.delete();
        }

        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));

            bufferedWriter.write(jsonObject.toJSONString());

            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clear() {
        if (mSaveThread != null) {
            mSaveThread.interrupt();
            mSaveThread = null;
        }
        if (mSaveQueue != null) {
            mSaveQueue.clear();
        }
    }
}

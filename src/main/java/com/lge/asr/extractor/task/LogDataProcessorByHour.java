/*
 * Mobile Communication Company, LG ELECTRONICS INC., SEOUL, KOREA
 * Copyright(c) 2019 by LG Electronics Inc.
 *
 * All rights reserved. No part of this work may be reproduced, stored in a retrieval system,
 * or transmitted by any means without prior written
 * Permission of LG Electronics Inc.
 */

package com.lge.asr.extractor.task;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.Counter;
import com.lge.asr.common.utils.ListPathUtil;
import com.lge.asr.extractor.dao.ExtractorDao;
import com.lge.asr.extractor.vo.Aws;
import com.lge.asr.extractor.vo.ResultVo;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class LogDataProcessorByHour extends Thread {

    private Logger mLogger;

    private String mTargetRegion;
    private String mTargetDate;
    private String mOutputPath;
    private String mDataPath;
    private String mTime;

    protected String condition = null;

    protected SqlSessionFactory sqlSessionFactory = null;
    private MetaSaver mMetaSaver;

    public LogDataProcessorByHour(String region, String date, String time, Logger logger) {
        mLogger = logger;
        initLogDataProcessorByHour(region, date, time);
    }

    public LogDataProcessorByHour(String region, String date, String time) {
        String loggerTime = date + "-" + time;
        mLogger = CommonUtils.getLogger(CommonConsts.LOGGER_EXTRACTOR, region, loggerTime);
        initLogDataProcessorByHour(region, date, time);
    }

    private void initLogDataProcessorByHour(String region, String date, String time) {
        mTargetRegion = region;
        mTargetDate = date;
        mTime = time;

        mOutputPath = CommonConsts.DATA_LAKE_WORKSPACE + "/" + mTargetDate + "/" + mTime + "/" + region;
        mDataPath = String.format("%s/%s/%s", CommonConsts.AWS_LOG_DATA_PATH + mTargetRegion, CommonConsts.LOGS, mTargetDate);

        mMetaSaver = new MetaSaver(mLogger, region, date, time);

        initSplitterPathList(region, mTargetDate);
    }

    @Override
    public void run() {
        try {
            File fNm = new File("config.xml");
            mLogger.debug("config.xml path :: " + fNm.getAbsolutePath());
            System.out.println("config.xml path :: " + fNm.getAbsolutePath());

            Reader reader = new FileReader(fNm);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, mTargetRegion);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (sqlSessionFactory == null) {
            System.out.println("sqlSessionFactory is null.");
            CommonUtils.terminateSystem();
        }

        mMetaSaver.start();

        /*condition = String.format(
                "and _year = '%s' and _month = '%s' and _day = '%s' and _hour = '%s' ORDER BY _minute desc",
                mTargetDate.substring(0, 4), mTargetDate.substring(4, 6), mTargetDate.substring(6, 8), mTime);*/

        DateFormat sdf = new SimpleDateFormat("yyyyMMdd_HH:mm:ss");
        try {
            Date start = sdf.parse(String.format("%s_%s:00:00", mTargetDate, mTime));
            mLogger.info("[jerome] start condition :: " + start.toString());

            Calendar cal = Calendar.getInstance();
            cal.setTime(start);
            cal.add(Calendar.HOUR, 1);
            cal.add(Calendar.SECOND, -1); // xx:00:00 ~ xx:59:59
            Date end = cal.getTime();

            long startTime = start.getTime();
            long endTime = end.getTime();

            condition = String.format("and savetime BETWEEN %s AND %s", startTime / 1000, endTime / 1000);
            mLogger.info("[jerome] condition :: " + condition);
        } catch (ParseException e) {
            mLogger.error(e.getMessage());
        }

        String startMessage = String.format("[LogDataProcess][Start] [%s]-%s-", mTargetRegion, mTargetDate);
        mLogger.info(startMessage);
        mLogger.info("DataPath :: " + mDataPath);

        ExtractorDao extractDAO = new ExtractorDao(sqlSessionFactory);
        Aws aws = extractDAO.selectAwsInfo();
        ObjectMapper mapper = JsonFactory.create();

        List<ResultVo> resultList = extractDAO.selectByCondition(condition);
        int resultSize = 0;
        if (resultList != null && !resultList.isEmpty()) {
            resultSize = resultList.size();

            mLogger.info("[LogDataProcess] " + resultSize + " data are queried.");
            ExecutorService executor = Executors.newFixedThreadPool(30);
            for (ResultVo result : resultList) {
                Runnable worker = new WorkThread2(extractDAO, aws, mapper, result, mOutputPath, mTargetRegion, mTargetDate, mLogger, mMetaSaver/*, mDeviceInfoSaver*/);
                executor.execute(worker);
            }
            executor.shutdown();
            while (!executor.isTerminated()) {
            }
        } else {
            mLogger.info("[LogDataProcess]-[FAIL] " + resultList);
        }

        mMetaSaver.finish();

        String finishMessage = String.format("[LogDataProcess][Finish] [%s]-%s- :: extracted ::  %s data.", mTargetRegion, mTargetDate, resultSize);
        Counter.getInstance().setDataCounter(mTargetRegion, resultSize);
        mLogger.info(CommonUtils.getLineSeparator());
        mLogger.info(finishMessage);
    }

    public void initSplitterPathList(String targetRegion, String targetDate) {
        String path = String.format("%s/%s", CommonConsts.SPLITTER_PATH_LIST, targetRegion);
        CommonUtils.makeDirectory(mLogger, path);
        path = ListPathUtil.getSplitterPathList(targetRegion, targetDate);

        File file = new File(path);
        if (file.exists()) {
            boolean result = file.delete();
            mLogger.debug("initMetaList >> " + (result ? "success" : "failed"));
        }
    }
}
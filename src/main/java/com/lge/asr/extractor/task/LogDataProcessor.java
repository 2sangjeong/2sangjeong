package com.lge.asr.extractor.task;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lge.asr.common.utils.ListPathUtil;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.log4j.Logger;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;

import com.lge.asr.common.ProgressNotifier;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.extractor.dao.ExtractorDao;

public class LogDataProcessor extends Thread {

    private Logger mLogger;

    private String mTargetRegion;
    private String mTargetDate;
    private String mOutputPath;
    private String mDataPath;

    private int mCounter = 0;

    protected SqlSessionFactory sqlSessionFactory = null;

    public LogDataProcessor(String region, String date) {
        mLogger = CommonUtils.getLogger(CommonConsts.LOGGER_EXTRACTOR, region, date);

        mTargetRegion = region;
        mTargetDate = date;
        mOutputPath = CommonConsts.OUTPUT_PATH + region;
        mDataPath = String.format("%s/%s/%s", CommonConsts.AWS_LOG_DATA_PATH + mTargetRegion, CommonConsts.LOGS, mTargetDate);
        initSplitterPathList(mTargetRegion, mTargetDate);
    }

    @Override
    public void run() {
        try {
            File fNm = new File("config.xml");
            mLogger.debug("config.xml path :: " + fNm.getAbsolutePath());
            Reader reader = new FileReader(fNm);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, mTargetRegion);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (sqlSessionFactory == null) {
            System.out.println("sqlSessionFactory is null.");
            CommonUtils.terminateSystem();
        }

        mLogger.info(">> sqlSessionFactory :: " + sqlSessionFactory.toString());

        String startMessage = String.format("[Extractor][Start] [%s]-%s-", mTargetRegion, mTargetDate);
        mLogger.info(startMessage);
        mLogger.info("DataPath :: " + mDataPath);
        ProgressNotifier.getInstance().setStatus(mLogger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_EXTRACTOR, ProgressNotifier.START);

        ExtractorDao extractDAO = new ExtractorDao(sqlSessionFactory);
        mLogger.info(">> extractDAO :: " + extractDAO.getTestInt());
        ObjectMapper mapper = JsonFactory.create();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        File pathDir = new File(mDataPath);
        if (pathDir.exists() && pathDir.isDirectory()) {

            File[] fileList = pathDir.listFiles();
            // for (int j = 0; j < fileList.length; j++) {
            for (int j = 0; j < 2; j++) {
                if (fileList[j].isFile()) {
                    Runnable worker = new WorkThread(mLogger, extractDAO, mapper, fileList[j], mOutputPath, mTargetRegion, mTargetDate);
                    executor.execute(worker);
                    mCounter++;
                }
            }
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        String finishMessage = String.format("[Extractor][Finish] [%s]-%s- :: extracted ::  %s data.", mTargetRegion, mTargetDate, mCounter);
        mLogger.info(CommonUtils.getLineSeparator());
        mLogger.info(finishMessage);
        ProgressNotifier.getInstance().setStatus(mLogger, mTargetDate, mTargetRegion, ProgressNotifier.PROCESS_EXTRACTOR, ProgressNotifier.FINISH);
        CommonUtils.stampKoreanStandardTime(mLogger);
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
package com.lge.asr.extractor.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.log4j.Logger;
import org.boon.json.ObjectMapper;

import com.lge.asr.extractor.dao.ExtractorDao;
import com.lge.asr.extractor.vo.ResultVo;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

public class WorkThread extends DefaultWorkThread {

    private File mSrcFile;

    private final int mDefaultIndex = 0;

    public WorkThread(Logger logger, ExtractorDao extractDAO, ObjectMapper mapper, File srcFile, String outputPath, String targetRegion,
            String targetDate) {
        super(logger, extractDAO, mapper, outputPath, targetRegion, targetDate);
        mSrcFile = srcFile;
    }

    @Override
    public void run() {
        String sql = " and logId = '" + mSrcFile.getName().substring(0, mSrcFile.getName().length() - 4) + "' ";
        // mLogger.info("sql :: " + sql);
        // mLogger.info("mExtractorDao ::" + mExtractorDao);

        mResultVo = (ResultVo)mExtractorDao.selectByCondition(sql).get(mDefaultIndex);
        // Dao, Vo, Dto
        // List<ResultVo> SbyCondition = mExtractorDao.selectByCondition(sql);
        // mLogger.info("SbyCondition ::" + SbyCondition);
        // mResultVo = (ResultVo)SbyCondition.get(mDefaultIndex);
        mLogger.info("mResultVo :: " + mResultVo);
        if (mResultVo == null) {
            mLogger.error("Result is null. for sql :: " + sql);
            mCachedInfo.setLoggedDate(mTargetDate);
            return;
        }
        mCachedInfo.setLogId(mResultVo.getLogId());
        mCachedInfo.setAppName(mResultVo.getAppName());

        JSONObject jsonObject = null;
        try {
            jsonObject = JSONObject.fromObject(mResultVo);
        } catch (JSONException e) {
            mLogger.error("Invalid Data. " + e.getMessage());
            return;
        }

        // 1. compose userdata
        composeUserData(jsonObject);
        // 2. compose result text
        boolean isSuccessDecryptResultText = composeResultText(jsonObject);
        // 3. save meta
        saveData(jsonObject, isSuccessDecryptResultText);
    }

    public void saveData(JSONObject jsonObject, boolean isSuccessDecryptResultText) {
        String result = EXTRACT_RESULT_FAIL;
        if (isSuccessDecryptResultText) {
            byte[] pcm = null;

            try {
                pcm = Files.readAllBytes(mSrcFile.toPath());
            } catch (IOException e) {
                pcm = null;
                mLogger.error("Invalid pcm Data. " + e.getMessage());
            }

            if (pcm != null) {
                result = savePcm(jsonObject, isSuccessDecryptResultText, pcm);
            }
        }
        saveMeta(jsonObject, result);
    }
}
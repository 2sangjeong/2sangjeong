package com.lge.asr.extractor.task;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import org.apache.log4j.Logger;
import org.boon.json.ObjectMapper;

import com.lge.asr.extractor.dao.ExtractorDao;
import com.lge.asr.extractor.utils.AmazonS3Manager;
import com.lge.asr.extractor.vo.Aws;
import com.lge.asr.extractor.vo.ResultVo;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class WorkThread2 extends DefaultWorkThread {

    private Aws mAws;
    private MetaSaver mMetaSaver;

    public WorkThread2(ExtractorDao extractDAO, Aws aws, ObjectMapper mapper, ResultVo resultVo, String outputPath, String targetRegion,
                       String targetDate, Logger logger, MetaSaver metaSaver) {
        super(logger, extractDAO, mapper, outputPath, targetRegion, targetDate);

        mAws = aws;
        mResultVo = resultVo;
        mMetaSaver = metaSaver;
    }

    @Override
    public void run() {
        if (mResultVo == null) {
            mLogger.error("Result is null");
            return;
        }

        mCachedInfo.setAppName(mResultVo.getAppName());

        JSONObject jsonObject = null;
        try {
            jsonObject = JSONObject.fromObject(mResultVo);
            mCachedInfo.setLogId((String) jsonObject.get("logId"));
        } catch (JSONException e) {
            mLogger.error("Invalid Data. " + e.getMessage());
            return;
        }

        // 1. compose user data
        composeUserData(jsonObject);
        if (isIgnoredProduct(mCachedInfo.getAppName())) {
            mLogger.debug(String.format("Ignore. AppName : %s", mCachedInfo.getAppName()));
            return;
        }
        // 2. compose result text
        boolean isSuccessDecryptResultText = composeResultText(jsonObject);
        // 3. save meta
        saveData(jsonObject, isSuccessDecryptResultText);
    }

    public void saveData(JSONObject jsonObject, boolean isSuccessDecryptResultText) {
        String result = EXTRACT_RESULT_FAIL;
        boolean isSkippedSavingPcm = isSkippedSavingPcm(mCachedInfo.getAppName(), mCachedInfo.getLanguage());
        if (isSkippedSavingPcm) {
            result = EXTRACT_RESULT_SUCCESS;
        } else if (isSuccessDecryptResultText) {
            byte[] pcm = null;
            String pcmData = (String) jsonObject.get("pcmData");
            pcm = downloadPcmFromAmazonS3(pcmData);
            if (pcm == null || pcm.length == 0) {
                pcm = downloadPcmFromS3Bucket(pcmData);
            }

            if (pcm == null || pcm.length == 0) {
                mLogger.info("[JEROME] saveData - fail to get pcm ... < " + pcmData);
            } else {
                deleteTempPcmFile(pcmData);
            }

            result = savePcm(jsonObject, isSuccessDecryptResultText, pcm);
        } else {
            mLogger.info(String.format("[JEROME] saveData < %s > - isSuccessDecryptResultText is False.", getServiceId(jsonObject)));
        }

        saveMeta(jsonObject, result);

        if (result.equals(EXTRACT_RESULT_SUCCESS) && !isSkippedSavingPcm) {
            mMetaSaver.addMeta(jsonObject.toString());
        }
    }

    private byte[] downloadPcmFromAmazonS3(String pcmData) {
        byte[] pcm = null;
        AmazonS3Manager s3Manager = AmazonS3Manager.getInstance();
        if (s3Manager.initialize(mAws)) {
            pcm = s3Manager.download(pcmData);
        } else {
            mLogger.info("S3 connection fail");
        }
        return pcm;
    }

    private byte[] downloadPcmFromS3Bucket(String pcmData) {
        mLogger.info("[JEROME] Try downloadPcmFromS3Bucket :: " + pcmData);

        byte[] pcm = null;

        String tempDownloadPath = CommonUtils.addSlash(CommonConsts.AWS_LOG_DATA_PATH) + mTargetRegion + "/logs/" + mTargetDate + "/";
        CommonUtils.makeDirectory(mLogger, tempDownloadPath);

        String command = String.format("aws s3 --profile=logging cp s3://%s/%s %s", getS3BucketName(), pcmData, tempDownloadPath);
        CommonUtils.shellCommand(mLogger, command, true, false);

        String tempPcmFile = CommonUtils.addSlash(CommonConsts.AWS_LOG_DATA_PATH) + mTargetRegion + "/" + pcmData;
        File pcmFile = new File(tempPcmFile);

        if (pcmFile.exists()) {
            try {
                pcm = Files.readAllBytes(pcmFile.toPath());
            } catch (IOException e) {
                pcm = null;
                mLogger.error("Invalid pcm Data. " + e.getMessage());
            }
        } else {
            mLogger.info("[JEROME] pcm file is not exists.  >> " + pcmData);
        }
        return pcm;
    }

    private void deleteTempPcmFile(String pcmData) {
        String tempPcmFile = CommonUtils.addSlash(CommonConsts.AWS_LOG_DATA_PATH) + mTargetRegion + "/" + pcmData;
        File pcmFile = new File(tempPcmFile);
        if (pcmFile.exists()) {
            boolean result = pcmFile.delete();
            mLogger.info("[JEROME] temp pcm file is deleted.  > " + result);
        }
    }

    private String getS3BucketName() {
        if (mTargetRegion.equalsIgnoreCase(CommonConsts.REGION_SEOUL_PRD)) {
            return "an2-speech-prd";
        } else if (mTargetRegion.equalsIgnoreCase(CommonConsts.REGION_SEOUL_DEV)) {
            return "an2-speech-dev";
        } else if (mTargetRegion.equalsIgnoreCase(CommonConsts.REGION_OREGON_PRD)) {
            return "uw2-speech-prd";
        }
        return "";
    }

}
package com.lge.asr.crnn_classifier;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.ListPathUtil;
import com.lge.asr.common.utils.TextUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.io.File;

public class ClassifierRunnable implements Runnable {
    private Logger mLogger;
    private String mTargetPath;
    private String mRegion;
    private String mAppName;
    private String mDate;
    private JSONObject mFinalResultJsonObject;

    private static final String STAGE_1_PY = "prepare_data_trn.py";
    private static final String STAGE_2_PY = "main_crnn_sed.py";
    private static final String STAGE_3_PY = "select-with-preds.py";

    public ClassifierRunnable(Logger logger, String targetPath, JSONObject resultJsonObj) {
        mLogger = logger;
        mTargetPath = targetPath;
        mFinalResultJsonObject = resultJsonObj;
        parseInfo(targetPath);
    }

    @Override
    public void run() {
        runExtractAndPackFeature();
        runClassifier();
        runSelectWithPreds();
        removeSplitResource();
    }

    // STAGE #1
    private void runExtractAndPackFeature() {
        mLogger.info("STAGE #1 :: runExtractAndPackFeature");
        String output_h5 = getOutputFileFullPath(CommonConsts.CRNN_CLASSIFIER_PACKED_FEATURES, "h5");
        File h5 = new File(output_h5);
        File listFile = new File(getOutputFileFullPath(CommonConsts.CRNN_CLASSIFIER_PACKED_FEATURES, "list"));

        if (h5.exists() && listFile.exists()) {
            mLogger.info("STAGE #1 :: Already done. skip this stage.");
            return;
        }

        CommonUtils.makeDirectory(mLogger, h5.getParentFile().getAbsolutePath());

        String command = String.format("/usr/bin/python %s/%s extract_n_pack_features --trn_path=\"%s\" --out_path=\"%s\" --list_path=\"%s\"",
                CommonConsts.CRNN_CLASSIFIER_PATH, STAGE_1_PY, mTargetPath, output_h5, getOutputFileFullPath(CommonConsts.CRNN_CLASSIFIER_PACKED_FEATURES, "list"));
        CommonUtils.shellCommand(mLogger, command, true, false);
    }

    // STAGE #2
    private void runClassifier() {
        mLogger.info("STAGE #2 :: runClassifier");
        String options = "THEANO_FLAGS=mode=FAST_RUN,device=gpu,floatX=float32";
        //options = ""; // no gpu, with cpu for multi task

        String te_hdf5 = getOutputFileFullPath(CommonConsts.CRNN_CLASSIFIER_PACKED_FEATURES, "h5");
        String model = CommonConsts.CRNN_CLASSIFIER_MODEL_SS_VA_2CLASS;

        String scalar_path = CommonConsts.CRNN_CLASSIFIER_SCALAR + model + "/training.scalar";
        String model_path = CommonConsts.CRNN_CLASSIFIER_MODELS + model;

        File output_csv_file = new File(getOutputFileFullPath(CommonConsts.CRNN_CLASSIFIER_PREDS, "csv.gz"));

        if (output_csv_file.exists()) {
            mLogger.info("STAGE #2 :: Already done. skip this stage.");
            return;
        }

        String output_path = output_csv_file.getParentFile().getAbsolutePath();
        String output_csv_file_name = output_csv_file.getName();

        CommonUtils.makeDirectory(mLogger, output_path);

        String command = String.format("%s /usr/bin/python %s/%s recognize --te_hdf5_path=\"%s\" --scalar_path=\"%s\" --model_dir=\"%s\" --out_dir=\"%s\" --out_csv_file=\"%s\"",
                options, CommonConsts.CRNN_CLASSIFIER_PATH, STAGE_2_PY, te_hdf5, scalar_path, model_path, output_path, output_csv_file_name);
        CommonUtils.shellCommand(mLogger, command, true, false);

        String unzip = String.format("gzip -d %s", output_csv_file);
        CommonUtils.shellCommand(mLogger, unzip, true, false);
    }

    // STAGE #3
    private void runSelectWithPreds() {
        File finalOutputFile = new File(getOutputFileFullPath(CommonConsts.CRNN_CLASSIFIER_OUTPUTS, "out"));
        String output_path = finalOutputFile.getParentFile().getAbsolutePath();
        CommonUtils.makeDirectory(mLogger, output_path);

        String csvFilePath = getOutputFileFullPath(CommonConsts.CRNN_CLASSIFIER_PREDS, "csv");
        String listFilePath = getOutputFileFullPath(CommonConsts.CRNN_CLASSIFIER_PACKED_FEATURES, "list");
        String outFilePath = getOutputFileFullPath(CommonConsts.CRNN_CLASSIFIER_OUTPUTS, "out");

        SelectWithPreds selectWithPreds = new SelectWithPreds(csvFilePath, listFilePath, outFilePath);
        boolean isSuccess = selectWithPreds.selectNormalSpeech();

        mLogger.info("STAGE #3 :: runSelectWithPreds :: " + isSuccess);
        if (isSuccess) {
            String resultKey = finalOutputFile.getName().replace(".out", "");
            mFinalResultJsonObject.put(resultKey, selectWithPreds.getResultJsonArr());
        }
    }

    private void removeSplitResource() {
        String asrPath = CommonUtils.removeSlash(mTargetPath) + "/asr";
        File asrDir = new File(asrPath);
        if (asrDir.exists()) {
            String cmdRemoveAsr = String.format("rm -rf %s/asr", mTargetPath);
            CommonUtils.shellCommand(mLogger, cmdRemoveAsr);
        }

        String kwsPath = CommonUtils.removeSlash(mTargetPath) + "/kws";
        File kwsDir = new File(kwsPath);
        if (kwsDir.exists()) {
            String cmdRemoveKws = String.format("rm -rf %s/kws", mTargetPath);
            CommonUtils.shellCommand(mLogger, cmdRemoveKws);
        }
    }

    private void parseInfo(String path) {
        String[] trn_paths = path.split("/");
        if (trn_paths.length >= 8) {
            mRegion = trn_paths[4];
            mAppName = trn_paths[6];
            mDate = trn_paths[8];
        }
    }

    public String getOutputFileFullPath(String stage, String extension) {
        if (!TextUtils.isEmpty(mRegion) && !TextUtils.isEmpty(mAppName) && !TextUtils.isEmpty(mDate)) {
            return ListPathUtil.getClassifierOutputFileFullPath(mRegion, mAppName, mDate, stage, extension);
        }
        return null;
    }
}

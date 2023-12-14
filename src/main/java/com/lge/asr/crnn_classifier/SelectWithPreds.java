package com.lge.asr.crnn_classifier;

import com.lge.asr.common.utils.TextUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;

public class SelectWithPreds {

    public static final String CRNN_KEY_TOTAL_SPEECH_CNT = "TotalSpeechCnt";
    public static final String CRNN_KEY_NORMAL_SPEECH_CNT = "NormalSpeechCnt";
    public static final String CRNN_KEY_NORMAL_PERCENTAGE = "NormalSpeechPercentage";

    private String mCsvFilePath;
    private String mListFilePath;
    private String mOutFilePath;

    private int mTotalSpeechCnt = 0;
    private int mNormalSpeechCnt = 0;

    public SelectWithPreds(String csvFilePath, String listFilePath, String outFilePath) {
        mCsvFilePath = csvFilePath;
        mListFilePath = listFilePath;
        mOutFilePath = outFilePath;

        initOutFile();
    }

    private void initOutFile() {
        File outFile = new File(mOutFilePath);
        if (outFile.exists()) {
            outFile.delete();
        }
    }

    public boolean selectNormalSpeech() {
        boolean isSuccess = false;
        File csvFile = new File(mCsvFilePath);
        if (csvFile.exists()) {
            try {
                FileReader filereader = new FileReader(csvFile);
                BufferedReader bufReader = new BufferedReader(filereader);

                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(mOutFilePath, true));

                String line = "";
                while ((line = bufReader.readLine()) != null) {
                    mTotalSpeechCnt++;
                    if (isNormalSpeech(line)) {
                        System.out.println(line);

                        mNormalSpeechCnt++;
                        bufferedWriter.write(line);
                        bufferedWriter.newLine();
                    }
                }

                bufferedWriter.flush();
                bufferedWriter.close();

                bufReader.close();

                isSuccess = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isSuccess;
    }

    private boolean isNormalSpeech(String line) {
        if (!TextUtils.isEmpty(line)) {
            String lineSplit = line.split("\t")[1];
            float prob_o = Float.parseFloat(lineSplit);
            return prob_o >= 0.6;
        }
        return false;
    }

    protected JSONArray getResultJsonArr() {
        JSONObject data = new JSONObject();
        data.put(CRNN_KEY_TOTAL_SPEECH_CNT, mTotalSpeechCnt);
        data.put(CRNN_KEY_NORMAL_SPEECH_CNT, mNormalSpeechCnt);

        String percentage = String.format("%.2f%%", (double) mNormalSpeechCnt / (double) mTotalSpeechCnt * 100.0);
        data.put(CRNN_KEY_NORMAL_PERCENTAGE, percentage);

        JSONArray resultArray = new JSONArray();
        resultArray.add(data);

        return resultArray;
    }
}


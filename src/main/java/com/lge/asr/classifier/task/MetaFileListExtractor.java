package com.lge.asr.classifier.task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.lge.asr.classifier.utils.ClassifierUtils;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;

public class MetaFileListExtractor extends Thread {

    String rootDirectory;
    String fileListPath;
    final String pattern = ".meta";

    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_CLASSIFIER);

    public MetaFileListExtractor(String directory, String fileListOutoutPath) {
        this.rootDirectory = directory;
        this.fileListPath = createFileListPath(fileListOutoutPath);
        initListFile();
    }

    @Override
    public void run() {
        logger.info("MetaFileListExtractor starts.");

        writeContent("[START] " + rootDirectory);

        subDirList(rootDirectory);

        writeContent("[END] " + rootDirectory);

        logger.info("MetaFileListExtractor is Done.");
    }

    private void initListFile() {
        File file = new File(fileListPath);
        if (file.exists()) {
            boolean result = file.delete();
            logger.debug("initListFile >> " + (result ? "success" : "failed"));
        }
    }

    private String createFileListPath(String fileListOutoutPath) {
        StringBuilder sb = new StringBuilder();
        sb.append(fileListOutoutPath);
        sb.append("/");
        sb = ClassifierUtils.getOutputPathSB(rootDirectory, sb);
        sb.append(CommonUtils.getDate(CommonConsts.DATE_UNIT.MIN, CommonUtils.today()));
        sb.append(".list");
        return sb.toString();
    }

    public String getFileListPath() {
        return this.fileListPath;
    }

    private void subDirList(String source) {
        File dir = new File(source);
        File[] fileList = dir.listFiles();
        try {
            for (int i = 0; i < fileList.length; i++) {
                File file = fileList[i];
                if (file.isFile()) {
                    String file_name = file.getName();
                    if (file_name.endsWith(pattern)) {
                        logger.debug("Write path :: " + file.getAbsolutePath());
                        writeContent(file.getAbsolutePath());
                    }
                } else if (file.isDirectory()) {
                    logger.debug("sub dir :: " + file.getName());
                    subDirList(file.getCanonicalPath().toString());
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void writeContent(String content) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileListPath, true));

            bw.write(content);
            bw.newLine();
            bw.flush();

            bw.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}

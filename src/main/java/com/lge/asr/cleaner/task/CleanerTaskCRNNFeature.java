package com.lge.asr.cleaner.task;

import com.lge.asr.cleaner.CleanerTargetPath;
import com.lge.asr.common.constants.CommonConsts;
import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

public class CleanerTaskCRNNFeature extends CleanerTaskDefault {

    public CleanerTaskCRNNFeature(String baseDate) {
        super(baseDate);

        super.mBasePath = CleanerTargetPath.CRNN_CLASSIFIER_PACKED_FEATURES;
        super.mTargetPosition = 7;
        if (CommonConsts.isTestMode) {
            super.mTargetPosition += 3;
        }
    }

    @Override
    public void run() {
        subDirList(mBasePath, mBaseDate);
        mLogger.info(String.format("CleanerCRNNOutput :: getTargetPath >> %d paths.", mTargetPath.size()));
        if (!mTargetPath.isEmpty()) {
            deleteTargetPath(mTargetPath);
        }
    }

    private void subDirList(String basePath, String baseDate) {
        mLogger.debug(String.format("subDirList << %s", basePath));

        File dir = new File(basePath);
        int pathLength = dir.getAbsolutePath().split("/").length;

        File[] fileList;
        if (pathLength == mTargetPosition + 1) {
            fileList = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String name) {
                    mLogger.debug(String.format("subDirList fileName :: %s", name));
                    mLogger.debug(String.format("subDirList fileName :: %s", file.getName()));
                    return name.endsWith(".h5");
                }
            });
        } else {
            fileList = dir.listFiles();
        }

        if (fileList == null) {
            return;
        }

        if (fileList.length > 1) {
            Arrays.sort(fileList, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        }

        try {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    if (pathLength <= mTargetPosition) {
                        subDirList(file.getCanonicalPath(), baseDate);
                    }
                } else if (file.isFile()) {
                    String fileDate = file.getName().substring(0, 8);
                    if (isTargetDate(baseDate, fileDate)) {
                        String abPath = file.getAbsolutePath();
                        mTargetPath.add(abPath);
                        mLogger.debug(String.format("subDirList add :: %s", abPath));
                    }
                }
            }
        } catch (IOException e) {
            mLogger.error(e.getMessage());
        }
    }
}

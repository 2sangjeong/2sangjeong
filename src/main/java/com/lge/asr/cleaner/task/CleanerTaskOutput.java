package com.lge.asr.cleaner.task;

import com.lge.asr.cleaner.CleanerTargetPath;
import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class CleanerTaskOutput extends CleanerTaskDefault {

    public CleanerTaskOutput(String baseDate) {
        super(baseDate);

        super.mBasePath = CleanerTargetPath.OUTPUT;
        super.mTargetPosition = 8;
        if (CommonConsts.isTestMode) {
            super.mTargetPosition += 3;
        }

        if (CommonConsts.REGIONS.length == 1) {
            super.mBasePath = CommonUtils.addSlash(mBasePath) + CommonConsts.REGIONS[0];
        }
    }

    @Override
    public void run() {
        subDirList(mBasePath, mBaseDate);
        mLogger.info(String.format("CleanerTaskOutput :: getTargetPath >> %d paths.", mTargetPath.size()));
        if (!mTargetPath.isEmpty()) {
            deleteTargetPath(mTargetPath);
        }
    }

    private void subDirList(String basePath, String baseDate) {
        File dir = new File(basePath);
        File[] fileList = dir.listFiles();

        if (fileList == null) {
            return;
        }

        if (fileList.length > 1) {
            Arrays.sort(fileList, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        }

        try {
            for (File directory : fileList) {
                String[] paths = directory.getAbsolutePath().split("/");
                if (directory.isDirectory()) {
                    if (paths.length <= mTargetPosition && !maybeDatePath(paths)) {
                        subDirList(directory.getCanonicalPath(), baseDate);
                    } else {
                        if (isTargetDate(baseDate, paths[paths.length - 1])) {
                            String abPath = directory.getAbsolutePath();
                            mTargetPath.add(abPath);
                            mLogger.debug(String.format("subDirList add :: %s", abPath));
                        }
                    }
                }
            }
        } catch (IOException e) {
            mLogger.error(e.getMessage());
        }
    }
}

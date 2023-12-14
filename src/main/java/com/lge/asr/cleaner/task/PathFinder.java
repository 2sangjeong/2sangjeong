package com.lge.asr.cleaner.task;

import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PathFinder {

    private Logger mLogger;

    private int mTargetPosition;
    private List<String> mTargetPath = new ArrayList<>();

    public PathFinder(Logger logger, int targetPosition) {
        mLogger = logger;
        mTargetPosition = targetPosition;
    }

    public List<String> getTargetPath(String basePath, String baseDate) {
        subDirList(basePath, baseDate);
        return mTargetPath;
    }

    private void subDirList(String basePath, String baseDate) {
        // mLogger.debug(String.format("subDirList << %s", basePath));
        File dir = new File(basePath);
        File[] fileList = dir.listFiles();

        if (fileList != null && fileList.length > 1) {
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

    private void subDirListForCrnnFeature(String basePath, String baseDate) {
        mLogger.debug(String.format("subDirList << %s", basePath));

        File dir = new File(basePath);
        int pathLength = dir.getAbsolutePath().split("/").length;

        File[] fileList;
        if (pathLength == mTargetPosition) {
            fileList = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String name) {
                    return file.isFile() && name.endsWith("h5");
                }
            });
        } else {
            fileList = dir.listFiles();
        }

        if (fileList != null && fileList.length > 1) {
            Arrays.sort(fileList, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        }

        try {
            for (File file : fileList) {
                if (file.isDirectory()) {
                    if (pathLength < mTargetPosition) {
                        subDirList(file.getCanonicalPath(), baseDate);
                    }
                } else if (file.isFile()) {
                    String fileDate = file.getName().substring(0,6);
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

    private boolean isTargetDate(String baseDate, String targetDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        try {
            Date base = sdf.parse(baseDate);

            Calendar cal = Calendar.getInstance();
            cal.setTime(base);
            cal.add(Calendar.YEAR, -1);
            base = cal.getTime();

            Date target = sdf.parse(targetDate);

            return target.before(base);
        } catch (ParseException e) {
            e.getMessage();
        }
        return false;
    }

    private boolean maybeDatePath(String[] path) {
        String dir = path[path.length - 1];
        return (dir.startsWith("201") || dir.startsWith("202"));
    }
}

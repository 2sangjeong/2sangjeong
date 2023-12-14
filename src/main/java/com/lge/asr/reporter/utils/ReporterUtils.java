package com.lge.asr.reporter.utils;

import com.lge.asr.common.utils.CommonUtils;
import org.apache.log4j.Logger;

import com.lge.asr.common.constants.CommonConsts;

import java.io.File;

public class ReporterUtils {

    public static String getJsonPath(Logger logger, String region, String day, String path, String name) {
        String year = day.substring(0, 4);
        String fileName = String.format("%s/%s/%s_%s_%s.json", path, year, day, name, region);
        File file = new File(fileName);
        CommonUtils.makeDirectory(logger, file.getParentFile().getAbsolutePath());
        return fileName;
    }

    public static String getJsonPath(Logger logger, String region, String day) {
        String year = day.substring(0, 4);
        String fileName;
        if (year.equalsIgnoreCase("2017")) {
            fileName = String.format("%s/%s/%s_report.json", CommonConsts.REPORT_JSON_PATH, year, day, region);
        } else {
            fileName = String.format("%s/%s/%s_report_%s.json", CommonConsts.REPORT_JSON_PATH, year, day, region);
        }
        return fileName;
    }
}

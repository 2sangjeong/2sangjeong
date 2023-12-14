package com.lge.asr.test;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.SlackWebHook;
import org.apache.log4j.Logger;

public class Test {

    public void run() {
        TestFailedMeta testFailedMeta = new TestFailedMeta();
        testFailedMeta.run();
    }

    private void testCrnnReport() {
        Logger logger = CommonUtils.getLogger(CommonConsts.LOGGER_COMMON, "", "");

        StringBuilder sb = new StringBuilder();
        sb.append("===== 이상발성 분류 결과 :: " + "20190703" + " =====");
        sb.append(CommonUtils.getLineSeparator());
        sb.append("한국어(ko-KR)에 대한 이상발성 Tagging 결과로, 오차가 있을 수 있습니다.\n< 정상발화 / 전체발화 >");
        sb.append(CommonUtils.getLineSeparator());

        sb.append("* com.lge.ha.dual18              :: 3 / 6    >>  60.00%");
        sb.append(CommonUtils.getLineSeparator());
        sb.append("* com.lge.ha.sigdryer27          :: 8 / 10    >>  80.00%");
        sb.append(CommonUtils.getLineSeparator());

        String finalResult = sb.toString();
        SlackWebHook slack = new SlackWebHook(finalResult);
        slack.sendSlack(logger, SlackWebHook.WEBHOOK_ASR_DATA_MANAGEMENT);
    }
}



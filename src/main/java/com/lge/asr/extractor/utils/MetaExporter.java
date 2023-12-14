package com.lge.asr.extractor.utils;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.common.utils.CommonUtils;

public class MetaExporter {
    private static Logger logger;

    private String mTargetRegion;
    private String mTargetDate;
    private String mTime;

    private String mYear;
    private String mMonth;
    private String mDay;

    private String mRdsHost;
    private String mPort;
    private String mDb;
    private String mUserName;
    private String mPassword;

    private String mCsvFile;

    private static final String SQL_QUERY = "mysql -h %s -u %s -p'%s' %s -P %s -e \"select * from tb_logging_data where _year='%s' and _month='%s' and _day='%s' and _hour='%s';\" | perl -F\"\\t\" -lane 'print join \",\", map {s/\"/\"\"/g; /^[\\d.]+$/ ? $_ : qq(\"$_\")} @F ' > %s";

    public MetaExporter(String region, String date, String time) {

        logger = CommonUtils.getLogger(CommonConsts.LOGGER_HOUR, region, date + "-" + time);

        this.mTargetRegion = region;
        this.mTargetDate = date;
        this.mTime = time;

        mYear = mTargetDate.substring(0, 4);
        mMonth = mTargetDate.substring(4, 6);
        mDay = mTargetDate.substring(6, 8);
    }

    public void exportMetaScv() {
        parsingConfiguration();
        // {%각_리전별_버킷이름%}/YYYY/mm/DD/HH/{%META_FILE_NAME%}.{%확장자%}
        String basePath = String.format("%s/%s/%s/%s/%s/%s/", CommonConsts.DATA_LAKE, mTargetRegion, mYear, mMonth, mDay, mTime);
        CommonUtils.makeDirectory(logger, basePath);
        mCsvFile = basePath + mTargetDate + "_" + mTime + "_" + mTargetRegion + ".csv";
        String query = String.format(SQL_QUERY, mRdsHost, mUserName, mPassword, mDb, mPort, mYear, mMonth, mDay, mTime, mCsvFile);
        CommonUtils.shellCommand(logger, query, true, false);
    }

    private void parsingConfiguration() {
        File config = new File("./config.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        Document doc = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(config);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
            return;
        }

        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("environment");
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)node;
                String region = element.getAttribute("id");
                if (mTargetRegion.equals(region)) {
                    NodeList regionNode = node.getChildNodes();
                    setParameter(regionNode, "url");
                    setParameter(regionNode, "username");
                    setParameter(regionNode, "password");
                }
            }
        }
    }

    private boolean setParameter(NodeList nList, String param) {
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            if (node.hasChildNodes()) {
                if (!setParameter(node.getChildNodes(), param)) {
                    continue;
                } else {
                    return true;
                }
            } else {
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element)node;
                    if (element.getAttribute("name").equals(param)) {
                        if (param.equals("url")) {
                            String url = element.getAttribute("value");
                            parsingUrl(url);
                        } else if (param.equals("username")) {
                            mUserName = element.getAttribute("value");
                        } else if (param.equals("password")) {
                            mPassword = element.getAttribute("value");
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void parsingUrl(String url) {
        String[] splitted = url.split(":|\\?");
        for (int i = 0; i < splitted.length; i++) {
            String host = splitted[i];
            if (host.startsWith("//rds")) {
                String[] portNdb = splitted[i + 1].split("/");
                mPort = portNdb[0];
                mDb = portNdb[1];
                mRdsHost = host.replace("//", "");
                break;
            }
        }
    }

    public String getCsvFileAbsolutePath() {
        return mCsvFile;
    }
}

package com.lge.asr.extractor.dao;

import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.log4j.Logger;

import com.lge.asr.common.constants.CommonConsts;
import com.lge.asr.extractor.vo.Aws;
import com.lge.asr.extractor.vo.ResultTextVo;
import com.lge.asr.extractor.vo.ResultVo;
import com.lge.asr.extractor.vo.TaggingDataVo;
import com.lge.asr.extractor.vo.UserDataVo;

public class ExtractorDao {
    private static final Logger logger = Logger.getLogger(CommonConsts.LOGGER_EXTRACTOR);

    private SqlSessionFactory sqlSessionFactory = null;
    private int test;

    public ExtractorDao(SqlSessionFactory sqlSessionFactory) {
        this.test = 4;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public int getTestInt(){
        return test;
    }
    /**
     * 2015.04.16, JIYEON, 태깅 데이터 가져오기
     * 
     * @param logId
     * @return
     */
    public TaggingDataVo selectTaggingData(String logId) {

        TaggingDataVo result = null;
        SqlSession session = sqlSessionFactory.openSession();

        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("logId", logId);

        try {
            result = session.selectOne("Result.selectTaggingData", hashMap);
        } catch (Exception e) {
            logger.debug("selectTaggingData Exception :" + e.getMessage());
        } finally {
            session.close();
        }

        return result;
    }

    // JIYEON, User Contact Data 가져오기
    public List<UserDataVo> selectUserDataByCondition(String condition) {

        List<UserDataVo> list = null;

        SqlSession session = sqlSessionFactory.openSession();

        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("condition", condition);

        try {
            list = session.selectList("Result.selectUserDataByCondition", hashMap);
        } catch (Exception e) {
            logger.debug("selectUserDataByCondition Exception :" + e.getMessage());
        } finally {
            session.close();
        }

        return list;
    }

    // 2015.06.18. JIYEON, logId 로 UserData 가져오기
    public UserDataVo selectUserData(String logId) {
        UserDataVo result = null;
        SqlSession session = sqlSessionFactory.openSession();

        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("logId", logId);

        try {
            result = session.selectOne("Result.selectUserData", hashMap);
        } catch (Exception e) {
            logger.debug("selectUserData Exception :" + e.getMessage());
        } finally {
            session.close();
        }

        return result;
    }

    public List<ResultVo> selectByCondition(String condition) {
        List<ResultVo> list = null;
        logger.error(">>condition: " + condition);
        SqlSession session = sqlSessionFactory.openSession();
        
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("condition", condition);

        try {
            list = session.selectList("Result.selectByConditionNew", hashMap);
            logger.error(">>list: " + list);
            //list = session.selectList("Result.selectByCondition", hashMap);
            //list = session.selectList("Result.selectByConditionTemp", hashMap);
        } catch (Exception e) {
            logger.debug("selectByCondition Exception :" + e.getMessage());
        } finally {
            session.close();
        }

        return list;
    }

    public List<ResultTextVo> selectResult(String logId) {
        List<ResultTextVo> list = null;
        SqlSession session = sqlSessionFactory.openSession();

        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("logId", logId);

        try {
            list = session.selectList("Result.selectResult", hashMap);
            //list = session.selectList("Result.selectResultTemp", hashMap);
        } catch (Exception e) {
            logger.debug("selectResult Exception :" + e.getMessage());
        } finally {
            session.close();
        }

        return list;
    }

    public Aws selectAwsInfo() {
        Aws aws = null;
        SqlSession session = sqlSessionFactory.openSession();

        try {
            aws = session.selectOne("Result.selectAwsInfo");
        } catch (Exception e) {
            logger.debug("selectAwsInfo Exception :" + e.getMessage());
        } finally {
            session.close();
        }

        return aws;
    }

}
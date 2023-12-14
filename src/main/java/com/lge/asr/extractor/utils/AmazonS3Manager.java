package com.lge.asr.extractor.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import com.lge.asr.common.utils.CommonUtils;
import com.lge.asr.common.utils.TextUtils;
import com.lge.asr.extractor.vo.Aws;

public class AmazonS3Manager {

    private AmazonS3 client = null;
    private AWSCredentials credentials = null;
    private String bucket = "";

    private static Logger sLogger = CommonUtils.getCommonLogger();

    private static class LazyHolder {
        private static final AmazonS3Manager INSTANCE = new AmazonS3Manager();
    }

    public static AmazonS3Manager getInstance() {
        return LazyHolder.INSTANCE;
    }

    public final AmazonS3 getClient() {
        return client;
    }

    public final void setClient(AmazonS3 client) {
        this.client = client;
    }

    public AWSCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(AWSCredentials credentials) {
        this.credentials = credentials;
    }

    @SuppressWarnings("deprecation")
    public boolean initialize(Aws aws) {
        if (client == null) {
            try {
                bucket = aws.getInputPath();
                sLogger.debug("s3 bucket : " + bucket);
                if (credentials == null) {
                    credentials = new BasicAWSCredentials(aws.getAccessKey(), aws.getSecretKey());
                }
                client = new AmazonS3Client(credentials);
            } catch (Exception e) {
                sLogger.debug(String.format("- exception occurred: %s", e.getMessage()));
                client = null;
            }
        }
        return client != null;
    }

    public boolean isInitialized() {
        if (getClient() != null) {
            return true;
        }
        return false;
    }

    public int upload(String logId, String key, String value) {
        if (!isInitialized()) {
            return -1;
        }

        if (key == null || key.isEmpty()) {
            return -2;
        }
        if (value == null || value.isEmpty()) {
            return -3;
        }
        if (logId == null || logId.isEmpty()) {
            return -4;
        }

        File f = null;
        try {
            f = createSampleFile(logId, value);
            PutObjectRequest putRequest = new PutObjectRequest(bucket, key, f);
            client.putObject(putRequest);
            sLogger.debug("s3 upload : " + key);
        } catch (Exception e) {
            sLogger.debug(String.format("- exception occurred: %s", e.getMessage()));
            client = null;
        } finally {
            if (f != null) {
                f.delete();
            }
        }
        return 0;
    }

    public final byte[] download(String key) {
        byte[] bytes = null;

        if (!isInitialized() || TextUtils.isEmpty(key)) {
            return bytes;
        }

        try {
            S3Object object = client.getObject(new GetObjectRequest(bucket, key));
            bytes = IOUtils.toByteArray(object.getObjectContent());
            sLogger.debug("s3 download : " + key);
        } catch (Exception e) {
            sLogger.debug(String.format("- exception occurred. file=%s, %s", key, e.getMessage()));
            client = null;
        }

        return bytes;
    }

    /**
     * Creates a temporary file with text data to demonstrate uploading a file to
     * Amazon S3
     *
     * @return A newly created temporary file with text data.
     * @throws IOException
     */
    private File createSampleFile(String path, String contents) throws IOException {
        File file = File.createTempFile(path, ".pcm");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        writer.write(contents);
        writer.close();

        return file;
    }
}

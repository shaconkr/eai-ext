package com.shacon;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shacon.hcis.EimsParser;

import kr.shacon.edi.type.MapDeserializer;

public class TestSuite {
    private static final Logger log = LoggerFactory.getLogger(TestSuite.class);
    private static final String gitPath[] = {"", "/eai-mci/", "/eai-int/", "/eai-ext/"};

    String eimsPath = "D:/HCIS/eai-repo/";
    String projPath = "D:/HCIS/";
    protected static Gson gson;
    private String url;
    String packageName;
    EimsParser eimsParser;
    String schemaPath;
    String resourcePath;
    String infraId;
    String ediSysId;

    public TestSuite(String eimsPath, String projPath) {
        this.eimsPath = eimsPath;
        this.projPath = projPath;
        this.eimsParser = new EimsParser(eimsPath);
    }
    
    static {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Map.class, new MapDeserializer())
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializeNulls()
                .create();
    }
    
	public void callTest(String ifId, String caseNo, String jsonString) {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(this.getUrl());
		HttpEntity stringEntity = new StringEntity(jsonString, ContentType.APPLICATION_JSON);
		httpPost.setEntity(stringEntity);
		httpPost.setHeader("ifId", ifId);
		httpPost.setHeader("testCase", caseNo);
		try {
			CloseableHttpResponse res = httpclient.execute(httpPost);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}

	
}

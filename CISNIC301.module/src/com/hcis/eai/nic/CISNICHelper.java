package com.hcis.eai.nic;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hcis.eai.common.HCISHelper;


public class CISNICHelper <N> extends HCISHelper<N> {
	
    private static final Logger log = LoggerFactory.getLogger(CISNICHelper.class);	
	
    @Override
    public InputStream getBeanXmlInputStream(String xmlpath) {
    	log.debug("load xml {}", xmlpath);
		return getClass().getResourceAsStream(xmlpath);
    }
    
    
	public byte[] testData() {		
		String testData = "TEST1234567890".repeat(500);
		return testData.getBytes();
	}

}
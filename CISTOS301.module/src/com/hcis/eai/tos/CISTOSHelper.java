package com.hcis.eai.tos;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hcis.eai.common.HCISHelper;

import kr.shacon.format.TibXsdParser;

public class CISTOSHelper<N> extends HCISHelper<N> {
	
    private static final Logger log = LoggerFactory.getLogger(CISTOSHelper.class);	
	
	@Override
    public String toEDI(String msgType, String jsonString, String encoding, String xml) throws Exception {
        String encodedString = new String(jsonString.getBytes(encoding), encoding);
        String interfaceId = getInterfaceId();
        CISTOSTransformer trans = new CISTOSTransformer(interfaceId);
        String streamName = (msgType.startsWith("_")) ? interfaceId + msgType : msgType;
        log.debug("## streamName [{}] xml [{}] msgType [{}] ", streamName, xml, msgType);
        String edi =  trans.toEDI(streamName, encodedString, encoding, xml);        
        return new String(setTotalLength(edi.getBytes(encoding), 8, 0, false, encoding),encoding); 
    }
	
	@Override	
    public String toJSON(String msgType, String ediString, String encoding, String xml) throws Exception {
        String interfaceId = getInterfaceId();
        CISTOSTransformer trans = new CISTOSTransformer(interfaceId);
        String streamName = (msgType.startsWith("_")) ? interfaceId + msgType : msgType;
        return new String(trans.toJSON(streamName, ediString, encoding, null).getBytes(encoding), encoding);
    }
	
	@Override
	public String logEDIBytes(byte[] bytes, String xsdpath) {
		InputStream is = getClass().getResourceAsStream(xsdpath);
		try {
			if(is.available() > 0 ) {
				log.debug("@@@ Found O {}", xsdpath);
			}else {
				log.debug("@@@ Not Found O {}", xsdpath);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		TibXsdParser parser = new TibXsdParser();
    	List<Map<String, Object>> layout = parser.parseXsdFromClasspath(is, "root");
    	StringBuffer sb = new StringBuffer();
    	int offset = 0;
    	logEDIItem(sb, layout, bytes, offset);    	
    	return sb.toString();
    }
}
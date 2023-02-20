package com.hcis.eai.kcb;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hcis.eai.common.HCISHelper;

import kr.shacon.format.TibXsdParser;


public class CISKCBHelper<N> extends HCISHelper<N> {
	
    private static final Logger log = LoggerFactory.getLogger(CISKCBHelper.class);	
	
	@Override
    public String toEDI(String msgType, String jsonString, String encoding, String xml) throws Exception {
        String encodedString = new String(jsonString.getBytes(encoding), encoding);
        String interfaceId = getInterfaceId();
        CISKCBTransformer trans = new CISKCBTransformer(interfaceId);
        String streamName = (msgType.startsWith("_")) ? interfaceId + msgType : msgType;
        log.debug("## streamName [{}] xml [{}] msgType [{}] ", streamName, xml, msgType);
        return trans.toEDI(streamName, encodedString, encoding, xml);        
    }
	
	@Override	
    public String toJSON(String msgType, String ediString, String encoding, String xml) throws Exception {
        String interfaceId = getInterfaceId();
        CISKCBTransformer trans = new CISKCBTransformer(interfaceId);
        String streamName = (msgType.startsWith("_")) ? interfaceId + msgType : msgType;
        return new String(trans.toJSON(streamName, ediString, encoding, null).getBytes(encoding), encoding);
    }
	
	@Override
	public String logEDIBytes(byte[] bytes, String xsdpath) {
		String ifId = getInterfaceId();
		String schemaPath = "/Schemas/" + ifId.substring(0,10) + "/" + ifId + "/" + ifId + ".";
		String xsdfile = xsdpath.startsWith("_") ? schemaPath + xsdpath : xsdpath;		
		InputStream is = getClass().getResourceAsStream(xsdfile);
		try {
			if(is.available() > 0 ) {
				log.debug("@@@ Found O {}", xsdfile);
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

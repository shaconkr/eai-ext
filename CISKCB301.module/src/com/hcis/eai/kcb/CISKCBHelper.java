package com.hcis.eai.kcb;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hcis.eai.common.HCISHelper;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;


public class CISKCBHelper<N> extends HCISHelper<N> {
	
    private static final Logger log = LoggerFactory.getLogger(CISKCBHelper.class);
	protected Object bytes3 = null;
	protected Object bytes4 = null;
	protected Object bytes5 = null;
	protected Object bytes = null;
	protected Object bytes1 = null;
	protected Object bytes2 = null;
	@Override
    public InputStream getBeanXmlInputStream(String xmlpath) {
    	log.debug("load xml {}", xmlpath);
		return getClass().getResourceAsStream(xmlpath);
    }
    
    public byte[] concatBytes3Bytes(byte[] bytes1, byte[] bytes2, byte[] bytes3) {
    	ByteBuf buf = Unpooled.buffer();
    	if(bytes1 != null && bytes1.length > 0) buf.writeBytes(bytes1);     
    	if(bytes2 != null && bytes2.length > 0) buf.writeBytes(bytes2);
    	if(bytes3 != null && bytes3.length > 0) buf.writeBytes(bytes3);
    	return ByteBufUtil.getBytes(buf);
    }
    
    public byte[] concatBytes5Bytes(byte[] bytes1, byte[] bytes2, byte[] bytes3, byte[] bytes4, byte[] bytes5) {
    	ByteBuf buf = Unpooled.buffer();
    	if(bytes1 != null && bytes1.length > 0) buf.writeBytes(bytes1);     
    	if(bytes2 != null && bytes2.length > 0) buf.writeBytes(bytes2);
    	if(bytes3 != null && bytes3.length > 0) buf.writeBytes(bytes3);
    	if(bytes4 != null && bytes4.length > 0) buf.writeBytes(bytes4);
    	if(bytes5 != null && bytes5.length > 0) buf.writeBytes(bytes5);
    	return ByteBufUtil.getBytes(buf);
    }
    
	public byte[] testData() {		
		String testData = "TEST1234567890".repeat(500);
		return testData.getBytes();
	}


}

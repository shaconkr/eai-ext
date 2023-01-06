package com.hcis.eai.ext.cis;

import java.util.Map;

import com.google.common.base.Joiner;
import com.hcis.eai.ext.EDIParserAndBuilder;

import kr.shacon.util.CastUtils;

public class LegacyHeaderParser extends EDIParserAndBuilder {

	public LegacyHeaderParser() {
		String xml = "/com/hcis/eai/ext/cis/LegacyHeader.xml";
		init(xml, "euc-kr");
	
	}

    /**
     * getHeader
     * @param bytes
     * @return  QueryString
     */
    public String getHeader(byte[] bytes, String recordName){
    	Map<String,String> head = CastUtils.cast((Map<?,?>)parseEDI(recordName, bytes));
//    	return  head.entrySet().stream().map(Object::toString).collect(Collectors.joining("&"));
    	return  Joiner.on("&").withKeyValueSeparator("=").join(head);       
    }
    
}

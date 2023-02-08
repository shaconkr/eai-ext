package com.hcis.eai.ext.nic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.hcis.eai.ext.EDIParserAndBuilder;

import kr.shacon.util.CastUtils;

public class NiceFileTransfer extends EDIParserAndBuilder {
	
	Map<String,Object> coFiles = Maps.newHashMap();
	String STAGE;
	
	
	public NiceFileTransfer() throws IOException {       
		coFiles.putAll(loadJson("/Resources/ext/nic/coFiles.json"));
	}
     
	
    public ArrayList<String> getCoFileList() {
		ArrayList<String> aList = new ArrayList<String>();
    	List<Map<String,Object>> lst = CastUtils.cast((List<?>) coFiles.get("coFileNames"));
		lst.stream().forEach(maps -> {
			maps.entrySet().forEach(map -> {
				Map<String,String> row = CastUtils.cast((Map<?,?>) map.getValue());
				String qStr = Joiner.on("&").withKeyValueSeparator("=").join(row);
				aList.add(qStr);					
			});
		});
		
    	return aList;
    } 

}

package com.hcis.eai.ext;

import org.junit.jupiter.api.Test;

import com.hcis.eai.common.HCISHelperAnc;

public class GenSchema {
	
    String eimsPath = "D:/HCIS/eai-repo/";
    String projPath = "D:/HCIS/";
	
	   @Test
	    public void prepareOne() throws Exception {
		   String reqIfId = "JNICISCTIC0030101";
		   String resIfId = "CISJNICTIC0030102";
	        String packageName = reqIfId.substring(0,10) + "/" + reqIfId;
	        String moduleName = reqIfId.substring(0,6) +  "301.module";
	    	String processName = packageName + "." + reqIfId + "_Sub";
		   
	        HCISHelperAnc helper = new HCISHelperAnc(eimsPath, projPath, moduleName);
	        helper.setProcessName(processName);
	        helper.prepare(reqIfId, resIfId);
	    }

}

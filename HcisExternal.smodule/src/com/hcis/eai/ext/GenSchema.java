package com.hcis.eai.ext;

import org.junit.jupiter.api.Test;

import com.hcis.eai.common.HCISHelperAnc;

public class GenSchema {
	
    String eimsPath = "D:/HCIS/eai-repo/";
    String projPath = "D:/HCIS/";
	
	   @Test
	    public void prepareOne() throws Exception {
	        HCISHelperAnc helper = new HCISHelperAnc(eimsPath, projPath, "NIBCIS301.module");
	        helper.setProcessName("CISTOSPMMZ.CISTOSPMMZ0140401.CISTOSPMMZ0140401_Sub");
	        helper.prepare("CISTOSPMMZ0140401", "TOSCISPMMZ0140402");
	    }


}

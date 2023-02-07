package com.hcis.eai.ext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.hcis.eai.common.HCISHelperAnc;

public class GenSchema {
	
	String eimsPath = "D:/HCIS/eai-repo/";
	String projPath = "D:/HCIS/";
	
	String reqIfId = "CISNICUPID0010601";   
	String resIfId = "NICCISUPID0010601"; 	
    
	String moduleName = reqIfId.substring(0,6) +  "301.module";
	
	String packageName = reqIfId.substring(0,10) + "/" + reqIfId;
	
   	String processName = packageName + "." + reqIfId + "_Sub";
   	
	   
	@Test
	public void buildSchemas() throws Exception {	  
        HCISHelperAnc helper = new HCISHelperAnc(eimsPath, projPath, moduleName);
        helper.setProcessName(processName);
        helper.prepare(reqIfId, resIfId);
	}
	   
	   
	@Test
	public void afterOne() throws IOException {
		String processPath = projPath + "eai-ext/" + moduleName + "/Processes";
		
		Path reqIf = Paths.get(processPath + "/" + reqIfId.substring(0,10) + "/" + reqIfId );
		Files.createDirectories(reqIf);
		
		Path repIf = Paths.get(processPath + "/" + resIfId.substring(0,10) + "/" + resIfId );
		Files.createDirectories(repIf);
		   
	}

}

package com.hcis.eai;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.shacon.hcis.DataFormatResourceBuilder;
import com.shacon.hcis.EimsParser;
import com.shacon.hcis.XsdSchemaBuilder;
import kr.shacon.edi.Transformer;
import kr.shacon.edi.util.CastUtils;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * HCIS Project
 * TIBCO Project 별 Schema 위치
 * Git-Repo / ~/eai-com , ~/eai-int, ~/eai-ext, ~/eai-mci
 * APP명 :  소스SysId(3) + 타겟sysId(3) + SEQ(2) + .application
 * AP모듈명 :  소스SysId(3) + 타겟sysId(3) + SEQ(2) + .module
 * Shared 모듈명 :  소스SysId(3) + 타겟sysId(3) + seq(1) + .smodule
 * 패키지명 : 소스 SysId(3) _ 소스업무(4) _ 타겟 SysId(3) _ 타겟업무(4)
 * Schema :  모듈명 /  schema / 패키지명
 * Resources : 모듈명 /  Resources / 패키지명
 *
 * @author choi hyoung ki
 */
public class HCISHelper {
    private static final Logger log = LoggerFactory.getLogger(HCISHelper.class);
    private static final String infra[] = {"", "/eai-mci/", "/eai-int/", "/eai-ext/"};
    String eimsPath;
    String projPath;

    public HCISHelper(String eimsPath, String projPath) {
        this.eimsPath = eimsPath;
        this.projPath = projPath;
    }

    public void buildXSD(String reqIfId, String resIfId) {
        XsdSchemaBuilder bld = new XsdSchemaBuilder(eimsPath, projPath);
        bld.createExtXSD(reqIfId, resIfId);
        bld.createIntXSD(reqIfId);
    }

    public void buildDFR(String reqIfId, String resIfId) throws DocumentException, IOException {
        DataFormatResourceBuilder bld = new DataFormatResourceBuilder(eimsPath, projPath);
        bld.buildTibRes(reqIfId, resIfId);
    }

    public String getInterfaceInfo(String reqIfId, String resIfId) throws Exception {
        Transformer transformer = new Transformer();
        return transformer.toJson(parseEIMS(reqIfId, resIfId));
    }

    private Map<String,Object> parseEIMS(String reqIfId, String resIfId) throws Exception {
        EimsParser parser = new EimsParser(eimsPath);
        return parser.parseXML(reqIfId, resIfId);
    }

    public String toEdiInt(String ifId, String jsonString) throws Exception {
        Transformer transformer = new Transformer();
        return transformer.toEDI(getIntPath(ifId), jsonString);
    }

    public String toEdiExt(String reqIfId, String resIfId, String jsonString) throws Exception {
        Transformer transformer = new Transformer();
        return transformer.toEDI(getExtPath(reqIfId, resIfId), jsonString);
    }
    public String toJsonInt(String ifId, String ediString) throws Exception {
        Transformer transformer = new Transformer();
        return transformer.toJSON(getIntPath(ifId), ediString);
    }

    public String toJsonExt(String reqIfId, String resIfId, String ediString) throws Exception {
        Transformer transformer = new Transformer();
        return transformer.toJSON(getExtPath(reqIfId, resIfId), ediString);
    }
    private String getIntPath(String reqIfId) throws Exception {
        return getXsdPath(parseEIMS(reqIfId, null));
    }

    private String getExtPath(String reqIfId, String resIfId) throws Exception {
        return getXsdPath(parseEIMS(reqIfId, resIfId));
    }

    private String getXsdPath(Map<String, Object> eaiInfo) throws Exception {
        Map<String, String> reqInfo = CastUtils.cast((Map<?, ?>) eaiInfo.get("request"));
        Map<String, String> source = CastUtils.cast((Map<?, ?>) eaiInfo.get("source"));
        Map<String, String> target = CastUtils.cast((Map<?, ?>) eaiInfo.get("target"));
        String packageName = Joiner.on("_").join(Lists.newArrayList(source.get("sysId"), source.get("workId"), target.get("sysId"), target.get("workId")));
        String moduleName = source.get("sysId") + target.get("sysId") + "01.module";
        String schemaPath = projPath + infra[Integer.parseInt(reqInfo.get("infraId"))] + moduleName + "/Schemas/" + packageName;
        return schemaPath;
    }
}

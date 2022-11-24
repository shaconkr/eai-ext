package com.shacon.edi;

import com.shacon.hcis.XsdSchemaBuilder;
import com.shacon.hcis.EimsParser;
import com.shacon.hcis.DataFormatResourceBuilder;
import io.netty.util.internal.StringUtil;
import kr.shacon.edi.Transformer;
import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class EDITransformTest {



    // EIMS 배포 위치 NAS    /hcisnas/eai_data/eims/latest
    String eimsPath = "D:/HCIS/eai-ext/shacon/test/resources/";

    // TIBCO Project 별 Schema 위치
    // Git-Repo / ~/eai-com , ~/eai-int, ~/eai-ext, ~/eai-mci
    // APP명 :  대상시스템 SysId(3) + SEQ(2) + .application
    // 모듈명 :  대상시스템 SysId(3) + SEQ(2) + .module
    // Shared 모듈명 :  대상시스템 SysId(3) + .smodule
    // 패키지명 :  대상시스템 SysId(3) / 업무(2) /  IfId(17)
    // Schema :  모듈명 /  schema / 패키지명
    // Resources : 모듈명 /  Resources / 패키지명

    //  /hcisnas/eai_data/ 1.eai-mci 2.eai-int 3.eai-ext / 대상SysId(3) + seq(2) / Schema , Resources / package /
    String projPath = "D:/HCIS/eai-ext/shacon/test/resources/";

    @Test
    public void xsdBuild() {
        XsdSchemaBuilder bld = new XsdSchemaBuilder(eimsPath, projPath);
        bld.createExtXSD("HCNCISCTJD0120103", "CISHCNCTJD0120104");
        bld.createIntXSD("CISTADAHFF0030102");
    }

    @Test
    public void buildTibDFRes() throws IOException, DocumentException {
        DataFormatResourceBuilder bld = new DataFormatResourceBuilder(eimsPath, projPath);
        bld.buildTibRes("HCNCISCTJD0120103", "CISHCNCTJD0120104");
        bld.buildTibRes("CISTADAHFF0030102", null);
    }

    private String utf8ToKsc5601(String s) throws UnsupportedEncodingException {
        return encodeCharset(s, "ksc5601");

    }

    private String ksc5601ToUtf8(String s) throws UnsupportedEncodingException {
        return encodeCharset(s, "utf-8");
    }

    private String encodeCharset(String s, String to) throws UnsupportedEncodingException {
        return new String(s.getBytes(to), to);
    }

    @Test
    public void transform() throws Exception {

        String word = "현대";
        System.out.println(word + " " + StringUtil.toHexString(word.getBytes(StandardCharsets.UTF_8)));
        System.out.println(word + " " + StringUtil.toHexString(word.getBytes()));

        String kor = new String(word.getBytes("euc-kr"), "euc-kr");
        System.out.println(kor + " " + StringUtil.toHexString(kor.getBytes("euc-kr")));
        System.out.println(kor + " " + StringUtil.toHexString(kor.getBytes()));

        String win = new String(word.getBytes("KSC5601"), "KSC5601");
        System.out.println(kor + " " + StringUtil.toHexString(win.getBytes("KSC5601")));
        System.out.println(kor + " " + StringUtil.toHexStringPadded(win.getBytes()));


        String eimsRootPath = "D:/HCIS/eai-ext/shacon/test/resources/HCNCISCTJD0120103_CISHCNCTJD0120104/";
        Transformer trans = new Transformer();

        String reqJsonUtf8 = Files.readString(Path.of(eimsRootPath + "HCNCISCTJD0120103_InReq.json"), StandardCharsets.UTF_8);
        String reqJsonKsc5601 = utf8ToKsc5601(reqJsonUtf8);

        String ediStr = trans.toEDI(eimsRootPath + "HCNCISCTJD0120103_OutReq.xsd", reqJsonKsc5601);
        Files.write(Paths.get(eimsRootPath + "HCNCISCTJD0120103_OutReq.edi"), ediStr.getBytes("euc-kr"));

        String edi = Files.readString(Path.of(eimsRootPath + "CISHCNCTJD0120104_OutRes.edi"));
        String jsonStr = trans.toJSON(eimsRootPath + "CISHCNCTJD0120104_InRes.xsd", edi);
        Files.write(Paths.get(eimsRootPath + "CISHCNCTJD0120104_InRes.json"), ksc5601ToUtf8(jsonStr).getBytes(StandardCharsets.UTF_8));

    }



}

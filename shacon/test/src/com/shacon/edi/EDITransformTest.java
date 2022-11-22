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


    @Test
    public void xsdBuild() {
        XsdSchemaBuilder bld = new XsdSchemaBuilder();
        bld.createExtXSD("HCNCISCTJD0120103", "CISHCNCTJD0120104");
        bld.createIntXSD("CISTADAHFF0030102");
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

    @Test
    public void buildTibDFRes() throws IOException, DocumentException {
        DataFormatResourceBuilder bld = new DataFormatResourceBuilder();
        bld.buildTibRes("HCNCISCTJD0120103", "CISHCNCTJD0120104");
        bld.buildTibRes("CISTADAHFF0030102", null);
    }

    @Test
    public void parseXml() throws DocumentException, IOException {
        EimsParser parser = new EimsParser();
        Map<String, Object> ret = parser.parseXML("HCNCISCTJD0120103", "CISHCNCTJD0120104");
        System.out.println(ret);
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



}

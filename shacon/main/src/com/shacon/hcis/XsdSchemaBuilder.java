package com.shacon.hcis;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.shacon.edi.type.MapDeserializer;
import kr.shacon.edi.util.CastUtils;
import org.apache.ws.commons.schema.*;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;

import javax.xml.namespace.QName;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static kr.shacon.edi.ShaConstants.*;
import static org.apache.ws.commons.schema.constants.Constants.URI_2001_SCHEMA_XSD;

/**
 * HCIS Project
 * <p>
 * EIMS Integration
 * <p>
 * EIMS 에서 등록된 전문정보로 고정길이전문 생성/파싱 용 XSD 생성
 * <p>
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
public class XsdSchemaBuilder {

    private static final Logger log = LoggerFactory.getLogger(XsdSchemaBuilder.class);
    private static final String infra[] = {"", "/eai-mci/", "/eai-int/", "/eai-ext/"};
    private static final String STD_HDR_NS = "http://schema.hcis.com/json";

    String targetNameSpace;
    String eimsPath;
    String projPath;
    String packageName;
    EimsParser eimsParser;
    String schemaPath;
    String resourcePath;

    protected static Gson gson;

    static {
        gson = new GsonBuilder()
                .registerTypeAdapter(Map.class, new MapDeserializer())
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializeNulls()
                .create();
    }

    public XsdSchemaBuilder(String eimsPath, String projPath) {
        this.eimsPath = eimsPath;
        this.projPath = projPath;
        this.eimsParser = new EimsParser(eimsPath);
    }

    /**
     * 대내용 XSD 생성 (DATA부 만 존재함) HCIS Header 추가?
     *
     * @param ifId
     */
    public void createIntXSD(String ifId) {
        try {
            Map<String, Object> eaiInfo = eimsParser.parseXML(ifId, null);
            log.debug(gson.toJson(eaiInfo));

            Map<String, String> reqInfo = CastUtils.cast((Map<?, ?>) eaiInfo.get("request"));
            Map<String, String> source = CastUtils.cast((Map<?, ?>) eaiInfo.get("source"));
            Map<String, String> target = CastUtils.cast((Map<?, ?>) eaiInfo.get("target"));

            packageName = Joiner.on("_").join(Lists.newArrayList(source.get("sysId"), source.get("workId"), target.get("sysId"), target.get("workId")));
            String moduleName = source.get("sysId") + target.get("sysId") + "01.module";
            schemaPath = projPath + infra[Integer.parseInt(reqInfo.get("infraId"))] + moduleName + "/Schemas/" + packageName;
            resourcePath = projPath + infra[Integer.parseInt(reqInfo.get("infraId"))] + moduleName + "/Resources/" + packageName;

            if (reqInfo.get("infraId").equals("2")) {  // 대내
                if (source.get("sysId").equals("TAD")) {
                    buildIntXsd(ifId, IN_REQ);
                    buildIntXsd(ifId, IN_RES);
                } else {
                    buildIntXsdforHcis(ifId, IN_REQ);
                    buildIntXsdforHcis(ifId, IN_RES);
                }
                if (target.get("sysId").equals("TAD")) {
                    buildIntXsd(ifId, OUT_REQ);
                    buildIntXsd(ifId, OUT_RES);
                } else {
                    buildIntXsdforHcis(ifId, OUT_REQ);
                    buildIntXsdforHcis(ifId, OUT_RES);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XmlSchemaSerializer.XmlSchemaSerializerException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 대외용 XSD 생성
     *
     * @param reqIfId 요청IFID
     * @param resIfId 응답IFID
     */
    public void createExtXSD(String reqIfId, String resIfId) {
        String folder = (resIfId != null) ? "/" + reqIfId + "_" + resIfId : "/" + reqIfId;
        try {
            Map<String, Object> eaiInfo = eimsParser.parseXML(reqIfId, resIfId);
            log.debug(gson.toJson(eaiInfo));
            Map<String, String> reqInfo = CastUtils.cast((Map<?, ?>) eaiInfo.get("request"));
            Map<String, String> source = CastUtils.cast((Map<?, ?>) eaiInfo.get("source"));
            Map<String, String> target = CastUtils.cast((Map<?, ?>) eaiInfo.get("target"));

            String moduleName = source.get("sysId") + target.get("sysId") + "01.module";

            packageName = Joiner.on("_").join(Lists.newArrayList(source.get("sysId"), source.get("workId"), target.get("sysId"), target.get("workId")));
            schemaPath = projPath + infra[Integer.parseInt(reqInfo.get("infraId"))] + moduleName + "/Schemas/" + packageName;
            resourcePath = projPath + infra[Integer.parseInt(reqInfo.get("infraId"))] + moduleName + "/Resources/" + packageName;
            String triggerSystem = reqInfo.get("extHtdspId");  // 1.당발 2. 타발

            if (triggerSystem.equals("1")) {
                buildExtXsd(folder, reqIfId, OUT_REQ);
                buildExtXsdforHcis(folder, reqIfId, IN_REQ);
                if (resIfId != null) {
                    buildExtXsd(folder, resIfId, OUT_RES);
                    buildExtXsdforHcis(folder, resIfId, IN_RES);
                }
            } else {
                buildExtXsd(folder, reqIfId, IN_REQ);
                buildExtXsdforHcis(folder, reqIfId, OUT_REQ);
                if (resIfId != null) {

                    buildExtXsd(folder, resIfId, IN_RES);
                    buildExtXsdforHcis(folder, resIfId, OUT_RES);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildIntXsd(String ifId, String direction) throws IOException, XmlSchemaSerializer.XmlSchemaSerializerException {
        targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;
        String readPath = eimsPath + "/" + ifId + "/" + ifId + direction + ".xsd";
        List<Map<String, Object>> body = eimsParser.parseEimsXsd(readPath);

        XmlSchema xsd = new XmlSchema(targetNameSpace, new XmlSchemaCollection());
        xsd.setElementFormDefault(XmlSchemaForm.QUALIFIED);
        xsd.setSchemaNamespacePrefix("xsd");
        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add("", URI_2001_SCHEMA_XSD);
        nsMap.add("edi", "http://shacon.kr/xsd");
        nsMap.add("ns", targetNameSpace);
        xsd.setNamespaceContext(nsMap);
        xsd.setTargetNamespace(targetNameSpace);

        XmlSchemaComplexType ct = new XmlSchemaComplexType(xsd, true);
        ct.setName(ifId + direction);
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        addElement(xsd, sequence, body);
        ct.setParticle(sequence);

        XmlSchemaElement elem = new XmlSchemaElement(xsd, true);
        elem.setName("root");
        elem.setSchemaTypeName(new QName(targetNameSpace, ct.getName()));
        Path path = Paths.get(schemaPath);
        Files.createDirectories(path);
        String writePath = schemaPath + "/" + ifId + direction + ".xsd";
        OutputStreamWriter or = new OutputStreamWriter(new FileOutputStream(writePath), StandardCharsets.UTF_8);
        xsd.write(or);
    }

    public void buildIntXsdforHcis(String ifId, String direction) throws IOException, XmlSchemaSerializer.XmlSchemaSerializerException {
        targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;
        String readPath = eimsPath + "/" + ifId + "/" + ifId + direction + ".xsd";
        List<Map<String, Object>> body = eimsParser.parseEimsXsd(readPath);

        XmlSchema xsd = new XmlSchema(targetNameSpace, new XmlSchemaCollection());

        xsd.setElementFormDefault(XmlSchemaForm.QUALIFIED);
        xsd.setSchemaNamespacePrefix("xsd");
        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add("", URI_2001_SCHEMA_XSD);
        nsMap.add("hdr", STD_HDR_NS);
        nsMap.add("edi", "http://shacon.kr/xsd");
        nsMap.add("ns", targetNameSpace);
        xsd.setNamespaceContext(nsMap);
        xsd.setTargetNamespace(targetNameSpace);

        XmlSchemaImport imp = new XmlSchemaImport(xsd);
        imp.setSchemaLocation("classpath://Schemas/HcisStdHeader.xsd");
        imp.setNamespace(STD_HDR_NS);

        XmlSchemaComplexType ct = new XmlSchemaComplexType(xsd, true);
        ct.setName(ifId + direction);
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        addElement(xsd, sequence, body);
        ct.setParticle(sequence);

        XmlSchemaComplexType rct = new XmlSchemaComplexType(xsd, true);
        rct.setName("rootType");
        XmlSchemaSequence seq = new XmlSchemaSequence();
        seq.getItems().add(addElement(xsd, "header", STD_HDR_NS, "hcisHeaderType"));
        seq.getItems().add(addElement(xsd, "data", targetNameSpace, ct.getName()));
        rct.setParticle(seq);

        XmlSchemaElement elem = new XmlSchemaElement(xsd, true);
        elem.setName("root");
        elem.setSchemaTypeName(new QName(targetNameSpace, rct.getName()));
        Path path = Paths.get(schemaPath);
        Files.createDirectories(path);
        String writePath = schemaPath + "/" + ifId + direction + ".xsd";
        OutputStreamWriter or = new OutputStreamWriter(new FileOutputStream(writePath), StandardCharsets.UTF_8);
        xsd.write(or);
    }

    /**
     * 1 depth xsd for Fixed Format Message
     *
     * @param folder
     * @param ifId
     * @param direction
     * @throws FileNotFoundException
     * @throws XmlSchemaSerializer.XmlSchemaSerializerException
     */
    public void buildExtXsd(String folder, String ifId, String direction) throws IOException, XmlSchemaSerializer.XmlSchemaSerializerException {

        String targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;
        String readPath = eimsPath + "/" + folder + "/" + ifId + direction;
        List<Map<String, Object>> header = eimsParser.parseEimsXsd(readPath + HDR_XSD);
        List<Map<String, Object>> body = eimsParser.parseEimsXsd(readPath + BODY_XSD);

        XmlSchema xsd = new XmlSchema(targetNameSpace, new XmlSchemaCollection());
        xsd.setElementFormDefault(XmlSchemaForm.QUALIFIED);
        xsd.setSchemaNamespacePrefix("xsd");
        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add("", URI_2001_SCHEMA_XSD);
        nsMap.add("ns", targetNameSpace);
        nsMap.add("edi", "http://shacon.kr/xsd");
        xsd.setNamespaceContext(nsMap);
        xsd.setTargetNamespace(targetNameSpace);

        XmlSchemaComplexType ct = new XmlSchemaComplexType(xsd, true);
        ct.setName(ifId + direction);
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        addElement(xsd, sequence, header);
        addElement(xsd, sequence, body);

        ct.setParticle(sequence);

        XmlSchemaElement elem = new XmlSchemaElement(xsd, true);
        elem.setName("root");
        elem.setSchemaTypeName(new QName(targetNameSpace, ct.getName()));
        Path path = Paths.get(schemaPath);
        Files.createDirectories(path);
        String writePath = schemaPath + "/" + ifId + direction + ".xsd";
        OutputStreamWriter or = new OutputStreamWriter(new FileOutputStream(writePath), StandardCharsets.UTF_8);
        xsd.write(or);
    }


    /**
     * 2 depth xsd for cis f/w
     *
     * @param folder
     * @param ifId
     * @param direction
     * @throws IOException
     * @throws XmlSchemaSerializer.XmlSchemaSerializerException
     */
    public void buildExtXsdforHcis(String folder, String ifId, String direction) throws IOException {
        targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;
        String readPath = eimsPath + "/" + folder + "/" + ifId + direction;
        List<Map<String, Object>> header = eimsParser.parseEimsXsd(readPath + HDR_XSD);
        List<Map<String, Object>> body = eimsParser.parseEimsXsd(readPath + BODY_XSD);

        XmlSchema xsd = new XmlSchema(targetNameSpace, new XmlSchemaCollection());
        xsd.setElementFormDefault(XmlSchemaForm.QUALIFIED);
        xsd.setSchemaNamespacePrefix("xsd");
        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add("", URI_2001_SCHEMA_XSD);
        nsMap.add("hdr", STD_HDR_NS);
        nsMap.add("edi", "http://shacon.kr/xsd");
        nsMap.add("ns", targetNameSpace);
        xsd.setNamespaceContext(nsMap);
        xsd.setTargetNamespace(targetNameSpace);

        XmlSchemaImport imp = new XmlSchemaImport(xsd);
        imp.setSchemaLocation("classpath://Schemas/HcisStdHeader.xsd");
        imp.setNamespace(STD_HDR_NS);

        XmlSchemaComplexType ctHeader = new XmlSchemaComplexType(xsd, true);
        ctHeader.setName(ifId + direction + "_Header");
        XmlSchemaSequence seqHeader = new XmlSchemaSequence();
        addElement(xsd, seqHeader, header);
        ctHeader.setParticle(seqHeader);

        XmlSchemaComplexType ctBody = new XmlSchemaComplexType(xsd, true);
        ctBody.setName(ifId + direction + "_Body");
        XmlSchemaSequence seqBody = new XmlSchemaSequence();
        addElement(xsd, seqBody, body);
        ctBody.setParticle(seqBody);

        XmlSchemaComplexType ct = new XmlSchemaComplexType(xsd, true);
        ct.setName(ifId + direction);
        XmlSchemaSequence seq = new XmlSchemaSequence();
        seq.getItems().add(addElement(xsd, "bizHeader", targetNameSpace, ifId + direction + "_Header"));
        seq.getItems().add(addElement(xsd, "bizBody", targetNameSpace, ifId + direction + "_Body"));
        ct.setParticle(seq);

        XmlSchemaComplexType rct = new XmlSchemaComplexType(xsd, true);
        rct.setName("rootType");
        XmlSchemaSequence seq2 = new XmlSchemaSequence();
        seq2.getItems().add(addElement(xsd, "header", STD_HDR_NS, "hcisHeaderType"));
        seq2.getItems().add(addElement(xsd, "data", targetNameSpace, ct.getName()));
        rct.setParticle(seq2);

        XmlSchemaElement elem = new XmlSchemaElement(xsd, true);
        elem.setName("root");
        elem.setSchemaTypeName(new QName(targetNameSpace, rct.getName()));
        String writePath = schemaPath + "/" + ifId + direction + ".xsd";
        OutputStreamWriter or = new OutputStreamWriter(new FileOutputStream(writePath), StandardCharsets.UTF_8);
        xsd.write(or);
    }

    private XmlSchemaComplexType addComplexType(XmlSchema xsd, String name, List<Map<String, Object>> elist) {
        XmlSchemaComplexType ct = new XmlSchemaComplexType(xsd, true);
        ct.setName(name);
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        addElement(xsd, sequence, elist);
        ct.setParticle(sequence);
        return ct;
    }

    private void addElement(XmlSchema xsd, XmlSchemaSequence sequence, List<Map<String, Object>> elist) {

        try {
            for (Map<String, Object> m : elist) {
                XmlSchemaElement elem = new XmlSchemaElement(xsd, false);
                elem.setName(String.valueOf(m.get("name")));
                elem.setMinOccurs(Long.valueOf(String.valueOf(m.get("minOcc"))));
                elem.setMaxOccurs(Long.valueOf(String.valueOf(m.get("maxOcc"))));
                elem.setNillable(true);
                Map<Object, Object> metaInfoMap = new HashMap<>();
                Map<Object, Object> metaInfo = new HashMap<>();
                org.w3c.dom.Document doc = xsd.getSchemaDocument();
                if (m.get("length") != null) {
                    elem.setSchemaTypeName(new QName(URI_2001_SCHEMA_XSD, String.valueOf(m.get("type"))));
                    addMetaInfo(doc, metaInfo, "length", String.valueOf(m.get("length")));
                } else { // add complexType
                    elem.setSchemaTypeName(new QName(targetNameSpace, String.valueOf(m.get("type"))));
                    List<Map<String, Object>> sublist = CastUtils.cast((List<?>) m.get(String.valueOf(m.get("name"))));
                    addComplexType(xsd, String.valueOf(m.get("type")), sublist);
                }
                addMetaInfo(doc, metaInfo, "kor", String.valueOf(m.get("kor")));

                if (String.valueOf(m.get("type")).equals("string")) {
                    addMetaInfo(doc, metaInfo, "padder", "SpaceRight");
                } else {
                    addMetaInfo(doc, metaInfo, "padder", "ZeroLeft");
                }

                metaInfoMap.put(Constants.MetaDataConstants.EXTERNAL_ATTRIBUTES, metaInfo);
                elem.setMetaInfoMap(metaInfoMap);
                sequence.getItems().add(elem);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private XmlSchemaElement addElement(XmlSchema xsd, String name, String prefix, String type) {
        XmlSchemaElement elem = new XmlSchemaElement(xsd, false);
        elem.setName(name);
        elem.setSchemaTypeName(new QName(prefix, type));
        return elem;
    }

    private void addMetaInfo(org.w3c.dom.Document doc, Map<Object, Object> metaInfo, String localPart, String val) {
        QName attrKey = new QName(NS_HCIS, localPart, "edi");
        Attr attrVal = doc.createAttributeNS("http://shacon.kr/xsd", localPart);
        attrVal.setValue(val);
        attrVal.setPrefix("edi");
        metaInfo.put(attrKey, attrVal);
    }

}


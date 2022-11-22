package com.shacon.hcis;

import kr.shacon.edi.util.CastUtils;
import org.apache.ws.commons.schema.*;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;


import javax.xml.namespace.QName;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
 *
 * @author choi hyoung ki
 */
public class EDISchemaBuilder {

    private static final Logger log = LoggerFactory.getLogger(EDISchemaBuilder.class);

    //TODO  NAS path modifi.
//	String eaiRepoPth = "/hcisnas/eai_data/eims/latest";
    String eaiRepoPth = "D:/HCIS/eai-ext/shacon/test/resources/";

    EimsParser eimsParser = new EimsParser();

    /**
     * 대내용 XSD 생성 (DATA부 만 존재함) HCIS Header 추가?
     *
     * @param ifId
     */
    public void createIntXSD(String ifId) {
        String dirPath = eaiRepoPth + ifId + "/";
        try {
            buildXsdInternal(dirPath, ifId, IN_REQ );
            buildXsdInternal(dirPath, ifId, IN_RES );
            buildXsdInternal(dirPath, ifId, OUT_REQ );
            buildXsdInternal(dirPath, ifId, OUT_RES );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XmlSchemaSerializer.XmlSchemaSerializerException e) {
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
        String dirPath = (resIfId != null) ? eaiRepoPth + reqIfId + "_" + resIfId + "/" : eaiRepoPth + reqIfId + "/";
        try {
            EimsParser parser = new EimsParser();
            Map<String, Object> eaiInfo = parser.parseXML(reqIfId, resIfId);
            Map<String, String> reqEaiInfo = CastUtils.cast((Map<?, ?>) eaiInfo.get("request"));
            String triggerSystem = reqEaiInfo.get("extHtdspId");  // 1.당발 2. 타발

            if (triggerSystem.equals("1")) {
                buildXsd1Depth(dirPath, reqIfId, OUT_REQ );
                buildXsd2Depth(dirPath, reqIfId, IN_REQ );
                if (resIfId != null) {
                    buildXsd1Depth(dirPath, resIfId, OUT_RES );
                    buildXsd2Depth(dirPath, resIfId, IN_RES );
                }
            } else {
                buildXsd1Depth(dirPath, reqIfId, IN_REQ );
                buildXsd2Depth(dirPath, reqIfId, OUT_REQ );
                if (resIfId != null) {
                    buildXsd1Depth(dirPath, resIfId, IN_RES );
                    buildXsd2Depth(dirPath, resIfId, OUT_RES );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buildXsdInternal(String dirPath, String ifId, String direction) throws FileNotFoundException, XmlSchemaSerializer.XmlSchemaSerializerException {
        String xsdPath = dirPath + ifId + direction + "_Int.xsd";
        String targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;

        List<Map<String, Object>> body = eimsParser.parseEimsXsd(dirPath + ifId + direction + ".xsd");

        XmlSchema xsd = new XmlSchema(targetNameSpace, new XmlSchemaCollection());
        xsd.setTargetNamespace(targetNameSpace);
        xsd.setElementFormDefault(XmlSchemaForm.QUALIFIED);
        xsd.setSchemaNamespacePrefix("xsd");
        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add("xs", URI_2001_SCHEMA_XSD);
        nsMap.add("edi", "http://shacon.kr/xsd");
        xsd.setNamespaceContext(nsMap);

        XmlSchemaComplexType ct = new XmlSchemaComplexType(xsd, true);
        ct.setName(ifId + direction);
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        addElement(xsd, sequence, body);
        ct.setParticle(sequence);

        XmlSchemaElement elem = new XmlSchemaElement(xsd, true);
        elem.setName("root");
        elem.setSchemaTypeName(new QName("", ct.getName()));

        OutputStreamWriter or = new OutputStreamWriter(new FileOutputStream(xsdPath), StandardCharsets.UTF_8);
        xsd.write(or);
    }

    /**
     * 1 depth xsd for Fixed Format Message
     *
     * @param dirPath
     * @param ifId
     * @param direction
     * @throws FileNotFoundException
     * @throws XmlSchemaSerializer.XmlSchemaSerializerException
     */
    public void buildXsd1Depth(String dirPath, String ifId, String direction) throws FileNotFoundException, XmlSchemaSerializer.XmlSchemaSerializerException {
        String xsdPath = dirPath + ifId + direction + "_Ext.xsd";
        String targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;

        List<Map<String, Object>> header = eimsParser.parseEimsXsd(dirPath + ifId + direction + HDR_XSD);
        List<Map<String, Object>> body = eimsParser.parseEimsXsd(dirPath + ifId + direction + BODY_XSD);

        XmlSchema xsd = new XmlSchema(targetNameSpace, new XmlSchemaCollection());
        xsd.setTargetNamespace(targetNameSpace);
        xsd.setElementFormDefault(XmlSchemaForm.QUALIFIED);
        xsd.setSchemaNamespacePrefix("xsd");
        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add("xs", URI_2001_SCHEMA_XSD);
        nsMap.add("edi", "http://shacon.kr/xsd");
        xsd.setNamespaceContext(nsMap);

        XmlSchemaComplexType ct = new XmlSchemaComplexType(xsd, true);
        ct.setName(ifId + direction);
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        addElement(xsd, sequence, header);
        addElement(xsd, sequence, body);
        ct.setParticle(sequence);

        XmlSchemaElement elem = new XmlSchemaElement(xsd, true);
        elem.setName("root");
        elem.setSchemaTypeName(new QName("", ct.getName()));

        OutputStreamWriter or = new OutputStreamWriter(new FileOutputStream(xsdPath), StandardCharsets.UTF_8);
        xsd.write(or);
    }


    /**
     * 2 depth xsd for cis f/w
     *
     * @param dirPath
     * @param ifId
     * @param direction
     * @throws IOException
     * @throws XmlSchemaSerializer.XmlSchemaSerializerException
     */
    public void buildXsd2Depth(String dirPath, String ifId, String direction) throws IOException {
        String xsdPath = dirPath + ifId + direction + ".xsd";
        String targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;

        List<Map<String, Object>> header = eimsParser.parseEimsXsd(dirPath + ifId + direction + HDR_XSD);
        List<Map<String, Object>> body = eimsParser.parseEimsXsd(dirPath + ifId + direction + BODY_XSD);

        XmlSchema xsd = new XmlSchema(targetNameSpace, new XmlSchemaCollection());
        xsd.setTargetNamespace(targetNameSpace);
        xsd.setElementFormDefault(XmlSchemaForm.QUALIFIED);
        xsd.setSchemaNamespacePrefix("xsd");
        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add("xs", URI_2001_SCHEMA_XSD);
        nsMap.add("edi", "http://shacon.kr/xsd");
        xsd.setNamespaceContext(nsMap);

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
        seq.getItems().add(addElement(xsd, "bizHeader", ifId + direction + "_Header"));
        seq.getItems().add(addElement(xsd, "bizBody", ifId + direction + "_Body"));
        ct.setParticle(seq);

        XmlSchemaElement elem = new XmlSchemaElement(xsd, true);
        elem.setName("root");
        elem.setSchemaTypeName(new QName("", ct.getName()));

        OutputStreamWriter or = new OutputStreamWriter(new FileOutputStream(xsdPath), StandardCharsets.UTF_8);
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
                Map<Object, Object> metaInfoMap = new HashMap<>();
                Map<Object, Object> metaInfo = new HashMap<>();
                org.w3c.dom.Document doc = xsd.getSchemaDocument();
                if (m.get("length") != null) {
                    elem.setSchemaTypeName(new QName(URI_2001_SCHEMA_XSD, String.valueOf(m.get("type"))));
                    addMetaInfo(doc, metaInfo, "length", String.valueOf(m.get("length")));
                } else { // add complexType
                    elem.setSchemaTypeName(new QName("", String.valueOf(m.get("type"))));
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
        } catch (XmlSchemaSerializer.XmlSchemaSerializerException e) {
            e.printStackTrace();
        }

    }

    private XmlSchemaElement addElement(XmlSchema xsd, String name, String type) {
        XmlSchemaElement elem = new XmlSchemaElement(xsd, false);
        elem.setName(name);
        elem.setSchemaTypeName(new QName("", type));
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


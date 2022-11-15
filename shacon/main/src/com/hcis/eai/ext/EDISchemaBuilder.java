package com.hcis.eai.ext;

import kr.shacon.edi.util.CastUtils;
import org.apache.ws.commons.schema.*;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.dom4j.DocumentException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import javax.xml.namespace.QName;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

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


    //TODO  NAS path modifi.
//	String eaiRepoPth = "/hcisnas/eai_data/eims/latest";
    String eaiRepoPth = "C:/worksrc/shacon/test/resources/";

    /**
     * 대외용 XSD 생성
     *
     * @param reqIfId 요청IFID
     * @param resIfId 응답IFID
     */
    public void createExtXSD(String reqIfId, String resIfId) {
        String dirPath = (resIfId != null) ? eaiRepoPth + reqIfId + "_" + resIfId + "/" : eaiRepoPth + reqIfId + "/";
        try {
            EimsXMLParser parser = new EimsXMLParser();
            Map<String, Object> eaiInfo = parser.parse(reqIfId, resIfId);
            Map<String, String> reqEaiInfo = CastUtils.cast((Map<?, ?>) eaiInfo.get("request"));
            String triggerSystem = reqEaiInfo.get("extHtdspId");  // 1.당발 2. 타발

            buildXsd2Depth(dirPath, reqIfId, IN_REQ);
            buildXsd2Depth(dirPath, reqIfId, OUT_REQ);
            if (resIfId != null) {
                buildXsd2Depth(dirPath, resIfId, IN_RES);
                buildXsd2Depth(dirPath, resIfId, OUT_RES);
            }
            if (triggerSystem.equals("1")) {
                buildXsd1Depth(dirPath, reqIfId, OUT_REQ);
                buildXsd1Depth(dirPath, resIfId, OUT_RES);
            }else{
                buildXsd1Depth(dirPath, reqIfId, IN_REQ);
                buildXsd1Depth(dirPath, resIfId, IN_RES);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        String xsdPath = dirPath + ifId + direction + "_EDI.xsd";
        String targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;

        List<Map<String, Object>> header = parseEIMS(getXmlSchema(dirPath + ifId + direction + HDR_XSD));
        List<Map<String, Object>> body = parseEIMS(getXmlSchema(dirPath + ifId + direction + BODY_XSD));

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
        elem.setName("main");
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
    public void buildXsd2Depth(String dirPath, String ifId, String direction) throws IOException, XmlSchemaSerializer.XmlSchemaSerializerException {
        String xsdPath = dirPath + ifId + direction + ".xsd";
        String targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;

        List<Map<String, Object>> header = parseEIMS(getXmlSchema(dirPath + ifId + direction + HDR_XSD));
        List<Map<String, Object>> body = parseEIMS(getXmlSchema(dirPath + ifId + direction + BODY_XSD));

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
        XmlSchemaSequence seqHeader = addElement(xsd, header);
        ctHeader.setParticle(seqHeader);

        XmlSchemaComplexType ctBody = new XmlSchemaComplexType(xsd, true);
        ctBody.setName(ifId + direction + "_Body");
        XmlSchemaSequence seqBody = addElement(xsd, body);
        ctBody.setParticle(seqBody);

        XmlSchemaComplexType ct = new XmlSchemaComplexType(xsd, true);
        ct.setName(ifId + direction);
        XmlSchemaSequence seq = new XmlSchemaSequence();
        seq.getItems().add(addElement(xsd, "bizHeader", ifId + direction + "_Header"));
        seq.getItems().add(addElement(xsd, "bizBody", ifId + direction + "_Body"));
        ct.setParticle(seq);

        XmlSchemaElement elem = new XmlSchemaElement(xsd, true);
        elem.setName("main");
        elem.setSchemaTypeName(new QName("", ct.getName()));

        OutputStreamWriter or = new OutputStreamWriter(new FileOutputStream(xsdPath), StandardCharsets.UTF_8);
        xsd.write(or);
    }

    private XmlSchemaElement addElement(XmlSchema xsd, String name, String type) {
        XmlSchemaElement elem = new XmlSchemaElement(xsd, false);
        elem.setName(name);
        elem.setSchemaTypeName(new QName("", type));
        return elem;
    }


    private XmlSchemaSequence addElement(XmlSchema xsd, List<Map<String, Object>> elist) {
        XmlSchemaSequence sequence = new XmlSchemaSequence();
        for (Map<String, Object> m : elist) {
            XmlSchemaElement elem = new XmlSchemaElement(xsd, false);
            elem.setName(String.valueOf(m.get("name")));
            elem.setSchemaTypeName(new QName(URI_2001_SCHEMA_XSD, (String) m.get("type")));
            elem.setMetaInfoMap((Map<Object, Object>) m.get("metaInfoMap"));
            sequence.getItems().add(elem);
        }
        return sequence;
    }

    private void addElement(XmlSchema xsd, XmlSchemaSequence sequence, List<Map<String, Object>> elist) {
        for (Map<String, Object> m : elist) {
            XmlSchemaElement elem = new XmlSchemaElement(xsd, false);
            elem.setName(String.valueOf(m.get("name")));
            elem.setSchemaTypeName(new QName(URI_2001_SCHEMA_XSD, (String) m.get("type")));
            elem.setMetaInfoMap((Map<Object, Object>) m.get("metaInfoMap"));
            sequence.getItems().add(elem);
        }
    }


    private XmlSchema getXmlSchema(String xsdPath) throws FileNotFoundException {
        XmlSchemaCollection xc = new XmlSchemaCollection();
        return xc.read(new FileReader(xsdPath));
    }


    /**
     * EIMS 배포된 XSD 파싱
     *
     * @param xsd
     * @return
     * @throws XmlSchemaSerializer.XmlSchemaSerializerException
     */
    private List<Map<String, Object>> parseEIMS(XmlSchema xsd) throws XmlSchemaSerializer.XmlSchemaSerializerException {
        Document doc = xsd.getSchemaDocument();
        List<Map<String, Object>> resultList = new LinkedList<>();

        XmlSchemaType rootType = xsd.getElementByName("root").getSchemaType();

        XmlSchemaParticle xmlSchemaParticle = ((XmlSchemaComplexType) rootType).getParticle();
        XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence) xmlSchemaParticle;
        List<XmlSchemaSequenceMember> items = xmlSchemaSequence.getItems();

        items.forEach((item) -> {
            XmlSchemaElement elem = (XmlSchemaElement) item;
            String name = elem.getName();
            XmlSchemaAnnotation anno = elem.getAnnotation();
            XmlSchemaDocumentation adoc = (XmlSchemaDocumentation) anno.getItems().get(1);
            String kor = String.valueOf(adoc.getMarkup().item(0).getNodeValue());

            XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType) elem.getSchemaType();
            XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction) simpleType.getContent();

            String type = r.getBaseTypeName().getLocalPart();
            String len = String.valueOf(r.getFacets().get(0).getValue());

            Map<Object, Object> metaInfo = new HashMap<>();
            addMetaInfo(doc, metaInfo, "length", len);
            addMetaInfo(doc, metaInfo, "kor", kor);
            if (type.equals("string")) {
                addMetaInfo(doc, metaInfo, "padder", "SpaceRight");
            } else {
                addMetaInfo(doc, metaInfo, "padder", "ZeroLeft");
            }
            Map<Object, Object> metaInfoMap = new HashMap<>();
            metaInfoMap.put(Constants.MetaDataConstants.EXTERNAL_ATTRIBUTES, metaInfo);
            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("metaInfoMap", metaInfoMap);
            resultMap.put("name", name);
            resultMap.put("type", type);
            resultList.add(resultMap);

        });
        return resultList;
    }

    private void addMetaInfo(Document doc, Map<Object, Object> metaInfo, String localPart, String val) {
        QName attrKey = new QName(NS_HCIS, localPart, "edi");
        Attr attrVal = doc.createAttributeNS("http://shacon.kr/xsd", localPart);
        attrVal.setValue(val);
        attrVal.setPrefix("edi");
        metaInfo.put(attrKey, attrVal);
    }
}

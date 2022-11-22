package com.shacon.hcis;

import kr.shacon.edi.util.CastUtils;
import org.apache.ws.commons.schema.*;
import org.apache.ws.commons.schema.constants.Constants;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static kr.shacon.edi.ShaConstants.*;

public class DataFormatResourceBuilder {
    private static final Logger log = LoggerFactory.getLogger(DataFormatResourceBuilder.class);

    String eaiRepoPth = "D:/HCIS/eai-ext/shacon/test/resources/";

    EimsParser eimsParser = new EimsParser();

    /**
     * 당발/타발 구분 대외거래 전문 Tibco Data Format Resource
     * @param reqIfId
     * @param resIfId
     * @throws DocumentException
     * @throws IOException
     */
    public void buildTibRes(String reqIfId, String resIfId) throws DocumentException, IOException {

        String dirPath = (resIfId != null) ? eaiRepoPth + reqIfId + "_" + resIfId + "/" : eaiRepoPth + reqIfId + "/";

        Map<String, Object> eaiInfo = eimsParser.parseXML(reqIfId, resIfId);

        Map<String, String> reqInfo = CastUtils.cast((Map<?, ?>) eaiInfo.get("request"));

        Map<String, String> source = CastUtils.cast((Map<?, ?>) eaiInfo.get("source"));
        Map<String, String> target = CastUtils.cast((Map<?, ?>) eaiInfo.get("target"));

        if( reqInfo.get("infraId").equals("2") ) {  // 대내
            if(source.get("sysId").equals("TAD")){
                build(dirPath, reqIfId, IN_REQ + "_Int");
                build(dirPath, reqIfId, IN_RES + "_Int");
            }
            if(target.get("sysId").equals("TAD")){
                build(dirPath, reqIfId, OUT_REQ + "_Int");
                build(dirPath, reqIfId, OUT_RES + "_Int");
            }
        }

        if( reqInfo.get("infraId").equals("3") ) {  // 대외

            String triggerSystem = reqInfo.get("extHtdspId");  // 1.당발 2. 타발

            if (triggerSystem.equals("1")) {
                build(dirPath, reqIfId, OUT_REQ + "_Ext");
                if (resIfId != null) {
                    build(dirPath, resIfId, OUT_RES + "_Ext");
                }
            } else {
                build(dirPath, reqIfId, IN_REQ + "_Ext");
                if (resIfId != null) {
                    build(dirPath, resIfId, IN_RES + "_Ext");
                }
            }
        }
    }

    public void build(String dirPath, String ifId, String direction) throws IOException {
        String resPath = dirPath + ifId + direction + ".dataFormatResource";
        String packageName = "edi";

        Document doc = DocumentHelper.createDocument();
        Namespace ns = new Namespace("jndi", "http://xsd.tns.tibco.com/amf/models/sharedresource/jndi");
        Element root = doc.addElement(new QName("namedResource", ns));
        root.setName("jndi:namedResource");
        root.addNamespace("xmi", "http://www.omg.org/XMI");
        root.addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.addNamespace(ifId, "http://schema.hcis.com/xsd/" + ifId);
        root.addNamespace("dataformat", "http://ns.tibco.com/bw/palette/dataformat");
        root.addNamespace("jndi", "http://xsd.tns.tibco.com/amf/models/sharedresource/jndi");
        root.addAttribute("name", packageName + "." + ifId + direction);
        root.addAttribute("type", "dataformat:DataFormat");

        Element imports = root.addElement(new QName("imports", ns));
        imports.addAttribute("importType", "http://www.w3.org/2001/XMLSchema");
//        imports.addAttribute("location", dirPath + ifId + direction + ".xsd");
        imports.addAttribute("namespace", "http://schema.hcis.com/xsd/" + ifId);

        Element config = root.addElement(new QName("configuration", ns));
        config.addAttribute("xsi:type", "dataformat:DataFormat");
        config.addAttribute("formatType", "Fixed format");
        config.addAttribute("colSeparator", ",");
        config.addAttribute("colSeparatorParseRule", "Treat all characters as entered as a single column separator string");
        config.addAttribute("lineSeparator", "New Line");
        config.addAttribute("fillCharacter", "Space");
        config.addAttribute("schemaElementQName", ifId + ":main");


        List<Map<String, String>> layout = parseXSD(dirPath + ifId + direction + ".xsd");
        List<String> ls = new LinkedList<>();
        int start = 0, end = 0, totalSize = 0;
        for (Map<String, String> m : layout) {
            int colLen = Integer.parseInt(String.valueOf(m.get("length")));
            totalSize += colLen;
            start = end;
            end = start + colLen;
            Element e = config.addElement("fieldOffsets");
            e.addAttribute("kor", String.valueOf(m.get("kor")));
            e.addAttribute("type", String.valueOf(m.get("type")));
            e.addAttribute("name", String.valueOf(m.get("name")));
            e.addAttribute("start", String.valueOf(start));
            e.addAttribute("end", String.valueOf(end));
            ls.add(m.get("name") + "(" + start + "," + end + ")");
        }
        config.addAttribute("lineLength", String.valueOf(totalSize));
        String fldOffsets = URLDecoder.decode(ls.stream().collect(Collectors.joining("&#xA;")), "UTF-8");
        config.addAttribute("fieldOffsetsStr", fldOffsets);

        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setNewLineAfterDeclaration(true);
        StringWriter sw = new StringWriter();
        XMLWriter xmlWriter = new XMLWriter(sw, format);
        xmlWriter.write(doc);
        String res = sw.toString().replace("&amp;", "&");

        log.debug(res);

        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resPath), StandardCharsets.UTF_8));
        writer.write(res);
        writer.flush();
        writer.close();


//        Writer modWriter = new ModifyingWriterFactory().createRegexModifyingWriter(orgWriter, "\\[\\$AMPERSAND_CHRACTER\\$\\]", "&");
    }

    private List<Map<String, String>> parseXSD(String path) throws FileNotFoundException {
        XmlSchema xsd = new XmlSchemaCollection().read(new FileReader(path));
        List<Map<String, String>> resultList = new LinkedList<>();
        XmlSchemaType rootType = xsd.getElementByName("root").getSchemaType();

        XmlSchemaParticle xmlSchemaParticle = ((XmlSchemaComplexType) rootType).getParticle();

        XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence) xmlSchemaParticle;
        List<XmlSchemaSequenceMember> items = xmlSchemaSequence.getItems();
        items.forEach((item) -> {
            XmlSchemaElement elem = (XmlSchemaElement) item;
            Map<String, String> resultMap = new HashMap<>();
            resultMap.put("name", elem.getName());
            resultMap.put("kor", getAttrVal(elem.getMetaInfoMap(), "kor"));
            resultMap.put("length", getAttrVal(elem.getMetaInfoMap(), "length"));
            resultMap.put("type", elem.getSchemaTypeName().getLocalPart());
            resultList.add(resultMap);

        });
        return resultList;
    }

    private String getAttrVal(Map<Object, Object> metaInfoMap, String localPart) {
        Map<Object, Object> map = (Map<Object, Object>) metaInfoMap.get(Constants.MetaDataConstants.EXTERNAL_ATTRIBUTES);
        javax.xml.namespace.QName key = new javax.xml.namespace.QName("http://shacon.kr/xsd", localPart, "edi");
        Attr attr = (Attr) map.get(key);
        return attr.getValue();
    }
}

package com.hcis.eai.ext;

import kr.shacon.edi.util.CastUtils;
import org.apache.ws.commons.schema.*;
import org.apache.ws.commons.schema.constants.Constants;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
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

public class TibDataFormatResBuilder {
    String eaiRepoPth = "C:/worksrc/shacon/test/resources/";


    public void buildTibRes(String reqIfId, String resIfId) throws DocumentException, IOException {
        String dirPath = (resIfId != null) ? eaiRepoPth + reqIfId + "_" + resIfId + "/" : eaiRepoPth + reqIfId + "/";
        EimsXMLParser parser = new EimsXMLParser();
        Map<String, Object> eaiInfo = parser.parse(reqIfId, resIfId);
        Map<String, String> reqEaiInfo = CastUtils.cast((Map<?, ?>) eaiInfo.get("request"));
        String triggerSystem = reqEaiInfo.get("extHtdspId");  // 1.당발 2. 타발

        if (triggerSystem.equals("1")) {
            build(dirPath, reqIfId, OUT_REQ);
            if (resIfId != null) {
                build(dirPath, resIfId, OUT_RES);
            }
        } else {
            build(dirPath, reqIfId, IN_REQ);
            if (resIfId != null) {
                build(dirPath, resIfId, IN_RES);
            }
        }
    }

    public void build(String dirPath, String ifId, String direction) throws IOException {
        String resPath = dirPath + ifId + direction + ".dataformatResource";
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
        root.addAttribute("name", packageName + "." + ifId );
        root.addAttribute("type", "dataformat:DataFormat");

        Element imports = root.addElement(new QName("imports", ns));
        imports.addAttribute("importType", "http://www.w3.org/2001/XMLSchema");
        imports.addAttribute("location", dirPath + ifId + direction + ".xsd");
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
            int colLen = Integer.valueOf(m.get("length"));
            totalSize += colLen;
            start = end;
            end = start + colLen;
            Element e = config.addElement("fieldOffsets");
            e.addAttribute("kor", m.get("kor"));
            e.addAttribute("type", m.get("type"));
            e.addAttribute("name", m.get("name"));
            e.addAttribute("start", String.valueOf(start));
            e.addAttribute("end", String.valueOf(end));
            ls.add(m.get("name") + "(" + start + "," + end + ")");
        }
        config.addAttribute("lineLength", String.valueOf(totalSize));
        String fldOffsets = URLDecoder.decode(ls.stream().collect(Collectors.joining("&#xA;")), "UTF-8");
        config.addAttribute("fieldOffsetsStr", fldOffsets);

        Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resPath), StandardCharsets.UTF_8));
//        FileWriter w = new FileWriter(resPath);
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setNewLineAfterDeclaration(true);
        XMLWriter writer = new XMLWriter(w, format);
        writer.write(doc);
        w.flush();
    }

    private List<Map<String, String>> parseXSD(String path) throws FileNotFoundException {
        XmlSchema xsd = new XmlSchemaCollection().read(new FileReader(path));
        List<Map<String, String>> resultList = new LinkedList<>();
        XmlSchemaType rootType = xsd.getElementByName("main").getSchemaType();
        Map<javax.xml.namespace.QName, XmlSchemaType> types = xsd.getSchemaTypes();
        XmlSchemaParticle xmlSchemaParticle = ((XmlSchemaComplexType) rootType).getParticle();

        XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence) xmlSchemaParticle;
        List<XmlSchemaSequenceMember> items = xmlSchemaSequence.getItems();
        items.forEach((item) -> {
            XmlSchemaElement elem = (XmlSchemaElement) item;
            String n = elem.getSchemaTypeName().getLocalPart();
            XmlSchemaType type = types.get(elem.getSchemaTypeName());
            XmlSchemaSequence seq = (XmlSchemaSequence) ((XmlSchemaComplexType) type).getParticle();
            List<XmlSchemaSequenceMember> subs = seq.getItems();
            subs.forEach((sub) -> {
                Map<String, String> resultMap = new HashMap<>();
                XmlSchemaElement e = (XmlSchemaElement) sub;
                resultMap.put("name", e.getName());
                resultMap.put("kor", getAttrVal(e.getMetaInfoMap(), "kor"));
                resultMap.put("length", getAttrVal(e.getMetaInfoMap(), "length"));
                resultMap.put("type", e.getSchemaTypeName().getLocalPart());
                resultList.add(resultMap);
            });
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

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
                build(dirPath, reqIfId, IN_REQ + "_Int", null);
                build(dirPath, reqIfId, IN_RES + "_Int", null);
            }
            if(target.get("sysId").equals("TAD")){
                build(dirPath, reqIfId, OUT_REQ + "_Int", null);
                build(dirPath, reqIfId, OUT_RES + "_Int", null);
            }
        }

        if( reqInfo.get("infraId").equals("3") ) {  // 대외

            String triggerSystem = reqInfo.get("extHtdspId");  // 1.당발 2. 타발

            if (triggerSystem.equals("1")) {
                build(dirPath, reqIfId, OUT_REQ + "_Ext", null);
                if (resIfId != null) {
                    build(dirPath, resIfId, OUT_RES + "_Ext", null);
                }
            } else {
                build(dirPath, reqIfId, IN_REQ + "_Ext", null);
                if (resIfId != null) {
                    build(dirPath, resIfId, IN_RES + "_Ext", null);
                }
            }
        }
    }

    public void build(String dirPath, String ifId, String direction, List<Map<String, Object>> param) throws IOException {
        String resPath = dirPath + ifId + direction + ".dataFormatResource";
        String packageName = "edi";

        List<Map<String, Object>> layout = (param==null) ? parseXSD(dirPath + ifId + direction + ".xsd") : param;

        Document doc = DocumentHelper.createDocument();
        Element config = createDFR(doc, ifId, packageName, direction);

        String fldOffsets = addFieldOffsets(config, layout, dirPath, ifId, direction);
        config.addAttribute("fieldOffsetsStr", fldOffsets);

        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setNewLineAfterDeclaration(true);
        StringWriter sw = new StringWriter();
        XMLWriter xmlWriter = new XMLWriter(sw, format);
        xmlWriter.write(doc);
        String res = sw.toString().replace("&amp;", "&");

        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resPath), StandardCharsets.UTF_8));
        writer.write(res);
        writer.flush();
        writer.close();
    }

    private Element createDFR(Document doc, String ifId, String packageName, String direction){
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
        return config;
    }

    private String addFieldOffsets(Element config, List<Map<String, Object>> layout, String dirPath, String ifId, String direction) throws IOException {
        List<String> ls = new LinkedList<>();
        int start = 0, end = 0, totalSize = 0;
        for (Map<String, Object> m : layout) {
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

            Object o = m.get(String.valueOf(m.get("name")));
            if(o instanceof List){
                build(dirPath, ifId, direction+"_"+String.valueOf(m.get("name")), CastUtils.cast((List) o));
            }
        }
        config.addAttribute("lineLength", String.valueOf(totalSize));
        String fldOffsets = ls.stream().collect(Collectors.joining("&#xA;"));
        return fldOffsets;
    }

    private List<Map<String, Object>> parseXSD(String path) throws FileNotFoundException {
        XmlSchema xsd = new XmlSchemaCollection().read(new FileReader(path));
        XmlSchemaType rootType = xsd.getElementByName("root").getSchemaType();
        List<Map<String, Object>> resultList = parseComplexType((XmlSchemaComplexType)rootType);
        return resultList;
    }

    private List<Map<String,Object>> parseComplexType(XmlSchemaComplexType ct){
        List<Map<String, Object>> resultList = new LinkedList<>();
        XmlSchemaParticle xmlSchemaParticle = ct.getParticle();
        XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence) xmlSchemaParticle;
        List<XmlSchemaSequenceMember> items = xmlSchemaSequence.getItems();
        items.forEach((item) -> {
            Map<String, Object> resultMap = new HashMap<>();
            XmlSchemaElement elem = (XmlSchemaElement) item;
            resultMap.put("name", elem.getName());
            resultMap.put("kor", getAttrVal(elem.getMetaInfoMap(), "kor"));
            resultMap.put("length", getAttrVal(elem.getMetaInfoMap(), "length"));
            resultMap.put("type", elem.getSchemaTypeName().getLocalPart());
            if( elem.getSchemaType() instanceof XmlSchemaComplexType ){
                resultMap.put(elem.getName(), parseComplexType((XmlSchemaComplexType)elem.getSchemaType()));
            }
            resultList.add(resultMap);
        });
        return resultList;
    }

    private String getAttrVal(Map<Object, Object> metaInfoMap, String localPart) {
        Map<Object, Object> map = (Map<Object, Object>) metaInfoMap.get(Constants.MetaDataConstants.EXTERNAL_ATTRIBUTES);
        javax.xml.namespace.QName key = new javax.xml.namespace.QName("http://shacon.kr/xsd", localPart, "edi");
        if(map.get(key) != null) {
            Attr attr = (Attr) map.get(key);
            return attr.getValue();
        }else{
            return "0";
        }
    }
}

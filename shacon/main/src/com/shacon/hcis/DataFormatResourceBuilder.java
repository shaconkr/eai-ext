package com.shacon.hcis;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static kr.shacon.edi.ShaConstants.*;

/**
 * TIBCO Data Format Resource Builder
 * <p>
 * TIBCO Project 별 Schema 위치
 * Git-Repo / ~/eai-com , ~/eai-int, ~/eai-ext, ~/eai-mci
 * APP명 :  소스SysId(3) + 타겟sysId(3) + SEQ(2) + .application
 * AP모듈명 :  소스SysId(3) + 타겟sysId(3) + SEQ(2) + .module
 * Shared 모듈명 :  대상시스템 SysId(3) + .smodule
 * 패키지명 : 소스 SysId(3) _ 소스업무(4) _ 타겟 SysId(3) _ 타겟업무(4)
 * Schema :  모듈명 /  schema / 패키지명
 * Resources : 모듈명 /  Resources / 패키지명
 * /hcisnas/eai_data/ 1.eai-mci 2.eai-int 3.eai-ext / AP모듈명 / Schema , Resources / package /
 */
public class DataFormatResourceBuilder {
    private static final Logger log = LoggerFactory.getLogger(DataFormatResourceBuilder.class);
    private static final String infra[] = {"", "/eai-mci/", "/eai-int/", "/eai-ext/"};
    String eimsPath;
    String projPath;
    String packageName;
    EimsParser eimsParser;
    String schemaPath;
    String resourcePath;

    public DataFormatResourceBuilder(String eimsPath, String projPath) {
        this.eimsPath = eimsPath;
        this.projPath = projPath;
        this.eimsParser = new EimsParser(eimsPath);
    }

    /**
     * 당발/타발 구분 대외거래 전문 Tibco Data Format Resource
     *
     * @param reqIfId
     * @param resIfId
     * @throws DocumentException
     * @throws IOException
     */
    public void buildTibRes(String reqIfId, String resIfId) throws DocumentException, IOException {
        String dirPath = (resIfId != null) ? projPath + reqIfId + "_" + resIfId + "/" : eimsPath + reqIfId + "/";

        Map<String, Object> eaiInfo = eimsParser.parseXML(reqIfId, resIfId);
        Map<String, String> reqInfo = CastUtils.cast((Map<?, ?>) eaiInfo.get("request"));
        Map<String, String> source = CastUtils.cast((Map<?, ?>) eaiInfo.get("source"));
        Map<String, String> target = CastUtils.cast((Map<?, ?>) eaiInfo.get("target"));

        String moduleName = source.get("sysId") + target.get("sysId") + "01.module";
        packageName = Joiner.on("_").join(Lists.newArrayList(source.get("sysId"), source.get("workId"), target.get("sysId"), target.get("workId")));
        schemaPath = projPath + infra[Integer.parseInt(reqInfo.get("infraId"))] + moduleName + "/Schemas/" + packageName;
        resourcePath = projPath + infra[Integer.parseInt(reqInfo.get("infraId"))] + moduleName + "/Resources/" + packageName;

        if (reqInfo.get("infraId").equals("2")) {  // 대내
            if (source.get("sysId").equals("TAD")) {
                build(reqIfId, IN_REQ, null);
                build(reqIfId, IN_RES, null);
            }
            if (target.get("sysId").equals("TAD")) {
                build(reqIfId, OUT_REQ, null);
                build(reqIfId, OUT_RES, null);
            }
        }

        if (reqInfo.get("infraId").equals("3")) {  // 대외
            String triggerSystem = reqInfo.get("extHtdspId");  // 1.당발 2. 타발
            if (triggerSystem.equals("1")) {
                build(reqIfId, OUT_REQ, null);
                if (resIfId != null) {
                    build(resIfId, OUT_RES, null);
                }
            } else {
                build(reqIfId, IN_REQ, null);
                if (resIfId != null) {
                    build(resIfId, IN_RES, null);
                }
            }
        }
    }

    public void build(String ifId, String direction, List<Map<String, Object>> param) throws IOException {
        String readPath = schemaPath + "/" + ifId + direction + ".xsd";
        char tail = direction.charAt(direction.length() - 1);
        if (tail == 'q' || tail == 's')
            splitComplexTypeXSD(readPath, ifId, direction);

        List<Map<String, Object>> layout = (param == null) ? parseXSD(readPath) : param;

        Document doc = DocumentHelper.createDocument();
        Element config = createDFR(doc, ifId, packageName, direction);

        String fldOffsets = addFieldOffsets(config, layout, ifId, direction);
        config.addAttribute("fieldOffsetsStr", fldOffsets);

        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setNewLineAfterDeclaration(true);
        StringWriter sw = new StringWriter();
        XMLWriter xmlWriter = new XMLWriter(sw, format);
        xmlWriter.write(doc);
        String res = sw.toString().replace("&amp;", "&");

        Path path = Paths.get(resourcePath);
        Files.createDirectories(path);
        String writePath = resourcePath + "/" + ifId + direction + ".dataFormatResource";
        Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writePath), StandardCharsets.UTF_8));
        writer.write(res);
        writer.flush();
        writer.close();
    }

    private Element createDFR(Document doc, String ifId, String packageName, String direction) {
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
        imports.addAttribute("location", "../../Schemas/" + ifId + direction + ".xsd");
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

    private String addFieldOffsets(Element config, List<Map<String, Object>> layout, String ifId, String direction) throws IOException {
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
            if (o instanceof List) {
                build(ifId, direction + "_" + String.valueOf(m.get("name")), CastUtils.cast((List) o));
            }
        }
        config.addAttribute("lineLength", String.valueOf(totalSize));
        String fldOffsets = ls.stream().collect(Collectors.joining("&#xA;"));
        return fldOffsets;
    }

    private List<Map<String, Object>> parseXSD(String path) throws FileNotFoundException {
        XmlSchema xsd = new XmlSchemaCollection().read(new FileReader(path));
        XmlSchemaType rootType = xsd.getElementByName("root").getSchemaType();
        List<Map<String, Object>> resultList = parseComplexType((XmlSchemaComplexType) rootType);
        return resultList;
    }

    private List<Map<String, Object>> parseComplexType(XmlSchemaComplexType ct) {
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
            if (elem.getSchemaType() instanceof XmlSchemaComplexType) {
                resultMap.put(elem.getName(), parseComplexType((XmlSchemaComplexType) elem.getSchemaType()));
            }
            resultList.add(resultMap);
        });
        return resultList;
    }

    private String getAttrVal(Map<Object, Object> metaInfoMap, String localPart) {
        Map<Object, Object> map = (Map<Object, Object>) metaInfoMap.get(Constants.MetaDataConstants.EXTERNAL_ATTRIBUTES);
        javax.xml.namespace.QName key = new javax.xml.namespace.QName("http://shacon.kr/xsd", localPart, "edi");
        if (map.get(key) != null) {
            Attr attr = (Attr) map.get(key);
            return attr.getValue();
        } else {
            return "0";
        }
    }

    private void splitComplexTypeXSD(String path, String ifId, String direction) throws IOException {
        XmlSchema src = new XmlSchemaCollection().read(new FileReader(path));
        List<XmlSchemaObject> items = src.getItems();
        items.forEach((item) -> {
            if (item instanceof XmlSchemaComplexType) {

                XmlSchema xsd = new XmlSchema(src.getTargetNamespace(), new XmlSchemaCollection());
                xsd.setNamespaceContext(src.getNamespaceContext());

                XmlSchemaComplexType ct = (XmlSchemaComplexType) item;

                XmlSchemaElement elem = new XmlSchemaElement(xsd, false);
                xsd.getItems().add(elem);
                elem.setName("root");

                XmlSchemaComplexType complexType = new XmlSchemaComplexType(xsd,false);
                elem.setType(complexType);

                complexType.setParticle(ct.getParticle());

                String postfix = (ct.getName().equals(ifId + direction)) ? "" : "_" + ct.getName();
                String writePath = schemaPath + "/" + ifId + direction + postfix + "_df.xsd";
                OutputStreamWriter or = null;
                try {
                    or = new OutputStreamWriter(new FileOutputStream(writePath), StandardCharsets.UTF_8);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                xsd.write(or);
            }

        });
    }
}

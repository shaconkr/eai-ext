package com.hcis.eai.ext;

import org.apache.ws.commons.schema.*;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;

import javax.xml.namespace.QName;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.apache.ws.commons.schema.constants.Constants.URI_2001_SCHEMA_XSD;

/**
 * HCIS Project
 * 
 * EIMS Integration
 * 
 * EIMS 에서 등록된 전문정보로 고정길이전문 생성/파싱 용 XSD 생성
 * 
 * 
 * @author choi hyoung ki
 *
 */
public class EimsToEaiExtSchema {	
	
	private String ifId;

	private final String targetNameSpace = "http://schema.hcis.com/xsd/";
	private final String IN_REQ 	= "_InReq";
	private final String OUT_REQ 	= "_OutReq";
	private final String IN_RES 	= "_InRes";
	private final String OUT_RES 	= "_OutRes";
	private final String HDR_XSD 	= "Hdr.xsd";
	private final String BODY_XSD 	= "Body.xsd";
	
	
	//TODO  NAS path modifi.
	String eimsRootPath = "D:/HCIS/eai-com/eims-doc/latest/"; // /app/tibco/eims/eai-com/eims-doc/latest  //TODO    /HCISNAS  NAS mount
	String xsdRootPath = "D:/HCIS/eai-ext/";  // + /systemId + /Schemas + ifId  + ~.xsd 
	
	
	/**
	 * 대외용 XSD 생성
	 * 
	 * @param reqIfId  요청IFID
	 * @param resIfId  응답IFID
	 */
	public void createExtXSD(String reqIfId, String resIfId) {
		try {
			String extSystemId = reqIfId.substring(3,6);
			if(resIfId != null) { 
				String prefix = eimsRootPath +  reqIfId + "_" + resIfId + "/" + resIfId ;
				mergeXSD(extSystemId, reqIfId, prefix, IN_REQ);
				mergeXSD(extSystemId, reqIfId, prefix, OUT_REQ);
				mergeXSD(extSystemId, resIfId, prefix, IN_RES);
				mergeXSD(extSystemId, resIfId, prefix, OUT_RES);					
			}else {
				String prefix = eimsRootPath +  reqIfId + "/" + reqIfId;				
				mergeXSD(extSystemId, reqIfId, prefix, IN_REQ);
				mergeXSD(extSystemId, reqIfId, prefix, OUT_REQ);	
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 *  대외용 header/body 로 분리된 XSD 를 merge 하고 EDI(고정길이) XSD 생성
	 *  
	 * @param headerXSD
	 * @param bodyXSD
	 * @return
	 * @throws IOException 
	 */
	public void mergeXSD(String systemId, String ifId, String prefix, String direction) throws IOException, XmlSchemaSerializer.XmlSchemaSerializerException {
//		String xsdPath = xsdRootPath + systemId + ".module/Schemas/" + ifId + "/" + ifId +  direction;
		String xsdPath = prefix + direction + ".xsd";
		String targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;

		List<Map<String,Object>> header = parseEIMS(getXmlSchema(prefix + direction + HDR_XSD));
		List<Map<String,Object>> body = parseEIMS(getXmlSchema(prefix + direction + BODY_XSD));

		XmlSchema xsd = new XmlSchema(targetNameSpace,new XmlSchemaCollection());
		xsd.setTargetNamespace(targetNameSpace);
		xsd.setElementFormDefault(XmlSchemaForm.QUALIFIED);
		xsd.setSchemaNamespacePrefix("xsd");
		NamespaceMap nsMap = new NamespaceMap();
		nsMap.add("xs", URI_2001_SCHEMA_XSD);
		nsMap.add("edi", "http://shacon.kr/xsd");
		xsd.setNamespaceContext(nsMap);

		XmlSchemaComplexType ct = new XmlSchemaComplexType(xsd,true);
		ct.setName(ifId + direction);

		XmlSchemaSequence sequence = new XmlSchemaSequence();
		addElement(xsd, sequence, header);
		addElement(xsd, sequence, body);
		ct.setParticle(sequence);

		XmlSchemaElement elem = new XmlSchemaElement(xsd, true);
		elem.setName("main");

		OutputStreamWriter or = new OutputStreamWriter(new FileOutputStream(xsdPath), StandardCharsets.UTF_8);
		xsd.write(or);
	}

	private void addElement(XmlSchema xsd, XmlSchemaSequence sequence, List<Map<String,Object>> elist){
		for(Map<String, Object> m : elist) {
			XmlSchemaElement elem = new XmlSchemaElement(xsd, false);
			elem.setName(String.valueOf(m.get("name")));
			elem.setSchemaTypeName(new QName(URI_2001_SCHEMA_XSD, (String) m.get("type")));
			elem.setMetaInfoMap((Map<Object, Object>) m.get("metaInfoMap"));
			sequence.getItems().add(elem);
		}
	}

	private XmlSchemaType getType(String type){
		XmlSchemaCollection schemaCol = new XmlSchemaCollection();
		return schemaCol.getTypeByQName(new QName(URI_2001_SCHEMA_XSD, type));
	}

	private XmlSchema getXmlSchema(String xsdPath) throws FileNotFoundException {
		InputStreamReader ir = null;
		ir = new FileReader(xsdPath);
		XmlSchemaCollection xc = new XmlSchemaCollection();
		return xc.read(ir);
	}


	/**
	 * EIMS 배포된 XSD 파싱
	 * @param xsd
	 * @return
	 * @throws XmlSchemaSerializer.XmlSchemaSerializerException
	 */
    private  List<Map<String, Object>> parseEIMS(XmlSchema xsd) throws XmlSchemaSerializer.XmlSchemaSerializerException {
		Document doc = xsd.getSchemaDocument();
		List<Map<String, Object>> resultList = new ArrayList<>();

		XmlSchemaType rootType = xsd.getElementByName("root").getSchemaType();

		XmlSchemaParticle xmlSchemaParticle = ((XmlSchemaComplexType)rootType).getParticle();
		XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence) xmlSchemaParticle;
		List<XmlSchemaSequenceMember> items = xmlSchemaSequence.getItems();

		items.forEach((item) -> {
			XmlSchemaElement elem = (XmlSchemaElement) item;
			String name = elem.getName();
			XmlSchemaAnnotation anno = elem.getAnnotation();
			XmlSchemaDocumentation adoc = (XmlSchemaDocumentation) anno.getItems().get(1);
			String kor = String.valueOf(adoc.getMarkup().item(0).getNodeValue());

			XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType)elem.getSchemaType();
			XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction) simpleType.getContent();

			String type = r.getBaseTypeName().getLocalPart();
			String len = String.valueOf(r.getFacets().get(0).getValue());

			Map<Object, Object> metaInfo = new HashMap<>();
			addMetaInfo(doc, metaInfo, "length", len);
			addMetaInfo(doc, metaInfo, "kor", kor);
			Map<Object, Object> metaInfoMap = new HashMap<>();
			metaInfoMap.put(Constants.MetaDataConstants.EXTERNAL_ATTRIBUTES,metaInfo);
			Map<String, Object> resultMap = new LinkedHashMap<>();
			resultMap.put("type", type);
			resultMap.put("name", name);
			resultMap.put("metaInfoMap", metaInfoMap);
			resultList.add(resultMap);

		});

		return resultList;
    }

	private void addMetaInfo(Document doc, Map<Object, Object> metaInfo, String localPart, String val){
		QName attrKey = new QName(targetNameSpace, localPart, "edi" );
		Attr attrVal = doc.createAttributeNS("http://shacon.kr/xsd",localPart );
		attrVal.setValue(val);
		attrVal.setPrefix("edi");
		metaInfo.put(attrKey, attrVal);
	}
}

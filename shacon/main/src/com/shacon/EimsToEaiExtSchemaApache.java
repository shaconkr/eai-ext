package com.shacon;

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

public class EimsToEaiExtSchemaApache {

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
	

	public static void main(String...args){

		EimsToEaiExtSchemaApache mg = new EimsToEaiExtSchemaApache();

		try {
			mg.createExtXSD("CISNICCDYZ0000010", "NICNICCDYZ0000011");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XmlSchemaSerializer.XmlSchemaSerializerException e) {
			e.printStackTrace();
		}
		;
	}



	/**
	 * 대외용 XSD 생성
	 * 
java	 * @param resIfId  응답IFID
	 */
	public void createExtXSD(String reqIfId, String resIfId) throws IOException, XmlSchemaSerializer.XmlSchemaSerializerException {
		String extSystemId = reqIfId.substring(3,6);
		if(resIfId != null) { 
			String prefix = eimsRootPath +  reqIfId + "_" + resIfId + "/" + reqIfId ;
			mergeXSD(extSystemId, reqIfId, prefix, IN_REQ);
			mergeXSD(extSystemId, reqIfId, prefix, OUT_REQ);
			prefix = eimsRootPath +  reqIfId + "_" + resIfId + "/" + resIfId ;
			mergeXSD(extSystemId, resIfId, prefix, IN_RES);
			mergeXSD(extSystemId, resIfId, prefix, OUT_RES);					
		}else {
			String prefix = eimsRootPath +  reqIfId + "/" + reqIfId;				
			mergeXSD(extSystemId, reqIfId, prefix, IN_REQ);
			mergeXSD(extSystemId, reqIfId, prefix, OUT_REQ);	
		}
	}
	
	
	
	/**
	 *  대외용 header/body 로 분리된 XSD 를 merge 하고 EDI(고정길이) XSD 생성
	 *  
	 * @param systemId (대외 SystemID
	 * @param ifId
	 * @param prefix
	 * @param direction
	 * @throws IOException 
	 */
	public void mergeXSD(String systemId, String ifId, String prefix, String direction) throws IOException, XmlSchemaSerializer.XmlSchemaSerializerException {
//		String xsdPath = xsdRootPath + systemId + ".module/Schemas/" + ifId + "/" + ifId +  direction;
		String xsdPath = prefix + direction + ".xsd";
		String targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;

		List<Map<String,Object>> header = parseEimsXsd(getXSDSchema(prefix + direction + HDR_XSD));
		List<Map<String,Object>> body = parseEimsXsd(getXSDSchema(prefix + direction + BODY_XSD));

		XmlSchema xsd = new XmlSchema(targetNameSpace,new XmlSchemaCollection());
		xsd.setTargetNamespace(targetNameSpace);
		xsd.setElementFormDefault(XmlSchemaForm.QUALIFIED);

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
		elem.setSchemaTypeName(new QName(targetNameSpace, ct.getName()));

		OutputStreamWriter or = new OutputStreamWriter(new FileOutputStream(xsdPath), StandardCharsets.UTF_8);
		xsd.write(or);
	}

	private void addElement(XmlSchema xsd, XmlSchemaSequence sequence, List<Map<String,Object>> elist){
		for(Map<String, Object> m : elist) {
			XmlSchemaElement elem = new XmlSchemaElement(xsd, false);
			elem.setName(String.valueOf(m.get("name")));
//			elem.setType(getType((String) m.get("type")));
			elem.setSchemaTypeName(new QName(URI_2001_SCHEMA_XSD, (String) m.get("type")));
			elem.setMetaInfoMap((Map<Object, Object>) m.get("metaInfoMap"));
			sequence.getItems().add(elem);
		}
	}

	private XmlSchemaType getType(String type){
		XmlSchemaCollection schemaCol = new XmlSchemaCollection();
		return schemaCol.getTypeByQName(new QName(URI_2001_SCHEMA_XSD, type));
	}

	private XmlSchema getXSDSchema(String xsdPath) {
		InputStreamReader ir = null;
		try {
			ir = new FileReader(xsdPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		XmlSchemaCollection xc = new XmlSchemaCollection();
		return xc.read(ir);
	}


    private  List<Map<String, Object>> parseEimsXsd(XmlSchema xsd) throws XmlSchemaSerializer.XmlSchemaSerializerException {
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

/**
        XSDElementDeclaration rootElement = null;
        XSDComplexTypeDefinition complexTypeDefinition = null;
        XSDComplexTypeContent complexTypeContent = null;
        XSDParticleContent particleContent = null;

        EList<XSDSchemaContent> contentEList = xsdSchema.getContents();
        for (XSDSchemaContent content : contentEList) {
            if (content instanceof XSDElementDeclaration) {
                rootElement = (XSDElementDeclaration) content;
                break;
            }
        }

        complexTypeDefinition = (XSDComplexTypeDefinition) rootElement.getTypeDefinition();
        String msgName = complexTypeDefinition.getName();
        System.out.println(msgName);
        complexTypeContent = complexTypeDefinition.getContent();

        EList<XSDParticle> xsdParticles = ((XSDModelGroup) ((XSDParticle) complexTypeContent).getContent()).getContents();

        for (XSDParticle particle : xsdParticles) {
            particleContent = particle.getContent();
            if (particleContent instanceof XSDElementDeclaration) {
                XSDElementDeclaration elem = (XSDElementDeclaration) particleContent;
                String colName = elem.getName();
                XSDAnnotation anno = ((XSDElementDeclaration) particleContent).getAnnotation();
                String korName = anno.getUserInformation().get(0).getFirstChild().getNodeValue();
                XSDSimpleTypeDefinition simpleType = elem.getTypeDefinition().getSimpleType();
				String type = simpleType.getBaseType().getName();
                int totLen = (simpleType.getEffectiveTotalDigitsFacet() == null) ? 0 : simpleType.getEffectiveTotalDigitsFacet().getValue();
                int maxLen = (simpleType.getMaxLengthFacet() == null) ? 0 : simpleType.getMaxLengthFacet().getValue();
                int colLen = (maxLen > -0) ? maxLen : totLen;
				Map<Object, Object> metaInfo = new HashMap<>();
				addMetaInfo(doc, metaInfo, "length", String.valueOf(colLen));
				addMetaInfo(doc, metaInfo, "kor", korName);
				Map<Object, Object> metaInfoMap = new HashMap<>();
				metaInfoMap.put(Constants.MetaDataConstants.EXTERNAL_ATTRIBUTES,metaInfo);
				Map<String, Object> resultMap = new LinkedHashMap<>();
				resultMap.put("type", type);
                resultMap.put("name", colName);
				resultMap.put("metaInfoMap", metaInfoMap);
				resultList.add(resultMap);
            }
        }
        return resultList;
 */
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

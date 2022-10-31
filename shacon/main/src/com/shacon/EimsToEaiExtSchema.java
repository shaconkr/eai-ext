package com.shacon;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.xsd.*;
import org.eclipse.xsd.util.XSDConstants;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceImpl;
import org.eclipse.xsd.util.XSDUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class EimsToEaiExtSchema {	
	
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

		EimsToEaiExtSchema mg = new EimsToEaiExtSchema();

		try {
			mg.createExtXSD("CISNICCDYZ0000010", "NICNICCDYZ0000011");
		} catch (IOException e) {
			e.printStackTrace();
		}
		;
	}



	/**
	 * 대외용 XSD 생성
	 * 
	 * @param reqIfId  요청IFID
	 * @param resIfId  응답IFID
	 */
	public void createExtXSD(String reqIfId, String resIfId) throws IOException {		
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
	public void mergeXSD(String systemId, String ifId, String prefix, String direction) throws IOException {
//		String xsdPath = xsdRootPath + systemId + ".module/Schemas/" + ifId + "/" + ifId +  direction;
		String xsdPath = prefix + direction + ".xsd";
		String targetNameSpace = "http://schema.hcis.com/xsd/" + ifId;

		Map<String,String> header = parseEimsXsd(getXSDSchema(prefix + direction + HDR_XSD));
		Map<String,String> body = parseEimsXsd(getXSDSchema(prefix + direction + BODY_XSD));				

		XSDFactory factory = XSDFactory.eINSTANCE;		
		
		XSDSchema xsdSchema = factory.createXSDSchema();
		xsdSchema.setSchemaForSchemaQNamePrefix("xsd");
		xsdSchema.setTargetNamespace(targetNameSpace);

		Map<String,String> namespaces = xsdSchema.getQNamePrefixToNamespaceMap();
		namespaces.put(xsdSchema.getSchemaForSchemaQNamePrefix(), XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001);
		namespaces.put("edi", targetNameSpace);
		
		XSDComplexTypeDefinition type = factory.createXSDComplexTypeDefinition();
		type.setTargetNamespace(targetNameSpace);
		type.setName(ifId + direction);
		
		XSDModelGroup sequence = factory.createXSDModelGroup();
		sequence.setCompositor(XSDCompositor.SEQUENCE_LITERAL);

		XSDParticle particle = factory.createXSDParticle();
		particle.setContent(sequence);

        for(Map.Entry<String,String> m : header.entrySet()) {
			String[] val = m.getValue().split("\\|");
			String colName = m.getKey();
			int colLen = Integer.valueOf(val[1]);
			String eType = val[3];
			String kName = val[2];
			XSDElementDeclaration e = factory.createXSDElementDeclaration();
			e.setName(colName);
			XSDSchema lSchema = XSDUtil.getSchemaForSchema(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001);
			e.setTypeDefinition(lSchema.resolveSimpleTypeDefinition(eType));
//    		el.setAttribute("length", String.valueOf(colLen));
			XSDParticle p = factory.createXSDParticle();
			p.setContent(e);
			sequence.getContents().add(p);
		}
        for(Map.Entry<String,String> m : body.entrySet()){
            String[] val = m.getValue().split("\\|");
            String colName = m.getKey();
            int colLen = Integer.valueOf(val[1]);
			String eType = val[3];
			String kName = val[2];
    		XSDElementDeclaration e = factory.createXSDElementDeclaration();		
    		e.setName(colName);
			XSDSchema lSchema = XSDUtil.getSchemaForSchema(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001);
			e.setTypeDefinition(lSchema.resolveSimpleTypeDefinition(eType));

//    		e.getElement().setAttribute("edi:length", String.valueOf(colLen));
    		XSDParticle p = factory.createXSDParticle();
    		p.setContent(e);
    		sequence.getContents().add(p);            
        }

		type.setContent(particle);
		xsdSchema.getContents().add(type);

		XSDElementDeclaration main = factory.createXSDElementDeclaration();		
		main.setName("main");
		main.setTypeDefinition(type);
		xsdSchema.getContents().add(main);
        
        ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());
		resourceSet.getLoadOptions().put(XSDResourceImpl.XSD_TRACK_LOCATION, Boolean.TRUE);
		XSDResourceImpl res = (XSDResourceImpl) resourceSet.createResource(URI.createFileURI(xsdPath));
        res.getContents().add(xsdSchema);
        res.getDefaultSaveOptions().put(XMLResource.OPTION_ENCODING, "UTF-8");
        res.save(null);
	}
	
	private XSDSchema getXSDSchema(String xsdPath) {
        ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());
		resourceSet.getLoadOptions().put(XSDResourceImpl.XSD_TRACK_LOCATION, Boolean.TRUE);
		XSDResourceImpl res = (XSDResourceImpl) resourceSet.getResource(URI.createFileURI(xsdPath), true);
        return res.getSchema();
	}

	/**
     * EIMS 수신 XSD  Parsing 
     * 
     * <p>
     * Key : 순번
     * value : 컬럼영문명 + "|" + 컬럼사이즈 + "|"  + "컬럼한글명"
     *
     * @param xsdSchema
     * @return Map
     */
    private  Map<String, String> parseEimsXsd(XSDSchema xsdSchema) {
        Map<String, String> result = new LinkedHashMap<>();
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

        int seq = 0;
        for (XSDParticle particle : xsdParticles) {
            particleContent = particle.getContent();
            if (particleContent instanceof XSDElementDeclaration) {
                XSDElementDeclaration elem = (XSDElementDeclaration) particleContent;
                String colName = elem.getName();
                XSDAnnotation anno = ((XSDElementDeclaration) particleContent).getAnnotation();
                String desc = anno.getUserInformation().get(0).getFirstChild().getNodeValue();
                XSDSimpleTypeDefinition simpleType = elem.getTypeDefinition().getSimpleType();
				String type = simpleType.getBaseType().getName();
                int totLen = (simpleType.getEffectiveTotalDigitsFacet() == null) ? 0 : simpleType.getEffectiveTotalDigitsFacet().getValue();
                int maxLen = (simpleType.getMaxLengthFacet() == null) ? 0 : simpleType.getMaxLengthFacet().getValue();
                int colLen = (maxLen > -0) ? maxLen : totLen;
                result.put(colName, seq++ + "|" + colLen + "|" + desc + "|" + type);
            }
        }
        return result;
    }
}

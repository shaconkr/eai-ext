package com.shacon;

//package com.tibco.bw.tools.migrator.v6.palette.parse.sharedResource;

import com.tibco.amf.model.sharedresource.jndi.NamedResource;
import com.tibco.bw.sharedresource.dataformat.model.dataformat.DataFormat;
import com.tibco.bw.sharedresource.dataformat.model.dataformat.DataFormatFieldOffset;
import com.tibco.bw.sharedresource.dataformat.model.dataformat.DataformatFactory;
import com.tibco.bw.sharedresource.dataformat.model.helper.DataFormatConstants;
import com.tibco.bw.sharedresource.dataformat.model.helper.DataFormatHelper;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.xsd.*;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceImpl;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.*;

/**
 * HCIS 대외 전문 리소스 생성
 *
 * EIMS 배포 -->  ~io.xml -> extract XSD  -> 대외일경우 dataFormatResource(fixed-length dataformat) 생성
 *
 * @author chris
 *
 */
public class ShaResCon {
    private final String DATAFORMAT_RESOURCE_FILE_EXTN = "dataFormatResource";
    private final QName DATAFORMAT_RESOURCE_QNAME = new QName("http://ns.tibco.com/bw/palette/dataformat", "DataFormat");
    private final QName DATA_FORMAT_QNAME = new QName("http://ns.tibco.com/bw/palette/dataformat", "DataFormat", "dataformat");
    private final String CONFIG_STRING = "config";

    public static void main(String[] args) {

        try {
            String systemCode = "EXT";
            String applicationCode = "EAI";
            String ifId = "TESTIF";
            ShaResCon conv = new ShaResCon();
            conv.genDataFromatRes(systemCode, applicationCode, ifId);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * HCIS 대외 전문 XSD --> dataFormatResource
     * create only inReq & inRes
     * <p>
     * EIMS
     * source-io.xml  inbound = IN_REQ , outbound = IN_RES
     * target-io.xml  inbound = OUT_REQ,  outbound = OUT_RES
     * <p>
     * ex)  d:/HCIS/eai-ext/ExtCommon-SharedResource/Schemas/CIS/UPID/CISNICUPID0020601-InReq.xsd
     * <p>
     * ex)  d:/HCIS/eai-ext/ExtCommon-SharedResource/resources/dataFormat/CIS/UPID/CISNICUPID0020601-InReq.xsd
     * <p>
     * written by chris.
     */
    public void genDataFromatRes(String systemCode, String applicationCode, String interfaceId) {

        String gitRepoPath = "d:/HCIS/eai-ext";
        String proejctName = "/ExtCommon-SharedResource";
        String projectPath = gitRepoPath + proejctName;
        String gategory = systemCode + "/" + applicationCode + "/";
        String xsdPath = projectPath + "/Schemas/" + gategory ;
        String resPath = projectPath + "/resources/dataFormat/" + gategory;
        List<String> syncList = Arrays.asList("InReq","OutReq", "OutRes", "InRes");
      //  List<String> aSyncList = Arrays.asList("InReq","OutReq");  just  xsd file exists check !!

        for(String direc : syncList) {
            String xsdUri = xsdPath + interfaceId + "-" + direc + ".xsd";
            String resUri = resPath + interfaceId + "-" + direc + ".dataFormatResource";
            excute(xsdUri, resUri);
        }

    }

    private void excute(String xsdPath, String resPath) {
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getLoadOptions().put(XSDResourceImpl.XSD_TRACK_LOCATION, Boolean.TRUE);
        XSDResourceImpl xsdResource = (XSDResourceImpl) (resourceSet.getResource(URI.createFileURI(xsdPath), true));
        XSDSchema xsdSchema = xsdResource.getSchema();

        String targetNamespace = xsdSchema.getTargetNamespace();
        System.out.println(" targetNamespace : " + targetNamespace);
        DataFormat dataFormat = DataformatFactory.eINSTANCE.createDataFormat();
        dataFormat.setFormatType("Fixed format");
        dataFormat.setColSeparator("");
        dataFormat.setLineSeparator("New Line");
        dataFormat.setFillCharacter("Space");
        makeFieldOffsets(dataFormat, xsdSchema);

        writeDataFormatResource(dataFormat, resPath);
    }



    /**
     * TIBCO dataFormat Resource 에 Offsets 을 생성한다
     *
     * @param xsdSchema
     * @param dataFormat
     */
    private void makeFieldOffsets(DataFormat dataFormat, XSDSchema xsdSchema) {

        DataFormatFieldOffset dataFormatFieldOffset = null;
        List<DataFormatFieldOffset> fieldOffsetList = new ArrayList();
        Map<String, String> map = parseEimsXsd(xsdSchema, dataFormat);
        XSDElementDeclaration elemDec = dataFormat.getSchemaElement();

        int start = 0 , end = 0, totalSize = 0;
        for(Map.Entry<String,String> m : map.entrySet()){
            String[] val = m.getValue().split("\\|");
            String colName = m.getKey();
            int colLen = Integer.valueOf(val[1]);
            totalSize += colLen;
            start = end;
            end = start + colLen;
            System.out.println(colName + "|"+start+"|"+end);
            dataFormatFieldOffset = DataformatFactory.eINSTANCE.createDataFormatFieldOffset();
            dataFormatFieldOffset.setName(colName);
            dataFormatFieldOffset.setStart(start);
            dataFormatFieldOffset.setEnd(end);
            fieldOffsetList.add(dataFormatFieldOffset);
        }
        dataFormat.getFieldOffsets().addAll(fieldOffsetList);
        dataFormat.setFieldOffsetsStr(DataFormatHelper.convertFieldOffsetToString(fieldOffsetList));
        dataFormat.setLineLength(totalSize);
    }

    /**
     * TIBCO dataFormat Resource 를 생성하기 위하여  EIMS 로부터 배포 수신된 XSD 를 Parsing 하여
     * <p>
     * Key : 순번
     * value : 컬럼영문명 + "|" + 컬럼사이즈 + "|"  + "컬럼한글명"
     *
     * @param xsdSchema
     * @return Map
     */
    private  Map<String, String> parseEimsXsd(XSDSchema xsdSchema, DataFormat dataFormat) {
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
        dataFormat.setSchemaElement(rootElement);
        dataFormat.setSchemaElementQName(new QName("http://ns.tibco.com/bw/palette/dataformat", "DataFormat"));

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
                int totLen = (simpleType.getEffectiveTotalDigitsFacet() == null) ? 0 : simpleType.getEffectiveTotalDigitsFacet().getValue();
                int maxLen = (simpleType.getMaxLengthFacet() == null) ? 0 : simpleType.getMaxLengthFacet().getValue();
                int colLen = (maxLen > -0) ? maxLen : totLen;
                System.out.println("| " + colName + " | " + colLen + " | " + desc);
                result.put(colName, seq++ + "|" + colLen + "|" + desc);
            }
        }
        return result;
    }

    protected void writeDataFormatResource(DataFormat dataFormat, String resPath) {
//        NamedResource namedResource = new NamedResourceImpl2();
//        namedResource.setConfiguration((EObject) dataFormat);
//        namedResource.setType(DataFormatConstants.DATA_FORMAT_QNAME);
//        namedResource.setName("dataFormatResource");
//        ResourceSet resourceSet = new ResourceSetImpl();
//        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("dataFormatResource", new XMIResourceFactoryImpl());
//        XMLResource res = (XMLResource) resourceSet.createResource(URI.createFileURI(resPath));
//        res.getContents().add(namedResource);
//        res.getDefaultSaveOptions().put(XMLResource.OPTION_ENCODING, "UTF-8");
//        try {
//            res.save(null);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        MigrationUtils.write(res);
    }
}

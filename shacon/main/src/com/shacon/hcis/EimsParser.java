package com.shacon.hcis;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.apache.ws.commons.schema.*;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *  EIMS 배포 위치 NAS    /hcisnas/eai_data/eims/latest*
 *  TIBCO Project 별 Schema 위치
 *  Git-Repo / ~/eai-com , ~/eai-int, ~/eai-ext, ~/eai-mci
 *  APP명 :  소스SysId(3) + 타겟sysId(3) + SEQ(2) + .application
 *  AP모듈명 :  소스SysId(3) + 타겟sysId(3) + SEQ(2) + .module
 *  Shared 모듈명 :  대상시스템 SysId(3) + .smodule
 *  패키지명 : 소스 SysId(3) _ 소스업무(4) _ 타겟 SysId(3) _ 타겟업무(4)
 *  Schema :  모듈명 /  schema / 패키지명
 *  Resources : 모듈명 /  Resources / 패키지명
 *   /hcisnas/eai_data/ 1.eai-mci 2.eai-int 3.eai-ext / AP모듈명 / Schema , Resources / package /
 */
public class EimsParser {
    private static final Logger log = LoggerFactory.getLogger(EimsParser.class);

    String eimsPath;

    public EimsParser(String eimsPath) {
        this.eimsPath = eimsPath;
    }

    public Map<String, Object> parseXML(String reqIfId, String resIfId) throws DocumentException, IOException {

        String dirPath = (!Strings.isNullOrEmpty(resIfId)) ? eimsPath + reqIfId + "_" + resIfId : eimsPath +  reqIfId;
        String reqIfXml = dirPath + "/interface.xml";
        String resIfXml = dirPath + "/interface-1.xml";
        String sourceIO = dirPath + "/source-io.xml";
        String targetIO = dirPath + "/target-io.xml";

        Map<String, Object> ifType = parseEimsIfType(getDocuemnt(reqIfXml));

        if (resIfId != null) {
            return ImmutableMap.<String, Object>builder()
                    .putAll(ifType)
                    .put("request", parseEimsInterfaceXML(getDocuemnt(reqIfXml), getCharset(getDocuemnt(sourceIO))))
                    .put("response", parseEimsInterfaceXML(getDocuemnt(resIfXml), getCharset(getDocuemnt(targetIO))))
                    .build();
        } else {
            return ImmutableMap.<String, Object>builder()
                    .putAll(ifType)
                    .put("request", parseEimsInterfaceXML(getDocuemnt(reqIfXml), getCharset(getDocuemnt(sourceIO))))
                    .build();
        }
    }

    public List<Map<String, Object>> parseEimsXsd(String path) throws FileNotFoundException {
        XmlSchema xsd = new XmlSchemaCollection().read(new FileReader(path));
        XmlSchemaType rootType = xsd.getElementByName("root").getSchemaType();
        List<Map<String, Object>> resultList = new LinkedList<>();
        parseEimsXsd(rootType, resultList);
        return resultList;
    }

    private void parseEimsXsd(XmlSchemaType xmlSchemaType, List<Map<String, Object>> resultList) {
        XmlSchemaParticle xmlSchemaParticle = ((XmlSchemaComplexType) xmlSchemaType).getParticle();
        XmlSchemaSequence xmlSchemaSequence = (XmlSchemaSequence) xmlSchemaParticle;
        List<XmlSchemaSequenceMember> items = xmlSchemaSequence.getItems();
        items.forEach((item) -> {
            XmlSchemaElement elem = (XmlSchemaElement) item;
            String name = elem.getName();
            XmlSchemaAnnotation anno = elem.getAnnotation();
            XmlSchemaDocumentation adoc = (XmlSchemaDocumentation) anno.getItems().get(1);
            String kor = String.valueOf(adoc.getMarkup().item(0).getNodeValue());
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("name", name);
            resultMap.put("kor", kor);
            resultMap.put("minOcc", elem.getMinOccurs());
            resultMap.put("maxOcc", elem.getMaxOccurs());

            if (elem.getSchemaType() instanceof XmlSchemaSimpleType) {
                XmlSchemaSimpleType simpleType = (XmlSchemaSimpleType) elem.getSchemaType();
                XmlSchemaSimpleTypeRestriction r = (XmlSchemaSimpleTypeRestriction) simpleType.getContent();
                String type = r.getBaseTypeName().getLocalPart();
                String len = String.valueOf(r.getFacets().get(0).getValue());
                resultMap.put("length", len);
                resultMap.put("type", type);
            } else if (elem.getSchemaType() instanceof XmlSchemaComplexType) {
                List<Map<String, Object>> sublist = new LinkedList<>();
                parseEimsXsd(elem.getSchemaType(), sublist);
                resultMap.put("type", elem.getSchemaTypeName().getLocalPart());
                resultMap.put(name, sublist);
            } else {
                log.error("is not supprot Type {}", elem.getName());
            }
            resultList.add(resultMap);
        });
    }

    private Map<String, String> parseEimsInterfaceXML(Document doc, String charset) {
        Element inf = doc.getRootElement();
        Node comm = doc.selectSingleNode("/interface/common");
        Node attr = comm.selectSingleNode("attribute");
        Node ext = attr.selectSingleNode("online/external");
        Node bat = attr.selectSingleNode("batch/file2file");
        Node after = doc.selectSingleNode("/interface/interface_type/target/process_type/mft/batch/file2file");
        Map<String, String> ret = ImmutableMap.<String, String>builder()
                .put("ifId", inf.valueOf("@id"))
                .put("ifNm", inf.valueOf("@name"))
                .put("sysId", comm.selectSingleNode("system").valueOf("@code"))                     // 시스템코드
                .put("sysNm", comm.selectSingleNode("system").valueOf("@name"))                     // 시스템명
                .put("workId", comm.selectSingleNode("work").valueOf("@code"))                      // 어플리케이션코드
                .put("workNm", comm.selectSingleNode("work").valueOf("@name"))                      // 어플리케이션명
                .put("infraId", attr.selectSingleNode("infra").valueOf("@code"))                    // 연계구분 1:MCI 2:대내I/F  3:대외I/F 6:통합UI
                .put("infraNm", attr.selectSingleNode("infra").valueOf("@name"))                    // 연계구분명칭
                .put("procId", attr.selectSingleNode("process").valueOf("@code"))                   // 처리유형
                .put("procNm", attr.selectSingleNode("process").valueOf("@name"))                   // 처리유형명칭
                .put("charset", charset)                                                                // 문자셑
                .put("extSysId", ext.selectSingleNode("system").valueOf("@code"))                   // 대외기관코드
                .put("extSysNm", ext.selectSingleNode("system").valueOf("@name"))                   // 대외기관명칭
                .put("extWorkId", ext.selectSingleNode("work").valueOf("@code"))                    // 대외업무코드
                .put("extWorkNm", ext.selectSingleNode("work").valueOf("@name"))                    // 대외업무명칭
                .put("extHtdspId", ext.selectSingleNode("htdsp").valueOf("@code"))                  // 당타발구분코드  1.당발 / 2:타발
                .put("extHtdspNm", ext.selectSingleNode("htdsp").valueOf("@name"))                  // 당타발구분명칭
                .put("extSndRcvId", ext.selectSingleNode("send_receive").valueOf("@code"))          // 대외송수신구분코드 H:송신 / E:수신
                .put("extSndRcvNm", ext.selectSingleNode("send_receive").valueOf("@name"))          // 대외송수신구분명칭
                .put("extOneWayYn", ext.selectSingleNode("no_timeout_async").valueOf("@code"))      // 단방향처리여부 Y:예 / N:아니오
                .put("extResTypeId", ext.selectSingleNode("no_res_type").valueOf("@code"))          // 무응답시 수신구분 - 1:요청전문 / 2:표준헤더
                .put("extResTypeNm", ext.selectSingleNode("no_res_type").valueOf("@name"))          // 무응답시 수신구분 - 1:요청전문 / 2:표준헤더
                .put("msgCd", ext.selectSingleNode("msg").valueOf("@code"))                         // 대외 전문종별
                .put("txCd", ext.selectSingleNode("tx").valueOf("@code"))                           // 대외 거래구분
                .put("extFilePath", bat.selectSingleNode("ext_file_path").getText())                    // 대외기관 파일경로
                .put("extfileNm", bat.selectSingleNode("ext_file_nm").getText())                        // 대외기관 파일명
                .put("extLineChar", bat.selectSingleNode("ext_line_char").getText())                    // 대외 개행문자
                .put("extRecordSize", bat.selectSingleNode("ext_record_size").getText())                // 대외 레코드크기
                .put("extDateChange", bat.selectSingleNode("ext_date_change").getText())                // 대외 일자 변동분
                .put("extFileCode", bat.selectSingleNode("ext_file_code").getText())                    // 대외 파일코드
                .put("extDuplYn", bat.selectSingleNode("ext_dupl_yn").getText())                        // 대외 중복수신 여부
                .put("scheduleYn", bat.selectSingleNode("schedule_yn").getText())                       // 전송 스케줄
                .put("dayCode", bat.selectSingleNode("day_code").getText())                             // 파일 스케줄
                .put("scheduleStart", bat.selectSingleNode("schedule_start").getText())                 // 스케줄 시작 시간
                .put("scheduleEnd", bat.selectSingleNode("schedule_end").getText())                     // 스케줄 종료 시간
                .put("afterExecute", after.selectSingleNode("after_execute").getText())                 // 후속처리 여부
                .put("afterShellName", after.selectSingleNode("after_shell_nm").getText())              // 후속처리 쉘 파일명
                .put("afterShellPath", after.selectSingleNode("after_shell_path").getText())            // 후속처리 쉘 파일경로
                .build();
        return ret;
    }

    private Map<String, Object> parseEimsIfType(Document doc) {
        Node source = doc.selectSingleNode("/interface/interface_type/source");
        Node target = doc.selectSingleNode("/interface/interface_type/target");
        return ImmutableMap.<String, Object>builder()
                .put("source", ImmutableMap.<String, String>builder()
                        .put("sysId", source.selectSingleNode("system").valueOf("@code"))                     // 시스템코드
                        .put("sysNm", source.selectSingleNode("system").valueOf("@name"))                     // 시스템명
                        .put("workId", source.selectSingleNode("work").valueOf("@code"))                      // 어플리케이션코드
                        .put("workNm", source.selectSingleNode("work").valueOf("@name"))                      // 어플리케이션명
                        .put("ioid", source.selectSingleNode("ioid").getText())                                   // IOID
                        .build())
                .put("target", ImmutableMap.<String, String>builder()
                        .put("sysId", target.selectSingleNode("system").valueOf("@code"))                     // 시스템코드
                        .put("sysNm", target.selectSingleNode("system").valueOf("@name"))                     // 시스템명
                        .put("workId", target.selectSingleNode("work").valueOf("@code"))                      // 어플리케이션코드
                        .put("workNm", target.selectSingleNode("work").valueOf("@name"))                      // 어플리케이션명
                        .put("ioid", target.selectSingleNode("ioid").getText())                                   // IOID
                        .build())
                .build();
    }

    private String getCharset(Document doc) {
        Node comm = doc.selectSingleNode("/io/common");
        return comm.selectSingleNode("charset").getText();
    }

    private Document getDocuemnt(String path) throws IOException, DocumentException {
        SAXReader reader = new SAXReader();
        return reader.read(Files.newBufferedReader(Path.of(path)));
    }

}

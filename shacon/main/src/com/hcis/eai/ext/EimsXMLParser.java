package com.hcis.eai.ext;

import com.google.common.collect.ImmutableMap;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class EimsXMLParser {

    String eaiRepoPth = "C:/worksrc/shacon/test/resources/";


    public Map<String, Object> parse(String reqIfId, String resIfId) throws DocumentException, IOException {

        String dirPath = (resIfId != null) ? eaiRepoPth + reqIfId + "_" + resIfId: eaiRepoPth + reqIfId;
        String reqIfXml = dirPath + "/interface.xml";
        String resIfXml = dirPath + "/interface-1.xml";
        String sourceIO = dirPath + "/source-io.xml";
        String targetIO = dirPath + "/target-io.xml";

        if (resIfId != null) {
            return ImmutableMap.<String, Object>builder()
                    .put("request", parseEimsInterfaceXML(getDocuemnt(reqIfXml), getCharset(getDocuemnt(sourceIO))))
                    .put("response", parseEimsInterfaceXML(getDocuemnt(resIfXml), getCharset(getDocuemnt(targetIO))))
                    .build();
        } else {
            return ImmutableMap.<String, Object>builder()
                    .put("request", parseEimsInterfaceXML(getDocuemnt(reqIfXml), getCharset(getDocuemnt(sourceIO))))
                    .build();

        }

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


    private String getCharset(Document doc) {
        Node comm = doc.selectSingleNode("/io/common");
        return comm.selectSingleNode("charset").getText();
    }


    private Document getDocuemnt(String path) throws IOException, DocumentException {
        SAXReader reader = new SAXReader();
        return reader.read(Files.newBufferedReader(Path.of(path)));
    }


}

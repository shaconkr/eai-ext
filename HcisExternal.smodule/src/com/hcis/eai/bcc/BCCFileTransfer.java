package com.hcis.eai.bcc;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hcis.eai.BatchFTP600;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class BCCFileTransfer extends BatchFTP600 {

    public BCCFileTransfer(String beanioXml, String encoding) {
        super(beanioXml, encoding);
    }

    /**
     * 전문종별
     * @param bytes
     * @return
     */
    public String getMsgCode(byte[] bytes){
        Map<String,Object> msg = parseEDI("BCC_COM_HDR", bytes);
        return String.valueOf(msg.get("tgrmSubCCd"));
    }

    public String getBlockNo(byte[] bytes){
        Map<String,Object> msg = parseEDI("BCC_COM_HDR", bytes);
        return String.valueOf(msg.get("blockNo"));
    }

    
    public String getJobMngInfo(byte[] bytes){
        Map<String,Object> msg = parseEDI("BCC_0600", bytes);
        return String.valueOf(msg.get("jobMngInfo"));
    }
    
    /**
     * Header 26 Bytes
     * @param msgCode
     * @return
     */
    private Map<String,Object> buildHeader(String msgCode){
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
//                .put("dataSize"		, "")           // TCP/IP용 전문 길이 정보
                .put("tskGbCd"		, "SPS")        // 업무구분 코드
                .put("istnCd"		, "72")         // 기관 코드: 작업대상 기관 ("BC")
                .put("tgrmSubCCd"	, msgCode)       // 전문종별 코드: 0600 or 0610
                .put("trdGbCd"		, "")           // 거래구분 코드: FILE에 대한 업무 코드
                .put("sdRvFlag"		, "C")          // 'C': 현백, 'B': BC CARD
                .put("fileName"		, "")           // 파일명
                .put("respCd"		, "000")        // 000: 정상, 310: 송신자명 오류, 320: PW오류
                .put("trgmSendDtm"	, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")))
                .build();
        return  Maps.newHashMap(msg);
    }

    public byte[] build0610(String jobMngInfo){
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
                            .putAll(buildHeader("0610"))
                            .put("jobMngInfo"	, jobMngInfo)				// 업무관리 코드: 001[업무개시] / 002[파일송수신완료] / 003[파일 송수신완료] / 004 [업무종료]
                            .put("senderNm"		, "HDDEPT")
                            .put("senderPw"		, "HDDEPT")
                            .put("dataSize"		, "75")           // 26 + 49
                            .build();
        return buildEDI("BCC_0610", msg);
    }

    public byte[] build0640(){
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
                            .putAll(buildHeader("0640"))
                            .put("fileName"	, "")
                            .put("fileSize"		, "0")
                            .put("ediByte"		, "")
                            .build();
        return buildEDI("BCC_0640", msg);
    }
    
    public byte[] build0300(int blockNo, int lastSeqNo, int lostCnt, int lostChk){
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
                            .putAll(buildHeader("0300"))
                            .put("blockNo"		, blockNo)
                            .put("lastSeqNo"	, lastSeqNo)
                            .put("lostCnt"		, lostCnt)
                            .put("lostChk"		, lostChk)
                            .build();
        return buildEDI("BCC_0300", msg);
    }    
}

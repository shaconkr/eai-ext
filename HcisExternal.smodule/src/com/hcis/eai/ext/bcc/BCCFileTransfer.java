package com.hcis.eai.ext.bcc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hcis.eai.ext.EDIParserAndBuilder;

public class BCCFileTransfer extends EDIParserAndBuilder {
	
    protected String trCode = "";
	protected String msgCode = "";
	protected byte[] bytes = null;
	
	public BCCFileTransfer() {
		String beanioXml = "/Schemas/ext/bcc/BCCBatch.xml";
		String encoding = "euc-kr";
        init(beanioXml, encoding);
    }
    
    /**
     * Header 26 Bytes
     * @param msgCode
     * @return
     */
    private Map<String,Object> putCommon(String msgSize, String msgCode,String fileName){
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
                .put("dataSize"		, msgSize)           // TCP/IP용 전문 길이 정보 (전문길이(4)제외)
                .put("tskGbCd"		, "SPS")        // 업무구분 코드
                .put("istnCd"		, "72")         // 기관 코드: 작업대상 기관 ("BC")
                .put("tgrmSubCCd"	, msgCode)       // 전문종별 코드: 0600 or 0610
                .put("trdGbCd"		, "H")           // 거래구분 코드: FILE에 대한 업무 코드
                .put("sdRvFlag"		, "C")          // 'C': 현백, 'B': BC CARD
                .put("fileName"		, fileName)           // 파일명
                .put("respCd"		, "000")        // 000: 정상, 310: 송신자명 오류, 320: PW오류
                .put("trgmSendDtm"	, LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")))
                .build();
        return  Maps.newHashMap(msg);
    }

    public byte[] build0610(String queryString, String jobMngInfo){
    	Map<String,String> req = queryStringToMap(queryString);
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
                            .putAll(putCommon(req.get("dataSize"),"0610", req.get("fileName")))
                            .put("jobMngInfo"	, jobMngInfo)				// 업무관리 코드: 001[업무개시] / 002[파일송수신완료] / 003[파일 송수신완료] / 004 [업무종료]
                            .put("senderNm"		, "HDDEPT")
                            .put("senderPw"		, "HDDEPT")
//                            .put("dataSize"		, "71")           // 26 + 49 - 4
                            .build();
        return buildEDI("M_0610", msg);
    }

    public byte[] build0640(String queryString){
    	Map<String,String> req = queryStringToMap(queryString);
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
        					.putAll(putCommon(req.get("dataSize"),"0640", req.get("fileName")))
                            .put("fileName", req.get("fileName"))
                            .put("fileSize", req.get("fileSize"))
                            .put("ediByte",  req.get("ediByte"))
                            .build();
        return buildEDI("M_0640", msg);
    }
    
    public byte[] build0300(String queryString, String blockNo, String lastSeqNo, int lostCnt, String lostChk){
    	Map<String,String> req = queryStringToMap(queryString);
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
        					.putAll(putCommon("0","0300", req.get("fileName")))
                            .put("blockNo"		, blockNo)
                            .put("lastSeqNo"	, lastSeqNo)
                            .put("lostCnt"		, lostCnt)
                            .put("lostChk"		, lostChk)
                            .build();
        return buildEDI("M_0300", msg);
    }

	public String getTrCode() {
			return trCode;
		}
	public void setTrCode(String val) {
			trCode = val;
		}

	public String getMsgCode() {
			return msgCode;
		}
	public void setMsgCode(String val) {
			msgCode = val;
		}

	public byte[] getBytes() {
			return bytes;
		}
	public void setBytes(byte[] val) {
			bytes = val;
		}    
}

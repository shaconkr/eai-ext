package com.hcis.eai.ext.tos;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hcis.eai.ext.EDIParserAndBuilder;

import kr.shacon.util.CastUtils;

/**
 * 
 * AS-IS cron tab 
 * 
 * 가상게좌 개시전문 ( 당발 )
 * 
 * 0 1,4,5 * * * /app/hcis/bin/dacom_start 32 1 > dacom_start_32.out  부산
 * 0 1,4,5 * * * /app/hcis/bin/dacom_start 11 1 > dacom_start_11.out  농협
 * 0 1,4,5 * * * /app/hcis/bin/dacom_start 20 1 > dacom_start_20.out  우리
 * 0 1,4,5 * * * /app/hcis/bin/dacom_start 04 1 > dacom_start_04.out  국민
 * 0 1,4,5 * * * /app/hcis/bin/dacom_start 31 1 > dacom_start_31.out  대구
 * 25 신한은행 ( 타발 ) - 개시응답
 * 
 * 
 * 종료전문
 * 
 * 50 23 * * * /app/hcis/bin/dacom_start 20 3 > dacom_stop_20.out 우리
 * 51 23 * * * /app/hcis/bin/dacom_start 31 3 > dacom_stop_31.out 대구
 * 
 * 
 * 즉시출금 개시전문
 * /app/source/crpm/batch/da01/da15lnc0_t.pc 소스에서 전송하고 있고
 * 
 * 
 * 종료전문 전송시 집계내역을 전송하지는 않습니다.
 * 
 *
 */
public class TossOnline extends EDIParserAndBuilder {

    private static final String TOSS_ENCODING = "euc-kr";
    protected String trCode = "";
	protected String msgCode = "";
	protected byte[] bytes = null;
	
	Map<String,Object> BANK = Maps.newHashMap();
	String STAGE;
	
	public TossOnline(String beanioXml, String stage, String encoding) throws IOException {
        super(beanioXml, encoding);       
        BANK.putAll(loadJson("/Resources/ext/tos/TOSBank.json"));
        STAGE = stage;
    }

	/**
	 * 비동기 응답전문을 위한 correlation ID
	 * 
	 * 전문코드(ediCd :4) + 업무구분(jobGb :3) + 전문번호(ediNo :6) + 전송일자(sdDate:8)
	 *    
	 * @param bytes
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public String getCorrelationId(byte[] bytes) throws UnsupportedEncodingException {
		byte[] corrBytes = readBytes(bytes, 58, 21);
		return new String(corrBytes, TOSS_ENCODING);
	}
	
	
    /**
     * 공통부 생성
     * @param sdId		송신자ID
     * @param bnkCd		은행코드
     * @param ediCd		전문코드
     * @param jobGb		업무구분
     * @param ediNo		전문번호
     * @return
     */
	public Map<String,Object> putCommon(String bnkCd,  String ediCd, String jobGb, String ediNo){
    	
		String section = (ediCd.equals("0600600")) ? "bank" : "bank2";		
		Map<String,Object> bank = CastUtils.cast((Map<?,?>) CastUtils.cast((Map<?,?>) BANK.get(section)).get(STAGE));    	
    	String sdId = String.valueOf(bank.get("sdId"));
		Map<String,String> bankInfo = CastUtils.cast((Map<?,?>) CastUtils.cast((Map<?,?>) bank.get("info")).get(bnkCd));
		String svcGb = bankInfo.get("svcGb");    	
    	String bankCd = bnkCd.matches("10|11|12") ? "11" : bnkCd;  //농협은 공통부에 11로 강제 Set. 2008.04.02 강주원
        return Maps.newHashMap(ImmutableMap.<String,Object>builder()
                .put("svcGb"	, svcGb)    			// 서비스구분
                .put("sdFlag"	, "1")      			// 송신자FLAG  1요구 2응답
                .put("sdId"		, sdId)   				// 송신자ID
                .put("rvId"		, bankInfo.get("rvId"))     // 수신자ID
                .put("cpnyCd"	, bankInfo.get("cpnyCd"))   // 업체코드,고객코드
                .put("bnkCd"	, bankCd)   			// 은행코드
                .put("ediCd"	, ediCd)   				// 전문코드   0800
                .put("jobGb"	, jobGb)   				// 업무구분드  100
                .put("ediNo"	, ediNo)   				// 전문번호
                .put("sdDate"	, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))  				// 전송일자
                .put("sdTime"	, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH24mmss")))   				// 전송시간
                .build());
    }

	/**
	 * 업무개시
	 * @param sdId
	 * @param bnkCd
	 * @param ediNo
	 * @return
	 */
    public byte[] build_0800_100(String bnkCd, String ediNo) {
    	Map<String, Object> hdr = putCommon(bnkCd, "0800", "100", ediNo);	// 공통부
        Map<String, Object> dat = Maps.newHashMap(ImmutableMap.<String,Object>builder()
						        .put("jatongApplYn", "Y")				// 자통법적용구분
						        .put("commBnkCd", "0" + bnkCd)			// 공통부은행코드
						        .build());
        byte[] common = buildEDI("M_COMMON", hdr);
        byte[] data = buildEDI("M_0800_100", dat);        
        return concatBytes(common, data);
    }


	/**
	 * 업무종료
	 * @param sdId
	 * @param bnkCd
	 * @param ediNo
	 * @return
	 */
    public byte[] build_0800_300(String bnkCd, String ediNo) {
    	Map<String, Object> hdr = putCommon(bnkCd, "0800", "300", ediNo);	// 공통부
        Map<String, Object> dat = Maps.newHashMap(ImmutableMap.<String,Object>builder()
						        .put("jatongApplYn", "Y")				// 자통법적용구분
						        .put("commBnkCd", "0" + bnkCd)			// 공통부은행코드
						        .build());
        byte[] common = buildEDI("M_COMMON", hdr);
        byte[] data = buildEDI("M_0800_300", dat);        
        return concatBytes(common, data);
    }    
    
    /**
     * 통보응답
     * @param queryString
     * @return
     */
    public byte[] build110(String queryString) {
       	Map<String,String> req = queryStringToMap(queryString);
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
                            .putAll(req)
                            .put("jobGb"	, "110")
                            .build();
        return buildEDI("M_110", msg);
    }
    
   

}

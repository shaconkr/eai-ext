package com.hcis.eai.ext.tos;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hcis.eai.ext.EDIParserAndBuilder;

import kr.shacon.util.CastUtils;

public class TossOnline extends EDIParserAndBuilder {

    private static final String TOSS_ENCODING = "euc-kr";
    protected String trCode = "";
	protected String msgCode = "";
	protected byte[] bytes = null;
	
	private Map<String,Object> BANKLIST = Maps.newHashMap();
	
	protected Map<String,Object> COMMON = Maps.newHashMap();
	
	
	public TossOnline(String beanioXml, String stage, String encoding) throws IOException {
        super(beanioXml, encoding);
        
      //0은행코드, 1수신자id, 2업체코드, 3모계좌, 4모계좌비번, 5기관코드
        BANKLIST.putAll(CastUtils.cast((Map<?,?>) loadJson("/com/hcis/eai/ext/TOSOnlineBank.json").get(stage)));
        
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
	public Map<String,Object> putCommon(String sdId, String bnkCd,  String ediCd, String jobGb, String ediNo){
    	Map<String,String> bank = CastUtils.cast((Map<?,?>) BANKLIST.get(bnkCd));    	
    	String bankCd = bnkCd.matches("10|11|12") ? "11" : bnkCd;  //농협은 공통부에 11로 강제 Set. 2008.04.02 강주원
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
                .put("svcGb"	, "DY2")    			// 서비스구분
                .put("sdFlag"	, "1")      			// 송신자FLAG  1요구 2응답
                .put("sdId"		, sdId)   				// 송신자ID
                .put("rvId"		, bank.get("rvId"))     // 수신자ID
                .put("cpnyCd"	, bank.get("cpnyCd"))   // 업체코드,고객코드
                .put("bnkCd"	, bankCd)   			// 은행코드
                .put("ediCd"	, ediCd)   				// 전문코드
                .put("jobGb"	, jobGb)   				// 업무구분드
                .put("ediNo"	, ediNo)   				// 전문번호
                .put("sdDate"	, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))  				// 전송일자
                .put("sdTime"	, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH24mmss")))   				// 전송시간
                .build();
        COMMON.putAll(msg);
        return  COMMON;
    }

	/**
	 * 공통부 빈 항목 채우기
	 * @param commonJson
	 * @return
	 */
	public Map<String,Object> fillCommon(String commonJson){
		Map<String,Object> common = gson.fromJson(commonJson, Map.class);
		String bnkCd = (String) common.get("bnkCd");
    	Map<String,String> bank = CastUtils.cast((Map<?,?>) BANKLIST.get(bnkCd));    	
    	String bankCd = bnkCd.matches("10|11|12") ? "11" : bnkCd;  //농협은 공통부에 11로 강제 Set. 2008.04.02 강주원
    	
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
        		.putAll(common)
                .put("svcGb"	, "DY2")    			// 서비스구분
                .put("sdFlag"	, "1")      			// 송신자FLAG  1요구 2응답
//                .put("sdId"		, sdId)   				// 송신자ID
                .put("rvId"		, bank.get("rvId"))     // 수신자ID
                .put("cpnyCd"	, bank.get("cpnyCd"))   // 업체코드,고객코드
                .put("bnkCd"	, bankCd)   			// 은행코드
//                .put("ediCd"	, ediCd)   				// 전문코드
//                .put("jobGb"	, jobGb)   				// 업무구분드
//                .put("ediNo"	, ediNo)   				// 전문번호
                .put("sdDate"	, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")))  				// 전송일자
                .put("sdTime"	, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH24mmss")))   				// 전송시간
                .build();
        COMMON.putAll(msg);
        return  COMMON;
    }
	
	/**
	 * 업무개시
	 * @param sdId
	 * @param bnkCd
	 * @param ediNo
	 * @return
	 */
    public byte[] build_0800_100(String sdId, String bnkCd, String ediNo) {
    	Map<String, Object> hdr = putCommon(sdId, bnkCd, "0800", "100", ediNo);	// 공통부
        Map<String, Object> dat = Maps.newHashMap(ImmutableMap.<String,Object>builder()
						        .put("jatongApplYn", "Y")							// 자통법적용구분
						        .put("commBnkCd", COMMON.get("bnkCd"))				// 공통부은행코드
						        .build());

        byte[] common = buildEDI("M_COMMON", hdr);
        byte[] data = buildEDI("M_0800_100", dat);
        
        return concatBytes(common, data);
    }


    /**
     * 송신파일 통보응답
     * @param queryString
     * @return
     */
    public byte[] build110(String queryString) {
       	Map<String,String> req = queryStringToMap(queryString);
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
                            .putAll(req)
                            .put("trgmCd"	, "110")
                            .build();
        return buildEDI("M_110", msg);
    }
    
   

}

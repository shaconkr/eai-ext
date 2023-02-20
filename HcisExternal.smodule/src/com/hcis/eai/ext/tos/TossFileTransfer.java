package com.hcis.eai.ext.tos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hcis.eai.ext.EDIParserAndBuilder;

import kr.shacon.util.CastUtils;

public class TossFileTransfer extends EDIParserAndBuilder {

    private static final String TOSS_ENCODING = "euc-kr";
    protected String trCode = "";
	protected String msgCode = "";
	protected byte[] bytes = null;
	
	Map<String,Object> BANK = Maps.newHashMap();
	
	public TossFileTransfer() throws IOException {
		String beanioXml = "/Schemas/ext/tos/TOSBatch.xml";
		String encoding = "euc-kr";
        init(beanioXml, encoding);
        
        BANK.putAll(loadJson("/Resources/ext/tos/TOSBank.json"));
    }

    enum ERRCD {
        E000("정상"),
        E001("시스템 장애"),
        E002("USER ID 오류"),
        E003("PASSWORD ID 오류"),
        E004("JOB TYPE 오류"),
        E005("조건에 맞는 자료 없음"),
        E006("전문 종류 오류"),
        E007("전송 Bytes 오류"),
        E008("전송 형식 오류"),
        E009("PASSWORD Change 오류"),
        E099("기타 오류");

        private final String stringValue;

        ERRCD(final String s) {
            stringValue = s;
        }

        public String toString() {
            return stringValue;
        }
    }


    private Map<String,Object> putCommon(String ediNo){
        Map<String,Object> msg = ImmutableMap.<String,Object>builder()
                .put("trdCd"	, spaces(9))      // TR코드
                .put("trgmCd"	, ediNo)        // 전문번호
                .put("respCd"	, "000")        // 응답코드
                .put("workFld"	, spaces(20))   // 업무필드
                .put("filler1"	, spaces(5))    // 예비1
                .build();
        return  Maps.newHashMap(msg);
    }

    /**
     * 로그인 요구
     * @param jobType
     * @param startDay
     * @param endDay
     * @param fileName
     * @return
     */
    public byte[] build003(String jobType, String startDay, String endDay, String fileName) {
        Map<String, Object> msg = Maps.newHashMap(ImmutableMap.<String,Object>builder()
        						.putAll(putCommon("003"))		// 공통부
        						.put("userId", "HELP518")		// magiclink id
        						.put("passwd", "1111")
        						.put("jobType", jobType)		// SD 자료송신, RD 자료수신
        						.build());                
        if (jobType.equals("RD")) {
            msg.put("fileName", fileName);
            msg.put("flag", "E");
            msg.put("startTime", startDay + "0000");
            msg.put("endTime", endDay + "2400");
        }
        msg.put("chgPwdYn", "N");
        msg.put("commSize", 2048);
        msg.put("destId", spaces(10));        
        msg.put("filler", spaces(12));
        return buildEDI("M_003", msg);
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
    
    /**
     * 로그아웃 요구
     * @param jobType
     * @param startDay
     * @param endDay
     * @param fileName
     * @return
     */
    public byte[] build007(String jobType, String startDay, String endDay, String fileName) {
        Map<String, Object> msg = Maps.newHashMap(ImmutableMap.<String,Object>builder()
				.putAll(putCommon("003"))		// 공통부
				.put("userId", "HELP518")		// magiclink id
				.put("passwd", "1111")
				.put("jobType", jobType)		// SD 자료송신, RD 자료수신
				.build());                
		if (jobType.equals("RD")) {		// 송신
			msg.put("fileName", fileName);
			msg.put("flag", "E");
			msg.put("startTime", startDay + "0000");
			msg.put("endTime", endDay + "2400");
		}
        msg.put("filler", spaces(35));        
		return buildEDI("M_007", msg);
    }

    /**
     * 송신 파일 통보
     * @param filename
     * @param filesize
     * @param lastYn
     * @return
     */
    public byte[]  build100(String filename, String filesize, String lastYn) {
        Map<String, Object> msg = Maps.newLinkedHashMap(putCommon("100"));
        msg.put("fileName", filename);
        msg.put("fileSize", filesize);
//        msg.put("sndId", spaces(20));
//        msg.put("rcvId", spaces(20));
//        msg.put("filler1", spaces(1));
//        msg.put("filler2", spaces(10));
        msg.put("lastYn", lastYn);		// NXT 다음에 전송할 파일있음, END 다음에 전송할 파일 없음
        msg.put("commTy", "NEW");  		// NEW 일반적인 송수신 , APP 이어보내기받기(사용안함)
//        msg.put("endTime", spaces(12));
//        msg.put("filler3", spaces(1));
//        msg.put("filler4", spaces(22));
		return buildEDI("M_100", msg);
    }

    /**
     * 송신 파일 통보 응답
     * @param filename
     * @param filesize
     * @param lastYn
     * @return
     */
    public byte[]  build110(String filename, String filesize, String lastYn) {
        Map<String, Object> msg = Maps.newLinkedHashMap(putCommon("100"));
        msg.put("fileName", filename);
        msg.put("fileSize", filesize);
//        msg.put("sndId", spaces(20));
//        msg.put("rcvId", spaces(20));
//        msg.put("filler1", spaces(1));
//        msg.put("filler2", spaces(10));
        msg.put("lastYn", lastYn);		// NXT 다음에 전송할 파일있음, END 다음에 전송할 파일 없음
        msg.put("commTy", "NEW");  		// NEW 일반적인 송수신 , APP 이어보내기받기(사용안함)
//        msg.put("endTime", spaces(12));
//        msg.put("filler3", spaces(1));
//        msg.put("filler4", spaces(22));
		return buildEDI("M_110", msg);
    }

    /**
     * 수신 확인
     * @param filename
     * @param filesize
     * @param procYn
     * @return
     */
    public byte[]  build130(String filename, String filesize, String procYn) {
        Map<String, Object> msg = Maps.newLinkedHashMap(putCommon("130"));
        msg.put("fileName", filename);
        msg.put("fileSize", filesize);
        msg.put("procYn", procYn);
//      msg.put("filler1", spaces(79));        
		return buildEDI("M_110", msg);
    }

    /**
     * TOSS EDI 수신 목록
     * @return
     */
    public ArrayList<String> getTOSRecvEdiList() {
        ArrayList<String> aList = new ArrayList<String>();
        List<Map<String,Object>> lst = CastUtils.cast((List<?>) BANK.get("RecvEdiList"));
        lst.stream().forEach(maps -> {
            maps.entrySet().forEach(map -> {
                Map<String,String> row = CastUtils.cast((Map<?,?>) map.getValue());
                String qStr = Joiner.on("&").withKeyValueSeparator("=").join(row);
                aList.add(qStr);                    
            });
        });
        
        return aList;
    }   

}

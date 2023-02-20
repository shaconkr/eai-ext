package com.hcis.eai.ext.kft;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.beanio.Marshaller;
import org.beanio.StreamFactory;
import org.beanio.internal.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.hcis.eai.ext.EDIParserAndBuilder;

public class KftcFileTransfer extends EDIParserAndBuilder  {
    private static final Logger LOGGER = LoggerFactory.getLogger(KftcFileTransfer.class);

    private static final String KFTC_COMPANY_CD = "52100280";                   // 기관코드(현대백화점) : 파일내에서는 9952100280
    private static final String KFTC_SENDER_NM = "HYUNDAIDEPARTMENT   ";
    //    private static final String KFTC_SENDER_PW = "005006";            // for real
    private static final String KFTC_SENDER_PW = "111111";      // for test

    private static final String KFTC_EDI_BYTE = "4096";
    private static final String KFTC_SD = "1";
    private static final String KFTC_RV = "2";
    private static final String KFTC_OK = "0";

    private static final String KFTC_ENCODING = "euc-kr";

    protected String trCode = "";
	protected String msgCode = "";
	protected byte[] bytes = null;
	
	public KftcFileTransfer() {
		String beanioXml = "/Schemas/ext/kft/KFTBatch.xml";
		String encoding = "euc-kr";
        init(beanioXml, encoding);
    }       
	
	
    public byte[] kftc600(String blnSdRv, String jobMngInfo) throws UnsupportedEncodingException {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0600", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("jobMngInfo", jobMngInfo);
        String sendDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        msg.put("senderNm", KFTC_SENDER_NM);
        msg.put("senderPw", senderEncrypt(KFTC_SENDER_NM, KFTC_SENDER_PW, KFTC_COMPANY_CD, sendDt));
        return setTotalLength(marshall("M_0600", msg).getBytes(KFTC_ENCODING), 4, true, KFTC_ENCODING);
    }

    public byte[] kftc610(String queryString, String blnSdRv, String jobMngInfo) throws UnsupportedEncodingException {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0610", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("jobMngInfo", jobMngInfo);
        String sendDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        msg.put("senderNm", KFTC_SENDER_NM);
        msg.put("senderPw", senderEncrypt(KFTC_SENDER_NM, KFTC_SENDER_PW, KFTC_COMPANY_CD, sendDt));
        return setTotalLength(marshall("M_0610", msg).getBytes(KFTC_ENCODING), 4, true, KFTC_ENCODING);
    }

    public byte[] kftc630(String queryString, String blnSdRv, String fileName, String fileSize) throws UnsupportedEncodingException {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0630", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("fileName", fileName);
        msg.put("fileSize", fileSize);
        msg.put("ediByte", KFTC_EDI_BYTE);
        return setTotalLength(marshall("M_0630", msg).getBytes(KFTC_ENCODING), 4, true, KFTC_ENCODING);
    }

    public byte[] kftc640(String queryString, String blnSdRv) throws UnsupportedEncodingException {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0640", trdGbCd, "E", spaces(8), "000");
        Map<String, String> dat = queryStringToMap(queryString);
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("fileName", dat.get("fileName"));
        msg.put("fileSize", dat.get("fileSize"));
        msg.put("ediByte", KFTC_EDI_BYTE);
        return setTotalLength(marshall("KFTC_0640", msg).getBytes(KFTC_ENCODING), 4, true, KFTC_ENCODING);
    }

    public byte[] kftc620(String queryString, String blnSdRv, String fileName, String blockNo, String lastSeqNo) throws UnsupportedEncodingException {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0620", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("fileName", fileName);
        msg.put("blockNo", blockNo);
        msg.put("lastSeqNo", lastSeqNo);
        return setTotalLength(marshall("M_0620", msg).getBytes(KFTC_ENCODING), 4, true, KFTC_ENCODING);
    }

    public byte[] kftc300(String queryString, String blnSdRv, String blockNo, String lastSeqNo, String lostCnt, String lostChk) throws UnsupportedEncodingException {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0300", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("blockNo", blockNo);
        msg.put("lastSeqNo", lastSeqNo);
        msg.put("lostCnt", lostCnt);
        msg.put("lostChk", lostChk);
        return setTotalLength(marshall("M_0300", msg).getBytes(KFTC_ENCODING), 4, true, KFTC_ENCODING);
    }

    public byte[] kftc310(String queryString, String blnSdRv, String blockNo, String seqNo, String realDataByte, String fileSpec) throws UnsupportedEncodingException {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0310", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("blockNo", blockNo);
        msg.put("seqNo", seqNo);
        msg.put("realDataByte", realDataByte);
        msg.put("fileSpec", fileSpec);
        return setTotalLength(marshall("M_0310", msg).getBytes(KFTC_ENCODING), 4, true, KFTC_ENCODING);
    }

    public byte[] kftc320(String queryString, String blnSdRv, String fileName, String blockNo, String seqNo, String realDataByte, String fileSpec) throws UnsupportedEncodingException {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0320", trdGbCd, "E", fileName, "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("blockNo", blockNo);
        msg.put("seqNo", seqNo);
        msg.put("realDataByte", realDataByte);
        msg.put("fileSpec", fileSpec);
        return setTotalLength(marshall("M_0320", msg).getBytes(KFTC_ENCODING), 4, true, KFTC_ENCODING);
    }

    /**
     * @param sendByte   송수신Byte
     * @param tgrmSubCCd 전문종별코드
     * @param trdGbCd    거래구분코드
     * @param sdRvFlag   송수신플래그
     * @param fileName   파일명
     * @param respCd     응답코드
     * @return 공통부
     */
    public Map<String, Object> putCommon(long sendByte, String tgrmSubCCd, String trdGbCd, String sdRvFlag, String fileName, String respCd) {
        return Maps.newHashMap(ImmutableMap.<String, Object>builder()
                .put("sendByte", sendByte)			// 송수신 바이트,4
                .put("tskGbCd", "FTE")				// 업무구분코드, 3
                .put("istnCd", KFTC_COMPANY_CD)		// 기관코드, 8
                .put("tgrmSubCCd", tgrmSubCCd)		// 전문종별코드, 4
                .put("trdGbCd", trdGbCd)			// 거래구분코드, 1  - 센터수신 R , 센터송신 S
                .put("sdRvFlag", sdRvFlag)			// 송수신플래그,1  - 센터전문발생 C , 기관전분발생 E
                .put("fileName", fileName)			// 파일명, 8
                .put("respCd", respCd)				// 응답코드, 3
                .build());
    }

    /**
     * @param senderNm  송신자명
     * @param planPw    송신자암호평문
     * @param companyCd 기관코드
     * @param sendDt    거래일
     * @return 송신자암호암호문
     */
    public String senderEncrypt(String senderNm, String planPw, String companyCd, String sendDt) {
        int M = 36;                          // Mudulus
        String senderPw = planPw + planPw + planPw.substring(0, 4);
        String inStr = companyCd.substring(0, 1) + companyCd.substring(7, 8) + sendDt + senderNm;

        char[] P = putConv(senderPw.toCharArray());         // 평문 대수치
        char[] K = putConv(inStr.toCharArray());           // 암호키 대수치

        char[] C = new char[inStr.length()];
        for (int i = 0; i < inStr.length(); i++) {
            int p = Character.getNumericValue(P[i]);
            int k = Character.getNumericValue(K[i]);
            int c = (p + k) % M;
            C[i] = (char) ((P[i] + K[i]) % M);
        }
        String enc = String.valueOf(getConv(C));
        return enc;
    }


    public String senderDecrypt(String senderNm, String encPass, String companyCd, String sendDt) {
        int M = 36;                          // Mudulus
        String inStr = companyCd.substring(0, 1) + companyCd.substring(7, 8) + sendDt + senderNm;

        char[] C = putConv(encPass.toCharArray());       // 암호문 대수치
        char[] K = putConv(inStr.toCharArray());          // 암호키 대수치

        char[] P = new char[inStr.length()];
        for (int i = 0; i < inStr.length(); i++) {
            P[i] = (char) ((C[i] + M - K[i]) % M);
        }
        return String.valueOf(getConv(P));
    }

    private char[] putConv(char[] ch) {
        char[] cv = new char[ch.length];
        for (int i = 0; i < ch.length; i++) {
            if (ch[i] >= '0' && ch[i] <= '9') cv[i] = (char) (57 - ch[i]);
            else if (ch[i] >= 'a' && ch[i] <= 'z') cv[i] = (char) (132 - ch[i]);
            else if (ch[i] >= 'A' && ch[i] <= 'Z') cv[i] = (char) (100 - ch[i]);
        }
        return cv;
    }

    private char[] getConv(char[] ch) {
        char[] cv = new char[ch.length];
        for (int i = 0; i < ch.length; i++) {
            if (ch[i] >= 10 && ch[i] <= 35) cv[i] = (char) (100 - ch[i]);
            else if (ch[i] >= 0 && ch[i] <= 9) cv[i] = (char) (57 - ch[i]);
        }
        return cv;
    }

    public String marshall(String msgType, Map<String, Object> map) {
        Marshaller marshaller = factory.createMarshaller(msgType);
        return marshaller.marshal(map, KFTC_ENCODING).toString();
    }

    public String spaces(int len) {
        return Strings.padStart(" ", len, ' ');
    }
}

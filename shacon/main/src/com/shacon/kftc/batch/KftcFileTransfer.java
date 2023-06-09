package com.shacon.kftc.batch;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.beanio.Marshaller;
import org.beanio.StreamFactory;
import org.beanio.internal.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class KftcFileTransfer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KftcFileTransfer.class);
    StreamFactory factory = newStreamFactory("kftcbatch.xml");

    private static final String KFTC_COMPANY_CD = "52100280";                   // 기관코드(현대백화점) : 파일내에서는 9952100280
    private static final String KFTC_SENDER_NM = "HYUNDAIDEPARTMENT   ";
    //    private static final String KFTC_SENDER_PW = "005006";            // for real
    private static final String KFTC_SENDER_PW = "111111";      // for test

    private static final String KFTC_EDI_BYTE = "4096";
    private static final String KFTC_SD = "1";
    private static final String KFTC_RV = "2";
    private static final String KFTC_OK = "0";

    enum ERRCD {
        E000("정상"),
        E090("시스템 장애"),
        E310("송신자명 오류"),
        E320("송신자암호 오류"),
        E630("기전송 완료"),
        E631("미등록 업무"),
        E632("비정상 파일명"),
        E633("비정상 전문 BYTE 수"),
        E634("파일송신 가능시간/일자 완료"),
        E635("E13파일 검증완료 전 EB13파일 수신"),
        E800("FORMAT 오류");

        private final String stringValue;

        ERRCD(final String s) {
            stringValue = s;
        }

        public String toString() {
            return stringValue;
        }
    }

    public String kftc600(Map<String, Object> common, String blnSdRv, String jobMngInfo) {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0600", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("jobMngInfo", jobMngInfo);
        String sendDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        msg.put("senderNm", KFTC_SENDER_NM);
        msg.put("senderPw", senderEncrypt(KFTC_SENDER_NM, KFTC_SENDER_PW, KFTC_COMPANY_CD, sendDt));
        return marshall("KFTC_0600", msg);
    }

    public String kftc610(Map<String, Object> common, String blnSdRv, String jobMngInfo) {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0610", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("jobMngInfo", jobMngInfo);
        String sendDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        msg.put("senderNm", KFTC_SENDER_NM);
        msg.put("senderPw", senderEncrypt(KFTC_SENDER_NM, KFTC_SENDER_PW, KFTC_COMPANY_CD, sendDt));
        return marshall("KFTC_0610", msg);
    }

    public String kftc630(Map<String, Object> common, String blnSdRv, String fileName, String fileSize) {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0630", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("fileName", fileName);
        msg.put("fileSize", fileSize);
        msg.put("fileSize", KFTC_EDI_BYTE);
        return marshall("KFTC_0630", msg);
    }

    public String kftc640(Map<String, Object> common, String blnSdRv, String fileName, String fileSize) {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0640", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("fileName", fileName);
        msg.put("fileSize", fileSize);
        msg.put("fileSize", KFTC_EDI_BYTE);
        return marshall("KFTC_0640", msg);
    }

    public String kftc620(Map<String, Object> common, String blnSdRv, String fileName, String blockNo, String lastSeqNo) {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0620", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("fileName", fileName);
        msg.put("blockNo", blockNo);
        msg.put("lastSeqNo", lastSeqNo);
        return marshall("KFTC_0620", msg);
    }

    public String kftc300(Map<String, Object> common, String blnSdRv, String blockNo, String lastSeqNo, String lostCnt, String lostChk) {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0300", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("blockNo", blockNo);
        msg.put("lastSeqNo", lastSeqNo);
        msg.put("lostCnt", lostCnt);
        msg.put("lostChk", lostChk);
        return marshall("KFTC_0300", msg);
    }

    public String kftc310(Map<String, Object> common, String blnSdRv, String blockNo, String seqNo, String realDataByte, String fileSpec) {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0310", trdGbCd, "E", spaces(8), "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("blockNo", blockNo);
        msg.put("seqNo", seqNo);
        msg.put("realDataByte", realDataByte);
        msg.put("fileSpec", fileSpec);
        return marshall("KFTC_0310", msg);
    }

    public String kftc320(Map<String, Object> common, String blnSdRv, String fileName, String blockNo, String seqNo, String realDataByte, String fileSpec) {
        String trdGbCd = (blnSdRv.equals(KFTC_SD)) ? "R" : "S";
        Map<String, Object> msg = putCommon(0, "0320", trdGbCd, "E", fileName, "000");
        msg.put("trgmSendDtm", LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHH24mmss")));
        msg.put("blockNo", blockNo);
        msg.put("seqNo", seqNo);
        msg.put("realDataByte", realDataByte);
        msg.put("fileSpec", fileSpec);
        return marshall("KFTC_0320", msg);
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
    private Map<String, Object> putCommon(long sendByte, String tgrmSubCCd, String trdGbCd, String sdRvFlag, String fileName, String respCd) {
        return Maps.newHashMap(ImmutableMap.<String, Object>builder()
                .put("sendByte", sendByte)
                .put("tskGbCd", "FTE")
                .put("istnCd", KFTC_COMPANY_CD)
                .put("tgrmSubCCd", tgrmSubCCd)
                .put("trdGbCd", trdGbCd)
                .put("sdRvFlag", sdRvFlag)
                .put("fileName", fileName)
                .put("respCd", respCd)
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
        return marshaller.marshal(map).toString();
    }

    private StreamFactory newStreamFactory(String config) {
        StreamFactory factory = StreamFactory.newInstance();
        InputStream is = getClass().getResourceAsStream(config);
        try {
            factory.load(is);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtil.closeQuietly(is);
        }
        return factory;
    }

    public String spaces(int len) {
        return Strings.padStart(" ", len, ' ');
    }
}

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
    private String senderEncrypt(String senderNm, String planPw, String companyCd, String sendDt) {
        int M = 36;                          // Mudulus
        String senderPw = planPw + planPw + planPw.substring(0, 6);
        String inStr = companyCd.charAt(0) + companyCd.charAt(7) + sendDt + senderNm;

        String P = putConv(senderPw);         // 평문 대수치
        String K = putConv(inStr);           // 암호키 대수치

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inStr.length(); i++) {
            sb.append((P.charAt(i) + K.charAt(i)) % M);
        }
        String C = sb.toString();           // 암호문 대수치
        return getConv(C, C.length());
    }


    private String senderDecrypt(String senderNm, String encPass, String companyCd, String sendDt) {
        int M = 36;                          // Mudulus
        String inStr = companyCd.charAt(0) + companyCd.charAt(7) + sendDt + senderNm;

        String C = putConv(encPass);       // 암호문 대수치
        String K = putConv(inStr);          // 암호키 대수치


        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < inStr.length(); i++) {
            sb.append((C.charAt(i) + M - K.charAt(i)) % M);
        }
        String P = sb.toString();           // 평문 대수치
        return getConv(P, P.length());

    }


    private String putConv(String s) {
        StringBuilder sb = new StringBuilder();
        for (Character ch : Lists.charactersOf(s)) {
            if (ch >= '0' && ch <= '9') sb.append(57 - ch);
            else if (ch >= 'a' && ch <= 'z') sb.append(132 - ch);
            else if (ch >= 'A' && ch <= 'Z') sb.append(100 - ch);
        }
        return sb.toString();
    }

    private String getConv(String s, int size) {
        StringBuilder sb = new StringBuilder();
        char[] ch = s.toCharArray();
        for (int i = 0; i < size; i++) {
            int c = Character.getNumericValue(ch[i]);
            if (c >= 10 && c <= 35) sb.append(100 - ch[i]);
            else if (c >= 0 && c <= 9) sb.append(57 - ch[i]);
        }
        return sb.toString();
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

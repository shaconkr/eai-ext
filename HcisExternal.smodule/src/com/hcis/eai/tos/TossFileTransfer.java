package com.hcis.eai.tos;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import kr.shacon.edi.util.CastUtils;
import org.beanio.Marshaller;
import org.beanio.StreamFactory;
import org.beanio.Unmarshaller;
import org.beanio.internal.util.IOUtil;

import java.io.InputStream;
import java.util.Map;

public class TossFileTransfer {

    private static final String TOSS_ENCODING = "euc-kr";

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

    StreamFactory factory = newStreamFactory("/com/hcis/eai/tos/TOSBatch.xml");


    public String getT003(String jobType, String startDay, String endDay, String fileName) {
        Map<String, Object> msg = Maps.newLinkedHashMap(putCommon("003"));
        msg.put("userId", "HELP518");
        msg.put("passwd", "1111");
        msg.put("jobType", jobType);
        if (jobType.equals("SD")) {
            msg.put("fileName", spaces(20));
            msg.put("flag", spaces(1));
            msg.put("startTime", spaces(12));
            msg.put("endTime", spaces(12));
        } else {
            msg.put("fileName", fileName);
            msg.put("flag", "E");
            msg.put("startTime", startDay + "0000");
            msg.put("endTime", endDay + "2400");
        }
        msg.put("chgPwdYn", "N");
        msg.put("commSize", 2048);
        msg.put("destId", spaces(10));
        return marshall("TOSS_003", msg);
    }

    public String getRespStatus(String edi) {
        Map<String, Object> msg = unmarshall("TOSS_030", edi);
        String retCd = String.valueOf(msg.get("respCd"));
        String retMsg = ERRCD.valueOf("E" + retCd).toString();
        //TODO add LOG
        return retCd;
    }

    public String getT007(String jobType, String startDay, String endDay, String fileName) {
        Map<String, Object> msg = Maps.newLinkedHashMap(putCommon("007"));
        msg.put("userId", "HELP518");
        msg.put("passwd", "1111");
        msg.put("jobType", jobType);
        if (jobType.equals("SD")) {
            msg.put("fileName", spaces(20));
            msg.put("flag", spaces(1));
            msg.put("startTime", spaces(12));
            msg.put("endTime", spaces(12));
        } else {
            msg.put("fileName", fileName);
            msg.put("flag", "E");
            msg.put("startTime", startDay + "0000");
            msg.put("endTime", endDay + "2400");
        }
        return marshall("TOSS_007", msg);
    }

    public String logoutRes(String edi) {
        Map<String, Object> msg = unmarshall("TOSS_070", edi);
        String retCd = String.valueOf(msg.get("respCd"));
        String retMsg = ERRCD.valueOf("E" + retCd).toString();
        //TODO add LOG
        return retCd;
    }

    public String getT100(String filename, String filesize, String lastYn) {
        Map<String, Object> msg = Maps.newLinkedHashMap(putCommon("100"));
        msg.put("fileName", filename);
        msg.put("fileSize", filesize);
        msg.put("sndId", spaces(20));
        msg.put("rcvId", spaces(20));
        msg.put("lastYn", lastYn);
        msg.put("commTy", "NEW");  // APP 이어받기 사용안함
        msg.put("endTime", spaces(12));
        return marshall("TOSS_100", msg);
    }

    public String sendFileRes(String edi) {
        Map<String, Object> msg = unmarshall("TOSS_110", edi);
        String retCd = String.valueOf(msg.get("respCd"));
        String retMsg = ERRCD.valueOf("E" + retCd).toString();
        //TODO add LOG
        return retCd;
    }

    public String rcvConfirm(String fileName, String fileSize, String procYn) {
        Map<String, Object> msg = Maps.newLinkedHashMap(putCommon("130"));
        msg.put("fileName", fileName);
        msg.put("fileSize", fileSize);
        msg.put("procYn", procYn);
        return marshall("TOSS_130", msg);
    }

    public Map<String, Object> putCommon(String trgmCd) {
        return ImmutableMap.<String, Object>builder().put("trdCd", spaces(9))
                .put("trgmCd", trgmCd).put("respCd", "000").put("workFld", spaces(20))
                .put("filler1", spaces(5)).build();
    }

    public String spaces(int len) {
        return Strings.padStart(" ", len, ' ');
    }


    /**
     * EDI to Map
     *
     * @param msgType loginReq, loginRes, logoutReq, logoutRes, sendFileNoti, sendFileRes, rcvConfirm
     * @param rec
     * @return
     */
    public Map<String, Object> unmarshall(String msgType, String rec) {
        Unmarshaller unmarshaller = factory.createUnmarshaller(msgType);
        return CastUtils.cast((Map<?, ?>) unmarshaller.unmarshal(rec, TOSS_ENCODING));
    }

    /**
     * @param msgType loginReq, loginRes, logoutReq, logoutRes, sendFileNoti, sendFileRes, rcvConfirm
     * @param map
     * @return
     */
    public String marshall(String msgType, Map<String, Object> map) {
        Marshaller marshaller = factory.createMarshaller(msgType);
        return marshaller.marshal(map, TOSS_ENCODING).toString();
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
}

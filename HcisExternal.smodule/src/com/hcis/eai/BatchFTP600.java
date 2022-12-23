package com.hcis.eai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.shacon.edi.type.MapDeserializer;
import kr.shacon.edi.util.CastUtils;
import org.beanio.Marshaller;
import org.beanio.StreamFactory;
import org.beanio.Unmarshaller;
import org.beanio.internal.util.IOUtil;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

public class BatchFTP600 {

    protected StreamFactory factory;
    protected String encoding;
    protected static Gson gson;

    static {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Map.class, new MapDeserializer())
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializeNulls()
                .create();
    }

    public BatchFTP600(String beanioXml, String encoding) {
        this.factory = newStreamFactory(beanioXml);
        this.encoding = encoding;
    }

    public byte[] buildEDI(String msgType, Map<String,Object> msgMap){
        Marshaller marshaller = factory.createMarshaller(msgType);
        String edi = marshaller.marshal(msgMap,encoding).toString();
        byte[] bytes = new byte[0];
        try {
            bytes = edi.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public byte[] buildEDIfromJson(String msgType, String jsonString)  {
        return buildEDI(msgType, gson.fromJson(jsonString, Map.class));
    }

    public String parseEDItoJson(String msgType, byte[] ediBytes)  {
        return gson.toJson( parseEDI(msgType, ediBytes), Map.class);
    }

    public Map<String,Object> parseEDI(String msgType, byte[] ediBytes){
        Unmarshaller unmarshaller = factory.createUnmarshaller(msgType);
        Map<String,Object>  msgMap= null;
        try {
            msgMap = CastUtils.cast((Map<?,?>) unmarshaller.unmarshal(new String(ediBytes, encoding), encoding));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return msgMap;
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
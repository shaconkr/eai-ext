package com.hcis.eai.ext;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.shacon.types.MapDeserializer;
import kr.shacon.util.CastUtils;
import org.beanio.Marshaller;
import org.beanio.StreamFactory;
import org.beanio.Unmarshaller;
import org.beanio.internal.util.IOUtil;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class EDIParserAndBuilder {

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
    
    public EDIParserAndBuilder() {
    }
    
    public void init(String beanioXml, String encoding) {
        this.factory = newStreamFactory(beanioXml);
        this.encoding = encoding;
    }
    
    public EDIParserAndBuilder(String beanioXml, String encoding) {
        this.factory = newStreamFactory(beanioXml);
        this.encoding = encoding;
    }

    public String getHeaderQueryString(byte[] bytes){
    	Map<String,String> head = CastUtils.cast((Map<?,?>)parseEDI("M_COMMON", bytes));
//    	return  head.entrySet().stream().map(Object::toString).collect(Collectors.joining("&"));
    	return  Joiner.on("&").withKeyValueSeparator("=").join(head);       
    }

    public String getDataQueryString(byte[] bytes, String msgCode){
        Map<String,String> data = CastUtils.cast((Map<?,?>)parseEDI("M_"+msgCode, bytes));        
        return Joiner.on("&").withKeyValueSeparator("=").join(data);
    }
    public String getHeaderJson(byte[] bytes){
    	Map<String,String> head = CastUtils.cast((Map<?,?>)parseEDI("COMMON", bytes));
    	return  gson.toJson(head);       
    }

    public String getDataJson(byte[] bytes, String msgCode){
        Map<String,String> data = CastUtils.cast((Map<?,?>)parseEDI("M_"+msgCode, bytes));        
        return gson.toJson(data);
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
    
	protected String getQueryStringValue(String queryString, String key )  {
		Map<String, String> queryMap = Arrays.stream(queryString.split("&")).map(s -> s.split("=")).collect(Collectors.toMap(s -> s[0], s -> s[1]));		
		return queryMap.get(key);
	}

	protected Map<String,String> queryStringToMap(String queryString)  {
		return Arrays.stream(queryString.split("&")).map(s -> s.split("=")).collect(Collectors.toMap(s -> s[0], s -> s[1]));		
	}
	
	
	protected byte[] concatBytes(byte[] bytes1, byte[] bytes2) {
        byte[] bytes = new byte[bytes1.length + bytes2.length];
        System.arraycopy(bytes1, 0, bytes, 0 , bytes1.length);
        System.arraycopy(bytes2, 0, bytes, bytes1.length , bytes2.length);
        return bytes;
    }
	
	@SuppressWarnings("unchecked")
	protected String concatJson(String json1, String json2) {
		Map<String,Object> map1 = gson.fromJson(json1, Map.class);
		Map<String,Object> map2 = gson.fromJson(json2, Map.class);
		return gson.toJson(Maps.newLinkedHashMap(ImmutableMap.<String, Object>builder().putAll(map1).putAll(map2).build()));		
	}
	
	protected byte[] setTotalLength(byte[] bytes, int len, String encoding) throws UnsupportedEncodingException {
		int byteLen = bytes.length - len;
        byte[] totalLength = String.format("%0"+len +"d", byteLen).getBytes(encoding);
        System.arraycopy(totalLength, 0, bytes, 0, totalLength.length);	
        return bytes;
	}	
	
    protected String spaces(int len) {
        return Strings.padStart(" ", len, ' ');
    }

}
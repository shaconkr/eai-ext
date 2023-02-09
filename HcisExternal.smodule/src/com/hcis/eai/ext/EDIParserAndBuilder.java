package com.hcis.eai.ext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.beanio.Marshaller;
import org.beanio.StreamFactory;
import org.beanio.Unmarshaller;
import org.beanio.internal.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import kr.shacon.types.MapDeserializer;
import kr.shacon.util.CastUtils;

public class EDIParserAndBuilder {
	
    private static final Logger log = LoggerFactory.getLogger(EDIParserAndBuilder.class);

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

    public String mapToQueryString(Map<String,String> p) {
    	return Joiner.on("&").withKeyValueSeparator("=").join(p);
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

    protected StreamFactory newStreamFactory(String config) {
    	log.debug("## beanIO xml path : [{}]", config);
    	log.debug("## CLASSPATH {}", System.getProperty("java.class.path"));
    	
    	
        StreamFactory factory = StreamFactory.newInstance();
        InputStream is = getClass().getResourceAsStream(config);
        
        try {
//        	String xml = new java.io.BufferedReader(
//        			new java.io.InputStreamReader(is,StandardCharsets.UTF_8))
//        			.lines()
//        			.collect(Collectors.joining("\n"));
//        	log.debug("## LOAD beanIO xml path : [{}] {}", config, xml);
        	
            factory.load(is);
            
        } catch (Exception e) {
        	log.error("## ERROR beanIO xml path : [{}]", config);
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
	
	
	public byte[] concatBytes(byte[] bytes1, byte[] bytes2) {
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
	
	/**
	 * 전체전문길이 설정
	 * @param bytes 
	 * @param len       전체전문길이 항목의 길이
	 * @param flag      포함하지 않음 true
	 * @param encoding
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	protected byte[] setTotalLength(byte[] bytes, int len, boolean flag, String encoding) throws UnsupportedEncodingException {		
		int byteLen = (flag) ? bytes.length - len : bytes.length;		
        byte[] totalLength = String.format("%0"+len +"d", byteLen).getBytes(encoding);
        System.arraycopy(totalLength, 0, bytes, 0, totalLength.length);	
        return bytes;
	}	
	
    protected String spaces(int len) {
        return Strings.padStart(" ", len, ' ');
    }

    public int getBytesLength(byte[] bytes)  {
	    return  bytes.length;
	}
	
    public byte[] readBytes(byte[] bytes, int offset, int len)  {
		byte[] readBytes = new byte[len];
		System.arraycopy(bytes, offset, readBytes, 0, len);
	    return readBytes;
	}	
    
    public String readBytesString(byte[] bytes, int offset, int len) throws UnsupportedEncodingException  {
    	return new String(readBytes(bytes, offset, len), "euc-kr");
    }
    
    /**
     * Byte Stream 을 Record 길이 단위로 
     * 개행문자 UNIX(0A) 삽입
     * 참고 MAC(0D) , WIN(0D0A)
     * 
     * @param bytes
     * @param recordLength
     * @return
     * @throws IOException
     */
    public byte[] chunkRecord(byte[] bytes, int recordLength) throws IOException  {    	
    	int recordCount = bytes.length / recordLength;
    	ByteArrayInputStream  in = new ByteArrayInputStream(bytes);
    	ByteArrayOutputStream out = new ByteArrayOutputStream();    		
		for(int i=0;i<recordCount;i++) {
			out.write(in.readNBytes(recordLength));
			out.write(0x0a);		// \n
		}
		return out.toByteArray();
	}	
    
    @SuppressWarnings("unchecked")
	protected Map<String, Object> loadJson(String jsonFile) throws IOException {
    	InputStream is = getClass().getResourceAsStream(jsonFile);       
    	String json = new java.io.BufferedReader(
    			new java.io.InputStreamReader(is,StandardCharsets.UTF_8))
    			.lines()
    			.collect(Collectors.joining("\n"));
    	log.debug("## {}",json);
	    return gson.fromJson(json, Map.class);
    }

}
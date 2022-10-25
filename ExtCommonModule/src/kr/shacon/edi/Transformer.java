package kr.shacon.edi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import kr.shacon.edi.type.IntegerTypeAdapter;
import kr.shacon.edi.type.LongTypeAdapter;
import kr.shacon.edi.type.MapDeserializer;
import org.xml.sax.InputSource;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Transformer {

    protected static Gson gson;

    static {
        gson = new GsonBuilder()
//                .registerTypeAdapter(Long.class, new LongTypeAdapter())
//                .registerTypeAdapter(Integer.class, new IntegerTypeAdapter())
                .registerTypeAdapter(Map.class, new MapDeserializer())
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializeNulls()
                .create();
    }

    public Transformer() {
    }

    /**
     * JSON to EDI
     *
     * @param xsdFile    EDI layout
     * @param jsonString JSON String
     * @return EDI String
     */
    public String toEDI(String xsdFile, String jsonString) {
        return toEDI(toInputSource(xsdFile), jsonString);
    }

    public String toEDI(InputSource is, String jsonString) {
        Marshaller<Map<String, Object>> marshaller = new Marshaller<>(loadDefinition(is));
        StringWriter sw = new StringWriter();
        marshaller.marshall(strJsonToHash(jsonString), sw);
        return sw.toString();
    }

    /**
     * EDI to JSON
     *
     * @param xsdFile   EDI layout
     * @param ediString EDI String
     * @return JSON String
     */
    public String toJSON(String xsdFile, String ediString) {
        return toJSON(toInputSource(xsdFile), ediString);
    }

    public String toJSON(InputSource is, String ediString) {
        Unmarshaller<Map<String, Object>> unmarshaller = new Unmarshaller<>(loadDefinition(is));
        Iterator<Map<String, Object>> iter = unmarshaller.unmarshallToIterator(new StringReader(ediString));
        return gson.toJson(iter.next());
    }

    /**
     * EDI file to JSON List
     *
     * @param xsdFile EDI layout
     * @param ediFile EDI file
     * @return JSON String List
     */
    public List<String> fromEDIfileToJSON(String xsdFile, String ediFile) {
        return fromEDIfileToJSON(xsdFile, ediFile);
    }

    public List<String> fromEDIfileToJSON(InputSource is, String ediFile) {
        Unmarshaller<Map<String, Object>> unmarshaller = new Unmarshaller<>(loadDefinition(is));
        Stream<Map<String, Object>> stream = unmarshaller.unmarshallToStream(openStream(ediFile));
        return stream.map(e -> gson.toJson(e)).collect(Collectors.toList());
    }

    public static InputSource toInputSource(String file) {
        return new InputSource(Utils.class.getResource(file).toExternalForm());
    }

    public static RecordDefinition loadDefinition(InputSource is) {
        return new DefinitionLoader().load(is);
    }

    public static Reader openStream(String file) {
        return new InputStreamReader(Utils.class.getResourceAsStream(file));
    }

    public Map<String, Object> strJsonToHash(String json) {
        return gson.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
    }
}

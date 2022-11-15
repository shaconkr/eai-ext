package kr.shacon.edi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import kr.shacon.edi.type.MapDeserializer;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

public class Transformer {

    protected static Gson gson;

    static {
        gson = new GsonBuilder()
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
     * @param ediXsdFile    EDI layout
     * @param jsonString JSON String
     * @return EDI String
     */
    public String toEDI(String ediXsdFile,  String jsonString) {
        Marshaller<Map<String, Object>> marshaller = new Marshaller<>(loadDefinition(ediXsdFile));
        StringWriter sw = new StringWriter();
        marshaller.marshall(strJsonToHash(jsonString), sw);
        return sw.toString();
    }

    /**
     * EDI to JSON
     *
     * @param ediXsdFile   EDI layout
     * @param ediString EDI String
     * @return JSON String
     */
    public String toJSON(String ediXsdFile, String ediString) {
        Unmarshaller<Map<String, Object>> unmarshaller = new Unmarshaller<>(loadDefinition(ediXsdFile));
        Iterator<Map<String, Object>> iter = unmarshaller.unmarshallToIterator(new StringReader(ediString));
        Object o = iter.hasNext();
        return gson.toJson(iter.next());
    }

    public static RecordDefinition loadDefinition(String path) {
        return new DefinitionLoader().load(new InputSource(Utils.getReader(path,true)));
    }

    public Map<String, Object> strJsonToHash(String json) {
        return gson.fromJson(json, new TypeToken<Map<String, Object>>() {
        }.getType());
    }
}

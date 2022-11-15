package com.shacon.toss.batch;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.beanio.BeanReader;
import org.beanio.Marshaller;
import org.beanio.StreamFactory;
import org.beanio.Unmarshaller;
import org.beanio.internal.util.IOUtil;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Map;
import java.util.stream.Collectors;

public class TossBatchTest {


    @Test
    public void tossBat() throws IOException {

        System.setProperty("javax.xml.accessExternalSchema", "all");

        StreamFactory factory = newStreamFactory("tossbatch.xml");

        Reader reader = new InputStreamReader(getClass().getResourceAsStream("tossLogin.edi"));
        BeanReader in = factory.createReader("loginReq", reader );
        Map map = (Map) in.read();
        System.out.println(new Gson().toJson(map, Map.class));
        in.close();

        Reader reader1 = new InputStreamReader(getClass().getResourceAsStream("tossLogin.edi"));
        Unmarshaller u = factory.createUnmarshaller("loginReq");
        String record = new BufferedReader(reader1).lines().collect(Collectors.joining("\n"));
        Map umap = (Map) u.unmarshal(record);

        Marshaller m = factory.createMarshaller("loginRes");
        Map mm = Maps.newHashMap();
        mm.put("trgmCd","003");
        mm.put("userId","HCIS");
        String edi = m.marshal(mm).toString();

        System.out.println(edi);



    }

    public String load(String filename) {
        Reader in = new InputStreamReader(getClass().getResourceAsStream(filename));
        StringBuilder s = new StringBuilder();
        try {
            int n = -1;
            char[] c = new char[1024];
            while ((n = in.read(c)) != -1) {
                s.append(c, 0, n);
            }
            return s.toString();
        } catch (IOException ex) {
            throw new IllegalStateException("IOException caught", ex);
        } finally {
            IOUtil.closeQuietly(in);
        }
    }

    /**
     * @param config
     * @return
     * @throws IOException
     */
    private StreamFactory newStreamFactory(String config) throws IOException {
        StreamFactory factory = StreamFactory.newInstance();
        loadMappingFile(factory, config);
        return factory;
    }

    private void loadMappingFile(StreamFactory factory, String config) throws IOException {
        InputStream in = getClass().getResourceAsStream(config);
        try {
            factory.load(in);
        } finally {
            IOUtil.closeQuietly(in);
        }
    }
}

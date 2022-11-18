package com.shacon.toss.batch;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.shacon.kftc.batch.KftcFileTransfer;
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
    public void kftcTest01(){

        KftcFileTransfer kftc = new KftcFileTransfer();

        String enc = kftc.senderEncrypt("Kimchung","123456","00000002","950815");
        String dec = kftc.senderDecrypt("Kimchung","SV30W5TYEDI99NHB","00000002","950815");

        System.out.println(enc);
        System.out.println(dec);
    }




    @Test
    public void tossBat() throws IOException {

        System.setProperty("javax.xml.accessExternalSchema", "all");

        StreamFactory factory = newStreamFactory("tossbatch.xml");

        Reader reader = new InputStreamReader(getClass().getResourceAsStream("tossLogin.edi"));
        BeanReader in = factory.createReader("TOSS_003", reader );
        Map map = (Map) in.read();
        System.out.println(new Gson().toJson(map, Map.class));
        in.close();

        Reader reader1 = new InputStreamReader(getClass().getResourceAsStream("tossLogin.edi"));
        Unmarshaller u = factory.createUnmarshaller("TOSS_003");
        String record = new BufferedReader(reader1).lines().collect(Collectors.joining("\n"));
        Map umap = (Map) u.unmarshal(record);

        Marshaller m = factory.createMarshaller("TOSS_030");
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

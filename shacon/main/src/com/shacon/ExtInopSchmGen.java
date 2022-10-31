//package com.shacon;
//
////
//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by FernFlower decompiler)
////
//
//
//import com.uni.jmims.app.com.dat.dao.vo.D010RDvo;
//import com.uni.jmims.app.com.dat.dso.ComDso;
//import com.uni.jmims.app.common.MmsException;
//import com.uni.jmims.app.iom.dat.dao.vo.B001MDvo;
//import com.uni.jmims.core.util.StringUtil;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.StringWriter;
//import java.util.List;
//import java.util.Map;
//import org.jdom2.CDATA;
//import org.jdom2.Comment;
//import org.jdom2.Document;
//import org.jdom2.Element;
//import org.jdom2.Namespace;
//import org.jdom2.output.Format;
//import org.jdom2.output.XMLOutputter;
//import org.jdom2.output.Format.TextMode;
//
//public class ExtInopSchmGen {
//    public static ExtInopSchmGen extInopgen;
//
//    public static ExtInopSchmGen getInstance() {
//        if (extInopgen == null) {
//            extInopgen = new ExtInopSchmGen();
//        }
//
//        return extInopgen;
//    }
//
//    public ExtInopSchmGen() {
//    }
//
//    public String mkSchm(B001MDvo inopInfo) throws Exception {
//        if (StringUtil.nullToEmpty(inopInfo.getExtTlgrClscCd()).equals("")) {
//            throw new MmsException("원인 : [필수값 누락] 대외 전문 종별코드 미포함 <br />조치 : 대외연계 I/O(" + inopInfo.getInopIdn() + ")에 전문종별코드를 설정하세요.");
//        } else if (!StringUtil.nullToEmpty(inopInfo.getPcsnSttsDcd()).equals("4") && !StringUtil.nullToEmpty(inopInfo.getPcsnSttsDcd()).equals("9") && !StringUtil.nullToEmpty(inopInfo.getPcsnSttsDcd()).equals("5")) {
//            throw new MmsException("원인 : [상태값 이상] IO의 상태값이 [배포가능][배포성공][배포실패]가 아닙니다. <br />조치 : I/O(" + inopInfo.getInopId() + ")를 체크아웃해서 [배포가능]상태로 변경해주세요.");
//        } else {
//            Element io = new Element("io");
//            io.setAttribute("id", StringUtil.nullToEmpty(inopInfo.getInopId()));
//            io.setAttribute("idn", StringUtil.nullToEmpty(inopInfo.getInopIdn().substring(inopInfo.getInopIdn().indexOf("_") + 1)));
//            io.setAttribute("version", StringUtil.nullToEmpty(inopInfo.getVrsnNo().toString()));
//            io.addNamespaceDeclaration(Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance"));
//            this.mkheader(io, inopInfo);
//            Element inbound = new Element("inbound");
//            io.addContent(inbound);
//            this.mkCndtTxt(inbound, "message", "id", inopInfo.getInndUtInopId(), inopInfo.getInndUtInopNm());
//            new Element("outbound");
//            return this.mkStream(io);
//        }
//    }
//
//    public void mkheader(Element parent, B001MDvo inopInfo) throws Exception {
//        Element common = new Element("common");
//        parent.addContent(common);
//        Element inopNm = new Element("io_name");
//        common.addContent(inopNm);
//        common.addContent(new Comment("I/O 명"));
//        inopNm.addContent(new CDATA(StringUtil.nullToEmpty(inopInfo.getInopNm())));
//        Element description = new Element("description");
//        common.addContent(description);
//        common.addContent(new Comment("I/O 설명"));
//        description.addContent(new CDATA(StringUtil.nullToEmpty(inopInfo.getDescCon())));
//        this.mkCndtItrn(common, "system", "code", "name", StringUtil.nullToEmpty(inopInfo.getInttSysDcd()), StringUtil.nullToEmpty(inopInfo.getInttSysNm()));
//        common.addContent(new Comment("시스템코드, 시스템명"));
//        this.mkCndtItrn(common, "work", "code", "name", StringUtil.nullToEmpty(inopInfo.getBswrCd()), StringUtil.nullToEmpty(inopInfo.getBswrNm()));
//        common.addContent(new Comment("업무구분코드"));
//        this.mkSetTextNmsp(common, "datetime", StringUtil.nullToEmpty(inopInfo.getWrtnTs()));
//        common.addContent(new Comment("작성 시간"));
//        this.mkSetTextNmsp(common, "writer", StringUtil.nullToEmpty(inopInfo.getMkrNm()));
//        common.addContent(new Comment("작성자"));
//        this.mkSetTextNmsp(common, "charset", StringUtil.nullToEmpty(inopInfo.getCharSet()));
//        common.addContent(new Comment("캐릭터셋"));
//        Element process_type = new Element("process_type");
//        process_type.setAttribute("type", inopInfo.getIoProcessType());
//        process_type.setAttribute("name", inopInfo.getIoProcessTypeNm());
//        parent.addContent(process_type);
//        parent.addContent(new Comment("IO처리유형 : O - 온라인, D - DB, F - FILE"));
//        Element online = new Element("online");
//        process_type.addContent(online);
//        Element external;
//        if (!StringUtil.nullToEmpty(inopInfo.getInndSvcId()).equals("")) {
//            this.mkCndtItrn(online, "service_in", "id", StringUtil.nullToEmpty(inopInfo.getInndSvcId()));
//        } else {
//            external = new Element("service_in");
//            online.addContent(external);
//        }
//
//        online.addContent(new Comment("Input 거래 ID"));
//        if (!StringUtil.nullToEmpty(inopInfo.getOtbnSvcId()).equals("")) {
//            this.mkCndtItrn(online, "service_out", "id", StringUtil.nullToEmpty(inopInfo.getOtbnSvcId()));
//        } else {
//            external = new Element("service_out");
//            online.addContent(external);
//        }
//
//        online.addContent(new Comment("Output 거래 ID"));
//        this.mkCndtItrn(online, "syn-asyn", "code", "name", StringUtil.nullToEmpty(inopInfo.getSyncAsyncDcd()), StringUtil.nullToEmpty(inopInfo.getSyncAsyncDsnm()));
//        online.addContent(new Comment("동기 / 비동기 여부"));
//        if (!StringUtil.nullToEmpty(inopInfo.getInndTrnCd()).equals("")) {
//            this.mkCndtItrn(online, "trncd_in", "code", StringUtil.nullToEmpty(inopInfo.getInndTrnCd()));
//        } else {
//            external = new Element("trncd_in");
//            online.addContent(external);
//        }
//
//        online.addContent(new Comment("Input 거래 코드"));
//        if (!StringUtil.nullToEmpty(inopInfo.getOtbnTrnCd()).equals("")) {
//            this.mkCndtItrn(online, "trncd_out", "code", StringUtil.nullToEmpty(inopInfo.getOtbnTrnCd()));
//        } else {
//            external = new Element("trncd_out");
//            online.addContent(external);
//        }
//
//        online.addContent(new Comment("Output 거래 코드"));
//        external = new Element("external");
//        online.addContent(external);
//        if (StringUtil.nullToEmpty(inopInfo.getOtisDcd()).isEmpty()) {
//            this.mkCndtItrn(external, "system", "code", "name", StringUtil.nullToEmpty(inopInfo.getOtisDcd()), StringUtil.nullToEmpty(inopInfo.getOtisNm()));
//            external.addContent(new Comment("대외 기관 구분 코드"));
//        } else {
//            ComDso comDso = new ComDso();
//            D010RDvo inptVo = new D010RDvo();
//            inptVo.setOtisDcd(inopInfo.getOtisDcd());
//            inptVo.setExtBswrCd(inopInfo.getExtBswrCd());
//            String otisNmList = "";
//            String otisCdList = "";
//            Map<String, Object> map = comDso.dynamicListOtisDcd(inptVo);
//            List<D010RDvo> otisCommList = (List)map.get("rows");
//            if (((D010RDvo)otisCommList.get(0)).getCmcdYn().equals("Y")) {
//                List<D010RDvo> otisList = comDso.dynamicListTbMmsD010r(inptVo);
//
//                for(int i = 0; i < otisList.size(); ++i) {
//                    otisNmList = otisNmList + ((D010RDvo)otisList.get(i)).getDtlsOtisNm() + ";";
//                    otisCdList = otisCdList + ((D010RDvo)otisList.get(i)).getDtlsOtisDcd() + ";";
//                }
//            } else {
//                otisCdList = StringUtil.nullToEmpty(inopInfo.getOtisDcd());
//                otisNmList = StringUtil.nullToEmpty(inopInfo.getOtisNm());
//            }
//
//            this.mkCndtItrn(external, "system", "code", "name", otisCdList, otisNmList);
//            external.addContent(new Comment("대외 기관 구분 코드"));
//        }
//
//        this.mkCndtItrn(external, "work", "code", "name", StringUtil.nullToEmpty(inopInfo.getExtBswrCd()), StringUtil.nullToEmpty(inopInfo.getExtBswrNm()));
//        external.addContent(new Comment("대외 업무 코드"));
//        this.mkCndtItrn(external, "msg", "code", StringUtil.nullToEmpty(inopInfo.getExtTlgrClscCd()));
//        external.addContent(new Comment("대외 전문 종별 코드"));
//        this.mkCndtItrn(external, "tx", "code", StringUtil.nullToEmpty(inopInfo.getExtTrnDcd()));
//        external.addContent(new Comment("대외 거래 구분 코드"));
//        this.mkCndtItrn(external, "tx", "code", StringUtil.nullToEmpty(inopInfo.getExtTrnDcd()));
//        external.addContent(new Comment("대외 거래 구분 코드"));
//        this.mkCndtItrn(external, "etc", "code", StringUtil.nullToEmpty(inopInfo.getExtEtcDcd()));
//        external.addContent(new Comment("대외 기타 구분 코드"));
//    }
//
//    public void mkCndtTxt(Element parent, String childNm, String key, String command1, String command2) {
//        Element child = new Element(childNm);
//        parent.addContent(child);
//        child.setAttribute(key, command1);
//        child.setText(command2);
//    }
//
//    public void mkCndtItrn(Element parent, String childNm, String key1, String key2, String command1, String command2) {
//        Element child = new Element(childNm);
//        parent.addContent(child);
//        if (!"".equals(StringUtil.nullToEmpty(command1))) {
//            child.setAttribute(key1, command1);
//        }
//
//        if (!"".equals(StringUtil.nullToEmpty(command2))) {
//            child.setAttribute(key2, command2);
//        }
//
//    }
//
//    public void mkCndtItrn(Element parent, String childNm, String key, String command) {
//        Element child = new Element(childNm);
//        parent.addContent(child);
//        if (!"".equals(StringUtil.nullToEmpty(command))) {
//            child.setAttribute(key, command);
//        }
//
//    }
//
//    public void mkSetTextNmsp(Element parent, String childNm, String command) {
//        Element child = new Element(childNm);
//        parent.addContent(child);
//        if (!"".equals(StringUtil.nullToEmpty(command))) {
//            child.setText(command);
//        }
//
//    }
//
//    public void mkSetTextNmsp(Element parent, Namespace namespace, String childNm, String command) {
//        Element child = new Element(childNm, namespace);
//        parent.addContent(child);
//        if (!"".equals(StringUtil.nullToEmpty(command))) {
//            child.setText(command);
//        }
//
//    }
//
//    public void mkCndtItrnNmsp(Element parent, Namespace namespace, String childNm, String key1, String key2, String command1, String command2) {
//        Element child = new Element(childNm, namespace);
//        parent.addContent(child);
//        if (!"".equals(StringUtil.nullToEmpty(command1))) {
//            child.setAttribute(key1, command1);
//        }
//
//        if (!"".equals(StringUtil.nullToEmpty(command2))) {
//            child.setAttribute(key2, command2);
//        }
//
//    }
//
//    public void mkCndtItrnNmsp(Element parent, Namespace namespace, String childNm, String key, String command) {
//        Element child = new Element(childNm, namespace);
//        parent.addContent(child);
//        if (!"".equals(StringUtil.nullToEmpty(command))) {
//            child.setAttribute(key, command);
//        }
//
//    }
//
//    public String mkStream(Element io) throws IOException {
//        Document document = new Document(io);
//        StringWriter result = new StringWriter();
//        XMLOutputter outputter = new XMLOutputter();
//        Format fm = outputter.getFormat();
//        fm.setEncoding("UTF-8");
//        fm.setIndent("    ");
//        fm.setLineSeparator("\r\n");
//        fm.setTextMode(TextMode.TRIM);
//        outputter.setFormat(fm);
//        outputter.output(document, result);
//        return result.toString();
//    }
//
//    public void mkFile(Element parent, String nm) throws IOException {
//        Document document = new Document(parent);
//        FileWriter writer = new FileWriter(nm + "Inop.xml");
//        XMLOutputter outputter = new XMLOutputter();
//        Format fm = outputter.getFormat();
//        fm.setEncoding("UTF-8");
//        fm.setIndent("    ");
//        fm.setLineSeparator("\r\n");
//        fm.setTextMode(TextMode.TRIM);
//        outputter.setFormat(fm);
//        outputter.output(document, writer);
//        writer.close();
//    }
//}

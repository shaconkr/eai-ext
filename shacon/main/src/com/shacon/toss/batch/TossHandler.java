package com.shacon.toss.batch;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.stream.ChunkedNioFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TossHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TossHandler.class);
    protected TossFileTransfer trans;
    String jobType;     // SD(자료송신), RD(자료수신), RS(송신List수신), RR(수신List수신)
    String dirpath =    "C:/hcisnas/eai_data/external/TOSS/rcv";
    String fileName;
    String fileSize;
    String startDay;
    String endDay;
    Boolean endFlag;
    Boolean loginFlag = false;

    Map respTrgmCd = ImmutableMap.of("003", "030", "007", "070", "100", "110");

    protected void login(ChannelHandlerContext ctx) {
        String loginReq = trans.loginReq(jobType, startDay, endDay, fileName);
        LOGGER.debug("@@@ 로그인요청 전문 송신 {} ", loginReq);
        ByteBuf buf = Unpooled.buffer(loginReq.length());
        buf.writeBytes(loginReq.getBytes());
        ctx.writeAndFlush(buf);
    }

    protected void sendFileNoti(ChannelHandlerContext ctx, String lastYn, String filepath, String jobType) {
        LOGGER.info("@@@ 송신파일 통보전문 전송");
        File file = new File(filepath);
        String filename = (jobType.equals("SD")) ? trans.spaces(20) : file.getName();
        String sendFileNoti = trans.sendFileNoti(filename, String.valueOf(file.length()), lastYn);
        ByteBuf buf = Unpooled.buffer(sendFileNoti.length());
        ctx.write(buf.writeBytes(sendFileNoti.getBytes(StandardCharsets.UTF_8)));
    }


    protected void logout(ChannelHandlerContext ctx) {
        String logoutReq = trans.logoutReq(jobType, startDay, endDay, fileName);
        LOGGER.debug("@@@ 로그아웃요청 전문 송신 {} ", logoutReq);
        ByteBuf buf = Unpooled.buffer(logoutReq.length());
        buf.writeBytes(logoutReq.getBytes());
        ctx.writeAndFlush(buf);
    }

    protected ByteBuf resp(String reqType, String resType, String edi) {
        Map<String, Object> res = trans.unmarshall(reqType, edi);
        res.put("trgmCd", respTrgmCd.get(res.get("trgmCd")));
        res.put("respCd", "000");
        LOGGER.debug("@@@ {}} 응답전문 송신", resType);
        String resEDI = trans.marshall(resType, res);
        ByteBuf buf = Unpooled.buffer(resEDI.length());
        buf.writeBytes(resEDI.getBytes(StandardCharsets.UTF_8));
        return buf;
    }

    protected void sendFile(ChannelHandlerContext ctx, String file) {
        try {
            File theFile = new File(file);
            FileChannel theFileChannel = new RandomAccessFile(theFile, "r").getChannel();
            long fileLength = theFileChannel.size();
            long offSet = 0;
            ctx.write(new ChunkedNioFile(theFileChannel, offSet, fileLength, 2048));
            ctx.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void recvFile(ChannelHandlerContext ctx, ByteBuf msg) {
        try {
            File file = new File(getDirpath());
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            while (msg.isReadable()) {
                ByteBuf buf = Unpooled.buffer(2048);
                msg.readBytes(buf);
                raf.write(buf.readableBytes());
            }
            Map<String, Object> rcvCfm = Maps.newHashMap();
            rcvCfm.put("trgmCd", "130");
            rcvCfm.put("respCd", "000");
            rcvCfm.put("fileName", file.getName());
            rcvCfm.put("fileSize", file.length());
            rcvCfm.put("procYn", "Y");
            String edi = trans.marshall("rcvConfirm", rcvCfm);
            ByteBuf buf = Unpooled.buffer(edi.length());
            buf.writeBytes(edi.getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected Set<String> listFilesDir(String dir) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }


    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getDirpath() {
        return dirpath;
    }

    public void setDirpath(String dirpath) {
        this.dirpath = dirpath;
    }

    public TossFileTransfer getTrans() {
        return trans;
    }

    public void setTrans(TossFileTransfer trans) {
        this.trans = trans;
    }

    public String getStartDay() {
        return startDay;
    }

    public void setStartDay(String startDay) {
        this.startDay = startDay;
    }

    public String getEndDay() {
        return endDay;
    }

    public void setEndDay(String endDay) {
        this.endDay = endDay;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }
}

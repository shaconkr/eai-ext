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
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TossHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(TossHandler.class);
    protected TossFileTransfer trans;
    String jobType;     // SD(자료송신), RD(자료수신), RS(송신List수신), RR(수신List수신)
    String dirpath;
    String fileName;
    String fileSize;
    String startDay;
    String endDay;
    Boolean endFlag;

    protected enum State {
        LOGIN_STANDBY,
        LOGIN_SUCCESS,
        LOGIN_FAIL,
        FILE_SEND_ING,
        FILE_SEND_END,
        FILE_RECV_ING,
        FILE_RECV_END,
        LOGOUT
    }

    protected State currState = State.LOGIN_STANDBY;

    Map respTrgmCd = ImmutableMap.of("003", "030", "007", "070", "100", "110");

    protected void sendT003(ChannelHandlerContext ctx) {
        String loginReq = trans.getT003(jobType, startDay, endDay, fileName);
        log.debug("#S# TOSS_003 로그인요청 전문 송신 {} ", loginReq);
        ByteBuf buf = Unpooled.buffer(loginReq.length());
        buf.writeBytes(loginReq.getBytes());
        ctx.writeAndFlush(buf);
    }

    protected void sendT100(ChannelHandlerContext ctx, String lastYn, String sendFileName, String jobType) {
        log.info("#S# TOSS_100 송신파일 통보전문 전송");
        File file = new File(getDirpath() + "/snd/" + sendFileName);
        String filename = (jobType.equals("SD")) ? trans.spaces(20) : file.getName();
        String sendFileNoti = trans.getT100(filename, String.valueOf(file.length()), lastYn);
        ByteBuf buf = Unpooled.buffer(sendFileNoti.length());
        buf.writeBytes(sendFileNoti.getBytes());
        ctx.writeAndFlush(buf);
    }


    protected void sendT007(ChannelHandlerContext ctx) {
        String logoutReq = trans.getT007(jobType, startDay, endDay, fileName);
        log.debug("#S# TOSS_007 로그아웃요청 전문 송신 {} ", logoutReq);
        ByteBuf buf = Unpooled.buffer(logoutReq.length());
        buf.writeBytes(logoutReq.getBytes());
        ctx.writeAndFlush(buf);
    }

    protected ByteBuf resp(String reqType, String resType, String edi) {
        Map<String, Object> res = trans.unmarshall(reqType, edi);
        res.put("trgmCd", respTrgmCd.get(res.get("trgmCd")));
        res.put("respCd", "000");
        log.debug("#S# {}} 응답전문 송신", resType);
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
            ctx.writeAndFlush(new ChunkedNioFile(theFileChannel, offSet, fileLength, 2048));
            ctx.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected RandomAccessFile recvStart(String fileName) {
        RandomAccessFile raf = null;
        try {
            String recvFile = getDirpath() + "/rcv/" + fileName + ".tmp";
            raf = new RandomAccessFile(recvFile, "rw");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return raf;
    }


    protected void sendT130(ChannelHandlerContext ctx, String filename, String filesize) {
        try {
            String recvFile = getDirpath() + "/rcv/" + fileName;
            Path source = Paths.get(recvFile + ".tmp");
            Files.move(source, source.resolveSibling(recvFile), StandardCopyOption.ATOMIC_MOVE);

            
            // Send Confirm
            log.debug("#S# TOSS_130 수신확인 전문송신");
            Map<String, Object> rcvCfm = Maps.newHashMap();
            rcvCfm.put("trgmCd", "130");
            rcvCfm.put("respCd", "000");
            rcvCfm.put("fileName", filename);
            rcvCfm.put("fileSize", filesize);
            rcvCfm.put("procYn", "Y");
            String edi = trans.marshall("TOSS_130", rcvCfm);
            ByteBuf buf = Unpooled.buffer(edi.length());
            buf.writeBytes(edi.getBytes(StandardCharsets.UTF_8));
            ctx.writeAndFlush(buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Directory file list
     *
     * @param dir
     * @return
     * @throws IOException
     */
    protected Set<String> getSendFildList(String dir)  {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .filter(path -> !path.getFileName().endsWith(".tmp"))       // 수신중파일
                    .filter(path -> !path.getFileName().endsWith(".done"))      // 송신완료파일
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected String getSendFile(String dir) {
        String ret = null;
        Iterator itr = null;
        itr = getSendFildList(dir).iterator();
        if (itr.hasNext()) {
            ret = String.valueOf(itr.next());
        }
        return ret;
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

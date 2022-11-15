package com.shacon.toss.batch;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@ChannelHandler.Sharable
public class TossClientHandler extends TossHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TossClientHandler.class);

    public TossClientHandler(String jobType, String startDay, String endDay, String filePath) {
        File file = new File(filePath);
        setJobType(jobType);
        setStartDay(startDay);
        setEndDay(endDay);
        setDirpath(filePath);
        setFileName(file.getName());
        setFileSize(String.valueOf(file.length()));
        setTrans(new TossFileTransfer());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        login(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        byteBuf.readableBytes();
        String rcvmsg = byteBuf.toString(StandardCharsets.UTF_8);
        LOGGER.debug("@@@ READ: {} ", rcvmsg);
        String trgmCd = rcvmsg.substring(9, 12);
        LOGGER.debug("@@@ 전문코드 trgmCd: {} ", trgmCd);

        switch (trgmCd) {
            case "030":
                LOGGER.debug("@@@ 030 로그인 응답전문 수신");
                LOGGER.debug("@@@ {} ", rcvmsg);
                Map<String, Object> map = trans.unmarshall("loginRes", rcvmsg);
                if (map.get("respCd").equals("000")) {
                    LOGGER.info("@@@ 로그인 성공");
                    loginFlag = true;
                    if (jobType.equals("SD")) {
                        sendFileNoti(ctx, "END", getFileName(), getFileSize());
                    }
                } else {
                    LOGGER.info("@@@ 로그인 실패");
                    loginFlag = false;
                }
                break;
            case "100":
                LOGGER.debug("@@@ 100 송신파일 통보전문 수신");
                LOGGER.debug("@@@ {} ", rcvmsg);
                Map<String, Object> sendFileNoti = trans.unmarshall("sendFileNoti", rcvmsg);
                setFileName((String)sendFileNoti.get("fileName"));
                setFileSize((String)sendFileNoti.get("fileSize"));
                LOGGER.debug("@@@ 송신파일 통보응답전문 전송");
                ctx.write(resp("sendFileNoti", "sendFileRes", rcvmsg));
                LOGGER.debug("@@@ 파일 수신 시작...");
                recvFile(ctx, (ByteBuf) msg);
                endFlag = (sendFileNoti.get("lastYn").equals("END")) ? true : false;
                break;
            case "110":
                LOGGER.debug("@@@ 110 송신파일 통보응답전문 수신");
                Map<String, Object> sendFileRes = trans.unmarshall("sendFileRes", rcvmsg);
                LOGGER.debug("@@@ 파일 송신 시작...");
                sendFile(ctx, dirpath);
                endFlag = (sendFileRes.get("lastYn").equals("END")) ? true : false;
            case "130":
                LOGGER.debug("@@@ 130 수신확인전문 수신");
                if(endFlag){
                    logout(ctx);
                }
            default:
                break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        LOGGER.info("@@@@@@@@ channelReadComplete ");
        ctx.flush();
        LOGGER.info("@@@@@@@@ channelReadComplete flush");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}


package com.shacon.toss.batch;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@ChannelHandler.Sharable
public class TossServerHandler extends TossHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TossServerHandler.class);

    public TossServerHandler(String dirpath) {
        setDirpath(dirpath);
        setTrans(new TossFileTransfer());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        String rcvmsg = byteBuf.toString(StandardCharsets.UTF_8);

        String trgmCd = rcvmsg.substring(9, 12);
        LOGGER.debug("@@@ 전문코드 trgmCd: {} ", trgmCd);

        //FIXME 다수 파일 송신 구현 필요? - 다수파일수신 테스트위해 필요
        File[] files = new File(getDirpath()).listFiles();
        for (int i = 0; i < files.length; i++) {
            String lastYn = "NXT";
            if(i == files.length -1){
                lastYn = "END";
            }
            if(trgmCd.equals("003")||trgmCd.equals("130")) {
                sendFileNoti(ctx, lastYn, files[i].getName(), String.valueOf(files.length));
            }
        }

        switch (trgmCd) {
            case "003":
                LOGGER.debug("@@@ 003 로그인 요청전문 수신");
                LOGGER.debug("@@@ {} ", rcvmsg);
                Map<String, Object> req = trans.unmarshall("loginReq", rcvmsg);
                setJobType((String) req.get("jobType"));
                ctx.write(resp("loginReq", "loginRes", rcvmsg));
                LOGGER.debug("@@@ 030 로그인 응답전문 전송");
                loginFlag = true;
                break;
            case "100":
                LOGGER.debug("@@@ 100 송신파일 통보전문 수신");
                LOGGER.debug("@@@ {} ", rcvmsg);
                Map<String, Object> sendFileNoti = trans.unmarshall("sendFileNoti", rcvmsg);
                setFileName((String)sendFileNoti.get("fileName"));
                setFileSize((String)sendFileNoti.get("fileSize"));
                LOGGER.debug("@@@ 110 송신파일 통보응답전문 전송");
                ctx.write(resp("sendFileNoti", "sendFileRes", rcvmsg));
                LOGGER.debug("@@@ 파일 수신 시작...");
                recvFile(ctx, (ByteBuf) msg);
                break;
            case "110":
                LOGGER.debug("@@@ 110 송신파일 통보응답전문 수신");
                LOGGER.debug("@@@ 파일 송신 시작...");
                sendFile(ctx, dirpath);
            case "130":
                LOGGER.debug("@@@ 130 수신확인전문 수신");
            case "007":
                LOGGER.debug("@@@ 007 로그아웃요청 수신");
                ctx.write(resp("logoutReq", "logoutRes", rcvmsg));
                LOGGER.debug("@@@ 070 로그아웃응답 전송");
            default:
                break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        LOGGER.info("@@@@@@@@ channelReadComplete ");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
        LOGGER.info("@@@@@@@@ cClose the connection when an exception is raised");
    }


}

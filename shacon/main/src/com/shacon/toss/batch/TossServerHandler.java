package com.shacon.toss.batch;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@ChannelHandler.Sharable
public class TossServerHandler extends TossHandler {
    private static final Logger log = LoggerFactory.getLogger(TossServerHandler.class);

    RandomAccessFile randomAccessFile = null;
    String[] sendFileList = null;
    int fp = -1;
    int packetSize = 0;

    public TossServerHandler(String dirpath) {
        setDirpath(dirpath);
        setTrans(new TossFileTransfer());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        log.debug("### currentState:{}", currState);
        ByteBuf buf = (ByteBuf) msg;

        if (currState.equals(State.FILE_RECV_ING) ) {
            log.debug("### 파일 수신 시작...");
            if (buf.isReadable()) {
                buf.readableBytes();
                byte[] bytes = ByteBufUtil.getBytes(buf);
                packetSize += bytes.length;
                if (randomAccessFile != null) {
                    randomAccessFile.write(bytes);
                }
            }
        } else {
            commandMode(ctx, buf);
        }
    }

    private void commandMode(ChannelHandlerContext ctx, ByteBuf buf) {
        buf.readableBytes();
        String rcvmsg = buf.toString(StandardCharsets.UTF_8);
        log.debug("#R# readableBytes:[{}] ", rcvmsg);
        String trgmCd = rcvmsg.substring(9, 12);

        switch (trgmCd) {
            case "003":
                log.debug("#R# TOSS_003 로그인 요구전문 수신");
                sendFileList = getSendFildList(getDirpath() + "/snd").toArray(String[]::new);  // 송신대상 파일목록
                Map<String, Object> req = trans.unmarshall("TOSS_003", rcvmsg);
                setJobType((String) req.get("jobType"));
                log.debug("#S# TOSS_030 로그인 응답전문 전송");
                ctx.write(resp("TOSS_003", "TOSS_030", rcvmsg));
                ctx.flush();
                currState = State.LOGIN_SUCCESS;
                break;
            case "100": // 수신
                log.debug("#R# TOSS_100 송신파일 통보전문 100 수신");
                currState = State.FILE_RECV_ING;

                Map<String, Object> sendFileNoti = trans.unmarshall("TOSS_100", rcvmsg);
                setFileName((String) sendFileNoti.get("fileName"));
                setFileSize((String) sendFileNoti.get("fileSize"));

                log.debug("#S# TOSS_110 송신파일 통보응답전문 전송"); // send 110
                ctx.write(resp("TOSS_100", "TOSS_110", rcvmsg));

                randomAccessFile = recvStart(getFileName());

                if (sendFileNoti.get("lastYn").equals("END")) {
                    currState = State.FILE_RECV_END;
                } else {
                    currState = State.FILE_RECV_ING;
                }
                break;
            case "110":
                log.debug("#R# TOSS_110 송신파일 통보응답전문 수신");
                currState = State.FILE_SEND_ING;
                break;
            case "130":
                log.debug("#R# TOSS_130 수신확인전문 수신");
                currState = State.FILE_SEND_END;
                break;
            case "007":
                log.debug("#R# TOSS_007 로그아웃요청 수신");
                ctx.write(resp("TOSS_007", "TOSS_007", rcvmsg));
                log.debug("#S# TOSS_070 로그아웃응답 전송");
                currState = State.LOGOUT;
                break;
            default:
                break;
        }

        if (currState.equals(State.LOGIN_SUCCESS) || currState.equals(State.FILE_SEND_END)) {
            fp++;
            log.info("@@@ 송신대상 파일수 {} idx {}", sendFileList.length, fp);
            if (sendFileList.length-1 >= fp) {
                String lastYn = (fp < sendFileList.length-1) ? "NXT" : "END";
                log.debug("send file {}", sendFileList[fp]);
                sendT100(ctx, lastYn, sendFileList[fp], "RD");
            }
        }
        if (currState.equals(State.FILE_SEND_ING)) {
            log.debug("@@@ 파일 송신 시작...");
            if (sendFileList.length > fp) {
                sendFile(ctx, dirpath + "/snd/" + sendFileList[fp]);
            }
        }
        log.debug("currentState:{}", currState);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        log.info("### channelReadComplete ");
        log.debug("### currentState:{}", currState);
        ctx.flush();
        if (currState.equals(State.FILE_RECV_ING) ) {
            try {
                if (randomAccessFile != null && packetSize == Integer.parseInt(getFileSize())) {
                    randomAccessFile.close();
                    sendT130(ctx, getFileName(), getFileSize());
                    currState = State.FILE_RECV_END;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.debug("### currentState:{}", currState);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
        log.error("### cClose the connection when an exception is raised");
    }


}

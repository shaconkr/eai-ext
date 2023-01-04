package com.shacon.toss.batch;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

@ChannelHandler.Sharable
public class TossClientHandler extends TossHandler {
    private static final Logger log = LoggerFactory.getLogger(TossClientHandler.class);

    RandomAccessFile randomAccessFile = null;
    String[] sendFileList = null;
    int fp = -1;
    int packetSize = 0;

    public TossClientHandler(String jobType, String startDay, String endDay, String dirpath) {
        setJobType(jobType);
        setStartDay(startDay);
        setEndDay(endDay);
        setDirpath(dirpath);
        setTrans(new TossFileTransfer());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Client " + ctx.channel().remoteAddress() + " connected");
        if (currState.equals(State.LOGIN_STANDBY)) sendT003(ctx);
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
//        log.debug("#R# readableBytes:[{}] ", rcvmsg);
        String trgmCd = rcvmsg.substring(9, 12);

        switch (trgmCd) {
            case "030":
                log.debug("#R# TOSS_030 로그인 응답전문 수신");
                Map<String, Object> map = trans.unmarshall("TOSS_030", rcvmsg);
                if (map.get("respCd").equals("000")) {
                    currState = State.LOGIN_SUCCESS;
                    if (jobType.equals("SD")) {
                        sendFileList = getSendFildList(getDirpath() + "/snd").toArray(String[]::new);  // 송신대상 파일목록
                        sendT100(ctx, "END", getFileName(), getFileSize());
                        currState = State.FILE_SEND_ING;
                    }
                } else {
                    log.info("### 로그인 실패");
                    currState = State.LOGIN_FAIL;
                }
                break;
            case "100":  // 수신 (다수파일 일괄수신)
                log.debug("#R# TOSS_100 송신파일 통보전문 100 수신");
                currState = State.FILE_RECV_ING;


                Map<String, Object> sendFileNoti = trans.unmarshall("TOSS_100", rcvmsg);
                setFileName((String) sendFileNoti.get("fileName"));
                setFileSize((String) sendFileNoti.get("fileSize"));

                log.debug("#S# TOSS_110 송신파일 통보응답전문 전송"); // send 110
                ctx.write(resp("TOSS_100", "TOSS_110", rcvmsg));

                randomAccessFile = recvStart(getFileName());
                currState = State.FILE_RECV_ING;

                break;
            case "110":
                log.debug("#R# TOSS_110 송신파일 통보응답전문 수신");
                currState = State.FILE_SEND_ING;
                break;
            case "130":
                log.debug("#R# TOSS_130 수신확인전문 수신");
                currState = State.FILE_SEND_END;
                break;
            default:
                break;
        }
        if (jobType.equals("SD")) {
            if (currState.equals(State.LOGIN_SUCCESS) || currState.equals(State.FILE_SEND_END)) {
                fp++;
                log.info("@@@ 송신대상 파일수 {} idx {}", sendFileList.length, fp);
                if (sendFileList.length - 1 >= fp) {
                    String lastYn = (fp < sendFileList.length - 1) ? "NXT" : "END";
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
                    packetSize = 0;
                    currState = State.FILE_RECV_END;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.debug("### currentState:{}", currState);
//        if (sendFileNoti.get("lastYn").equals("END")) {
//            sendT007(ctx);
//        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
        log.error("### cClose the connection when an exception is raised");
    }
}


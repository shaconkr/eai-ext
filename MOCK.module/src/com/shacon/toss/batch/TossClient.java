package com.shacon.toss.batch;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.oio.OioSocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TossClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TossClient.class);
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    static final String dirpath  = "C:/app/sam/pm/dacom";

    public static void main(String[] args) throws Exception {

//        String jobType = args[0];          // S send , R receive
//        String filePath = args[1];

        TossClient client = new TossClient();


        String filePath = dirpath +  "FILE_FILE_FILE";

        client.start("RD", "20221114","20221114", dirpath);
    }

    public void start(String jobType, String startDay, String endDay, String dirpath) throws Exception {


        EventLoopGroup group = new OioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(OioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
//                            p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new ChunkedWriteHandler());
                            p.addLast(new TossClientHandler(jobType, startDay, endDay, dirpath));
                        }
                    });

            LOGGER.info("TossClient started...");

            // Start the client.
            ChannelFuture f = b.connect(HOST, PORT).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}

package com.shacon.toss.batch;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TossServer {
    static final int PORT = Integer.parseInt(System.getProperty("port", "8007"));
    private static final Logger LOGGER = LoggerFactory.getLogger(TossServer.class);
    static final String dirpath = "C:/app/sam/pm/toss";

    public static void main(String[] args) throws Exception {

//        String filePath = args[0];
        TossServer server = new TossServer();
        String filePath = dirpath;
        server.start(dirpath);
    }

    public void start(String dirpath) throws Exception {
        EventLoopGroup bossGroup = new OioEventLoopGroup(1);
        EventLoopGroup workerGroup = new OioEventLoopGroup();
        final TossServerHandler serverHandler = new TossServerHandler(dirpath);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(OioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
//                            p.addLast(new LoggingHandler(LogLevel.INFO));
                            p.addLast(new ChunkedWriteHandler());
                            p.addLast(serverHandler);
                        }
                    });

            LOGGER.info("TossServer started...");

            // Start the server.
            ChannelFuture f = b.bind(PORT).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

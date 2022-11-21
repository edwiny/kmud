package com.example.network

import com.example.config.AppContext
import com.example.repository.NetworkServerInterface
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslHandler
import io.netty.handler.ssl.util.SelfSignedCertificate
import io.netty.util.CharsetUtil
import java.util.concurrent.TimeUnit


val connMgr = ConnectionManager()


class ConnectionManager {
    val connections = mutableListOf<SocketChannel>()
    fun addConn(ch: SocketChannel) {
        println("ConnectionManager.addConn() called - channel ${ch.id()}")

        this.connections.add(ch)
    }

    fun removeConn(ch: SocketChannel) {
        val foundIndex = this.connections.indexOf(ch)
        println("ConnectionManager.removeConn() called - channel index at ${foundIndex} - ${ch.id()}")

        this.connections.remove(ch)
    }

    fun broadcastSomething(msg: String) {
        connections.forEach {
            it.writeAndFlush(TextWebSocketFrame(msg.uppercase()))
        }
    }
}


class NettyWebsocketServer(val SSL: Boolean = false,
                            val PORT: Int = 8080) : NetworkServerInterface {

    lateinit var newConnectionHandler: (SocketChannel) -> Boolean
    lateinit var closeConnectionHandler: (SocketChannel) -> Boolean
    lateinit var incomingDataHandler: (SocketChannel, String) -> String
    lateinit var intervalHandler: () -> Boolean



    fun run(args: Array<String>) {
        // Configure SSL.
        val sslCtx: SslContext?
        sslCtx = if (SSL) {
            val ssc = SelfSignedCertificate()
            SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build()
        } else {
            null
        }
        val bossGroup: EventLoopGroup = NioEventLoopGroup(1)
        val workerGroup: EventLoopGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.DEBUG))
                .childHandler(WebSocketServerInitializer(sslCtx,
                                                            this.newConnectionHandler,
                                                            this.incomingDataHandler,
                                                            this.closeConnectionHandler))
            val ch = b.bind(PORT).sync().channel()
            println(
                "Open your web browser and navigate to " +
                        (if (SSL) "https" else "http") + "://127.0.0.1:" + PORT + '/'
            )

            println("WebSocketServer:run() bind completed")

            var lastTime = System.currentTimeMillis() / 1000
            val future = ch.closeFuture()
            ch.eventLoop().scheduleAtFixedRate( {
                val currentTime = System.currentTimeMillis() / 100
                connMgr.broadcastSomething("schedule $currentTime")
                this.intervalHandler()
                //println("timer event")
            }, 0, 1000, TimeUnit.MILLISECONDS, )

            /*
            while(!future.isDone && !future.await(10, TimeUnit.MILLISECONDS)) {
                val currentTime = System.currentTimeMillis() / 1000
                if (currentTime > lastTime + 1) {
                    lastTime = currentTime
                    connMgr.broadcastSomething(currentTime.toString())
                }
            }

             */

            ch.closeFuture().sync()
        } finally {
            println("WebSocketServer:run() completes in finally block")
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }

        println("WebSocketServer:run() completes.")
    }

    override fun newConnectionHandler(handler: (channel: SocketChannel) -> Boolean) {
        this.newConnectionHandler = handler
    }

    override fun closeConnectionHandler(handler: (channel: SocketChannel) -> Boolean) {
        this.closeConnectionHandler = handler
    }

    override fun incomingDataHandler(handler: (channel: SocketChannel, message: String) -> String) {
        this.incomingDataHandler = handler
    }

    override fun intervalHandler(handler: () -> Boolean) {
        this.intervalHandler = handler
    }


}

class WebSocketServerInitializer(private val sslCtx: SslContext?,
                                 private val newConnectionHandler: (channel: SocketChannel) -> Boolean,
                                 private val incomingDataHandler: (channel: SocketChannel, message: String) -> String,
                                 private val closeConnectionHandler: (channel: SocketChannel) -> Boolean)
    : ChannelInitializer<SocketChannel>() {
    @Throws(Exception::class)
    public override fun initChannel(ch: SocketChannel) {
        val pipeline = ch.pipeline()
        if (sslCtx != null) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()))
        }
        pipeline.addLast(HttpServerCodec())
        pipeline.addLast(HttpObjectAggregator(65536))
        pipeline.addLast(WebSocketServerProtocolHandler(WEBSOCKET_PATH, null, true))
        pipeline.addLast(WebSocketIndexPageHandler(WEBSOCKET_PATH))
        pipeline.addLast(WebSocketFrameHandler(incomingDataHandler, closeConnectionHandler))
        println("WebSocketServerInitializer end")
        connMgr.addConn(ch)
        newConnectionHandler(ch)
    }

    companion object {
        private const val WEBSOCKET_PATH = "/chat"
    }
}


class WebSocketFrameHandler(val incomingDataHandler: (channel: SocketChannel, message: String) -> String,
                            val closeConnectionHandler: (channel: SocketChannel) -> Boolean)
    : SimpleChannelInboundHandler<WebSocketFrame>() {
    @Throws(java.lang.Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, frame: WebSocketFrame) {
        // ping and pong frames already handled
        if (frame is TextWebSocketFrame) {
            // Send the uppercase string back.
            val request = frame.text()
            val result = incomingDataHandler(ctx.channel() as SocketChannel, request)
            if (result != "") ctx.channel().writeAndFlush(TextWebSocketFrame(result))
        } else {
            val message = "unsupported frame type: " + frame.javaClass.name
            throw java.lang.UnsupportedOperationException(message)
        }
    }

    override fun channelUnregistered(ctx: ChannelHandlerContext?) {
        super.channelUnregistered(ctx)
        connMgr.removeConn(ctx!!.channel() as SocketChannel)
        this.closeConnectionHandler(ctx!!.channel() as SocketChannel)
    }
}


class WebSocketIndexPageHandler(private val websocketPath: String) :
    SimpleChannelInboundHandler<FullHttpRequest>() {
    @Throws(java.lang.Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, req: FullHttpRequest) {
        // Handle a bad request.
        if (!req.decoderResult().isSuccess) {
            sendHttpResponse(
                ctx, req, DefaultFullHttpResponse(
                    req.protocolVersion(), HttpResponseStatus.BAD_REQUEST,
                    ctx.alloc().buffer(0)
                )
            )
            return
        }

        // Allow only GET methods.
        if (!HttpMethod.GET.equals(req.method())) {
            sendHttpResponse(
                ctx, req, DefaultFullHttpResponse(
                    req.protocolVersion(), HttpResponseStatus.FORBIDDEN,
                    ctx.alloc().buffer(0)
                )
            )
            return
        }

        // Send the index page
        if ("/" == req.uri() || "/index.html" == req.uri()) {
            val webSocketLocation = getWebSocketLocation(
                ctx.pipeline(), req,
                websocketPath
            )
            val content: ByteBuf = WebSocketServerIndexPage.getContent(webSocketLocation)
            val res: FullHttpResponse = DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.OK, content)
            res.headers()[HttpHeaders.Names.CONTENT_TYPE] = "text/html; charset=UTF-8"
            HttpUtil.setContentLength(res, content.readableBytes().toLong())
            sendHttpResponse(ctx, req, res)
        } else {
            sendHttpResponse(
                ctx, req, DefaultFullHttpResponse(
                    req.protocolVersion(), HttpResponseStatus.NOT_FOUND,
                    ctx.alloc().buffer(0)
                )
            )
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }

    companion object {
        private fun sendHttpResponse(ctx: ChannelHandlerContext, req: FullHttpRequest, res: FullHttpResponse) {
            // Generate an error page if response getStatus code is not OK (200).
            val responseStatus = res.status()
            if (responseStatus.code() != 200) {
                ByteBufUtil.writeUtf8(res.content(), responseStatus.toString())
                HttpUtil.setContentLength(res, res.content().readableBytes().toLong())
            }
            // Send the response and close the connection if necessary.
            val keepAlive = HttpUtil.isKeepAlive(req) && responseStatus.code() == 200
            HttpUtil.setKeepAlive(res, keepAlive)
            val future = ctx.writeAndFlush(res)
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE)
            }
        }

        private fun getWebSocketLocation(cp: ChannelPipeline, req: HttpRequest, path: String): String {
            var protocol = "ws"
            if (cp.get(SslHandler::class.java) != null) {
                // SSL in use so use Secure WebSockets
                protocol = "wss"
            }
            return protocol + "://" + req.headers()[HttpHeaderNames.HOST] + path
        }
    }
}


/**
 * Generates the demo HTML page which is served at http://localhost:8080/
 */
object WebSocketServerIndexPage {
    private const val NEWLINE = "\r\n"
    fun getContent(webSocketLocation: String): ByteBuf {
        return Unpooled.copiedBuffer(
            "<html><head><title>Web Socket Test</title></head>" + NEWLINE +
                    "<body>" + NEWLINE +
                    "<script type=\"text/javascript\">" + NEWLINE +
                    "var socket;" + NEWLINE +
                    "if (!window.WebSocket) {" + NEWLINE +
                    "  window.WebSocket = window.MozWebSocket;" + NEWLINE +
                    '}' + NEWLINE +
                    "if (window.WebSocket) {" + NEWLINE +
                    "  socket = new WebSocket(\"" + webSocketLocation + "\");" + NEWLINE +
                    "  socket.onmessage = function(event) {" + NEWLINE +
                    "    var ta = document.getElementById('responseText');" + NEWLINE +
                    "    ta.value = ta.value + '\\n' + event.data" + NEWLINE +
                    "  };" + NEWLINE +
                    "  socket.onopen = function(event) {" + NEWLINE +
                    "    var ta = document.getElementById('responseText');" + NEWLINE +
                    "    ta.value = \"Web Socket opened!\";" + NEWLINE +
                    "  };" + NEWLINE +
                    "  socket.onclose = function(event) {" + NEWLINE +
                    "    var ta = document.getElementById('responseText');" + NEWLINE +
                    "    ta.value = ta.value + \"Web Socket closed\"; " + NEWLINE +
                    "  };" + NEWLINE +
                    "} else {" + NEWLINE +
                    "  alert(\"Your browser does not support Web Socket.\");" + NEWLINE +
                    '}' + NEWLINE +
                    NEWLINE +
                    "function send(message) {" + NEWLINE +
                    "  if (!window.WebSocket) { return; }" + NEWLINE +
                    "  if (socket.readyState == WebSocket.OPEN) {" + NEWLINE +
                    "    socket.send(message);" + NEWLINE +
                    "  } else {" + NEWLINE +
                    "    alert(\"The socket is not open.\");" + NEWLINE +
                    "  }" + NEWLINE +
                    '}' + NEWLINE +
                    "</script>" + NEWLINE +
                    "<form onsubmit=\"return false;\">" + NEWLINE +
                    "<input type=\"text\" name=\"message\" value=\"Hello, World!\"/>" +
                    "<input type=\"button\" value=\"Send Web Socket Data\"" + NEWLINE +
                    "       onclick=\"send(this.form.message.value)\" />" + NEWLINE +
                    "<h3>Output</h3>" + NEWLINE +
                    "<textarea id=\"responseText\" style=\"width:500px;height:300px;\"></textarea>" + NEWLINE +
                    "</form>" + NEWLINE +
                    "</body>" + NEWLINE +
                    "</html>" + NEWLINE, CharsetUtil.US_ASCII
        )
    }
}






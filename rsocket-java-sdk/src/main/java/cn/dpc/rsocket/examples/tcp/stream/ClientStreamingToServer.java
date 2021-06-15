package cn.dpc.rsocket.examples.tcp.stream;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;

public class ClientStreamingToServer {
    static final Logger logger = LoggerFactory.getLogger(ClientStreamingToServer.class);


    public static void main(String[] args) throws InterruptedException {
        RSocketServer.create()
                .acceptor(SocketAcceptor.forRequestStream(
                        payload ->
                                Flux.interval(Duration.ofMillis(100))
                                        .map(aLong -> DefaultPayload.create("Interval: " + aLong))))
                .bindNow(TcpServerTransport.create(7000));

        RSocket socket =
                RSocketConnector.create()
                        .setupPayload(DefaultPayload.create("test", "test"))
                        .connect(TcpClientTransport.create("localhost", 7000))
                        .block();

        final Payload payload = DefaultPayload.create("Hello");
        socket
                .requestStream(payload)
                .map(Payload::getDataUtf8)
                .doOnNext(logger::debug)
                .take(10)
                .then()
                .doFinally(signalType -> socket.dispose())
                .then()
                .block();

        Thread.sleep(1000000);
    }
}

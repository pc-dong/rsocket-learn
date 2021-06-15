package cn.dpc.rsocket.examples.tcp.stream;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static io.rsocket.SocketAcceptor.forRequestStream;

public class ServerStreamingToClient {
    static final Logger logger = LoggerFactory.getLogger(ServerStreamingToClient.class);

    public static void main(String[] args) {
        RSocketServer.create(
                (setup, rsocket) -> {
                    rsocket
                            .requestStream(DefaultPayload.create("Hello-Bidi"))
                            .map(Payload::getDataUtf8)
                            .log()
                            .subscribe();

                    return Mono.just(new RSocket() {});
                })
                .bindNow(TcpServerTransport.create("localhost", 7000));

        RSocket rsocket =
                RSocketConnector.create()
                        .acceptor(
                                forRequestStream(
                                        payload ->
                                                Flux.interval(Duration.ofSeconds(1))
                                                        .map(aLong -> DefaultPayload.create("Bi-di Response => " + aLong))))
                        .connect(TcpClientTransport.create("localhost", 7000))
                        .block();

        rsocket.onClose().block();
    }
}

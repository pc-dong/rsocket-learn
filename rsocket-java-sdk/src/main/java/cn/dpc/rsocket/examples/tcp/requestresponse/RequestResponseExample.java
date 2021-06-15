package cn.dpc.rsocket.examples.tcp.requestresponse;

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
import reactor.core.publisher.Mono;

public class RequestResponseExample {
    static final Logger logger = LoggerFactory.getLogger(RequestResponseExample.class);

    public static void main(String[] args) {
        RSocket rsocket =
                new RSocket() {
                    boolean fail = true;

                    @Override
                    public Mono<Payload> requestResponse(Payload p) {
                        if (fail) {
                            fail = false;
                            return Mono.error(new Throwable("Simulated error"));
                        } else {
                            return Mono.just(p);
                        }
                    }
                };

        RSocketServer.create(SocketAcceptor.with(rsocket))
                .bindNow(TcpServerTransport.create("localhost", 7000));

        RSocket socket =
                RSocketConnector.connectWith(TcpClientTransport.create("localhost", 7000)).block();

        for (int i = 0; i < 3; i++) {
            socket
                    .requestResponse(DefaultPayload.create("Hello"))
                    .map(Payload::getDataUtf8)
                    .onErrorReturn("error")
                    .doOnNext(logger::debug)
                    .block();
        }

        socket.dispose();
    }
}

package cn.dpc.rsocket.examples.tcp.fnf;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class FireAndForgetSimple {

    public static void main(String[] args) throws InterruptedException {
        RSocketServer.create(new MyAcceptor())
                .bind(TcpServerTransport.create(9001))
                .block();


        final Logger logger = LoggerFactory.getLogger("cn.dpc.rsocket.examples.tcp.fnf.FireAndForgetSimpleClient[Test]");

        Mono<RSocket> rSocketMono = RSocketConnector.create()
                .setupPayload(DefaultPayload.create("test"))
                .acceptor(SocketAcceptor.forFireAndForget(payload -> {
                    logger.info("Received Processed message: {}", payload.getDataUtf8());
                    payload.release();
                    return Mono.empty();
                }))
                .connect(TcpClientTransport.create(9001));

        RSocketClient rSocketClient = RSocketClient.from(rSocketMono);

        rSocketClient.fireAndForget(Mono.just(DefaultPayload.create("hello")))
        .block();

        Thread.sleep(4000);
    }

    static class MyAcceptor implements SocketAcceptor {
        Logger logger = LoggerFactory.getLogger(MyAcceptor.class);

        @Override
        public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
            this.logger.info("Receive a client connected with id: {}", setup.getDataUtf8());
            return Mono.just(new RSocketHandler(sendingSocket, setup.getDataUtf8()));
        }

        private static class RSocketHandler implements RSocket {
            Logger logger = LoggerFactory.getLogger(RSocketHandler.class);

            private final RSocket rSocket;
            private final String id;

            private RSocketHandler(RSocket rSocket, String id) {
                this.rSocket = rSocket;
                this.id = id;
            }

            @Override
            public Mono<Void> fireAndForget(Payload payload) {
                this.logger.info("Received a message [{}] from Client.ID[{}]", payload.getDataUtf8(), id);
                payload.release();

                return rSocket.fireAndForget(DefaultPayload.create("Processed " + payload.getDataUtf8()));
            }
        }
    }
}

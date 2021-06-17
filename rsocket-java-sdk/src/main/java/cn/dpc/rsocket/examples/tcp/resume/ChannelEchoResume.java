package cn.dpc.rsocket.examples.tcp.resume;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;

public class ChannelEchoResume {
    static final Logger logger = LoggerFactory.getLogger(ChannelEchoResume.class);

    public static void main(String[] args) {
        SocketAcceptor acceptor = SocketAcceptor
                .forRequestChannel(payloads ->
                        Flux.from(payloads)
                                .map(Payload::getDataUtf8)
                                .map(s -> "echo " + s)
                                .map(DefaultPayload::create)
                );

        Resume resume =
                new Resume()
                        .sessionDuration(Duration.ofMinutes(5))
                        .retry(
                                Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(1))
                                        .doBeforeRetry(s -> logger.debug("Disconnected. Trying to resume...")));

        RSocketServer.create()
                .resume(resume)
                .acceptor(acceptor)
                .bindNow(TcpServerTransport.create( 7000));

        RSocket socket = RSocketConnector.create()
                .resume(resume)
                .connect(TcpClientTransport.create( 7001)).block();

        socket
                .requestChannel(
                        Flux.interval(Duration.ofMillis(1000)).map(i -> DefaultPayload.create("Hello" + i)).onBackpressureDrop()
                )
                .map(Payload::getDataUtf8)
                .take(100)
                .doOnNext(logger::info)
                .doFinally(signalType -> socket.dispose())
                .blockLast();
    }
}

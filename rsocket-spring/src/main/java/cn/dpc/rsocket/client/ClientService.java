package cn.dpc.rsocket.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {
    private final RSocketRequester.Builder rsocketRequesterBuilder;
//    private final RSocketStrategies rSocketStrategies;

    private RSocketRequester rSocketRequester;

    @PostConstruct
    void init() {
        this.rSocketRequester = rsocketRequesterBuilder
                .setupRoute("setup")
                .setupData("test")
//                .rsocketStrategies(rSocketStrategies)
                .tcp("localhost", 8081);


//        this.rSocketRequester.rsocket()
//                .onClose()
//                .doOnError(error -> log.warn("Connection CLOSED"))
//                .doFinally(consumer -> log.info("Client DISCONNECTED"))
//                .subscribe();
    }


    Mono<Void> log(final String message) {
       return rSocketRequester.route("log")
                .data(message)
                .send();
    }

    Mono<Message> toUpperCase(final String message) {
        return rSocketRequester.route("toUpperCase")
                .data(message)
                .retrieveMono(Message.class);
    }

    Flux<Message> channelToUpperCase(List<String> messages) {
        return rSocketRequester.route("channelToUpperCase")
                .data(Flux.fromIterable(messages))
                .retrieveFlux(Message.class);
    }

    Flux<Character> splitString(final String messages) {
        return rSocketRequester.route("splitString")
                .data(messages)
                .retrieveFlux(Character.class);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private String message;
    }
}

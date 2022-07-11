package cn.dpc.rsocket.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Controller
@Slf4j
public class MessageController {

    @ConnectMapping("setup")
    void setUp(RSocketRequester requester,
               @Payload String client) {

        requester.rsocket()
                .onClose()
                .doFirst(() -> {
                    // Add all new clients to a client list
                    log.info("Client: {} CONNECTED.", client);
                })
                .doOnError(error -> {
                    // Warn when channels are closed by clients
                    log.warn("Channel to client {} CLOSED", client);
                })
                .doFinally(consumer -> {
                    // Remove disconnected clients from the client list
                    log.info("Client {} DISCONNECTED", client);
                })
                .subscribe();
    }

    @MessageMapping("log")
    Mono<Void> log(final String message) {
        log.info("receive log message: {}", message);
        return Mono.empty();
    }

    @MessageMapping("toUpperCase")
    Mono<Message> toUpperCase(final String message) {
        return Mono.just(new Message(message.toUpperCase()));
    }

    @MessageMapping({"channelToUpperCase"})
    Flux<Message> channelToUpperCase(Flux<String> messages) {
        return Flux.interval(Duration.ofSeconds(1)).zipWith(messages)
                .map(tuple -> tuple.getT2().toUpperCase())
                .map(Message::new);
    }

    @MessageMapping("splitString")
    Flux<Character> transString(final String message) {
        return Flux.interval(Duration.ofSeconds(1))
                .map(index -> message.charAt(index.intValue()))
                .take(message.length());
    }

    @ResponseBody
    @GetMapping("/splitString")
    Flux<Character> splitString(final String message) {
        return Flux.interval(Duration.ofSeconds(1))
                .map(index -> message.charAt(index.intValue()))
                .take(message.length());
    }

    @Data
    @AllArgsConstructor
    public static class Message {
        private String message;
    }
}



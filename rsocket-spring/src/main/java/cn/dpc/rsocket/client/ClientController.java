package cn.dpc.rsocket.client;

import cn.dpc.rsocket.client.ClientService.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping("/log")
    Mono<Void> log(@RequestBody String message) {
        return clientService.log(message);
    }

    @PostMapping("/toUpperCase")
    Mono<Message> toUpperCase(@RequestBody String message) {
        return clientService.toUpperCase(message);
    }

    @PostMapping({"/channelToUpperCase"})
    Flux<Message> channelToUpperCase(@RequestBody List<String> messages) {
        return clientService.channelToUpperCase(messages);
    }

    @PostMapping("splitString")
    Flux<Character> splitString(@RequestBody String message) {
        return clientService.splitString(message);
    }
}

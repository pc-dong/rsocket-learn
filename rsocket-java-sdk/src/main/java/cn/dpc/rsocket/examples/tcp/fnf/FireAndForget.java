package cn.dpc.rsocket.examples.tcp.fnf;

import io.rsocket.ConnectionSetupPayload;
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
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * An example of long-running tasks processing (a.k.a Kafka style) where a client submits tasks over
 * request `FireAndForget` and then receives results over the same method but on it is own side.
 *
 * <p>This example shows a case when the client may disappear, however, another a client can connect
 * again and receive undelivered completed tasks remaining for the previous one.
 */
public class FireAndForget {
    static final Logger log = LoggerFactory.getLogger(FireAndForget.class);

    public static void main(String[] args) throws Exception {
        Sinks.Many<Task> tasksProcessor =
                Sinks.many().unicast().onBackpressureBuffer(Queues.<Task>unboundedMultiproducer().get());
        ConcurrentMap<String, BlockingQueue<Task>> idToCompletedTasksMap = new ConcurrentHashMap<>();
        ConcurrentMap<String, RSocket> idToRSocketMap = new ConcurrentHashMap<>();

        BackgroundWorker backgroundWorker =
                new BackgroundWorker(tasksProcessor.asFlux(), idToCompletedTasksMap, idToRSocketMap);

        RSocketServer.create(new TasksAcceptor(tasksProcessor, idToCompletedTasksMap, idToRSocketMap))
                .bindNow(TcpServerTransport.create(9991));

        Mono<RSocket> rSocketMono =
                RSocketConnector.create()
                        .setupPayload(DefaultPayload.create("Test"))
                        .acceptor(
                                SocketAcceptor.forFireAndForget(
                                        p -> {
                                            log.info("Received Processed Task[{}]", p.getDataUtf8());
                                            p.release();
                                            return Mono.empty();
                                        }))
                        .connect(TcpClientTransport.create(9991));

        RSocket rSocketRequester1 = rSocketMono.block();

        for (int i = 0; i < 10; i++) {
            rSocketRequester1.fireAndForget(DefaultPayload.create("task" + i)).block();
        }

        Thread.sleep(4000);

        rSocketRequester1.dispose();
        log.info("Disposed");

        Thread.sleep(4000);

        RSocket rSocketRequester2 = rSocketMono.block();

        log.info("Reconnected");

        Thread.sleep(10000);

    }

    static class BackgroundWorker extends BaseSubscriber<Task> {
        final ConcurrentMap<String, BlockingQueue<Task>> idToCompletedTasksMap;
        final ConcurrentMap<String, RSocket> idToRSocketMap;

        BackgroundWorker(
                Flux<Task> taskProducer,
                ConcurrentMap<String, BlockingQueue<Task>> idToCompletedTasksMap,
                ConcurrentMap<String, RSocket> idToRSocketMap) {

            this.idToCompletedTasksMap = idToCompletedTasksMap;
            this.idToRSocketMap = idToRSocketMap;

            // mimic a long running task processing
            taskProducer
                    .concatMap(
                            t ->
                                    Mono.delay(Duration.ofMillis(ThreadLocalRandom.current().nextInt(200, 2000)))
                                            .thenReturn(t))
                    .subscribe(this);
        }

        @Override
        protected void hookOnNext(Task task) {
            BlockingQueue<Task> completedTasksQueue =
                    idToCompletedTasksMap.computeIfAbsent(task.id, __ -> new LinkedBlockingQueue<>());

            completedTasksQueue.offer(task);
            RSocket rSocket = idToRSocketMap.get(task.id);
            if (rSocket != null) {
                rSocket
                        .fireAndForget(DefaultPayload.create(task.content))
                        .subscribe(null, e -> {}, () -> completedTasksQueue.remove(task));
            }
        }
    }

    static class TasksAcceptor implements SocketAcceptor {
        final Sinks.Many<Task> tasksToProcess;
        final ConcurrentMap<String, BlockingQueue<Task>> idToCompletedTasksMap;
        final ConcurrentMap<String, RSocket> idToRSocketMap;

        TasksAcceptor(
                Sinks.Many<Task> tasksToProcess,
                ConcurrentMap<String, BlockingQueue<Task>> idToCompletedTasksMap,
                ConcurrentMap<String, RSocket> idToRSocketMap) {
            this.tasksToProcess = tasksToProcess;
            this.idToCompletedTasksMap = idToCompletedTasksMap;
            this.idToRSocketMap = idToRSocketMap;
        }

        @Override
        public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
            String id = setup.getDataUtf8();
            log.info("Accepting a new client connection with ID {}", id);


            if (this.idToRSocketMap.compute(
                    id, (__, old) -> old == null || old.isDisposed() ? sendingSocket : old)
                    == sendingSocket) {
                return Mono.<RSocket>just(
                        new RSocketTaskHandler(idToRSocketMap, tasksToProcess, id, sendingSocket))
                        .doOnSuccess(__ -> checkTasksToDeliver(sendingSocket, id));
            }

            return Mono.error(
                    new IllegalStateException("There is already a client connected with the same ID"));
        }

        private void checkTasksToDeliver(RSocket sendingSocket, String id) {
            log.info("Accepted a new client connection with ID {}. Checking for remaining tasks", id);
            BlockingQueue<Task> tasksToDeliver = this.idToCompletedTasksMap.get(id);

            if (tasksToDeliver == null || tasksToDeliver.isEmpty()) {
                // means nothing yet to send
                return;
            }

            log.info("Found remaining tasks to deliver for client {}", id);

            for (; ; ) {
                Task task = tasksToDeliver.poll();

                if (task == null) {
                    return;
                }

                sendingSocket
                        .fireAndForget(DefaultPayload.create(task.content))
                        .subscribe(
                                null,
                                e -> {
                                    // offers back a task if it has not been delivered
                                    tasksToDeliver.offer(task);
                                });
            }
        }

        private static class RSocketTaskHandler implements RSocket {

            private final String id;
            private final RSocket sendingSocket;
            private ConcurrentMap<String, RSocket> idToRSocketMap;
            private Sinks.Many<Task> tasksToProcess;

            public RSocketTaskHandler(
                    ConcurrentMap<String, RSocket> idToRSocketMap,
                    Sinks.Many<Task> tasksToProcess,
                    String id,
                    RSocket sendingSocket) {
                this.id = id;
                this.sendingSocket = sendingSocket;
                this.idToRSocketMap = idToRSocketMap;
                this.tasksToProcess = tasksToProcess;
            }

            @Override
            public Mono<Void> fireAndForget(Payload payload) {
                log.info("Received a Task[{}] from Client.ID[{}]", payload.getDataUtf8(), id);
                Sinks.EmitResult result = tasksToProcess.tryEmitNext(new Task(id, payload.getDataUtf8()));
                payload.release();
                return result.isFailure() ? Mono.error(new Sinks.EmissionException(result)) : Mono.empty();
            }

            @Override
            public void dispose() {
                idToRSocketMap.remove(id, sendingSocket);
            }
        }
    }


    static class Task {
        final String id;
        final String content;

        Task(String id, String content) {
            this.id = id;
            this.content = content;
        }
    }
}



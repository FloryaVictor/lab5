package lab5;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Query;
import akka.japi.Pair;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import org.asynchttpclient.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;


import lab5.Actors.CacheActor;
import lab5.Messages.GetMsg;
import lab5.Messages.StoreMsg;



import static org.asynchttpclient.Dsl.*;

public class Main {
    private final static Duration timeout = Duration.ofSeconds(5);
    private final static AsyncHttpClient asyncHttpClient = asyncHttpClient();
    private static ActorRef cache;
    public static void main(String[] args) throws IOException {
        System.out.println("start!");
        ActorSystem system = ActorSystem.create("lab5");
        cache  = system.actorOf(Props.create(CacheActor.class));
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createFlow(materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
               routeFlow,
               ConnectHttp.toHost("localhost", 8080),
               materializer
        );
        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        System.in.read();
        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate());
    }

    public static Flow<HttpRequest, HttpResponse, NotUsed> createFlow(ActorMaterializer mat){
        return Flow.of(HttpRequest.class)
                .map((req) ->{
                    Query q = req.getUri().query();
                    String url = q.get("testUrl").get();
                    Integer count = Integer.parseInt(q.get("count").get());
                    return new Pair<>(url, count);
                })
                .mapAsync(1, (Pair<String, Integer> p)->{
                    CompletionStage<Object> cs = Patterns.ask(cache, new GetMsg(p.first()), timeout);
                    return cs.thenCompose((Object res)->{
                        if ((Integer)res >= 0) {
                            return CompletableFuture.completedFuture(new Pair<>(p.first(), (Integer) res));
                        }
                        Flow<Pair<String, Integer>, Integer, NotUsed> requestFlow =
                                Flow.<Pair<String, Integer>>create()
                                .mapConcat(pair->{
                                    return new ArrayList<>(Collections.nCopies(pair.second(), pair.first()));
                                })
                                .mapAsync(p.second(), (String url)->{
                                    Instant t = Instant.now();
                                    Future<Response> resp = asyncHttpClient.prepareGet(url).execute();
                                    resp.get();
                                    long time = t.until(Instant.now(), ChronoUnit.MILLIS);
                                    return CompletableFuture.completedFuture((int) time);
                                });
                        return Source.single(p)
                                .via(requestFlow)
                                .toMat(Sink.fold(0, Integer::sum), Keep.right())
                                .run(mat)
                                .thenApply(sum->{
                                    return new Pair<>(p.first(), sum/p.second());
                                });
                    });
                })
                .map((Pair<String, Integer> p)->{
                    cache.tell(new StoreMsg(p.first(), p.second()), ActorRef.noSender());
                    return HttpResponse.create().withEntity(String.valueOf(p.second()));
                });
    }
}

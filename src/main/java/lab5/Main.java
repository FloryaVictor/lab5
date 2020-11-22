package lab5;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Query;
import akka.japi.Pair;

import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.Graph;
import akka.stream.SinkShape;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.sun.xml.internal.ws.util.CompletedFuture;
import lab5.Actors.CacheActor;
import lab5.Messages.GetMsg;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import lab5.Messages.StoreMsg;
import org.asynchttpclient.*;
import scala.Int;

import static org.asynchttpclient.Dsl.*;

public class Main {
    private final static Duration timeout = Duration.ofSeconds(5);

    public static void main(String[] args) throws IOException {
        System.out.println("start!");
        ActorSystem system = ActorSystem.create("lab5");
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createFlow(system, materializer);
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

    public static Flow<HttpRequest, HttpResponse, NotUsed> createFlow(ActorSystem system,
                                                                      ActorMaterializer mat){
        ActorRef cache = system.actorOf(Props.create(CacheActor.class));
        return Flow.of(HttpRequest.class)
                .map((req) ->{
                    Query q = req.getUri().query();
                    String url = q.get("testUrl").get();
                    Integer count = Integer.parseInt(q.get("count").get());
                    return new Pair<String, Integer>(url, count);
                })
                .mapAsync(1, (Pair<String, Integer> p)->{
                    CompletionStage<Object> cs = Patterns.ask(cache, new GetMsg(p.first()), timeout);
                    cs.thenApply((Object res)->{
                        if ((Integer)res >= 0) {
                            return new CompletedFuture<>(new Pair<>(p.first(), (Integer) res),
                                    null);
                        }
                        Flow<Pair<String, Integer>, Integer, NotUsed> interFlow =
                                Flow.<Pair<String, Integer>>create()
                                .mapConcat(pair->{
                                    return new ArrayList<>(Collections.nCopies(pair.second(), pair.first()));
                                })
                                .mapAsync(p.second(), (String url)->{
                                    Instant t = Instant.now();
                                    AsyncHttpClient asyncHttpClient = asyncHttpClient();
                                    Future<Response> resp = asyncHttpClient.prepareGet(url).execute();
                                    resp.get();
                                    long time = t.until(Instant.now(), ChronoUnit.MILLIS);
                                    asyncHttpClient.close();
                                    return CompletableFuture.completedFuture((int) time);
                                });
                        return Source.single(p)
                                .via(interFlow)
                                .toMat(Sink.fold(0, Integer::sum), Keep.right())
                                .run(mat)
                                .thenApply(sum->{
                                    return new Pair<>(p.first(), sum/p.second());
                                });
                    });
                    Integer r = (Integer) cs.toCompletableFuture().get();
                    System.out.println(r);
                    return cs;
                })
                .map((Object o)->{
//                    Pair<String, Integer> p = (Pair<String, Integer>)o;
//                    cache.tell(new StoreMsg(p.first(), p.second()), ActorRef.noSender());
                    return HttpResponse.create().withEntity(String.valueOf((Integer)o));
                });
    }
}

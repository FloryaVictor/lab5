package lab5;

import akka.NotUsed;
import akka.actor.Actor;
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
import akka.stream.javadsl.Source;
import akka.util.Timeout;
import com.sun.xml.internal.ws.util.CompletedFuture;
import lab5.Actors.CacheActor;
import lab5.Messages.GetMsg;

import scala.Int;
import scala.concurrent.Future;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;


public class Main {
    private final static Duration timeout = Duration.ofSeconds(5);

    public static void main(String[] args) throws IOException {
//        System.out.println("start!");
//        ActorSystem system = ActorSystem.create("lab5");
//        final Http http = Http.get(system);
//        final ActorMaterializer materializer = ActorMaterializer.create(system);
//        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow;
//        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
//               routeFlow,
//               ConnectHttp.toHost("localhost", 8080),
//               materializer
//        );
//        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
//        System.in.read();
//        binding
//                .thenCompose(ServerBinding::unbind)
//                .thenAccept(unbound -> system.terminate());
    }

    public static Flow<HttpRequest, HttpResponse, NotUsed> createFlow(Http http, ActorSystem system,
                                                                      ActorMaterializer mat){
        ActorRef cache = system.actorOf(Props.create(CacheActor.class));
        Flow.of(HttpRequest.class)
                .map((req) ->{
                    Query q = req.getUri().query();
                    String url = q.get("testUrl").get();
                    Integer count = Integer.parseInt(q.get("count").get());
                    return new Pair<String, Integer>(url, count);
                })
                .mapAsync(1, (Pair<String, Integer> p)->{
                    CompletionStage<Object> cs = Patterns.ask(cache, new GetMsg(p.first()), timeout);
                    cs.thenCompose((Object res)->{
                        if (res != null) {
                            return new CompletedFuture<Integer>((Integer) res, null);
                        }
                        return Source.single(p)
                                .toMat().run(mat);
                    });
                });

    }
}

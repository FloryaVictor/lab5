package lab5.Actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.dispatch.OnComplete;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import akka.util.Timeout;
import lab5.Messages.GetMsg;
import lab5.Messages.TestMsg;
import scala.concurrent.Future;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class TestActor extends AbstractActor {
    private final ActorRef cacheActor = getContext().actorOf(
            Props.create(CacheActor.class)
    );
    private final Timeout timeout = Timeout.create(Duration.ofSeconds(5));

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(TestMsg.class, msg->{

                })
                .match(GetMsg.class, msg->{
                    Future<Object> res = Patterns.ask(cacheActor, msg, timeout);
                    res.onComplete(new OnComplete<Object>() {
                        public void onComplete(Throwable t, Object res){
                            getSender().tell(res, ActorRef.noSender());
                        }
                    }, getContext().getDispatcher());

                })
                .build();
    }
}

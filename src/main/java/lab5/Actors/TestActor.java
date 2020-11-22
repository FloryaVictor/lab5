package lab5.Actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.pattern.Patterns;
import lab5.Messages.GetMsg;
import lab5.Messages.TestMsg;

import java.util.concurrent.CompletionStage;

public class TestActor extends AbstractActor {
    private final ActorRef cacheActor = getContext().actorOf(
            Props.create(CacheActor.class)
    );
    private final 
    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(TestMsg.class, msg->{

                })
                .match(GetMsg.class, msg->{
                    CompletionStage<Integer> res = Patterns.ask(msg);
                    getSender().tell();
                })
                .build();
    }
}

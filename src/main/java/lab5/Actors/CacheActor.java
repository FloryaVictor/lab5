package lab5.Actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import lab5.Messages.GetMsg;
import lab5.Messages.StoreMsg;

import java.util.HashMap;

public class CacheActor extends AbstractActor {
    private final HashMap<String, Integer> cache = new HashMap<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(GetMsg.class, msg->{
                    getSender().tell(cache.getOrDefault(msg.getUrl(), -1), ActorRef.noSender());
                })
                .match(StoreMsg.class, msg->{
//                    cache.put(msg.getUrl(), msg.getTime());
                })
                .build();
    }
}

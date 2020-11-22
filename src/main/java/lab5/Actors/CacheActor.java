package lab5.Actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;

public class CacheActor extends AbstractActor {
    private final HashMap<String, Integer> cache = new HashMap<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(String.class, url->{
                    getSender().tell(cache.getOrDefault(url, null), ActorRef.noSender());
                })
                .build();
    }
}

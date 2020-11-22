package lab5.Actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;

public class TestActor extends AbstractActor {
    private final ActorRef cacheActor = getContext().actorOf(
            Props.create(CacheActor.class)
    );

    @Override
    public Receive createReceive() {
        return null;
    }
}

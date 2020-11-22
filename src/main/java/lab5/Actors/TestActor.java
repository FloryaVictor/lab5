package lab5.Actors;

import akka.actor.AbstractActor;

public class TestActor extends AbstractActor {
    private final ActorRef cache = getContext();

    @Override
    public Receive createReceive() {
        return null;
    }
}

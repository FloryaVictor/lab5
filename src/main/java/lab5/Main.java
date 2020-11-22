package lab5;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;

public class Main {
    ActorSystem system = ActorSystem.create("lab5");
    ActorMaterializer materializer = ActorMaterializer.create(system);
    
}

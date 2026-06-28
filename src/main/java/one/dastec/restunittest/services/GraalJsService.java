package one.dastec.restunittest.services;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

@Service
public class GraalJsService {

    private final Engine engine = Engine.newBuilder().build();

    public Context createContext() {
        return newBuilder().build();
    }

    public Engine getEngine() {
        return engine;
    }

    public org.graalvm.polyglot.Context.Builder newBuilder() {
        return Context.newBuilder("js")
                .engine(engine)
                .allowHostAccess(HostAccess.ALL);
    }
}

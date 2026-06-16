package one.dastec.restunittest.services;

import jakarta.annotation.PreDestroy;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

@Service
public class GraalJsService implements AutoCloseable {

    private final Context context;

    public GraalJsService() {
        this.context = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .build();

        // Simple JS evaluation
        this.context.eval("js", "console.log('Hello from GraalJS!');");
    }

    public Value executeScript(String script) {
        synchronized (context) {
            return context.eval("js", script);
        }
    }

    public void putMember(String key, Object value) {
        synchronized (context) {
            context.getBindings("js").putMember(key, value);
        }
    }

    @Override
    @PreDestroy
    public void close() {
        if (context != null) {
            context.close();
        }
    }
}

package one.dastec.restunittest.services;

import jakarta.annotation.PreDestroy;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Service;

@Service
public class GraalJsService {

    public Context createContext() {
        return Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .build();
    }
}

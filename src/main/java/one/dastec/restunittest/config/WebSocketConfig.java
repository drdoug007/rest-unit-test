package one.dastec.restunittest.config;

import one.dastec.restunittest.debugger.DebuggerWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DebuggerWebSocketHandler debuggerWebSocketHandler;

    public WebSocketConfig(DebuggerWebSocketHandler debuggerWebSocketHandler) {
        this.debuggerWebSocketHandler = debuggerWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(debuggerWebSocketHandler, "/ws/debugger").setAllowedOrigins("*");
    }
}

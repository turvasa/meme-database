package code.backend.CORS;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;



/**
 * Handless Cross-Origin requests
 */
public class CORSWrapper implements HttpHandler {

    
    private final HttpHandler nextHandler;



    /**
     * Creates Cross-Origin wrapper for the given HTTP handler
     * 
     * @param nextHandler HTTP handler to be wrapped
     */
    public CORSWrapper(HttpHandler nextHandler) {
        this.nextHandler = nextHandler;
    }



    @Override
    public void handle(HttpExchange exchange) throws IOException {
        CORSHelper.applyCORSHeaders(exchange);

        if (CORSHelper.handlePreflight(exchange)) return;
        nextHandler.handle(exchange);
    }
}

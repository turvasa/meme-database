package code.backend.CORS;

import java.io.IOException;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class CORSHelper {


    public static void applyCORSHeaders(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
    }


    public static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("OPTIONS")) {
            applyCORSHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return true;
        }

        return false;
    }
    
}

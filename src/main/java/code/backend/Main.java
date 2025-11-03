package code.backend;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.TreeSet;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONException;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import code.backend.CORS.CORSWrapper;
import code.backend.handlers.HelpHandler;
import code.backend.handlers.MemeHandler;
import code.backend.handlers.MemeSearchHandler;
import code.backend.handlers.ServerHandler;
import code.backend.handlers.UserHandler;
import code.backend.user.UserAuthenticator;

public class Main {
    public static void main(String[] args) throws JSONException, IOException {

		// Checks that args have right amount (2) arguments
		if (args.length != 2) {
            throw new IllegalArgumentException("\n[ERROR] - MAIN : Insufficient main arguments. Expected keystore and password.");
        }

		// Set arguments
		String keystorePath = args[0];
    	String keystorePassword = args[1];

		try {
			// Create the https server to port 8001 with default logger
			HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
			SSLContext sslContext = serverSSLContext(keystorePath, keystorePassword);
			configureServer(server, sslContext);

			// Create database
			Database database = Database.open("memes.db");

			// Load the database to memory
			TreeSet<Tag> allTags = database.getTagSet();

			// Configure authenticator
			UserAuthenticator authenticator = new UserAuthenticator(database);

			// Create CORS contexts
            createCORSContext(server, "/api", new ServerHandler());
			HttpContext help = createCORSContext(server, "/api/help", new HelpHandler());
			HttpContext registration = createCORSContext(server, "/api/user/registration", new UserHandler(authenticator));
			HttpContext post = createCORSContext(server, "/api/meme", new MemeHandler(database, allTags));
			HttpContext search = createCORSContext(server, "/api/meme/search", new MemeSearchHandler(database, allTags));

			// Set authenticators
			help.setAuthenticator(null);
			registration.setAuthenticator(null);
			post.setAuthenticator(authenticator);
			search.setAuthenticator(null);

			// Creates a default executor
			server.setExecutor(Executors.newCachedThreadPool());

			// Start server
			server.start();
			System.out.println("Server started\n");
		}

		catch (Exception e) {
			System.out.println(
				"""
                [ERROR] - MAIN: Served did not start. 
                \r"""+e.getMessage()
			);
		}
	}




    /**
    * Creates SSL context
    *
    * @param  keystorePath Path to the private key
    * @param  keystorePassword Password for the private key
    * @return SSL context
    */
	private static SSLContext serverSSLContext(String keystorePath, String keystorePassword) throws Exception {
		char[] passphrase = keystorePassword.toCharArray();
		KeyStore keystore = KeyStore.getInstance("JKS");
		keystore.load(new FileInputStream(keystorePath), passphrase);

		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		keyManagerFactory.init(keystore, passphrase);

		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
		trustManagerFactory.init(keystore);

		SSLContext ssl = SSLContext.getInstance("TLS");
		ssl.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

		return ssl;
	}




    /**
    * Configures created HTTP server to be HTTPS
    *
    * @param  server Created HTTP server
    * @param  sslContext Created SSL context
    */
	private static void configureServer(HttpsServer server, SSLContext sslContext) {
		// Configurate server 
		server.setHttpsConfigurator (
			new HttpsConfigurator(sslContext) {
				@Override
				public void configure (HttpsParameters params) {
					//InetSocketAddress remote = params.getClientAddress();
					SSLContext c = getSSLContext();
					SSLParameters sslparams = c.getDefaultSSLParameters();
					params.setSSLParameters(sslparams);
				}
			}
		);
	}




	private static HttpContext createCORSContext(HttpServer server, String path, HttpHandler handler) {
		CORSWrapper corsWrapper = new CORSWrapper(handler);

		// CORS context
		return server.createContext(path, corsWrapper);

		// Normal context
		//return server.createContext(path, handler);
	}
}
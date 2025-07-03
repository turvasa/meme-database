package code;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import org.json.JSONArray;
import org.json.JSONException;

public class Main {
    public static void main(String[] args) throws JSONException, IOException {

		// Checks that args have right amount (2) arguments
		if (args.length != 2) {
            throw new IllegalArgumentException("\nInsufficient main arguments. Expected keystore and password.");
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
			Database database = Database.open("database.db");

			// Load the database to memory
			Set<Tag> allTags = database.getTagSet();
			SortedMap<Meme, Integer> memes = database.getMemesTree();

			// Configure authenticator
			UserAuthenticator authenticator = new UserAuthenticator(database);

			// Create contexts
            HttpContext mainPage = server.createContext("", new ServerHandler());
			HttpContext help = server.createContext("/help", new HelpHandler());
			HttpContext registration = server.createContext("/registration", new UserHandler(authenticator));
			HttpContext search = server.createContext("/search", new SearchHandler(database, allTags, memes));

			// Set authenticators
			help.setAuthenticator(null);
			registration.setAuthenticator(null);
			search.setAuthenticator(authenticator);

			// Creates a default executor
			server.setExecutor(Executors.newCachedThreadPool());

			// Start server
			server.start();
			System.out.println("Server started\n");
		}

		catch (Exception e) {
			System.out.println("\nServed did not start: "+e.getMessage());
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

}
package com.coresystems.sparky;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.coresystems.sparky.store.MockStore;
import com.coresystems.sparky.store.Registration;

import java.io.IOException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static spark.Spark.post;
import static spark.Spark.staticFileLocation;

/**
 * Spark uses "route matching" (such as a get to obtain a token). The first
 * route that matches will be invoked. You can find more information on how it
 * works here: http://sparkjava.com/documentation.html
 *
 * @author zafr
 */
public final class Webapp {
    private static final String DATA_TYPE_JSON = "application/json";
    // For starters this should be enough, later we can increase the max capacity based on resources/experience
    //TODO to connect the Redis server, we'll simply use a fallback strategy: If redis not available > use in-memory storage
    private static final SparkyService service = new SparkyService(new MockStore(5000));
    static final Logger logger;
    static final Gson gson = new Gson();

    static {
        logger = Logger.getLogger("com.twilio.Sparky");
        try {
            logger.addHandler(new FileHandler("sparky.log"));
        } catch (IOException e) {
            Log.e(logger.getName(), "Failed to create file handler for logging.", e);
        }
        logger.setLevel(Level.ALL);

        logger.info("Sparky is up and running.");
    }

    public static void main(String[] args) {
        // Serve static files from src/main/resources/public
        staticFileLocation("/public");
        logger.info("Received request from client.");
        /**
         * Creates a new or loads an existing access token using the twilio credentials.
         */
        post("/token", DATA_TYPE_JSON, (request, response) -> service.getAuthenticationJson(SparkyService.getStaticProperties(), request.body()));

        /**
         * Request a list of currently authenticated users. This requires an account to be passed, so we can filter
         * the users by that account.
         */
        post("/users", DATA_TYPE_JSON, (request, response) -> gson.toJson(service.getAuthenticatedUsersByAccount(request.body()), new TypeToken<List<Registration>>() {
        }.getType()));

        /**
         * Unregister as an active user.
         */
        post("/unregister", DATA_TYPE_JSON, (request, response) -> gson.toJson(service.removeRegistration(request.body())));
    }

}

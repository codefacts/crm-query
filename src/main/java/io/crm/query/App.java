package io.crm.query;

import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class App {
    private EventBus bus;
    private Vertx vertx;
    private MongoClient mongoClient;
    private JsonObject mongoConfig;
    private ConfigurableApplicationContext context;

    private static final ThreadLocal<DateFormat> dateFormatThreadLocal = defaultDateFormatThreadLocal();
    public static volatile Runnable testRun;

    public static DateFormat defaultDateFormat() {
        return dateFormatThreadLocal.get();
    }

    public EventBus getBus() {
        return bus;
    }

    public Vertx getVertx() {
        return vertx;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public JsonObject getMongoConfig() {
        return mongoConfig;
    }

    public ConfigurableApplicationContext getContext() {
        return context;
    }

    void initialize(EventBus bus, Vertx vertx, MongoClient mongoClient, JsonObject mongoConfig, ConfigurableApplicationContext context) {
        this.bus = bus;
        this.vertx = vertx;
        this.mongoClient = mongoClient;
        this.mongoConfig = mongoConfig;
        this.context = context;
    }

    public static void main(String... args) {
        Vertx.clusteredVertx(new VertxOptions(), new Handler<AsyncResult<Vertx>>() {

            @Override
            public void handle(AsyncResult<Vertx> e) {
                if (e.succeeded()) {
                    System.out.println("VERTEX CLUSTER STARTED");
                    e.result().deployVerticle(MainVerticle.class.getName(), new DeploymentOptions()
                            .setInstances(8));
                } else {
                    System.out.println("ERROR STARTING VERTEX CLUSTER");
                }
            }
        });
    }

    private static ThreadLocal<DateFormat> defaultDateFormatThreadLocal() {
        return new ThreadLocal<DateFormat>() {
            @Override
            protected DateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            }
        };
    }
}

package io.crm.query;

import io.crm.mc;
import io.crm.query.service.*;
import io.crm.util.AsyncUtil;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.apache.commons.io.IOUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.io.InputStream;

import static io.crm.query.Events.*;

/**
 * Created by someone on 08-Jul-2015.
 */
public class MainVerticle extends AbstractVerticle {
    private static final String FIND_ALL_USER_TYPES = "FIND_ALL_USER_TYPES";
    private Future<Void> startFuture;
    private int count = mc.values().length;
    private App app;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        System.out.println("--------------Strating verticle");
        this.startFuture = startFuture;

        final JsonObject config = new JsonObject(loadConfig("/mongo-config.json"));
        final MongoClient mongoClient = MongoClient.createShared(getVertx(), config);
        final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext("io.crm.query", "io.crm.query.service", "io.crm.query.codec");
        context.start();

        app = context.getBean(App.class);
        app.initialize(getVertx().eventBus(), getVertx(), mongoClient, config, context);

        onDbInialized();

        System.out.println("--------------Verticle complete");
    }

    private void onFail(Throwable throwable) {
        if (startFuture != null && !startFuture.isComplete()) {
            startFuture.fail(throwable);
            startFuture = null;
            System.out.println("<------------------------FAILED----------------------->");
        }
    }

    private void onDbInialized() {
        onSpringContextLoaded(app.getContext());
    }

    private void onSpringContextLoaded(final ConfigurableApplicationContext context) {
        registerCodecs(context);
        registerEvents(context);
        onComplete();
    }

    private void registerCodecs(ConfigurableApplicationContext ctx) {
    }

    private void registerEvents(ConfigurableApplicationContext ctx) {
        final EventBus bus = getVertx().eventBus();

        bus.consumer(GET_DB_TREE, (Message<JsonObject> m) -> ctx.getBean(DbTreeService.class).treeWithSummary(m));
        bus.consumer(GET_COLLECTION_COUNT, ctx.getBean(QueryService.class)::count);

        bus.consumer(FIND_ALL_REGIONS, ctx.getBean(QueryService.class)::listRegions);
        bus.consumer(FIND_ALL_AREAS, ctx.getBean(QueryService.class)::listAreas);
        bus.consumer(FIND_ALL_HOUSES, (Handler<Message<JsonObject>>) (message) -> {
            ctx.getBean(QueryService.class).listHouses(message);
            System.out.println("RETURNED");
        });
        bus.consumer(FIND_ALL_BRANDS, ctx.getBean(QueryService.class)::listBrands);
        bus.consumer(FIND_ALL_LOCATIONS, ctx.getBean(QueryService.class)::listLocations);
        bus.consumer(FIND_ALL_CLIENTS, ctx.getBean(QueryService.class)::listClients);
        bus.consumer(FIND_ALL_EMPLOYEES, ctx.getBean(QueryService.class)::listEmployees);
        bus.consumer(FIND_ALL_CONTACTS, ctx.getBean(QueryService.class)::listContacts);
        bus.consumer(FIND_ALL_USER_TYPES, ctx.getBean(QueryService.class)::listUserTypes);
        bus.consumer(Events.FIND_EMPLOYEE, ctx.getBean(QueryService.class)::findEmployee);
    }

    public static String loadConfig(String file) {
        InputStream stream = MainVerticle.class.getResourceAsStream(file);
        try {
            String string = IOUtils.toString(stream, "UTF-8");
            return string;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "{}";
    }

    private void onComplete() {
        startFuture.complete();
        startFuture = null;
        System.out.println("<-------------------COMPLETE-------------------->");
        if (App.testRun != null) App.testRun.run();
    }

    @Override
    public void stop() throws Exception {
        app.getContext().close();
        app.getMongoClient().close();
    }
}

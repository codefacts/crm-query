package io.crm.query.service;

import io.crm.mc;
import io.crm.query.App;
import io.crm.query.model.Query;
import io.crm.query.model.User;
import io.crm.util.ExceptionUtil;
import io.crm.util.TaskCoordinator;
import io.crm.util.TaskCoordinatorBuilder;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;

import static io.crm.query.model.Query.count;
import static io.crm.util.ExceptionUtil.withReply;

/**
 * Created by someone on 12-Aug-2015.
 */
@Component
final public class QueryService {
    private final App app;

    @Autowired
    public QueryService(App app) {
        this.app = app;
    }

    public void count(Message message) {

        final JsonObject collectionInfo = new JsonObject(new LinkedHashMap<>());
        final TaskCoordinator coordinator = TaskCoordinatorBuilder.create()
                .count(mc.values().length).message(message)
                .onSuccess(() ->
                        message.reply(collectionInfo)).get();

        Arrays.asList(mc.values()).forEach(k -> {
            final String name = k.name();
            app.getMongoClient().count(name, new JsonObject(), coordinator.add(count -> {
                collectionInfo.put(name, count);
            }));
        });
    }

    public void listAreas(Message<JsonObject> message) {
        app.getMongoClient().find(mc.areas.name(), new JsonObject(), withReply(list ->
                message.reply(new JsonArray(list)), message));
    }

    public void listRegions(Message<JsonObject> message) {
        app.getMongoClient().find(mc.regions.name(), new JsonObject(), withReply(list ->
                message.reply(new JsonArray(list)), message));
    }

    public void listHouses(Message<JsonObject> message) {
        app.getMongoClient().find(mc.distribution_houses.name(), new JsonObject(), withReply(list ->
                message.reply(new JsonArray(list)), message));
    }

    public void listLocations(Message<JsonObject> message) {
        app.getMongoClient().find(mc.locations.name(), new JsonObject(), withReply(list ->
                message.reply(new JsonArray(list)), message));
    }

    public void listBrands(Message<JsonObject> message) {
        app.getMongoClient().find(mc.brands.name(), new JsonObject(), withReply(list ->
                message.reply(new JsonArray(list)), message));
    }

    public void listEmployees(Message<JsonObject> message) {
        final JsonObject body = message.body();
        final JsonObject criteria = body != null && body.containsKey(Query.params) ? body.getJsonObject(Query.params) : new JsonObject();
        app.getMongoClient().find(mc.employees.name(), criteria, withReply(list ->
                message.reply(new JsonArray(list)), message));
    }

    public void listClients(Message<JsonObject> message) {
        app.getMongoClient().find(mc.clients.name(), new JsonObject(), withReply(list ->
                message.reply(new JsonArray(list)), message));
    }

    public void listContacts(Message<JsonObject> message) {
        app.getMongoClient().find(mc.consumer_contacts.name(), new JsonObject(), withReply(list ->
                message.reply(new JsonArray(list)), message));
    }

    public void listUserTypes(Message<JsonObject> message) {
        app.getMongoClient().find(mc.user_types.name(), new JsonObject(), withReply(list -> {
            final TaskCoordinator taskCoordinator = TaskCoordinatorBuilder.create().count(list.size())
                    .onSuccess(() -> message.reply(new JsonArray(list)))
                    .onError(e -> ExceptionUtil.fail(message, e)).get();

            list.forEach(userType -> {
                app.getMongoClient().count(mc.employees.name(), new JsonObject()
                        .put(Query.userTypeId, userType.getLong(Query.id)), taskCoordinator.add(userCount -> userType.put(Query.count, userCount)));
            });
        }, message));
    }

    public void listCampaigns(Message<JsonObject> message) {
        app.getMongoClient().find(mc.campaigns.name(), new JsonObject(), withReply(list ->
                message.reply(new JsonArray(list)), message));
    }

    public void findEmployee(Message<String> message) {
        app.getMongoClient().findOne(mc.employees.name(), new JsonObject()
                .put(User.userId, message.body()), new JsonObject(), ExceptionUtil.withReply(j -> message.reply(j), message));
    }

    public static void main(String... args) throws Exception {

    }
}

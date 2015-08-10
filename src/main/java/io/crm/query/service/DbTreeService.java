package io.crm.query.service;

import io.crm.query.App;
import io.crm.query.MongoCollections;
import io.crm.query.model.*;
import io.crm.query.util.ExceptionUtil;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by someone on 30-Jul-2015.
 */
@Component
public class DbTreeService {

    public void treeWithSummary(Message<JsonObject> message) {
        App.mongoClient.find(MongoCollections.region, new JsonObject(), event -> {
            if (event.failed()) {
                ExceptionUtil.fail(message, event.cause());
                return;
            }

            final List<JsonObject> regions = event.result();
            CC regionCount = new CC(regions.size());

            if (regionCount.count <= 0) {
                onTreeComplete(message, regions);
            }

            regions.forEach(region -> {
                App.mongoClient.find(MongoCollections.area, new JsonObject().put(Area.region, region.getString(Model.id)), event1 -> {
                    if (event1.failed()) {
                        ExceptionUtil.fail(message, event1.cause());
                        return;
                    }

                    final List<JsonObject> areas = event1.result();
                    region.put(MongoCollections.area, areas);
                    CC areaCount = new CC(areas.size());

                    Runnable onAreaTracker = () -> {
                        if (areaCount.count <= 0) {
                            regionCount.count--;
                            if (regionCount.count <= 0) {
                                onTreeComplete(message, regions);
                            }
                        }
                    };
                    onAreaTracker.run();
                    onAreaFound(areas, message, regions, onAreaTracker, areaCount);
                });
            });
        });
    }

    private void onAreaFound(List<JsonObject> areas, Message<JsonObject> message, List<JsonObject> regions, Runnable onAreaTracker, CC areaCount) {
        areas.forEach(area -> {
            App.mongoClient.find(MongoCollections.distribution_house, new JsonObject().put(House.area, area.getString(Model.id)), event2 -> {
                if (event2.failed()) {
                    ExceptionUtil.fail(message, event2.cause());
                    return;
                }

                final List<JsonObject> houses = event2.result();
                area.put(MongoCollections.distribution_house, houses);
                CC houseCount = new CC(houses.size());

                Runnable onHouseTraker = () -> {
                    if (houseCount.count <= 0) {
                        areaCount.count--;
                        onAreaTracker.run();
                    }
                };
                onHouseTraker.run();
                onHouseFound(houses, message, areas, onHouseTraker, houseCount);
            });
        });
    }

    private void onHouseFound(List<JsonObject> houses, Message<JsonObject> message, List<JsonObject> areas, Runnable onHouseTraker, CC houseCount) {
        houses.forEach(house -> {
            App.mongoClient.find(MongoCollections.br, new JsonObject().put(Br.distributionHouse, house.getString(Model.id)), event3 -> {
                if (event3.failed()) {
                    ExceptionUtil.fail(message, event3.cause());
                    return;
                }

                final List<JsonObject> brs = event3.result();
                house.put(MongoCollections.br, brs);
                CC brsCount = new CC(brs.size());

                Runnable brSuccessTracker = () -> {

                    if (brsCount.count <= 0) {
                        houseCount.count--;
                        onHouseTraker.run();
                    }
                };

                brSuccessTracker.run();
                onBrsFound(brs, message, houses, brSuccessTracker, brsCount);
            });
        });
    }

    private void onBrsFound(List<JsonObject> brs, Message<JsonObject> message, List<JsonObject> houses, Runnable brSuccessTracker, CC brsCount) {

        brs.forEach(br -> {

            summarizeBr(br, message, touple -> {
                br
                        .put(Query.contactCount, touple.total)
                        .put(Query.ptrCount, touple.ptr)
                        .put(Query.swpCount, touple.swp);
                brsCount.count--;
                brSuccessTracker.run();
            });
        });
    }

    private void summarizeBr(JsonObject br, Message message, java.util.function.Consumer<Touple> onSuccess) {
        CC queryCount = new CC(3);
        Touple touple = new Touple();

        App.mongoClient.count(MongoCollections.consumer_contact, new JsonObject().put(Contact.br, br.getString(Model.id)),
                totalCountResult -> {
                    if (totalCountResult.failed()) {
                        ExceptionUtil.fail(message, totalCountResult.cause());
                        return;
                    }
                    touple.total = totalCountResult.result();
                    queryCount.count--;
                    if (queryCount.count <= 0) {
                        onSuccess.accept(touple);
                    }
                });

        App.mongoClient.count(MongoCollections.consumer_contact, new JsonObject().put(Contact.br, br.getString(Model.id))
                .put(Contact.ptr, true), ptrCountResult -> {
            if (ptrCountResult.failed()) {
                ExceptionUtil.fail(message, ptrCountResult.cause());
                return;
            }
            touple.ptr = ptrCountResult.result();
            queryCount.count--;
            if (queryCount.count <= 0) {
                onSuccess.accept(touple);
            }
        });

        App.mongoClient.count(MongoCollections.consumer_contact, new JsonObject().put(Contact.br, br.getString(Model.id))
                .put(Contact.swp, true), swpCountResult -> {
            if (swpCountResult.failed()) {
                ExceptionUtil.fail(message, swpCountResult.cause());
                return;
            }
            touple.swp = swpCountResult.result();
            queryCount.count--;
            if (queryCount.count <= 0) {
                onSuccess.accept(touple);
            }
        });
    }

    private void onTreeComplete(Message message, List<JsonObject> regions) {
        long ptrTotal = 0, swpTotal = 0, contactTotal = 0;
        for (JsonObject region : regions) {
            long ptrRegion = 0, swpRegion = 0, contactRegion = 0;
            for (Object areaObj : region.getJsonArray(MongoCollections.area)) {
                JsonObject area = (JsonObject) areaObj;
                long ptrArea = 0, swpArea = 0, contactArea = 0;
                for (Object houseObj : area.getJsonArray(MongoCollections.distribution_house)) {
                    JsonObject house = (JsonObject) houseObj;
                    long ptrHouse = 0, swpHouse = 0, contactHouse = 0;
                    for (Object brObj : house.getJsonArray(MongoCollections.br)) {
                        JsonObject br = (JsonObject) brObj;
                        ptrHouse += br.getLong(Query.ptrCount);
                        swpHouse += br.getLong(Query.swpCount);
                        contactTotal += br.getLong(Query.contactCount);
                    }
                    house.put(Query.ptrCount, ptrHouse)
                            .put(Query.swpCount, swpHouse)
                            .put(Query.contactCount, contactHouse);
                    ptrArea += ptrHouse;
                    swpArea += swpHouse;
                    contactArea += contactHouse;
                }
                area.put(Query.ptrCount, ptrArea)
                        .put(Query.swpCount, swpArea)
                        .put(Query.contactCount, contactArea);
                ptrRegion += ptrArea;
                swpRegion += swpArea;
                contactRegion += contactArea;
            }
            region.put(Query.ptrCount, ptrRegion)
                    .put(Query.swpCount, swpRegion)
                    .put(Query.contactCount, contactRegion);
            ptrTotal += ptrRegion;
            swpTotal += swpRegion;
            contactTotal += contactRegion;
        }
        JsonObject tree = new JsonObject().put(Query.contactCount, contactTotal)
                .put(Query.ptrCount, ptrTotal).put(Query.swpCount, swpTotal)
                .put(MongoCollections.region, regions);

        message.reply(tree);
    }

    class Touple {
        long total, ptr, swp;
    }

    class CC {
        int count = 0;

        CC(int count) {
            this.count = count;
        }
    }
}

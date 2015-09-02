package io.crm.query.service;

import io.crm.mc;
import io.crm.query.App;
import io.crm.query.model.EmployeeType;
import io.crm.query.model.Query;
import io.crm.query.model.User;
import io.crm.util.ExceptionUtil;
import io.crm.util.TaskCoordinator;
import io.crm.util.TaskCoordinatorBuilder;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static io.crm.query.model.Query.*;
import static io.crm.util.ExceptionUtil.withReply;

/**
 * Created by someone on 01/09/2015.
 */
@Component
public class DbTreeWithUsers {
    private final App app;

    @Autowired
    public DbTreeWithUsers(App app) {
        this.app = app;
    }

    public void treeWithSummary(Message<JsonObject> message) {
        new TreeBuilder(app).treeWithSummary(message);
    }

    public static final class TreeBuilder {
        private final App app;
        private int areaTotal = 0;
        private int houseTotal = 0;
        private int brTotal = 0;
        private int locationTotal = 0;
        private int acTotal = 0;
        private int supTotal = 0;

        public TreeBuilder(App app) {
            this.app = app;
        }

        @Override
        public String toString() {
            return "TreeBuilder{" +
                    "app=" + app +
                    ", areaTotal=" + areaTotal +
                    ", houseTotal=" + houseTotal +
                    ", brTotal=" + brTotal +
                    ", locationTotal=" + locationTotal +
                    ", acTotal=" + acTotal +
                    ", supTotal=" + supTotal +
                    '}';
        }

        public void treeWithSummary(Message<JsonObject> message) {
            final MongoClient mongoClient = app.getMongoClient();

            mongoClient.find(mc.regions.name(), new JsonObject(), withReply(regionList -> {
                final TaskCoordinator regionTaskCoordinator = TaskCoordinatorBuilder.create().count(regionList.size())
                        .onSuccess(() -> {
                            message.reply(new JsonObject()
                                    .put(Query.regions, regionList)
                                    .put(Query.regionCount, regionList.size())
                                    .put(Query.areaCount, areaTotal)
                                    .put(Query.houseCount, houseTotal)
                                    .put(Query.locationCount, locationTotal)
                                    .put(Query.acCount, acTotal)
                                    .put(Query.supCount, supTotal)
                                    .put(Query.brCount, brTotal));
                            System.err.print(this);
                        })
                        .message(message)
                        .get();

                regionList.forEach(region -> {

                    mongoClient.find(mc.areas.name(), new JsonObject()
                            .put(Query.regionId, region.getLong(Query.id)), withReply(
                            areaList -> {
                                final int areaListSize = areaList.size();
                                region.put(mc.areas.name(), areaList);
                                region.put(Query.areaCount, areaListSize);
                                areaTotal += areaListSize;

                                final TaskCoordinator areaTaskCoordinator = TaskCoordinatorBuilder.create().count(areaListSize * 2)
                                        .onComplete(t -> regionTaskCoordinator.countdown())
                                        .message(message)
                                        .get();

                                areaList.forEach(area -> {

                                    mongoClient.find(mc.distribution_houses.name(), new JsonObject()
                                            .put(Query.areaId, area.getLong(Query.id)), withReply(
                                            houseList -> {
                                                final int houseListSize = houseList.size();
                                                area.put(mc.distribution_houses.name(), houseList);
                                                area.put(Query.houseCount, houseListSize);
                                                region.put(Query.houseCount, region.getInteger(Query.houseCount, 0) + houseListSize);
                                                houseTotal += houseListSize;

                                                final TaskCoordinator houseTaskCoordinator = TaskCoordinatorBuilder.create().count(houseListSize * 3)
                                                        .onComplete(t -> areaTaskCoordinator.countdown())
                                                        .message(message)
                                                        .get();

                                                houseList.forEach(house -> {

                                                    mongoClient.find(mc.employees.name(), new JsonObject()
                                                            .put(Query.userTypeId, EmployeeType.br.id)
                                                            .put(Query.houseId, house.getLong(Query.id)), houseTaskCoordinator.add(
                                                            brList -> {
                                                                final int size = brList.size();
                                                                house.put(Query.brs, brList);
                                                                house.put(Query.brCount, size);
                                                                area.put(Query.brCount, area.getInteger(Query.brCount, 0));
                                                                region.put(Query.brCount, region.getInteger(Query.brCount, 0));
                                                                brTotal += size;
                                                            }));

                                                    mongoClient.find(mc.employees.name(), new JsonObject()
                                                            .put(Query.userTypeId, EmployeeType.br_supervisor.id)
                                                            .put(Query.houseId, house.getLong(Query.id)), houseTaskCoordinator.add(
                                                            supsList -> {
                                                                final int size = supsList.size();
                                                                house.put(brSupervisors, supsList);
                                                                house.put(supCount, size);
                                                                area.put(supCount, area.getInteger(supCount, 0));
                                                                region.put(supCount, region.getInteger(supCount, 0));
                                                                supTotal += size;
                                                            }));

                                                    mongoClient.find(mc.locations.name(), new JsonObject()
                                                            .put(Query.houseId, house.getLong(Query.id)), houseTaskCoordinator.add(
                                                            locationList -> {
                                                                final int size = locationList.size();
                                                                house.put(locations, locationList);
                                                                house.put(locationCount, size);
                                                                area.put(locationCount, area.getInteger(locationCount, 0));
                                                                region.put(locationCount, region.getInteger(locationCount, 0));
                                                                locationTotal += size;
                                                            }));
                                                });
                                            }, message));

                                    mongoClient.find(mc.employees.name(), new JsonObject()
                                            .put(Query.userTypeId, EmployeeType.area_coordinator.id)
                                            .put(Query.areaId, area.getLong(Query.id)), areaTaskCoordinator.add(
                                            acList -> {
                                                final int size = acList.size();
                                                area.put(Query.areaCoordinators, acList);
                                                area.put(Query.acCount, size);
                                                region.put(Query.acCount, region.getInteger(Query.acCount, 0));
                                                acTotal += size;
                                            }));
                                });
                            }, message));
                });

            }, message));
        }
    }
}

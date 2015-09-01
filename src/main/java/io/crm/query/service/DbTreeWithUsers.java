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
@Scope("prototype")
public class DbTreeWithUsers {
    private final App app;

    private Map<Long, JsonObject> regionMap = new HashMap<>();
    private int regionTotal = 0;
    private int areaTotal = 0;
    private int houseTotal = 0;
    private int brTotal = 0;
    private int locationTotal = 0;
    private int acTotal = 0;
    private int supTotal = 0;

    @Autowired
    public DbTreeWithUsers(App app) {
        this.app = app;
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
                    })
                    .message(message)
                    .get();

            regionList.forEach(region -> {

                mongoClient.find(mc.areas.name(), new JsonObject()
                        .put(Query.regionId, region.getLong(Query.id)), regionTaskCoordinator.add(
                        areaList -> {
                            region.put(mc.areas.name(), areaList);
                            region.put(Query.areaCount, areaList.size());
                            areaTotal += areaList.size();

                            final TaskCoordinator areaTaskCoordinator = TaskCoordinatorBuilder.create().count(areaList.size())
                                    .message(message)
                                    .get();

                            final TaskCoordinator areaTaskCoordinator2 = TaskCoordinatorBuilder.create().count(areaList.size())
                                    .message(message)
                                    .get();

                            areaList.forEach(area -> {

                                mongoClient.find(mc.distribution_houses.name(), new JsonObject()
                                        .put(Query.areaId, area.getLong(Query.id)), areaTaskCoordinator.add(
                                        houseList -> {
                                            area.put(mc.distribution_houses.name(), houseList);
                                            area.put(Query.houseCount, houseList.size());
                                            region.put(Query.houseCount, region.getInteger(Query.houseCount, 0) + houseList.size());
                                            houseTotal += houseList.size();

                                            final TaskCoordinator houseTaskCoordinator = TaskCoordinatorBuilder.create().count(houseList.size())
                                                    .message(message)
                                                    .get();

                                            final TaskCoordinator houseTaskCoordinator2 = TaskCoordinatorBuilder.create().count(houseList.size())
                                                    .message(message)
                                                    .get();

                                            houseList.forEach(house -> {
                                                mongoClient.find(mc.employees.name(), new JsonObject()
                                                        .put(Query.userTypeId, EmployeeType.br.id)
                                                        .put(Query.houseId, house.getLong(Query.id)), houseTaskCoordinator.add(
                                                        brList -> {
                                                            house.put(Query.brs, brList);
                                                            house.put(Query.brCount, brList.size());
                                                            area.put(Query.brCount, area.getInteger(Query.brCount, 0));
                                                            region.put(Query.brCount, region.getInteger(Query.brCount, 0));
                                                            brTotal += brList.size();
                                                        }));

                                                mongoClient.find(mc.employees.name(), new JsonObject()
                                                        .put(Query.userTypeId, EmployeeType.br_supervisor.id)
                                                        .put(Query.houseId, house.getLong(Query.id)), houseTaskCoordinator.add(
                                                        supsList -> {
                                                            house.put(brSupervisors, supsList);
                                                            house.put(supCount, supsList.size());
                                                            area.put(supCount, area.getInteger(supCount, 0));
                                                            region.put(supCount, region.getInteger(supCount, 0));
                                                            supTotal += supsList.size();
                                                        }));

                                                mongoClient.find(mc.locations.name(), new JsonObject()
                                                        .put(Query.houseId, house.getLong(Query.id)), houseTaskCoordinator2.add(
                                                        locationList -> {
                                                            house.put(locations, locationList);
                                                            house.put(locationCount, locationList.size());
                                                            area.put(locationCount, area.getInteger(locationCount, 0));
                                                            region.put(locationCount, region.getInteger(locationCount, 0));
                                                            locationTotal += locationList.size();
                                                        }));
                                            });
                                        }));

                                mongoClient.find(mc.employees.name(), new JsonObject()
                                        .put(Query.userTypeId, EmployeeType.area_coordinator.id)
                                        .put(Query.areaId, area.getLong(Query.id)), areaTaskCoordinator2.add(
                                        acList -> {
                                            area.put(Query.areaCoordinators, acList);
                                            area.put(Query.acCount, acList.size());
                                            region.put(Query.acCount, region.getInteger(Query.acCount, 0));
                                            acTotal += acList.size();
                                        }));
                            });
                        }));
            });

        }, message));
    }
}

package io.crm.query.service;

import io.crm.QC;
import io.crm.mc;
import io.crm.query.App;
import io.crm.model.EmployeeType;
import io.crm.util.TaskCoordinator;
import io.crm.util.TaskCoordinatorBuilder;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static io.crm.QC.*;
import static io.crm.util.ExceptionUtil.withReply;

/**
 * Created by someone on 01/09/2015.
 */
@Component
final public class DbTreeWithUsers {
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
                                    .put(QC.regions, regionList)
                                    .put(QC.regionCount, regionList.size())
                                    .put(QC.areaCount, areaTotal)
                                    .put(QC.houseCount, houseTotal)
                                    .put(QC.locationCount, locationTotal)
                                    .put(QC.acCount, acTotal)
                                    .put(QC.supCount, supTotal)
                                    .put(QC.brCount, brTotal));
                            System.err.print(this);
                        })
                        .message(message)
                        .get();

                regionList.forEach(region -> {

                    mongoClient.find(mc.areas.name(), new JsonObject()
                            .put(QC.regionId, region.getLong(QC.id)), regionTaskCoordinator.catchOnException(
                            areaList -> {
                                final int areaListSize = areaList.size();
                                region.put(mc.areas.name(), areaList);
                                region.put(QC.areaCount, areaListSize);
                                areaTotal += areaListSize;

                                final TaskCoordinator areaTaskCoordinator = TaskCoordinatorBuilder.create().count(areaListSize * 2)
                                        .onComplete(t -> regionTaskCoordinator.countdown())
                                        .message(message)
                                        .get();

                                areaList.forEach(area -> {

                                    mongoClient.find(mc.distributionHouses.name(), new JsonObject()
                                            .put(QC.areaId, area.getLong(QC.id)), areaTaskCoordinator.catchOnException(
                                            houseList -> {
                                                final int houseListSize = houseList.size();
                                                area.put(mc.distributionHouses.name(), houseList);
                                                area.put(QC.houseCount, houseListSize);
                                                region.put(QC.houseCount, region.getInteger(QC.houseCount, 0) + houseListSize);
                                                houseTotal += houseListSize;

                                                final TaskCoordinator houseTaskCoordinator = TaskCoordinatorBuilder.create().count(houseListSize * 3)
                                                        .onComplete(t -> areaTaskCoordinator.countdown())
                                                        .message(message)
                                                        .get();

                                                houseList.forEach(house -> {

                                                    mongoClient.find(mc.employees.name(), new JsonObject()
                                                            .put(QC.userTypeId, EmployeeType.br.id)
                                                            .put(QC.houseId, house.getLong(QC.id)), houseTaskCoordinator.add(
                                                            brList -> {
                                                                final int size = brList.size();
                                                                house.put(QC.brs, brList);
                                                                house.put(QC.brCount, size);
                                                                area.put(QC.brCount, area.getInteger(QC.brCount, 0));
                                                                region.put(QC.brCount, region.getInteger(QC.brCount, 0));
                                                                brTotal += size;
                                                            }));

                                                    mongoClient.find(mc.employees.name(), new JsonObject()
                                                            .put(QC.userTypeId, EmployeeType.br_supervisor.id)
                                                            .put(QC.houseId, house.getLong(QC.id)), houseTaskCoordinator.add(
                                                            supsList -> {
                                                                final int size = supsList.size();
                                                                house.put(brSupervisors, supsList);
                                                                house.put(supCount, size);
                                                                area.put(supCount, area.getInteger(supCount, 0));
                                                                region.put(supCount, region.getInteger(supCount, 0));
                                                                supTotal += size;
                                                            }));

                                                    mongoClient.find(mc.locations.name(), new JsonObject()
                                                            .put(QC.houseId, house.getLong(QC.id)), houseTaskCoordinator.add(
                                                            locationList -> {
                                                                final int size = locationList.size();
                                                                house.put(locations, locationList);
                                                                house.put(locationCount, size);
                                                                area.put(locationCount, area.getInteger(locationCount, 0));
                                                                region.put(locationCount, region.getInteger(locationCount, 0));
                                                                locationTotal += size;
                                                            }));
                                                });
                                            }));

                                    mongoClient.find(mc.employees.name(), new JsonObject()
                                            .put(QC.userTypeId, EmployeeType.area_coordinator.id)
                                            .put(QC.areaId, area.getLong(QC.id)), areaTaskCoordinator.add(
                                            acList -> {
                                                final int size = acList.size();
                                                area.put(QC.areaCoordinators, acList);
                                                area.put(QC.acCount, size);
                                                region.put(QC.acCount, region.getInteger(QC.acCount, 0));
                                                acTotal += size;
                                            }));
                                });
                            }));
                });

            }, message));
        }
    }
}

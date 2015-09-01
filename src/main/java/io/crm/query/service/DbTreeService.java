package io.crm.query.service;

import io.crm.Events;
import io.crm.mc;
import io.crm.query.App;
import io.crm.query.model.*;
import io.crm.util.ExceptionUtil;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.crm.query.model.Query.id;

/**
 * Created by someone on 30-Jul-2015.
 */
@Component
@Scope("prototype")
public class DbTreeService {
    private final App app;

    private Map<Long, JsonObject> regionMap = new HashMap<>();
    private int regionTotal = 0;
    private int areaTotal = 0;
    private int houseTotal = 0;
    private int brTotal = 0;
    private int locationTotal = 0;

    @Autowired
    public DbTreeService(App app) {
        this.app = app;
    }

    public void treeWithSummary(Message<JsonObject> message) {
        app.getMongoClient().find(mc.regions.name(), new JsonObject(), event -> {
            if (event.failed()) {
                ExceptionUtil.fail(message, event.cause());
                return;
            }

            final List<JsonObject> regions = event.result();
            regionTotal = regions.size();
            final CC regionCount = new CC(regions.size());

            if (regionCount.count <= 0) {
                onTreeComplete(message, regions);
            }

            regions.forEach(region -> {

                app.getMongoClient().find(mc.areas.name(), new JsonObject().put(Query.regionId, region.getLong(id)), event1 -> {
                    if (event1.failed()) {
                        ExceptionUtil.fail(message, event1.cause());
                        event1.cause().printStackTrace();
                        return;
                    }

                    final List<JsonObject> areas = event1.result();

                    final int aCount = areas.size();
                    areaTotal += aCount;

                    region.put(mc.areas.name(), areas);
                    region.put(Region.areaCount, aCount);

                    regionMap.put(region.getLong(id), region);

                    CC areaCount = new CC(aCount);

                    Runnable onAreaTracker = () -> {
                        if (areaCount.count <= 0) {
                            regionCount.count--;
                            if (regionCount.count <= 0) {
                                onTreeComplete(message, regions);
                            }
                        }
                    };
                    onAreaTracker.run();
                    onAreaFound(areas, message, region, onAreaTracker, areaCount);
                });
            });
        });
    }

    private void onAreaFound(List<JsonObject> areas, Message<JsonObject> message, JsonObject region, Runnable onAreaTracker, CC areaCount) {
        CC cc = new CC(0);
        areas.forEach(area -> {

            app.getMongoClient().find(mc.distribution_houses.name(), new JsonObject().put(Query.areaId, area.getLong(id)), event2 -> {
                if (event2.failed()) {
                    ExceptionUtil.fail(message, event2.cause());
                    event2.cause().printStackTrace();
                    return;
                }

                final List<JsonObject> houses = event2.result();

                final int hCount = houses.size();
                houseTotal += hCount;

                area.put(mc.distribution_houses.name(), houses);
                area.put(Area.houseCount, hCount);

                Integer regionHouseCount = region.getInteger(Area.houseCount);
                regionHouseCount = regionHouseCount == null ? 0 : regionHouseCount;
                region.put(Area.houseCount, regionHouseCount + hCount);

                CC houseCount = new CC(hCount);

                Runnable onHouseTraker = () -> {
                    if (houseCount.count <= 0) {
                        areaCount.count--;
                        onAreaTracker.run();
                    }
                };
                onHouseTraker.run();
                onHouseFound(houses, message, area, onHouseTraker, houseCount);
            });
        });
    }

    private void onHouseFound(List<JsonObject> houses, Message<JsonObject> message, JsonObject area, Runnable onHouseTraker, CC houseCount) {
        houses.forEach(house -> {

            final Touple2 touple2 = new Touple2();

            final java.util.function.Consumer<Touple2> then = t2 -> {
                final List<JsonObject> brs = t2.brs;
                final List<JsonObject> locations = t2.locations;

                final int brCount = brs.size();
                final int locationCount = locations.size();

                brTotal += brCount;
                locationTotal += locationCount;

                house.put(Query.brs, brs);
                house.put(mc.locations.name(), locations);

                house.put(House.brCount, brCount);
                house.put(House.locationCount, locationCount);

                Integer areaBrCount = area.getInteger(House.brCount);
                areaBrCount = areaBrCount == null ? 0 : areaBrCount;
                area.put(House.brCount, areaBrCount + brCount);

                Integer areaLocationCount = area.getInteger(House.locationCount);
                areaLocationCount = areaLocationCount == null ? 0 : areaLocationCount;
                area.put(House.locationCount, areaLocationCount + locationCount);

                final JsonObject region = regionMap.get(area.getJsonObject(Area.region).getLong(id));

                Integer regionBrCount = region.getInteger(House.brCount);
                regionBrCount = regionBrCount == null ? 0 : regionBrCount;
                region.put(House.brCount, regionBrCount + brCount);

                Integer regionLocationCount = region.getInteger(House.locationCount);
                regionLocationCount = regionLocationCount == null ? 0 : regionLocationCount;
                region.put(House.locationCount, regionLocationCount + locationCount);

                houseCount.count--;
                onHouseTraker.run();
            };

            final Long houseId = house.getLong(id);
            CC cc = new CC(2);
            app.getMongoClient().find(mc.employees.name(), new JsonObject()
                    .put(Query.houseId, houseId)
                    .put(Query.userTypeId, EmployeeType.br.id), event3 -> {
                if (event3.failed()) {
                    ExceptionUtil.fail(message, event3.cause());
                    event3.cause().printStackTrace();
                    return;
                }

                touple2.brs = orEmpty(event3.result());
                cc.count--;
                if (cc.count <= 0) {
                    then.accept(touple2);
                }
            });

            app.getMongoClient().find(mc.locations.name(), new JsonObject()
                    .put(Query.houseId, houseId), event3 -> {
                if (event3.failed()) {
                    ExceptionUtil.fail(message, event3.cause());
                    event3.cause().printStackTrace();
                    return;
                }

                touple2.locations = orEmpty(event3.result());
                cc.count--;
                if (cc.count <= 0) {
                    then.accept(touple2);
                }
            });


        });
    }

    private void onTreeComplete(Message message, List<JsonObject> regions) {
        final JsonObject tree = new JsonObject()
                .put(Query.regionCount, regionTotal)
                .put(Query.areaCount, areaTotal)
                .put(Query.houseCount, houseTotal)
                .put(Query.brCount, brTotal)
                .put(Query.locationCount, locationTotal)
                .put(Query.regions, regions);

        message.reply(tree);
        System.out.println("REPLY SUCCESS: " + Events.GET_DB_TREE);
    }

    static class CC {
        int count = 0;

        CC(int count) {
            this.count = count;
        }

        @Override
        public String toString() {
            return count + "";
        }
    }

    static class Touple2 {
        List<JsonObject> brs;
        List<JsonObject> locations;
    }

    private List<JsonObject> orEmpty(List<JsonObject> list) {
        return list == null ? new ArrayList<>() : list;
    }

    public static void main(String... args) {

    }
}

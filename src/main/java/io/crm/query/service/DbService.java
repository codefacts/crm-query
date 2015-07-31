package io.crm.query.service;

import io.crm.query.App;
import io.crm.query.MongoCollections;
import io.crm.query.model.Area;
import io.crm.query.model.Br;
import io.crm.query.model.DistributionHouse;
import io.crm.query.model.Model;
import io.crm.query.util.ExceptionUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by someone on 30-Jul-2015.
 */
@Component
public class DbService {

    public void treeWithSummary(Message<JsonObject> message) {
        App.mongoClient.find(MongoCollections.region, new JsonObject(), event -> {
            if (event.failed()) {
                ExceptionUtil.fail(message, event.cause());
                return;
            }
            final List<JsonObject> regions = event.result();

            regions.forEach(region -> {
                App.mongoClient.find(MongoCollections.area, new JsonObject().put(Area.region, region.getString(Model.id)), event1 -> {
                    if (event1.failed()) {
                        ExceptionUtil.fail(message, event1.cause());
                        return;
                    }

                    final List<JsonObject> areas = event1.result();
                    region.put(MongoCollections.area, areas);

                    areas.forEach(area -> {
                        App.mongoClient.find(MongoCollections.distribution_house, new JsonObject().put(DistributionHouse.area, area.getString(Model.id)), event2 -> {
                            if (event2.failed()) {
                                ExceptionUtil.fail(message, event2.cause());
                                return;
                            }

                            final List<JsonObject> houses = event2.result();
                            area.put(MongoCollections.distribution_house, houses);

                            houses.forEach(house -> {
                                App.mongoClient.find(MongoCollections.br, new JsonObject().put(Br.distributionHouse, house.getString(Model.id)), event3 -> {
                                    if (event3.failed()) {
                                        ExceptionUtil.fail(message, event3.cause());
                                        return;
                                    }

                                    final List<JsonObject> brs = event3.result();
                                    house.put(MongoCollections.br, brs);
                                });
                            });
                        });
                    });
                });
            });
        });
    }
}

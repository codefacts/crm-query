package io.crm.query.service;

import io.crm.query.App;
import io.crm.query.MongoCollections;
import io.crm.query.util.ExceptionUtil;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Component;

/**
 * Created by someone on 15-Jul-2015.
 */
@Component
public class DistributionHouseService {

    public void findAll(Message message) {
        App.mongoClient.find(MongoCollections.distribution_house, new JsonObject(), r -> {
            if (r.failed()) {
                ExceptionUtil.fail(message, r.cause());
                return;
            }
            message.reply(r.result());
        });
    }
}

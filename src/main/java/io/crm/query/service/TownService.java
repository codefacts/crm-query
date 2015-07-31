package io.crm.query.service;

import io.crm.query.App;
import io.crm.query.MongoCollections;
import io.crm.query.util.ExceptionUtil;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.springframework.stereotype.Component;

/**
 * Created by sohan on 7/27/2015.
 */
@Component
public class TownService {

    public void findAll(Message message) {
        App.mongoClient.find(MongoCollections.town, new JsonObject(), r -> {
            if (r.failed()) {
                ExceptionUtil.fail(message, r.cause());
                return;
            }
            message.reply(r.result());
        });
    }


}

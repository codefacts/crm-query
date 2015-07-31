package io.crm.query.service;

import io.crm.query.App;
import io.crm.query.Events;
import io.crm.query.model.UserType;
import io.crm.query.util.ExceptionUtil;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by someone on 15-Jul-2015.
 */
@Component
public class BrSupervisorService {
    public static final String id_prefix = "sup-";

    private final AtomicLong atomicLong = new AtomicLong(0L);
    @Autowired
    private UserService userService;

    public void create(Message<JsonObject> message) {
        final JsonObject supervisor = message.body();

        userService.create(supervisor, message, newUserId(), UserType.employee, r -> {
            if (r.failed()) {
                ExceptionUtil.fail(message, r.cause());
                return;
            }
            message.reply(null);
            App.bus.publish(Events.NEW_BR_SUPERVISOR_CREATED, supervisor);
        });
    }

    public String newUserId() {
        return String.format(id_prefix + "%04d", atomicLong.incrementAndGet());
    }
}

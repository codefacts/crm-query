package io.crm.query.service;

import io.crm.query.App;
import io.crm.query.exceptions.ValidationException;
import io.crm.query.Events;
import io.crm.query.model.AC;
import io.crm.query.model.Model;
import io.crm.query.model.UserType;
import io.crm.query.util.ExceptionUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.AsyncResultHandler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by someone on 15-Jul-2015.
 */
@Component
public class AreaCoordinatorService {
    public static final String id_prefix = "ac-";

    private final AtomicLong atomicLong = new AtomicLong(0L);
    @Autowired
    private UserIndexService userIndexService;
    @Autowired
    private EmployeeService employeeService;
    private static final String mongo_collection = "area_coordinators";
    @Autowired
    private UserService userService;

    public void create(Message<JsonObject> message) {

        final JsonObject user = message.body();

        validate(user, new AsyncResultHandler<JsonObject>() {
            @Override
            public void handle(AsyncResult<JsonObject> jsonObjectAsyncResult) {

                userService.create(user, message, newUserId(), UserType.employee, r1 -> {
                    if (r1.succeeded()) {
                        ExceptionUtil.fail(message, r1.cause());
                        return;
                    }
                    message.reply(null);
                    App.bus.publish(Events.NEW_AREA_COORDINATOR_CREATED, user);
                });
            }
        });

    }

    private void validate(JsonObject areaCoordinator, AsyncResultHandler<JsonObject> asyncResultHandler) {
        ExceptionUtil.sallowCall(() -> {
            String area_id;
            if (!areaCoordinator.containsKey(AC.area)) {
                throw new ValidationException("Area id required.");
            }
            Object value = areaCoordinator.getValue(AC.area);
            if (value instanceof JsonObject) {
                JsonObject vl = ((JsonObject) value);
                if (!vl.containsKey(AC.area)) {
                    throw new ValidationException("Area id required.");
                }
                area_id = vl.getString(Model.id);
            } else {
                area_id = areaCoordinator.getString(AC.area);
                if (area_id.trim().isEmpty()) {
                    throw new ValidationException("Area id required.");
                }
            }
            areaCoordinator.put(AC.area, area_id);
            return areaCoordinator;
        }, asyncResultHandler);
    }

    private String newUserId() {
        return String.format(id_prefix + "%04d", atomicLong.incrementAndGet());
    }
}

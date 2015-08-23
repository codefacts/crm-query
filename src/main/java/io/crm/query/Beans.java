package io.crm.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.text.SimpleDateFormat;

/**
 * Created by someone on 16-Aug-2015.
 */
@Configuration
public class Beans {

    @Bean
    public ObjectMapper objectMapper() {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(App.defaultDateFormat());
        return objectMapper;
    }
}

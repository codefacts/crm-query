package io.crm.query.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Area {
    public static final String name = "name";
    public static final String region = "region";
    public static final String active = "active";
    public static final String houseCount = "houseCount";

    Area() {
    }
}
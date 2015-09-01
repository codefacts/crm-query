package io.crm.query.model;

import static io.crm.query.model.Br.distributionHouse;

public class Query {
    public static final String contactCount = "contactCount";
    public static final String ptrCount = "ptrCount";
    public static final String swpCount = "swpCount";
    public static final String id = "_id";
    public static final String regionId = Area.region + "." + id;
    public static final String areaId = House.area + "." + id;
    public static final String houseId = concat(distributionHouse, id);
    public static final String userTypeId = concat(User.userType, id);
    public static final String brId = concat(Contact.br, id);
    public static final String regionCount = "regionCount";
    public static final String areaCount = "areaCount";
    public static final String houseCount = "houseCount";
    public static final String brCount = "brCount";
    public static final String locationCount = "locationCount";
    public static final String regions = "regions";
    public static final String brs = "brs";
    public static final String __self = "__self";
    public static final String createDate = "createDate";
    public static final String modifyDate = "modifyDate";
    public static final String createdBy = "createdBy";
    public static final String modifiedBy = "modifiedBy";
    public static final String params = "params";
    public static final String count = "count";
    public static final String locations = "locations";
    public static final String areaCoordinators = "areaCoordinators";
    public static final String brSupervisors = "brSupervisors";
    public static final String acCount = "acCount";
    public static final String supCount = "supCount";

    public static final String concat(String... strings) {
        return String.join(".", strings);
    }
}

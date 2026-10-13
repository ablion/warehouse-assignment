package com.fulfilment.application.monolith.fulfilment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FulfilmentAssociationEndpointTest {

  @Test
  void associatesProductAndEnforcesTwoWarehousesPerProductPerStore() {
    Long firstId = given()
        .contentType("application/json")
        .body("{\"storeId\":1,\"productId\":1,\"warehouseBusinessUnitCode\":\"MWH.001\"}")
        .when().post("/fulfilment-associations")
        .then().statusCode(201)
        .body(containsString("MWH.001"), containsString("TONSTAD"))
        .extract().path("id");

    Long secondId = given()
        .contentType("application/json")
        .body("{\"storeId\":1,\"productId\":1,\"warehouseBusinessUnitCode\":\"MWH.012\"}")
        .when().post("/fulfilment-associations")
        .then().statusCode(201)
        .extract().path("id");

    given()
        .contentType("application/json")
        .body("{\"storeId\":1,\"productId\":1,\"warehouseBusinessUnitCode\":\"MWH.023\"}")
        .when().post("/fulfilment-associations")
        .then().statusCode(422)
        .body("error", containsString("at most 2 warehouses per store"));

    given().when().get("/fulfilment-associations/store/1")
        .then().statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), not(containsString("MWH.023")));

    given().when().delete("/fulfilment-associations/" + firstId).then().statusCode(204);
    given().when().delete("/fulfilment-associations/" + secondId).then().statusCode(204);
  }

  @Test
  void rejectsUnknownStore() {
    given().when().get("/fulfilment-associations/store/999999")
        .then().statusCode(404);
  }
}

package com.vehicle.inspection;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class InspectionApiTest {
    @Test
    void putReplacesExistingItemsWithoutUniqueKeyCollision() {
        long inspectionId = createDraft(List.of(item("EXT_FRONT", "GOOD", null)));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "version", 1,
                        "inspector_name", "Inspector test",
                        "has_key", true,
                        "items", List.of(
                                item("EXT_FRONT", "MINOR_DAMAGE", "Lecet"),
                                item("EXT_REAR", "GOOD", null))))
                .when()
                .put("/api/v1/inspections/{id}", inspectionId)
                .then()
                .statusCode(200)
                .body("data.version", equalTo(2))
                .body("data.items", hasSize(2))
                .body("data.items[0].condition", equalTo("MINOR_DAMAGE"));
    }

    @Test
    void rejectsInspectableEngineConditionWhenVehicleKeyIsMissing() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "vehicle_id", 1,
                        "inspector_name", "Inspector test",
                        "has_key", false,
                        "items", List.of(item("ENG_STARTUP", "GOOD", null))))
                .when()
                .post("/api/v1/inspections")
                .then()
                .statusCode(400)
                .body("errors[0].field", equalTo("items[0].condition"));
    }

    @Test
    void rejectsIllegalDraftToApprovedTransition() {
        long inspectionId = createDraft(List.of());

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("to", "APPROVED", "actor", "Inspector test"))
                .when()
                .post("/api/v1/inspections/{id}/transition", inspectionId)
                .then()
                .statusCode(409);
    }

    @Test
    void syncIsIdempotentAndRejectsStaleBaseVersion() {
        String clientId = UUID.randomUUID().toString();
        Map<String, Object> first = new HashMap<>();
        first.put("client_id", clientId);
        first.put("base_version", null);
        first.put("vehicle_id", 1);
        first.put("inspector_name", "Inspector test");
        first.put("has_key", true);
        first.put("status", "DRAFT");
        first.put("items", List.of());
        Map<String, Object> request = Map.of(
                "device_id", "backend-test",
                "inspections", List.of(first));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/inspections/sync")
                .then()
                .statusCode(200)
                .body("data.results[0].result", equalTo("CREATED"));

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/v1/inspections/sync")
                .then()
                .statusCode(200)
                .body("data.results[0].result", equalTo("DUPLICATE"));

        Map<String, Object> stale = Map.of(
                "client_id", clientId,
                "base_version", 99,
                "vehicle_id", 1,
                "inspector_name", "Different inspector",
                "has_key", true,
                "status", "DRAFT",
                "items", List.of());

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("device_id", "backend-test", "inspections", List.of(stale)))
                .when()
                .post("/api/v1/inspections/sync")
                .then()
                .statusCode(200)
                .body("data.results[0].result", equalTo("CONFLICT"));
    }

    private long createDraft(List<Map<String, Object>> items) {
        Object id = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "vehicle_id", 1,
                        "inspector_name", "Inspector test",
                        "has_key", true,
                        "items", items))
                .when()
                .post("/api/v1/inspections")
                .then()
                .statusCode(201)
                .extract()
                .path("data.id");
        return ((Number) id).longValue();
    }

    private Map<String, Object> item(String code, String condition, String note) {
        if (note == null) {
            return Map.of(
                    "point_code", code,
                    "condition", condition,
                    "photo_refs", List.of());
        }
        return Map.of(
                "point_code", code,
                "condition", condition,
                "note", note,
                "photo_refs", List.of());
    }
}

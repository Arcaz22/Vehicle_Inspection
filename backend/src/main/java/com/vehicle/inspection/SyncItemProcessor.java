package com.vehicle.inspection;

import com.vehicle.audit.AuditService;
import com.vehicle.common.ApiError;
import com.vehicle.common.ApiException;
import com.vehicle.vehicle.Vehicle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class SyncItemProcessor {
    @Inject
    InspectionRepository inspections;

    @Inject
    InspectionService inspectionService;

    @Inject
    AuditService audit;

    @Inject
    EntityManager entityManager;

    @Transactional
    public SyncResult process(String deviceId, SyncInspectionInput input) {
        UUID clientId = input == null ? null : input.clientId();
        if (input == null) {
            return reject(deviceId, null, null, List.of(new ApiError("inspection", "must not be null")));
        }

        Inspection existing = clientId == null ? null : inspections.findWithItemsByClientId(clientId).orElse(null);
        if (existing != null && matchesPayload(existing, input)) {
            return result(clientId, "DUPLICATE", existing, List.of());
        }
        if (existing != null && (existing.status == InspectionStatus.APPROVED
                || existing.status == InspectionStatus.SUBMITTED
                || !Objects.equals(input.baseVersion(), existing.version))) {
            Map<String, Object> meta = metadata(deviceId, clientId);
            meta.put("reason", "server state or base_version does not allow this update");
            audit.append(existing.id, "SYNC_CONFLICT", existing.status.name(), existing.status.name(), deviceId, meta);
            return result(clientId, "CONFLICT", existing, List.of());
        }

        ValidationResult validation = validate(deviceId, input);
        if (!validation.errors().isEmpty()) {
            return reject(deviceId, clientId, existing, validation.errors());
        }

        Vehicle vehicle = validation.vehicle();
        InspectionStatus targetStatus = validation.status();
        List<InspectionItem> items = validation.items();
        if (existing == null) {
            Inspection created = new Inspection();
            created.vehicle = vehicle;
            created.clientId = clientId;
            created.inspectorName = input.inspectorName().trim();
            created.hasKey = input.hasKey();
            created.status = targetStatus;
            created.version = 1L;
            created.replaceItems(items);
            inspections.persist(created);
            entityManager.flush();
            audit.append(created.id, "SYNC_CREATED", null, targetStatus.name(), deviceId,
                    metadata(deviceId, clientId));
            return result(clientId, "CREATED", created, List.of());
        }

        InspectionStatus fromStatus = existing.status;
        existing.vehicle = vehicle;
        existing.inspectorName = input.inspectorName().trim();
        existing.hasKey = input.hasKey();
        existing.status = targetStatus;
        existing.replaceItems(items);
        existing.updatedAt = Instant.now();
        entityManager.flush();
        audit.append(existing.id, "SYNC_UPDATED", fromStatus.name(), targetStatus.name(), deviceId,
                metadata(deviceId, clientId));
        return result(clientId, "UPDATED", existing, List.of());
    }

    private ValidationResult validate(String deviceId, SyncInspectionInput input) {
        List<ApiError> errors = new ArrayList<>();
        if (input.clientId() == null) {
            errors.add(new ApiError("client_id", "is required"));
        }
        if (input.baseVersion() != null && input.baseVersion() < 1) {
            errors.add(new ApiError("base_version", "must be a positive integer or null"));
        }
        if (input.vehicleId() == null || input.vehicleId() < 1) {
            errors.add(new ApiError("vehicle_id", "is required and must be a positive integer"));
        }
        if (input.inspectorName() == null || input.inspectorName().isBlank()) {
            errors.add(new ApiError("inspector_name", "is required"));
        }
        if (input.hasKey() == null) {
            errors.add(new ApiError("has_key", "is required"));
        }

        InspectionStatus status = parseSyncStatus(input.status());
        if (status == null) {
            errors.add(new ApiError("status", "must be DRAFT or SUBMITTED"));
        }
        if (!errors.isEmpty()) {
            return new ValidationResult(null, status, List.of(), errors);
        }

        Vehicle vehicle = inspectionService.findVehicle(input.vehicleId());
        if (vehicle == null) {
            errors.add(new ApiError("vehicle_id", "vehicle does not exist"));
            return new ValidationResult(null, status, List.of(), errors);
        }

        List<InspectionItem> items;
        try {
            items = inspectionService.validateItemsForSync(input.items(), input.hasKey());
        } catch (ApiException exception) {
            return new ValidationResult(vehicle, status, List.of(), exception.errors());
        }
        if (status == InspectionStatus.SUBMITTED) {
            errors.addAll(inspectionService.missingSubmissionPoints(input.items()));
        }
        return new ValidationResult(vehicle, status, items, errors);
    }

    private InspectionStatus parseSyncStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            InspectionStatus status = InspectionStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
            return status == InspectionStatus.DRAFT || status == InspectionStatus.SUBMITTED ? status : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean matchesPayload(Inspection inspection, SyncInspectionInput input) {
        InspectionStatus status = parseSyncStatus(input.status());
        if (status == null || input.vehicleId() == null || input.hasKey() == null
                || !Objects.equals(inspection.vehicle.id, input.vehicleId())
                || !Objects.equals(inspection.inspectorName, input.inspectorName())
                || inspection.hasKey != input.hasKey()
                || inspection.status != status) {
            return false;
        }
        List<InspectionItemInput> inputs = input.items() == null ? List.of() : input.items();
        if (inputs.size() != inspection.items.size()) {
            return false;
        }
        Map<String, InspectionItem> existingItems = new HashMap<>();
        inspection.items.forEach(item -> existingItems.put(item.pointCode, item));
        for (InspectionItemInput itemInput : inputs) {
            if (itemInput == null || itemInput.pointCode() == null || itemInput.condition() == null) {
                return false;
            }
            InspectionItem item = existingItems.get(itemInput.pointCode());
            if (item == null || !item.condition.name().equals(itemInput.condition())
                    || !Objects.equals(item.note, itemInput.note())) {
                return false;
            }
            List<String> photos = itemInput.photoRefs() == null ? List.of() : itemInput.photoRefs();
            if (!Objects.equals(item.photoRefs == null ? List.of() : item.photoRefs, photos)) {
                return false;
            }
        }
        return true;
    }

    private SyncResult reject(String deviceId, UUID clientId, Inspection inspection, List<ApiError> errors) {
        Map<String, Object> meta = metadata(deviceId, clientId);
        meta.put("errors", errors.stream()
                .map(error -> Map.of("field", error.field(), "reason", error.reason()))
                .toList());
        audit.append(inspection == null ? null : inspection.id, "SYNC_REJECTED",
                inspection == null ? null : inspection.status.name(),
                inspection == null ? null : inspection.status.name(), deviceId, meta);
        return new SyncResult(clientId, "REJECTED", inspection == null ? null : inspection.id,
                inspection == null ? null : inspection.version,
                inspection == null ? null : inspection.status, errors);
    }

    private Map<String, Object> metadata(String deviceId, UUID clientId) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (deviceId != null) {
            meta.put("device_id", deviceId);
        }
        if (clientId != null) {
            meta.put("client_id", clientId.toString());
        }
        return meta;
    }

    private SyncResult result(UUID clientId, String outcome, Inspection inspection, List<ApiError> errors) {
        return new SyncResult(clientId, outcome, inspection.id, inspection.version,
                "CONFLICT".equals(outcome) ? inspection.status : null, errors);
    }

    private record ValidationResult(Vehicle vehicle, InspectionStatus status,
                                    List<InspectionItem> items, List<ApiError> errors) {}
}

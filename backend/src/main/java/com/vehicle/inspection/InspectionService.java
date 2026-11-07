package com.vehicle.inspection;

import com.vehicle.common.ApiError;
import com.vehicle.common.ApiException;
import com.vehicle.audit.AuditService;
import com.vehicle.vehicle.Vehicle;
import com.vehicle.vehicle.VehicleRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.Instant;
import java.util.Map;
import java.util.Locale;

@ApplicationScoped
public class InspectionService {
    @Inject
    InspectionRepository inspections;

    @Inject
    VehicleRepository vehicles;

    @Inject
    ChecklistCatalog checklist;

    @Inject
    EntityManager entityManager;

    @Inject
    AuditService audit;

    @Transactional
    public InspectionView create(CreateInspectionInput input) {
        if (input == null) {
            throw ApiException.validation(List.of(new ApiError("body", "request body is required")));
        }
        validateHeader(input.vehicleId(), input.inspectorName(), input.hasKey());
        Vehicle vehicle = vehicles.findById(input.vehicleId());
        if (vehicle == null) {
            throw ApiException.notFound("Vehicle not found");
        }
        List<InspectionItem> items = validateAndMapItems(input.items(), input.hasKey());

        Inspection inspection = new Inspection();
        inspection.vehicle = vehicle;
        inspection.inspectorName = input.inspectorName().trim();
        inspection.hasKey = input.hasKey();
        inspection.status = InspectionStatus.DRAFT;
        inspection.version = 1L;
        inspection.replaceItems(items);
        inspections.persist(inspection);
        entityManager.flush();
        return toView(inspection);
    }

    @Transactional
    public InspectionView update(long id, UpdateInspectionInput input) {
        if (input == null) {
            throw ApiException.validation(List.of(new ApiError("body", "request body is required")));
        }
        if (input.version() == null || input.version() < 1) {
            throw ApiException.validation(List.of(new ApiError("version", "must be a positive integer")));
        }
        validateInspectorFields(input.inspectorName(), input.hasKey());
        if (input.items() == null) {
            throw ApiException.validation(List.of(new ApiError("items", "is required for an update")));
        }

        Inspection inspection = inspections.findWithItems(id)
                .orElseThrow(() -> ApiException.notFound("Inspection not found"));
        if (inspection.status != InspectionStatus.DRAFT && inspection.status != InspectionStatus.RECHECK) {
            throw ApiException.conflict("Only DRAFT or RECHECK inspections can be updated");
        }
        if (!inspection.version.equals(input.version())) {
            throw ApiException.conflict("Inspection version is stale");
        }

        List<InspectionItem> items = validateAndMapItems(input.items(), input.hasKey());
        inspection.inspectorName = input.inspectorName().trim();
        inspection.hasKey = input.hasKey();
        inspection.replaceItems(items);
        inspection.updatedAt = Instant.now();
        entityManager.flush();
        return toView(inspection);
    }

    @Transactional
    public InspectionView get(long id) {
        Inspection inspection = inspections.findWithItems(id)
                .orElseThrow(() -> ApiException.notFound("Inspection not found"));
        return toView(inspection);
    }

    @Transactional
    public InspectionView transition(long id, TransitionInput input) {
        if (input == null) {
            throw ApiException.validation(List.of(new ApiError("body", "request body is required")));
        }
        if (input.actor() == null || input.actor().isBlank()) {
            throw ApiException.validation(List.of(new ApiError("actor", "is required")));
        }
        InspectionStatus target;
        try {
            target = InspectionStatus.valueOf(input.to() == null ? "" : input.to().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw ApiException.validation(List.of(new ApiError("to", "is not a valid inspection status")));
        }

        Inspection inspection = inspections.findWithItems(id)
                .orElseThrow(() -> ApiException.notFound("Inspection not found"));
        InspectionStatus from = inspection.status;
        boolean allowed = (from == InspectionStatus.DRAFT && target == InspectionStatus.SUBMITTED)
                || (from == InspectionStatus.SUBMITTED && (target == InspectionStatus.APPROVED || target == InspectionStatus.RECHECK))
                || (from == InspectionStatus.RECHECK && target == InspectionStatus.SUBMITTED);
        if (!allowed) {
            throw ApiException.conflict("Illegal transition from " + from + " to " + target);
        }
        if (target == InspectionStatus.SUBMITTED) {
            List<ApiError> missing = missingSubmissionPoints(inspection);
            if (!missing.isEmpty()) {
                throw ApiException.validation(missing);
            }
        }

        inspection.status = target;
        inspection.updatedAt = Instant.now();
        entityManager.flush();
        audit.append(inspection.id, "STATUS_TRANSITION", from.name(), target.name(), input.actor().trim(),
                Map.of("note", input.note() == null ? "" : input.note()));
        return toView(inspection);
    }

    @Transactional
    public boolean exists(long id) {
        return inspections.exists(id);
    }

    @Transactional
    public List<InspectionSummary> list(InspectionStatus status, Long vehicleId, int page, int limit) {
        if (page < 1) {
            throw ApiException.validation(List.of(new ApiError("page", "must be at least 1")));
        }
        if (limit < 1 || limit > 100) {
            throw ApiException.validation(List.of(new ApiError("limit", "must be between 1 and 100")));
        }
        if (vehicleId != null && vehicleId < 1) {
            throw ApiException.validation(List.of(new ApiError("vehicle_id", "must be a positive integer")));
        }
        return inspections.search(status, vehicleId, page, limit).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public long count(InspectionStatus status, Long vehicleId) {
        return inspections.countMatching(status, vehicleId);
    }

    private void validateHeader(Long vehicleId, String inspectorName, Boolean hasKey) {
        List<ApiError> errors = new ArrayList<>();
        if (vehicleId == null || vehicleId < 1) {
            errors.add(new ApiError("vehicle_id", "is required and must be a positive integer"));
        }
        errors.addAll(inspectorFieldErrors(inspectorName, hasKey));
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
    }

    private void validateInspectorFields(String inspectorName, Boolean hasKey) {
        List<ApiError> errors = inspectorFieldErrors(inspectorName, hasKey);
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
    }

    private List<ApiError> inspectorFieldErrors(String inspectorName, Boolean hasKey) {
        List<ApiError> errors = new ArrayList<>();
        if (inspectorName == null || inspectorName.isBlank()) {
            errors.add(new ApiError("inspector_name", "is required"));
        }
        if (hasKey == null) {
            errors.add(new ApiError("has_key", "is required"));
        }
        return errors;
    }

    public List<InspectionItem> validateItemsForSync(List<InspectionItemInput> inputs, boolean hasKey) {
        return validateAndMapItems(inputs, hasKey);
    }

    public List<ApiError> missingSubmissionPoints(List<InspectionItemInput> inputs) {
        Set<String> present = new HashSet<>();
        if (inputs != null) {
            inputs.stream().filter(item -> item != null && item.pointCode() != null)
                    .map(item -> item.pointCode().trim()).forEach(present::add);
        }
        return checklist.pointCodes().stream()
                .filter(pointCode -> !present.contains(pointCode))
                .map(pointCode -> new ApiError("items." + pointCode, "is required before submission"))
                .toList();
    }

    public List<ApiError> missingSubmissionPoints(Inspection inspection) {
        Set<String> present = new HashSet<>();
        inspection.items.forEach(item -> present.add(item.pointCode));
        return checklist.pointCodes().stream()
                .filter(pointCode -> !present.contains(pointCode))
                .map(pointCode -> new ApiError("items." + pointCode, "is required before submission"))
                .toList();
    }

    public Vehicle findVehicle(long vehicleId) {
        return vehicles.findById(vehicleId);
    }

    private List<InspectionItem> validateAndMapItems(List<InspectionItemInput> inputs, boolean hasKey) {
        if (inputs == null) {
            return List.of();
        }
        List<ApiError> errors = new ArrayList<>();
        List<InspectionItem> mapped = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int index = 0; index < inputs.size(); index++) {
            InspectionItemInput input = inputs.get(index);
            String prefix = "items[" + index + "]";
            if (input == null) {
                errors.add(new ApiError(prefix, "must not be null"));
                continue;
            }
            String pointCode = input.pointCode() == null ? "" : input.pointCode().trim();
            if (!checklist.contains(pointCode)) {
                errors.add(new ApiError(prefix + ".point_code", "is not in the CAR checklist"));
                continue;
            }
            if (!seen.add(pointCode)) {
                errors.add(new ApiError(prefix + ".point_code", "must not be duplicated"));
                continue;
            }
            InspectionCondition condition;
            try {
                condition = InspectionCondition.valueOf(input.condition() == null ? "" : input.condition());
            } catch (IllegalArgumentException exception) {
                errors.add(new ApiError(prefix + ".condition", "is not a valid condition"));
                continue;
            }
            List<String> photoRefs = input.photoRefs() == null ? List.of() : new ArrayList<>(input.photoRefs());
            if (photoRefs.stream().anyMatch(photo -> photo == null || photo.isBlank())) {
                errors.add(new ApiError(prefix + ".photo_refs", "must contain non-empty file references"));
            }
            if (condition == InspectionCondition.NOT_INSPECTABLE &&
                    (input.note() == null || input.note().isBlank())) {
                errors.add(new ApiError(prefix + ".note", "is required for NOT_INSPECTABLE"));
            }
            if (condition == InspectionCondition.MAJOR_DAMAGE && photoRefs.isEmpty()) {
                errors.add(new ApiError(prefix + ".photo_refs", "at least one photo is required for MAJOR_DAMAGE"));
            }
            if (!hasKey && checklist.isEnginePoint(pointCode) && condition != InspectionCondition.NOT_INSPECTABLE) {
                errors.add(new ApiError(prefix + ".condition", "ENGINE items must be NOT_INSPECTABLE when has_key is false"));
            }

            InspectionItem item = new InspectionItem();
            item.pointCode = pointCode;
            item.condition = condition;
            item.note = input.note();
            item.photoRefs = new ArrayList<>(photoRefs);
            mapped.add(item);
        }
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
        return mapped;
    }

    private InspectionView toView(Inspection inspection) {
        List<InspectionItemView> items = inspection.items.stream()
                .map(item -> new InspectionItemView(item.pointCode, item.condition, item.note,
                        item.photoRefs == null ? List.of() : List.copyOf(item.photoRefs)))
                .toList();
        return new InspectionView(inspection.id, inspection.vehicle.id, inspection.clientId,
                inspection.inspectorName, inspection.hasKey, inspection.status, inspection.version,
                inspection.createdAt, inspection.updatedAt, items);
    }

    private InspectionSummary toSummary(Inspection inspection) {
        return new InspectionSummary(inspection.id, inspection.vehicle.id, inspection.clientId,
                inspection.inspectorName, inspection.hasKey, inspection.status, inspection.version,
                inspection.createdAt, inspection.updatedAt);
    }
}

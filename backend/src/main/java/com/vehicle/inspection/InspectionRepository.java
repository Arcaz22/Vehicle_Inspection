package com.vehicle.inspection;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class InspectionRepository implements PanacheRepository<Inspection> {
    public Optional<Inspection> findWithItems(long id) {
        return find("select distinct i from Inspection i left join fetch i.items where i.id = ?1", id)
                .firstResultOptional();
    }

    public boolean exists(long id) {
        return count("id", id) > 0;
    }

    public Optional<Inspection> findWithItemsByClientId(java.util.UUID clientId) {
        return find("select distinct i from Inspection i left join fetch i.items where i.clientId = ?1", clientId)
                .firstResultOptional();
    }

    public List<Inspection> search(InspectionStatus status, Long vehicleId, int page, int limit) {
        Query query = query(status, vehicleId);
        return find(query.jpql(), query.parameters())
                .page(Page.of(page - 1, limit))
                .list();
    }

    public long countMatching(InspectionStatus status, Long vehicleId) {
        Query query = query(status, vehicleId);
        return count(query.jpql(), query.parameters());
    }

    private Query query(InspectionStatus status, Long vehicleId) {
        StringBuilder jpql = new StringBuilder("select i from Inspection i where 1 = 1");
        Parameters parameters = new Parameters();
        if (status != null) {
            jpql.append(" and i.status = :status");
            parameters.and("status", status);
        }
        if (vehicleId != null) {
            jpql.append(" and i.vehicle.id = :vehicleId");
            parameters.and("vehicleId", vehicleId);
        }
        jpql.append(" order by i.updatedAt desc, i.id desc");
        return new Query(jpql.toString(), parameters);
    }

    private record Query(String jpql, Parameters parameters) {}
}

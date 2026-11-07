package com.vehicle.audit;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AuditService {
    @Inject
    MongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String databaseName;

    public void append(Long inspectionId, String action, String fromStatus, String toStatus,
                       String actor, Map<String, Object> meta) {
        Document event = new Document("inspection_id", inspectionId)
                .append("action", action)
                .append("from_status", fromStatus)
                .append("to_status", toStatus)
                .append("actor", actor)
                .append("at", Date.from(Instant.now()))
                .append("meta", new Document(meta == null ? Map.of() : meta));
        collection().insertOne(event);
    }

    public List<AuditEventView> findByInspectionId(long inspectionId) {
        return collection().find(Filters.eq("inspection_id", inspectionId))
                .sort(Sorts.ascending("at", "_id"))
                .map(this::toView)
                .into(new ArrayList<>());
    }

    private MongoCollection<Document> collection() {
        return mongoClient.getDatabase(databaseName).getCollection("inspection_audit");
    }

    private AuditEventView toView(Document document) {
        Object id = document.get("inspection_id");
        Number number = id instanceof Number value ? value : null;
        Date date = document.getDate("at");
        Document metadata = document.get("meta", Document.class);
        Map<String, Object> meta = new LinkedHashMap<>();
        if (metadata != null) {
            meta.putAll(metadata);
        }
        return new AuditEventView(number == null ? null : number.longValue(),
                document.getString("action"), document.getString("from_status"),
                document.getString("to_status"), document.getString("actor"),
                date == null ? null : date.toInstant(), meta);
    }
}

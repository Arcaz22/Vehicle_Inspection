package com.vehicle.inspection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inspection_items", uniqueConstraints =
        @UniqueConstraint(name = "uq_inspection_item_point", columnNames = {"inspection_id", "point_code"}))
public class InspectionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    public Inspection inspection;

    @Column(name = "point_code", nullable = false)
    public String pointCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public InspectionCondition condition;

    @Column(columnDefinition = "text")
    public String note;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "photo_refs", nullable = false, columnDefinition = "jsonb")
    public List<String> photoRefs = new ArrayList<>();
}

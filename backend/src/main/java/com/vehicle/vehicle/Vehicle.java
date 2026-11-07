package com.vehicle.vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    public Long id;

    @Column(name = "plate_number", nullable = false, unique = true)
    public String plateNumber;

    @Column(nullable = false)
    public String type;

    @Column(nullable = false)
    public String brand;

    @Column(nullable = false)
    public String model;

    @Column(nullable = false)
    public Integer year;
}

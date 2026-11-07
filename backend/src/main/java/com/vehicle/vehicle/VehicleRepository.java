package com.vehicle.vehicle;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class VehicleRepository implements PanacheRepository<Vehicle> {
    public List<Vehicle> listOrdered() {
        return find("order by id").list();
    }
}

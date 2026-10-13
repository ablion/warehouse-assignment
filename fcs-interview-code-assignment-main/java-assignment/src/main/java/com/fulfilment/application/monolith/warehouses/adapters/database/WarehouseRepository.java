package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {
  @Override
  public List<Warehouse> getAll() {
    return list("archivedAt is null").stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  @Transactional
  public void create(Warehouse warehouse) {
    DbWarehouse entity = new DbWarehouse();
    copy(warehouse, entity);
    if (entity.createdAt == null) entity.createdAt = LocalDateTime.now();
    persist(entity);
    warehouse.createdAt = entity.createdAt;
    warehouse.archivedAt = entity.archivedAt;
  }

  @Override
  @Transactional
  public void update(Warehouse warehouse) {
    DbWarehouse entity = find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
        .list().stream().max(Comparator.comparing(e -> e.createdAt, Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null);
    if (entity == null) {
      entity = find("businessUnitCode = ?1", warehouse.businessUnitCode).list().stream()
          .max(Comparator.comparing(e -> e.createdAt, Comparator.nullsFirst(Comparator.naturalOrder())))
          .orElseThrow(() -> new jakarta.ws.rs.WebApplicationException("Warehouse not found: " + warehouse.businessUnitCode, 404));
    }
    copy(warehouse, entity);
  }

  @Override
  @Transactional
  public void remove(Warehouse warehouse) {
    DbWarehouse entity = find("businessUnitCode = ?1 and archivedAt is null", warehouse.businessUnitCode)
        .firstResult();
    if (entity != null) delete(entity);
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    if (buCode == null || buCode.isBlank()) return null;
    DbWarehouse entity = find("businessUnitCode = ?1 and archivedAt is null", buCode).list().stream()
        .max(Comparator.comparing(e -> e.createdAt, Comparator.nullsFirst(Comparator.naturalOrder())))
        .orElse(null);
    if (entity == null) {
      try { entity = findById(Long.valueOf(buCode)); } catch (NumberFormatException ignored) { }
    }
    return entity == null ? null : entity.toWarehouse();
  }

  public DbWarehouse findActiveEntity(String code) {
    return find("businessUnitCode = ?1 and archivedAt is null", code).firstResult();
  }

  private void copy(Warehouse source, DbWarehouse target) {
    target.businessUnitCode = source.businessUnitCode;
    target.location = source.location;
    target.capacity = source.capacity;
    target.stock = source.stock;
    target.createdAt = source.createdAt;
    target.archivedAt = source.archivedAt;
  }
}

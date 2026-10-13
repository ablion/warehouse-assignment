package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.location.LocationGateway;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.warehouse.api.WarehouseResource;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.WebApplicationException;
import java.time.LocalDateTime;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {
  @Inject WarehouseRepository warehouseRepository;
  @Inject LocationGateway locationGateway;

  @Override
  public List<com.warehouse.api.beans.Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @Transactional
  public com.warehouse.api.beans.Warehouse createANewWarehouseUnit(@NotNull com.warehouse.api.beans.Warehouse data) {
    Warehouse warehouse = toDomain(data);
    validateRequired(warehouse);
    if (warehouseRepository.findByBusinessUnitCode(warehouse.businessUnitCode) != null)
      throw new WebApplicationException("Business unit code already exists: " + warehouse.businessUnitCode, 409);
    validateLocationAndCapacity(warehouse, null);
    warehouse.createdAt = LocalDateTime.now();
    warehouseRepository.create(warehouse);
    return toWarehouseResponse(warehouse);
  }

  @Override
  public com.warehouse.api.beans.Warehouse getAWarehouseUnitByID(String id) {
    Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) throw new WebApplicationException("Warehouse not found: " + id, 404);
    return toWarehouseResponse(warehouse);
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    Warehouse warehouse = warehouseRepository.findByBusinessUnitCode(id);
    if (warehouse == null) throw new WebApplicationException("Warehouse not found: " + id, 404);
    if (warehouse.archivedAt != null) throw new WebApplicationException("Warehouse is already archived", 409);
    warehouse.archivedAt = LocalDateTime.now();
    warehouseRepository.update(warehouse);
  }

  @Override
  @Transactional
  public com.warehouse.api.beans.Warehouse replaceTheCurrentActiveWarehouse(
      String businessUnitCode, @NotNull com.warehouse.api.beans.Warehouse data) {
    Warehouse old = warehouseRepository.findByBusinessUnitCode(businessUnitCode);
    if (old == null || old.archivedAt != null)
      throw new WebApplicationException("Active warehouse not found: " + businessUnitCode, 404);

    Warehouse replacement = toDomain(data);
    if (replacement.businessUnitCode == null || replacement.businessUnitCode.isBlank())
      replacement.businessUnitCode = businessUnitCode;
    if (!businessUnitCode.equals(replacement.businessUnitCode))
      throw new WebApplicationException("Replacement must reuse the existing business unit code", 400);
    validateRequired(replacement);
    if (!java.util.Objects.equals(old.stock, replacement.stock))
      throw new WebApplicationException("Replacement stock must match the previous warehouse stock", 400);
    if (replacement.capacity < old.stock)
      throw new WebApplicationException("Replacement capacity cannot accommodate existing stock", 400);
    validateLocationAndCapacity(replacement, old);
    old.archivedAt = LocalDateTime.now();
    warehouseRepository.update(old);
    replacement.createdAt = LocalDateTime.now();
    replacement.archivedAt = null;
    warehouseRepository.create(replacement);
    return toWarehouseResponse(replacement);
  }

  private void validateRequired(Warehouse w) {
    if (w.businessUnitCode == null || w.businessUnitCode.isBlank()
        || w.location == null || w.location.isBlank() || w.capacity == null || w.stock == null)
      throw new WebApplicationException("businessUnitCode, location, capacity and stock are required", 400);
    if (w.capacity < 0 || w.stock < 0 || w.stock > w.capacity)
      throw new WebApplicationException("Capacity and stock must be non-negative and stock cannot exceed capacity", 400);
  }

  private void validateLocationAndCapacity(Warehouse w, Warehouse replacing) {
    Location location = locationGateway.resolveByIdentifier(w.location);
    if (location == null) throw new WebApplicationException("Invalid location: " + w.location, 400);
    List<Warehouse> active = warehouseRepository.getAll().stream()
        .filter(x -> x.archivedAt == null && x.location.equals(w.location))
        .filter(x -> replacing == null || !x.businessUnitCode.equals(replacing.businessUnitCode))
        .toList();
    if (active.size() >= location.maxNumberOfWarehouses)
      throw new WebApplicationException("Maximum number of warehouses reached for location " + w.location, 400);
    int totalCapacity = active.stream().mapToInt(x -> x.capacity == null ? 0 : x.capacity).sum();
    if (w.capacity > location.maxCapacity || totalCapacity + w.capacity > location.maxCapacity)
      throw new WebApplicationException("Location capacity limit exceeded for " + w.location, 400);
  }

  private Warehouse toDomain(com.warehouse.api.beans.Warehouse data) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = data.getBusinessUnitCode();
    w.location = data.getLocation();
    w.capacity = data.getCapacity();
    w.stock = data.getStock();
    return w;
  }

  private com.warehouse.api.beans.Warehouse toWarehouseResponse(Warehouse warehouse) {
    var response = new com.warehouse.api.beans.Warehouse();
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);
    return response;
  }
}

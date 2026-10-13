package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.inject.Inject;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

/** Manages warehouse-to-product fulfilment for individual stores. */
@Path("fulfilment-associations")
@ApplicationScoped
@Consumes("application/json")
@Produces("application/json")
public class FulfilmentAssociationResource {
  @Inject WarehouseRepository warehouseRepository;
  @Inject ProductRepository productRepository;

  private static final long MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  private static final long MAX_WAREHOUSES_PER_STORE = 3;
  private static final long MAX_PRODUCTS_PER_WAREHOUSE = 5;

  @GET
  public List<FulfilmentAssociationView> list() {
    return FulfilmentAssociation.listAll(Sort.by("id"))
        .stream().map(FulfilmentAssociationView::from).toList();
  }

  @GET
  @Path("store/{storeId}")
  public List<FulfilmentAssociationView> listForStore(@PathParam("storeId") Long storeId) {
    if (Store.findById(storeId) == null) {
      throw new WebApplicationException("Store with id " + storeId + " does not exist.", 404);
    }
    return FulfilmentAssociation.list("store.id", Sort.by("id"), storeId)
        .stream().map(FulfilmentAssociationView::from).toList();
  }

  @POST
  @Transactional
  public Response associate(FulfilmentAssociationRequest request) {
    if (request == null || request.storeId == null || request.productId == null
        || request.warehouseBusinessUnitCode == null || request.warehouseBusinessUnitCode.isBlank()) {
      throw new WebApplicationException(
          "storeId, productId and warehouseBusinessUnitCode are required.", 400);
    }

    Store store = Store.findById(request.storeId);
    if (store == null) throw new WebApplicationException("Store with id " + request.storeId + " does not exist.", 404);
    Product product = productRepository.findById(request.productId);
    if (product == null) throw new WebApplicationException("Product with id " + request.productId + " does not exist.", 404);
    DbWarehouse warehouse = warehouseRepository.find(
        "businessUnitCode = ?1 and archivedAt is null", request.warehouseBusinessUnitCode).firstResult();
    if (warehouse == null) {
      throw new WebApplicationException("Active warehouse not found: " + request.warehouseBusinessUnitCode, 404);
    }

    if (FulfilmentAssociation.count("store = ?1 and product = ?2 and warehouse = ?3", store, product, warehouse) > 0) {
      throw new WebApplicationException("This product is already associated with this warehouse for the store.", 409);
    }

    long productWarehouseCount = FulfilmentAssociation.count(
        "store = ?1 and product = ?2", store, product);
    if (productWarehouseCount >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new WebApplicationException("A product can be fulfilled by at most 2 warehouses per store.", 422);
    }

    long storeWarehouseCount = FulfilmentAssociation.find("store = ?1", store).stream()
        .map(a -> a.warehouse.id).distinct().count();
    boolean warehouseAlreadyServesStore = FulfilmentAssociation.count(
        "store = ?1 and warehouse = ?2", store, warehouse) > 0;
    if (!warehouseAlreadyServesStore && storeWarehouseCount >= MAX_WAREHOUSES_PER_STORE) {
      throw new WebApplicationException("A store can be fulfilled by at most 3 different warehouses.", 422);
    }

    long warehouseProductCount = FulfilmentAssociation.find("warehouse = ?1", warehouse).stream()
        .map(a -> a.product.id).distinct().count();
    boolean productAlreadyStored = FulfilmentAssociation.count(
        "warehouse = ?1 and product = ?2", warehouse, product) > 0;
    if (!productAlreadyStored && warehouseProductCount >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new WebApplicationException("A warehouse can store at most 5 different product types.", 422);
    }

    FulfilmentAssociation association = new FulfilmentAssociation();
    association.store = store;
    association.product = product;
    association.warehouse = warehouse;
    association.persist();
    return Response.status(Response.Status.CREATED).entity(FulfilmentAssociationView.from(association)).build();
  }

  @DELETE
  @Path("{id}")
  @Transactional
  public Response remove(@PathParam("id") Long id) {
    FulfilmentAssociation association = FulfilmentAssociation.findById(id);
    if (association == null) throw new WebApplicationException("Fulfilment association not found: " + id, 404);
    association.delete();
    return Response.noContent().build();
  }
}

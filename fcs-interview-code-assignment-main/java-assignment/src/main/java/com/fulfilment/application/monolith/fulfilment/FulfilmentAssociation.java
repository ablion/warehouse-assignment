package com.fulfilment.application.monolith.fulfilment;

import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A product supplied to a store by a warehouse. */
@Entity
@Table(name = "fulfilment_association", uniqueConstraints = @UniqueConstraint(
    name = "uk_fulfilment_store_product_warehouse",
    columnNames = {"store_id", "product_id", "warehouse_id"}))
public class FulfilmentAssociation extends PanacheEntity {

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "store_id", nullable = false, foreignKey = @ForeignKey(name = "fk_fulfilment_store"))
  public Store store;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_fulfilment_product"))
  public Product product;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "warehouse_id", nullable = false, foreignKey = @ForeignKey(name = "fk_fulfilment_warehouse"))
  public DbWarehouse warehouse;
}

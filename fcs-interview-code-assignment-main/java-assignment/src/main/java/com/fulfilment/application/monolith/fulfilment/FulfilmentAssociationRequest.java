package com.fulfilment.application.monolith.fulfilment;

/** JSON request for linking a product and warehouse to a store. */
public class FulfilmentAssociationRequest {
  public Long storeId;
  public Long productId;
  /** Active warehouse business unit code, not the database row id. */
  public String warehouseBusinessUnitCode;
}

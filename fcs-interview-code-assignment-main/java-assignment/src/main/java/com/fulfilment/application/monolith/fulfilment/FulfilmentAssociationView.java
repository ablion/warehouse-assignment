package com.fulfilment.application.monolith.fulfilment;

public class FulfilmentAssociationView {
  public Long id;
  public Long storeId;
  public String storeName;
  public Long productId;
  public String productName;
  public String warehouseBusinessUnitCode;
  public String warehouseLocation;

  public static FulfilmentAssociationView from(FulfilmentAssociation association) {
    FulfilmentAssociationView view = new FulfilmentAssociationView();
    view.id = association.id;
    view.storeId = association.store.id;
    view.storeName = association.store.name;
    view.productId = association.product.id;
    view.productName = association.product.name;
    view.warehouseBusinessUnitCode = association.warehouse.businessUnitCode;
    view.warehouseLocation = association.warehouse.location;
    return view;
  }
}

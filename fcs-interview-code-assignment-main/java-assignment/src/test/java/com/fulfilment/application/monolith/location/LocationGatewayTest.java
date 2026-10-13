package com.fulfilment.application.monolith.location;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LocationGatewayTest {
  private final LocationGateway gateway = new LocationGateway();

  @Test
  void resolvesExistingLocation() {
    var location = gateway.resolveByIdentifier("ZWOLLE-001");
    assertNotNull(location);
    assertEquals("ZWOLLE-001", location.identification);
    assertEquals(1, location.maxNumberOfWarehouses);
    assertEquals(40, location.maxCapacity);
  }

  @Test
  void resolutionIsCaseInsensitiveAndTrimsWhitespace() {
    assertEquals("AMSTERDAM-001", gateway.resolveByIdentifier(" amsterdam-001 ").identification);
  }

  @Test
  void unknownOrBlankLocationReturnsNull() {
    assertNull(gateway.resolveByIdentifier("UNKNOWN"));
    assertNull(gateway.resolveByIdentifier(" "));
    assertNull(gateway.resolveByIdentifier(null));
  }
}

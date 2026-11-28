package com.matchmap.api.geo;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class CityPinService {

  private final CityPinRepository pins;

  CityPinService(CityPinRepository pins) {
    this.pins = pins;
  }

  CityPin setPin(UUID userId, UUID cityId, PinStatus status) {
    Boolean active = pins.cityActive(cityId);
    if (active == null) {
      throw GeoException.cityNotFound();
    }
    if (!active) {
      throw GeoException.cityRetired();
    }
    pins.upsert(userId, cityId, status);
    return new CityPin(pins.findCity(cityId), status);
  }

  boolean removePin(UUID userId, UUID cityId) {
    pins.delete(userId, cityId);
    return true;
  }

  List<CityPin> pinsOf(UUID userId) {
    return pins.findByUser(userId);
  }

  PinStatus statusInCountry(UUID userId, String countryCode) {
    return pins.statusInCountry(userId, countryCode);
  }

  List<City> visitedCitiesIn(UUID userId, String countryCode) {
    return pins.visitedCitiesIn(userId, countryCode);
  }
}

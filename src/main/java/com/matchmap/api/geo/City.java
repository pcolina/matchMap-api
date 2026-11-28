package com.matchmap.api.geo;

import java.math.BigDecimal;
import java.util.UUID;

record City(UUID id, String name, BigDecimal latitude, BigDecimal longitude, String countryCode) {}

package com.matchmap.api.account;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
class CatalogChangeTestService {

  @PreAuthorize("isAuthenticated()")
  String change() {
    return "ok";
  }
}

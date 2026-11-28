package com.matchmap.api.account;

import org.springframework.stereotype.Service;

@Service
class CatalogChangeTestService {

  @RequiresAdmin
  String change() {
    return "ok";
  }
}

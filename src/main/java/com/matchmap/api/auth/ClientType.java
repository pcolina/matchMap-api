package com.matchmap.api.auth;

enum ClientType {
  WEB,
  MOBILE;

  static ClientType from(String header) {
    if ("web".equals(header)) {
      return WEB;
    }
    if ("mobile".equals(header)) {
      return MOBILE;
    }
    throw AuthException.invalidClient();
  }
}

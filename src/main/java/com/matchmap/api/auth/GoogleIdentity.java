package com.matchmap.api.auth;

record GoogleIdentity(String subject, String email, boolean emailVerified, String name) {}

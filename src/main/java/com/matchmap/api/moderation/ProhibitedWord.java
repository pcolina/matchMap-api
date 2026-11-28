package com.matchmap.api.moderation;

import java.util.UUID;

record ProhibitedWord(
    UUID id, String word, String normalized, String category, String language, boolean active) {}

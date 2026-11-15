package com.matchmap.api.moderation;

import java.util.List;

record WordLists(List<WordEntry> prohibited, List<WordEntry> reserved) {}

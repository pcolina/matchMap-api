INSERT INTO reserved_word (id, word, normalized, category, language, active, created_at, updated_at) VALUES
    (gen_random_uuid(), 'admin', 'admin', 'RESERVED', 'en', true, now(), now()),
    (gen_random_uuid(), 'administrator', 'administrator', 'RESERVED', 'en', true, now(), now()),
    (gen_random_uuid(), 'administrador', 'administrador', 'RESERVED', 'es', true, now(), now()),
    (gen_random_uuid(), 'root', 'rot', 'RESERVED', 'en', true, now(), now()),
    (gen_random_uuid(), 'superadmin', 'superadmin', 'RESERVED', 'en', true, now(), now()),
    (gen_random_uuid(), 'support', 'suport', 'RESERVED', 'en', true, now(), now()),
    (gen_random_uuid(), 'soporte', 'soporte', 'RESERVED', 'es', true, now(), now()),
    (gen_random_uuid(), 'staff', 'staf', 'RESERVED', 'en', true, now(), now()),
    (gen_random_uuid(), 'moderator', 'moderator', 'RESERVED', 'en', true, now(), now()),
    (gen_random_uuid(), 'moderador', 'moderador', 'RESERVED', 'es', true, now(), now()),
    (gen_random_uuid(), 'matchmap', 'matchmap', 'RESERVED', 'en', true, now(), now()),
    (gen_random_uuid(), 'system', 'system', 'RESERVED', 'en', true, now(), now()),
    (gen_random_uuid(), 'sistema', 'sistema', 'RESERVED', 'es', true, now(), now());

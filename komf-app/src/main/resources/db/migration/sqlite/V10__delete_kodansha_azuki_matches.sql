-- Kodansha retired api.kodansha.us and moved its catalog to Azuki.
-- The old API identified series and books with integer ids; Azuki uses uuids,
-- so every stored Kodansha match now points at an id that cannot be resolved.
DELETE
FROM SERIES_MATCH
WHERE PROVIDER = 'KODANSHA';

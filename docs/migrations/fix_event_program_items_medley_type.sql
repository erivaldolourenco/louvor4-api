-- 1. Atualiza a check constraint para aceitar o novo valor 'MEDLEY'.
--    PostgreSQL não permite alterar check constraints diretamente — é necessário
--    dropar e recriar.
ALTER TABLE event_program_items
    DROP CONSTRAINT event_program_items_type_check;

ALTER TABLE event_program_items
    ADD CONSTRAINT event_program_items_type_check
        CHECK (type IN ('MUSIC', 'MEDLEY', 'TEXT'));

-- 2. Corrige registros históricos que foram salvos como 'MUSIC' mas referenciam
--    um setlist item do tipo 'MEDLEY' (bug anterior em ProgramServiceImpl).
UPDATE event_program_items epi
SET type = 'MEDLEY'
WHERE epi.type = 'MUSIC'
  AND EXISTS (
      SELECT 1
      FROM event_setlist_items esi
      WHERE esi.id = epi.setlist_item_id
        AND esi.type = 'MEDLEY'
  );

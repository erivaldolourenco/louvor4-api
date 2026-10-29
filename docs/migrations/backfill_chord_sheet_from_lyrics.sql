-- Unificação letra + cifra: a letra passa a ser só um modo de visualização da
-- cifra. Para toda música que tem `lyrics` mas ainda não tem cifra, gera um
-- `chord_sheet_json` sem acordes a partir da letra.
--
-- Mesma regra do app (`_splitLyricsIntoBlocks` em chord_sheet_editor.dart):
-- blocos separados por linha em branco viram seções `other` sem nome, e
-- linhas em branco são descartadas.
--
-- Idempotente: só atinge músicas com `chord_sheet_json IS NULL`. A coluna
-- `lyrics` não é alterada.

WITH blocks AS (
    SELECT s.id,
           b.block,
           b.block_idx
    FROM songs s
    CROSS JOIN LATERAL regexp_split_to_table(
        replace(s.lyrics, E'\r\n', E'\n'),
        E'\n[ \t]*(\n[ \t]*)+'
    ) WITH ORDINALITY AS b(block, block_idx)
    WHERE s.chord_sheet_json IS NULL
      AND s.lyrics IS NOT NULL
      AND btrim(s.lyrics) <> ''
),
sections AS (
    SELECT bl.id,
           bl.block_idx,
           jsonb_build_object(
               'type', 'other',
               'label', '',
               'lines', jsonb_agg(
                   jsonb_build_object('text', l.line, 'chords', '[]'::jsonb, 'repeat', NULL)
                   ORDER BY l.line_idx
               )
           ) AS section
    FROM blocks bl
    CROSS JOIN LATERAL regexp_split_to_table(bl.block, E'\n')
        WITH ORDINALITY AS l(line, line_idx)
    WHERE btrim(l.line) <> ''
    GROUP BY bl.id, bl.block_idx
),
sheets AS (
    SELECT sec.id,
           jsonb_build_object(
               'schemaVersion', 1,
               'song', jsonb_build_object('originalKey', s.musical_key, 'bpm', s.bpm),
               'sections', jsonb_agg(sec.section ORDER BY sec.block_idx)
           ) AS sheet
    FROM sections sec
    JOIN songs s ON s.id = sec.id
    GROUP BY sec.id, s.musical_key, s.bpm
)
UPDATE songs s
SET chord_sheet_json = sh.sheet
FROM sheets sh
WHERE s.id = sh.id
  AND s.chord_sheet_json IS NULL;

-- Conferência: deve retornar 0.
-- SELECT count(*) FROM songs
-- WHERE lyrics IS NOT NULL AND btrim(lyrics) <> '' AND chord_sheet_json IS NULL;

-- Permite description nulo em events (nem toda requisição de criação envia descrição)

ALTER TABLE events ALTER COLUMN description DROP NOT NULL;

-- Permite o novo valor EDIT_CHORD_SHEET na coluna permission de event_participant_permissions

ALTER TABLE event_participant_permissions DROP CONSTRAINT IF EXISTS event_participant_permissions_permission_check;

ALTER TABLE event_participant_permissions ADD CONSTRAINT event_participant_permissions_permission_check
    CHECK (permission IN (
        'ADD_SONG',
        'EDIT_SETLIST',
        'REMOVE_SONG',
        'MANAGE_PARTICIPANTS',
        'EDIT_EVENT',
        'EDIT_CHORD_SHEET'
    ));

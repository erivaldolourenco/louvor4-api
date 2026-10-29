-- Permite os novos valores CAMERA, COMPUTER, MOBILE_PHONE e NOTEBOOK_MUSIC na coluna icon_key de project_skills

ALTER TABLE project_skills DROP CONSTRAINT IF EXISTS project_skills_icon_key_check;

ALTER TABLE project_skills ADD CONSTRAINT project_skills_icon_key_check
    CHECK (icon_key IN (
        'ACOUSTIC_GUITAR',
        'ELECTRIC_GUITAR',
        'PIANO',
        'KEYBOARD',
        'DRUMS',
        'MIC_VOCAL',
        'BASS_GUITAR',
        'MUSIC',
        'CAMERA',
        'COMPUTER',
        'MOBILE_PHONE',
        'NOTEBOOK_MUSIC'
    ));

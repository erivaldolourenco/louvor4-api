-- A feature de convite a projetos (PENDING_INVITE/DECLINED em
-- ProjectMemberStatus e PROJECT_MEMBER_INVITE* em NotificationType) foi
-- adicionada ao código, mas as check constraints do banco nunca foram
-- atualizadas para aceitar os novos valores.

-- 1. music_project_members.status
ALTER TABLE music_project_members
    DROP CONSTRAINT music_project_members_status_check;

ALTER TABLE music_project_members
    ADD CONSTRAINT music_project_members_status_check
        CHECK (status IN ('ACTIVE', 'REMOVED', 'PENDING_INVITE', 'DECLINED'));

-- 2. user_notification.type
ALTER TABLE user_notification
    DROP CONSTRAINT user_notification_type_check;

ALTER TABLE user_notification
    ADD CONSTRAINT user_notification_type_check
        CHECK (type IN (
            'EVENT_INVITE',
            'EVENT_PARTICIPANT_ACCEPTED',
            'EVENT_PARTICIPANT_DECLINED',
            'EVENT_PARTICIPANT_REMOVED',
            'EVENT_UPDATED',
            'EVENT_CANCELLED',
            'EVENT_REMINDER',
            'EVENT_SONG_ADDED',
            'EVENT_SONG_REMOVED',
            'EVENT_PROGRAM_UPDATED',
            'PROJECT_MEMBER_ADDED',
            'PROJECT_MEMBER_REMOVED',
            'PROJECT_MEMBER_INVITE',
            'PROJECT_MEMBER_INVITE_ACCEPTED',
            'PROJECT_MEMBER_INVITE_DECLINED',
            'MESSAGE_RECEIVED',
            'SYSTEM_NOTIFICATION'
        ));

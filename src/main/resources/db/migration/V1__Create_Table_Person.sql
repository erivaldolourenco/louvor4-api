CREATE TABLE IF NOT EXISTS `tb_person`
(
    `id` binary(16)   NOT NULL,
    `email` varchar(255) NOT NULL,
    `password` varchar(255) DEFAULT NULL,
    `account_non_expired` bit(1) DEFAULT NULL,
    `account_non_locked` bit(1) DEFAULT NULL,
    `credentials_non_expired` bit(1) DEFAULT NULL,
    `enabled` bit(1) DEFAULT NULL,
    `first_name` varchar(130) DEFAULT NULL,
    `last_name` varchar(255) DEFAULT NULL,
    `birthday` datetime(6) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY  (`email`)
);

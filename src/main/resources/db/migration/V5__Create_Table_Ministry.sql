CREATE TABLE IF NOT EXISTS `tb_ministry`
(
    `id` binary(16)   NOT NULL,
    `name` varchar(255) NOT NULL,
    `access_code` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
);

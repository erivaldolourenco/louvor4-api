CREATE TABLE IF NOT EXISTS `tb_member`
(
    `id` bigint(20)   NOT NULL AUTO_INCREMENT,
    `id_person` binary(16)  NOT NULL,
    `id_ministry` binary(16) NOT NULL,
    PRIMARY KEY (`id`)
);

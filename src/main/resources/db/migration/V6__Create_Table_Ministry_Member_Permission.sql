CREATE TABLE IF NOT EXISTS `tb_ministry_member_permission` (
    `id_ministry` binary(16) NOT NULL,
    `id_member` binary(16) NOT NULL,
    `id_permission` bigint(20) NOT NULL,
    PRIMARY KEY (`id_ministry`,`id_member`,`id_permission`),
    KEY `fk_ministry__member_permission` (`id_ministry`),
    CONSTRAINT `tb_ministry_member_permission_FK` FOREIGN KEY (`id_member`) REFERENCES `tb_person`(`id`),
    CONSTRAINT `tb_ministry_member_permission_FK_1` FOREIGN KEY (`id_permission`) REFERENCES `tb_permission`(`id`),
    CONSTRAINT `tb_ministry_member_permission_FK_2` FOREIGN KEY ( `id_ministry`) REFERENCES  `tb_ministry`(`id`)
)ENGINE=InnoDB

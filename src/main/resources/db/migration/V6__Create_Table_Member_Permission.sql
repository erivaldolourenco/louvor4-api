CREATE TABLE IF NOT EXISTS `tb_member_permission` (
  `id_member` bigint(20) NOT NULL,
  `id_ministry_permission` bigint(20) NOT NULL,
  PRIMARY KEY (`id_member`,`id_ministry_permission`),
  KEY `fk_user_permission_permission` (`id_ministry_permission`),
  CONSTRAINT `fk_member_permission` FOREIGN KEY (`id_member`) REFERENCES `tb_member` (`id`),
  CONSTRAINT `fk_ministry_permission` FOREIGN KEY (`id_ministry_permission`) REFERENCES `tb_ministry_permission` (`id`)
) ENGINE=InnoDB;

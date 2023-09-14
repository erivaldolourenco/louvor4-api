CREATE TABLE IF NOT EXISTS `tb_person_permission` (
  `id_person` binary(16) NOT NULL,
  `id_permission` bigint(20) NOT NULL,
  PRIMARY KEY (`id_person`,`id_permission`),
  KEY `fk_user_permission_permission` (`id_permission`),
  CONSTRAINT `fk_person` FOREIGN KEY (`id_person`) REFERENCES `tb_person` (`id`),
  CONSTRAINT `fk_permission` FOREIGN KEY (`id_permission`) REFERENCES `tb_permission` (`id`)
) ENGINE=InnoDB;

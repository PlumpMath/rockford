# ************************************************************
# Sequel Pro SQL dump
# Version 4541
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 5.7.10)
# Database: rockford
# Generation Time: 2016-06-20 15:46:29 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table alignment
# ------------------------------------------------------------

DROP TABLE IF EXISTS `alignment`;

CREATE TABLE `alignment` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `reference_id` int(11) unsigned DEFAULT NULL,
  `alignment_name` varchar(100) DEFAULT NULL,
  `results_filename` text,
  `complete` tinyint(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table alignment_results
# ------------------------------------------------------------

DROP TABLE IF EXISTS `alignment_results`;

CREATE TABLE `alignment_results` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `alignment_id` int(11) unsigned DEFAULT NULL,
  `participant_id` int(11) unsigned DEFAULT NULL,
  `dataset_id` int(11) unsigned DEFAULT NULL,
  `sequence` text,
  `dot_sequence` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table alignment_results_codons
# ------------------------------------------------------------

DROP TABLE IF EXISTS `alignment_results_codons`;

CREATE TABLE `alignment_results_codons` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `alignment_id` int(11) unsigned NOT NULL,
  `dataset_id` int(11) unsigned NOT NULL,
  `codon_id` int(11) unsigned NOT NULL,
  `sequence` varchar(30) NOT NULL,
  `dot_sequence` varchar(30) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table consensus
# ------------------------------------------------------------

DROP TABLE IF EXISTS `consensus`;

CREATE TABLE `consensus` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `alignment_id` int(11) unsigned DEFAULT NULL,
  `header` text,
  `filename` text,
  `sequence` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table consensus_codons
# ------------------------------------------------------------

DROP TABLE IF EXISTS `consensus_codons`;

CREATE TABLE `consensus_codons` (
  `consensus_id` int(11) unsigned NOT NULL,
  `codon_id` int(11) NOT NULL,
  `sequence` varchar(10) DEFAULT NULL,
  PRIMARY KEY (`consensus_id`,`codon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table reference
# ------------------------------------------------------------

DROP TABLE IF EXISTS `reference`;

CREATE TABLE `reference` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `reference_name` varchar(255) DEFAULT NULL,
  `sequence` text,
  `start_codon` int(11) DEFAULT NULL,
  `end_codon` int(11) DEFAULT NULL,
  `complete` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Dump of table reference_drms
# ------------------------------------------------------------

DROP TABLE IF EXISTS `reference_drms`;

CREATE TABLE `reference_drms` (
  `reference_id` int(11) unsigned NOT NULL,
  `codon_id` int(11) NOT NULL,
  `sequence` varchar(10) DEFAULT NULL,
  `is_drm` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`reference_id`,`codon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

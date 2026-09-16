-- 초기 MySQL 볼륨 생성 시 사용할 PetWay 테이블·키·인덱스를 정의한다.
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `app_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(150) NOT NULL,
  `nickname` varchar(50) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_role` enum('ADMIN','USER') NOT NULL,
  `user_status` enum('ACTIVE','WITHDRAWN') NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_app_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `user_auth` (
  `user_auth_id` bigint NOT NULL AUTO_INCREMENT,
  `auth_type` enum('KAKAO','LOCAL') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `password_hash` varchar(255) DEFAULT NULL,
  `provider` varchar(30) DEFAULT NULL,
  `provider_user_id` varchar(100) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_auth_id`),
  UNIQUE KEY `uk_user_auth_provider_user` (`provider`,`provider_user_id`),
  UNIQUE KEY `uk_user_auth_provider` (`provider`,`provider_user_id`),
  KEY `FK4u4u6c5mp2tsk82kjlsc4wo54` (`user_id`),
  CONSTRAINT `FK4u4u6c5mp2tsk82kjlsc4wo54` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `pet` (
  `pet_id` bigint NOT NULL AUTO_INCREMENT,
  `activity_level` enum('HIGH','LOW','MEDIUM') DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `breed` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `crowd_tolerance` enum('HIGH','LOW','MEDIUM') DEFAULT NULL,
  `is_primary` bit(1) NOT NULL,
  `neutered` bit(1) DEFAULT NULL,
  `pet_name` varchar(50) NOT NULL,
  `place_preference` enum('BOTH','INDOOR','OUTDOOR') DEFAULT NULL,
  `sociality_level` enum('HIGH','LOW','MEDIUM') DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `vaccinated` bit(1) DEFAULT NULL,
  `weight_kg` decimal(5,2) NOT NULL,
  `user_id` bigint NOT NULL,
  `size_type` enum('LARGE','MEDIUM','SMALL') DEFAULT NULL,
  `animal_registered` bit(1) DEFAULT NULL,
  `breed_type` enum('AFGHAN_HOUND','AIREDALE_TERRIER','AKITA','ALASKAN_MALAMUTE','AMERICAN_STAFFORDSHIRE_TERRIER','BASENJI','BEAGLE','BERNESE_MOUNTAIN_DOG','BICHON_FRISE','BORDER_COLLIE','BORZOI','BOSTON_TERRIER','BOXER','BULLDOG','BULL_TERRIER','CANE_CORSO','CAVALIER_KING_CHARLES_SPANIEL','CHIHUAHUA','CHOW_CHOW','COCKER_SPANIEL','DACHSHUND','DALMATIAN','DANGEROUS_BREED_MIX','DOBERMAN','ENGLISH_SPRINGER_SPANIEL','FRENCH_BULLDOG','GERMAN_SHEPHERD','GOLDEN_RETRIEVER','GREAT_DANE','GREAT_PYRENEES','ITALIAN_GREYHOUND','JACK_RUSSELL_TERRIER','JINDO','LABRADOR_RETRIEVER','LHASA_APSO','MALTESE','MINIATURE_PINSCHER','MIXED','NEWFOUNDLAND','OLD_ENGLISH_SHEEPDOG','PAPILLON','PEKINGESE','PIT_BULL_TERRIER','POMERANIAN','POODLE','PUG','ROTTWEILER','ROUGH_COLLIE','SAINT_BERNARD','SAMOYED','SCHNAUZER','SHETLAND_SHEEPDOG','SHIBA_INU','SHIH_TZU','SIBERIAN_HUSKY','STAFFORDSHIRE_BULL_TERRIER','TOSA','WELSH_CORGI','WEST_HIGHLAND_WHITE_TERRIER','WHIPPET','YORKSHIRE_TERRIER') DEFAULT NULL,
  PRIMARY KEY (`pet_id`),
  KEY `idx_pet_user_id` (`user_id`),
  CONSTRAINT `FKcw2vyauuecj7rj3515ivmsn02` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `favorite` (
  `favorite_id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(300) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `target_id` varchar(150) NOT NULL,
  `target_type` varchar(20) NOT NULL,
  `thumbnail_url` varchar(500) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`favorite_id`),
  UNIQUE KEY `uk_favorite_user_target` (`user_id`,`target_type`,`target_id`),
  KEY `idx_favorite_user_created` (`user_id`,`created_at`),
  CONSTRAINT `FKs07snk7840aod1vtwsrn66kra` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `review` (
  `review_id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(100) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `rating` int NOT NULL,
  `target_id` varchar(150) NOT NULL,
  `target_type` varchar(20) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`review_id`),
  UNIQUE KEY `uk_review_user_target` (`user_id`,`target_type`,`target_id`),
  CONSTRAINT `FKc7y0l3wac4n2ewm6a2uecd54c` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE IF NOT EXISTS `saved_trip` (
  `saved_trip_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `end_date` date NOT NULL,
  `itinerary_json` longtext NOT NULL,
  `place_count` int NOT NULL,
  `region_name` varchar(50) NOT NULL,
  `start_date` date NOT NULL,
  `title` varchar(100) NOT NULL,
  `transport_mode` varchar(20) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`saved_trip_id`),
  KEY `idx_saved_trip_user_created` (`user_id`,`created_at`),
  CONSTRAINT `FKmt30vdmib9h5kgswmy1pn1y65` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

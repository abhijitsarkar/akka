DROP TABLE IF EXISTS `USERS`;
CREATE TABLE `USERS` (
  `user_id` VARCHAR(16) NOT NULL,
  `first_name` VARCHAR(50) NOT NULL,
  `last_name` VARCHAR(50) NOT NULL,
  `phone_num` VARCHAR(25) NOT NULL,
  `email` VARCHAR(50) NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE INDEX `phone_num_UNIQUE` (`phone_num` ASC),
  UNIQUE INDEX `email_UNIQUE` (`email` ASC));
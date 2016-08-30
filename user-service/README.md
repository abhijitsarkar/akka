`mongod --dbpath /var/lib/mongodb`

> Change MySql directory permission.

```

CREATE SCHEMA `akka` DEFAULT CHARACTER SET utf8 ;

CREATE TABLE `akka`.`users` (
  `user_id` VARCHAR(16) NOT NULL,
  `first_name` VARCHAR(50) NOT NULL,
  `last_name` VARCHAR(50) NOT NULL,
  `phone_num` VARCHAR(25) NOT NULL,
  `email` VARCHAR(50) NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE INDEX `phone_num_UNIQUE` (`phone_num` ASC),
  UNIQUE INDEX `email_UNIQUE` (`email` ASC));
```

> There are various settings that seem to randomly affect the tests, like whether or not connection pool is enabled,
> the `PatienceConfig`. I create and delete a test user before and after every test and sometimes the user is
> getting deleted before the test finishes. If I run within a transaction, the results are different.
> None of this happens with MongoDB, only RDBMS (in-memory?).

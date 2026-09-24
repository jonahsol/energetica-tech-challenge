#!/bin/bash

docker exec -i enron-mysql \
  mysql -uroot -proot enron < enron-mysqldump_v5.sql

docker exec -i enron-mysql \
  mysql -uroot -proot enron < fts-init.sql
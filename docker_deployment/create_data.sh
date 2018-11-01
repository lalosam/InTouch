#!/usr/bin/env bash
mysql -u root -p$MYSQL_ROOT_PASSWORD < /scripts/vs_challenge_schema.sql
mysql -u root -p$MYSQL_ROOT_PASSWORD < /scripts/populate_data.sql
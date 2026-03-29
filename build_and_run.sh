#!/bin/bash

/usr/bin/mvn clean package -DskipTests && \
java -jar target/$(ls -t target/mysql-manager-*.jar | head -1 | xargs basename)

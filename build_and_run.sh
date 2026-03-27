#!/bin/bash

/usr/bin/mvn clean package -DskipTests && \\
java -jar target/mysql-manager-1.0.0.jar

#!/bin/bash
mvn clean
mvn install
cp target/http-proxy-server-1.0-SNAPSHOT-jar-with-dependencies.jar http-proxy.jar

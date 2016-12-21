#!/usr/bin/env bash

# Create the management network
docker network inspect dovs-mgmt > /dev/null 2>&1 ||                          \
    docker network create dovs-mgmt                                           \
        -o "com.docker.network.bridge.name"="dovs-mgmt"                       \
        -o "com.docker.network.bridge.enable_ip_masquerade"="false"

# Create the internal network
docker network inspect dovs-int > /dev/null 2>&1 ||                          \
    docker network create dovs-int --internal                                \
        -o "com.docker.network.bridge.name"="dovs-int"

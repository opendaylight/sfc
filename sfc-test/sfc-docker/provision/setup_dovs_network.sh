#!/usr/bin/env bash


# Create the management network
docker network inspect dovs-mgmt > /dev/null 2>&1 ||                 \
    docker network create                                            \
        -o "com.docker.network.bridge.name"="dovs-mgmt"              \
        -o "com.docker.network.bridge.enable_ip_masquerade"="false"  \
        dovs-mgmt

# Create the internal network
docker network inspect dovs-tun > /dev/null 2>&1 ||                  \
    docker network create                                            \
        --internal                                                   \
        -o "com.docker.network.bridge.name"="dovs-tun"               \
        dovs-tun

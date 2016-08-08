#!/bin/bash

function build_docker {
    cd /vagrant; docker-compose up -d
    # wait for containers ready
    if [ $(docker-compose ps -q | wc -l) -ne 6 ]; then
        sleep 60
    fi
}

function start_demo {

    /sfc/sfc-demo/sfc103/setup_sfc.py
    #wait for openflow effective
    sleep 60

    docker exec -it classifier1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
    docker exec -it classifier2 ovs-ofctl dump-flows -OOpenflow13 br-sfc
    docker exec -it sff1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
    docker exec -it sff2 ovs-ofctl dump-flows -OOpenflow13 br-sfc

    docker exec -it classifier1 ip netns exec app wget http://192.168.2.2


    #dynamic insert & remove sf
    /sfc/sfc-demo/sfc103/update_sfc.py

    #wait for openflow effective
    sleep 60

    docker exec -it classifier1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
    docker exec -it classifier2 ovs-ofctl dump-flows -OOpenflow13 br-sfc
    docker exec -it sff1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
    docker exec -it sff2 ovs-ofctl dump-flows -OOpenflow13 br-sfc

    docker exec -it classifier1 ip netns exec app wget http://192.168.2.2
}

echo "SFC DEMO: Build Docker"
build_docker

echo "SFC DEMO: Start Demo"
start_demo


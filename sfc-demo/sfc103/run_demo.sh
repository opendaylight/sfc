#!/bin/bash

function clean {
    cd /vagrant; docker-compose down
    printf "Stopping karaf ...  "
    spin=('/' '-' '\' '|' '-')
    i=0
    while $HOME/sfc/sfc-karaf/target/assembly/bin/client -u karaf 'system:shutdown -f' &> /dev/null
    do
        printf "\b${spin[$i]}"
        i=$(( (( $i + 1 )) % 5 ))
        # karaf is still running, wait for effective shutdown
        sleep 5
    done
    printf "\bdone\n"
}

function start_sfc {
    cd $HOME/sfc/sfc-karaf/target/assembly/
    sed -i "/^featuresBoot[ ]*=/ s/$/,odl-sfc-provider,odl-sfc-core,odl-sfc-ui,odl-sfc-openflow-renderer,odl-sfc-scf-openflow,odl-sfc-sb-rest,odl-sfc-ovs,odl-sfc-netconf/" etc/org.apache.karaf.features.cfg;
    echo "log4j.logger.org.opendaylight.sfc = DEBUG,stdout" >> etc/org.ops4j.pax.logging.cfg;
    rm -rf journal snapshots; bin/start
    #wait for sfc ready
    retries=3
    while [ $retries -gt 0 ]
    do
        sleep 60
        sfcfeatures=$($HOME/sfc/sfc-karaf/target/assembly/bin/client -u karaf 'feature:list -i' 2>&1 | grep odl-sfc | wc -l)
        if [ $sfcfeatures -eq 9 ]; then
            break
        fi
        retries=$(( $retries - 1 ))
    done
    if [ $retries -eq 0 ]; then
        echo "Karaf not started. Exit immediately"
        exit 1
    fi
}

function build_docker {
    cd /vagrant; docker-compose up -d
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


echo "SFC DEMO: Clean"
clean

echo "SFC DEMO: Start SFC"
start_sfc

echo "SFC DEMO: Build Docker"
build_docker

echo "SFC DEMO: Give some time to have all things ready"
sleep 120

echo "SFC DEMO: Start Demo"
start_demo

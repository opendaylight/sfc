#!/bin/bash

cd /vagrant
docker-compose up -d

inprog=1

cd ${HOME}
while [ $inprog -ne 0 ]
do
    inprog=`true &> /dev/null | sfc/sfc-demo/sfc103/sfc_ready.sh`
    if [ $inprog -ne 0 ]; then
        sleep 60
    fi
done

sfc/sfc-demo/sfc103/setup_sfc.py

#wait for openflow effective
sleep 60

docker exec -it classifier1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
docker exec -it classifier2 ovs-ofctl dump-flows -OOpenflow13 br-sfc
docker exec -it sff1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
docker exec -it sff2 ovs-ofctl dump-flows -OOpenflow13 br-sfc
docker exec -it classifier1 ip netns exec app wget http://192.168.2.2

#wait for openflow effective
sleep 60

sfc/sfc-demo/sfc103/update_sfc.py
docker exec -it classifier1 ip netns exec app wget http://192.168.2.2

#!/bin/bash

cd ${HOME}
printf "WAITING FOR SFC READY ... "
inprog=1
while [ $inprog -ne 0 ]
do
    inprog=`true &> /dev/null | /sfc/sfc-demo/sfc103/sfc_ready.sh`
    sleep 60
done
printf "OK\n\n"

cd /vagrant
printf "START CONTAINTERS DEPLOYMENT\n\n"
docker-compose up -d
# wait for docker deployment just in case
sleep 60
printf "\nCONTAINTERS DEPLOYMENT DONE\n\n"

cd ${HOME}
printf "START SETUP SFC\n\n"
true &> /dev/null | /sfc/sfc-demo/sfc103/setup_sfc.py

#wait for openflow effective
sleep 60
printf "\nSETUP SFC DONE\n"

printf "\n\nFLOWS AFTER SFC SETUP AND BEFORE WGET\n"
printf "\nCLASSIFIER1 FLOWS:\n"
docker exec -it classifier1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
printf "\nCLASSIFIER2 FLOWS:\n"
docker exec -it classifier2 ovs-ofctl dump-flows -OOpenflow13 br-sfc
printf "\nSFF1 FLOWS:\n"
docker exec -it sff1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
printf "\nSFF2 FLOWS:\n"
docker exec -it sff2 ovs-ofctl dump-flows -OOpenflow13 br-sfc

printf "\n\nWGET AFTER SFC SETUP\n"
docker exec -it classifier1 ip netns exec app wget http://192.168.2.2

printf "\n\nFLOWS AFTER SFC SETUP AND WGET\n"
printf "\nCLASSIFIER1 FLOWS:\n"
docker exec -it classifier1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
printf "\nCLASSIFIER2 FLOWS:\n"
docker exec -it classifier2 ovs-ofctl dump-flows -OOpenflow13 br-sfc
printf "\nSFF1 FLOWS:\n"
docker exec -it sff1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
printf "\nSFF2 FLOWS:\n"
docker exec -it sff2 ovs-ofctl dump-flows -OOpenflow13 br-sfc

printf "\n\nUPDATE SFC\n\n"
true &> /dev/null | /sfc/sfc-demo/sfc103/update_sfc.py

#wait for openflow effective
sleep 60
printf "\nUPDATE SFC DONE\n"

printf "\n\nWGET AFTER SFC UPDATE\n\n"
docker exec -it classifier1 ip netns exec app wget http://192.168.2.2

printf "\n\nFLOWS AFTER SFC UPDATE AND WGET\n"
printf "\nCLASSIFIER1 FLOWS:\n"
docker exec -it classifier1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
printf "\nCLASSIFIER2 FLOWS:\n"
docker exec -it classifier2 ovs-ofctl dump-flows -OOpenflow13 br-sfc
printf "\nSFF1 FLOWS:\n"
docker exec -it sff1 ovs-ofctl dump-flows -OOpenflow13 br-sfc
printf "\nSFF2 FLOWS:\n"
docker exec -it sff2 ovs-ofctl dump-flows -OOpenflow13 br-sfc

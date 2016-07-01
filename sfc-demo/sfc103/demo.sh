#!/bin/bash
set -eux

vagrant destroy -f
vagrant up
vagrant ssh odl -c "nohup /vagrant/setup_odl.sh & sleep 1"

connecting=1

while [ $connecting -ne 0 ]
do
    cat sfc.prog
    # 4 ovs nodes (sff1/2, classifier1/2) are connected
    vagrant ssh odl -c "grep 'Created OVS Node.*getOvsVersion' nohup.out | wc -l | grep 4" >& /dev/null
    connecting=$?
    sleep 60
done

vagrant ssh odl -c "/vagrant/setup.py"

#wait for openflow effective
sleep 60

vagrant ssh classifier1  -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh classifier2  -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh sff1 -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh sff2 -c "sudo ovs-ofctl dump-flows -OOpenflow13 br-sfc"
vagrant ssh classifier1  -c "sudo ip netns exec app wget http://192.168.2.2"

#!/bin/bash
#sudo apt-get install virtualbox vagrant -y
vagrant destroy -f
vagrant up
vagrant ssh -c "/vagrant/setup_sfc.sh"
vagrant ssh -c "/vagrant/setup_agent.sh"
vagrant ssh -c "nohup sfc-karaf-0.3.0-SNAPSHOT/bin/karaf & sleep 1"
vagrant ssh -c "nohup sudo python3.4 /sfc/sfc-py/sfc/sfc_agent.py --rest --odl-ip-port 192.168.1.4:8181 --auto-sff-name --nfq-class & sleep 1"

inprog=1

while [ $inprog -ne 0 ]
do
    echo "check system is ready"
    inprog=0
    vagrant ssh sfc -c "sfc-karaf-0.3.0-SNAPSHOT/bin/client -u karaf 'log:display' 2>/dev/null | grep 'Initialized RSP listener'"
    inprog+=$?
done

vagrant ssh -c "/vagrant/setup.py"
vagrant ssh -c "python3.4 /sfc/sfc-py/sfc/sff_client.py --remote-sff-ip 192.168.1.4 --remote-sff-port 4789 --sfp-id 1 --sfp-index 255"


#Demo Tutorial

This is a Mininet wrapper to create SFC topologies with dummy SFs to evaluate MAC Chaining and SFC VLAN


## Dependencies
- Python
- Mininet
- Scapy ( pipi install scapy, pip install requests)
- tcpdump, ethtool, hping3 (for load generation)


## Usage

- ODL must be running locally or remotely with SFC installed (odl-sfc-provider, odl-sfc-scf-openflow, odl-sfc-openflow-renderer)
- The API usage can be seen in the files sfc-macChain-topo.py and sfc-vla-topo.py
- Run any of those files with sudo and IP where the ODL is running
    - sudo python sfc-macChain-topo.py <ODL IP>
- It build a topology with mininet and configure ODL with the specified chain
- At the end of the chain a gateway will recovery the original mac addresses
- dummy sf and gateway logs can be foud on tmp dir
- To exercise the chain: in the mininet console, run any kind of UDP/TCP traffic between h1 and h2
    - ex: h1 hping3 --udp -p 5010 -o 2  -S 10.0.0.2 -c 1
- See ACL (classifier) rules in the odlConfGeneration.py







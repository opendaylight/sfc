
#Demo Tutorial

This is a Mininet wrapper to create SFC topologies with dummy SFs to evaluate two SFC OpenFlow renderers - MAC Chaining and SFC VLAN


## Dependencies
- Python
- Mininet
- Scapy (pip install scapy, pip install requests)
- tcpdump, ethtool, hping3 (for load generation)


## Usage

- ODL must be running locally or remotely with SFC installed (odl-sfc-provider, odl-sfc-scf-openflow, odl-sfc-openflow-renderer)
- The API usage can be seen in the files sfc-macChain-topo.py and sfc-vlan-topo.py
- Run any of those files with sudo and IP where the ODL is running
    - sudo python sfc-macChain-topo.py <ODL IP>
- The demo has only been teste on single node deployment, so use 127.0.0.1 as ODL IP for now
- It builds a topology with mininet and configure ODL with the specified chain
- At the end of the chain a gateway will recover the original mac addresses
- dummy SFs and gateway logs can be found on \tmp dir
- To exercise the chain: in the mininet console, run any kind of UDP/TCP traffic between h1 and h2
    - ex: h1 hping3 -p 5040 -o 2 -S 10.0.0.2 -c 1
- See ACL (classifier) rules in the odlConfGeneration.py

## Topology of sfc-macChain-topo.py file


+---------------+  +--------------+   +--------------+  +---------------+
|      SF 1     |  |     SF 2     |   |     SF 3     |  |      SF 4     |
|               |  |              |   |              |  |               |
+---------------+  +--------------+   +--------------+  +---------------+
             |        |                           |       |
             |        |                           |       |
         +---------------+                     +--------------+
         |     SFF2      |_____________________|     SFF3     |
         |               |                     |              |
         +---------------+                     +--------------+
                        \                      /
                         \                    /
                           +-----------------+           +--------------+
                           |      SFF1       |___________|              |
                           |  (classifier)   |           |   gateway    |
                           +-----------------+           +--------------+
                               |       |
                               |       |
                  +--------------+   +--------------+
                  |    host 1    |   |    host 2    |
                  |              |   |              |
                  +--------------+   +--------------+

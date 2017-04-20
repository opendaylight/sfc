
    dovs is a small utility that helps with:
    - running multiple independent OVS docker instances (nodes).
    - attaching network namespaces to such OVS instances (guests).
    - setup overlay neutron networks with the help of a running ODL instance.

    dovs expects an host environment with:

    - a docker image named 'docker-ovs:yyang' that runs openvswitch.
    - a docker network/bridge 'dovs-tun' to interconnect all the containers
    - a docker network/bridge 'dovs-mgmt' to reach the ODL controller
    - to setup overlay neutron networks, an ODL controller with netvirt-openstack
      feature enabled, reachable through dovsbr0.

    Note: if 'dovs-mgmt' has acquired an ip address via dhcp, ip addresses of the
          container interfaces connected to such network will also be assigned via
          dhcp.

    This results in a very lightweight environment on which certain aspects
    of neutron networking can be simulated and others that depend on it,
    tested:

    +------------------------------------------------------------------------+
    |                                                                        |
    | Guest                                                                  |
    |         +---------+   +---------+        +--------+    +--------+      |
    |         |network  |   |         |        |        |    |        |      |
    |         |namespace|   |         |        |        |    |        |      |
    |         |         |   |         |        |        |    |        |      |
    |         |         |   |         |        |        |    |        |      |
    |         |         |   |         |        |        |    |        |      |
    |         +----+----+   +----+----+        +----+---+    +---+----+      |
    |              |             |                  |            |           |
    |              |veth         |                  |            |           |
    |         +-----------------------+        +----+------------+----+      |
    |         |    |             |    |        |                      |      |
    |         |  +-+-------------+-+  |        |                      |      |
    |         |       br-int          |        |                      |      |
    |         |                       |        |                      |      |
    |         |                       |        |                      |      |
    |         |     OVS docker        |        |                      |      |
    |         |     instance          |        |                      |      |
    |         |                       |        |                      |      |
    |         +-----+--------+--------+        +---+----------+-------+      |
    |               |        |                     |          |              |
    |         +-----+------------------------------+------------------+      |
    |      dovs-tun          |                                |              |
    |                        |                                |              |to ODL
    |         +--------------+--------------------------------+--------------------->
    |      dovs-mgmt                                                         |
    +------------------------------------------------------------------------+

    Subcommand sfc-config allows:
     - Creating Networking (compute nodes and guests)
     - Setting Service Function Chaining (SFC) Configuration in Opendaylight Controller (ODL)
        service-function-chains (SFC), service-function-paths (SFP),
        service-function-forwarders (SFF), service-functions (SF)
     - Creating/removing Rendered Service Path (RSP) in ODL


    Example 1: Run a OVS instance and attach a guest namespace.

      > dovs add-node --name node-1
      > dovs add-guest --name guest-1 --ip 10.0.0.1/24 --node node-1
      > dovs add-guest --name guest-2 --ip 10.0.0.2/24 --node node-1

      Starts a docker container with a running OVS and creates two network
      namespaces that are connected to a bridge of that OVS. Each namespace
      ip stack is configured appropriately.

      By default the OVS bridge behaves as a learning switch: a ping from the
      guest-1 namespace to the guest-2 ip address should work:
      > ip netns exec dovs-guest-1 ping -c 1 10.0.0.2

      This is a simple setup that does not require ODL and does not
      resemble any neutron networking, but can be used as a quick 'up and
      running' setup of an usable OVS instance.

      TODO: Support similar scenario with more than one container manually
      setting up the tunnels between them.

      To go back to the initial state:
      > dovs clean


    Example 2: Networking of 2 compute nodes, 4 VMs and 2 isolated subnets.

      > dovs add-node --name compute-1 --odl 192.168.56.1
      > dovs add-node --name compute-2 --odl 192.168.56.1
      > dovs add-guest --name vm-1 --ip 10.0.0.1/24 --net subnet-A --node compute-1 --odl 192.168.56.1
      > dovs add-guest --name vm-2 --ip 10.0.0.1/24 --net subnet-B --node compute-1 --odl 192.168.56.1
      > dovs add-guest --name vm-3 --ip 10.0.0.2/24 --net subnet-A --node compute-2 --odl 192.168.56.1
      > dovs add-guest --name vm-4 --ip 10.0.0.2/24 --net subnet-B --node compute-2 --odl 192.168.56.1

      Starts two docker containers, each of them with an independent OVS.
      Creates 4 network namespaces each attached to an OVS bridge of the
      specified container and the ip configuration as provided.

      Everything is setup as a neutron agent would setup and as ODL would find
      it on an Openstack deployment. Thus, this could be seen as simulating
      the networking of 2 compute nodes and 2 VMs on each node.

      We setup two overlay subnets with overlapping ip addresses in ODL. This
      results in the flow & tunnel configuration on each OVS so that there is
      connectivity between every namespace of the same subnet.

      Similar can be achieved with the spawn shortcut command:
      > dovs spawn --nodes 2 --guests 2 --nets 2 --odl 192.168.56.1

      A guest can be re-attached to a different to simulate a VM migration:
      > dovs move-guest --name-guest vm-1 --name-dstnode compute-2

      To go back to the initial state:
      > dovs clean --odl 192.168.56.1

     Example 3: Networking with routing between subnets.

      > dovs add-node --name compute-1 --odl 192.168.56.1
      > dovs add-node --name compute-2 --odl 192.168.56.1
      > dovs add-guest --name vm-1 --ip 10.0.0.1/24 --net subnet-A --router router-1 --node compute-1 --odl 192.168.56.1
      > dovs add-guest --name vm-2 --ip 10.0.1.1/24 --net subnet-B --router router-1 --node compute-1 --odl 192.168.56.1
      > dovs add-guest --name vm-3 --ip 10.0.0.2/24 --net subnet-A --router router-1 --node compute-2 --odl 192.168.56.1
      > dovs add-guest --name vm-4 --ip 10.0.1.2/24 --net subnet-B --router router-1 --node compute-2 --odl 192.168.56.1

      This is very similar to the Example 2 setup above, but now we do not have
      overlapping ip addresses. Each subnet is interconnected through a router
      and there is connectivity between every namespace.

      Similar can be achieved with the spawn shortcut command:
      > dovs spawn --nodes 2 --guests 2 --nets 2 --routed --odl 192.168.56.1

     Example 4: Create two chains (one symmetric of each other) and one SF

     Using the following command, three nodes and one guest per node (client,
     firewall and server) are created. A SFC chain is also created.

      > sudo dovs sfc-config --chains "[['client1', 'firewall', 'server1']]" --odl 172.28.128.1

     Then you create the two RSPs with the following command:

      > sudo dovs sfc-config --create-rsp-from-id 1 --odl 172.28.128.1
 
     Create the classification rule

      > curl -i -H "Content-Type: application/json" -H "Cache-Control: no-cache" --data '{ "acl": [ { "acl-name": "ACL1", "acl-type": "ietf-access-control-list:ipv4-acl", "access-list-entries": { "ace": [ { "rule-name": "ACE1", "actions": { "netvirt-sfc-acl:rsp-name": "RSP1" }, "matches": { "network-uuid" : "177bef73-514e-4922-990f-d7aba0f3b0f4", "source-ipv4-network": "10.0.0.0/24", "protocol": "6", "source-port-range": { "lower-port": 0 }, "destination-port-range": { "lower-port": 80 } } } ] } }]}' -X PUT --user admin:admin http://172.28.128.1:8181/restconf/config/ietf-access-control-list:access-lists/acl/ietf-access-control-list:ipv4-acl/ACL1

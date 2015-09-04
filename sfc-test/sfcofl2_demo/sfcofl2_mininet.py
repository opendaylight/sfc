__author__ = "Brady Johnson"
__author__ = "Rodrigo Lopez Manrique"
__copyright__ = "Copyright(c) 2015, Ericsson, Inc."
__license__ = "Eclipse Public License v1.0"
__version__ = "0.2"
__email__ = "brady.allen.johnson@ericsson.com"
__email__ = "rodrigolopezmanrique@gmail.com"
__status__ = "demo code"

### NOTES:
### 1 Install vlan: sudo apt-get install vlan
### 2 Install openswitch version 2.2+. Visit https://github.com/mininet/mininet/wiki/Installing-new-version-of-Open-vSwitch

from mininet.cli import CLI
from mininet.node import Controller, Host, RemoteController, OVSController
from mininet.util import customConstructor
from mininet.net import Mininet
from mininet.topo import Topo
from mininet.clean import cleanup

from mininet.log import lg
from mininet.log import setLogLevel, info

from optparse import OptionParser
from string import Template
import argparse
import apt
import os

#package check Start
cache = apt.Cache()
if cache['vlan'].is_installed:
    print "Vlan installed"
else:
    print "ERROR:VLAN package not  installed. Please run sudo apt-get install vlan"
    exit(1)


# Constants
MAX_CLIENTS  = 10
MAX_SERVERS  = 10
MAX_ELEMENTS = 9 # Notice, changing this value will affect the MAC addresses
VLAN_ID      = 1000
MININET_DUMP_FILE = '/tmp/mininet_dump.txt'
MININET_NET_FILE = '/tmp/mininet_net.txt'
LOOPBACK_FILE_PATTERN = '''
sudo ovs-vsctl set bridge $NODE protocols=OpenFlow10,OpenFlow11,OpenFlow12,OpenFlow13
sudo ovs-ofctl -O Openflow13 add-flow $NODE "table=0,priority=10,in_port=1,dl_type=0x0800,nw_proto=6,tp_dst=80,actions=move:NXM_OF_ETH_DST[0..31]->NXM_NX_REG0[],move:NXM_OF_ETH_DST[32..47]->NXM_NX_REG1[0..15],move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],move:NXM_NX_REG0[0..31]->NXM_OF_ETH_SRC[0..31],move:NXM_NX_REG1[0..15]->NXM_OF_ETH_SRC[32..47],in_port"
sudo ovs-ofctl -O Openflow13 add-flow $NODE "table=0,priority=10,in_port=1,dl_type=0x0800,nw_proto=6,tp_src=80,actions=move:NXM_OF_ETH_DST[0..31]->NXM_NX_REG0[],move:NXM_OF_ETH_DST[32..47]->NXM_NX_REG1[0..15],move:NXM_OF_ETH_SRC[]->NXM_OF_ETH_DST[],move:NXM_NX_REG0[0..31]->NXM_OF_ETH_SRC[0..31],move:NXM_NX_REG1[0..15]->NXM_OF_ETH_SRC[32..47],in_port"
sudo ovs-ofctl -O Openflow13 add-flow $NODE "table=0,priority=1,actions=drop"
'''


#
# Simple class that holds Service Function info
# A Service Function (SF) in the context of SFC is a host
#
class ServiceFunctionInfo(object):
    def __init__(self, name='SF', mac='00:00:00:00:00:01', ip='10.0.0.1', vlan_id=0):
        self.name    =  name
        self.mac_     =  mac
        self.ip_      =  ip
        self.vlan_id_ =  vlan_id


#
# Simple class that holds openFlow Switch info
#
class SwitchInfo(object):
    def __init__(self, name='S', opts=''):
        self.name    =  name
        self.opts     =  opts


#
# Simple class that holds all of the mininet configurable parameters
#
class Context(object):
    def __init__(self):
        # Just setting default values
        self.service_functions = []
        self.sf_forwarders  = []
        self.sf_loopbacks   = []

        self.clients = []
        self.clients_number = '1'

        self.servers = []
        self.servers_number = '1'

        self.gateways = []

        self.base_port = 1
        self.operation_mode = 'vlan'
        self.demo_mode  = 'vlan'
        self.topology_tor = False
        self.tor_info = None

        self.gateway_client = 'gw1'
        self.gateway_server = 'gw2'
        self.gateway_args   = ''


        self.switch_number  = '2'
        self.switch_args    = 'OpenFlow13'

        self.sf_number      = '2'
        self.sf_args        = ''
        self.sf_loops       = False

        self.remote_controller_ip   = '192.168.56.101'
        self.remote_controller_port = '6633'
        self.remote_controller_args = ''
        self.remote_controller_name = 'c5'

#
# Get the command line args
#
def get_cmd_line_args(context):

    opts = argparse.ArgumentParser()


    ### ARGUMENTS ###

    # Demo mode
    opts.add_argument('--demo-mode', '-D',
                  default=context.demo_mode,
                  dest='demo_mode',
                  help='Set the demo mode, vlan or mpls. Vlan by default')
    # Operation mode
    opts.add_argument('--operation-mode', '-O',
                  default=context.operation_mode,
                  dest='operation_mode',
                  help='Set a VLAN or or not between the SFFs and the SFs: vlan or no-vlan')
    # Switch topology mode, linear or underlay/star mode
    opts.add_argument('--topology-tor', '-U',
                  default=context.topology_tor,
                  action = 'store_const',
                  const = True,
                  dest='topology_tor',
                  help='Set the topology to use a Top-Of-Rack underlay switch')

    # Remote controller
    opts.add_argument('--controller-ip', '-I',
                  default=context.remote_controller_ip,
                  dest='remote_controller_ip',
                  help='Remote Controller IP')

    opts.add_argument('--controller-port', '-P',
                  default=context.remote_controller_port,
                  dest='remote_controller_port',
                  help='Remote Controller port')

    # Switch
    opts.add_argument('--switch-number', '-S',
                  default=context.switch_number,
                  dest='switch_number',
                  help='Number of OPF Switches')

    opts.add_argument('--switch-protocol', '-T',
                  default=context.switch_args,
                  dest='switch_args',
                  help='Switches protocol')

    # Service function
    opts.add_argument('--sf-loop', '-L',
                  dest='sf_loop',
                  action = 'store_const',
                  const = True,
                  default=context.sf_loops,
                  help='Activate to create loops in service functions')

    opts.add_argument('--sf-number','-N',
                  default=context.sf_number,
                  dest='sf_number',
                  help='Total number of Service Functions')

    #Clients
    opts.add_argument('--clients-number', '-C',
                  default=context.clients_number,
                  dest='clients_number',
                  help='Number of clients')

    #Servers
    opts.add_argument('--servers-number', '-R',
                  default=context.servers_number,
                  dest='servers_number',
                  help='Number of servers')

    args = opts.parse_args()

    context.remote_controller_ip    =  args.remote_controller_ip
    context.remote_controller_port  =  args.remote_controller_port
    context.remote_controller_args  = 'remote,ip=%s,port=%s' % (context.remote_controller_ip, context.remote_controller_port)

    context.sf_number        =  args.sf_number
    context.sf_loop          =  args.sf_loop

    context.switch_number    =  args.switch_number
    context.switch_args      = 'protocols=' + args.switch_args

    context.clients_number   = args.clients_number
    context.servers_number   = args.servers_number

    context.demo_mode = args.demo_mode
    context.operation_mode   =  args.operation_mode
    context.topology_tor     =  args.topology_tor

    #### CHECK ARGUMENTS ####

    # Demo mode
    if (context.demo_mode != 'vlan') and (context.demo_mode != 'mpls'):
        print "Error: Demo mode is incorrect. Try with --demo-mode vlan or --demo-mode mpls"
        return False

    # Operation mode
    if (context.operation_mode != 'vlan') and (context.operation_mode != 'no-vlan'):
        print "Error: Operation mode is incorrect. Try with --operation-mode vlan or --operation-mode no-vlan"
        return False

    # Number of switches and Service functions
    if int(context.switch_number ) > MAX_ELEMENTS  or int(context.sf_number ) > MAX_ELEMENTS:
        print "Error: max number of openFlow switches/ Service Functions is %d" % MAX_ELEMENTS
        return False

    # Number of clients
    if int(context.clients_number) > MAX_CLIENTS:
        print "Error: Max number of clients is %d" % MAX_CLIENTS
        return False

    # Number of servers
    if int(context.servers_number) > MAX_SERVERS:
        print "Error: Max number of servers is %d" % MAX_SERVERS
        return False
    #### If there should be 1 SF per switch, then match them ####

    if int(context.switch_number) > int(context.sf_number):
        print "Error: There must be at least one SF per SFF"
        return False

    if int(context.sf_number) % int(context.switch_number) != 0:
        print "Error: The number of SFs must be a multiple of the number of SFFs"
        return False

    #### INITIALIZE ALL COMPONENTS ###

    # Clients
    for i in range(int(context.clients_number)):
        client_name = "{}{}".format("client", i+1)
        client_mac  = "{}{}".format("00:00:00:00:01:0", i+1)
        client_ip   = "{}{}".format("10.0.0.", i+1)
        context.clients.append(ServiceFunctionInfo(client_name, client_mac, client_ip))

    # Servers
    for i in range(int(context.servers_number)):
        server_name = "{}{}".format("server", i+1)
        server_mac  = "{}{}".format("00:00:00:00:11:0", i+1)
        server_ip   = "{}{}".format("10.10.0.", i+1)
        context.servers.append(ServiceFunctionInfo(server_name, server_mac, server_ip))

    # Gateways
    context.gateways.append(SwitchInfo("gw1" , '' ))
    context.gateways.append(SwitchInfo("gw2" , '' ))

    if context.topology_tor:
        context.tor_info = SwitchInfo("tor1" , '' )

    # SFs
    for i in range(int(context.sf_number)):
        sf_name = "{}{}".format("sf", i+1)
        sf_mac  = "{}{}".format("00:00:00:00:00:0", i+2)
        sf_ip   = "{}{}".format("10.0.0.", int(context.clients_number) + i + 1)
        sf_vlan = VLAN_ID  * (i+1)
        if context.sf_loop:
            context.service_functions.append(None)
        else:
            if context.operation_mode == 'vlan':
                context.service_functions.append(ServiceFunctionInfo(sf_name, sf_mac, sf_ip,sf_vlan))
            else:
                context.service_functions.append(ServiceFunctionInfo(sf_name, sf_mac, sf_ip))

    # SFFs
    ##### NOTE: This order is important. The Openflows switches have to be synchronized with the ODL configuration.
    for i in range(int(context.switch_number)):
        switch_name     = "{}{}".format("sff", i+1)
        switch_protocol = context.switch_args 
        context.sf_forwarders.append(SwitchInfo(switch_name , switch_protocol ))

    return True

#### SET UP VLAN ###

class VlanHost(Host):
    def __init__(self, *args, **kwargs):
        super(VlanHost, self).__init__(*args, **kwargs)

    def config( self, vlan=100, **params ):
        """Configure VlanHost according to (optional) parameters:
           vlan: VLAN ID for default interface"""
        r = super( Host, self ).config( **params )

        intf = self.defaultIntf()
        # remove IP from default, "physical" interface
        retval = self.cmd( 'ifconfig %s inet 0' % intf )
        #print 'ifconfig %s inet 0 => result: %s' % (intf, retval)
        # create VLAN interface
        retval = self.cmd( 'vconfig add %s %d' % ( intf, vlan ) )
        print 'vconfig add %s %d => result: %s' % (intf, vlan, retval)
        # assign the host's IP to the VLAN interface
        retval = self.cmd( 'ifconfig %s.%d inet %s' % ( intf, vlan, params['ip'] ) )
        #print 'ifconfig %s.%d inet %s => result: %s' % (intf, vlan, params['ip'], retval)

        # update the intf name and host's intf map
        newName = '%s.%d' % ( intf, vlan )

        # update the (Mininet) interface to refer to VLAN interface name
        intf.name = newName
        # add VLAN interface to host's name to intf map
        self.nameToIntf[ newName ] = intf

        return r

### CREATE TOPOLOGY  ###
### 1 SFF per SF. Multiple clients can be linked to a GW, the same with the servers.

def create_topology(context):

    topo = Topo()
    h = ''
    sfs_per_sff = int(context.sf_number) / int(context.switch_number)


    # Add the links SFFs - SFs
    for i in range(len(context.sf_forwarders)):
        print context.sf_forwarders[i].opts
        s = topo.addSwitch(context.sf_forwarders[i].name, opts=context.sf_forwarders[i].opts)
        for j in range(sfs_per_sff):
            sf_index = (i*int(sfs_per_sff))+j

            if not context.service_functions[sf_index]:
                # Add the Loop switches instead of normal hosts
                sf_loopback_name = '%s-node%d' % (context.sf_forwarders[i].name,j+1)
                context.sf_loopbacks.append(sf_loopback_name)
                h = topo.addSwitch(sf_loopback_name, opts='')
            else:
                # Add the SFs as normal hosts
                if context.service_functions[sf_index].vlan_id_ == 0:
                    h = topo.addHost(context.service_functions[sf_index].name,
                             ip=context.service_functions[sf_index].ip_,
                             mac=context.service_functions[sf_index].mac_)

                else:
                    h = topo.addHost(context.service_functions[sf_index].name,
                             cls=VlanHost,
                             vlan=context.service_functions[sf_index].vlan_id_,
                             ip=context.service_functions[sf_index].ip_,
                             mac=context.service_functions[sf_index].mac_)

            # Connect the SF to the SFF
            topo.addLink(node1=h, node2=s)

    # Add the GWs
    gw1 = topo.addSwitch(context.gateways[0].name, opts=context.gateways[0].opts)
    gw2 = topo.addSwitch(context.gateways[1].name, opts=context.gateways[1].opts)

    if context.topology_tor:
        # Create the Top-of-Rack switch
        tor = topo.addSwitch(context.tor_info.name, opts=context.tor_info.opts)

        # Connect each SFF to the ToR switch
        for i in range(len(context.sf_forwarders)):
            topo.addLink(context.tor_info.name, context.sf_forwarders[i].name)

        # Add the links between the GWs and the tor
        topo.addLink(context.gateways[0].name, context.tor_info.name)
        topo.addLink(context.gateways[1].name, context.tor_info.name)

    else:
        # Add the links between SFFs
        for i in range(len(context.sf_forwarders)-1):
            topo.addLink(context.sf_forwarders[i].name, context.sf_forwarders[i+1].name)

        # Add the links between SFFs and GWs
        topo.addLink(context.gateways[0].name, context.sf_forwarders[0].name)
        topo.addLink(context.gateways[1].name, context.sf_forwarders[len(context.sf_forwarders)-1].name)

    # Add the link between gw1 and gw2
    topo.addLink(context.gateway_client, context.gateway_server )

    # Add the clients and their links to GW1
    for i in range(int(context.clients_number)):
        h = topo.addHost(context.clients[i].name,
                         ip=context.clients[i].ip_,
                         mac=context.clients[i].mac_)
        topo.addLink(node1=h, node2=gw1)

    # Add the servers and their links to GW2
    for i in range(len(context.servers)):
        h = topo.addHost(context.servers[i].name,
                         ip=context.servers[i].ip_,
                         mac=context.servers[i].mac_)
        topo.addLink(node1=h, node2=gw2)

    return topo

def start_switches(context,network, odl_controller, local_controller):
    for switch in network.switches:
        if switch.opts == context.switch_args:
            print 'Starting switch (%s) with ODL controller...' % (switch.name)
            switch.start([odl_controller])
        else:
            print 'Starting switch (%s) with local controller...' % (switch.name)
            switch.start([local_controller])

def dump_hosts(network):
    for host in network.hosts:
        print 'Host %s, IP %s, MAC %s, IFs %s' % (host.name, host.IP(), host.MAC(), host.intfNames())

# Copied from mininet.cli.py but I need the output to go to a file
def dumpNodes(nodes):
    "Dump node info."

    with open(MININET_DUMP_FILE, 'w+b') as outfile:
        for node in nodes:
            outfile.write( '%s\n' % repr( node ) )

# Copied from mininet.util.py but I need the output to go to a file
def dumpNodeConnections(nodes):
    "Dump connections to/from nodes."

    with open(MININET_NET_FILE, 'w+b') as outfile:
        for node in nodes:
            outfile.write( node.name )

            for intf in node.intfList():
                outfile.write( ' %s:' % intf )
                if intf.link:
                    intfs = [ intf.link.intf1, intf.link.intf2 ]
                    intfs.remove( intf )
                    outfile.write( str(intfs[ 0 ]) )
                else:
                    outfile.write( ' ' )

            outfile.write( '\n' )

# Insert flows into the switches
def init_flows(context):

    #Gateways initialization flows
    insert_flows_by_cmd('sudo ovs-vsctl set bridge gw1 protocols=OpenFlow10,OpenFlow11,OpenFlow12,OpenFlow13')
    insert_flows_by_cmd('sudo ovs-vsctl set bridge gw2 protocols=OpenFlow10,OpenFlow11,OpenFlow12,OpenFlow13')
    # Match any drop flows in both GWs
    insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw1 "table=0,priority=1,actions=drop"')
    insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw2 "table=0,priority=1,actions=drop"')

    # Gatway client flows
    gw1_out_port = int(3)
    for client in context.clients:
        if (context.demo_mode == 'vlan'):
            print 'GWS - VLAN client flows:'
            insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw1 "table=0,priority=30,in_port=1,vlan_tci=0x1000/0x1000,ip,nw_dst=%s,actions=strip_vlan,set_field:%s->eth_dst,output=%d"' % (client.ip_, client.mac_, gw1_out_port))

            # ARP flows
            #   Arp Req and Rsp from the client
            insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw1 "table=0,priority=20,in_port=%d,arp,actions=output=2"' % (gw1_out_port))
            #   Arp Req and Rsp to the client
            insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw1 "table=0,priority=20,in_port=2,arp,arp_tpa=%s,actions=set_field:%s->eth_dst,output=%d"' % (client.ip_, client.mac_, gw1_out_port))

            # ICMP ping flows
            #   ICMP from the client
            insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw1 "table=0,priority=10,in_port=%d,icmp,actions=output=2"' % (gw1_out_port))
            #   ICMP to the client
            insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw1 "table=0,priority=10,in_port=2,icmp,nw_dst=%s,actions=set_field:%s->eth_dst,output=%d"' % (client.ip_, client.mac_, gw1_out_port))

        elif (context.demo_mode == 'mpls'):
            print 'GWS - MPLS client flows:'
            insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw1 "table=0,priority=30,in_port=1,mpls,nw_dst=%s,actions=pop_mpls=0x000b,set_field:%s->eth_dst,output=%d"' % (client.ip_, client.mac_, gw1_out_port))
        gw1_out_port += 1

    # Gatway server flows
    gw2_out_port = int(3)
    for server in context.servers:
        if (context.demo_mode == 'vlan'):
            print 'GWS - VLAN server flows:'
            insert_flows_by_cmd('sudo ovs-ofctl -O OpenFlow13 add-flow gw2 "table=0,priority=10,in_port=1,vlan_tci=0x1000/0x1000,ip,nw_dst=%s,actions=strip_vlan,set_field:%s->eth_dst,output=%d"' % (server.ip_, server.mac_, gw2_out_port))

            # ARP flows
            #   Arp Req and Rsp from the server
            insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw2 "table=0,priority=20,in_port=%d,arp,actions=output=2"' % (gw2_out_port))
            #   Arp Req and Rsp to the server
            insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw2 "table=0,priority=20,in_port=2,arp,arp_tpa=%s,actions=set_field:%s->eth_dst,output=%d"' % (server.ip_, server.mac_, gw2_out_port))

            # ICMP ping flows
            #   ICMP from the server
            insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw2 "table=0,priority=10,in_port=%d,icmp,actions=output=2"' % (gw2_out_port))
            #   ICMP to the server
            insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow gw2 "table=0,priority=10,in_port=2,icmp,nw_dst=%s,actions=set_field:%s->eth_dst,output=%d"' % (server.ip_, server.mac_, gw2_out_port))

        elif (context.demo_mode == 'mpls'):
            print 'GWS - MPLS server flows:'
            insert_flows_by_cmd('sudo ovs-ofctl -O OpenFlow13 add-flow gw2 "table=0,priority=10,in_port=1,mpls,nw_dst=%s,actions=pop_mpls=0x000b,set_field:%s->eth_dst,output=%d"' % (server.ip_, server.mac_, gw2_out_port))

        gw2_out_port += 1


    # SFs flows
    if context.sf_loop:
        print 'SF Loopback switch flows:'
        for loopback_sf in context.sf_loopbacks:
            loopback_template = Template(LOOPBACK_FILE_PATTERN)
            insert_flows_by_cmd(loopback_template.substitute(NODE=loopback_sf))

    # ToR flows
    if context.topology_tor:
        print 'ToR setup...'
        insert_flows_by_cmd('sudo ovs-vsctl set bridge tor1 protocols=OpenFlow10,OpenFlow11,OpenFlow12,OpenFlow13')
        insert_flows_by_cmd('sudo ovs-ofctl -O Openflow13 add-flow tor1 "table=0,priority=5,actions=drop"')

# Load the flow commands from a file and execute them
def insert_flows_from_file(fileName):
    if not os.path.exists(fileName):
        print 'Cant load flows from file, file does not exist %s' % fileName
        return

    with open(fileName,'r') as f:
        for line in f.readlines():
            os.system(line)

def insert_flows_by_cmd(flow_cmd):
    os.system(flow_cmd)
#
# Main
#
def main():
    lg.setLogLevel('info')

    context = Context()
    if not get_cmd_line_args(context):
        return

    print 'Starting with [%s] switches' % context.switch_number

    # Create the topology with the context args taken from the cmd-line
    myTopo = create_topology(context)

    # The network
    myNet = Mininet(myTopo)

    # The SDN-remote_controller connection
    mySDNController = myNet.addController(context.remote_controller_name,
                                       customConstructor({'remote': RemoteController},
                                       context.remote_controller_args))
    myLocalController = myNet.addController('c1', controller=OVSController)
    myLocalController.start()

    dump_hosts(myNet)

    # This will output the nodes port connections to MININET_NET_FILE
    dumpNodeConnections(myNet.values())
    # This will output the nodes port connections to MININET_DUMP_FILE    
    dumpNodes(myNet.values())

    #start_gateways(myNet)
    start_switches(context, myNet, mySDNController, myLocalController)

    # Insert ovs-ofctl rules
    print 'Inserting flows into the switches...'
    init_flows(context)

    # Start the command line
    CLI(myNet)

    cleanup()

if __name__ == '__main__':
    main()


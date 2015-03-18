__author__ = "Brady Allen Johnson"
__author__ = "Rodrigo Lopez Manrique"
__copyright__ = "Copyright(c) 2015, Ericsson, Inc."
__license__ = "New-style BSD"
__version__ = "0.1"
__email__ = "brady.allen.johnson@ericsson.com"
__email__ = "rodrigolopezmanrique@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 03/18/2015"


from mininet.cli import CLI
from mininet.node import Controller, Host, RemoteController, OVSController
from mininet.util import customConstructor
from mininet.net import Mininet
from mininet.topo import Topo
from mininet.clean import cleanup

from mininet.log import lg
from mininet.log import setLogLevel, info

from optparse import OptionParser
import argparse
import apt

#package check Start
cache = apt.Cache()
if cache['vlan'].is_installed:
    print "Vlan installed"
else:
    print "ERROR:VLAN package not  installed please run sudo apt-get install vlan"
    exit(1)


# Constants
MAX_CLIENTS  = 3
MAX_SERVERS  = 2
MAX_ELEMENTS = 7 # Care, it will affect MAC address
VLAN_ID      = 100

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

        self.clients = []
        self.clients_number = '1'

        self.servers = []
        self.servers_number = '1'

        self.gateways = []

        self.base_port = 1
        self.operation_mode = 'vlan'

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

    # Operation mode
    opts.add_argument('--mode', '-M',
                  default=context.operation_mode,
                  dest='operation_mode',
                  help='Set the operation mode: vlan or no-vlan')

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
    #TODO Implement if the number of SFFs and SFs can differ
    """opts.add_argument('--switch-number', '-S',
                  default=context.switch_number,
                  dest='switch_number',
                  help='Number of OPF Switches') """

    opts.add_argument('--switch-protocol', '-T',
                  default=context.switch_args,
                  dest='switch_args',
                  help='Protocol used by SFF. Default is Openflow13')

    # Service function
    opts.add_argument('--sf-loop', '-L',
                  dest='sf_loop',
                  action = 'store_const',
                  const = True,
                  default=context.sf_loops,
                  help='Active to create loops in service functions')

    opts.add_argument('--sf-number','-N',
                  default=context.sf_number,
                  dest='sf_number',
                  help='Number of Service Functions.')

    #Clients
    opts.add_argument('--clients-number', '-C',
                  default=context.clients_number,
                  dest='clients_number',
                  help='Number of clients')

    #Servers
    opts.add_argument('--servers-number', '-S',
                  default=context.servers_number,
                  dest='servers_number',
                  help='Number of servers')


    args = opts.parse_args()

    context.remote_controller_ip    =  args.remote_controller_ip
    context.remote_controller_port  =  args.remote_controller_port
    context.remote_controller_args  = 'remote,ip=%s,port=%s' % (context.remote_controller_ip, context.remote_controller_port)

    context.sf_number        =  args.sf_number
    context.sf_loop          =  args.sf_loop


    context.switch_number    =  context.sf_number
    context.switch_args      = 'protocols=' + args.switch_args

    context.clients_number   = args.clients_number

    context.servers_number   = args.servers_number

    context.operation_mode   =  args.operation_mode


    #### CHECK ARGUMENTS ####

    # Operation mode
    if (context.operation_mode != 'vlan') and (context.operation_mode != 'no-vlan'):
        print "Error: Operation mode is incorrect. Try with --mode vlan or --mode mpls"
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

    if int(context.switch_number ) > int(context.sf_number ):
        context.sf_number     = context.switch_number
    else:
        context.switch_number = context.sf_number



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

# Create topology with 1 switch per host. Also all swiches are linked

def create_topology(context):

    topo = Topo()
    h = ''


    # Add the links SFFs - SFs
    for i in range(len(context.sf_forwarders)):
        print context.sf_forwarders[i].opts
        s = topo.addSwitch(context.sf_forwarders[i].name, opts=context.sf_forwarders[i].opts)
        # Add the Loop switches instead of normal hosts
        if not context.service_functions[i]:
            h = topo.addSwitch(context.sf_forwarders[i].name+'-node', opts=context.sf_forwarders[i].opts)
        # Add the SFs
        else:
            if context.service_functions[i].vlan_id_ == 0:
                h = topo.addHost(context.service_functions[i].name,
                         ip=context.service_functions[i].ip_,
                         mac=context.service_functions[i].mac_)

            else:
                h = topo.addHost(context.service_functions[i].name,
                         cls=VlanHost,
                         vlan=context.service_functions[i].vlan_id_,
                         ip=context.service_functions[i].ip_,
                         mac=context.service_functions[i].mac_)


        topo.addLink(node1=h, node2=s)

    # Add the links between SFFs
    for i in range(len(context.sf_forwarders)-1):
        topo.addLink(context.sf_forwarders[i].name, context.sf_forwarders[i+1].name )


    # Add the GWs
    gw1 = topo.addSwitch(context.gateways[0].name, opts=context.gateways[0].opts)
    gw2 = topo.addSwitch(context.gateways[1].name, opts=context.gateways[1].opts)


    # Add the links between SFFs and GWs
    topo.addLink(context.gateways[0].name, context.sf_forwarders[0].name)
    topo.addLink(context.gateways[1].name, context.sf_forwarders[len(context.sf_forwarders)-1].name)

    # Add the link between gw1 and gw2
    topo.addLink(context.gateway_client, context.gateway_server )


    # Add the clients and GW1
    for i in range(int(context.clients_number)):
        h = topo.addHost(context.clients[i].name,
                         ip=context.clients[i].ip_,
                         mac=context.clients[i].mac_)
        topo.addLink(node1=h, node2=gw1)

    # Add the servers and GW2
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

#
# main
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

    #start_gateways(myNet)
    start_switches(context, myNet, mySDNController, myLocalController)

    # Start the command line
    CLI(myNet)

    cleanup()

if __name__ == '__main__':
    main()


#!/usr/bin/python

from mininet.net import Mininet
from mininet.node import Controller, RemoteController, OVSController
from mininet.node import CPULimitedHost, Host, Node
from mininet.node import OVSKernelSwitch, UserSwitch
from mininet.node import IVSSwitch
from mininet.cli import CLI
from mininet.log import setLogLevel, info, output
from mininet.link import TCLink, Intf
from subprocess import call

from mininet.term import cleanUpScreens, makeTerm
import time # sleep
import argparse

def execInXterm(nodes, script):
    for node in nodes:
        makeTerm(node, title=str(node)+' '+ script , term='xterm', display=None,
            cmd='bash -c '+ script)


def myNetwork(with_windows = False):

    net = Mininet( topo=None,
                   build=False,
                   ipBase='10.0.0.0/8')

    info( '*** Adding controller\n' )
    c0=net.addController(name='c0',
                      controller=Controller,
                      protocol='tcp',
                      port=6633)

    info( '*** Add switches\n')
    s1 = net.addSwitch('s1', cls=OVSKernelSwitch)
    s2 = net.addSwitch('s2', cls=OVSKernelSwitch)
    s3 = net.addSwitch('s3', cls=OVSKernelSwitch)

    info( '*** Add hosts\n')
    h1 = net.addHost('h1', cls=Host, ip='10.0.0.1', defaultRoute=None)
    h4 = net.addHost('h4', cls=Host, ip='10.0.0.4', defaultRoute=None)
    h3 = net.addHost('h3', cls=Host, ip='10.0.0.3', defaultRoute=None)
    h2 = net.addHost('h2', cls=Host, ip='10.0.0.2', defaultRoute=None)

    info( '*** Add links\n')
    net.addLink(h1, s1)
    net.addLink(s1, s2)
    net.addLink(s2, h2)
    net.addLink(s2, s3)
    net.addLink(s3, h3)
    net.addLink(s3, h4)

    info( '*** Starting network\n')
    net.build()
    info( '*** Starting controllers\n')
    for controller in net.controllers:
        controller.start()

    info( '*** Starting switches\n')
    net.get('s1').start([c0])
    net.get('s2').start([c0])
    net.get('s3').start([c0])

    info( '*** Post configure switches and hosts\n')

    net.pingAll()

    # s1
    s1.dpctl('add-flow', 'priority=5,in_port=1,actions=output:2', '')
    s1.dpctl('add-flow', 'priority=5,in_port=2,actions=output:1', '')

    info( 'flows s1: ' + s1.dpctl('dump-flows', '', '') )

    # s2, s3
    flow_1_2 = 'priority=255,ip,in_port=1,nw_src=10.0.0.1,nw_dst=10.0.0.4,actions=output:2'
    flow_2_3 = 'priority=255,ip,in_port=2,nw_src=10.0.0.1,nw_dst=10.0.0.4,actions=output:3'
    flow_3_2 = 'priority=255,ip,in_port=3,nw_src=10.0.0.4,nw_dst=10.0.0.1,actions=output:2'
    flow_2_1 = 'priority=255,ip,in_port=2,nw_src=10.0.0.4,nw_dst=10.0.0.1,actions=output:1'

    s2.dpctl('add-flow', flow_1_2, '')
    s2.dpctl('add-flow', flow_2_3, '')
    s2.dpctl('add-flow', flow_3_2, '')
    s2.dpctl('add-flow', flow_2_1, '')

    s3.dpctl('add-flow', flow_1_2, '')
    s3.dpctl('add-flow', flow_2_3, '')
    s3.dpctl('add-flow', flow_3_2, '')
    s3.dpctl('add-flow', flow_2_1, '')

    s2.dpctl('add-flow', 'priority=65535,arp,arp_tpa=10.0.0.1,actions=output:1', '')
    s2.dpctl('add-flow', 'priority=65535,arp,arp_tpa=10.0.0.2,actions=output:2', '')
    s2.dpctl('add-flow', 'priority=65535,arp,arp_tpa=10.0.0.3,actions=output:3', '')
    s2.dpctl('add-flow', 'priority=65535,arp,arp_tpa=10.0.0.4,actions=output:3', '')

    s3.dpctl('add-flow', 'priority=65535,arp,arp_tpa=10.0.0.1,actions=output:1', '')
    s3.dpctl('add-flow', 'priority=65535,arp,arp_tpa=10.0.0.2,actions=output:1', '')
    s3.dpctl('add-flow', 'priority=65535,arp,arp_tpa=10.0.0.3,actions=output:2', '')
    s3.dpctl('add-flow', 'priority=65535,arp,arp_tpa=10.0.0.4,actions=output:3', '')

    """<<< Just in case asymmetric flows were needed
    # s2, s3
    flow_1_2 = 'priority=255,in_port=1,actions=output:2'
    flow_2_3 = 'priority=255,in_port=2,actions=output:3'
    flow_3_1 = 'priority=255,in_port=3,actions=output:1'
    s2.dpctl('add-flow', flow_1_2, '')
    s2.dpctl('add-flow', flow_2_3, '')
    s2.dpctl('add-flow', flow_3_1, '')

    s3.dpctl('add-flow', flow_1_2, '')
    s3.dpctl('add-flow', flow_2_3, '')
    s3.dpctl('add-flow', flow_3_1, '')
    <<<<"""

    info( 'flows s2: ' + s1.dpctl('dump-flows', '', '') )
    info( 'flows s3: ' + s1.dpctl('dump-flows', '', '') )

    # net.pingAll()

    if (with_windows):
        cleanUpScreens()
        execInXterm([h2, h3], './sf_hhe.sh; read')
        execInXterm([h4], './server.sh')
        execInXterm([h4], './tcp_dump.sh')
        execInXterm([h1], './tcp_dump.sh')
        execInXterm([h2], './tcp_dump.sh')
        execInXterm([h3], './tcp_dump.sh')
        execInXterm([h1], 'xterm')

        ## wireshark
        cmd='wireshark'
        opts='-i h1-eth0 -k'
        h1.cmd( cmd + ' ' + opts + '&' )
        opts='-i h2-eth0 -k'
        h2.cmd( cmd + ' ' + opts + '&' )
        opts='-i h3-eth0 -k'
        h3.cmd( cmd + ' ' + opts + '&' )
        opts='-i h4-eth0 -k'
        h4.cmd( cmd + ' ' + opts + '&' )
        time.sleep(1)
    else:
        h4.sendCmd('./server.sh')
        time.sleep(1)
        h3.sendCmd('./sf_hhe.sh')
        time.sleep(1)
        h2.sendCmd('./sf_hhe.sh')
        time.sleep(1)
        #h1.sendCmd('./tcp_dump.sh > h1_tcp_dump.txt')

        h1.cmd('./client.sh')
    time.sleep(1)

    h1.sendCmd('python3 ../sf_hhe/HttpClient.py -ip 10.0.0.4')
    output( h1.waitOutput() )

    if (with_windows):
        execInXterm([h1], './client.sh ; read')
        CLI(net)
        cleanUpScreens()
    else:
        h1.cleanup()
        h2.cleanup()
        h3.cleanup()
        h4.cleanup()

    net.stop()

if __name__ == '__main__':
    #setLogLevel( 'info' )
    setLogLevel( 'output' )

    parser = argparse.ArgumentParser(description='Mininet for testing Service Function HTTP Header Enrichment sf_hhe.py', prog='sfc_topology_symmetric_chain.py', usage='%(prog)s [options]', add_help=True)

    parser.add_argument('-g', '--gui', action='store_true', default=False,
        help='With Xterms and wireshark (Need support for X session)')

    info(parser.format_help())

    args = parser.parse_args()
    myNetwork(with_windows=args.gui)


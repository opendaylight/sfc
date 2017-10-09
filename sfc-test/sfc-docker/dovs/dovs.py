#!/usr/bin/env python

from plumbum import local, cli, ProcessExecutionError, colors
from plumbum.cli.switches import Flag
from itertools import groupby
from functools import reduce, wraps
from ipaddress import ip_interface, ip_network
from requests import HTTPError
from retry import retry
from ast import literal_eval
from sets import Set

import shlex
import time
import tortilla
import uuid
import logging

import json
import requests
import ast
import os
import sys
import random

BRIDGE = 'br-int'
DOVS_PREFIX = 'dovs-'

def ifNotFound(default):
    """ Decorator to return a default value on HTTP 404 """
    def onHttpRequest(request):
        @wraps(request)
        def wrapper(*args, **kwargs):
            try:
                got = request(*args, **kwargs)
            except HTTPError as e:
                if not e.response.status_code == 404:
                    raise e
                return default
            return got
        return wrapper
    return onHttpRequest

def InfoIfNoException(method):
    """ Decorator to call Info if the wrapped method completed with no
    exception """
    @wraps(method)
    def wrapper(self):
        try:
            method(self)
        except:
            raise
        else:
            if self.parent:
                Info.invoke()
    return wrapper

class LOG:
    @staticmethod
    def debug(msg, *args, **kwargs):
        logging.debug(' ' + msg.format(*args, **kwargs))

    @staticmethod
    def info(msg, *args, **kwargs):
        logging.info(msg.format(*args, **kwargs))

    @staticmethod
    def warn(msg, *args, **kwargs):
        logging.warning(colors.warn | '  ' + msg.format(*args, **kwargs))

    @staticmethod
    def error(msg, *args, **kwargs):
        logging.error(colors.fatal | '  ' + msg.format(*args, **kwargs))

class ODL:

    """ Helper to work with ODL restconf API """

    NEUTRON_RES = 'restconf/config/neutron:neutron'
    IETF_IF_RES = 'restconf/config/ietf-interfaces:interfaces/interface'
    NETWORK = DOVS_PREFIX + 'net'
    NETWORK_ID = '177bef73-514e-4922-990f-d7aba0f3b0f4'
    TENANT_ID = '5d806f0e-e197-4a72-88e6-72b024fa5c97'
    SEGMENTATION_ID = '1'

    class Ugly:
        """ So ugly that it just does not compare """
        def __eq__():
            return False

    @staticmethod
    def getApi(ip, debug):
        if not ip:
            return None
        api = tortilla.wrap('http://{}:8181'.format(ip), debug=debug)
        api.config.headers.authorization = 'Basic YWRtaW46YWRtaW4='
        return api

    @staticmethod
    @ifNotFound(False)
    def isInterfaceParent(api, name, isParent):
        try:
            interface = api(ODL.IETF_IF_RES).get(name)['interface'][0]
            parent = interface['odl-interface:parent-interface']
            return True if parent == isParent else False
        except KeyError:
            return False

    @staticmethod
    @ifNotFound([])
    def getAllNetworks(api):
        networks = api(ODL.NEUTRON_RES).networks.get()
        networks = networks.get('networks',{}).get('network',[])
        return [n for n in networks if n.get('tenant-id', ODL.Ugly()) == ODL.TENANT_ID]

    @staticmethod
    @ifNotFound(None)
    def getNetwork(api, idOrName):
        networks = ODL.getAllNetworks(api)
        return next(
            (n for n in networks if
                n['uuid']==idOrName or n.get('name', ODL.Ugly())==idOrName),
            None)

    @staticmethod
    def addNetwork(api, id, name):
        network = {
            'network' : {
                'uuid' : id,
                'name' : name if name else id,
                'tenant-id' : ODL.TENANT_ID,
                'shared' : 'false',
                'admin-state-up' : 'true',
                'network-provider-extension:network-type' : 'neutron-networks:network-type-vxlan',
                'network-provider-extension:segmentation-id' : ODL.SEGMENTATION_ID,
                'network-l3-extension:external' : "false",
             }
        }
        api(ODL.NEUTRON_RES).networks.network(id).put(data=network)
        return network['network']

    @staticmethod
    def delAllNetworks(api):
        networks = ODL.getAllNetworks(api)
        for network in networks:
            api(ODL.NEUTRON_RES).networks.network.delete(network['uuid'])

    @staticmethod
    @ifNotFound([])
    def getAllSubnets(api):
        subnets = api(ODL.NEUTRON_RES).subnets.get()
        subnets = subnets.get('subnets',{}).get('subnet',[])
        return [s for s in subnets if s.get('tenant-id', ODL.Ugly()) == ODL.TENANT_ID]

    @staticmethod
    @ifNotFound(None)
    def getSubnet(api, idOrName):
        subnets = ODL.getAllSubnets(api)
        return next(
            (s for s in subnets if
                s['uuid']==idOrName or s.get('name', ODL.Ugly())==idOrName),
            None)

    @staticmethod
    def addSubnet(api, id, name, net, cidr, gwip):
        subnet = {
            'subnet' : {
                'uuid' : id,
                'name' : name if name else id,
                'tenant-id' : ODL.TENANT_ID,
                'network-id' : net,
                'ip-version' : 'neutron-constants:ip-version-v4',
                'cidr' : cidr,
                'gateway-ip' : gwip
            }
        }
        api(ODL.NEUTRON_RES).subnets.subnet(id).put(data=subnet)
        return subnet['subnet']

    @staticmethod
    def delAllSubnets(api):
        subnets = ODL.getAllSubnets(api)
        for subnet in subnets:
            api(ODL.NEUTRON_RES).subnets.subnet.delete(subnet['uuid'])

    @staticmethod
    @ifNotFound([])
    def getAllRouters(api):
        routers = api(ODL.NEUTRON_RES).routers.get()
        routers = routers.get('routers',{}).get('router',[])
        return [r for r in routers if r.get('tenant-id', ODL.Ugly()) == ODL.TENANT_ID]

    @staticmethod
    @ifNotFound(None)
    def getRouter(api, idOrName):
        routers = ODL.getAllRouters(api)
        return next(
            (r for r in routers if
                r['uuid']==idOrName or r.get('name', ODL.Ugly())==idOrName),
            None)

    @staticmethod
    def addRouter(api, id, name):
        router = {
            'router' : {
                'uuid' : id,
                'name' : name,
                'tenant-id' : ODL.TENANT_ID
            }
        }
        api(ODL.NEUTRON_RES).routers.router(id).put(data=router)
        return router['router']

    @staticmethod
    def delAllRouters(api):
        routers = ODL.getAllRouters(api)
        for router in routers:
            api(ODL.NEUTRON_RES).routers.router.delete(router['uuid'])

    @staticmethod
    @ifNotFound([])
    def getAllPorts(api):
        ports = api(ODL.NEUTRON_RES).ports.get()
        ports = ports.get('ports',{}).get('port',[])
        return [p for p in ports if p.get('tenant-id', ODL.Ugly()) == ODL.TENANT_ID]

    @staticmethod
    @ifNotFound(None)
    def getPortsOfSubnet(api, snId):
        ports = ODL.getAllPorts(api)
        return [p for p in ports if p['fixed-ips'][0]['subnet-id'] == snId]

    @staticmethod
    @ifNotFound(None)
    def getPortOfSubnetRouter(api, snId):
        ports = ODL.getPortsOfSubnet(api, snId)
        return next(
            (p for p in ports if
                p['device-owner'] == 'network:router_interface'),
            None)

    @staticmethod
    def addPort(api, id, name, net, subnet, mac, ip, owner, dev):
        port = {
            'port' : {
                'uuid' : id,
                'name' : name if name else id,
                'tenant-id' : ODL.TENANT_ID,
                'network-id' : net,
                'admin-state-up' : 'true',
                'device-owner' : owner,
                'device-id' : dev,
                'mac-address' : mac,
                'fixed-ips' : {
                    'subnet-id' : subnet,
                    'ip-address' : ip
                }
            }
        }

        if owner == 'compute':
            port['port']['neutron-binding:host-id'] = 'ovsdb://uuid/{}/bridge/{}'.format(dev, BRIDGE)
            port['port']['neutron-binding:vif-type'] = 'macvtap'

        api(ODL.NEUTRON_RES).ports.port(id).put(data=port)
        return port['port']

    @staticmethod
    def delAllPorts(api):
        ports = ODL.getAllPorts(api)
        for port in ports:
            api(ODL.NEUTRON_RES).ports.port.delete(port['uuid'])


class CMD:
    """  Helper to work with simple shell pipeline commands """

    DOVS_MGMT = DOVS_PREFIX + 'mgmt'
    DOVS_INT = DOVS_PREFIX + 'tun'
    DOCKER_IMAGE = 'docker-ovs:yyang'
    # tmpfs due to kernel bug
    # https://github.com/docker/docker/issues/12080
    DOCKER_OPTS = '-itd --cap-add NET_ADMIN --tmpfs /var/run/'

    NODE_RUN = 'docker run %s --net %s {opts} %s' % (DOCKER_OPTS, DOVS_MGMT, DOCKER_IMAGE)
    NODE_INT = 'docker network connect %s {id}' % DOVS_INT
    NODE_STOP = 'docker stop {id}'
    NODE_REMOVE = 'docker rm -fv {id}'
    NODE_STATUS = 'docker inspect -f {{{{.State.Running}}}} {id}'
    NODE_IP_FMT = '{{{{(index .NetworkSettings.Networks \\"%s\\").IPAddress}}}}' % DOVS_INT
    NODE_PREFIXLEN_FMT = '{{{{(index .NetworkSettings.Networks \\"%s\\").IPPrefixLen}}}}' % DOVS_INT
    NODE_CIDR_FMT = NODE_IP_FMT + '/' + NODE_PREFIXLEN_FMT
    NODE_CIDR = 'docker inspect -f "' + NODE_CIDR_FMT + '" {id}'
    NODE_PID = 'docker inspect -f {{{{.State.Pid}}}} {id}'
    NODE_NAME_LIST = 'docker ps --no-trunc -a | awk "/ %s / {{print $NF}}"' % DOCKER_IMAGE
    NODE_ID_LIST = 'docker ps --no-trunc -a | awk "/ %s / {{print $1}}"' % DOCKER_IMAGE
    NODE_EXEC = 'docker exec {id} '

    OVS_STOP = 'supervisorctl stop all'
    NODE_OVS_STOP = NODE_EXEC + OVS_STOP
    OVS_STATUS = 'supervisorctl status configure-ovs | awk "{{print $2}}"'
    NODE_OVS_STATUS = NODE_EXEC + OVS_STATUS

    OVS_EXTRA_INFO = 'other-config:dovs-info'
    OVS_ATT_MAC = 'external_ids:attached-mac'
    OVS_IFACE_ID = 'external_ids:iface-id'
    OVS_LOCAL_IP = 'other_config:local_ip'
    OVS_OFPORT = 'ofport'

    OVS_WAIT_BRIDGE = '--timeout=20 wait-until bridge %s' % BRIDGE
    OVS_ADD_BRIDGE = 'add-br %s' % BRIDGE
    OVS_ADD_PORT = 'add-port %s {dev}' % BRIDGE
    OVS_DEL_PORT = 'del-port %s {dev}' % BRIDGE
    OVS_BRIDGE_DPID = 'get br %s datapath_id' % BRIDGE
    OVS_SET_NODE_IP = 'set Open_vSwitch {ovsid} %s={ip}' % OVS_LOCAL_IP
    OVS_SET_BRIDGE_IP = 'set br %s %s={ip}' % (BRIDGE, OVS_LOCAL_IP)
    OVS_IF_SET = 'set interface {dev} {info}'
    OVS_IF_GET = 'get interface {dev} {info}'
    OVS_IF_DEL = 'remove interface {dev} {info}'
    OVS_UUID = 'show | awk "NR==1"'
    OVS_SET_MAN = 'set-manager tcp:{ip}:6640'
    OVS_SET_CONT = 'set-controller %s tcp:{ip}:6653' % BRIDGE
    OVS_LIST_IF = '--columns=name find interface ' + OVS_EXTRA_INFO + '!=dummy | awk "{{print $3}}"'

    NODE_OVS_EXEC = NODE_EXEC + 'ovs-vsctl '
    NODE_OVS_BRIDGE_DPID = NODE_OVS_EXEC + OVS_BRIDGE_DPID
    NODE_OVS_WAIT_BRIDGE = NODE_OVS_EXEC + OVS_WAIT_BRIDGE
    NODE_OVS_ADD_BRIDGE = NODE_OVS_EXEC + OVS_ADD_BRIDGE
    NODE_OVS_SET_BRIDGE_IP = NODE_OVS_EXEC + OVS_SET_BRIDGE_IP + ' -- ' + OVS_SET_NODE_IP
    NODE_OVS_IF_SET = NODE_OVS_EXEC + OVS_IF_SET
    NODE_OVS_IF_GET = NODE_OVS_EXEC + OVS_IF_GET
    NODE_OVS_ADD_PORT = NODE_OVS_EXEC + OVS_ADD_PORT + ' -- ' + OVS_IF_SET
    NODE_OVS_DEL_PORT = NODE_OVS_EXEC + OVS_DEL_PORT
    NODE_OVS_UUID = NODE_OVS_EXEC + OVS_UUID
    NODE_OVS_SET_MAN = NODE_OVS_EXEC + OVS_SET_MAN
    NODE_OVS_SET_ODL = NODE_OVS_EXEC + OVS_SET_MAN + ' -- ' + OVS_SET_CONT
    NODE_OVS_LIST_IF = NODE_OVS_EXEC + OVS_LIST_IF

    NS_CREATE = 'ip netns add {ns}'
    NS_DEL = 'ip netns del {ns}'
    NS_PIDS = 'ip netns pids {ns}'
    NS_LIST = 'ip netns | grep "^%s"' % DOVS_PREFIX
    NS_MKDIR = 'mkdir -p /var/run/netns'
    NS_LINK = 'ln -s /proc/{pid}/ns/net /var/run/netns/{pid}'
    NS_UNLINK = 'rm -rf /var/run/netns/{pid}'
    NS_EXEC = 'ip netns exec {ns} '
    NS_VETH_CREATE = NS_EXEC + 'ip l add {dev} type veth peer name {peer}'
    NS_MOVE = NS_EXEC + 'ip l set {dev} netns {tons}'
    NS_IF_SET_IP = NS_EXEC + 'ip a add {ip} dev {dev}'
    NS_IF_MAC = NS_EXEC + 'ip a show {dev} | awk " /^ *link/ {{print $2}}"'
    NS_IF_IP = NS_EXEC + 'ip a show {dev} | awk " /^ *inet / {{print $2}}"'
    NS_IF_UP = NS_EXEC + 'ip l set dev {dev} up'
    NS_DHCP_START = NS_EXEC + 'dhclient eth0 -pf /var/run/dhclient.{id}.pid -lf /var/lib/dhclient/dhclient.{id}.leases'
    NS_DHCP_STOP = NS_EXEC + 'dhclient eth0 -r -pf /var/run/dhclient.{id}.pid -lf /var/lib/dhclient/dhclient.{id}.leases'
    NS_DEFAULT_ROUTE = NS_EXEC + 'ip route add default via {ip}'
    KILL = 'kill {pid}'
    MGMT_DHCP = 'ip address show %s | grep "inet "' % DOVS_MGMT

    @staticmethod
    def __run(c, **kwargs):
        c = c.format(**kwargs)
        LOG.debug('Parse command: {}', c)
        atoms = shlex.split(c)
        g = [list(g) for k,g in groupby(atoms, lambda k: k == '|') if not k]
        runnable = reduce(lambda x,y: x|y, [local[x[0]][x[1:]] for x in g])
        LOG.debug('Execute command: {}', runnable)
        output = [line for line in runnable().splitlines() if line.strip()]
        if len(output)==1 and not kwargs.get('asList', False):
            output = output[0]
        return output if len(output) else ''

    @staticmethod
    def runWithNodeNs(func, **kwargs):
        nodeId = kwargs.pop('nodeId')
        pid = CMD.__run(CMD.NODE_PID, id=nodeId)
        CMD.__run(CMD.NS_MKDIR)
        CMD.__run(CMD.NS_LINK, pid=pid)
        try:
            func(nodens=pid, **kwargs)
        finally:
            CMD.__run(CMD.NS_UNLINK, pid=pid)

    @staticmethod
    def runWithTwoNodeNs(func, **kwargs):
        nodeId1 = kwargs.pop('node1')
        pid1 = CMD.__run(CMD.NODE_PID, id=nodeId1)
        CMD.__run(CMD.NS_MKDIR)
        CMD.__run(CMD.NS_LINK, pid=pid1)
        nodeId2 = kwargs.pop('node2')
        pid2 = CMD.__run(CMD.NODE_PID, id=nodeId2)
        if pid1 != pid2:
            CMD.__run(CMD.NS_MKDIR)
            CMD.__run(CMD.NS_LINK, pid=pid2)
        try:
            func(fromns=pid1, nodens=pid2, **kwargs)
            tap_dev = kwargs.pop('dev')
            CMD.__run(CMD.NS_IF_UP, ns=pid2, dev=tap_dev)
        finally:
            CMD.__run(CMD.NS_UNLINK, pid=pid1)
            if pid1 != pid2:
                CMD.__run(CMD.NS_UNLINK, pid=pid2)

    @staticmethod
    def mgmtHasDhcp():
        return ' '.join(CMD.__run(CMD.MGMT_DHCP)).find('dynamic') >= 0

    @staticmethod
    def startNsDhcp(nodens, id):
        CMD.__run(CMD.NS_DHCP_START, ns=nodens, id=id)

    @staticmethod
    def stopNsDhcp(nodens, id):
        CMD.__run(CMD.NS_DHCP_STOP, ns=nodens, id=id)

    @staticmethod
    def runNode(name):
        opts = '--name {}'.format(name) if name else ''
        id = CMD.__run(CMD.NODE_RUN, opts=opts)
        while not CMD.__run(CMD.NODE_STATUS, id=id).startswith('true'):
            time.sleep(1)
        while not CMD.__run(CMD.NODE_OVS_STATUS, id=id).startswith('EXITED'):
            time.sleep(1)
        CMD.__run(CMD.NODE_INT, id=id)
        if CMD.mgmtHasDhcp():
            CMD.runWithNodeNs(CMD.startNsDhcp, nodeId=id, id=id)
        return id

    @staticmethod
    def getNodes():
        return CMD.__run(CMD.NODE_NAME_LIST, asList=True)

    @staticmethod
    def existsNode(idOrName):
        nodeNames = CMD.__run(CMD.NODE_NAME_LIST, asList=True)
        if any(n for n in nodeNames if n==idOrName):
            return True
        nodeIds = CMD.__run(CMD.NODE_ID_LIST, asList=True)
        if any(n for n in nodeIds if n==idOrName):
            return True
        return False

    @staticmethod
    def stopAllNodes():
        dhcp = CMD.mgmtHasDhcp()
        try:
            nodes = CMD.__run(CMD.NODE_ID_LIST, asList=True)
        except:
            nodes = []
        for node in nodes:
            CMD.__run(CMD.NODE_OVS_STOP, id=node)
            if dhcp:
                CMD.runWithNodeNs(CMD.stopNsDhcp, nodeId=node, id=node)
            CMD.__run(CMD.NODE_STOP, id=node)
            CMD.__run(CMD.NODE_REMOVE, id=node)

    @staticmethod
    def delAllNs():
        try:
            nss = CMD.__run(CMD.NS_LIST, asList=True)
        except:
            nss = []
        for ns in nss:
            pids = CMD.__run(CMD.NS_PIDS, ns=ns, asList=True)
            for pid in pids:
                CMD.__run(CMD.KILL, pid=pid)
            CMD.__run(CMD.NS_DEL, ns=ns)

    @staticmethod
    def getNodeOvsId(node):
        return CMD.__run(CMD.NODE_OVS_UUID, id=node)

    @staticmethod
    def getNodeCidr(node):
        return CMD.__run(CMD.NODE_CIDR, id=node)

    @staticmethod
    def setNodeOvsOdl(node, ip):
        CMD.__run(CMD.NODE_OVS_SET_ODL, id=node, ip=ip)

    @staticmethod
    def getNodeOvsBridgeId(node):
        return CMD.__run(CMD.NODE_OVS_BRIDGE_DPID, id=node)[1:-1]

    @staticmethod
    def getNodeOvsPorts(id):
        return [x[1:-1]
                for x in CMD.__run(CMD.NODE_OVS_LIST_IF, id=id, asList=True)]

    @staticmethod
    def getNodeOvsPortInfo(id, dev):
        request = CMD.OVS_EXTRA_INFO
        request += ' ' + CMD.OVS_ATT_MAC
        request += ' ' + CMD.OVS_IFACE_ID
        request += ' ' + CMD.OVS_OFPORT
        response = CMD.__run(CMD.NODE_OVS_IF_GET, id=id, dev=dev, info=request)
        info = {
            'extra-info' : response[0][1:-1],
            'mac' : response[1][1:-1],
            'ifaceid' : response[2][1:-1],
            'of-port' : response[3]
        }
        return info

    @staticmethod
    def addNodeOvsBridge(node, node_ip, odl_ip):
        addBridge = True
        if odl_ip:
            CMD.__run(CMD.NODE_OVS_SET_MAN, id=node, ip=odl_ip)
            try:
                CMD.__run(CMD.NODE_OVS_WAIT_BRIDGE, id=node)
                LOG.debug('Integration bridge added by controller')
                addBridge = False
            except ProcessExecutionError as e:
                if e.retcode != 142:
                    raise e
                LOG.warn('Integration bridge NOT added by controller, adding it')
        if addBridge:
            CMD.__run(CMD.NODE_OVS_ADD_BRIDGE, id=node)
        if odl_ip and addBridge:
            CMD.__run(CMD.NODE_OVS_SET_ODL, id=node, ip=odl_ip)
        if node_ip:
            # netvirt uses other_config:local_ip
            ovsid = CMD.__run(CMD.NODE_OVS_UUID, id=node)
            CMD.__run(CMD.NODE_OVS_SET_BRIDGE_IP, id=node, ip=node_ip, ovsid=ovsid)

    @staticmethod
    def createNsToNode(name, node, dev, peer, ip):
        CMD.__run(CMD.NS_CREATE, ns=name)
        CMD.__run(CMD.NS_VETH_CREATE, ns=name, dev=dev, peer=peer)
        if ip:
            CMD.__run(CMD.NS_IF_SET_IP, ns=name, dev=dev, ip=ip)
        CMD.runWithNodeNs(CMD.moveDevToNs, nodeId=node, dev=peer, fromns=name)
        CMD.runWithNodeNs(CMD.setNsDevUp, nodeId=node, dev=peer)
        CMD.__run(CMD.NS_IF_UP, ns=name, dev=dev)
        CMD.__run(CMD.NS_IF_UP, ns=name, dev='lo')
        mac = CMD.__run(CMD.NS_IF_MAC, ns=name, dev=dev)
        return mac

    @staticmethod
    def setNsDefaultRoute(name, ip):
        CMD.__run(CMD.NS_DEFAULT_ROUTE, ns=name, ip=ip)

    @staticmethod
    def addDevToOvs(node, tap, rmac, neutron, extra):
        info = '{}={} {}={}'.format(CMD.OVS_IFACE_ID, neutron, CMD.OVS_ATT_MAC, rmac)
        CMD.__run(CMD.NODE_OVS_ADD_PORT, id=node, dev=tap, info=info, mac=rmac)
        info = CMD.OVS_EXTRA_INFO + '="{}"'.format(str(extra))
        CMD.__run(CMD.NODE_OVS_IF_SET, id=node, dev=tap, info=info)

    @staticmethod
    def delDevFromOvs(node, tap, rmac, neutron, extra):
        info = '{}={} {}={}'.format(CMD.OVS_IFACE_ID, neutron, CMD.OVS_ATT_MAC, rmac)
        CMD.__run(CMD.NODE_OVS_DEL_PORT, id=node, dev=tap, info=info, mac=rmac)
        #info = CMD.OVS_EXTRA_INFO + '="{}"'.format(str(extra))
        #CMD.__run(CMD.NODE_OVS_IF_DEL, id=node, dev=tap, info=info)

    @staticmethod
    def getInfoFromGuest(guestName):
        target_node = None
        target_port = None
        target_info = None
        nodes = CMD.getNodes()
        for node in nodes:
            for port in CMD.getNodeOvsPorts(node):
                info = CMD.getNodeOvsPortInfo(node, port)
                extra = literal_eval(info.get('extra-info', {}))
                if guestName == extra['ns']:
                    target_node = node
                    target_port = port
                    target_info = info
                    return (target_node, target_port, target_info)
        return (target_node, target_port, target_info)

    @staticmethod
    def moveDevToNs(dev, fromns, nodens):
        CMD.__run(CMD.NS_MOVE, dev=dev, ns=fromns, tons=nodens)

    @staticmethod
    def setNsDevUp(dev, nodens):
        CMD.__run(CMD.NS_IF_UP, ns=nodens, dev=dev)

    @staticmethod
    def getNsIp(name, dev):
        return CMD.__run(CMD.NS_IF_IP, ns=name, dev=dev)

    @staticmethod
    def getNsMac(name, dev):
        return CMD.__run(CMD.NS_IF_MAC, ns=name, dev=dev)

class Dovs(cli.Application):

    """

    dovs is a small companion utility to the docker-ovs image. It helps with:
    - running multiple independant OVS docker instances (nodes)
    - attaching network namespaces to such OVS instances (guests)
    - Setup overlay neutron networks with the help of a running ODL instance.

    dovs expects a host environment with:

    - a docker image named 'docker-ovs:yyang' that runs openvswitch
    - a docker network/bridge 'dovs-tun' to interconnect all the containers
    - a docker network/bridge 'dovs-mgmt' to reach the ODL controller
    - To setup overlay neutron networks, an ODL instance with genius and
      netvirt-openstack features must be enabled.

    This results in a very lightweight environment on which certain aspects
    of neutron networking can be simulated and others that depend on it,
    tested.


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


    Example 2: Networking of 2 compute nodes, 4 VMs and 2 isulated subnets.

      > dovs add-node --name compute-1 --odl 192.168.56.1
      > dovs add-node --name compute-2 --odl 192.168.56.1
      > dovs add-guest --name vm-1 --ip 10.0.0.1/24 --net subnet-A --node compute-1 --odl 192.168.56.1
      > dovs add-guest --name vm-2 --ip 10.0.0.1/24 --net subnet-B --node compute-1 --odl 192.168.56.1
      > dovs add-guest --name vm-3 --ip 10.0.0.2/24 --net subnet-A --node compute-2 --odl 192.168.56.1
      > dovs add-guest --name vm-4 --ip 10.0.0.2/24 --net subnet-B --node compute-2 --odl 192.168.56.1

      Starts two docker containers, each of them with an independant OVS.
      Creates 4 network namespaces each attached to an OVS bridge of the
      specified container and the ip configuration as provided.

      Everything is setup as a neutron agent would setup and as ODL would find
      it on an openstack deployment. Thus, this could be seen as simulating
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
    """

    PROGNAME = "Dovs"
    VERSION = "0.4.0"

    class Error(Exception):
        """ For errors raised from this module """
        pass

    debug = Flag(["-d", "--debug"], help = 'Print debug information')

    def main(self):
        format = '%(levelname)s[%(name)s]: %(message)s'
        level = logging.DEBUG if self.debug else logging.INFO
        logging.basicConfig(format=format, level=level)

@Dovs.subcommand('info')
class Info(cli.Application):
    """
    Show current dovs nodes & guests setup
    """

    _guest_name = None

    @cli.autoswitch(str)
    def guest_name(self, guest_name):
        """
        Get info only of one guest
        """
        self._guest_name = guest_name

    def main(self):
        nodes = CMD.getNodes()
        print('')
        if self.parent:
            print('Current dovs nodes & guests setup:')
        else:
            print('Outstanding dovs nodes & guests setup:')
        print('')
        if not nodes:
            print('Empty')
            print('')
            return
        lines = []
        for node in nodes:
            lines += ['Node: {}'.format(node)]
            for port in CMD.getNodeOvsPorts(node):
                info = CMD.getNodeOvsPortInfo(node, port)
                extra = literal_eval(info.get('extra-info', {}))
                if ((self._guest_name == None) or
                    (self._guest_name == extra['name'])):
                    lines += [' Guest: {}'.format(extra['name'])]
                    lines += ['  namespace: {}'.format(extra['ns'])]
                    lines += ['  ofport: {}'.format(info['of-port'])]
                    lines += ['  mac: {}'.format(info['mac'])]
                    lines += ['  ip: {}'.format(extra['ip'])]
                    lines += ['  ifaceid: {}'.format(info['ifaceid'])]
                    lines += ['  subnet: {}'.format(extra['net'])]
                    lines += ['  router: {}'.format(extra['router'])]
            print '\n'.join(lines)
            del lines[:]
        print '\n'
        print 'Remember to Use "dovs clean" to clean up'
        print '\n'

@Dovs.subcommand('add-node')
class AddNode(cli.Application):
    """
    Start an OVS docker instance
    """

    _name = None
    _odl = None
    _api = None

    @cli.autoswitch(str)
    def name(self, name):
        """
        name for the docker instance
        """
        self._name = name

    @cli.autoswitch(str)
    def odl(self, odl):
        """
        ip address of ODL controller
        """
        self._odl = odl
        self._api = ODL.getApi(odl, self.parent.debug if self.parent else False)

    @InfoIfNoException
    def main(self):
        name = self._name
        id = CMD.runNode(name)
        LOG.info('Started node {}', name if name else id)


        node_cidr = ip_interface(CMD.getNodeCidr(id))
        node_ip = str(node_cidr.ip)
        LOG.debug('')
        CMD.addNodeOvsBridge(id, node_ip, self._odl)


@Dovs.subcommand('add-guest')
class AddGuest(cli.Application):
    """
    Create a guest namespace connected to specified node
    """

    _name = None
    _ip = None
    _net = None
    _api = None
    _node = None
    _eth = 'eth0'
    _ns = None
    _router = None

    @cli.autoswitch(str)
    def name(self, name):
        """
        name of the guest
        """
        self._name = name
        self._ns = DOVS_PREFIX + name

    @cli.autoswitch(str, mandatory = True)
    def ip(self, ip):
        """
        ip interface configuration for the guest in CIDR notation
        """
        self._ip = unicode(ip)

    @cli.autoswitch(str, requires = ['--odl'])
    def net(self, net):
        """
        create a neutron port on the specified subnet for this guest
        """
        self._net = net

    @cli.autoswitch(str, requires = ['--odl', '--net'])
    def router(self, router):
        """
        create a neutron port on the specified router for this guests subnet
        """
        self._router = router

    @cli.autoswitch(str)
    def node(self, node):
        """
        attach the guest to the OVS instance running on the node
        """
        self._node = node

    @cli.autoswitch(str)
    def odl(self, odl):
        """
        ip address of ODL controller
        """
        self._api = ODL.getApi(odl, self.parent.debug if self.parent else False)


    @InfoIfNoException
    def main(self):
        api = self._api
        id = uuid.uuid4()
        shortId = str(id)[:11]
        node = self._node
        ns = self._ns
        net = self._net
        name = self._name if self._name else shortId
        ip = self._ip
        localdev = self._eth
        tapdev = 'tap' + shortId

        LOG.info('Start guest {}', name)

        if not CMD.existsNode(node):
            raise Dovs.Error('Node {} does not exist'.format(node))

        LOG.debug('Create guest namespace {} attached to node {}', ns, node)
        mac = CMD.createNsToNode(ns, node, localdev, tapdev, ip)

        router = None
        if api and net:
            network = self.addNetwork()
            subnet = self.addSubnet()
            router, rport = self.addRouter(subnet)
            port = self.addPort(id, name, mac)

        extra = {
            'name' : name,
            'ns' : ns,
            'ip' : ip,
            'net' : net if net else '',
            'router' : router['name'] if router else ''
        }
        LOG.debug('Adding guest tap device {} to OVS of node {}', tapdev, node)
        CMD.addDevToOvs(node, tapdev, mac, str(id), extra)

        if api and net:
            LOG.debug('Wait for {} to be set as parent of {}', tapdev, id)
            self.waitForParentInterface(str(id), tapdev)

    @retry(Dovs.Error, tries=60, delay=1)
    def waitForParentInterface(self, name, parent):
        if not ODL.isInterfaceParent(self._api, name, parent):
            raise Dovs.Error('{} was not set as parent interface of {}'.format(parent, name))


    def addNetwork(self):
        network = ODL.getNetwork(self._api, ODL.NETWORK)
        if network == None:
            LOG.debug('Add neutron network {}'.format(ODL.NETWORK))
            network = ODL.addNetwork(self._api, ODL.NETWORK_ID, ODL.NETWORK)
        return network

    def addSubnet(self):
        subnet = ODL.getSubnet(self._api, self._net)
        if subnet == None:
            network = ip_interface(self._ip).network
            prefix = str(network)
            gwip = str(network[-2])
            subnetId = str(uuid.uuid4())
            LOG.debug('Add neutron subnet {}'.format(self._net))
            subnet = ODL.addSubnet(self._api, subnetId, self._net, ODL.NETWORK_ID, prefix, gwip)
        elif not ip_interface(self._ip).network == ip_network(subnet['cidr']):
            raise Dovs.Error(
                'Subnet {} exists with address {} incompatible to guest address {}'
                    .format(self._net, subnet['cidr'], self._ip))
        return subnet

    def addRouter(self, subnet):
        routerPort = ODL.getPortOfSubnetRouter(self._api, subnet['uuid'])
        router = (ODL.getRouter(self._api, routerPort['device-id']) if routerPort
            else ODL.getRouter(self._api, self._router) if self._router
            else None)

        if routerPort and not router:
            raise Dovs.Error('Internal error: could not find router for port {}'
                .format(routerPort['uuid']))

        if routerPort and not routerPort['device-id'] == router['uuid']:
            raise Dovs.Error('Subnet {} not reachable on router {}, use {}'
                .format(subnet['name'], router['uuid'], routerPort['device-id']))

        if router and self._router and not router['name'] == self._router:
            raise Dovs.Error('Subnet {} not reachable on router {}, use {}'
                .format(subnet['name'], self._router, router['name']))

        if routerPort:
            LOG.debug('Subnet attached to existing router {}', router['name'])
            LOG.debug('Add default route on namespace {}', self._ns)
            ip = routerPort['fixed-ips'][0]['ip-address']
            CMD.setNsDefaultRoute(self._ns, ip)
            return router, routerPort

        if not self._router:
            LOG.debug('Not adding router as it was not specified')
            return None, None

        if not router:
            LOG.debug('Add neutron router {}'.format(self._router))
            routerId = str(uuid.uuid4())
            router = ODL.addRouter(self._api, routerId, self._router)

        routerId = router['uuid']
        portName = self._router + '-' + self._net
        portId = str(uuid.uuid4())
        ip = subnet['gateway-ip']
        mac = "52:54:%02x:%02x:%02x:%02x" % (
            random.randint(2, 12),
            random.randint(0, 255),
            random.randint(0, 255),
            random.randint(0, 255)
        )
        owner = 'network:router_interface'
        LOG.debug('Add neutron router port for subnet {}', subnet['name'])
        port = ODL.addPort(
            self._api,
            portId,
            portName,
            ODL.NETWORK_ID,
            subnet['uuid'],
            mac,
            ip,
            owner,
            routerId)
        LOG.debug('Add default route on namespace {}', self._ns)
        CMD.setNsDefaultRoute(self._ns, ip)
        return router, port

    def addPort(self, id, name, mac):
        node = self._node
        api = self._api
        net = self._net
        eth = self._eth
        ip = self._ip
        ovsId = CMD.getNodeOvsId(node)
        network = ODL.getNetwork(api, ODL.NETWORK)
        subnet = ODL.getSubnet(api, net)
        prefix = subnet['cidr']

        if not subnet:
            raise Dovs.Error('subnet {} does not exist'.format(net))

        if not ip_interface(ip).network == ip_network(prefix):
            raise Dovs.Error('Invalid ip {} for subnet {}'.format(ip, prefix))

        netId = str(subnet['uuid'])
        owner = 'compute'
        LOG.debug('Configure neutron port {} on subnet {}', id, net)
        ODL.addPort(
            api,
            str(id),
            name,
            ODL.NETWORK_ID,
            netId,
            mac,
            str(ip_interface(ip).ip),
            owner,
            str(id))

        return netId

@Dovs.subcommand('move-guest')
class MoveGuest(cli.Application):
    """
    Move a guest from the current location node to another selected node
    """

    _dstnode = None
    _guest = None


    @cli.autoswitch(str)
    def name_guest(self, guest):
        """
        Name of the guest to relocate
        """
        self._guest = guest

    @cli.autoswitch(str)
    def name_dstnode(self, dstnode):
        """
        Name of the destination node where the guest will be re-located
        """
        self._dstnode = dstnode

    @InfoIfNoException
    def main(self):
        dstnode = self._dstnode
        guest = self._guest
        LOG.info('Moving Guest {} to node {}', guest, dstnode)
        node, port, info = CMD.getInfoFromGuest(guest)
        mac = info['mac']
        neutron_id = info['ifaceid']
        extra = info['extra-info']

        CMD.delDevFromOvs(node, port, mac, neutron_id, extra)
        CMD.runWithTwoNodeNs(CMD.moveDevToNs, node1=node, dev=port, node2=dstnode)
        CMD.addDevToOvs(dstnode, port, mac, neutron_id, extra)


@Dovs.subcommand('spawn')
class Spawn(cli.Application):

    """ Setup nodes, guests and overlay networks in a single command """

    _nodes = 0
    _guests = 0
    _nets = 0
    _routed= False
    _odl = None

    NODE_PREFIX = 'node-{}'
    GUEST_PREFIX = '-guest-{}'
    NET_PREFIX = 'net-{}'
    NET_CIDR = u'10.0.0.0/16'

    @cli.autoswitch(int, mandatory=True)
    def nodes(self, nodes):
        """
        number of nodes to spawn
        """
        self._nodes = nodes

    @cli.autoswitch(int, requires = ['--nodes'])
    def guests(self, guests):
        """
        number of guests per node to spawn
        """
        self._guests = guests

    @cli.autoswitch(int, requires = ['--odl'])
    def nets(self, nets):
        """
        number of subnets to spawn
        """
        self._nets = nets

    @cli.autoswitch()
    def routed(self):
        """
        interconnect all networks through a router
        """
        self._routed = True

    @cli.autoswitch(str)
    def odl(self, odl):
        """
        ip address of ODL controller
        """
        self._odl = odl

    @InfoIfNoException
    def main(self):
        nodes = self._nodes
        guests = self._guests
        nets = self._nets if self._nets else 1
        routed = self._routed
        LOG.info('Spawn {} nodes, with {} guests each spread among {} nets',
                 nodes, guests, nets)
        netsinfo=[]
        supernet = ip_network(Spawn.NET_CIDR)
        subnets = list(supernet.subnets(new_prefix=24))[0:nets]
        for net in range(1, nets + 1):
            net_name = DOVS_PREFIX + Spawn.NET_PREFIX.format(net)
            network = subnets[net % len(subnets)] if routed else subnets[0]
            nethostsit = network.hosts()
            netsinfo.append((net_name, network, nethostsit))

        cguest = 0
        for node in range(1, nodes + 1):
            node_name = Spawn.NODE_PREFIX.format(node)
            AddNode.invoke(name=DOVS_PREFIX + node_name, odl=self._odl)
            for guest in range(1, guests + 1):
                guest_name = node_name + Spawn.GUEST_PREFIX.format(guest)
                ip = str(netsinfo[cguest % nets][2].next())
                ip += '/' + str(netsinfo[cguest % nets][1].prefixlen)
                net = netsinfo[cguest % nets][0] if self._nets else None
                AddGuest.invoke(
                  name=guest_name,
                  node=DOVS_PREFIX + node_name,
                  ip=ip,
                  net=net,
                  router='router1' if routed else None,
                  odl=self._odl)
                cguest += 1


@Dovs.subcommand('clean')
class Clean(cli.Application):

    """ Removes OVS docker instances and guest namespaces """

    _api = None

    @cli.autoswitch(str)
    def odl(self, odl):
        """
        ip address of ODL controller.
        """
        self._api = ODL.getApi(odl, self.parent.debug if self.parent else False)

    @InfoIfNoException
    def main(self):
        LOG.info('Will remove any modification done by the utility')
        LOG.debug('Stop nodes and remove namespaces')
        CMD.delAllNs()
        CMD.stopAllNodes()

        if not self._api:
            return

        LOG.debug('Delete transport-zone, network, subnets and ports')
        ODL.delAllPorts(self._api)
        ODL.delAllSubnets(self._api)
        ODL.delAllRouters(self._api)
        ODL.delAllNetworks(self._api)
        ODL.delTz(self._api, ODL.TZ)

@Dovs.subcommand('sfc-config')
class SfcConfig(cli.Application):
    """
    - Create Networking (compute nodes and guests)
    - Set Service Function Chaining (SFC) Configuration in Opendaylight Controller (ODL)
       service-function-chains (SFC), service-function-paths (SFP),
       service-function-forwarders (SFF), service-functions (SF)
    - Create/remove Rendered Service Path (RSP) in ODL

    Example:
     sudo dovs sfc-config --chains "[['client1', 'firewall, napt44', 'server1'], ['client2', 'napt44', 'server2']]" --odl 172.28.128.4 --different-subnets
       # It creates 6 nodes ('client1', 'firewall', 'napt44', 'server1', 'client2' and 'server2'
       # It creates 6 guests, one per node ('dovs-client1', 'dovs-firewall', etc.) each one in its own subnet ("10.0.1.x", "10.0.2.x" ...)
       # It configures in ODL:
       #  2 symmetric SFC ("SFC1" with 'firewall, napt44' and "SFC2" with 'napt44')
       #  2 SFP ("SFP1" associated to "SFC1" and "SFP2" associated to "SFC2"),
       #  1 SFF ("sfflogical1")
       #  2 SF ("firewall" and "napt44") with its corresponding "interface-name" neutron port uuid
     sudo dovs sfc-config --create-rsp-from-id 1 --odl 172.28.128.4
       # It creates symmetric RSP "RSP1" associated to "SFP1"
     sudo dovs sfc-config --create-rsp-from-id 2 --odl 172.28.128.4
       # It creates symmetric RSP "RSP2" associated to "SFP2"
     sudo dovs sfc-config --delete-rsp-from-id 1 --odl 172.28.128.4
       # It removes RSP "RSP1"
     sudo dovs sfc-config --delete-rsp-from-id 2 --odl 172.28.128.4
       # It removes RSP "RSP2"
    """
    _odl = None
    _chains = []
    _cfg_from_net = False
    _only_config = False
    _different_subnets = False
    _create_rsp_from_id = None
    _delete_rsp_from_id = None
    _create_classifier_from_id = None
    _classifier_port = None
    _all_in_one_node = False

    @cli.autoswitch(str)
    def odl(self, odl):
        """
        IP address of ODL controller.
        """
        self._odl = odl

    @staticmethod
    def createListChains(self, input_chains):
        list_chains = []
        chains = ast.literal_eval(input_chains)
        for chain in chains:
            list_chain_elems = []
            if (len(chain) != 3):
                raise ValueError('Incorrect number of chain parameters')
            else:
                for elem_chain in chain:
                    elem_item_split = [n.strip() for n in elem_chain.split(',')]
                    if (len(elem_item_split) < 1):
                        raise ValueError('Incorrect number of elements in a chain')
                    list_chain_elems.append(elem_item_split)
            list_chains.append(list_chain_elems)
        return list_chains

    @cli.autoswitch(str, requires = ['--odl'])
    def chains(self, chains):
        """
        Create Networking and set SFC configuration in ODL, base on the specify chain
        """
        # print ("chains:" + chains)
        """ Example of service-function-type
         firewall,
         dpi (Deep Packet Inspection),
         napt44 (Traditional NAT [RFC3022]),
         qos (Quality of Service),
         ids (Intrusion Detection System),
         http-header-enrichment
        """
        try:
            self._chains = self.createListChains(self, chains)
        except (ValueError, SyntaxError) as exceptError:
            LOG.error("""Chains format is not correct ({}),
            example: --chains \"[[\'client1\', \'firewall, napt44\', \'server1'], [\'client2\', \'napt44\', \'server2\']]\"""",
                exceptError)
            os._exit(1)

    @cli.autoswitch(requires = ['--odl'])
    def cfg_from_net(self):
        """
        Create SFC configuration base on existing nodes
        """
        self._cfg_from_net = True

    @cli.autoswitch(requires = ['--chains'])
    def different_subnets(self):
        """
        When creating guests associated them to different subnets
        """
        self._different_subnets = True

    @cli.autoswitch(requires = ['--chains'])
    def only_config(self):
        """
        Only Configure the SFC (network topology has to be created before)
        """
        self._only_config = True

    @cli.autoswitch(int, requires = ['--odl'])
    def create_rsp_from_id(self, chain_id):
        """
        Create a RSP "RSP<CHAIN_ID>"
        """
        self._create_rsp_from_id = chain_id

    @cli.autoswitch(int, requires = ['--odl'])
    def delete_rsp_from_id(self, chain_id):
        """
        Delete a RSP "RSP<CHAIN_ID>"
        """
        self._delete_rsp_from_id = chain_id

    @cli.autoswitch(int, requires = ['--odl'])
    def create_classifer_from_id(self, chain_id):
        """
        Create a classifier rule
        """
        self._create_classifier_from_id = chain_id

    @cli.autoswitch(int, requires = ['--create-classifer-from-id'])
    def classifier_port(self, port):
        """
        Define the destination port
        """
        self._classifier_port = port

    @cli.autoswitch()
    def allinonenode(self):
        """
        Deploy all guests in one node
        """
        self._all_in_one_node = True

    @staticmethod
    def getChainNameFromId(chainId):
        return "SFC" + str(chainId)

    @staticmethod
    def getListChainNames(self):
        chainNames = []
        chains = self._chains
        chainId = 0
        for chain in chains:
            chainId += 1
            chainName = self.getChainNameFromId(chainId)
            chainNames.append(chainName)
        return chainNames

    @staticmethod
    def getListDifferentChainTypes(self):
        chainTypes = []
        for sfc in self._chains:
            chain = sfc[1]
            for c in chain:
                chainTypes.append(c)
        chainTypes = set(chainTypes)
        return chainTypes

    @staticmethod
    def getListDifferentGuestNames(self):
        set_guest_nodes = Set([])
        for sfc in self._chains:
            for guests in sfc:
                for guest in guests:
                    set_guest_nodes.add(guest)
        return set_guest_nodes

    @staticmethod
    def createNetworkFromNames(self, names):
        net_infos=[]
        network = ip_network(Spawn.NET_CIDR)
        net_name="shared_subnet"
        ip_value='10.0.0.1/24'
        router_value='router1'
        indexItem = 0
        if self._all_in_one_node:
            ovs_name="SFF-classifier"
            AddNode.invoke(name=ovs_name, odl=self._odl)
            for nameItem in names:
                indexItem += 1
                if (self._different_subnets):
                    net_name = nameItem
                    ip_value='10.0.' + str(indexItem) + '.1/24'
                else:
                    ip_value='10.0.0.' + str(indexItem) + '/24'

                AddGuest.invoke(
                    name=nameItem,
                    node=ovs_name,
                    net=net_name,
                    ip=ip_value,
                    router=router_value,
                    odl=self._odl)
        else:
            for nameItem in names:
                indexItem += 1
                if (self._different_subnets):
                    net_name = nameItem
                    ip_value='10.0.' + str(indexItem) + '.1/24'
                else:
                    ip_value='10.0.0.' + str(indexItem) + '/24'

                AddNode.invoke(name=nameItem, odl=self._odl)
                AddGuest.invoke(
                    name=nameItem,
                    node=nameItem,
                    net=net_name,
                    ip=ip_value,
                    router=router_value,
                    odl=self._odl)

    @staticmethod
    def createNetworkFromGuestNames(self):
        guest_names = self.getListDifferentGuestNames(self)
        LOG.info('create Network from Guest Names {}', guest_names)
        self.createNetworkFromNames(self, guest_names)

    @staticmethod
    def getSffsJson(sff):
        stringServiceFunctionForwardersJson = """
{
    "service-function-forwarders": {
        "service-function-forwarder": [
           {
                "name": """ + '"' + sff + '"' + """
            }
        ]
    }
}
        """
        return stringServiceFunctionForwardersJson

    @staticmethod
    def getSfsJson():
        stringServiceFunctionsJson = """
{
  "service-functions": {
    "service-function": [
    ]
  }
}
        """
        return stringServiceFunctionsJson

    @staticmethod
    def getSfJson(sf, sff, ifName):
        stringServiceFunctionJson = """
      {
        "name": """ + '"' + sf + '"' + """,
        "type": """ + '"' + sf + '"' + """,
        "sf-data-plane-locator": [
          {
            "name": """ + '"' + sf + "-" + sff + "-" + "dpl" + '"' + """,
            "transport": "service-locator:mac",
            "interface-name": """ + '"' + ifName + '"' + """ ,
            "service-function-forwarder": """ + '"' + sff + '"' + """
          }
        ]
      }
        """
        return stringServiceFunctionJson

    @staticmethod
    def getSfpsJson():
        stringServiceFunctionPaths = """
{
  "service-function-paths": {
    "service-function-path": [
    ]
  }
}
        """
        return stringServiceFunctionPaths

    @staticmethod
    def getSfpJson(sfpName, chainName):
        stringServiceFunctionPath = """
      {
        "name": """ + '"' + sfpName + '"' + """,
        "service-chain-name": """ + '"' + chainName + '"' + """,
        "symmetric": true
      }
        """
        return stringServiceFunctionPath


    @staticmethod
    def getSfcsJson():
        stringServiceFunctionChains = """
{
  "service-function-chains": {
    "service-function-chain": [
    ]
  }
}
        """
        return stringServiceFunctionChains

    @staticmethod
    def getSfcChainJson(chainName):
        stringServiceFunctionChain = """
      {
        "name": """ + '"' + chainName + '"' + """,
        "symmetric": "true",
        "sfc-service-function": [
        ]
      }
        """
        return stringServiceFunctionChain

    @staticmethod
    def getSfcElemChainJson(chainType, chainOrder):
        stringServiceFunctionElemChain = """
          {
            "name": """ + '"' + chainType + '-' + str(chainOrder) + '"' + """,
            "type": """ + '"' + chainType + '"' + """,
            "order" : """ + '"' + str(chainOrder) + '"' + """
          }
        """
        return stringServiceFunctionElemChain

    @staticmethod
    def getCreateRspChainJson(chainId):
        stringRenderedServicePath = """
{
    "input": {
        "name": """ + '"' + 'RSP' + str(chainId) + '"' + """,
        "parent-service-function-path": """ + '"' + 'SFP' + str(chainId) + '"' + """,
        "symmetric": "true"
    }
}
        """
        return stringRenderedServicePath

    @staticmethod
    def getDeleteRspChainJson(chainId):
        stringRenderedServicePath = """
{
    "input": {
        "name": """ + '"' + 'RSP' + str(chainId) + '"' + """
    }
}
        """
        return stringRenderedServicePath

    @staticmethod
    def getDeleteReverseRspChainJson(chainId):
        stringRenderedServicePath = """
{
    "input": {
        "name": """ + '"' + 'RSP' + str(chainId) + '-Reverse"' + """
    }
}
        """
        return stringRenderedServicePath


    @staticmethod
    def getCreateClassifierJson(chainId, port):
        stringClassifier = """
{
    "acl": [ {
        "acl-name": """ + '"' + 'ACL' + str(chainId) + '"' + """,
        "acl-type": "ietf-access-control-list:ipv4-acl",
        "access-list-entries":
        {
             "ace": [ {
                  "rule-name": """ + '"' + 'ACE' + str(chainId) + '"' + """,
                  "actions": {
                       "netvirt-sfc-acl:rsp-name": "RSP""" + str(chainId) + """"
                   },
                  "matches": {
                       "network-uuid" : "177bef73-514e-4922-990f-d7aba0f3b0f4",
                       "source-ipv4-network": "10.0.0.0/24",
                       "protocol": "6",
                       "source-port-range": { "lower-port": 0 },
                       "destination-port-range": { "lower-port": """ + str(port) + """ }
                   }
              } ]
        }
    } ]
}
        """
        return stringClassifier

    @staticmethod
    def printSfConfig(strJson):
        try:
            sfJson = json.loads(strJson)
        except TypeError as exceptTypeError:
            LOG.error('Nothing to process ({})',
                exceptTypeError)
            return
        sfString = 'SFs: ['
        for i in range(0, len(sfJson[u'service-functions'][u'service-function'])):
            sfName = sfJson[u'service-functions'][u'service-function'][i][u'name']
            sfString += '[' + sfName + ']'
        LOG.info(sfString + ']')

    @staticmethod
    def generateSfConfig(self, sff):
        returnValue = ''
        if self._chains != []:
            returnValue = self.generateSfConfigFromChains(self, sff)
        elif self._cfg_from_net == True:
            returnValue = self.generateSfConfigFromNodes(self, sff)
        return returnValue

    @staticmethod
    def generateSfConfigFromNodes(self, sff):
        sfJson = json.loads(self.getSfsJson())
        nodes = CMD.getNodes()
        if not nodes:
            return
        LOG.info('create SF Config from Nodes {}', nodes)
        for node in nodes:
            for port in CMD.getNodeOvsPorts(node):
                info = CMD.getNodeOvsPortInfo(node, port)
                extra = literal_eval(info.get('extra-info', {}))
                sfJson[u'service-functions'][u'service-function'].append(
                    json.loads(self.getSfJson(extra['name'],
                    sff,
                    info['ifaceid'])))
        return json.dumps(sfJson, indent=4, sort_keys=True)

    @staticmethod
    def getNeutronIdFromGuest(self, guestName):
        neutronId = ''
        nodes = CMD.getNodes()
        for node in nodes:
            for port in CMD.getNodeOvsPorts(node):
                info = CMD.getNodeOvsPortInfo(node, port)
                extra = literal_eval(info.get('extra-info', {}))
                if guestName == extra['name']:
                    neutronId = info['ifaceid']
        return neutronId

    @staticmethod
    def generateSfConfigFromChains(self, sff):
        chainTypes = self.getListDifferentChainTypes(self)
        LOG.info('create SF Config from Chain Types {}', chainTypes)
        sfJson = json.loads(self.getSfsJson())
        for name in chainTypes:
            sfJson[u'service-functions'][u'service-function'].append(
                json.loads(self.getSfJson(name,
                sff,
                self.getNeutronIdFromGuest(self, name))))
        return json.dumps(sfJson, indent=4, sort_keys=True)

    @staticmethod
    def generateSfpConfig(self):
        sfpJson = json.loads(self.getSfpsJson())
        chainNames = self.getListChainNames(self)
        chainId = 0
        for chainName in chainNames:
            chainId += 1
            sfpName = "SFP" + str(chainId)
            sfpJson[u'service-function-paths'][u'service-function-path'].append(
                json.loads(self.getSfpJson(sfpName, chainName)))

        return json.dumps(sfpJson, indent=4, sort_keys=True)

    @staticmethod
    def generateSfChainConfig(self):
        LOG.info('Generating SFC configuration for chains'
            + str(self._chains))
        sfcsJson = json.loads(self.getSfcsJson())

        chainId = 0
        for sfc in self._chains:
            chainId += 1
            chainName = self.getChainNameFromId(chainId)
            sfcChainJson = json.loads(self.getSfcChainJson(chainName))
            chainOrder = 0
            # sfc[0]-> clients; [1]-> chain; [2]-> server
            chain = sfc[1]
            for chainType in chain:
                sfcChainJson[u'sfc-service-function'].append(
                    json.loads(self.getSfcElemChainJson(
                        chainType, chainOrder)))
                chainOrder += 1
            sfcsJson[u'service-function-chains'][u'service-function-chain'].append(
                sfcChainJson)
        return json.dumps(sfcsJson, indent=4, sort_keys=True)

    @staticmethod
    def sendOdlConfig(self, type_request, odlIpAddress, restconf, payload):
        if payload != '':
            url = "http://" + odlIpAddress + ":8181" + restconf
            headers = {
                'authorization': "Basic YWRtaW46YWRtaW4=",
                'content-type': "application/json",
                'cache-control': "no-cache",
                }

            print(url)
            print(headers)
            print(payload)
            response = requests.request(type_request, url, data=payload, headers=headers)
            print(response.text)

    @staticmethod
    def sendOdlConfigWithoutPayload(self, type_request, odlIpAddress, restconf):
        url = "http://" + odlIpAddress + ":8181" + restconf
        headers = {
            'authorization': "Basic YWRtaW46YWRtaW4=",
            'content-type': "application/json",
            'cache-control': "no-cache",
            }

        print(url)
        print(headers)
        response = requests.request(type_request, url, headers=headers)
        print(response.text)

    @staticmethod
    def sendPutOdlConfig(self, odlIpAddress, restconf, payload):
        self.sendOdlConfig(self, "PUT", odlIpAddress, restconf, payload)

    @staticmethod
    def sendPostOdlConfig(self, odlIpAddress, restconf, payload):
        self.sendOdlConfig(self, "POST", odlIpAddress, restconf, payload)

    @staticmethod
    def sendDeleteOdlConfig(self, odlIpAddress, restconf):
        self.sendOdlConfigWithoutPayload(self, "DELETE", odlIpAddress, restconf)
        time.sleep(0.3)

    @staticmethod
    def putSfConfig(self, odlIpAddress, payload):
        if payload != '':
            restconf = "/restconf/config/service-function:service-functions/"
            self.sendDeleteOdlConfig(self, odlIpAddress, restconf)
            self.sendPutOdlConfig(self, odlIpAddress, restconf, payload)
            # Check SFs names
            self.printSfConfig(payload)

    @staticmethod
    def deleteSfConfig(self, odlIpAddress, sf_name):
        # Example DELETE service function napt44
        # http://{{ip}}:8181/restconf/config/service-function:service-functions/service-function/napt44
        if sf_name != '':
            restconf = "/restconf/config/service-function:service-functions/service-function/" + sf_name
            self.sendDeleteOdlConfig(self, odlIpAddress, restconf)

    @staticmethod
    def deleteAllSfInChains(self):
        chainTypes = self.getListDifferentChainTypes(self)
        for name in chainTypes:
            self.deleteSfConfig(self, self._odl, name)

    @staticmethod
    def putSfpConfig(self, odlIpAddress, payload):
        restconf = "/restconf/config/service-function-path:service-function-paths/"
        self.sendDeleteOdlConfig(self, odlIpAddress, restconf)
        self.sendPutOdlConfig(self, odlIpAddress, restconf, payload)

    @staticmethod
    def putSfcConfig(self, odlIpAddress, payload):
        restconf = "/restconf/config/service-function-chain:service-function-chains/"
        self.sendDeleteOdlConfig(self, odlIpAddress, restconf)
        self.sendPutOdlConfig(self, odlIpAddress, restconf, payload)

    @staticmethod
    def putSffConfig(self, odlIpAddress, payload):
        restconf = "/restconf/config/service-function-forwarder:service-function-forwarders/"
        self.sendPutOdlConfig(self, odlIpAddress, restconf, payload)

    @staticmethod
    def sendCreateRspConfig(self, odlIpAddress, payload):
        restconf = "/restconf/operations/rendered-service-path:create-rendered-path/"
        self.sendPostOdlConfig(self, odlIpAddress, restconf, payload)

    @staticmethod
    def sendDeleteRspConfig(self, odlIpAddress, payload):
        restconf = "/restconf/operations/rendered-service-path:delete-rendered-path/"
        self.sendPostOdlConfig(self, odlIpAddress, restconf, payload)

    @staticmethod
    def sendCreateClassifierConfig(self, odlIpAddress, rsp_id, payload):
        restconf = ("/restconf/config/ietf-access-control-list:access-lists/"
                    "acl/ietf-access-control-list:ipv4-acl/ACL" + str(rsp_id))
        self.sendPutOdlConfig(self, odlIpAddress, restconf, payload)

    def main(self):
        print(sys.argv[0] + ' arguments: ' + str(sys.argv[1:])) + '\n'

        if self._chains != []:
            if (not self._only_config):
                self.deleteAllSfInChains(self)
                time.sleep(1)
                self.createNetworkFromGuestNames(self)
            self.putSfcConfig(self,
                self._odl, self.generateSfChainConfig(self))
            self.putSfpConfig(self,
                self._odl, self.generateSfpConfig(self))
            sff = "sfflogical1"
            # Configure service-function-forwarders (only one sff)
            self.putSffConfig(self,
                self._odl, self.getSffsJson(sff))
            self.putSfConfig(self,
                self._odl, self.generateSfConfig(self, sff))

        if self._create_rsp_from_id != None:
            self.sendCreateRspConfig(self,
                self._odl, self.getCreateRspChainJson(
                    self._create_rsp_from_id))
        if self._delete_rsp_from_id != None:
            self.sendDeleteRspConfig(self,
                self._odl, self.getDeleteRspChainJson(
                    self._delete_rsp_from_id))
            self.sendDeleteRspConfig(self,
                self._odl, self.getDeleteReverseRspChainJson(
                    self._delete_rsp_from_id))

        if self._create_classifier_from_id != None:
            self.sendCreateClassifierConfig(self,
                self._odl, self._create_classifier_from_id,
                self.getCreateClassifierJson(
                    self._create_classifier_from_id,
                    self._classifier_port))

        Info.invoke()


def main():
    try:
        if os.geteuid() != 0:
            raise Dovs.Error('This tool can only be run as root')
        Dovs.run()
    except ProcessExecutionError as e:
        LOG.error('Command execution failed')
        LOG.error('  command: {}', ' '.join(e.argv))
        LOG.error('  exit status: {}', e.retcode)
        LOG.error('  stderr: {}', e.stderr)
    except HTTPError as e:
        status = e.response.status_code
        reason = e.response.reason
        try:
            d = e.response.json()
            d = d if d else {}
            d = d.get('errors', {}).get('error', [{}])[0].get('error-message','')
            LOG.error('REST invocation failed')
            LOG.error('  status code: {} {}', status, reason)
            LOG.error('  detail: {}', d)
        except ValueError as exceptValueError:
            LOG.error('{} ({} {})',
                exceptValueError, e.response.status_code, e.response.reason)
    except Dovs.Error as e:
        LOG.error(e.args[0])

if __name__ == '__main__':
        main()

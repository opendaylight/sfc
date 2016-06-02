#! /usr/bin/env python

__author__ = "Brady Johnson"
__copyright__ = "Copyright(c) 2015, Ericsson, Inc."
__license__ = "Eclipse Public License v1.0"
__version__ = "0.2"
__email__ = "brady.allen.johnson@ericsson.com"
__status__ = "demo code"

#
# This script is intended to only work with sfc-openflow-renderer_mininet.py
#
# This script will do the following for clients:
#    add a client to RSP mapping in GW1
#    change a client to RSP mapping in GW1
#    show the client to RSP mapping in GW1
#    list all clients
#

import subprocess
import tempfile
from string import Template
import argparse

#
# Simple class that holds all of the client configurable parameters
#
class Context(object):
    def __init__(self):
        # Just setting default values
        self.client_name = ''
        self.rsp_id = ''
        self.do_set = False
        self.do_list = False
        self.do_list_all = False
        self.do_rsp_list = False
        self.clients = [] # List of ClientInfo instances
        self.vlan_to_rsp_id_mappings = {}
        self.rsp_id_to_vlan_mappings = {}

class ClientInfo(object):
    def __init__(self, name='', ip='', rsp_id='', vlan=''):
        self.name    =  name
        self.ip      =  ip
        self.rsp_id  =  rsp_id
        self.vlan    =  vlan
    def __str__(self):
        return '%s => Ip [%s] rsp_id [%s] vlan [%s]' % (self.name, self.ip, self.rsp_id, self.vlan)
    def __repr__(self):
        return self.__str__(self)

MININET_DUMP_FILE = '/tmp/mininet_dump.txt'
MININET_NET_FILE  = '/tmp/mininet_net.txt'

#
# Get the command line args
#
def get_cmd_line_args(context):

    opts = argparse.ArgumentParser()

    # Client IP
    opts.add_argument('--client', '-C',
                  default=context.client_name,
                  dest='client_name',
                  help='Set the client name')

    # RSP ID
    opts.add_argument('--rsp-id', '-I',
                  default=context.rsp_id,
                  dest='rsp_id',
                  help='Set the RSP ID to use for the specified client')

    # List clients
    opts.add_argument('--list', '-L',
                  dest='do_list',
                  action='store_const',
                  default=context.do_list,
                  const=True,
                  help='List one (if client specified) or all clients')

    # List RSP to vlan mappings
    opts.add_argument('--rsps', '-R',
                  dest='do_rsp_list',
                  action='store_const',
                  default=context.do_rsp_list,
                  const=True,
                  help='List the rsp_id to vlan mappings')

    # TODO add an option to delete the client from the GW

    args = opts.parse_args()

    context.client_name =  args.client_name
    context.rsp_id      =  args.rsp_id
    context.do_rsp_list =  args.do_rsp_list

    # Analyze the arguments
    if args.do_list:
        if len(args.client_name) == 0:
            context.do_list_all = True
            context.do_list = False
        else:
            context.do_list_all = False
            context.do_list = True

    if not args.do_rsp_list and not args.do_list:
        if len(args.client_name) < 1 or len(args.rsp_id) < 1:
            print 'ERROR: both the client name and rsp id must be present to set them in the GW'
            return False
        context.do_set = True

    return True

#
# Internal method to execute a system command and return the return_code and stdout as a str
#
def _execute_command(arg_list_str):
    return_code = 0
    stdout_str = ''

    with tempfile.TemporaryFile(mode='w+b') as f:
        try:
            return_code=subprocess.call(arg_list_str, stdout=f, shell=True)
        except OSError as e:
            print 'OS Error [%d] \"%s\", with command \"%s\"' % (e.errno, e.strerror, ' '.join(arg_list_str))
            return (-1, stdout_str)
        except:
            print 'Unexpected error with command: \"%s\"' % (' '.join(arg_list_str))
            return (-1, stdout_str)

        #print '\"%s\", rc=%d' % (' '.join(arg_list_str), return_code)

        if return_code != 0:
            print 'Non-zero return code [%d] for command: \"%s\"' % (return_code, ' '.join(arg_list_str))
            return (return_code, stdout_str)

        # Flush the tempfile and go to the beginning of it
        f.flush()
        f.seek(0, 0)

        stdout_str = f.read()

    return (return_code, stdout_str.strip().split('\n'))

def _find_client_by_ip(client_info_list, client_ip):
    for client in client_info_list:
        if client.ip == client_ip:
            return client
    return None

def _find_client_by_name(client_info_list, client_name):
    for client in client_info_list:
        if client.name == client_name:
            return client
    return None

def _find_rsp_id(context, vlan):
    return context.vlan_to_rsp_id_mappings.get(vlan, '')

def _find_vlan(context, rsp_id):
    return context.rsp_id_to_vlan_mappings.get(rsp_id, '')

def _get_sffs():
    (rc, sffs_str) = _execute_command('sudo ovs-vsctl list-br | grep sff | grep -v node')
    if rc != 0:
        print 'Error get_sffs RC %d' % rc
        return []

    return sffs_str

def _get_rsp_and_vlan_mappings(context):
    all_sffs = _get_sffs()
    for sff in all_sffs:
        (rc, vlan_flows) = _execute_command('sudo ovs-ofctl -O OpenFlow13 dump-flows %s table=1 | egrep \"strip_vlan|pop_vlan\" | awk \'{print $6, $7}\''%sff)

        if rc != 0:
            print 'Error _get_rsp_and_vlan_mappings RC %d' % rc

        # Expecting: priority=350,dl_vlan=101 actions=pop_vlan,write_metadata:0x1/0xffff,goto_table:3
        for flow in vlan_flows:
            if len(flow) < 1:
                continue

            space_fields = flow.split()
            if 'ip' in space_fields[0]:
                # The matches that use 'ip' are for the SFs and not needed here
                continue

            vlan = ''
            rsp_id = ''

            # Get the VLAN
            match_fields = space_fields[0].split(',')
            if len(match_fields) < 2:
                # Skip the match any: priority=5 actions=goto_table:2
                continue

            if match_fields[1].startswith('dl_vlan='):
                vlan = match_fields[1].split('dl_vlan=')[1]

            # Get the RSP ID
            action_fields = space_fields[1].split(',')
            if action_fields[1].startswith('write_metadata:'):
                rsp_id_hex = action_fields[1].split('write_metadata:')[1].split('/')[0]
                rsp_id = str(int(rsp_id_hex, 16))

            if len(vlan) > 0 and len(rsp_id) > 0:
                prev_vlan = context.rsp_id_to_vlan_mappings.get(rsp_id, None)
                if prev_vlan != None:
                    if int(vlan) < int(prev_vlan):
                        # If the RSP ID already exists, only store the vlan if its smaller
                        del context.vlan_to_rsp_id_mappings[prev_vlan]
                        context.vlan_to_rsp_id_mappings[vlan] = rsp_id
                        context.rsp_id_to_vlan_mappings[rsp_id] = vlan
                else:
                    context.vlan_to_rsp_id_mappings[vlan] = rsp_id
                    context.rsp_id_to_vlan_mappings[rsp_id] = vlan
            else:
                print 'ERROR could not get RSP and VLAN from flow entry %s' % flow

    return

# Populates the context.clients list with all clients defined in mininet
def _get_clients(context):
    with open(MININET_DUMP_FILE) as f:
        for line in f:
            # Looking for something like this: <Host client1: client1-eth0:10.0.0.1 pid=3459>
            if line.startswith('<Host client'):
                space_fields = line.split()
                client_name = space_fields[1].rstrip(':')
                client_ip = space_fields[2].split(':')[1]
                context.clients.append(ClientInfo(name=client_name, ip=client_ip))

# Populates the context.clients entries with their vlan and rsp_id
# The information is retrieved from the GW1 flow entries
def _get_client_mappings(context):
    (rc, client_mappings) = _execute_command('sudo ovs-ofctl -O OpenFlow13 dump-flows gw1 table=0 | grep push_vlan | awk \'{print $6, $7}\'')

    if rc != 0:
        print 'Error get_client_mappings RC %d' % rc

    # Expecting something like this:
    #   priority=10,tcp,in_port=3,nw_src=10.0.0.1 actions=push_vlan:0x8100,set_field:4196->vlan_vid,set_field:0->vlan_pcp,set_field:00:00:00:00:11:01->eth_dst,output:1
    for client_str in client_mappings:
        if len(client_str) < 1:
            continue

        fields = client_str.split()
        # First get the IP from the matches
        client_ip = fields[0].split(',')[3].split('=')[1]

        # Then get the vlan ID from the actions
        vlan = ''
        action_fields = fields[1].split(',')
        for action_field in action_fields:
            if action_field.startswith('set_field:') and action_field.endswith('->vlan_vid'):
                vlan = str((int(action_field.split('set_field:')[1].split('->')[0])) & 0x0FFF)
            elif action_field.startswith('mod_vlan_vid'):
                # this is on older OVS versions
                vlan = str(int(action_field.split('mod_vlan_vid:')[1]))

        client_info = _find_client_by_ip(context.clients, client_ip)
        client_info.rsp_id = _find_rsp_id(context, vlan)
        client_info.vlan = vlan

def _get_gw2_servers():
    # When sfc-openflow-renderer_mininet.py starts, it stores the output of the mininet "dump" command here: /tmp/mininet_dump.txt
    # Return a list of all servers
    # Looking for the following lines:
    #     <Host server1: server1-eth0:10.10.0.1 pid=15553> 
    #     <Host server2: server2-eth0:10.10.0.2 pid=15554> 

    server_list = []

    with open(MININET_DUMP_FILE) as f:
        for line in f:
            if line.startswith('<Host server'):
                entries = line.split()
                server = entries[1].rstrip(':')
                server_ip = entries[2].split(':')[1]
                server_list.append((server, server_ip))

    return server_list

def _get_gw_port(gw_name, conn_name):
    # When sfc-openflow-renderer_mininet.py starts, it stores the output of the mininet "net" command here: /tmp/mininet_net.txt
    # Looking for one of the following lines to know the gw switch ports
    #    gw1 lo:  gw1-eth1:tor1-eth3 gw1-eth2:gw2-eth2 gw1-eth3:client1-eth0
    #    gw2 lo:  gw2-eth1:tor1-eth4 gw2-eth2:gw1-eth2 gw2-eth3:server1-eth0 gw2-eth4:server2-eth0

    with open(MININET_NET_FILE) as f:
        for line in f:
            if line.startswith(gw_name):
                entries = line.split()
                for entry in entries:
                    if conn_name in entry:
                        client_gw_entry = entry.split(':')[0]
                        return client_gw_entry.split('%s-eth'%gw_name)[1]

    # TODO, what to do here?
    return '-1'


def list_all_clients(context):
    print 'There are [%d] clients defined' % len(context.clients)
    for client in context.clients:
        print '\t%s' % client

def list_client(context):
    client = _find_client_by_name(context.clients, context.client_name)

    if client is None:
        print 'Client [%s] does not exist' % context.client_name
    else:
        print client

def list_all_rsps(context):
    print 'There are [%d] RSP IDs defined' % len(context.rsp_id_to_vlan_mappings)
    for (key, value) in sorted(context.rsp_id_to_vlan_mappings.iteritems()):
        print '\tRSP Id [%s] => VLAN [%s]' % (key, value)

def set_client_rsp_mapping(context):
    client_gw_port = _get_gw_port('gw1', context.client_name)
    if int(client_gw_port) < 0:
        print 'ERROR cant get switch port on gw1 for client %s' % context.client_name
        return

    vlan = _find_vlan(context, context.rsp_id)
    if vlan == None:
        print 'ERROR cant get vlan for rsp id %s' % context.rsp_id
        return

    client_info = _find_client_by_name(context.clients, context.client_name)
    if client_info is None:
        print 'ERROR cant get client info for client %s' % context.client_name
        return
    if len(client_info.ip) < 1:
        print 'ERROR cant get client ip for client %s' % context.client_name
        return

    # Populate the client info in gw1
    (rc, result_str) = _execute_command('sudo ovs-ofctl -O OpenFlow13  add-flow gw1 "table=0,priority=10,in_port=%s,dl_type=0x0800,nw_proto=6,nw_src=%s,actions=push_vlan=0x8100,mod_vlan_vid=%s,mod_vlan_pcp=0, output=1"' % (client_gw_port, client_info.ip, vlan))
    if rc != 0:
        print 'ERROR creating client flow in gw1 rc=%d' % rc

    # Also need to populate gw2 with the server IP and vlan for each server
    server_list = _get_gw2_servers()
    for server_info in server_list:
        # TODO assuming all RSPs are symmetric, and the next VLAN is offset by 100
        return_vlan = int(vlan)+100
        server_gw2_port = _get_gw_port('gw2', server_info[0])
        (rc, result_str) = _execute_command('sudo ovs-ofctl  -O OpenFlow13 add-flow gw2 "table=0,priority=10,in_port=%s,dl_type=0x0800,nw_proto=6,nw_dst=%s,actions=push_vlan=0x8100,mod_vlan_vid=%d,mod_vlan_pcp=0,set_field=00:00:00:00:01:01->eth_dst,output=1"' % (server_gw2_port, client_info.ip, return_vlan))
        if rc != 0:
            print 'ERROR creating server flow in gw2 rc=%d' % rc

def main():
    context = Context()
    if not get_cmd_line_args(context):
        return

    _get_rsp_and_vlan_mappings(context)
    _get_clients(context)
    _get_client_mappings(context)

    if context.do_list:
        list_client(context)
    if context.do_list_all:
        list_all_clients(context)
    if context.do_rsp_list:
        list_all_rsps(context)
    if context.do_set:
        set_client_rsp_mapping(context)

    return

if __name__ == '__main__':
    main()


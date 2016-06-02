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
# This script will read the SFF flows and populate the ToR switch accordingly
# It will look for these sorts of flows, and populate the ToR with the necessary info:
#    table=1, n_packets=0, priority=350,dl_vlan=101 actions=pop_vlan,write_metadata:0x1/0xffff,goto_table:3
#    table=1, n_packets=0, priority=350,dl_vlan=200 actions=pop_vlan,write_metadata:0x2/0xffff,goto_table:3
#    table=1, n_packets=0, priority=350,dl_vlan=500 actions=pop_vlan,write_metadata:0x5/0xffff,goto_table:3
#    table=1, n_packets=0, priority=350,dl_vlan=600 actions=pop_vlan,write_metadata:0x6/0xffff,goto_table:3
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
        self.gw_ingress = 'gw1'
        self.gw_egress  = 'gw2'

#
# Get the command line args
#
def get_cmd_line_args(context):

    opts = argparse.ArgumentParser()

    # Ingress GW
    opts.add_argument('--gw-ingress', '-I',
                  default=context.gw_ingress,
                  dest='gw_ingress',
                  help='Set the uplink ingress GW name, default gw1')

    # Egress GW
    opts.add_argument('--gw-egress', '-E',
                  default=context.gw_egress,
                  dest='gw_egress',
                  help='Set the uplink egress GW name, default gw2')

    args = opts.parse_args()

    context.gw_ingress = args.gw_ingress
    context.gw_egress  = args.gw_egress

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

def get_sffs():
    (rc, sffs_str) = _execute_command('sudo ovs-vsctl list-br | grep sff | grep -v node')
    if rc != 0:
        print 'Error get_sffs RC %d' % rc
        return []

    return sffs_str

def get_sff_vlans(sff):
    (rc, sff_vlan_str) = _execute_command('sudo ovs-ofctl -O OpenFlow13 dump-flows %s table=1 | awk \'{print $6}\''%sff)
    if rc != 0:
        print 'Error get_sff_vlans RC %d' % rc
        return []

    sff_vlan_list = []
    for sff_vlan in sff_vlan_str:
        strs = sff_vlan.split(',')
        if len(strs) == 2:
            sff_vlan_list.append(strs[1].split('=')[1])

    return sff_vlan_list

def get_gw_egress_vlans(gw_name):
    (rc, gw_vlan_str) = _execute_command('sudo ovs-ofctl -O OpenFlow13 dump-flows %s table=0 | grep push_vlan | awk \'{print $7}\''%gw_name)
    if rc != 0:
        print 'Error get_gw_egress_vlans RC %d' % rc
        return []

    # expecting a list of strs like this, need to get 4196:
    # actions=push_vlan:0x8100,set_field:4196->vlan_vid,set_field:0->vlan_pcp,set_field:00:00:00:00:11:01->eth_dst,output:1
    #   OR this
    # actions=push_vlan:0x8100,mod_vlan_vid:300,mod_vlan_pcp:0,set_field:00:00:00:00:11:01->eth_dst,output:1


    gw_vlan_list = []
    for gw_vlan in gw_vlan_str:
        fields = gw_vlan.split(',')
        for field in fields:
            if field.startswith('set_field:') and field.endswith('->vlan_vid'):
                gw_vlan_list.append((int(field.split('set_field:')[1].split('->')[0])) & 0x00FF)
            elif field.startswith('mod_vlan_vid'):
                gw_vlan_list.append((int(field.split('mod_vlan_vid:')[1])))
    return gw_vlan_list

def get_tor_port(sff_name):
    # When sfc-openflow-renderer_mininet.py starts, it stores the output of the mininet "net" command here: /tmp/mininet_net.txt
    # Looking for the following line to know the tor1 switch ports
    #    tor1 lo:  tor1-eth1:sff1-eth2 tor1-eth2:sff2-eth2 tor1-eth3:gw1-eth1 tor1-eth4:gw2-eth1

    with open('/tmp/mininet_net.txt') as f:
        for line in f:
            if line.startswith('tor1'):
                entries = line.split()
                for entry in entries:
                    if sff_name in entry:
                        tor_sff_entry = entry.split(':')[0]
                        return tor_sff_entry.split('tor1-eth')[1]

    # TODO, what to do here?
    return '-1'

def create_tor_vlan_flow(vlan, tor_port):
    print 'Create tor flows : vlan [%s] => tor port [%s]' % (vlan, tor_port)
    flow_template = Template('sudo ovs-ofctl -O Openflow13 add-flow tor1 "table=0,priority=10,vlan_tci=0x1000/0x1000,dl_vlan=$VLAN,actions=output=$PORT"')
    (rc, tor_flow_str) = _execute_command(flow_template.substitute(VLAN=vlan, PORT=tor_port))

def create_tor_vlan_flows(sff_name, sff_vlan_list, tor_port):
    for vlan in sff_vlan_list:
        create_tor_vlan_flow(vlan, tor_port)


def main():
    context = Context()
    if not get_cmd_line_args(context):
        print 'ERROR in command line args, exiting...'
        return

    all_vlans = []

    # Create tor flows for the SFFs
    sff_list = get_sffs()
    for sff in sff_list:
        sff_vlans = get_sff_vlans(sff)
        all_vlans.extend(sff_vlans)
        tor_port = get_tor_port(sff)
        create_tor_vlan_flows(sff, sff_vlans, tor_port)

    if len(all_vlans) == 0:
        print 'No flows in SFFs to process, exiting...'
        return

    # Get the RSP vlan ranges, needed for the RSP egress to GW population
    min_to_max_vlans = {}
    min_vlan = int(min(all_vlans))
    prev_vlan = min_vlan
    for vlanStr in sorted(all_vlans , key=lambda x: int(x)):
        vlan = int(vlanStr)
        if (vlan - min_vlan) >= 100:
            min_to_max_vlans[min_vlan] = prev_vlan+1
            min_vlan = vlan
        prev_vlan = vlan
    min_to_max_vlans[min_vlan] = prev_vlan+1

    # Create the RSP egress flows to the egress GW
    is_uplink = True
    uplink_egress_tor_port = get_tor_port(context.gw_egress)
    downlink_egress_tor_port = get_tor_port(context.gw_ingress)
    for (vlan_min, vlan_max) in sorted(min_to_max_vlans.iteritems()):
        if is_uplink:
            create_tor_vlan_flow(vlan_max, uplink_egress_tor_port)
            is_uplink = False
        else:
            create_tor_vlan_flow(vlan_max, downlink_egress_tor_port)
            is_uplink = True

if __name__ == '__main__':
    main()


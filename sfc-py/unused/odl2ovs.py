#
# Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import logging
import socket

from flask import *  # noqa
from random import randint
import sys
import getopt
import json
import requests
from odl2ovs_cli import *  # noqa

__author__ = "Paul Quinn, Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "paulq@cisco.com, rapenno@gmail.com"
__status__ = "alpha"

""" SFF REST Server. This Server should be co-located with a OVS switch """

app = Flask(__name__)
my_topo = {}

sff_topo = {}
path = {}

# ODL IP:port
ODLIP = "127.0.0.1:8181"
# Static URLs for testing
SF_URL = "http://" + ODLIP + "/restconf/config/service-function:service-functions/"
SFC_URL = "http://" + ODLIP + "/restconf/config/service-function-chain:service-function-chains/"
SFF_URL = "http://" + ODLIP + "/restconf/config/service-function-forwarder:service-function-forwarders/"
SFT_URL = "http://" + ODLIP + "/restconf/config/service-function-type:service-function-types/"
SFP_URL = "http://" + ODLIP + "/restconf/config/service-function-path:service-function-paths/"

SFF_PARAMETER_URL = "http://{}/restconf/config/service-function-forwarder:service-function-forwarders/"

SFF_NAME_PARAMETER_URL = "http://{}/restconf/config/service-function-forwarder:service-function-forwarders/service-function-forwarder/{}"  # noqa

USERNAME = "admin"
PASSWORD = "admin"

logger = logging.getLogger(__name__)


def sffinit():
    """
    This function is used when testing without actual OVS switch
    :return:
    """

    sff_topo_init = {
        "service-function-forwarders": {
            "service-function-forwarder": [
                {
                    "name": "SFF1",
                    "service-node": "OVSDB1",
                    "sff-data-plane-locator": [
                        {
                            "name": "eth0",
                            "service-function-forwarder-ovs:ovs-bridge": {
                                "bridge-name": "br-tun",
                                "uuid": "4c3778e4-840d-47f4-b45e-0988e514d26c"
                            },
                            "data-plane-locator": {
                                "port": 4789,
                                "ip": "10.100.100.1",
                                "transport": "service-locator:vxlan-gpe"
                            }
                        }
                    ],
                    "rest-uri": "http://198.18.134.23",
                    "service-function-dictionary": [
                        {
                            "name": "SF1",
                            "type": "dp1",
                            "sff-sf-data-plane-locator": {
                                "port": 4789,
                                "ip": "10.1.1.4",
                                "transport": "service-locator:vxlan-gpe",
                                "service-function-forwarder-ovs:ovs-bridge": {
                                    "bridge-name": "br-int"
                                }
                            }
                        },
                        {
                            "name": "SF2",
                            "type": "napt44",
                            "sff-sf-data-plane-locator": {
                                "port": 4789,
                                "ip": "10.1.1.5",
                                "transport": "service-locator:vxlan-gpe",
                                "service-function-forwarder-ovs:ovs-bridge": {
                                    "bridge-name": "br-int"
                                }
                            }
                        }
                    ],
                    "classifier": "acl-sfp-1",
                    "ip-mgmt-address": "198.18.134.23"
                },
                {
                    "name": "SFF2",
                    "service-node": "OVSDB2",
                    "sff-data-plane-locator": [
                        {
                            "name": "eth0",
                            "service-function-forwarder-ovs:ovs-bridge": {
                                "bridge-name": "br-tun",
                                "uuid": "fd4d849f-5140-48cd-bc60-6ad1f5fc0a0"
                            },
                            "data-plane-locator": {
                                "port": 4789,
                                "ip": "10.100.100.2",
                                "transport": "service-locator:vxlan-gpe"
                            }
                        }
                    ],
                    "rest-uri": "http://198.18.134.23",
                    "service-function-dictionary": [
                        {
                            "name": "SF3",
                            "type": "firewall",
                            "sff-sf-data-plane-locator": {
                                "port": 4789,
                                "ip": "10.1.2.6",
                                "transport": "service-locator:vxlan-gpe",
                                "service-function-forwarder-ovs:ovs-bridge": {
                                    "bridge-name": "br-int"
                                }
                            }
                        }
                    ],
                    "ip-mgmt-address": "198.18.134.24"
                }
            ]
        }
    }

    return sff_topo_init


def pathinit():
    """
    This function is used when testing without actual OVS switch
    :return:
    """
    path_init = {
        "service-function-paths": {
            "service-function-path": [
                {
                    "name": "Path-1-SFC1",
                    "path-id": 1,
                    "starting-index": 3,
                    "service-chain-name": "SFC1",
                    "service-path-hop": [
                        {
                            "hop-number": 0,
                            "service-function-name": "SF1",
                            "service_index": 3,
                            "service-function-forwarder": "SFF1"
                        },
                        {
                            "hop-number": 1,
                            "service-function-name": "SF2",
                            "service_index": 2,
                            "service-function-forwarder": "SFF1"
                        },
                        {
                            "hop-number": 2,
                            "service-function-name": "SF3",
                            "service_index": 1,
                            "service-function-forwarder": "SFF2"
                        }
                    ]
                }
            ]
        }
    }

    return path_init


# the following dictionaries are for testing only.  Remove when running on OVS.
def get_bridge_info():
    b1 = {
        'status': '{}',
        'fail_mode': '[]',
        'datapath_id': '"0000e21a84dd0c4c"',
        'datapath_type': '""',
        'sflow': '[]',
        'mirrors': '[]',
        'ipfix': '[]',
        '_uuid': 'dd841ae1-0a6e-4c0c-b24c-059e7b0b87f8',
        'other_config': '{}',
        'flood_vlans': '[]',
        'stp_enable': 'false',
        'controller': '[]',
        'mcast_snooping_enable': 'false',
        'flow_tables': '{}',
        'ports': '[60ce3635-70d2-4c48-98f6-cefd65ab0e58]',
        'external_ids': '{bridge-id="SFF1"}',
        'netflow': '[]',
        'protocols': '[]',
        'name': '"br-int"'
    }

    b2 = {
        'status': '{}',
        'fail_mode': '[]',
        'datapath_id': '"000052f810c06148"',
        'datapath_type': '""',
        'sflow': '[]',
        'mirrors': '[]',
        'ipfix': '[]',
        '_uuid': 'c010f853-5c8a-4861-9e53-050981fbc121',
        'other_config': '{}',
        'flood_vlans': '[]',
        'stp_enable': 'false',
        'controller': '[]',
        'mcast_snooping_enable': 'false',
        'flow_tables': '{}',
        'ports': '[4a194fdd-ed59-47cf-998b-7c996c46e3e6]',
        'external_ids': '{}',
        'netflow': '[]',
        'protocols': '[]',
        'name': '"br-tun"'
    }

    # br_list = []
    br_dict_list = []
    # bc = 0
    # br_dict={}

    # bridges = subprocess.check_output(['ovs-vsctl', 'list-br'])

    # for line in bridges.split('\n'):
    # br_list.append(line)

    # while bc < (len(br_list) - 1):
    # b = subprocess.check_output(['ovs-vsctl', 'list', 'bridge', br_list[b]])
    # for row in b.split('\n'):
    # if ': ' in row:
    # key, value = row.split(': ')
    # br_dict[key.strip()] = value.strip()
    #  br_dict_list.append(br_dict)
    #  b = b+1

    # test code

    br_dict_list.append(b1)
    br_dict_list.append(b2)
    return br_dict_list


# This function does not work if machine has more than one IP/interface
def get_my_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(('8.8.8.8', 80))
    myip = (s.getsockname()[0])
    s.close()
    myip = "http://" + myip + ":/paths"
    return myip


def parse_bridges(bridge_list):
    # num_of_bridges = len(bridge_list)
    all_bridges = []
    br_dict = {}

    for bridge in bridge_list:
        if bridge['name'] == '"br-tun"' or '"br-int"':
            br_dict = {
                'name': bridge['name'][1:-1],
                'external_ids': bridge['external_ids'], 'uuid': bridge['_uuid']
            }
        all_bridges.append(br_dict)
    return all_bridges


# Not used anymore
def who_am_i(path, bridges):
    for path in path['service-function-paths']['service-function-path']:
        for sff in path['service-path-hop']:
            for bridge in bridges:
                if sff['service-function-forwarder'] == bridge['external_ids'][12:-2]:
                    return sff['service-function-forwarder']


def who_am_i_sfp(service_path):
    """
    Determines the name of the local attached SFF
    :param service_path: A single Service Function Path
    :return: The name of the local attached SFF
    """
    ovsbridges = get_bridge_info()
    bridges = parse_bridges(ovsbridges)
    for sff in service_path['service-path-hop']:
        for bridge in bridges:
            if sff['service-function-forwarder'] == bridge['external_ids'][12:-2]:
                return sff['service-function-forwarder']
    return None


def who_am_i_sff():
    """
    Determines the name of the local attached SFF by checking
    against the collections of all known SFFs
    :return: The name of the local attached SFF
    """
    ovsbridges = get_bridge_info()
    bridges = parse_bridges(ovsbridges)
    for bridge in bridges:
        if bridge['external_ids'][12:-2] in sff_topo.keys():
            return bridge['external_ids'][12:-2]
    return None


# Not used anymore
def build_a_path(path, my_sff):
    # me = 'SFF-bootstrap'
    sflist = []
    nextsff = {}
    sfdict = {}
    count = 0
    pid = 0

    for path in path['service-function-paths']['service-function-path']:
        pid = path['path-id']
        for sf in path['service-path-hop']:
            if sf['service-function-forwarder'] == my_sff:
                sfdict['sff'] = sf['service-function-forwarder']
                sfdict['pid'] = path['path-id']
                sfdict['name'] = sf['service-function-name']
                sfdict['index'] = sf['service_index']
                find_sf_loc(sfdict)
                sflist.append(sfdict)
                sfdict = {}
                count += 1
    nextsff['sff-name'] = path['service-path-hop'][count]['service-function-forwarder']
    nextsff['sff-index'] = path['service-path-hop'][count]['service_index']
    nextsffloc = find_sff_loc(nextsff)
    return sflist, nextsffloc, nextsff, my_sff, pid


def build_service_path(service_path, my_sff_name):
    """
    Builds a dictionary of the local attached Service Functions
    :param path: A single Service Function Path
    :param my_sff_name: The name of the local attached SFF
    :return:
    """
    # my_sff = 'SFF-bootstrap'
    sflist = []
    nextsff = {}
    sfdict = {}
    count = 0

    for service_hop in service_path['service-path-hop']:
        if service_hop['service-function-forwarder'] == my_sff_name:
            sfdict['sff'] = service_hop['service-function-forwarder']
            sfdict['pid'] = service_path['path-id']
            sfdict['name'] = service_hop['service-function-name']
            sfdict['index'] = service_hop['service_index']
            sfdict['locator'] = find_sf_locator(sfdict['name'], sfdict['sff'])
            if sfdict['locator'] is None:
                logger.error("Could not find data plane locator for SF: %s", sfdict['name'])
            sflist.append(sfdict)
            sfdict = {}
            count += 1
    nextsff['sff-name'] = service_path['service-path-hop'][count]['service-function-forwarder']
    nextsff['sff-index'] = service_path['service-path-hop'][count]['service_index']
    nextsffloc = find_sff_locator(nextsff['sff-name'])
    if nextsffloc is None:
        logger.error("Could not find data plane locator for SFF: %s", nextsff['sff-name'])
    return sflist, nextsffloc, nextsff


def find_sf_locator(sf_name, sff_name):
    """
    Looks for the SF name  within the service function
    dictionary of sff_name. If found, return the
    corresponding data plane locator

    :param sfdict: A dictionary with a single SF attributes
    :return: SF data plane locator
    """
    service_dictionary = sff_topo[sff_name]['service-function-dictionary']
    for service_function in service_dictionary:
        if sf_name == service_function['name']:
            return service_function['sff-sf-data-plane-locator']['ip']
    return None


def find_sff_locator(sff_name):
    """
    For a given SFF name, look into local SFF topology for a match
    and returns the corresponding data plane locator
    :param sff_name:
    :return: SFF data plane locator
    """
    try:
        return sff_topo[sff_name]['sff-data-plane-locator'][0]['data-plane-locator']['ip']
    except KeyError:
        msg = "SFF {} locator not found".format(sff_name)
        logger.warning(msg)
        return None
    except:
        logger.warning("Unexpected exception, re-raising it")
        raise


# Not used anymore
def find_sff_loc(sff):
    for sffi in sff_topo['service-function-forwarders']['service-function-forwarder']:
        if sffi['name'] == sff['sff-name']:
            return sffi['sff-data-plane-locator'][0]['data-plane-locator']['ip']


# Not used anymore
def find_sf_loc(sfdict):
    count = 0
    while count < len(sff_topo['service-function-forwarders']['service-function-forwarder']):
        if sff_topo['service-function-forwarders']['service-function-forwarder'][count]['name'] == sfdict['sff']:
            for sfi in (sff_topo['service-function-forwarders']
                        ['service-function-forwarder']
                        [count]['service-function-dictionary']):
                if sfdict['name'] == sfi['name']:
                    sfdict['locator'] = sfi['sff-sf-data-plane-locator']['ip']
        count += 1
        return


def mytopo(nextsffloc, vxlanid):
    global my_topo
    if nextsffloc in my_topo.keys():
        return my_topo[nextsffloc]
    else:
        vxlan = 'vxlan' + str(vxlanid)
        my_topo[nextsffloc] = vxlan
        vxlanid += 1
        return vxlanid


def cli():
    global path
    global sff_topo
    path = pathinit()
    sff_topo = sffinit()
    ovsbridges = get_bridge_info()
    bridge_info = parse_bridges(ovsbridges)
    my_sff = who_am_i(path, bridge_info)
    vxlanid = 0
    key = hex(randint(1, 16777216))
    build_a_path(path, my_sff)
    mysflist, nextsffloc, nextsff, my_sff, pid = build_a_path(path, my_sff)
    vxlanid = mytopo(nextsffloc, vxlanid)
    vxlanid = cli_local(mysflist, vxlanid, key)
    cli_nextsff(nextsffloc, nextsff, key, vxlanid, pid)
    return


# Not used anymore
def ovsbuildit(path):
    print "BUILDING CHAIN..."
    ovsbridges = get_bridge_info()
    bridge_info = parse_bridges(ovsbridges)
    my_sff = who_am_i(path, bridge_info)
    # my_topo = {}
    vxlanid = 0
    key = hex(randint(1, 16777216))

    mysflist, nextsffloc, nextsff, me, pid = build_a_path(path, my_sff)
    my_topo, vxlanid = mytopo(nextsffloc, vxlanid)
    vxlanid = ovs_cli_local(mysflist, vxlanid, key)
    ovs_cli_nextsff(nextsffloc, nextsff, key, vxlanid, pid)
    return


def ovsbuild_one_path(service_path):
    """
    :param path: A single Service Function Path
    :return: Nothing
    """
    logger.info("BUILDING CHAIN...")
    my_sff_name = who_am_i_sfp(service_path)
    if my_sff_name is None:
        logger.info("Service path does not contain local SFF")
        return
    # Is this correct?
    vxlanid = 0
    key = hex(randint(1, 16777216))

    mysflist, nextsffloc, nextsff = build_service_path(service_path, my_sff_name)
    vxlanid = mytopo(nextsffloc, vxlanid)
    vxlanid = ovs_cli_local(mysflist, vxlanid, key)
    pid = mysflist[0]['pid']
    ovs_cli_nextsff(nextsffloc, nextsff, key, vxlanid, pid)
    return


@app.route('/config/service-function-path:service-function-paths/', methods=['GET'])
def get_paths():
    return jsonify({'Service paths': path})


@app.route('/config/service-function-forwarder:service-function-forwarders/', methods=['GET'])
def get_sffs():
    return jsonify({'SFFs': sff_topo})


@app.route('/config/service-function-path:service-function-paths/', methods=['PUT'])
def create_paths():
    global path
    if not request.json:
        abort(400)
    else:
        path = {
            'service-function-paths': request.json['service-function-paths']
        }
    if any(sff_topo):
        ovsbuildit(path)
    return jsonify({'path': path}), 201


@app.route('/config/service-function-path:service-function-paths/service-function-path/<sfpname>', methods=['PUT'])
def create_path(sfpname):
    global path
    if not request.json:
        abort(400)
    else:
        # print json.dumps(sfpjson)
        # sfpj_name = sfpjson["service-function-path"][0]['name']
        path[sfpname] = request.get_json()["service-function-path"][0]

    if any(sff_topo):
        ovsbuild_one_path(path[sfpname])
    return jsonify({'path': path}), 201


@app.route('/config/service-function-path:service-function-paths/service-function-path/<sfpname>', methods=['DELETE'])
def delete_path(sfpname):
    global path
    try:
        del path[sfpname]
    except KeyError:
        msg = "SFP name {} not found, message".format(sfpname)
        logger.warning(msg)
        return msg, 404
    except:
        logger.warning("Unexpected exception, re-raising it")
        raise
    return '', 204


@app.route('/config/service-function-forwarder:service-function-forwarders/service-function-forwarder/<sffname>',
           methods=['PUT'])
def create_sff(sffname):
    global sff_topo
    if not request.json:
        abort(400)
    else:
        sff_topo[sffname] = request.get_json()['service-function-forwarder'][0]
    if any(path):
        ovsbuild_one_path(path)
    return jsonify({'sff': sff_topo}), 201


@app.route('/config/service-function-forwarder:service-function-forwarders/service-function-forwarder/<sffname>',
           methods=['DELETE'])
def delete_sff(sffname):
    global sff_topo
    try:
        del sff_topo[sffname]
    except KeyError:
        msg = "SFF name {} not found, message".format(sffname)
        logger.warning(msg)
        return msg, 404
    except:
        logger.warning("Unexpected exception, re-raising it")
        raise
    return '', 204


@app.route('/config/service-function-forwarder:service-function-forwarders/', methods=['PUT'])
def create_sffs():
    global sff_topo
    if not request.json:
        abort(400)
    else:
        sff_topo = {
            'service-function-forwarders': request.json['service-function-forwarders']
        }
    if any(path):
        ovsbuildit(path)
    return jsonify({'sff': sff_topo}), 201


@app.route('/config/service-function-forwarder:service-function-forwarders/', methods=['DELETE'])
def delete_sffs():
    global sff_topo
    sff_topo = {}
    return jsonify({'sff': sff_topo}), 201


@app.errorhandler(404)
def page_not_found(e):
    return render_template('404.html'), 404


def get_sffs_from_odl(odl_ip_port):
    """
    Retrieves the list fo configured SFFs from ODL
    :return: Nothing
    """
    global sff_topo
    s = requests.Session()
    print ("Getting SFF information from ODL... \n")
    r = s.get(SFF_PARAMETER_URL.format(odl_ip_port), stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        sff_json = json.loads(r.text)['service-function-forwarders']['service-function-forwarder']
        for sff in sff_json:
            sff_topo[sff['name']] = sff
    else:
        print ("=>Failed to GET SFF from ODL \n")


def get_sff_from_odl(odl_ip_port, sff_name):
    """
    Retrieves the list fo configured SFFs from ODL
    :return: Nothing
    """
    global sff_topo
    s = requests.Session()
    print ("Getting SFF information from ODL... \n")
    r = s.get(SFF_PARAMETER_URL.format(odl_ip_port, sff_name), stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        sff_topo[sff_name] = request.get_json()['service-function-forwarder'][0]
    else:
        print ("=>Failed to GET SFF from ODL \n")


def main(argv):
    global ODLIP
    try:
        logging.basicConfig(level=logging.INFO)
        opt, args = getopt.getopt(argv, "hrc", ["help", "rest", "cli", "odl-get-sff", "odl-ip-port="])
    except getopt.GetoptError:
        print 'rest2ovs --help | --rest | --cli | --odl-get-sff | --odl-ip-port'
        sys.exit(2)

    odl_get_sff = False
    rest = False
    for opt, arg in opt:
        if opt == "--odl-get-sff":
            odl_get_sff = True
            continue

        if opt == "--odl-ip-port":
            ODLIP = arg
            continue

        if opt in ('-h', '--help'):
            print 'rest2ovs -m cli | rest --odl-get-sff --odl-ip-port'
            sys.exit()

        if opt in ('-c', '--cli'):
            cli()
            sys.exit()

        if opt in ('-r', '--rest'):
            rest = True

    if odl_get_sff:
        get_sffs_from_odl(ODLIP)

    if rest:
        app.debug = True
        app.run(host='0.0.0.0')


if __name__ == "__main__":
    main(sys.argv[1:])

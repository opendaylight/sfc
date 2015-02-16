__author__ = "Paul Quinn, Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.4"
__email__ = "paulq@cisco.com, rapenno@gmail.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

""" SFC Agent Server. This Server should be co-located with the python SFF data
    plane implementation (sff_thread.py)"""


import sys
import json
import socket
import getopt
import logging
import argparse
import requests
import netifaces
import collections
import multiprocessing

#TODO: fix this kind of imports
from flask import *
from pprint import pprint
from threading import Thread


import xe_cli
import ovs_cli

#TODO: fix this kind of imports
from sff_globals import *
from common.launcher import start_sff, start_sf

if sys.platform.startswith('linux'):
    #TODO: fix this kind of imports
    from nfq_class_thread import *


# IP address of associated SFF thread.
# WE assume agent and thread and co-located
SFF_UDP_IP = "127.0.0.1"
app = Flask(__name__)
logger = logging.getLogger(__name__)


def tree():
    return collections.defaultdict(tree)


def find_sf_locator(sf_name, sff_name):
    """
    Looks for the SF name within the service function dictionary of sff_name.
    If found, return the corresponding data plane locator.

    :param sf_name: SF name
    :param  sff_name: SFF name

    :return SF data plane locator

    """

    local_sff_topo = get_agent_globals().get_sff_topo()
    sf_locator = {}
    if sff_name not in local_sff_topo.keys():
        if get_sff_from_odl(ODLIP, sff_name) != 0:
            logger.error("Failed to find data plane locator for SF: %s", sf_name)
            return sf_locator
    service_dictionary = local_sff_topo[sff_name]['service-function-dictionary']
    for service_function in service_dictionary:
        if sf_name == service_function['name']:
            sf_locator['ip'] = service_function['sff-sf-data-plane-locator']['ip']
            sf_locator['port'] = service_function['sff-sf-data-plane-locator']['port']
            return sf_locator

    if not sf_locator:
        logger.error("Failed to find data plane locator for SF: %s", sf_name)
    return sf_locator


def find_sff_locator_by_ip(addr):
    """
    For a given IP addr iterate over all SFFs looking for which one has a the
    same data plane locator IP

    :param addr: IP address
    :type addr: str

    :return SFF name or None

    """
    sff_name = None

    local_sff_topo = agent_globals.get_sff_topo()
    for sff_name, sff_value in local_sff_topo.items():
        for locator_value in sff_value['sff-data-plane-locator']:
            if locator_value['data-plane-locator']['ip'] == addr:
                return sff_name

    return None


def find_sff_locator(sff_name):
    """
    For a given SFF name, look into local SFF topology for a match
    and returns the corresponding data plane locator. If SFF is not known
    tries to retrieve it from ODL
    :param sff_name:
    :return: SFF data plane locator
    """

    local_sff_topo = get_agent_globals().get_sff_topo()
    sff_locator = {}
    if sff_name not in local_sff_topo.keys():
        if get_sff_from_odl(ODLIP, sff_name) != 0:
            logger.error("Failed to find SFF data plane locator for SFF: %s", sff_name)
            return sff_locator

    sff_locator['ip'] = local_sff_topo[sff_name]['sff-data-plane-locator'][0]['data-plane-locator']['ip']
    sff_locator['port'] = local_sff_topo[sff_name]['sff-data-plane-locator'][0]['data-plane-locator']['port']
    return sff_locator


@app.route('/operational/rendered-service-path:rendered-service-paths/', methods=['GET'])
def get_paths():
    return jsonify(get_agent_globals().get_path())


@app.route('/operational/data-plane-path:data-plane-paths/', methods=['GET'])
def get_data_plane_paths():
    return jsonify(get_agent_globals().get_data_plane_path())


@app.route('/config/service-function-forwarder:service-function-forwarders/', methods=['GET'])
def get_sffs():
    return jsonify(get_agent_globals().get_sff_topo())


@app.route('/operational/rendered-service-path:rendered-service-paths/', methods=['PUT', 'POST'])
def create_paths():
    if not request.json:
        abort(400)
    else:
        path_json = {
            'rendered-service-paths': request.json['rendered-service-paths']
        }

        # reset path data
        get_agent_globals().reset_data_plane_path()
        local_path = get_agent_globals().get_path()

        for path_item in path_json['rendered-service-paths']['rendered-service-path']:
            local_path[path_item['name']] = path_item
            # rebuild path data
            build_data_plane_service_path(path_item)

    return jsonify({'path': path}), 201


def build_data_plane_service_path(service_path):
    """
    Builds a dictionary of the local attached Service Functions
    :param service_path: A single Service Function Path
    :return:
    """

    local_data_plane_path = get_agent_globals().get_data_plane_path()

    for service_hop in service_path['rendered-service-path-hop']:

        if service_hop['service-function-forwarder'] == get_agent_globals().get_my_sff_name():
            if service_path['path-id'] not in local_data_plane_path.keys():
                local_data_plane_path[service_path['path-id']] = {}
            sf_locator = find_sf_locator(service_hop['service-function-name'],
                                         service_hop['service-function-forwarder'])
            if sf_locator:
                local_data_plane_path[service_path['path-id']][service_hop['service-index']] = sf_locator
            else:
                logger.error("Failed to build rendered service path: %s", service_path['name'])
                return -1
        else:
            # If SF resides in another SFF, the locator is just the data plane
            # locator of that SFF.
            if service_path['path-id'] not in local_data_plane_path.keys():
                local_data_plane_path[service_path['path-id']] = {}
            sff_locator = find_sff_locator(service_hop['service-function-forwarder'])
            if sff_locator:
                local_data_plane_path[service_path['path-id']][service_hop['service-index']] = sff_locator
            else:
                logger.error("Failed to build rendered service path: %s", service_path['name'])
                return -1

    return 0


@app.route('/operational/service-function-forwarder:service-function-forwarders-state/threads', methods=['GET'])
def get_sffs_threads():
    local_sff_threads = get_sff_threads()
    serialized_threads = {}
    for key, value in local_sff_threads.items():
        if value['thread'].is_alive():
            serialized_threads[key] = {}
            serialized_threads[key]['socket'] = str(value['socket'])
            serialized_threads[key]['thread'] = str(value['thread'])
    return jsonify(serialized_threads)


@app.route('/operational/rendered-service-path:rendered-service-paths/rendered-service-path/<sfpname>',
           methods=['PUT', 'POST'])
def create_path(sfpname):
    # global path
    local_path = get_agent_globals().get_path()
    local_sff_os = get_agent_globals().get_sff_os()
    if not request.json:
        abort(400)
    else:

        if (not get_agent_globals().get_my_sff_name()) and (auto_sff_name()):
            logger.fatal("Could not determine my SFF name \n")
            sys.exit(1)
        # print json.dumps(sfpjson)
        # sfpj_name = sfpjson["service-function-path"][0]['name']
        local_path[sfpname] = request.get_json()["rendered-service-path"][0]
        logger.info("Building Service Path for path: %s", sfpname)
        if not build_data_plane_service_path(local_path[sfpname]):
            # Testing XE cli processing module
            if local_sff_os == 'XE':
                logger.info("Provisioning %s SFF", local_sff_os)
                xe_cli.process_xe_cli(get_agent_globals().get_data_plane_path())
            elif local_sff_os == 'OVS':
                logger.info("Provisioning %s SFF", local_sff_os)
                # process_ovs_cli(data_plane_path)
                ovs_cli.process_ovs_sff__cli(get_agent_globals().get_data_plane_path())
            elif local_sff_os == "ODL":
                logger.info("Provisioning %s SFF", local_sff_os)
            else:
                logger.error("Unknown SFF OS: %s", local_sff_os)  # should never get here
                # json_string = json.dumps(data_plane_path)
            return jsonify(local_path), 201
        else:
            msg = "Could not build service path: {}".format(sfpname)
            return msg, 400


@app.route('/operational/rendered-service-path:rendered-service-paths/rendered-service-path/<sfpname>',
           methods=['DELETE'])
def delete_path(sfpname):
    local_data_plane_path = get_agent_globals().get_data_plane_path()
    local_path = get_agent_globals().get_path()
    try:
        sfp_id = local_path[sfpname]['path-id']
        local_data_plane_path.pop(sfp_id, None)
        local_path.pop(sfpname, None)

        # remove nfq classifier for this path
        if sys.platform.startswith('linux'):
            nfq_class_manager = get_nfq_class_manager_ref()
            if nfq_class_manager:
                nfq_class_manager.destroy_packet_forwarder(sfp_id)

        json_string = json.dumps(local_data_plane_path)
    except KeyError:
        msg = "SFP name {} not found, message".format(sfpname)
        logger.warning(msg)
        return msg, 404
    except:
        logger.warning("Unexpected exception, re-raising it")
        raise
    return '', 204


@app.route('/config/service-function:service-functions/service-function/<sfname>',
           methods=['PUT', 'POST'])
def create_sf(sfname):
    logger.info("Received request for SF creation: %s", sfname)
    local_sf_topo = get_agent_globals().get_sf_topo()
    local_sf_threads = get_agent_globals().get_sf_threads()
    control_port = get_agent_globals().get_data_plane_control_port()

    if not request.json:
        abort(400)
    else:
        local_sf_topo[sfname] = request.get_json()['service-function'][0]
        data_plane_locator_list = local_sf_topo[sfname]['sf-data-plane-locator']
        for i, data_plane_locator in enumerate(data_plane_locator_list):
            if ("ip" in data_plane_locator) and ("port" in data_plane_locator):
                sf_port = data_plane_locator['port']
                sf_prefix, sf_type = (local_sf_topo[sfname]['type']).split(':')
                local_sf_threads[sfname] = {}

                # TODO: We need more checks to make sure IP in locator actually corresponds to one
                # TODO: of the existing interfaces in the system
                sf_thread = Thread(target=start_sf,
                                   args=(sfname, "0.0.0.0", sf_port, sf_type, control_port, local_sf_threads))
                sf_thread.start()

                if sf_thread.is_alive():

                    local_sf_threads[sfname]['thread'] = sf_thread
                    local_sf_threads[sfname]['data_plane_control_port'] = control_port
                    control_port += 1
                    get_agent_globals().set_data_plane_control_port(control_port)
                    logger.info("SF thread %s start successfully", sfname)
                else:
                    local_sf_threads.pop(sfname, None)
                    logger.error("Failed to start SF thread %s", sfname)

    return jsonify({'sf': local_sf_topo[sfname]}), 201


@app.route('/config/service-function:service-functions/service-function/<sfname>',
           methods=['DELETE'])
def delete_sf(sfname):
    logger.info("Received request for SF deletion: %s", sfname)
    local_sf_topo = get_agent_globals().get_sf_threads()
    local_sf_threads = get_agent_globals().get_sf_threads()

    try:
        if sfname in local_sf_threads.keys():
            kill_sf_thread(sfname)
        local_sf_topo.pop(sfname, None)
    except KeyError:
        msg = "SF name {} not found, message".format(sfname)
        logger.warning(msg)
        return msg, 404
    except:
        logger.warning("Unexpected exception, re-raising it")
        raise
    return '', 204


@app.route('/config/service-function-forwarder:service-function-forwarders/'
           'service-function-forwarder/<sffname>', methods=['PUT', 'POST'])
def create_sff(sffname):
    """
    This function creates a SFF on-the-fly when it receives a PUT request from
    ODL. The SFF runs on a separate thread. If a SFF thread with same name
    already exist it is killed before a new one is created. This happens when a
    SFF is modified or recreated

    :param sffname: SFF name
    :type sffname: str

    """
    if not request.json:
        abort(400)

    local_sff_threads = get_sff_threads()
    if sffname in local_sff_threads.keys():
        kill_sff_thread(sffname)

    r_json = request.get_json()
    local_sff_topo = agent_globals.get_sff_topo()

    local_sff_topo[sffname] = r_json['service-function-forwarder'][0]
    sff_port = (local_sff_topo[sffname]['sff-data-plane-locator']
                                       [0]
                                       ['data-plane-locator']
                                       ['port'])

    control_port = agent_globals.get_data_plane_control_port()
    sff_thread = Thread(target=start_sff,
                        args=(sffname, "0.0.0.0",
                              sff_port, control_port, local_sff_threads))

    local_sff_threads[sffname] = {}
    local_sff_threads[sffname]['thread'] = sff_thread
    local_sff_threads[sffname]['data_plane_control_port'] = control_port

    sff_thread.start()
    if sff_thread.is_alive():
        logger.info("SFF thread %s start successfully", sffname)
    else:
        logger.error("Failed to start SFF thread %s", sffname)

    control_port += 1
    agent_globals.set_data_plane_control_port(control_port)

    return jsonify({'sff': agent_globals.get_sff_topo()}), 201


def kill_sff_thread(sffname):
    """
    This function kills a SFF thread
    :param sffname:
    :return:
    """

    local_sff_threads = get_sff_threads()
    logger.info("Killing thread for SFF: %s", sffname)
    # Yes, we will come up with a better protocol in the future....
    message = "Kill thread".encode(encoding="UTF-8")

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(message, (SFF_UDP_IP, local_sff_threads[sffname]['data_plane_control_port']))
    if local_sff_threads[sffname]['thread'].is_alive():
        local_sff_threads[sffname]['thread'].join()
    if not local_sff_threads[sffname]['thread'].is_alive():
        logger.info("Thread for SFF %s is dead", sffname)
        # We need to close the socket used by thread here as well or we get an address reuse error. This is probably
        # some bug in asyncio since it should be enough for the SFF thread to close the socket.
        local_sff_threads[sffname]['socket'].close()
        local_sff_threads.pop(sffname, None)
        # udpserver_socket.close()


def kill_sf_thread(sfname):
    """
    This function kills a SF thread
    :param sffname:
    :return:
    """

    local_sf_threads = get_agent_globals().get_sf_threads()
    logger.info("Killing thread for SF: %s", sfname)
    # Yes, we will come up with a better protocol in the future....
    message = "Kill thread".encode(encoding="UTF-8")

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(message, (SFF_UDP_IP, local_sf_threads[sfname]['data_plane_control_port']))
    if local_sf_threads[sfname]['thread'].is_alive():
        local_sf_threads[sfname]['thread'].join()
    if not local_sf_threads[sfname]['thread'].is_alive():
        logger.info("Thread for SF %s is dead", sfname)
        # We need to close the socket used by thread here as well or we get an address reuse error. This is probably
        # some bug in asyncio since it should be enough for the SFF thread to close the socket.
        local_sf_threads[sfname]['socket'].close()
        local_sf_threads.pop(sfname, None)
        # udpserver_socket.close()


@app.route('/config/service-function-forwarder:service-function-forwarders/service-function-forwarder/<sffname>',
           methods=['DELETE'])
def delete_sff(sffname):
    """
    Deletes SFF from topology, kills associated thread  and if necessary remove all SFPs that depend on it
    :param sffname: SFF name
    :return:
    """

    local_sff_topo = get_agent_globals().get_sff_topo()
    local_sff_threads = get_sff_threads()

    try:
        if sffname in local_sff_threads.keys():
            kill_sff_thread(sffname)
        local_sff_topo.pop(sffname, None)
        if sffname == get_agent_globals().get_my_sff_name():
            reset_path()
            get_agent_globals().reset_data_plane_path()
    except KeyError:
        msg = "SFF name {} not found, message".format(sffname)
        logger.warning(msg)
        return msg, 404
    except:
        logger.warning("Unexpected exception, re-raising it")
        raise
    return '', 204


@app.route('/config/service-function-forwarder:service-function-forwarders/', methods=['PUT', 'POST'])
def create_sffs():
    local_sff_topo = get_agent_globals().get_sff_topo()
    if not request.json:
        abort(400)
    else:
        local_sff_topo = {
            'service-function-forwarders': request.json['service-function-forwarders']
        }
    return jsonify({'sff': local_sff_topo}), 201


@app.route('/config/service-function-forwarder:service-function-forwarders/', methods=['DELETE'])
def delete_sffs():
    """
    Delete all SFFs, SFPs, RSPs
    :return:
    """

    # We always use accessors
    get_agent_globals().reset_sff_topo()
    reset_path()
    get_agent_globals().reset_data_plane_path()
    return jsonify({'sff': get_agent_globals().get_sff_topo()}), 201


@app.route('/config/ietf-acl:access-lists/access-list/<aclname>', methods=['PUT', 'POST'])
def apply_one_acl(aclname):
    if sys.platform.startswith('linux'):
        nfq_class_manager = get_nfq_class_manager_ref()
        try:
            logger.debug("apply_one_acl: nfq_class_manager=%s", nfq_class_manager)

            # check nfq
            if not nfq_class_manager:
                return "NFQ not running. Received acl data has been ignored", 500

            if not request.json:
                abort(400)
            else:
                acl_item = request.get_json()["access-list"][0]

                result = nfq_class_manager.compile_one_acl(acl_item)

                if len(result) > 0:
                    return "Acl compiled with errors. " + str(result), 201
                else:
                    return "Acl compiled", 201

        except:
            logger.exception('apply_one_acl: exception')
            raise
    else:
        return "ACLs only supported on Linux Platforms", 403


@app.errorhandler(404)
def page_not_found(e):
    return render_template('404.html'), 404


def get_sff_sf_locator(odl_ip_port, sff_name, sf_name):
    """
    #TODO: add description
    #TODO: add arguments description and type

    """
    try:
        print("Getting SFF information from ODL... \n")
        odl_dataplane_url = SFF_SF_DATA_PLANE_LOCATOR_URL.format(odl_ip_port,
                                                                 sff_name,
                                                                 sf_name)

        s = requests.Session()
        r = s.get(odl_dataplane_url, auth=(USERNAME, PASSWORD), stream=False)
    except:
        print('Can\'t get SFF information from ODL')
        return

    if r.ok:
        r_json = r.json()
        sff_json = r_json['service-function-forwarders']

        local_sff_topo = agent_globals.get_sff_topo()
        for sff in sff_json['service-function-forwarder']:
            local_sff_topo[sff['name']] = sff
    else:
        print("=>Failed to GET SFF from ODL \n")


def get_sffs_from_odl(odl_ip_port):
    """
    Retrieves the list of configured SFFs from ODL and update global
    dictionary of SFFs

    :param odl_ip_port: ODL IP and port
    :type odl_ip_port: str

    :return Nothing

    """
    try:
        print("Getting SFFs configured in ODL... \n")
        odl_sff_url = SFF_PARAMETER_URL.format(odl_ip_port)

        s = requests.Session()
        r = s.get(odl_sff_url, auth=(USERNAME, PASSWORD), stream=False)
    except:
        print('Can\'t get SFFs from ODL; HINT: is ODL up and running?')
        return

    if r.ok:
        r_json = r.json()
        sff_json = r_json['service-function-forwarders']

        agent_globals.reset_sff_topo()
        local_sff_topo = agent_globals.get_sff_topo()
        for sff in sff_json['service-function-forwarder']:
            local_sff_topo[sff['name']] = sff
    else:
        print("=>Failed to GET SFFs from ODL \n")

    l_topo = agent_globals.get_sff_topo()
    if l_topo:
        pprint(l_topo)


def get_sff_from_odl(odl_ip_port, sff_name):
    """
    Retrieves a single configured SFF from ODL and update global dictionary of
    SFFs

    :param odl_ip_port: ODL IP and port
    :type odl_ip_port: str
    :param sff_name: SFF name
    :type sff_name: str

    :return int or None

    """
    try:
        print('Contacting ODL about information for SFF: %s' % sff_name)
        odl_sff_url = SFF_NAME_PARAMETER_URL.format(odl_ip_port, sff_name)

        s = requests.Session()
        r = s.get(odl_sff_url, auth=(USERNAME, PASSWORD), stream=False)
    except:
        print('Can\'t get SFF "{sff}" from ODL'.format(sff=sff_name))
        return

    if r.ok:
        r_json = r.json()

        local_sff_topo = agent_globals.get_sff_topo()
        local_sff_topo[sff_name] = r_json['service-function-forwarder'][0]
        return 0
    else:
        print("=>Failed to GET SFF {sff} from ODL \n".format(sff=sff_name))
        return -1


def auto_sff_name():
    """
    This function will iterate over all interfaces on the system and compare
    their IP addresses with the IP data plane locators of all SFFs downloaded
    from ODL. If a match is found, we set the name of this SFF as the SFF name
    configured in ODL. This allow the same script with the same parameters to
    the run on different machines.

    :return int

    """
    for intf in netifaces.interfaces():
        addr_list_dict = netifaces.ifaddresses(intf)
        inet_addr_list = addr_list_dict[netifaces.AF_INET]

        for value in inet_addr_list:
            sff_name = find_sff_locator_by_ip(value['addr'])
            if sff_name:
                agent_globals.set_my_sff_name(sff_name)
                sff_name = agent_globals.get_my_sff_name()

                logger.info("Auto SFF name is: %s \n", sff_name)
                return 0
    else:
        logger.error("Could not determine SFF name \n")
        return -1


def main():
    """Create a CLI parser for the SFC Agent and execute appropriate actions"""
    #: default values
    global ODLIP
    agent_port = 5000
    odl_auto_sff = False
    ovs_local_sff_cp_ip = '0.0.0.0'

    #: setup parser -----------------------------------------------------------
    parser = argparse.ArgumentParser(description='SFC Agent',
             usage=("\npython3.4 sfc_agent "
                    "--rest "
                    "--nfq-class "
                    "--odl-get-sff "
                    "--ovs-sff-cp-ip <local SFF IP dataplane address> "
                    "--odl-ip-port=<ODL REST IP:port> --sff-name=<my SFF name>"
                    " --agent-port=<agent listening port>"
                    "\n\nnote:\nroot privileges are required "
                    "if `--nfq-class` flag is used"))

    parser.add_argument('--odl-get-sff', action='store_true',
                        help='Get SFF from ODL')

    parser.add_argument('--auto-sff-name', action='store_true',
                        help='Automatically get SFF name')

    parser.add_argument('--nfq-class', action='store_true',
                        help='Flag to use NFQ Classifier')

    parser.add_argument('-r', '--rest', action='store_true',
                        help='Flag to use REST')

    parser.add_argument('--sff-name',
                        help='Set SFF name')

    parser.add_argument('--odl-ip-port',
                        help='Set ODL IP and port in form <IP>:<PORT>. '
                             'Default is %s' % ODLIP)

    parser.add_argument('--ovs-sff-cp-ip',
                        help='Set local SFF Open vSwitch IP. '
                             'Default is %s' % ovs_local_sff_cp_ip)

    parser.add_argument('--sff-os', choices=['XE', 'OVS'],
                        help='Set SFF switch OS')

    parser.add_argument('--agent-port', type=int,
                        help='Set SFC Agent port. Default is %s' % agent_port)

    #: parse CMD arguments ----------------------------------------------------
    args = parser.parse_args()

    if args.odl_ip_port is not None:
        ODLIP = args.odl_ip_port

    if args.agent_port is not None:
        agent_port = args.agent_port

    if args.ovs_sff_cp_ip is not None:
        ovs_local_sff_cp_ip = args.ovs_sff_cp_ip

    if args.auto_sff_name:
        odl_auto_sff = True
        args.odl_get_sff = True

    if args.sff_name is not None:
        agent_globals.set_my_sff_name(args.sff_name)

    if args.sff_os is not None:
        sff_os = args.sff_os

        agent_globals.set_sff_os(sff_os)
        if sff_os not in agent_globals.sff_os_set:
            logger.error(sff_os + ' is an unsupported SFF switch OS')
            sys.exit()

        if sff_os == "OVS":
            ovs_cli.init_ovs()

    #: execute actions --------------------------------------------------------
    if args.odl_get_sff:
        get_sffs_from_odl(ODLIP)

    if odl_auto_sff:
        auto_sff_name()

    if args.nfq_class:
        if sys.platform.startswith('linux'):
            start_nfq_classifier(True)

    if args.rest:
        app.run(port=agent_port,
                host=ovs_local_sff_cp_ip,
                debug=False,
                use_reloader=False)

if __name__ == "__main__":
    main()

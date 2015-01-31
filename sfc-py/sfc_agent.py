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

import logging
import socket
import netifaces
from sff_thread import start_sff
from threading import Thread
from sff_globals import *
import collections
from flask import *
import getopt
import json
import requests
import sys

if sys.platform.startswith('linux'):
    from nfq_class_thread import *
import xe_cli
import ovs_cli

app = Flask(__name__)

# SFF data plane listens to commands on this port.
sff_control_port = 6000

# IP address of associated SFF thread. WE assume agent and thread and co-located
SFF_UDP_IP = "127.0.0.1"

logger = logging.getLogger(__name__)


def tree():
    return collections.defaultdict(tree)


def find_sf_locator(sf_name, sff_name):
    """
    Looks for the SF name  within the service function
    dictionary of sff_name. If found, return the
    corresponding data plane locator

    :param sf_name: SF name
    :param  sff_name: SFF name
    :return: SF data plane locator
    """

    local_sff_topo = get_sff_topo()
    sf_locator = {}
    if sff_name not in local_sff_topo.keys():
        if get_sff_from_odl(ODLIP, sff_name) != 0:
            return None
    service_dictionary = local_sff_topo[sff_name]['service-function-dictionary']
    for service_function in service_dictionary:
        if sf_name == service_function['name']:
            sf_locator['ip'] = service_function['sff-sf-data-plane-locator']['ip']
            sf_locator['port'] = service_function['sff-sf-data-plane-locator']['port']
            return sf_locator
    logger.error("Failed to find data plane locator for SF: %s", sf_name)
    return None


def find_sff_locator_by_ip(addr):
    """
    For a given IP addr iterate over all SFFs looking for which one has a
    the same data plane locator ip
    :param addr:
    :return: SFF name
    """

    local_sff_topo = get_sff_topo()
    for sff_name, sff_value in local_sff_topo.items():
        for i, locator_value in enumerate(sff_value['sff-data-plane-locator']):
            if locator_value['data-plane-locator']['ip'] == addr:
                return sff_name


def find_sff_locator(sff_name):
    """
    For a given SFF name, look into local SFF topology for a match
    and returns the corresponding data plane locator. If SFF is not known
    tries to retrieve it from ODL
    :param sff_name:
    :return: SFF data plane locator
    """

    local_sff_topo = get_sff_topo()
    sff_locator = {}
    if sff_name not in local_sff_topo.keys():
        if get_sff_from_odl(ODLIP, sff_name) != 0:
            return None

    sff_locator['ip'] = local_sff_topo[sff_name]['sff-data-plane-locator'][0]['data-plane-locator']['ip']
    sff_locator['port'] = local_sff_topo[sff_name]['sff-data-plane-locator'][0]['data-plane-locator']['port']
    return sff_locator


@app.route('/operational/rendered-service-path:rendered-service-paths/', methods=['GET'])
def get_paths():
    return jsonify(path)


@app.route('/operational/data-plane-path:data-plane-paths/', methods=['GET'])
def get_data_plane_paths():
    return jsonify(data_plane_path)


@app.route('/config/service-function-forwarder:service-function-forwarders/', methods=['GET'])
def get_sffs():
    return jsonify(sff_topo)

@app.route('/operational/service-function-forwarder:service-function-forwarders-state/threads', methods=['GET'])
def get_sffs_thread_state():
    return jsonify(sff_threads)


@app.route('/operational/rendered-service-path:rendered-service-paths/', methods=['PUT', 'POST'])
def create_paths():
    if not request.json:
        abort(400)
    else:
        path_json = {
            'rendered-service-paths': request.json['rendered-service-paths']
        }

        # reset path data
        local_data_plane_path = get_data_plane_path()
        local_path = get_path()

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

    local_data_plane_path = get_data_plane_path()

    for service_hop in service_path['rendered-service-path-hop']:

        if service_hop['service-function-forwarder'] == get_agent_globals().get_my_sff_name():
            if service_path['path-id'] not in data_plane_path.keys():
                local_data_plane_path[service_path['path-id']] = {}
            local_data_plane_path[service_path['path-id']][service_hop['service-index']] = \
                find_sf_locator(service_hop['service-function-name'], service_hop['service-function-forwarder'])
        else:
            # If SF resides in another SFF, the locator is just the data plane
            # locator of that SFF.
            if service_path['path-id'] not in local_data_plane_path.keys():
                local_data_plane_path[service_path['path-id']] = {}
            local_data_plane_path[service_path['path-id']][service_hop['service-index']] = \
                find_sff_locator(service_hop['service-function-forwarder'])

    return


@app.route('/operational/rendered-service-path:rendered-service-paths/rendered-service-path/<sfpname>',
           methods=['PUT', 'POST'])
def create_path(sfpname):
    # global path
    local_path = get_path()
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
        build_data_plane_service_path(local_path[sfpname])
        # Testing XE cli processing module
        if local_sff_os == 'XE':
            logger.info("Provisioning %s SFF", local_sff_os)
            xe_cli.process_xe_cli(data_plane_path)
        elif local_sff_os == 'OVS':
            logger.info("Provisioning %s SFF", local_sff_os)
            # process_ovs_cli(data_plane_path)
            ovs_cli.process_ovs_sff__cli(data_plane_path)
        elif local_sff_os == "ODL":
            logger.info("Provisioning %s SFF", local_sff_os)
        else:
            logger.error("Unknown SFF OS: %s", local_sff_os)  # should never get here
            # json_string = json.dumps(data_plane_path)
    return jsonify(local_path), 201


@app.route('/operational/rendered-service-path:rendered-service-paths/rendered-service-path/<sfpname>',
           methods=['DELETE'])
def delete_path(sfpname):
    local_data_plane_path = get_data_plane_path()
    local_path = get_path()
    try:
        sfp_id = local_path[sfpname]['path-id']
        local_data_plane_path.pop(sfp_id, None)
        local_path.pop(sfpname, None)

        # remove nfq classifier for this path
        if sys.platform.startswith('linux'):
            nfq_class_manager = get_nfq_class_manager_ref()
            if nfq_class_manager:
                nfq_class_manager.destroy_packet_forwarder(sfp_id)

        json_string = json.dumps(data_plane_path)
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
    return '', 200


@app.route('/config/service-function:service-functions/service-function/<sfname>',
           methods=['DELETE'])
def delete_sf(sfname):
    logger.info("Received request for SF deletion: %s", sfname)
    return '', 200


@app.route('/config/service-function-forwarder:service-function-forwarders/service-function-forwarder/<sffname>',
           methods=['PUT', 'POST'])
def create_sff(sffname):
    """
    This function creates a SFF on-the-fly when it receives a PUT request from ODL. The SFF runs on a
    separate thread. If a SFF thread with same name already exist it is killed before a new one is created.
    This happens when a SFF is modified or recreated
    :param sffname: SFF name
    :return:
    """
    # global sff_topo
    local_sff_topo = get_sff_topo()
    global sff_control_port
    local_sff_threads = get_sff_threads()

    if not request.json:
        abort(400)
    else:
        if sffname in local_sff_threads.keys():
            kill_sff_thread(sffname)
        local_sff_topo[sffname] = request.get_json()['service-function-forwarder'][0]
        sff_port = local_sff_topo[sffname]['sff-data-plane-locator'][0]['data-plane-locator']['port']
        sff_thread = Thread(target=start_sff, args=(sffname, "0.0.0.0", sff_port, sff_control_port, local_sff_threads))

        local_sff_threads[sffname] = {}
        local_sff_threads[sffname]['thread'] = sff_thread
        local_sff_threads[sffname]['sff_control_port'] = sff_control_port

        sff_thread.start()

        sff_control_port += 1

    return jsonify({'sff': sff_topo}), 201


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
    sock.sendto(message, (SFF_UDP_IP, local_sff_threads[sffname]['sff_control_port']))
    if local_sff_threads[sffname]['thread'].is_alive():
        local_sff_threads[sffname]['thread'].join()
    if not local_sff_threads[sffname]['thread'].is_alive():
        logger.info("Thread for SFF %s is dead", sffname)
        # We need to close the socket used by thread here as well or we get an address reuse error. This is probably
        # some bug in asyncio since it should be enough for the SFF thread to close the socket.
        local_sff_threads[sffname]['socket'].close()
        local_sff_threads.pop(sffname, None)
        # udpserver_socket.close()


@app.route('/config/service-function-forwarder:service-function-forwarders/service-function-forwarder/<sffname>',
           methods=['DELETE'])
def delete_sff(sffname):
    """
    Deletes SFF from topology, kills associated thread  and if necessary remove all SFPs that depend on it
    :param sffname: SFF name
    :return:
    """

    local_sff_topo = get_sff_topo()
    local_sff_threads = get_sff_threads()

    try:
        if sffname in local_sff_threads.keys():
            kill_sff_thread(sffname)
        local_sff_topo.pop(sffname, None)
        if sffname == get_agent_globals().get_my_sff_name():
            reset_path()
            reset_data_plane_path()
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
    global sff_topo
    if not request.json:
        abort(400)
    else:
        sff_topo = {
            'service-function-forwarders': request.json['service-function-forwarders']
        }
    return jsonify({'sff': sff_topo}), 201


@app.route('/config/service-function-forwarder:service-function-forwarders/', methods=['DELETE'])
def delete_sffs():
    """
    Delete all SFFs, SFPs, RSPs
    :return:
    """

    # We always use accessors
    reset_sff_topo()
    reset_path()
    reset_data_plane_path()
    return jsonify({'sff': sff_topo}), 201


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
    # global sff_topo
    local_sff_topo = get_sff_topo()
    s = requests.Session()
    print("Getting SFF information from ODL... \n")
    r = s.get(SFF_SF_DATA_PLANE_LOCATOR_URL.format(odl_ip_port, sff_name, sf_name), stream=False,
              auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        sff_json = json.loads(r.text)['service-function-forwarders']['service-function-forwarder']
        for sff in sff_json:
            local_sff_topo[sff['name']] = sff
    else:
        print("=>Failed to GET SFF from ODL \n")


def get_sffs_from_odl(odl_ip_port):
    """
    Retrieves the list of configured SFFs from ODL and update global dictionary of SFFs
    :return: Nothing
    """

    # global sff_topo
    local_sff_topo = get_sff_topo()
    s = requests.Session()
    print("Getting SFFs configure in ODL... \n")
    r = s.get(SFF_PARAMETER_URL.format(odl_ip_port), stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        sff_json = json.loads(r.text)['service-function-forwarders']['service-function-forwarder']
        for sff in sff_json:
            local_sff_topo[sff['name']] = sff
    else:
        print("=>Failed to GET SFF from ODL \n")


def get_sff_from_odl(odl_ip_port, sff_name):
    """
    Retrieves a single configured SFF from ODL and update global dictionary of SFFs
    :return: Nothing
    """

    # global sff_topo
    local_sff_topo = get_sff_topo()
    s = requests.Session()
    logger.info("Contacting ODL to get information for SFF: %s \n", sff_name)
    r = s.get(SFF_NAME_PARAMETER_URL.format(odl_ip_port, sff_name), stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        local_sff_topo[sff_name] = json.loads(r.text)['service-function-forwarder'][0]
        return 0
    else:
        print("=>Failed to GET SFF from ODL \n")
        return -1


def auto_sff_name():
    """
    This function will iterate over all interfaces on the system and compare their IP addresses
    with the IP data plane locators of all SFFs downloaded from ODL. If a match is found, we set the name of this
    SFF as the SFF name configured in ODL. This allow the same script with the same parameters to the run on different
    machines

    """
    sff_name = ""
    intfs = netifaces.interfaces()
    for intf in intfs:
        addr_list_dict = netifaces.ifaddresses(intf)
        inet_addr_list = addr_list_dict[netifaces.AF_INET]
        for i, value in enumerate(inet_addr_list):
            sff_name = find_sff_locator_by_ip(value['addr'])
            if sff_name:
                get_agent_globals().set_my_sff_name(sff_name)
                logger.info("Auto SFF name is: %s \n", get_agent_globals().get_my_sff_name())
                return 0
    if not sff_name:
        logger.error("Could not determine SFF name \n")
        return -1


def main(argv):
    global ODLIP

    ovs_local_sff_cp_ip = '0.0.0.0'

    try:
        logging.basicConfig(level=logging.DEBUG)
        logger.setLevel(level=logging.INFO)
        opt, args = getopt.getopt(argv, "hr",
                                  ["help", "rest", "nfq-class", "auto-sff-name", "odl-get-sff", "odl-ip-port=",
                                   "sff-name=", "agent-port=", "ovs-sff-cp-ip=", "sff-os="])
    except getopt.GetoptError:
        print("sfc_agent --help | --nfq-class | --rest | --ovs-sff-cp-ip | --odl-get-sff | --odl-ip-port | --sff-name" +
              " | --agent-port | --sff-os (XE | OVS)")
        sys.exit(2)

    odl_get_sff = False
    odl_auto_sff = False
    agent_port = 5000
    rest = False
    nfq_class = False
    for opt, arg in opt:
        logger.debug(opt)
        if opt == "--odl-get-sff":
            odl_get_sff = True
            continue

        if opt == "--odl-ip-port":
            ODLIP = arg
            continue

        if opt in ('-h', '--help'):
            print(
                "sfc_agent --rest --nfq-class --odl-get-sff --ovs-sff-cp-ip=<local SFF IP dataplane address>" +
                "--odl-ip-port=<ODL REST IP:port> --sff-name=<my SFF name>" "--agent-port=<agent listening port>")
            sys.exit()

        if opt in ('-r', '--rest'):
            rest = True

        if opt == "--auto-sff-name":
            odl_get_sff = True
            odl_auto_sff = True
            continue

        if opt == "--sff-name":
            get_agent_globals().set_my_sff_name(arg)

        if opt == "--ovs-sff-cp-ip":
            ovs_local_sff_cp_ip = arg

        if opt == "--nfq-class":
            nfq_class = True

        if opt == "--agent-port":
            agent_port = int(arg)

        if opt == "--sff-os":
            local_sff_os = arg.upper()
            get_agent_globals().set_sff_os(local_sff_os)
            if local_sff_os not in get_agent_globals().sff_os_set:
                logger.error(local_sff_os + ' is an unsupported SFF switch OS')
                sys.exit()

            if local_sff_os == "OVS":
                ovs_cli.init_ovs()

    if odl_get_sff:
        get_sffs_from_odl(ODLIP)

    if odl_auto_sff:
        auto_sff_name()

    if nfq_class:
        if sys.platform.startswith('linux'):
            start_nfq_classifier(True)

    if rest:
        app.run(host=ovs_local_sff_cp_ip, debug=False, port=agent_port, use_reloader=False)


if __name__ == "__main__":
    main(sys.argv[1:])

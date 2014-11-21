import collections

__author__ = "Paul Quinn, Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "paulq@cisco.com, rapenno@gmail.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

""" SFF REST URI module. This file is imported from main SFF. """

import logging
import socket

from flask import *
import json
import requests

app = Flask(__name__)
logger = logging.getLogger(__name__)

# Globals

my_topo  = {}
sff_topo = {}

# Contains all Paths in JSON format as received from ODL
path = {}

# Contains all SFP in a format easily consumable by the data plane when
# processing NSH packets. data_plane_path[sfp-id][sfp-index] will return the
# locator of the SF/SFF.
#data_plane_path = {}
# For unit testing, without ODL uncomment following line
data_plane_path = {1: {1: {'port': 4789, 'ip': '10.100.100.2'}, 2: {'port': 4789, 'ip': '10.100.100.1'}, 3: {'port': 4789, 'ip': '10.100.100.1'}}}

# Contains the name of this SFF. For example, SFF1
my_sff_name = ""

# ODL IP:port
ODLIP = "127.0.0.1:8181"
# Static URLs for testing
SF_URL = "http://" + ODLIP + "/restconf/config/service-function:service-functions/"
SFC_URL = "http://" + ODLIP + "/restconf/config/service-function-chain:service-function-chains/"
SFF_URL = "http://" + ODLIP + "/restconf/config/service-function-forwarder:service-function-forwarders/"
SFT_URL = "http://" + ODLIP + "/restconf/config/service-function-type:service-function-types/"
SFP_URL = "http://" + ODLIP + "/restconf/config/service-function-path:service-function-paths/"

SFF_PARAMETER_URL = "http://{}/restconf/config/service-function-forwarder:service-function-forwarders/"

SFF_NAME_PARAMETER_URL = "http://{}/restconf/config/service-function-forwarder:service-function-forwarders/" + \
                         "service-function-forwarder/{}"
SFF_SF_DATA_PLANE_LOCATOR_URL = "http://{}/restconf/config/service-function-forwarder:service-function-forwarders/" + \
                                "service-function-forwarder/{}/service-function-dictionary/{}/" + \
                                "sff-sf-data-plane-locator/"

USERNAME = "admin"
PASSWORD = "admin"


def tree():
    return collections.defaultdict(tree)


# This function does not work if machine has more than one IP/interface
def get_my_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    s.connect(('8.8.8.8', 80))
    myip = (s.getsockname()[0])
    s.close()
    myip = "http://" + myip + ":/paths"
    return myip


def find_sf_locator(sf_name, sff_name):
    """
    Looks for the SF name  within the service function
    dictionary of sff_name. If found, return the
    corresponding data plane locator

    :param sf_name: SF name
    :param  sff_name: SFF name
    :return: SF data plane locator
    """
    sf_locator = {}
    if sff_name not in sff_topo.keys():
        if get_sff_from_odl(ODLIP, sff_name) != 0:
            return None
    service_dictionary = sff_topo[sff_name]['service-function-dictionary']
    for service_function in service_dictionary:
        if sf_name == service_function['name']:
            sf_locator['ip'] = service_function['sff-sf-data-plane-locator']['ip']
            sf_locator['port'] = service_function['sff-sf-data-plane-locator']['port']
            return sf_locator
    logger.error("Failed to find data plane locator for SF: %s", sf_name)
    return None


def find_sff_locator(sff_name):
    """
    For a given SFF name, look into local SFF topology for a match
    and returns the corresponding data plane locator. If SFF is not known
    tries to retrieve it from ODL
    :param sff_name:
    :return: SFF data plane locator
    """

    sff_locator = {}
    if sff_name not in sff_topo.keys():
        if get_sff_from_odl(ODLIP, sff_name) != 0:
            return None

    sff_locator['ip'] = sff_topo[sff_name]['sff-data-plane-locator'][0]['data-plane-locator']['ip']
    sff_locator['port'] = sff_topo[sff_name]['sff-data-plane-locator'][0]['data-plane-locator']['port']
    return sff_locator


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
    return jsonify({'path': path}), 201


def build_data_plane_service_path(service_path):
    """
    Builds a dictionary of the local attached Service Functions
    :param service_path: A single Service Function Path
    :return:
    """

    for service_hop in service_path['service-path-hop']:

        if service_hop['service-function-forwarder'] == my_sff_name:
            if service_path['path-id'] not in data_plane_path.keys():
                data_plane_path[service_path['path-id']] = {}
            data_plane_path[service_path['path-id']][service_hop['service_index']] = \
                find_sf_locator(service_hop['service-function-name'], service_hop['service-function-forwarder'])
        else:
            # If SF resides in another SFF, the locator is just the data plane
            # locator of that SFF.
            if service_path['path-id'] not in data_plane_path.keys():
                data_plane_path[service_path['path-id']] = {}
            data_plane_path[service_path['path-id']][service_hop['service_index']] = \
                find_sff_locator(service_hop['service-function-forwarder'])

    return


@app.route('/config/service-function-path:service-function-paths/service-function-path/<sfpname>', methods=['PUT'])
def create_path(sfpname):
    global path
    if not request.json:
        abort(400)
    else:
        # print json.dumps(sfpjson)
        # sfpj_name = sfpjson["service-function-path"][0]['name']
        path[sfpname] = request.get_json()["service-function-path"][0]
        logger.info("Building Service Path for path: %s", sfpname)
        build_data_plane_service_path(path[sfpname])
        # json_string = json.dumps(data_plane_path)
        #SFF_UDP_IP = "127.0.0.1"
        #SFF_UDP_PORT = 6000

        #sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        #sock.sendto(bytes(json_string, 'UTF-8'), (SFF_UDP_IP, SFF_UDP_PORT))
    return jsonify({'path': path}), 201


@app.route('/config/service-function-path:service-function-paths/service-function-path/<sfpname>', methods=['DELETE'])
def delete_path(sfpname):
    global path
    try:
        sfp_id = path[sfpname]['path-id']
        data_plane_path.pop(sfp_id, None)
        path.pop(sfpname, None)
        json_string = json.dumps(data_plane_path)
        # SFF_UDP_IP = "127.0.0.1"
        #SFF_UDP_PORT = 6000

        #sock = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
        #sock.sendto(json_string, (SFF_UDP_IP, SFF_UDP_PORT))
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
    return jsonify({'sff': sff_topo}), 201


@app.route('/config/service-function-forwarder:service-function-forwarders/service-function-forwarder/<sffname>',
           methods=['DELETE'])
def delete_sff(sffname):
    """
    Deletes SFF from topology and if necessary remove all SFPs that depend on it
    :param sffname: SFF name
    :return:
    """
    global sff_topo
    global path
    global data_plane_path
    try:
        sff_topo.pop(sffname, None)
        if sffname == my_sff_name:
            path = {}
            data_plane_path = {}
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
    return jsonify({'sff': sff_topo}), 201


@app.route('/config/service-function-forwarder:service-function-forwarders/', methods=['DELETE'])
def delete_sffs():
    """
    Delete all SFFs, SFPs
    :return:
    """
    global sff_topo
    global path
    global data_plane_path
    sff_topo = {}
    path = {}
    data_plane_path = {}
    return jsonify({'sff': sff_topo}), 201


@app.errorhandler(404)
def page_not_found(e):
    return render_template('404.html'), 404


def get_sff_sf_locator(odl_ip_port, sff_name, sf_name):
    global sff_topo
    s = requests.Session()
    print("Getting SFF information from ODL... \n")
    r = s.get(SFF_SF_DATA_PLANE_LOCATOR_URL.format(odl_ip_port, sff_name, sf_name), stream=False,
              auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        sff_json = json.loads(r.text)['service-function-forwarders']['service-function-forwarder']
        for sff in sff_json:
            sff_topo[sff['name']] = sff
    else:
        print("=>Failed to GET SFF from ODL \n")


def get_sffs_from_odl(odl_ip_port):
    """
    Retrieves the list of configured SFFs from ODL and update global dictionary of SFFs
    :return: Nothing
    """
    global sff_topo
    s = requests.Session()
    print("Getting SFF information from ODL... \n")
    r = s.get(SFF_PARAMETER_URL.format(odl_ip_port), stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        sff_json = json.loads(r.text)['service-function-forwarders']['service-function-forwarder']
        for sff in sff_json:
            sff_topo[sff['name']] = sff
    else:
        print("=>Failed to GET SFF from ODL \n")


def get_sff_from_odl(odl_ip_port, sff_name):
    """
    Retrieves a single configured SFF from ODL and update global dictionary of SFFs
    :return: Nothing
    """
    global sff_topo
    s = requests.Session()
    print("Getting SFF information from ODL... \n")
    r = s.get(SFF_NAME_PARAMETER_URL.format(odl_ip_port, sff_name), stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        sff_topo[sff_name] = json.loads(r.text)['service-function-forwarder'][0]
        return 0
    else:
        print("=>Failed to GET SFF from ODL \n")
        return -1


def flask_bootstrap(sff_name, odl_ip_port):
    """
    This function is called from SFF main module as the entry point
    of flask thread
    :param sff_name:
    :param odl_ip_port:
    :return:
    """
    global ODLIP
    global my_sff_name
    app.run(host='0.0.0.0')

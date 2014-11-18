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

""" SFF REST Server. This Server should be co-located python reference SFF implementation """

import logging
import socket

from flask import *
import sys
import getopt
import json
import requests

app = Flask(__name__)

# Globals

my_topo = {}
sff_topo = {}
path = {}
data_plane_path = {}
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

logger = logging.getLogger(__name__)


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

    :param sf_name: SF name
    :param  sff_name: SFF name
    :return: SF data plane locator
    """

    if sff_name not in sff_topo.keys():
        if get_sff_from_odl(ODLIP, sff_name) != 0:
            return None
    service_dictionary = sff_topo[sff_name]['service-function-dictionary']
    for service_function in service_dictionary:
        if sf_name == service_function['name']:
            return service_function['sff-sf-data-plane-locator']['ip']
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

    if sff_name not in sff_topo.keys():
        if get_sff_from_odl(ODLIP, sff_name) != 0:
            return None

    locator = sff_topo[sff_name]['sff-data-plane-locator'][0]['data-plane-locator']['ip']
    return locator


def mytopo(nextsffloc, vxlanid):
    global my_topo
    if nextsffloc in my_topo.keys():
        return my_topo[nextsffloc]
    else:
        vxlan = 'vxlan' + str(vxlanid)
        my_topo[nextsffloc] = vxlan
        vxlanid += 1
        return vxlanid


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
        # if any(sff_topo):
        # ovsbuildit(path)

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
        build_data_plane_service_path(path[sfpname])
        json_string = json.dumps(data_plane_path)
        SFF_UDP_IP = "127.0.0.1"
        SFF_UDP_PORT = 6000

        sock = socket.socket(socket.AF_INET, # Internet
                     socket.SOCK_DGRAM) # UDP
        sock.sendto(json_string, (SFF_UDP_IP, SFF_UDP_PORT))

    # if any(sff_topo):
    # ovsbuild_one_path(path[sfpname])
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
    # if any(path):
    # ovsbuild_one_path(path)
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
    return jsonify({'sff': sff_topo}), 201


@app.route('/config/service-function-forwarder:service-function-forwarders/', methods=['DELETE'])
def delete_sffs():
    global sff_topo
    sff_topo = {}
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


def main(argv):
    global ODLIP
    global my_sff_name
    try:
        logging.basicConfig(level=logging.INFO)
        opt, args = getopt.getopt(argv, "hr", ["help", "rest", "sff-name=", "odl-get-sff", "odl-ip-port="])
    except getopt.GetoptError:
        print("rest2ovs --help | --rest | --sff-name | --odl-get-sff | --odl-ip-port")
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
            print("rest2ovs -m cli | rest --odl-get-sff --odl-ip-port")
            sys.exit()

        if opt in ('-r', '--rest'):
            rest = True

        if opt == "--sff-name":
            my_sff_name = arg

    if odl_get_sff:
        get_sffs_from_odl(ODLIP)

    if rest:
        app.debug = True
        app.run(host='0.0.0.0')


if __name__ == "__main__":
    main(sys.argv[1:])

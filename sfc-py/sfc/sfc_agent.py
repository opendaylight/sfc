#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import os
import sys
import flask
import signal
import logging
import argparse
import requests
import netifaces
import json

from urllib.parse import urlparse


# fix Python 3 relative imports inside packages
# CREDITS: http://stackoverflow.com/a/6655098/4183498
parent_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(1, parent_dir)
import sfc  # noqa
__package__ = 'sfc'


from sfc.common import classifier
from sfc.cli import xe_cli, xr_cli, ovs_cli
from sfc.common import sfc_globals as _sfc_globals
from sfc.common.launcher import start_sf, stop_sf, start_sff, stop_sff


__author__ = "Paul Quinn, Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.4"
__email__ = "paulq@cisco.com, rapenno@gmail.com"
__status__ = "alpha"


"""
SFC Agent Server. This Server should be co-located with the python SFF data
plane implementation (sff_thread.py)
"""

app = flask.Flask(__name__)
logger = logging.getLogger(__file__)
nfq_classifier = classifier.NfqClassifier()
sfc_globals = _sfc_globals.sfc_globals


def _sff_present(sff_name, local_sff_topo):
    """
    Check if SFF data plain locator is present in the local SFF topology

    :param sff_name: SFF name
    :type sff_name: str
    :param local_sff_topo: local SFF topology
    :type local_sff_topo: dict

    :return bool

    """
    sff_present = True

    if sff_name not in local_sff_topo.keys():
        if get_sff_from_odl(sfc_globals.get_odl_locator(), sff_name) != 0:
            logger.error("Failed to find data plane locator for SFF: %s",
                         sff_name)

            sff_present = False

    return sff_present


def _sf_local_host(sf_name):
    """
    Check if SFC agent controls this SF. The check is done based on the
    rest-uri hostname against the local IP addresses

    :param sf_name: SF name
    :type sf_name: str

    :return bool

    """
    sf_hosted = False
    local_sf_topo = sfc_globals.get_sf_topo()
    if (sf_name in local_sf_topo.keys()) or (get_sf_from_odl(sfc_globals.get_odl_locator(), sf_name) == 0):
        ip_addr, _ = urlparse(local_sf_topo[sf_name]['rest-uri']).netloc.split(':')
        if _ip_local_host(ip_addr):
            sf_hosted = True

    return sf_hosted


def _ip_local_host(ip_addr):
    """
    This function will iterate over all interfaces on the system and compare
    their IP addresses with the one given as a parameter

    :param ip_addr: IP addr
    :type ip_addr: str

    :return int

    """
    for intf in netifaces.interfaces():
        addr_list_dict = netifaces.ifaddresses(intf)
        # Some interfaces have no address
        if addr_list_dict:
            # Some interfaces have no IPv4 address.
            if netifaces.AF_INET in addr_list_dict:
                inet_addr_list = addr_list_dict[netifaces.AF_INET]
                for value in inet_addr_list:
                    if value['addr'] == ip_addr:
                        return True
            if netifaces.AF_INET6 in addr_list_dict:
                inet_addr_list = addr_list_dict[netifaces.AF_INET6]
                for value in inet_addr_list:
                    if value['addr'] == ip_addr:
                        return True

    return False


def find_sf_locator(sf_name, sff_name):
    """
    Looks for the SF name within the service function dictionary of sff_name.
    If SFF is not present in local SFF topology it is requested from ODL.

    Return the corresponding SF data plane locator or None if not found.

    :param sf_name: SF name
    :type sf_name: str
    :param sff_name: SFF name
    :type sff_name: str

    :return sf_locator: A dictionary with keys 'ip' and 'port'

    """
    sf_locator = {}
    local_sff_topo = sfc_globals.get_sff_topo()

    if not _sff_present(sff_name, local_sff_topo):
        return sf_locator

    service_dict = local_sff_topo[sff_name]['service-function-dictionary']
    for service_function in service_dict:
        if sf_name == service_function['name']:
            _sf_locator = service_function['sff-sf-data-plane-locator']

            # A locator might use something other than IP
            if 'ip' in _sf_locator:
                sf_locator['ip'] = _sf_locator['ip']
                sf_locator['port'] = _sf_locator['port']

            return sf_locator

    if not sf_locator:
        logger.error("Failed to find data plane locator for SF: %s", sf_name)

    return sf_locator


def find_sff_locator(sff_name):
    """
    For a given SFF name, look into local SFF topology for a match and returns
    the corresponding data plane locator. If SFF is not known tries to retrieve
    it from ODL.

    :param sff_name: SFF name
    :type sff_name: str

    :return sff_locator: A dictionary with keys 'ip' and 'port'

    """
    sff_locator = {}
    local_sff_topo = sfc_globals.get_sff_topo()

    if not _sff_present(sff_name, local_sff_topo):
        return sff_locator

    _sff_locator = local_sff_topo[sff_name]['sff-data-plane-locator'][0]
    sff_locator['ip'] = _sff_locator['data-plane-locator']['ip']
    sff_locator['port'] = _sff_locator['data-plane-locator']['port']

    return sff_locator


def find_sff_locator_by_ip(addr):
    """
    For a given IP address iterate over all SFFs looking for which one has a
    the same data plane locator IP

    :param addr: IP address
    :type addr: str

    :return str or None

    """
    local_sff_topo = sfc_globals.get_sff_topo()

    for sff_name, sff_value in local_sff_topo.items():
        try:
            for locator_value in sff_value['sff-data-plane-locator']:
                if locator_value['data-plane-locator']['ip'] == addr:
                    return sff_name
                else:
                    continue
        except KeyError:
            continue

    return None


def build_data_plane_service_path(service_path):
    """
    Builds a dictionary of the local attached Service Functions

    :param service_path: a single Service Function Path
    :type service_path:

    :return int

    """
    sp_id = service_path['path-id']
    local_data_plane_path = sfc_globals.get_data_plane_path()
    sfc_globals.set_sfp_parent_path(service_path['parent-service-function-path'])
    for service_hop in service_path['rendered-service-path-hop']:
        sh_index = service_hop['service-index']
        sh_sff = service_hop['service-function-forwarder']

        if sh_sff == sfc_globals.get_my_sff_name():
            # SF is reachable by my SFF
            if sp_id not in local_data_plane_path.keys():
                local_data_plane_path[sp_id] = {}

            sf_locator = find_sf_locator(service_hop['service-function-name'],
                                         sh_sff)

            if sf_locator:
                local_data_plane_path[sp_id][sh_index] = sf_locator
                sf_name = service_hop['service-function-name']
                check_and_start_sf_thread(sf_name)

            else:
                logger.error("Failed to build rendered service path: %s",
                             service_path['name'])
                return -1
        else:
            # If SF resides in another SFF, the locator is just the data plane
            # locator of that SFF.
            if sp_id not in local_data_plane_path.keys():
                local_data_plane_path[sp_id] = {}

            sff_locator = find_sff_locator(sh_sff)
            if sff_locator:
                local_data_plane_path[sp_id][sh_index] = sff_locator
            else:
                logger.error("Failed to build rendered service path: %s",
                             service_path['name'])
                return -1

    return 0


def check_and_start_sf_thread(sf_name):
    """
    Checks whether a SF is local and its thread has been started. If thread is
    not running we start it.

    :param sf_name: Service Function Name
    :type sf_name: str
    :return int

    """
    if _sf_local_host(sf_name):
        local_sf_threads = sfc_globals.get_sf_threads()
        if sf_name not in local_sf_threads.keys():
            local_sf_topo = sfc_globals.get_sf_topo()
            _, sf_type = (local_sf_topo[sf_name]['type']).split(':')
            data_plane_locator_list = local_sf_topo[sf_name]['sf-data-plane-locator']
            for data_plane_locator in data_plane_locator_list:
                if ("ip" in data_plane_locator) and ("port" in data_plane_locator):
                    sf_port = data_plane_locator['port']
                    start_sf(sf_name, "0.0.0.0", sf_port, sf_type)


def check_nfq_classifier_state():
    """
    Check if the NFQ classifier is running, log an error and abort otherwise
    """
    if not nfq_classifier.nfq_running():
        logger.warning('Classifier is not running: ignoring ACL')
        flask.abort(500)


@app.errorhandler(404)
def page_not_found(e):
    return 'Not found', 404


@app.route('/config/ietf-acl:access-lists/access-list/<acl_name>',
           methods=['PUT', 'POST'])
def apply_acl(acl_name):
    check_nfq_classifier_state()
    logger.info("Received request from ODL to create ACL ...")

    if not flask.request.json:
        logger.error('Received ACL is empty, aborting ...')
        flask.abort(400)

    try:
        r_json = flask.request.get_json()
        with open("jsonputACL.txt", "w") as outfile:
            json.dump(r_json, outfile)
        nfq_classifier.process_acl(r_json)
    except:
        return '', 500

    return '', 201


@app.route('/config/ietf-acl:access-lists/access-list/<acl_name>',
           methods=['DELETE'])
def remove_acl(acl_name):
    check_nfq_classifier_state()
    logger.info("Received request from ODL to delete ACL ...")
    acl_data = {'access-list': [{'access-list-entries': [{'delete': True}],
                                 'acl-name': acl_name}]}

    nfq_classifier.process_acl(acl_data)
    return '', 204


@app.route('/operational/rendered-service-path:rendered-service-paths/'
           'rendered-service-path/<rsp_name>', methods=['DELETE'])
def delete_path(rsp_name):
    status_code = 204
    not_found_msg = 'RSP "%s" not found' % rsp_name
    logger.info("Received request from ODL to delete RSP ...")
    local_path = sfc_globals.get_path()
    local_data_plane_path = sfc_globals.get_data_plane_path()

    try:
        sfp_id = local_path[rsp_name]['path-id']
        local_data_plane_path.pop(sfp_id, None)
        local_path.pop(rsp_name, None)

        if nfq_classifier.nfq_running():
            nfq_classifier.remove_rsp(rsp_name)

    except KeyError:
        logger.error(not_found_msg)
        status_code = 404

    return '', status_code


@app.route('/operational/rendered-service-path:rendered-service-paths/',
           methods=['GET'])
def get_paths():
    logger.info("Received request from ODL to send RSs ...")
    return flask.jsonify(sfc_globals.get_path())


@app.route('/operational/rendered-service-path:rendered-service-paths/',
           methods=['PUT', 'POST'])
def create_paths():
    if not flask.request.json:
        flask.abort(400)

    # reset path data
    sfc_globals.reset_data_plane_path()
    local_path = sfc_globals.get_path()
    logger.info("Received request from ODL to create RSPs ...")
    r_json = flask.request.get_json()
    with open("jsonputRSPs.txt", "w") as outfile:
        json.dump(r_json, outfile)
    rsps = flask.request.json['rendered-service-paths']
    for path_item in rsps:
        local_path[path_item['name']] = path_item
        # rebuild path data
        build_data_plane_service_path(path_item)

    return flask.jsonify({'path': sfc_globals.path}), 201


@app.route('/config/service-function-metadata:service-function-metadata/',
           methods=['GET'])
def get_odl_metadata():
    logger.info("Requesting ODL for Metadata")
    return flask.jsonify(sfc_globals.get_odl_metadata()())


@app.route('/config/service-function-metadata:service-function-metadata/',
           methods=['PUT', 'POST'])
def set_odl_metadata():
    logger.info("Received request for Metadata creation")
    if not flask.request.json:
        flask.abort(400)
        logger.warning("=>Failed to GET Metadata from ODL \n")
    r_json = flask.request.get_json()
    with open("jsonputMDT.txt", "w") as outfile:
        json.dump(r_json, outfile)
    metadata_json = flask.request.get_json()['service-function-metadata']
    sfc_globals.reset_odl_metadata()
    sfc_globals.set_odl_metadata(metadata_json)


@app.route('/operational/data-plane-path:data-plane-paths/',
           methods=['GET'])
def get_data_plane_paths():
    logger.info("Received request from ODL to send DPs ...")
    return flask.jsonify(sfc_globals.get_data_plane_path())


@app.route('/config/service-function:service-functions/service-function/'
           '<sfname>', methods=['PUT', 'POST'])
def create_sf(sfname):
    logger.info("Received request for SF creation: %s", sfname)

    if not flask.request.json:
        flask.abort(400)

    logger.info("Received request from ODL to create SF ...")
    local_sf_topo = sfc_globals.get_sf_topo()
    r_json = flask.request.get_json()
    with open("jsonputSF.txt", "w") as outfile:
        json.dump(r_json, outfile)
    local_sf_topo[sfname] = flask.request.get_json()['service-function'][0]
    data_plane_locator_list = local_sf_topo[sfname]['sf-data-plane-locator']

    for data_plane_locator in data_plane_locator_list:
        if ("ip" in data_plane_locator) and ("port" in data_plane_locator):
            sf_port = data_plane_locator['port']
            _, sf_type = (local_sf_topo[sfname]['type']).split(':')
            sf_ip = data_plane_locator['ip']
            # TODO: We need more checks to make sure IP in locator actually
            # corresponds to one of the existing interfaces in the system
            # start_sf(sfname, "0.0.0.0", sf_port, sf_type)
            start_sf(sfname, sf_ip, sf_port, sf_type)
    return flask.jsonify({'sf': local_sf_topo[sfname]}), 201


@app.route('/config/service-function:service-functions/service-function/'
           '<sfname>', methods=['DELETE'])
def delete_sf(sfname):
    logger.info("Received request for SF deletion: %s", sfname)

    status_code = 204
    local_sf_topo = sfc_globals.get_sf_topo()
    local_sf_threads = sfc_globals.get_sf_threads()
    logger.info("Received request from ODL to delete SF ...")
    try:
        if sfname in local_sf_threads.keys():
            stop_sf(sfname)

        local_sf_topo.pop(sfname)

    except KeyError:
        logger.warning("SF name %s not found", sfname)
        status_code = 404

    return '', status_code


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
    if not flask.request.json:
        flask.abort(400)

    logger.info("Received request from ODL to create SFF ...")
    local_sff_threads = sfc_globals.get_sff_threads()
    if sffname in local_sff_threads.keys():
        stop_sff(sffname)

    r_json = flask.request.get_json()
    with open("jsonputSFF.txt", "w") as outfile:
            json.dump(r_json, outfile)
    local_sff_topo = sfc_globals.get_sff_topo()

    local_sff_topo[sffname] = r_json['service-function-forwarder'][0]
    sff_port = (local_sff_topo[sffname]['sff-data-plane-locator'][0]['data-plane-locator']['port'])
    sff_ip = (local_sff_topo[sffname]['sff-data-plane-locator'][0]['data-plane-locator']['ip'])
    nfq_classifier.set_fwd_socket(sff_ip)
    start_sff(sffname, sff_ip, sff_port)

    return flask.jsonify({'sff': sfc_globals.get_sff_topo()}), 201


@app.route('/config/service-function-forwarder:service-function-forwarders/'
           'service-function-forwarder/<sffname>', methods=['DELETE'])
def delete_sff(sffname):
    """
    Deletes SFF from topology, kills associated thread and if necessary remove
    all SFPs that depend on it

    :param sffname: SFF name
    :type sffname: str

    """
    status_code = 204
    local_sff_topo = sfc_globals.get_sff_topo()
    local_sff_threads = sfc_globals.get_sff_threads()
    logger.info("Received request from ODL to delete SFF ...")
    try:
        if sffname in local_sff_threads.keys():
            stop_sff(sffname)

        if sffname == sfc_globals.get_my_sff_name():
            sfc_globals.reset_path()
            sfc_globals.reset_data_plane_path()

        local_sff_topo.pop(sffname)

    except KeyError:
        logger.warning('SFF name %s not found', sffname)
        status_code = 404

    return '', status_code


@app.route('/config/service-function-forwarder:service-function-forwarders/',
           methods=['GET'])
def get_sffs():
    logger.info("Received request from ODL to send SFFs ...")
    return flask.jsonify(sfc_globals.get_sff_topo())


@app.route('/operational/service-function-forwarder:'
           'service-function-forwarders-state/threads', methods=['GET'])
def get_sffs_threads():
    serialized_threads = {}
    local_sff_threads = sfc_globals.get_sff_threads()
    logger.info("Received request from ODL to send SFFs topo ...")
    for key, value in local_sff_threads.items():
        if value['thread'].is_alive():
            serialized_threads[key] = {}
            serialized_threads[key]['socket'] = str(value['socket'])
            serialized_threads[key]['thread'] = str(value['thread'])

    return flask.jsonify(serialized_threads)


@app.route('/config/service-function-forwarder:service-function-forwarders/',
           methods=['PUT', 'POST'])
def create_sffs():
    if not flask.request.json:
        flask.abort(400)

    logger.info("Received request from ODL to create SFFs ...")
    r_json = flask.request.get_json()
    with open("jsonputSFFs.txt", "w") as outfile:
        json.dump(r_json, outfile)
    sffs = 'service-function-forwarders'
    local_sff_topo = {
        sffs: flask.request.json[sffs]
    }

    return flask.jsonify({'sff': local_sff_topo}), 201


@app.route('/config/service-function-forwarder:service-function-forwarders/',
           methods=['DELETE'])
def delete_sffs():
    """
    Delete all SFFs, SFPs, RSPs
    """
    logger.info("Received request from ODL to delete SFFs ...")
    # We always use accessors
    sfc_globals.reset_sff_topo()
    sfc_globals.reset_path()
    sfc_globals.reset_data_plane_path()

    return flask.jsonify({'sff': sfc_globals.get_sff_topo()}), 201


def get_sff_sf_locator(odl_ip_port, sff_name, sf_name):
    """
    #TODO: add description
    #TODO: add arguments description and type

    """
    try:
        logger.info("Getting SFF information from ODL ...")
        url = _sfc_globals.SFF_SF_DATA_PLANE_LOCATOR_URL
        odl_dataplane_url = url.format(odl_ip_port, sff_name, sf_name)

        s = requests.Session()
        r = s.get(odl_dataplane_url, auth=sfc_globals.get_odl_credentials(),
                  stream=False)
    except (requests.exceptions.ConnectionError,
            requests.exceptions.RequestException) as exc:
        logger.exception('Can\'t get SFF {} data plane from ODL. Error: {}',
                         exc)
        return

    if r.ok:
        r_json = r.json()
        with open("jsongetSFF_DPL.txt", "w") as outfile:
            json.dump(r_json, outfile)
        sff_json = r_json['service-function-forwarders']

        local_sff_topo = sfc_globals.get_sff_topo()
        for sff in sff_json['service-function-forwarder']:
            local_sff_topo[sff['name']] = sff
    else:
        logger.warning("=>Failed to GET SFF from ODL \n")


def get_metadata_from_odl(odl_ip_port):
    """
    Retrieves the metadata from ODL and update global
    dictionary of metadata

    :param odl_ip_port: ODL IP and port
    :type odl_ip_port: str

    :return Nothing

    """
    try:
        logger.info("Getting Metadata configured in ODL ...")
        url = _sfc_globals.METADATA_URL
        odl_sff_url = url.format(odl_ip_port)

        s = requests.Session()
        r = s.get(odl_sff_url, auth=sfc_globals.get_odl_credentials(),
                  stream=False)
    except requests.exceptions.ConnectionError as e:
        logger.exception('Can\'t get SFFs from ODL. Error: {}'.format(e))
        return
    except requests.exceptions.RequestException as e:
        logger.exception('Can\'t get SFFs from ODL. Error: {}'.format(e))
        return

    if r.ok:
        r_json = r.json()
        with open("jsongetMDT.txt", "w") as outfile:
            json.dump(r_json, outfile)
        metadata_json = r_json['service-function-metadata']

        sfc_globals.reset_odl_metadata()
        sfc_globals.set_odl_metadata(metadata_json)
    else:
        logger.warning("=>Failed to GET Metadata from ODL \n")


def get_sfp_from_odl(odl_ip_port):
    """
    Retrieves the sfp from ODL and update global
    dictionary of sfp

    :param odl_ip_port: ODL IP and port
    :type odl_ip_port: str

    :return Nothing

    """
    try:
        logger.info("Getting SFP configured in ODL ...")
        url = _sfc_globals.SFP_NAME_PARAMETER_URL
        odl_sfp_url = url.format(odl_ip_port)

        s = requests.Session()
        r = s.get(odl_sfp_url, auth=sfc_globals.get_odl_credentials(),
                  stream=False)
    except requests.exceptions.ConnectionError as e:
        logger.exception('Can\'t get SFPs from ODL. Error: {}'.format(e))
        return
    except requests.exceptions.RequestException as e:
        logger.exception('Can\'t get SFPs from ODL. Error: {}'.format(e))
        return

    if r.ok:
        r_json = r.json()
        with open("jsongetSFP.txt", "w") as outfile:
            json.dump(r_json, outfile)
        sfp_json = r_json['service-function-paths']

        sfc_globals.reset_sfp_topo()
        local_sfp = sfc_globals.get_sfp_topo()
        try:
            for sfp in sfp_json['service-function-path']:
                local_sfp[sfp['name']] = sfp
            sfc_globals.set_sfp_topo(local_sfp)
        except KeyError:
            logger.info("=>No configured SFP in ODL \n")
    else:
        logger.warning("=>Failed to GET SFP from ODL \n")


def get_sffs_from_odl(odl_ip_port):
    """
    Retrieves the list of configured SFFs from ODL and update global
    dictionary of SFFs

    :param odl_ip_port: ODL IP and port
    :type odl_ip_port: str

    :return Nothing

    """

    try:
        logger.info("Getting SFFs configured in ODL ...")
        url = _sfc_globals.SFF_PARAMETER_URL
        odl_sff_url = url.format(odl_ip_port)

        s = requests.Session()
        r = s.get(odl_sff_url, auth=sfc_globals.get_odl_credentials(),
                  stream=False)
    except requests.ConnectionError as e:
        logger.warning("Not able to get SFFs from ODL: {}".format(e.args[0]))
        return
    except ConnectionRefusedError as e:     # noqa
        logger.warning("Not able to get SFFs from ODL: {}".format(e.args[0]))
        return
    except OSError as e:
        for i in enumerate(e.args):
            logger.warning('Not able to get SFFs from ODL. Error: {}'.format((i[1].args[1])))
        return

    if r.ok:
        r_json = r.json()
        with open("jsongetSFFs.txt", "w") as outfile:
            json.dump(r_json, outfile)
        sff_json = r_json['service-function-forwarders']
        sfc_globals.reset_sff_topo()
        local_sff_topo = sfc_globals.get_sff_topo()
        try:
            for sff in sff_json['service-function-forwarder']:
                local_sff_topo[sff['name']] = sff
        except KeyError:
            logger.info("=>No configured SFFs in ODL \n")
    else:
        logger.warning("=>Failed to GET SFFs from ODL \n")


def get_sf_from_odl(odl_ip_port, sf_name):
    """
    Retrieves a single configured SF from ODL and update global dictionary of
    SFs

    :param odl_ip_port: ODL IP and port
    :type odl_ip_port: str
    :param sf_name: SF name
    :type sf_name: str

    :return int or None

    """
    try:
        logger.info('Contacting ODL about information for SF: %s' % sf_name)
        url = _sfc_globals.SF_NAME_PARAMETER_URL
        odl_sf_url = url.format(odl_ip_port, sf_name)

        s = requests.Session()
        r = s.get(odl_sf_url, auth=sfc_globals.get_odl_credentials(),
                  stream=False)
    except (requests.exceptions.ConnectionError,
            requests.exceptions.RequestException) as exc:
        logger.exception('Can\'t get SF "{}" from ODL. Error: {}',
                         sf_name, exc)
        return -1

    if r.ok:
        r_json = r.json()
        with open("jsongetSF.txt", "w") as outfile:
            json.dump(r_json, outfile)
        local_sf_topo = sfc_globals.get_sf_topo()
        local_sf_topo[sf_name] = r_json['service-function'][0]
        return 0
    else:
        logger.warning("=>Failed to GET SF {} from ODL \n".format(sf_name))
        return -1


@app.route('/operational/rendered-service-path:rendered-service-paths/'
           'rendered-service-path/<rsp_name>', methods=['PUT', 'POST'])
def create_path(rsp_name):
    if not flask.request.json:
        flask.abort(400)

    local_path = sfc_globals.get_path()
    local_sff_os = sfc_globals.get_sff_os()

    if (not sfc_globals.get_my_sff_name()) and auto_sff_name():
        logger.fatal("Could not determine my SFF name \n")
        sys.exit(1)
    local_path[rsp_name] = flask.request.get_json()["rendered-service-path"][0]
    logger.info("Building Service Path for path: %s", rsp_name)

    if not build_data_plane_service_path(local_path[rsp_name]):
        # Testing XE cli processing module
        if local_sff_os == 'XE':
            logger.info("Provisioning %s XE SFF", local_sff_os)
            xe_cli.process_xe_cli(sfc_globals.get_data_plane_path())

        elif local_sff_os == 'XR':
            logger.info("Provisioning %s XR SFF", local_sff_os)
            xr_cli.process_xr_cli(sfc_globals.get_data_plane_path())

        elif local_sff_os == 'OVS':
            logger.info("Provisioning %s SFF", local_sff_os)
            # process_ovs_cli(data_plane_path)
            ovs_cli.process_ovs_sff__cli(sfc_globals.get_data_plane_path())

        elif local_sff_os == "ODL":
            logger.info("Provisioning %s SFF", local_sff_os)

        # should never get here
        else:
            logger.error("Unknown SFF OS: %s", local_sff_os)
            # json_string = json.dumps(data_plane_path)
        logger.info("ODL locator %s ", sfc_globals.get_odl_locator())
        get_sfp_from_odl(sfc_globals.get_odl_locator())
        get_metadata_from_odl(sfc_globals.get_odl_locator())
        find_metadata()
        return flask.jsonify(local_path), 201
    else:
        msg = "Could not build service path: {}".format(rsp_name)
        return msg, 400


def find_metadata():
    """
    Serach for a metadata in the stored dictionary based on the key
    received in SFP
    """
    local_odl_metadata = sfc_globals.get_odl_metadata()
    local_sfp = sfc_globals.get_sfp_topo()

    local_parent_path = sfc_globals.get_sfp_parent_path()
    if local_parent_path:
        local_mtdt = local_sfp[local_parent_path]
        context_mtdt = 'context-metadata'
        variable_mtdt = 'variable-metadata'
        if context_mtdt in local_mtdt.keys():
            context_key = local_mtdt[context_mtdt]
            for mtdt in local_odl_metadata[context_mtdt]:
                if mtdt['name'] == context_key:
                    sfc_globals.sfp_context_metadata = mtdt
                    logger.info("New context metadata for SFP: '%s'", local_parent_path)
                    return
        if variable_mtdt in local_mtdt.keys():
            variable_key = local_mtdt[variable_mtdt]
            for mtdt in local_odl_metadata[variable_mtdt]:
                if mtdt['name'] == variable_key:
                    sfc_globals.sfp_variable_metadata = mtdt
                    logger.info("New variable metadata for SFP: '%s'", local_parent_path)


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
        logger.info('Contacting ODL about information for SFF: %s' % sff_name)
        url = _sfc_globals.SFF_NAME_PARAMETER_URL
        odl_sff_url = url.format(odl_ip_port, sff_name)

        s = requests.Session()
        r = s.get(odl_sff_url, auth=sfc_globals.get_odl_credentials(),
                  stream=False)
    except (requests.exceptions.ConnectionError,
            requests.exceptions.RequestException) as exc:
        logger.exception('Can\'t get SFF "{}" from ODL. Error: {}', sff_name, exc)
        return -1

    if r.ok:
        r_json = r.json()
        with open("jsongetSFF.txt", "w") as outfile:
            json.dump(r_json, outfile)
        local_sff_topo = sfc_globals.get_sff_topo()
        local_sff_topo[sff_name] = r_json['service-function-forwarder'][0]
        return 0
    else:
        logger.warning("=>Failed to GET SFF {} from ODL \n".format(sff_name))
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
        # Some interfaces have no address
        if addr_list_dict:
            # Some interfaces have no IPv4 address.
            if netifaces.AF_INET in addr_list_dict:
                inet_addr_list = addr_list_dict[netifaces.AF_INET]

                for value in inet_addr_list:
                    # logger.info('addr %s', value['addr'])
                    sff_name = find_sff_locator_by_ip(value['addr'])
                    if sff_name:
                        sfc_globals.set_my_sff_name(sff_name)
                        sff_name = sfc_globals.get_my_sff_name()

                        logger.info("Auto SFF name is: %s", sff_name)
                        return 0
            if netifaces.AF_INET6 in addr_list_dict:
                inet_addr_list = addr_list_dict[netifaces.AF_INET6]

                for value in inet_addr_list:
                    # logger.info('addr %s', value['addr'])
                    sff_name = find_sff_locator_by_ip(value['addr'])
                    if sff_name:
                        sfc_globals.set_my_sff_name(sff_name)
                        sff_name = sfc_globals.get_my_sff_name()

                        logger.info("Auto SFF name is: %s", sff_name)
                        return 0
    else:
        logger.warn("Could not determine SFF name. This means ODL is not running"
                    " or there is no SFF with a data plane locator IP that matches"
                    " one where the SFC agent is running. SFC Agent will retry later... \n")
        return -1


def main():
    """Create a CLI parser for the SFC Agent and execute appropriate actions"""
    #: default values
    agent_port = 5000
    odl_auto_sff = False
    ovs_local_sff_cp_ip = '0.0.0.0'
    debug_level = False

    #: setup parser -----------------------------------------------------------
    parser = argparse.ArgumentParser(description='SFC Agent',
                                     usage=("\npython3.4 sfc_agent "
                                            "--rest "
                                            "--nfq-class "
                                            "--odl-get-sff "
                                            "--debug_level "
                                            "--ovs-sff-cp-ip <local SFF IP dataplane address> "
                                            "--odl-ip-port=<ODL REST IP:port> --sff-name=<my SFF name>"
                                            "--sff-os=<agent os>"
                                            " --agent-port=<agent listening port>"
                                            "\n\nnote:\n"
                                            "root privileges are required "
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

    parser.add_argument('--debug-level', action='store_true',
                        help='Set logging level to DEBUG')

    parser.add_argument('--odl-ip-port',
                        help='Set ODL IP and port in form <IP>:<PORT>. '
                             'Default is %s' % sfc_globals.get_odl_locator())

    parser.add_argument('--ovs-sff-cp-ip',
                        help='Set local SFF Open vSwitch IP. '
                             'Default is %s' % ovs_local_sff_cp_ip)

    parser.add_argument('--sff-os', choices=['XE', 'XR', 'OVS'],
                        help='Set SFF switch OS')

    parser.add_argument('--agent-port', type=int,
                        help='Set SFC Agent port. Default is %s' % agent_port)

    #: parse CMD arguments ----------------------------------------------------
    args = parser.parse_args()

    if args.odl_ip_port is not None:
        sfc_globals.set_odl_locator(args.odl_ip_port)
        logger.info('ODL locator: %s', sfc_globals.get_odl_locator())

    if args.agent_port is not None:
        agent_port = args.agent_port

    if args.ovs_sff_cp_ip is not None:
        ovs_local_sff_cp_ip = args.ovs_sff_cp_ip

    if args.auto_sff_name:
        odl_auto_sff = True
        args.odl_get_sff = True

    if args.sff_name is not None:
        sfc_globals.set_my_sff_name(args.sff_name)

    if args.debug_level is not None:
        debug_level = args.debug_level

    if args.sff_os is not None:
        sff_os = args.sff_os

        sfc_globals.set_sff_os(sff_os)
        if sff_os not in sfc_globals.sff_os_set:
            logger.error(sff_os + ' is an unsupported SFF switch OS')
            sys.exit()

        if sff_os == "OVS":
            ovs_cli.init_ovs()

    #: execute actions --------------------------------------------------------
    try:
        if debug_level:
            logger.setLevel(logging.DEBUG)
        else:
            logger.setLevel(logging.INFO)

        logger.info("====== STARTING SFC AGENT ======")
        logger.info("SFC Agent will listen to Opendaylight REST Messages and take any "
                    "appropriate action such as creating, deleting, updating  SFs, SFFs, "
                    "or classifier. \n")

        if args.odl_get_sff:
            get_sffs_from_odl(sfc_globals.get_odl_locator())

        if odl_auto_sff:
            auto_sff_name()

        if args.nfq_class:
            classifier.start_classifier()

        if args.rest:
            app.run(port=agent_port,
                    host=ovs_local_sff_cp_ip,
                    debug=False,
                    use_reloader=False)
    except KeyboardInterrupt:
        pass
    finally:
        classifier.clear_classifier()

    # not a great way how to exit, but it works and prevents the
    # `Exception ignored in: ...` message from beeing displayed
    os.kill(os.getpid(), signal.SIGTERM)


if __name__ == "__main__":
    main()

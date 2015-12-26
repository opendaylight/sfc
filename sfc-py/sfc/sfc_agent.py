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
import argparse

# fix Python 3 relative imports inside packages
# CREDITS: http://stackoverflow.com/a/6655098/4183498
parent_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(1, parent_dir)
import sfc  # noqa

__package__ = 'sfc'

from sfc.common import classifier
from sfc.common.odl_api import *  # noqa
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


def build_data_plane_service_path(service_path):
    """
    Builds a dictionary of the local attached Service Functions

    :param service_path: a single Service Function Path
    :type service_path: dict

    :return int


    255, SF1, SFF1
    254, SF2, SFF2

    """

    sp_id = service_path['path-id']
    local_data_plane_path = sfc_globals.get_data_plane_path()
    sfc_globals.set_sfp_parent_path(service_path['parent-service-function-path'])
    prev_sff = None
    service_hop_list = service_path['rendered-service-path-hop']
    for list_index, service_hop in enumerate(service_hop_list):
        sh_index = service_hop['service-index']
        sh_sff = service_hop['service-function-forwarder']
        if prev_sff and (sh_sff != prev_sff):
            local_data_plane_path[prev_sff][sp_id][sh_index] = find_sff_locator(sh_sff)

        sf_locator = find_sf_locator(service_hop['service-function-name'],
                                     sh_sff)
        if sf_locator:
            if sh_sff not in local_data_plane_path:
                local_data_plane_path[sh_sff] = {}
            if sp_id not in local_data_plane_path[sh_sff]:
                local_data_plane_path[sh_sff][sp_id] = {}
            local_data_plane_path[sh_sff][sp_id][sh_index] = sf_locator
            sf_name = service_hop['service-function-name']
            check_and_start_sf_thread(sf_name)
        else:
            logger.error("Failed to build rendered service path: %s",
                         service_path['name'])
            return -1

        prev_sff = sh_sff

        # if sh_sff in sfc_globals.get_my_sff_name():
        #     # SF is reachable by (one of) my SFFs
        #     if sp_id not in local_data_plane_path.keys():
        #         local_data_plane_path[sp_id] = {}
        #
        #     sf_locator = find_sf_locator(service_hop['service-function-name'],
        #                                  sh_sff)
        #
        #     if sf_locator:
        #         local_data_plane_path[sp_id][sh_index] = sf_locator
        #         sf_name = service_hop['service-function-name']
        #         check_and_start_sf_thread(sf_name)
        #
        #     else:
        #         logger.error("Failed to build rendered service path: %s",
        #                      service_path['name'])
        #         return -1
        # else:
        #     # If SF resides in another SFF, the locator is just the data plane
        #     # locator of that SFF.
        #     if sp_id not in local_data_plane_path.keys():
        #         local_data_plane_path[sp_id] = {}
        #
        #     sff_locator = find_sff_locator(sh_sff)
        #     if sff_locator:
        #         local_data_plane_path[sp_id][sh_index] = sff_locator
        #     else:
        #         logger.error("Failed to build rendered service path: %s",
        #                      service_path['name'])
        #         return -1

    return 0


def check_and_start_sf_thread(sf_name):
    """
    Checks whether a SF is local and its thread has been started. If thread is
    not running we start it.

    :param sf_name: Service Function Name
    :type sf_name: str
    :return int

    """
    if sf_local_host(sf_name):
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


@app.route('/config/ietf-access-control-list:access-lists/acl/<acl_name>',
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


@app.route('/config/ietf-access-control-list:access-lists/acl/<acl_name>',
           methods=['DELETE'])
def remove_acl(acl_name):
    check_nfq_classifier_state()
    logger.info("Received request from ODL to delete ACL ...")
    acl_data = {'acl': [{'access-list-entries': [{'delete': True}], 'acl-name': acl_name}]}

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
        for key, sff_path in local_data_plane_path.iteritems():
            sff_path.pop(sfp_id, None)
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


@app.route('/config/service-function-path-metadata:service-function-metadata/',
           methods=['GET'])
def get_odl_metadata():
    logger.info("Requesting ODL for Metadata")
    return flask.jsonify(sfc_globals.get_odl_metadata()())


@app.route('/config/service-function-path-metadata:service-function-metadata/',
           methods=['PUT', 'POST'])
def set_odl_metadata():
    logger.info("Received request for Metadata creation")
    if not flask.request.json:
        flask.abort(400)
        logger.warning("=>Failed to PUT Metadata to ODL \n")
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

        if sffname in sfc_globals.get_my_sff_name():
            sfc_globals.reset_path()
            sfc_globals.reset_sff_data_plane_path()

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


def main():
    """Create a CLI parser for the SFC Agent and execute appropriate actions"""
    #: default values
    agent_port = 5000
    odl_auto_sff = False
    ovs_local_sff_cp_ip = '0.0.0.0'
    debug_level = False

    #: setup parser -----------------------------------------------------------
    parser = argparse.ArgumentParser(description='SFC Agent',
                                     usage=("\npython3.5 sfc_agent "
                                            "--rest "
                                            "--nfq-class "
                                            "--odl-get-sff "
                                            "--debug-level "
                                            "--NSH-type"
                                            "--legacy-vxlan"
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

    parser.add_argument('--NSH-type',
                        choices=['1', '3'],
                        help='Set NSH type '
                             'Default is %s' % sfc_globals.get_nsh_type())

    parser.add_argument('--legacy-vxlan', action='store_true',
                        help='Using Vxlan header instead of Vxlan-gpe')

    parser.add_argument('--ovs-sff-cp-ip',
                        help='Set local SFF Open vSwitch IP. '
                             'Default is %s' % ovs_local_sff_cp_ip)

    parser.add_argument('--sff-os',
                        choices=['XE', 'XR', 'OVS'],
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

    if args.NSH_type is not None:
        sfc_globals.set_nsh_type(args.NSH_type)

    if args.legacy_vxlan is not None:
        sfc_globals.set_legacy_vxlan(True)
    else:
        sfc_globals.set_legacy_vxlan(False)

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
            logging.basicConfig(level=logging.DEBUG)
        else:
            logging.basicConfig(level=logging.INFO)

        logger.info("\n\n====== STARTING SFC AGENT ======")
        logger.info("\n\nSFC Agent will listen to Opendaylight REST Messages and take any\n"
                    "appropriate action such as creating, deleting, updating  SFs, SFFs,\n "
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

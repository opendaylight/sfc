#
# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import logging
import requests
import netifaces
import json

from sfc.common import sfc_globals as _sfc_globals
from urllib.parse import urlparse

__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "rapenno@gmail.com"
__status__ = "alpha"

"""
SFC ODL API.
"""

sfc_globals = _sfc_globals.sfc_globals
logger = logging.getLogger(__file__)


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


def _get_sf(sf_name):
    """
    First try to get the SF from the local SF topology.
    If its not present locally, get it from the ODL and
    store it locally

    :param sf_name: SF name
    :type sf_name: str

    :return sf dictionary, may be None if not found

    """
    local_sf_topo = sfc_globals.get_sf_topo()
    sf = local_sf_topo.get(sf_name, None)
    if sf:
        return sf

    if get_sf_from_odl(sfc_globals.get_odl_locator(), sf_name) == 0:
        sf = local_sf_topo.get(sf_name, None)

    return sf


def sf_local_host(sf_name):
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

    service_list = local_sff_topo[sff_name]['service-function-dictionary']
    try:
        for sf in service_list:
            if sf.get('name') == sf_name:
                _sf_sff_locator = sf['sff-sf-data-plane-locator']

                # A locator might use something other than IP
                if 'sf-dpl-name' in _sf_sff_locator:
                    # We get the full SF
                    local_sf = _get_sf(sf_name)
                    if not local_sf:
                        return None

                    sf_dpl_list = local_sf['sf-data-plane-locator']
                    for dpl in sf_dpl_list:
                        if dpl.get('sf-dpl-name') == sf.get('sf-dpl-name'):
                            sf_dpl = dpl
                            if 'ip' in sf_dpl:
                                sf_locator['ip'] = sf_dpl['ip']
                                sf_locator['port'] = sf_dpl['port']
                                return sf_locator
    except (ValueError, KeyError):
        # if the entry is not found in the list, return an empty locator
        return sf_locator

    # if sf_list_entry:
    #     _sf_sff_locator = sf_list_entry['sff-sf-data-plane-locator']
    #
    #     # A locator might use something other than IP
    #     if 'sf-dpl-name' in _sf_sff_locator:
    #         local_sf = _get_sf(sf_name)
    #         if not local_sf:
    #             return None
    #
    #         sf_dpl_list = local_sf['sf-data-plane-locator']
    #         for dpl in sf_dpl_list:
    #             if dpl.get('sf-dpl-name') == sf.get('sf-dpl-name'):
    #                 sf_dpl = dpl
    #         if sf_dpl:
    #             if 'ip' in sf_dpl:
    #                 sf_locator['ip'] = sf_dpl['ip']
    #                 sf_locator['port'] = sf_dpl['port']
    #                 return sf_locator

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
    sff_name_list = []
    local_sff_topo = sfc_globals.get_sff_topo()

    for sff_name, sff_value in local_sff_topo.items():
        try:
            for locator_value in sff_value['sff-data-plane-locator']:
                if locator_value['data-plane-locator']['ip'] == addr:
                    sff_name_list.append(sff_name)
                    # return sff_name
                else:
                    continue
        except KeyError:
            continue

    return sff_name_list
    # return None


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
    sff_name = []

    for intf in netifaces.interfaces():
        addr_list_dict = netifaces.ifaddresses(intf)
        # Some interfaces have no address
        if addr_list_dict:
            # Some interfaces have no IPv4 address.
            if netifaces.AF_INET in addr_list_dict:
                inet_addr_list = addr_list_dict[netifaces.AF_INET]

                for value in inet_addr_list:
                    # logger.info('addr %s', value['addr'])
                    # We return the list of all SFFs that match
                    sff_name = find_sff_locator_by_ip(value['addr'])
                    if sff_name:
                        sfc_globals.set_my_sff_name(sff_name)
                        # sff_name = sfc_globals.get_my_sff_name()

                        # logger.info("Auto SFF name is: %s", sff_name)

            if netifaces.AF_INET6 in addr_list_dict:
                inet_addr_list = addr_list_dict[netifaces.AF_INET6]

                for value in inet_addr_list:
                    # logger.info('addr %s', value['addr'])
                    sff_name = find_sff_locator_by_ip(value['addr'])
                    if sff_name:
                        sfc_globals.set_my_sff_name(sff_name)
                        sff_name = sfc_globals.get_my_sff_name()

                        logger.info("Auto SFF name is: %s", sff_name)

    if not sff_name:
        logger.warn("\n\nCould not determine SFF name. This means ODL is not running \n"
                    "or there is no SFF with a data plane locator IP that matches \n"
                    "one where the SFC agent is running. SFC Agent will retry later... \n")
        return -1
    else:
        return 0


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
    except ConnectionRefusedError as e:  # noqa
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


def get_sff_sf_locator(odl_ip_port, sff_name, sf_name):
    """
    #TODO: add description
    #TODO: add arguments description and type
    :param sf_name: SF Name
    :type sf_name: str
    :param sff_name: SFF name
    :type sff_name: str
    :param odl_ip_port: ODL IP and port
    :type odl_ip_port: tuple

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

# flake8: noqa
#
# Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


import os
import sys
import json


__author__ = 'Dusan Madar'
__email__ = 'madar.dusan@gmail.com'
__copyright__ = 'Copyright(c) 2015, Cisco Systems, Inc.'
__version__ = '0.1'
__status__ = 'alpha'


"""
Common testing constants, mappings, functions; global pytest settings
"""


#: enable ../sfc-py/common files usage in tests
test_path = os.path.dirname(os.path.abspath(__file__))
test_path_parts = test_path.split(os.sep)
main_path = os.sep.join(test_path_parts[:-1])
sys.path.insert(0, main_path)


#: static URLs for (local) testing
ODL_PORT = 8181
ODL_IP = 'localhost'
LOCAL_ODL_LOCATOR = "{ip}:{port}".format(ip=ODL_IP, port=ODL_PORT)

base_url = "http://{odl_loc}/restconf/".format(odl_loc=LOCAL_ODL_LOCATOR)
cfg_url = base_url + "config/"
opr_url = base_url + "operations/"

SF_URL = cfg_url + "service-function:service-functions/"
SFT_URL = cfg_url + "service-function-type:service-function-types/"
SFP_URL = cfg_url + "service-function-path:service-function-paths/"
SFC_URL = cfg_url + "service-function-chain:service-function-chains/"
SFF_URL = cfg_url + "service-function-forwarder:service-function-forwarders/"
SCF_URL = cfg_url + "service-function-classifier:service-function-classifiers/"
IETF_ACL_URL = cfg_url + "ietf-acl:access-lists/"
RSP_RPC_URL = opr_url + "rendered-service-path:create-rendered-path"


#: map URLs to common test file names and HTTP methods
url_2_json_data = {SF_URL: {'file': 'service_functions',
                            'method': 'put'},
                   SFP_URL: {'file': 'service_path',
                             'method': 'put'},
                   SFC_URL: {'file': 'service_chains',
                             'method': 'put'},
                   SFF_URL: {'file': 'service_function_forwarders',
                             'method': 'put'},
                   RSP_RPC_URL: {'file': 'rendered_service_path_rpc',
                                 'method': 'post'}}


def get_test_files(target_direcotry=None):
    """
    Get all testing *.json files from 'data' directory.

    Create a mapping: file name (without extension) -> absolute file path.

    :param target_direcotry: absolute path to directory with testing files
    :type target_direcotry: str

    :returns dict

    """
    if target_direcotry is None:
        target_direcotry = os.path.join(os.path.dirname(__file__), 'data')

    test_files = []
    test_files_names = []

    for virl_file in os.listdir(target_direcotry):
        path_parts = os.path.splitext(virl_file)
        if path_parts[1] == '.json':
            test_files.append(os.path.join(target_direcotry, virl_file))
            test_files_names.append(path_parts[0])

    return dict(zip(test_files_names, test_files))


def read_json(json_file, output=dict):
    """
    Load JSON to dict

    Create a mapping: file name (without extension) -> absolute file path.

    :param json_file: absolute path to the JSON file
    :type json_file: str
    :param output: specify output format
    :type output: class

    :returns dict

    """
    with open(json_file, mode='r') as _json_file:
        if output is dict:
            json_data = json.load(_json_file)
        elif output is str:
            json_data = _json_file.read()

    return json_data

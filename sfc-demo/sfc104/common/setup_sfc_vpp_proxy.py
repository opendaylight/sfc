#!/usr/bin/python
import argparse
import requests,json
from requests.auth import HTTPBasicAuth
from string import Template
from subprocess import call
import time
import sys
import os

# Setup the SFC data model for VPP Proxied Service Functions
#   ServiceNode, SF, and SFF data models
# The rest are loaded in setup_sfc_common.py

controller=Template("$ODL_CONTROLLER").substitute(os.environ)
DEFAULT_PORT='8181'

USERNAME='admin'
PASSWORD='admin'

SFF1_CP_IP=Template("$SFF1_IP").substitute(os.environ)
SF1_CP_IP=Template("$SF1_IP").substitute(os.environ)
SF2_CP_IP=Template("$SF2_IP").substitute(os.environ)
SFF2_CP_IP=Template("$SFF2_IP").substitute(os.environ)
SFF1_DP_IP=Template("$SFF1_VPP_IP").substitute(os.environ)
SF1_DP_IP=Template("$SF1_VPP_IP").substitute(os.environ)
SF2_DP_IP=Template("$SF2_VPP_IP").substitute(os.environ)
SF2_PROXY_DP_IP=Template("$SF2_VPP_PROXY_IP").substitute(os.environ)
SFF2_DP_IP=Template("$SFF2_VPP_IP").substitute(os.environ)
CLASSIFIER1_CP_IP=Template("$CLASSIFIER1_IP").substitute(os.environ)
CLASSIFIER1_DP_IP=Template("$CLASSIFIER1_VPP_IP").substitute(os.environ)
CLASSIFIER2_CP_IP=Template("$CLASSIFIER2_IP").substitute(os.environ)
CLASSIFIER2_DP_IP=Template("$CLASSIFIER2_VPP_IP").substitute(os.environ)

proxies = {
    "http": None,
    "https": None
}

def put(host, port, uri, data, debug=False):
    """Perform a PUT rest operation, using the URL and data provided"""

    url='http://'+host+":"+port+uri

    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug == True:
        print "PUT %s" % url
        print json.dumps(data, indent=4, sort_keys=True)
    r = requests.put(url, proxies=proxies, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug == True:
        print r.text
    r.raise_for_status()
    time.sleep(5)

def get_service_nodes_uri():
    return "/restconf/config/service-node:service-nodes"

def get_service_nodes_data():
    return {
    "service-nodes": {
        "service-node": [
            {
                "name": "classifier1",
                "service-function": [
                ],
                "ip-mgmt-address": CLASSIFIER1_CP_IP
            },
            {
                "name": "sff1",
                "service-function": [
                ],
                "ip-mgmt-address": SFF1_CP_IP
            },
            {
                "name": "sf1",
                "service-function": [
                    "dpi-1"
                ],
                "ip-mgmt-address": SF1_CP_IP
            },
            {
                "name": "sf2",
                "service-function": [
                    "firewall-1"
                ],
                "ip-mgmt-address": SF2_CP_IP
            },
            {
                "name": "sff2",
                "service-function": [
                ],
                "ip-mgmt-address": SFF2_CP_IP
            },
            {
                "name": "classifier2",
                "service-function": [
                ],
                "ip-mgmt-address": CLASSIFIER2_CP_IP
            }
        ]
    }
}

def get_service_functions_uri():
    return "/restconf/config/service-function:service-functions"

def get_service_functions_data():
    return {
    "service-functions": {
        "service-function": [
            {
                "name": "dpi-1",
                "ip-mgmt-address": SF1_CP_IP,
                "type": "dpi",
                "sf-data-plane-locator": [
                    {
                        "name": "dpi-1-dpl",
                        "port": 4790,
                        "ip": SF1_DP_IP,
                        "transport": "service-locator:vxlan-gpe",
                        "service-function-forwarder": "SFF1"
                    }
                ]
            },
            {
                "name": "firewall-1",
                "ip-mgmt-address": SF2_CP_IP,
                "type": "firewall",
                "sf-data-plane-locator": [
                    {
                        "name": "firewall-1-dpl",
                        "port": 4789,
                        "ip": SF2_DP_IP,
                        "transport": "service-locator:vxlan",
                        "service-function-forwarder": "SFF2",
                        "service-function-proxy:proxy-data-plane-locator": {
                            "port": 4790,
                            "ip": SF2_PROXY_DP_IP,
                            "transport": "service-locator:vxlan-gpe"
                        }
                    }
                ]
            }
        ]
    }
}

def get_service_function_forwarders_uri():
    return "/restconf/config/service-function-forwarder:service-function-forwarders"

def get_service_function_forwarders_data():
    return {
    "service-function-forwarders": {
        "service-function-forwarder": [
            {
                "name": "CLASSIFIER1",
                "ip-mgmt-address": CLASSIFIER1_CP_IP,
                "service-node": "classifier1",
                "service-function-forwarder-vpp:sff-netconf-node-type": "netconf-node-type-honeycomb",
                "sff-data-plane-locator": [
                    {
                        "name": "classifier1-dpl",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 4790,
                            "ip": CLASSIFIER1_DP_IP
                        }
                    }
                ]
            },
            {
                "name": "SFF1",
                "ip-mgmt-address": SFF1_CP_IP,
                "service-node": "sff1",
                "service-function-forwarder-vpp:sff-netconf-node-type": "netconf-node-type-honeycomb",
                "sff-data-plane-locator": [
                    {
                        "name": "sff1-dpl",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 4790,
                            "ip": SFF1_DP_IP
                        }
                    }
                ],
                "service-function-dictionary": [
                    {
                        "name": "dpi-1",
                        "sff-sf-data-plane-locator": {
                             "sf-dpl-name": "dpi-1-dpl",
                             "sff-dpl-name": "sff1-dpl"
                        }
                    }
                ],
            },
            {
                "name": "SFF2",
                "ip-mgmt-address": SFF2_CP_IP,
                "service-node": "sff2",
                "service-function-forwarder-vpp:sff-netconf-node-type": "netconf-node-type-honeycomb",
                "sff-data-plane-locator": [
                    {
                        "name": "sff2-dpl",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 4790,
                            "ip": SFF2_DP_IP
                        }
                    }
                ],
                "service-function-dictionary": [
                    {
                        "name": "firewall-1",
                        "sff-sf-data-plane-locator": {
                            "sf-dpl-name": "firewall-1-dpl",
                            "sff-dpl-name": "sff2-dpl"
                        }
                    }
                ]
            },
            {
                "name": "CLASSIFIER2",
                "ip-mgmt-address": CLASSIFIER2_CP_IP,
                "service-node": "classifier2",
                "service-function-forwarder-vpp:sff-netconf-node-type": "netconf-node-type-honeycomb",
                "sff-data-plane-locator": [
                    {
                        "name": "classifier2-dpl",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 4790,
                            "ip": CLASSIFIER2_DP_IP
                        }
                    }
                ]
            }
        ]
    }
}

if __name__ == "__main__":

    print "sending service nodes"
    put(controller, DEFAULT_PORT, get_service_nodes_uri(), get_service_nodes_data(), True)
    print "sending service functions"
    put(controller, DEFAULT_PORT, get_service_functions_uri(), get_service_functions_data(), True)
    print "sending service function forwarders"
    put(controller, DEFAULT_PORT, get_service_function_forwarders_uri(), get_service_function_forwarders_data(), True)
    print "waiting till SFFs are connected successfully..."
    time.sleep(60)


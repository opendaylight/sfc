#!/usr/bin/python
import argparse
import requests,json
from requests.auth import HTTPBasicAuth
from string import Template
from subprocess import call
import time
import sys
import os

# Setup the SFC data model for OVS Proxied Service Functions
#   ServiceNode, SF, and SFF data models
# The rest are loaded in setup_sfc_common.py

controller=Template("$ODL_CONTROLLER").substitute(os.environ)
DEFAULT_PORT='8181'

USERNAME='admin'
PASSWORD='admin'

CLASSIFIER1_IP=Template("$CLASSIFIER1_IP").substitute(os.environ)
SFF1_IP=Template("$SFF1_IP").substitute(os.environ)
SF1_IP=Template("$SF1_IP").substitute(os.environ)
SF2_IP=Template("$SF2_IP").substitute(os.environ)
SF2_PROXY_IP=Template("$SF2_PROXY_IP").substitute(os.environ)
SFF2_IP=Template("$SFF2_IP").substitute(os.environ)
CLASSIFIER2_IP=Template("$CLASSIFIER2_IP").substitute(os.environ)
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
                "ip-mgmt-address": CLASSIFIER1_IP
            },
            {
                "name": "sff1",
                "service-function": [
                ],
                "ip-mgmt-address": SFF1_IP
            },
            {
                "name": "sf1",
                "service-function": [
                    "dpi-1"
                ],
                "ip-mgmt-address": SF1_IP
            },
            {
                "name": "sf2",
                "service-function": [
                    "firewall-1"
                ],
                "ip-mgmt-address": SF2_IP
            },
            {
                "name": "sff2",
                "service-function": [
                ],
                "ip-mgmt-address": SFF2_IP
            },
            {
                "name": "classifier2",
                "service-function": [
                ],
                "ip-mgmt-address": CLASSIFIER2_IP
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
                "ip-mgmt-address": SF1_IP,
                "type": "dpi",
                "sf-data-plane-locator": [
                    {
                        "name": "dpi-1-dpl",
                        "port": 4790,
                        "ip": SF1_IP,
                        "transport": "service-locator:vxlan-gpe",
                        "service-function-forwarder": "SFF1"
                    }
                ]
            },
            {
                "name": "firewall-1",
                "ip-mgmt-address": SF2_IP,
                "type": "firewall",
                "sf-data-plane-locator": [
                    {
                        "name": "firewall-1-dpl",
                        "port": 4789,
                        "ip": SF2_IP,
                        "transport": "service-locator:vxlan",
                        "service-function-forwarder": "SFF2",
                        "service-function-proxy:proxy-data-plane-locator": {
                            "port": 4790,
                            "ip": SF2_PROXY_IP,
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
                "name": "Classifier1",
                "service-node": "classifier1",
                "service-function-forwarder-ovs:ovs-bridge": {
                    "bridge-name": "br-sfc",
                },
                "sff-data-plane-locator": [
                    {
                        "name": "sff0-dpl",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 4790,
                            "ip": CLASSIFIER1_IP
                        },
                        "service-function-forwarder-ovs:ovs-options": {
                            "remote-ip": "flow",
                            "dst-port": "4790",
                            "key": "flow",
                            "exts": "gpe"
                        }
                    }
                ],
            },
            {
                "name": "SFF1",
                "service-node": "sff1",
                "service-function-forwarder-ovs:ovs-bridge": {
                    "bridge-name": "br-sfc",
                },
                "sff-data-plane-locator": [
                    {
                        "name": "sff1-dpl",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 4790,
                            "ip": SFF1_IP
                        },
                        "service-function-forwarder-ovs:ovs-options": {
                            "remote-ip": "flow",
                            "dst-port": "4790",
                            "key": "flow",
                            "exts": "gpe"
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
                "service-node": "sff2",
                "service-function-forwarder-ovs:ovs-bridge": {
                    "bridge-name": "br-sfc",
                },
                "sff-data-plane-locator": [
                    {
                        "name": "sff2-dpl",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 4790,
                            "ip": SFF2_IP
                        },
                        "service-function-forwarder-ovs:ovs-options": {
                            "remote-ip": "flow",
                            "dst-port": "4790",
                            "key": "flow",
                            "exts": "gpe"
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
                "name": "Classifier2",
                "service-node": "classifier2",
                "service-function-forwarder-ovs:ovs-bridge": {
                    "bridge-name": "br-sfc",
                },
                "sff-data-plane-locator": [
                    {
                        "name": "sff3-dpl",
                        "data-plane-locator": {
                            "transport": "service-locator:vxlan-gpe",
                            "port": 4790,
                            "ip": CLASSIFIER2_IP
                        },
                        "service-function-forwarder-ovs:ovs-options": {
                            "remote-ip": "flow",
                            "dst-port": "4790",
                            "key": "flow",
                            "exts": "gpe"
                        }
                    }
                ],
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


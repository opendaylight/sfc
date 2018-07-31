#!/usr/bin/python
import argparse
import requests,json
from requests.auth import HTTPBasicAuth
from string import Template
from subprocess import call
import time
import sys
import os

# Setup common SFC data models independent of OVS or VPP or Proxied SFs
#   SFC, SFP, RSP, and SFC classifier models

controller=Template("$ODL_CONTROLLER").substitute(os.environ)
DEFAULT_PORT='8181'

USERNAME='admin'
PASSWORD='admin'

proxies = {
    "http": None,
    "https": None
}

def put(host, port, uri, data, debug=False):
    """Perform a PUT rest operation, using the URL and data provided"""

    url='http://'+host+":"+port+uri

    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug:
        print "PUT %s" % url
        print json.dumps(data, indent=4, sort_keys=True)
    r = requests.put(url, proxies=proxies, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    if debug:
        print r.text
    r.raise_for_status()
    time.sleep(5)

def get(host, port, uri, debug=False):
    """Perform a GET rest operation, using the URL provided"""

    url='http://'+host+":"+port+uri
    headers = {'Content-type': 'application/yang.data+json',
               'Accept': 'application/yang.data+json'}
    if debug:
        print "GET %s" % url
    r = requests.get(url, proxies=proxies, headers=headers, auth=HTTPBasicAuth(USERNAME, PASSWORD))
    result = {}
    if 200 <= r.status_code <= 299:
        result = json.loads(r.text)

    if debug:
        print json.dumps(result, indent=4, sort_keys=True, separators=(',', ': '))
    r.raise_for_status()
    return result

def get_service_function_chains_uri():
    return "/restconf/config/service-function-chain:service-function-chains/"

def get_service_function_chains_data():
    return {
    "service-function-chains": {
        "service-function-chain": [
            {
                "name": "SFC1",
                "sfc-service-function": [
                    {
                        "name": "dpi-abstract1",
                        "type": "dpi"
                    },
                    {
                        "name": "firewall-abstract1",
                        "type": "firewall"
                    }
                ]
            }
        ]
    }
}

def get_service_function_paths_uri():
    return "/restconf/config/service-function-path:service-function-paths/"

def get_service_function_paths_data():
    return {
    "service-function-paths": {
        "service-function-path": [
            {
                "name": "SFP1",
                "service-chain-name": "SFC1",
                "starting-index": 255,
                "symmetric": "true",
                "context-metadata": "NSH1"
            }
        ]
    }
}

def get_rsp_name(sfp_name, get_reverse=False):
    sfp_state_uri = "/restconf/operational/service-function-path:service-function-paths-state/service-function-path-state/%s" % sfp_name
    sfp_state_dict = get(controller, DEFAULT_PORT, sfp_state_uri, True)
    if not len(sfp_state_dict):
        print "ERROR empty dictionary, not able to get RSP name for SFP: %s" % sfp_name
        return ""

    if not len(sfp_state_dict["service-function-path-state"]):
        print "ERROR key sfp-rendered-service-path does not exist, not able to get RSP name for SFP: %s" % sfp_name
        return ""

    name_list = sfp_state_dict["service-function-path-state"][0]["sfp-rendered-service-path"]
    for entry in name_list:
        name = entry["name"]
        if name.endswith("Reverse"):
            if get_reverse:
                return name
        else:
            return name

    print "ERROR not able to get RSP name for SFP: %s" % sfp_name
    return ""

def get_service_function_metadata_uri():
    return "/restconf/config/service-function-path-metadata:service-function-metadata/"

def get_service_function_metadata_data():
    return {
  "service-function-metadata": {
    "context-metadata": [
      {
        "name": "NSH1",
        "context-header1": "1",
        "context-header2": "2",
        "context-header3": "3",
        "context-header4": "4"
      }
    ]
  }
}

def get_service_function_acl_uri():
    return "/restconf/config/ietf-access-control-list:access-lists/"

def get_service_function_acl_data(rsp_name, rev_rsp_name):
    return  {
  "access-lists": {
    "acl": [
      {
        "acl-name": "ACL1",
        "acl-type": "ietf-access-control-list:ipv4-acl",
        "access-list-entries": {
          "ace": [
            {
              "rule-name": "ACE11",
              "actions": {
                "service-function-acl:rendered-service-path": rsp_name
              },
              "matches": {
                "destination-ipv4-network": "192.168.2.0/24",
                "source-ipv4-network": "192.168.2.0/24",
                "protocol": "1",
                "source-port-range": {
                    "lower-port": "0"
                },
                "destination-port-range": {
                    "lower-port": "0"
                }
              }
            },
            {
              "rule-name": "ACE12",
              "actions": {
                "service-function-acl:rendered-service-path": rsp_name
              },
              "matches": {
                "destination-ipv4-network": "192.168.2.0/24",
                "source-ipv4-network": "192.168.2.0/24",
                "protocol": "6",
                "source-port-range": {
                    "lower-port": 0
                },
                "destination-port-range": {
                    "lower-port": 80
                }
              }
            }
          ]
        }
      },
      {
        "acl-name": "ACL2",
        "acl-type": "ietf-access-control-list:ipv4-acl",
        "access-list-entries": {
          "ace": [
            {
              "rule-name": "ACE21",
              "actions": {
                "service-function-acl:rendered-service-path": rev_rsp_name
              },
              "matches": {
                "destination-ipv4-network": "192.168.2.0/24",
                "source-ipv4-network": "192.168.2.0/24",
                "protocol": "1",
                "source-port-range": {
                    "lower-port": "0"
                },
                "destination-port-range": {
                    "lower-port": "0"
                }
              }
            },
            {
              "rule-name": "ACE22",
              "actions": {
                "service-function-acl:rendered-service-path": rev_rsp_name
              },
              "matches": {
                "destination-ipv4-network": "192.168.2.0/24",
                "source-ipv4-network": "192.168.2.0/24",
                "protocol": "6",
                "source-port-range": {
                    "lower-port": 80
                },
                "destination-port-range": {
                    "lower-port": 0
                }
              }
            }
          ]
        }
      }
    ]
  }
}

def get_service_function_classifiers_uri():
    return "/restconf/config/service-function-classifier:service-function-classifiers/"

def get_service_function_classifiers_data():
    return  {
  "service-function-classifiers": {
    "service-function-classifier": [
      {
        "name": "Classifier1",
        "scl-service-function-forwarder": [
          {
            "name": "Classifier1",
            "interface": "veth-br"
          }
        ],
        "acl": {
            "name": "ACL1",
            "type": "ietf-access-control-list:ipv4-acl"
         }
      },
      {
        "name": "Classifier2",
        "scl-service-function-forwarder": [
          {
            "name": "Classifier2",
            "interface": "veth-br"
          }
        ],
        "acl": {
            "name": "ACL2",
            "type": "ietf-access-control-list:ipv4-acl"
         }
      }
    ]
  }
}

if __name__ == "__main__":
    print "sending service function chains"
    put(controller, DEFAULT_PORT, get_service_function_chains_uri(), get_service_function_chains_data(), True)
    print "sending service function metadata"
    put(controller, DEFAULT_PORT, get_service_function_metadata_uri(), get_service_function_metadata_data(), True)
    print "sending service function paths"
    put(controller, DEFAULT_PORT, get_service_function_paths_uri(), get_service_function_paths_data(), True)
    print "sending service function acl"
    put(controller, DEFAULT_PORT, get_service_function_acl_uri(),
        get_service_function_acl_data(get_rsp_name("SFP1"), get_rsp_name("SFP1", True)), True)
    print "sending service function classifiers"
    put(controller, DEFAULT_PORT, get_service_function_classifiers_uri(), get_service_function_classifiers_data(), True)


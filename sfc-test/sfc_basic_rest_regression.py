__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.1"
__email__ = "rapenno@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 09/10/2014"

import requests
import json
import time

putheaders = {'content-type': 'application/json'}
getheaders = {'Accept': 'application/json'}
# ODL IP:port
ODLIP   = "127.0.0.1:8181"
# Static URLs for testing
GET_ALL_SF_URL  = "http://" + ODLIP + "/restconf/config/service-function:service-functions/"
GET_ALL_SFC_URL = "http://" + ODLIP + "/restconf/config/service-function-chain:service-function-chains/"
GET_ALL_SFF_URL = "http://" + ODLIP + "/restconf/config/service-function-forwarder:service-function-forwarders/"
GET_ALL_SFT_URL = "http://" + ODLIP + "/restconf/config/service-function-type:service-function-types/"
GET_ALL_SFP_URL = "http://" + ODLIP + "/restconf/config/service-function-path:service-function-paths/"
PUT_ONE_SFP_URL = "http://" + ODLIP + "/restconf/config/service-function-path:service-function-paths/"

GETURL  = "http://" + ODLIP + "/restconf/config/service-function:service-functions/service-function/%d/"
# Incremental PUT. This URL is for a list element
PUTURL  = "http://" + ODLIP + "/restconf/config/service-function:service-functions/service-function/%d/"


def get_initial_sf():
    s = requests.Session()
    print ("GETTing initially configured Service Functions \n")
    r = s.get(GET_ALL_SF_URL, stream=False )
    if (r.status_code == 200) and (json.loads(r.text) == json.loads(GET_ALL_SF_RESP_JSON)):
        print ("Success \n")
    else:
        print ("Failure to get SFs \n")



def get_initial_sfc():
    s = requests.Session()
    print ("GETTing initially configured Service Functions Chains \n")
    r = s.get(GET_ALL_SFC_URL, stream=False)
    if (r.status_code == 200) and (json.loads(r.text) == json.loads(GET_ALL_SFC_RESP_JSON)):
        print ("Success \n")
    else:
        print ("Failure to get SFCs \n")

def get_initial_sff():
    s = requests.Session()
    print ("GETTing initially configured Service Functions Forwarders \n")
    r = s.get(GET_ALL_SFF_URL, stream=False)
    if (r.status_code == 200) and (json.loads(r.text) == json.loads(GET_ALL_SFF_RESP_JSON)):
        print ("Success \n")
    else:
        print ("Failure to get SFFs \n")


def get_initial_sft():
    s = requests.Session()
    print ("GETTing initially configured Service Functions Types \n")
    r = s.get(GET_ALL_SFT_URL, stream=False)
    if (r.status_code == 200) and (json.loads(r.text) == json.loads(GET_ALL_SFT_RESP_JSON)):
        print ("Success \n")
    else:
        print ("Failure to get SFTs \n")

def put_one_sfp():
    s = requests.Session()
    print ("PUTing a single SFP \n")
    r = s.put(PUT_ONE_SFP_URL, data=PUT_ONE_SFP_JSON, headers=putheaders, stream=False)
    if r.status_code == 200:
        print ("Checking created SFP \n")
        # Creation of SFPs is slow, need to pause here.
        time.sleep(2)
        r = s.get(GET_ALL_SFP_URL, stream=False)
        if (r.status_code == 200) and (json.loads(r.text) == json.loads(GET_ALL_SFP_RESP_JSON)):
            print ("SFP created successfully \n")
        else:
            print ("Created SFP did not pass check - Failure \n")

    else:
        print ("Failed to create SFP \n")


PUT_ONE_SFP_JSON = """
{
  "service-function-paths": {
    "service-function-path": [
      {
        "name": "Path-1-SFC1",
        "service-chain-name": "SFC1"
      }
    ]
  }
}"""

GET_ALL_SFP_RESP_JSON = """
{
  "service-function-paths": {
    "service-function-path": [
      {
        "name": "Path-1-SFC1",
        "path-id": 1,
        "starting-index": 3,
        "service-chain-name": "SFC1",
        "service-path-hop": [
          {
            "hop-number": 0,
            "service-function-name": "dpi-102-1",
            "service_index": 3,
            "service-function-forwarder": "SFF-bootstrap"
          },
          {
            "hop-number": 1,
            "service-function-name": "napt44-104",
            "service_index": 2,
            "service-function-forwarder": "SFF-bootstrap"
          },
          {
            "hop-number": 2,
            "service-function-name": "firewall-104",
            "service_index": 1,
            "service-function-forwarder": "SFF-bootstrap"
          }
        ]
      }
    ]
  }
}"""

GET_ALL_SF_RESP_JSON = """
{
  "service-functions": {
    "service-function": [
      {
        "name": "firewall-104",
        "sf-data-plane-locator": [
          {
            "name": "my-locator",
            "service-function-forwarder": "SFF-bootstrap",
            "ip": "10.3.1.104",
            "port": 10001
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "10.3.1.104",
        "type": "firewall"
      },
      {
        "name": "napt44-104",
        "sf-data-plane-locator": [
          {
            "name": "3",
            "service-function-forwarder": "SFF-bootstrap",
            "ip": "10.3.1.104",
            "port": 10020
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "10.3.1.104",
        "type": "napt44"
      },
      {
        "name": "napt44-103-2",
        "sf-data-plane-locator": [
          {
            "name": "preferred",
            "service-function-forwarder": "SFF-bootstrap",
            "ip": "10.3.1.103",
            "port": 10002
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "10.3.1.103",
        "type": "napt44"
      },
      {
        "name": "napt44-103-1",
        "sf-data-plane-locator": [
          {
            "name": "master",
            "service-function-forwarder": "SFF-bootstrap",
            "ip": "10.3.1.103",
            "port": 10001
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "10.3.1.103",
        "type": "napt44"
      },
      {
        "name": "firewall-101-1",
        "sf-data-plane-locator": [
          {
            "name": "007",
            "service-function-forwarder": "SFF-bootstrap",
            "ip": "10.3.1.101",
            "port": 10001
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "10.3.1.101",
        "type": "firewall"
      },
      {
        "name": "dpi-102-3",
        "sf-data-plane-locator": [
          {
            "name": "101",
            "service-function-forwarder": "SFF-bootstrap",
            "ip": "10.3.1.102",
            "port": 10003
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "10.3.1.102",
        "type": "dpi"
      },
      {
        "name": "firewall-101-2",
        "sf-data-plane-locator": [
          {
            "name": "2",
            "service-function-forwarder": "SFF-bootstrap",
            "ip": "10.3.1.101",
            "port": 10002
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "10.3.1.101",
        "type": "firewall"
      },
      {
        "name": "dpi-102-2",
        "sf-data-plane-locator": [
          {
            "name": "1",
            "service-function-forwarder": "SFF-bootstrap",
            "ip": "10.3.1.102",
            "port": 10002
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "10.3.1.102",
        "type": "dpi"
      },
      {
        "name": "dpi-102-1",
        "sf-data-plane-locator": [
          {
            "name": "4",
            "service-function-forwarder": "SFF-bootstrap",
            "ip": "10.3.1.102",
            "port": 10001
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "10.3.1.102",
        "type": "dpi"
      }
    ]
  }
}"""

GET_ALL_SFC_RESP_JSON = """
{
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
            "name": "napt44-abstract1",
            "type": "napt44"
          },
          {
            "name": "firewall-abstract1",
            "type": "firewall"
          }
        ]
      },
      {
        "name": "SFC2",
        "sfc-service-function": [
          {
            "name": "firewall-abstract2",
            "type": "firewall"
          },
          {
            "name": "napt44-abstract2",
            "type": "napt44"
          }
        ]
      }
    ]
  }
}"""


GET_ALL_SFF_RESP_JSON = """
{
  "service-function-forwarders": {
    "service-function-forwarder": [
      {
        "name": "SFF-bootstrap",
        "ovs": {
          "bridge-name": "br-int",
          "rest-uri": "http://www.example.com/sffs/sff-bootstrap",
          "uuid": "4c3778e4-840d-47f4-b45e-0988e514d26c"
        },
        "service-node": "OVSDB1",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "data-plane-locator": {
              "port": 5000,
              "ip": "192.168.1.1",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "service-function-dictionary": [
          {
            "name": "SF1",
            "type": "dp1",
            "sff-sf-data-plane-locator": {
              "port": 5000,
              "ip": "10.1.1.1"
            }
          },
          {
            "name": "SF2",
            "type": "firewall",
            "sff-sf-data-plane-locator": {
              "port": 5000,
              "ip": "10.1.1.2"
            }
          }
        ],
        "classifier": "acl-sfp-1"
      },
      {
        "name": "br-int-ovs-2",
        "ovs": {
          "bridge-name": "br-int",
          "rest-uri": "http://www.example.com/sffs/br-int-ovs-2",
          "uuid": "fd4d849f-5140-48cd-bc60-6ad1f5fc0a0"
        },
        "service-node": "OVSDB2",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "data-plane-locator": {
              "port": 5000,
              "ip": "192.168.1.2",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "service-function-dictionary": [
          {
            "name": "SF5",
            "type": "qos",
            "sff-sf-data-plane-locator": {
              "port": 5000,
              "ip": "10.1.1.5"
            }
          },
          {
            "name": "SF6",
            "type": "napt44",
            "sff-sf-data-plane-locator": {
              "port": 5000,
              "ip": "10.1.1.6"
            }
          }
        ]
      }
    ]
  }
}"""

GET_ALL_SFT_RESP_JSON = """
{
  "service-function-types": {
    "service-function-type": [
      {
        "type": "dpi",
        "sft-service-function-name": [
          {
            "name": "dpi-102-1"
          },
          {
            "name": "dpi-102-2"
          },
          {
            "name": "dpi-102-3"
          }
        ]
      },
      {
        "type": "napt44",
        "sft-service-function-name": [
          {
            "name": "napt44-104"
          },
          {
            "name": "napt44-103-1"
          },
          {
            "name": "napt44-103-2"
          }
        ]
      },
      {
        "type": "firewall",
        "sft-service-function-name": [
          {
            "name": "firewall-104"
          },
          {
            "name": "firewall-101-2"
          },
          {
            "name": "firewall-101-1"
          }
        ]
      }
    ]
  }
}"""


if __name__ == "__main__":
    get_initial_sf()
    get_initial_sfc()
    get_initial_sff()
    get_initial_sft()
    put_one_sfp()
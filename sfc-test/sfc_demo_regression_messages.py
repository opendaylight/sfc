__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.1"
__email__ = "andrej.kincel@gmail.com"
__status__ = ""

SERVICE_FUNCTIONS_JSON = """
{
  "service-functions": {
    "service-function": [
      {
        "name": "SF1",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "10.0.2.101",
            "port": 40000,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF1"
          }
        ],
        "rest-uri": "http://10.0.2.101:5000",
        "nsh-aware": true,
        "ip-mgmt-address": "10.0.2.101",
        "type": "service-function-type:dpi"
      },
      {
        "name": "SF2",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "10.0.2.102",
            "port": 40000,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF2"
          }
        ],
        "rest-uri": "http://10.0.2.102:5000",
        "nsh-aware": true,
        "ip-mgmt-address": "10.0.2.102",
        "type": "service-function-type:napt44"
      },
      {
        "name": "SF3",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "10.0.2.103",
            "port": 40000,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF3"
          }
        ],
        "rest-uri": "http://10.0.2.103:5000",
        "nsh-aware": true,
        "ip-mgmt-address": "10.0.2.103",
        "type": "service-function-type:firewall"
      }
    ]
  }
}"""

SERVICE_FUNCTION_FORWARDERS_JSON = """
{
  "service-function-forwarders": {
    "service-function-forwarder": [
      {  
        "name": "SFF1",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "data-plane-locator": {
              "port": 30000,
              "ip": "10.0.2.101",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://10.0.2.101:5000",
        "service-function-dictionary": [
          {
            "name": "SF1",
            "type": "service-function-type:dpi",
            "sff-sf-data-plane-locator": {
              "port": 40000,
              "ip": "10.0.2.101",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "ip-mgmt-address": "10.0.2.101",
        "service-node": "Xubuntu-1"
      },
      {
        "name": "SFF2",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "data-plane-locator": {
              "port": 30000,
              "ip": "10.0.2.102",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://10.0.2.102:5000",
        "service-function-dictionary": [
          {
            "name": "SF2",
            "type": "service-function-type:napt44",
            "sff-sf-data-plane-locator": {
              "port": 40000,
              "ip": "10.0.2.102",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "ip-mgmt-address": "10.0.2.102",
        "service-node": "Xubuntu-2"
      },
      {
        "name": "SFF3",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "data-plane-locator": {
              "port": 30000,
              "ip": "10.0.2.103",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://10.0.2.103:5000",
        "service-function-dictionary": [
          {
            "name": "SF3",
            "type": "service-function-type:firewall",
            "sff-sf-data-plane-locator": {
              "port": 40000,
              "ip": "10.0.2.103",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "ip-mgmt-address": "10.0.2.103",
        "service-node": "Xubuntu-3"
      },
      {
        "name": "SFF-classifier",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "data-plane-locator": {
              "port": 30000,
              "ip": "10.0.2.10",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://10.0.2.10:5000",
        "ip-mgmt-address": "10.0.2.10",
        "service-node": "Xubuntu-class"
      },
      {
        "name": "SFF-rev-classifier",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "data-plane-locator": {
              "port": 30000,
              "ip": "10.0.2.20",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://10.0.2.20:5000",
        "ip-mgmt-address": "10.0.2.20",
        "service-node": "Xubuntu-rev-class"
      }
    ]
  }
}"""

SERVICE_CHAINS_JSON = """
{
  "service-function-chains": {
    "service-function-chain": [
      {
        "name": "SFC1",
        "sfc-service-function": [
          {
            "name": "dpi-abstract",
            "type": "service-function-type:dpi",
            "order": 0
          },
          {
            "name": "napt44-abstract",
            "type": "service-function-type:napt44",
            "order": 1
          },
          {
            "name": "firewall-abstract",
            "type": "service-function-type:firewall",
            "order": 2
          }
        ]
      }
    ]
  }
}"""

SERVICE_PATH_JSON = """
{
  "service-function-paths": {
    "service-function-path": [
      {
        "name": "SFC1-SFP1",
        "service-chain-name": "SFC1",
        "path-id": 1,
        "symmetric": false
      }
    ]
  }
}"""

RENDERED_SERVICE_PATH_RPC_REQ = """
{
  "input": {
    "parent-service-function-path": "SFC1-SFP1"
  }
}"""

RENDERED_SERVICE_PATH_RPC_RESP = """
{
  "output": {
    "result": true
  }
}"""


RENDERED_SERVICE_PATH_RESP_JSON = """
{
  "rendered-service-paths": {
    "rendered-service-path": [
      {
        "name": "SFC1-SFP1",
        "parent-service-function-path": "SFC1-SFP1",
        "service-chain-name": "SFC1",
        "starting-index": 255,
        "path-id": 1,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-name": "SF1",
            "service-function-forwarder": "SFF1",
            "service-index": 255
          },
          {
            "hop-number": 0,
            "service-function-name": "SF2",
            "service-function-forwarder": "SFF2",
            "service-index": 254
          },
          {
            "hop-number": 0,
            "service-function-name": "SF3",
            "service-function-forwarder": "SFF3",
            "service-index": 253
          }
        ]
      }
    ]
  }
}
"""

SERVICE_FUNCTION_FORWARDERS_OPER_JSON = """
{
  "service-function-forwarders-state": {
    "service-function-forwarder-state": [
      {
        "name": "SFF1",
        "sff-service-path": [
          {
            "name": "SFC1-SFP1"
          }
        ]
      },
      {
        "name": "SFF2",
        "sff-service-path": [
          {
            "name": "SFC1-SFP1"
          }
        ]
      },
      {
        "name": "SFF3",
        "sff-service-path": [
          {
            "name": "SFC1-SFP1"
          }
        ]
      }
    ]
  }
}"""

SERVICE_FUNCTION_OPER_JSON = """
{
  "service-functions-state": {
    "service-function-state": [
      {
        "name": "SF1",
        "sf-service-path": [
          {
            "name": "SFC1-SFP1"
          }
        ]
      },
      {
        "name": "SF2",
        "sf-service-path": [
          {
            "name": "SFC1-SFP1"
          }
        ]
      },
      {
        "name": "SF3",
        "sf-service-path": [
          {
            "name": "SFC1-SFP1"
          }
        ]
      }
    ]
  }
}"""

SERVICE_FUNCTION_TYPE_JSON = """
{
  "service-function-types": {
    "service-function-type": [
      {
        "type": "service-function-type:dpi",
        "sft-service-function-name": [
          {
            "name": "SF1"
          }
        ]
      },
      {
        "type": "service-function-type:napt44",
        "sft-service-function-name": [
          {
            "name": "SF2"
          }
        ]
      },
      {
        "type": "service-function-type:firewall",
        "sft-service-function-name": [
          {
            "name": "SF3"
          }
        ]
      }
    ]
  }
}"""

IETF_ACL_JSON_IPV4 = """
{
  "access-lists": {
    "access-list": [
      {
        "acl-name": "ACL1",
        "access-list-entries": [
          {
            "rule-name": "ACE1",
            "matches": {
              "destination-ipv4-address": "0.0.0.0/0",
              "source-ipv4-address": "0.0.0.0/0",
              "destination-port-range": {
                "upper-port": 10000,
                "lower-port": 5000
              },
              "ip-protocol": 17
            },
            "actions": {
              "service-function-acl:rendered-service-path": "SFC1-SFP1"
            }
          }
        ]
      }
    ]
  }
}"""


SERVICE_CLASSIFIER_JSON = """
{
  "service-function-classifiers": {
    "service-function-classifier": [
      {
        "name": "Classifier1",
        "access-list": "ACL1",
        "scl-service-function-forwarder": [
          {
            "name": "SFF-classifier"
          }
        ]
      }
    ]
  }
}"""

__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.3"
__email__ = "andrej.kincel@gmail.com"
__email__ = "rapennol@gmail.com"
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
            "ip": "{ip}",
            "port": 40001,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF1"
          }
        ],
        "rest-uri": "http://{ip}:5000",
        "nsh-aware": true,
        "ip-mgmt-address": "{ip}",
        "type": "dpi"
      },
      {
        "name": "SF2",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "{ip}",
            "port": 40002,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF2"
          }
        ],
        "rest-uri": "http://{ip}:5000",
        "nsh-aware": true,
        "ip-mgmt-address": "{ip}",
        "type": "firewall"
      },
      {
        "name": "SF3",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "{ip}",
            "port": 40003,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF3"
          }
        ],
        "rest-uri": "http://{ip}:5000",
        "nsh-aware": true,
        "ip-mgmt-address": "{ip}",
        "type": "napt44"
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
              "port": 30001,
              "ip": "{ip}",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://{ip}:5000",
        "service-function-dictionary": [
          {
            "name": "SF1",
            "sff-sf-data-plane-locator": {
              "sff-dpl-name": "eth0",
              "sf-dpl-name": "vxlan"
            }
          }
        ],
        "ip-mgmt-address": "{ip}",
        "service-node": "Xubuntu-1"
      },
      {
        "name": "SFF2",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "data-plane-locator": {
              "port": 30002,
              "ip": "{ip}",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://{ip}:5000",
        "service-function-dictionary": [
          {
            "name": "SF2",
            "sff-sf-data-plane-locator": {
              "sff-dpl-name": "eth0",
              "sf-dpl-name": "vxlan"
            }
          }
        ],
        "ip-mgmt-address": "{ip}",
        "service-node": "Xubuntu-2"
      },
      {
        "name": "SFF3",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "data-plane-locator": {
              "port": 30003,
              "ip": "{ip}",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://{ip}:5000",
        "service-function-dictionary": [
          {
            "name": "SF3",
            "sff-sf-data-plane-locator": {
              "sff-dpl-name": "eth0",
              "sf-dpl-name": "vxlan"
            }
          }
        ],
        "ip-mgmt-address": "{ip}",
        "service-node": "Xubuntu-3"
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
            "type": "dpi",
            "order": 0
          },
          {
            "name": "firewall-abstract",
            "type": "firewall",
            "order": 1
          },
          {
            "name": "napt44-abstract",
            "type": "napt44",
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
        "symmetric": true
      }
    ]
  }
}"""


SERVICE_PATH2_JSON = """
{
  "service-function-paths": {
    "service-function-path": [
      {
        "name": "SFC1-SFP1",
        "service-chain-name": "SFC1",
        "path-id": 1,
        "symmetric": true
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
    "name": "SFC1-SFP1-Path-194"
  }
}"""


RENDERED_SERVICE_PATH_RESP_JSON = """
{
  "rendered-service-paths": {
    "rendered-service-path": [
      {
        "name": "SFC1-SFP1-Path-272-Reverse",
        "transport-type": "service-locator:vxlan-gpe",
        "parent-service-function-path": "SFC1-SFP1",
        "path-id": 8388880,
        "service-chain-name": "SFC1",
        "starting-index": 255,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-forwarder-locator": "eth0",
            "service-function-name": "SF3",
            "service-function-forwarder": "SFF3",
            "service-index": 255
          },
          {
            "hop-number": 1,
            "service-function-forwarder-locator": "eth0",
            "service-function-name": "SF2",
            "service-function-forwarder": "SFF2",
            "service-index": 254
          },
          {
            "hop-number": 2,
            "service-function-forwarder-locator": "eth0",
            "service-function-name": "SF1",
            "service-function-forwarder": "SFF1",
            "service-index": 253
          }
        ],
        "symmetric-path-id": 272
      },
      {
        "name": "SFC1-SFP1-Path-272",
        "transport-type": "service-locator:vxlan-gpe",
        "parent-service-function-path": "SFC1-SFP1",
        "path-id": 272,
        "service-chain-name": "SFC1",
        "starting-index": 255,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-forwarder-locator": "eth0",
            "service-function-name": "SF1",
            "service-function-forwarder": "SFF1",
            "service-index": 255
          },
          {
            "hop-number": 1,
            "service-function-forwarder-locator": "eth0",
            "service-function-name": "SF2",
            "service-function-forwarder": "SFF2",
            "service-index": 254
          },
          {
            "hop-number": 2,
            "service-function-forwarder-locator": "eth0",
            "service-function-name": "SF3",
            "service-function-forwarder": "SFF3",
            "service-index": 253
          }
        ],
        "symmetric-path-id": 8388880
      }
    ]
  }
}"""

SERVICE_FUNCTION_FORWARDERS_OPER_JSON = """
{
  "service-function-forwarders-state": {
    "service-function-forwarder-state": [
      {
        "name": "SFF1",
        "sff-service-path": [
          {
            "name": "SFC1-SFP1-Path-272-Reverse"
          },
          {
            "name": "SFC1-SFP1-Path-272"
          }
        ]
      },
      {
        "name": "SFF2",
        "sff-service-path": [
          {
            "name": "SFC1-SFP1-Path-272-Reverse"
          },
          {
            "name": "SFC1-SFP1-Path-272"
          }
        ]
      },
      {
        "name": "SFF3",
        "sff-service-path": [
          {
            "name": "SFC1-SFP1-Path-272-Reverse"
          },
          {
            "name": "SFC1-SFP1-Path-272"
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
        "name": "SF3",
        "sf-service-path": [
          {
            "name": "SFC1-SFP1-Path-272-Reverse"
          },
          {
            "name": "SFC1-SFP1-Path-272"
          }
        ]
      },
      {
        "name": "SF2",
        "sf-service-path": [
          {
            "name": "SFC1-SFP1-Path-272-Reverse"
          },
          {
            "name": "SFC1-SFP1-Path-272"
          }
        ]
      },
      {
        "name": "SF1",
        "sf-service-path": [
          {
            "name": "SFC1-SFP1-Path-272-Reverse"
          },
          {
            "name": "SFC1-SFP1-Path-272"
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
        "type": "dpi",
        "sft-service-function-name": [
          {
            "name": "SF1"
          }
        ]
      },
      {
        "type": "napt44",
        "sft-service-function-name": [
          {
            "name": "SF2"
          }
        ]
      },
      {
        "type": "firewall",
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
              "destination-ipv4-address": "192.168.200.100/32",
              "destination-port-range": {
                "upper-port": 10000,
                "lower-port": 5000
              },
              "ip-protocol": 6
            },
            "actions": {
              "service-function-acl:rendered-service-path": "SFC1-SFP1"
            }
          }
        ]
      },
      {
        "acl-name": "ACL1-Reverse",
        "access-list-entries": [
          {
            "rule-name": "ACE1-R",
            "matches": {
              "source-ipv4-address": "192.168.200.100/32",
              "source-port-range": {
                "upper-port": 10000,
                "lower-port": 5000
              },
              "ip-protocol": 6
            },
            "actions": {
              "service-function-acl:rendered-service-path": "SFC1-SFP1-Reverse"
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
      },
      {
        "name": "Classifier1-Reverse",
        "access-list": "ACL1-Reverse",
        "scl-service-function-forwarder": [
          {
            "name": "SFF-classifier-Reverse"
          }
        ]
      }
    ]
  }
}"""

__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.2"
__email__ = "rapenno@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 10/05/2014"

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
        "service-function-forwarder-ovs:ovs": {
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
        "service-function-forwarder-ovs:ovs": {
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

PUT_SFC3_JSON = """
{
  "service-function-chain": [
    {
      "name": "SFC3",
      "sfc-service-function" : [
        {
        "name" : "Chain-3-service-1",
        "type" : "dpi"
        },
        {
        "name" : "Chain-3-service-2",
        "type" : "qos"
        },
        {
        "name" : "Chain-3-service-3",
        "type" : "firewall"
        }
      ]
    }
  ]
}"""

GET_ALL_SFC3_RESP_JSON = """
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
      },
      {
        "name": "SFC3",
        "sfc-service-function": [
          {
            "name": "Chain-3-service-3",
            "type": "firewall"
          },
          {
            "name": "Chain-3-service-2",
            "type": "qos"
          },
          {
            "name": "Chain-3-service-1",
            "type": "dpi"
          }
        ]
      }
    ]
  }
}"""



__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.1"
__email__ = "rapenno@gmail.com"
__status__ = ""

SERVICE_FUNCTIONS_JSON = """
{
  "service-functions": {
    "service-function": [
      {
        "name": "SF5",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "10.0.1.43",
            "port": 40001,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF4"
          }
        ],
        "nsh-aware": true,
        "rest-uri": "http://10.0.1.43:5000",
        "ip-mgmt-address": "10.0.1.43",
        "type": "service-function-type:napt44"
      },
      {
        "name": "SF1",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "10.0.1.41",
            "port": 40001,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF1"
          }
        ],
        "rest-uri": "http://10.0.1.41:5000",
        "nsh-aware": true,
        "ip-mgmt-address": "10.0.1.41",
        "type": "service-function-type:dpi"
      },
      {
        "name": "SF2",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "10.0.1.42",
            "port": 40001,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF2"
          }
        ],
        "rest-uri": "http://10.0.1.42:5000",
        "nsh-aware": true,
        "ip-mgmt-address": "10.0.1.42",
        "type": "service-function-type:ids"
      },
      {
        "name": "SF3",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "10.0.1.43",
            "port": 40001,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF3"
          }
        ],
        "nsh-aware": true,
        "rest-uri": "http://10.0.1.43:5000",
        "ip-mgmt-address": "10.0.1.43",
        "type": "service-function-type:qos"
      },
      {
        "name": "SF4",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "10.0.1.42",
            "port": 40001,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF3"
          }
        ],
        "nsh-aware": true,
        "rest-uri": "http://10.0.1.42:5000",
        "ip-mgmt-address": "10.0.1.42",
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
        "name": "SFF5",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "service-function-forwarder-ovs:ovs-bridge": {
              "bridge-name": "br-tun",
              "uuid": "fd4d849f-5140-48cd-bc60-6ad1f5fc0a0"
            },
            "data-plane-locator": {
              "port": 4789,
              "ip": "10.0.1.45",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://10.0.1.45:5000",
        "service-node": "OVSDB2",
        "ip-mgmt-address": "10.0.1.44"
      },
      {
        "name": "SFF4",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "service-function-forwarder-ovs:ovs-bridge": {
              "bridge-name": "br-tun",
              "uuid": "fd4d849f-5140-48cd-bc60-6ad1f5fc0a0"
            },
            "data-plane-locator": {
              "port": 4789,
              "ip": "10.0.1.44",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://10.0.1.44:5000",
        "service-function-dictionary": [
          {
            "name": "SF4",
            "type": "service-function-type:firewall",
            "sff-sf-data-plane-locator": {
              "service-function-forwarder-ovs:ovs-bridge": {
                "bridge-name": "br-int"
              },
              "port": 40001,
              "ip": "10.0.1.44",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "service-node": "OVSDB2",
        "ip-mgmt-address": "10.0.1.44"
      },
      {
        "name": "SFF1",
        "service-node": "OVSDB1",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "service-function-forwarder-ovs:ovs-bridge": {
              "bridge-name": "br-tun",
              "uuid": "4c3778e4-840d-47f4-b45e-0988e514d26c"
            },
            "data-plane-locator": {
              "port": 4789,
              "ip": "10.0.1.41",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://10.0.1.41:5000",
        "service-function-dictionary": [
          {
            "name": "SF1",
            "type": "service-function-type:dpi",
            "sff-sf-data-plane-locator": {
              "service-function-forwarder-ovs:ovs-bridge": {
                "bridge-name": "br-int"
              },
              "port": 40001,
              "ip": "10.0.1.41",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "ip-mgmt-address": "10.0.1.41"
      },
      {
        "name": "SFF2",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "service-function-forwarder-ovs:ovs-bridge": {
              "bridge-name": "br-tun",
              "uuid": "fd4d849f-5140-48cd-bc60-6ad1f5fc0a0"
            },
            "data-plane-locator": {
              "port": 4789,
              "ip": "10.0.1.42",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://10.0.1.42:5000",
        "service-function-dictionary": [
          {
            "name": "SF2",
            "type": "service-function-type:ids",
            "sff-sf-data-plane-locator": {
              "service-function-forwarder-ovs:ovs-bridge": {
                "bridge-name": "br-int"
              },
              "port": 40001,
              "ip": "10.0.1.42",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "service-node": "OVSDB2",
        "ip-mgmt-address": "10.0.1.42"
      },
      {
        "name": "SFF3",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "service-function-forwarder-ovs:ovs-bridge": {
              "bridge-name": "br-tun",
              "uuid": "fd4d849f-5140-48cd-bc60-6ad1f5fc0a0"
            },
            "data-plane-locator": {
              "port": 4789,
              "ip": "10.0.1.43",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://10.0.1.43:5000",
        "service-function-dictionary": [
          {
            "name": "SF3",
            "type": "service-function-type:qos",
            "sff-sf-data-plane-locator": {
              "service-function-forwarder-ovs:ovs-bridge": {
                "bridge-name": "br-int"
              },
              "port": 40001,
              "ip": "10.0.1.43",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "service-node": "OVSDB2",
        "ip-mgmt-address": "10.0.1.43"
      }
    ]
  }
}"""

SERVICE_CHAINS_JSON = """
{
  "service-function-chains": {
    "service-function-chain": [
      {
        "name": "SFC2",
        "sfc-service-function": [
          {
            "name": "firewall-abstract2",
            "type": "service-function-type:firewall",
            "order": 0
          },
          {
            "name": "napt44-abstract2",
            "type": "service-function-type:napt44",
            "order": 1
          }
        ]
      },
      {
        "name": "SFC1",
        "symmetric": true,
        "sfc-service-function": [
          {
            "name": "dpi-abstract1",
            "type": "service-function-type:dpi"
          },
          {
            "name": "ids-abstract1",
            "type": "service-function-type:ids"
          },
          {
            "name": "qos-abstract1",
            "type": "service-function-type:qos"
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
        "name": "Path-1-SFC1",
        "service-chain-name": "SFC1",
        "symmetric": true
      },
      {
        "name": "Path-2-SFC2",
        "service-chain-name": "SFC2"
      }
    ]
  }
}"""

SERVICE_PATH_RESP_JSON = """
{
  "service-function-paths": {
    "service-function-path": [
      {
        "name": "Path-1-SFC1",
        "service-chain-name": "SFC1"
      },
      {
        "name": "Path-2-SFC2",
        "service-chain-name": "SFC2"
      }
    ]
  }
}
"""

RENDERED_SERVICE_PATH_RESP_JSON = """
{
  "rendered-service-paths": {
    "rendered-service-path": [
      {
        "name": "Path-2-SFC2",
        "path-id": 1,
        "service-chain-name": "SFC2",
        "starting-index": 255,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-name": "SF4",
            "service-function-forwarder": "SFF3",
            "service_index": 255
          },
          {
            "hop-number": 1,
            "service-function-name": "SF5",
            "service-function-forwarder": "SFF4",
            "service_index": 254
          }
        ]
      },
      {
        "name": "Path-1-SFC1-Reverse",
        "path-id": 3,
        "service-chain-name": "SFC1",
        "starting-index": 255,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-name": "SF3",
            "service-function-forwarder": "SFF3",
            "service_index": 255
          },
          {
            "hop-number": 1,
            "service-function-name": "SF2",
            "service-function-forwarder": "SFF2",
            "service_index": 254
          },
          {
            "hop-number": 2,
            "service-function-name": "SF1",
            "service-function-forwarder": "SFF1",
            "service_index": 253
          }
        ]
      },
      {
        "name": "Path-1-SFC1",
        "path-id": 2,
        "service-chain-name": "SFC1",
        "starting-index": 255,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-name": "SF1",
            "service-function-forwarder": "SFF1",
            "service_index": 255
          },
          {
            "hop-number": 1,
            "service-function-name": "SF2",
            "service-function-forwarder": "SFF2",
            "service_index": 254
          },
          {
            "hop-number": 2,
            "service-function-name": "SF3",
            "service-function-forwarder": "SFF3",
            "service_index": 253
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
            "name": "Path-1-SFC1"
          },
          {
            "name": "Path-1-SFC1-Reverse"
          }
        ]
      },
      {
        "name": "SFF2",
        "sff-service-path": [
          {
            "name": "Path-1-SFC1"
          },
          {
            "name": "Path-1-SFC1-Reverse"
          }
        ]
      },
      {
        "name": "SFF3",
        "sff-service-path": [
          {
            "name": "Path-2-SFC2"
          },
          {
            "name": "Path-1-SFC1"
          },
          {
            "name": "Path-1-SFC1-Reverse"
          }
        ]
      },
      {
        "name": "SFF4",
        "sff-service-path": [
          {
            "name": "Path-2-SFC2"
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
        "name": "SF4",
        "sf-service-path": [
          {
            "name": "Path-2-SFC2"
          }
        ]
      },
      {
        "name": "SF3",
        "sf-service-path": [
          {
            "name": "Path-1-SFC1-Reverse"
          },
          {
            "name": "Path-1-SFC1"
          }
        ]
      },
      {
        "name": "SF2",
        "sf-service-path": [
          {
            "name": "Path-1-SFC1-Reverse"
          },
          {
            "name": "Path-1-SFC1"
          }
        ]
      },
      {
        "name": "SF1",
        "sf-service-path": [
          {
            "name": "Path-1-SFC1-Reverse"
          },
          {
            "name": "Path-1-SFC1"
          }
        ]
      },
      {
        "name": "SF5",
        "sf-service-path": [
          {
            "name": "Path-2-SFC2"
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
            "name": "SF5"
          }
        ]
      },
      {
        "type": "service-function-type:firewall",
        "sft-service-function-name": [
          {
            "name": "SF4"
          }
        ]
      },
      {
        "type": "service-function-type:qos",
        "sft-service-function-name": [
          {
            "name": "SF3"
          }
        ]
      },
      {
        "type": "service-function-type:ids",
        "sft-service-function-name": [
          {
            "name": "SF2"
          }
        ]
      }
    ]
  }
}"""

SERVICE_PATH_ADD_ONE_JSON = """
{
  "service-function-path": [
    {
      "name": "Path-3-SFC2",
      "service-chain-name": "SFC2"
    }
  ]
}"""

SERVICE_FUNCTION_TYPE_DELETE_ONE_SF_JSON = """
{
  "service-function-types": {
    "service-function-type": [
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
      },
      {
        "type": "service-function-type:qos",
        "sft-service-function-name": [
          {
            "name": "SF4"
          }
        ]
      },
      {
        "type": "service-function-type:ids",
        "sft-service-function-name": [
          {
            "name": "SF5"
          }
        ]
      }
    ]
  }
}"""

RENDERED_SERVICE_PATH_ADD_ONE_JSON = """
{
  "rendered-service-paths": {
    "rendered-service-path": [
      {
        "name": "Path-2-SFC2",
        "path-id": 38,
        "service-chain-name": "SFC2",
        "starting-index": 2,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-name": "SF3",
            "service-function-forwarder": "SFF2",
            "service_index": 2
          },
          {
            "hop-number": 1,
            "service-function-name": "SF2",
            "service-function-forwarder": "SFF1",
            "service_index": 1
          }
        ]
      },
      {
        "name": "Path-1-SFC1",
        "path-id": 37,
        "service-chain-name": "SFC1",
        "starting-index": 3,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-name": "SF1",
            "service-function-forwarder": "SFF1",
            "service_index": 3
          },
          {
            "hop-number": 1,
            "service-function-name": "SF5",
            "service-function-forwarder": "SFF4",
            "service_index": 2
          },
          {
            "hop-number": 2,
            "service-function-name": "SF4",
            "service-function-forwarder": "SFF3",
            "service_index": 1
          }
        ]
      },
      {
        "name": "Path-3-SFC2",
        "path-id": 39,
        "service-chain-name": "SFC2",
        "starting-index": 2,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-name": "SF3",
            "service-function-forwarder": "SFF2",
            "service_index": 2
          },
          {
            "hop-number": 1,
            "service-function-name": "SF2",
            "service-function-forwarder": "SFF1",
            "service_index": 1
          }
        ]
      }
    ]
  }
}"""


IETF_ACL_JSON = """
{
  "access-lists": {
    "access-list": [
      {
        "acl-name": "acl-1",
        "access-list-entries": [
          {
            "rule-name": "ace-1",
            "matches": {
              "absolute": {
                "active": true
              },
              "destination-port-range": {
                "lower-port": 80,
                "upper-port": 80
              },
              "ip-protocol": 6,
              "source-port-range": {
                "lower-port": 0,
                "upper-port": 65535
              },
              "source-ipv4-address": "192.168.0.0/24"
            },
            "actions": {
              "service-function-acl:rendered-service-path": "Path-1-SFC1"
            }
          }
        ]
      }
    ]
  }
}"""

RENDERED_SERVICE_PATH_DEL_ONE_JSON = """
{
  "rendered-service-paths": {
    "rendered-service-path": [
      {
        "name": "Path-2-SFC2",
        "path-id": 5,
        "service-chain-name": "SFC2",
        "starting-index": 2,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-name": "SF3",
            "service-function-forwarder": "SFF2",
            "service_index": 2
          },
          {
            "hop-number": 1,
            "service-function-name": "SF2",
            "service-function-forwarder": "SFF1",
            "service_index": 1
          }
        ]
      },
      {
        "name": "Path-3-SFC2",
        "path-id": 6,
        "service-chain-name": "SFC2",
        "starting-index": 2,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-name": "SF3",
            "service-function-forwarder": "SFF2",
            "service_index": 2
          },
          {
            "hop-number": 1,
            "service-function-name": "SF2",
            "service-function-forwarder": "SFF1",
            "service_index": 1
          }
        ]
      }
    ]
  }
}"""


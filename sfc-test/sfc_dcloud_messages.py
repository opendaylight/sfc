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
        "name": "SF1",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "10.0.1.4",
            "port": 40001,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF1"
          }
        ],
        "nsh-aware": true,
        "rest-uri": "http://10.0.1.4:5000",
        "type": "dpi",
        "ip-mgmt-address": "10.0.1.4"
      },
      {
        "name": "SF2",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "10.0.1.4",
            "port": 40002,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF1"
          }
        ],
        "nsh-aware": true,
        "rest-uri": "http://10.0.1.4:5000",
        "type": "napt44",
        "ip-mgmt-address": "10.0.1.4"
      },
      {
        "name": "SF3",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "10.1.2.6",
            "port": 4789,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF2"
          }
        ],
        "nsh-aware": true,
        "type": "firewall",
        "ip-mgmt-address": "198.18.134.28"
      }
    ]
  }
}
"""

SERVICE_FUNCTION_FORWARDERS_JSON = """
{
  "service-function-forwarders": {
    "service-function-forwarder": [
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
              "ip": "10.100.100.1",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://10.0.1.4:5000",
        "service-function-dictionary": [
          {
            "name": "SF2",
            "type": "napt44",
            "sff-sf-data-plane-locator": {
              "service-function-forwarder-ovs:ovs-bridge": {
                "bridge-name": "br-int"
              },
              "port": 4789,
              "ip": "10.1.1.5",
              "transport": "service-locator:vxlan-gpe"
            }
          },
          {
            "name": "SF1",
            "type": "dp1",
            "sff-sf-data-plane-locator": {
              "service-function-forwarder-ovs:ovs-bridge": {
                "bridge-name": "br-int"
              },
              "port": 4789,
              "ip": "10.1.1.4",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "classifier": "acl-sfp-1",
        "ip-mgmt-address": "198.18.134.23"
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
              "ip": "10.100.100.2",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://198.18.134.24",
        "service-function-dictionary": [
          {
            "name": "SF3",
            "type": "firewall",
            "sff-sf-data-plane-locator": {
              "service-function-forwarder-ovs:ovs-bridge": {
                "bridge-name": "br-int"
              },
              "port": 4789,
              "ip": "10.1.2.6",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "ip-mgmt-address": "198.18.134.24",
        "service-node": "OVSDB2"
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
            "order": 0,
            "type": "firewall"
          },
          {
            "name": "napt44-abstract2",
            "order": 1,
            "type": "napt44"
          }
        ]
      },
      {
        "name": "SFC1",
        "sfc-service-function": [
          {
            "name": "dpi-abstract1",
            "order": 0,
            "type": "dpi"
          },
          {
            "name": "napt44-abstract1",
            "order": 1,
            "type": "napt44"
          },
          {
            "name": "firewall-abstract1",
            "order": 2,
            "type": "firewall"
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
        "service-chain-name": "SFC1"
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
        "name": "Path-1-SFC1",
        "path-id": 1,
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
            "service-function-name": "SF2",
            "service-function-forwarder": "SFF1",
            "service_index": 2
          },
          {
            "hop-number": 2,
            "service-function-name": "SF3",
            "service-function-forwarder": "SFF2",
            "service_index": 1
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
          }
        ]
      },
      {
        "name": "SFF2",
        "sff-service-path": [
          {
            "name": "Path-1-SFC1"
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
            "name": "Path-1-SFC1"
          }
        ]
      },
      {
        "name": "SF2",
        "sf-service-path": [
          {
            "name": "Path-1-SFC1"
          }
        ]
      },
      {
        "name": "SF1",
        "sf-service-path": [
          {
            "name": "Path-1-SFC1"
          }
        ]
      }
    ]
  }
}
"""

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
}
"""
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
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF1",
            "ip": "10.1.1.4",
            "port": 4789
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "198.18.134.26",
        "type": "dpi"
      },
      {
        "name": "SF2",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF1",
            "ip": "10.1.1.5",
            "port": 4789
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "198.18.134.27",
        "type": "napt44"
      },
      {
        "name": "SF3",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF2",
            "ip": "10.1.2.6",
            "port": 4789
          }
        ],
        "nsh-aware": true,
        "ip-mgmt-address": "198.18.134.28",
        "type": "firewall"
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
              "rest-uri": "http://www.example.com/sffs/sff-bootstrap",
              "uuid": "4c3778e4-840d-47f4-b45e-0988e514d26c",
              "bridge-name": "br-tun"
            },
            "data-plane-locator": {
              "port": 4789,
              "ip": "10.100.100.1",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "service-function-dictionary": [
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
          },
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
          }
        ],
        "classifier": "acl-sfp-1"
      },
      {
        "name": "SFF2",
        "service-node": "OVSDB2",
        "sff-data-plane-locator": [
          {
            "name": "eth0",
            "service-function-forwarder-ovs:ovs-bridge": {
              "rest-uri": "http://www.example.com/sffs/sff-bootstrap",
              "uuid": "fd4d849f-5140-48cd-bc60-6ad1f5fc0a0",
              "bridge-name": "br-tun"
            },
            "data-plane-locator": {
              "port": 4789,
              "ip": "10.100.100.2",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
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
        ]
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
        "path-id": 3,
        "starting-index": 3,
        "service-chain-name": "SFC1",
        "service-path-hop": [
          {
            "hop-number": 0,
            "service-function-name": "SF1",
            "service_index": 3,
            "service-function-forwarder": "SFF1"
          },
          {
            "hop-number": 1,
            "service-function-name": "SF3",
            "service_index": 2,
            "service-function-forwarder": "SFF2"
          },
          {
            "hop-number": 2,
            "service-function-name": "SF2",
            "service_index": 1,
            "service-function-forwarder": "SFF1"
          }
        ]
      }
    ]
  }
}
"""
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
        "rest-uri": "http://198.18.134.23",
        "service-function-dictionary": [
          {
            "name": "SF1",
            "type": "dp1",
            "sff-sf-data-plane-locator": {
              "port": 4789,
              "ip": "10.1.1.4",
              "transport": "service-locator:vxlan-gpe",
              "service-function-forwarder-ovs:ovs-bridge": {
                "bridge-name": "br-int"
              }
            }
          },
          {
            "name": "SF2",
            "type": "napt44",
            "sff-sf-data-plane-locator": {
              "port": 4789,
              "ip": "10.1.1.5",
              "transport": "service-locator:vxlan-gpe",
              "service-function-forwarder-ovs:ovs-bridge": {
                "bridge-name": "br-int"
              }
            }
          }
        ],
        "classifier": "acl-sfp-1",
        "ip-mgmt-address": "198.18.134.23"
      },
      {
        "name": "SFF2",
        "service-node": "OVSDB2",
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
              "port": 4789,
              "ip": "10.1.2.6",
              "transport": "service-locator:vxlan-gpe",
              "service-function-forwarder-ovs:ovs-bridge": {
                "bridge-name": "br-int"
              }
            }
          }
        ],
        "ip-mgmt-address": "198.18.134.24"
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
            "type": "dpi",
            "order" : 0
          },
          {
            "name": "napt44-abstract1",
            "type": "napt44",
            "order" : 1
          },
          {
            "name": "firewall-abstract1",
            "type": "firewall",
            "order" : 2
          }
        ]
      },
      {
        "name": "SFC2",
        "sfc-service-function": [
          {
            "name": "firewall-abstract2",
            "type": "firewall",
            "order" : 0
          },
          {
            "name": "napt44-abstract2",
            "type": "napt44",
            "order" : 1
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
        "path-id": 1,
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
            "service-function-name": "SF2",
            "service_index": 2,
            "service-function-forwarder": "SFF1"
          },
          {
            "hop-number": 2,
            "service-function-name": "SF3",
            "service_index": 1,
            "service-function-forwarder": "SFF2"
          }
        ]
      }
    ]
  }
}
"""
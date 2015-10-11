__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.1"
__email__ = "andrej.kincel@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 04/14/2015"

SERVICE_FUNCTION_FORWARDERS_JSON = """
{
  "service-function-forwarders": {
    "service-function-forwarder": [
      {
        "name": "SFF2",
        "sff-data-plane-locator": [
          {
            "name": "sff2-dp1",
            "service-function-forwarder-ovs:ovs-bridge": {},
            "data-plane-locator": {
              "transport": "service-locator:vxlan-gpe",
              "port": 6633,
              "ip": "10.0.2.7"
            },
            "service-function-forwarder-ovs:ovs-options": {
              "nshc1": "flow",
              "nsp": "flow",
              "key": "flow",
              "remote-ip": "flow",
              "nsi": "flow",
              "nshc2": "flow",
              "nshc3": "flow",
              "dst-port": "6633",
              "nshc4": "flow"
            }
          }
        ],
        "service-function-forwarder-ovs:ovs-bridge": {
          "bridge-name": "br2"
        },
        "service-function-dictionary": [
          {
            "name": "firewall-2",
            "type": "service-function-type:firewall",
            "sff-sf-data-plane-locator": {
              "service-function-forwarder-ovs:ovs-bridge": {},
              "transport": "service-locator:vxlan-gpe",
              "port": 6633,
              "ip": "10.0.2.9"
            }
          }
        ],
        "ip-mgmt-address": "10.0.2.7",
        "service-node": "",
        "service-function-forwarder-metadata-features:vxlan-overlay-classifier-type-1": {}
      }
    ]
  }
}
"""

SERVICE_FUNCTIONS_JSON = """
{
  "service-functions": {
    "service-function": [
      {
        "name": "firewall-2",
        "sf-data-plane-locator": [
          {
            "name": "firewall-2-dp1",
            "ip": "10.0.2.9",
            "port": 6633,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF2"
          }
        ],
        "rest-uri": "http://10.0.2.9:5000",
        "nsh-aware": true,
        "ip-mgmt-address": "10.0.2.9",
        "type": "service-function-type:firewall"
      }
    ]
  }
}"""

SERVICE_CHAINS_JSON = """
{
  "service-function-chains": {
    "service-function-chain": [
      {
        "name": "Chain-1",
        "sfc-service-function": [
          {
            "name": "firewall",
            "order": 0,
            "type": "service-function-type:firewall"
          }
        ]
      }
    ]
  }
}
"""

SERVICE_PATH_JSON = """
{
  "service-function-paths": {
    "service-function-path": [
      {
        "name": "Chain-1-Path-1",
        "service-chain-name": "Chain-1"
      }
    ]
  }
}"""

RENDERED_SERVICE_PATH_RPC_REQ = """
{
  "input": {
    "parent-service-function-path": "Chain-1-Path-1"
  }
}"""


RENDERED_SERVICE_PATH_RPC_RESP = """
{
  "output": {
    "name": "Chain-1-Path-1-Path-1"
  }
}"""

RENDERED_SERVICE_PATH_RESP_JSON = """
{
  "rendered-service-paths": {
    "rendered-service-path": [
      {
        "name": "Chain-1-Path-1-Path-1",
        "parent-service-function-path": "Chain-1-Path-1",
        "transport-type": "service-locator:vxlan-gpe",
        "path-id": 1,
        "service-chain-name": "Chain-1",
        "starting-index": 255,
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-index": 255,
            "service-function-forwarder-locator": "sff2-dp1",
            "service-function-name": "firewall-2",
            "service-function-forwarder": "SFF2"
          }
        ]
      }
    ]
  }
}"""


SERVICE_FORWARDER_OVS_RPC_REQ_1 = """
{
  "input":
  {
    "name": "br1",
    "ovs-node": {
      "ip": "10.0.2.6"
    }
  }
}"""

SERVICE_FORWARDER_OVS_RPC_REQ_2 = """
{
  "input":
  {
    "name": "br2",
    "ovs-node": {
      "ip": "10.0.2.6"
    }
  }
}"""

SERVICE_FORWARDER_OVS_RPC_RESP = """
{
  "output": {
    "result": true
  }
}"""



SERVICE_FUNCTION_FORWARDERS_OPER_JSON = """
{
  "service-function-forwarders-state": {
    "service-function-forwarder-state": [
      {
        "name": "ovsdb://10.0.2.6:57567/bridge/br1",
        "sff-service-path": [
          {
            "name": "ovs-sfp-Path-1"
          }
        ]
      },
      {
        "name": "ovsdb://10.0.2.6:57567/bridge/br2",
        "sff-service-path": [
          {
            "name": "ovs-sfp-Path-1"
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
        "name": "sf1",
        "sf-service-path": [
          {
            "name": "ovs-sfp-Path-1"
          }
        ]
      },
      {
        "name": "sf2",
        "sf-service-path": [
          {
            "name": "ovs-sfp-Path-1"
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
        "type": "service-function-type:firewall",
        "sft-service-function-name": [
          {
            "name": "firewall-2"
          }
        ]
      }
    ]
  }
}

"""
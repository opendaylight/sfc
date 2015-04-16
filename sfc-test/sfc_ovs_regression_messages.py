__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.1"
__email__ = "andrej.kincel@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 04/14/2015"

SERVICE_FUNCTIONS_JSON = """
{
  "service-functions": {
    "service-function": [
      {
        "name": "sf2",
        "type": "service-function-type:dpi",
        "sf-data-plane-locator": [
          {
            "name": "dp2",
            "service-function-forwarder": "ovsdb://10.0.2.6:52339/bridge/br2",
            "transport": "service-locator:vxlan-gpe"
          }
        ]
      },
      {
        "name": "sf1",
        "type": "service-function-type:firewall",
        "sf-data-plane-locator": [
          {
            "name": "dp1",
            "service-function-forwarder": "ovsdb://10.0.2.6:52339/bridge/br1",
            "transport": "service-locator:vxlan-gpe"
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
        "name": "ovs",
        "sfc-service-function": [
          {
            "name": "firewall",
            "order": 0,
            "type": "service-function-type:firewall"
          },
          {
            "name": "dpi",
            "order": 1,
            "type": "service-function-type:dpi"
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

SERVICE_PATH_JSON = """
{
  "service-function-paths": {
    "service-function-path": [
      {
        "name": "ovs-sfp",
        "service-chain-name": "ovs",
        "path-id": 1,
        "symmetric": false
      }
    ]
  }
}"""

RENDERED_SERVICE_PATH_RPC_REQ = """
{
  "input": {
    "parent-service-function-path": "ovs-sfp"
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
        "name": "ovs-sfp-Path-1",
        "starting-index": 255,
        "transport-type": "service-locator:vxlan-gpe",
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-function-forwarder": "ovsdb://10.0.2.6:52339/bridge/br1",
            "service-function-forwarder-locator": "br1",
            "service-function-name": "sf1",
            "service-index": 255
          },
          {
            "hop-number": 1,
            "service-function-forwarder": "ovsdb://10.0.2.6:52339/bridge/br2",
            "service-function-forwarder-locator": "br2",
            "service-function-name": "sf2",
            "service-index": 254
          }
        ],
        "path-id": 1,
        "parent-service-function-path": "ovs-sfp",
        "service-chain-name": "ovs"
      }
    ]
  }
}"""

SERVICE_FUNCTION_FORWARDERS_OPER_JSON = """
{
  "service-function-forwarders-state": {
    "service-function-forwarder-state": [
      {
        "name": "ovsdb://10.0.2.6:52339/bridge/br1",
        "sff-service-path": [
          {
            "name": "ovs-sfp-Path-1"
          }
        ]
      },
      {
        "name": "ovsdb://10.0.2.6:52339/bridge/br2",
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
            "name": "sf1"
          }
        ]
      },
      {
        "type": "service-function-type:dpi",
        "sft-service-function-name": [
          {
            "name": "sf2"
          }
        ]
      }
    ]
  }
}"""

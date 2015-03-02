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
            "ip": "127.0.0.1",
            "port": 40001,
            "transport": "service-locator:vxlan-gpe",
            "service-function-forwarder": "SFF1"
          }
        ],
        "rest-uri": "http://127.0.0.1:5000",
        "nsh-aware": true,
        "ip-mgmt-address": "127.0.0.1",
        "type": "service-function-type:dpi"
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
            "service-function-forwarder-ovs:ovs-bridge": {
              "bridge-name": "br-tun",
              "uuid": "4c3778e4-840d-47f4-b45e-0988e514d26c"
            },
            "data-plane-locator": {
              "port": 30001,
              "ip": "127.0.0.1",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "rest-uri": "http://127.0.0.1:5000",
        "service-function-dictionary": [
          {
            "name": "SF1",
            "type": "service-function-type:dpi",
            "sff-sf-data-plane-locator": {
              "service-function-forwarder-ovs:ovs-bridge": {
                "bridge-name": "br-int"
              },
              "port": 40001,
              "ip": "127.0.0.1",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "ip-mgmt-address": "127.0.0.1",
        "service-node": "Ubuntu1"
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
              "source-port-range": {
                "upper-port": 20000,
                "lower-port": 15000
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

IETF_ACL_JSON_IPV6 = """
{
  "access-lists": {
    "access-list": [
      {
        "acl-name": "ACL1",
        "access-list-entries": [
          {
            "rule-name": "ACE1",
            "matches": {
              "destination-ipv6-address": "::0/0",
              "source-ipv6-address": "::0",
              "flow-label": "1234"
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

IETF_ACL_JSON_MAC = """
{
  "access-lists": {
    "access-list": [
      {
        "acl-name": "ACL1",
        "access-list-entries": [
          {
            "rule-name": "ACE1",
            "matches": {
              "source-mac-address": "00:00:00:00:00:00",
              "destination-mac-address": "00:11:22:33:44:55"
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
            "name": "SFF1"
          }
        ]
      }
    ]
  }
}"""

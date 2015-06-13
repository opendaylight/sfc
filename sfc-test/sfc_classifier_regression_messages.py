__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.1"
__email__ = "andrej.kincel@gmail.com"
__status__ = ""

SERVICE_FUNCTIONS_JSON_IPV4 = """
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

SERVICE_FUNCTIONS_JSON_IPV6 = """
{
  "service-functions": {
    "service-function": [
      {
        "name": "SF1",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "ip": "::1",
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


SERVICE_FUNCTIONS_JSON_MAC = """
{
  "service-functions": {
    "service-function": [
      {
        "name": "SF1",
        "sf-data-plane-locator": [
          {
            "name": "vxlan",
            "mac": "00:00:11:22:33:44",
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

SERVICE_FUNCTION_FORWARDERS_JSON_IPV4 = """
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

SERVICE_FUNCTION_FORWARDERS_JSON_IPV4_6 = """
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
              "ip": "::1",
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


SERVICE_FUNCTION_FORWARDERS_JSON_IPV6 = """
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
              "ip": "::1",
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
              "ip": "::1",
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

SERVICE_FUNCTION_FORWARDERS_JSON_IPV6_4 = """
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
              "ip": "::1",
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
              "ip": "172.0.0.1",
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

SERVICE_FUNCTION_FORWARDERS_JSON_MAC = """
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
              "mac": "00:00:11:22:33:44",
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
              "mac": "00:00:11:22:33:44",
              "transport": "service-locator:vxlan-gpe"
            }
          }
        ],
        "ip-mgmt-address": "fd12:3456:789a:::10",
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
        "symmetric": false,
        "context-metadata": "test_context_metadata"
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
        "name": "SFC1-SFP1-Path-1",
        "path-id": 1,
        "parent-service-function-path": "SFC1-SFP1",
        "starting-index": 255,
        "service-chain-name": "SFC1",
        "rendered-service-path-hop": [
          {
            "hop-number": 0,
            "service-index": 255,
            "service-function-forwarder": "SFF1",
            "service-function-forwarder-locator": "eth0",
            "service-function-name": "SF1"
          }
        ]
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
            "name": "SFC1-SFP1-Path-1"
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
            "name": "SFC1-SFP1-Path-1"
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
              "destination-ipv4-address": "172.0.0.1/0",
              "source-ipv4-address": "172.0.0.1/0",
              "source-port-range": {
                "upper-port": 20000,
                "lower-port": 15000
              },
              "ip-protocol": 17
            },
            "actions": {
              "service-function-acl:rendered-service-path": "SFC1-SFP1-Path-1"
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
              "source-ipv6-address": "::1",
              "source-port-range": {
                "upper-port": 20000,
                "lower-port": 15000
              },
              "flow-label": "1234",
              "ip-protocol": 17
            },
            "actions": {
              "service-function-acl:rendered-service-path": "SFC1-SFP1-Path-1"
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
              "service-function-acl:rendered-service-path": "SFC1-SFP1-Path-1"
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


METADATA_JSON = """
{"service-function-metadata": {
        "context-metadata": [
            {
                "name": "test_context_metadata",
                "context-header2": 11195,
                "context-header4": 19933,
                "context-header3": 15564,
                "context-header1": 6826
            },
            {
                "name": "test_context_metadata1",
                "context-header2": 11195,
                "context-header4": 19933,
                "context-header3": 15564,
                "context-header1": 6826
            },
            {
                "name": "test-metadata",
                "context-header2": 2,
                "context-header3": 3,
                "context-header1": 1,
                "context-header4": 4
            }
        ],
        "variable-metadata": [
            {
                "name": "test_variable_metadata",
                "tlv-metadata": [
                    {
                        "tlv-class": 16,
                        "tlv-type": 5,
                        "tlv-data": "abcdefgh",
                        "length": 2,
                        "flags": "r1"
                    }
                ]
            },
            {
                "name": "test_variable_metadata1",
                "tlv-metadata": [
                    {
                        "tlv-class": 16,
                        "tlv-type": 5,
                        "tlv-data": "abcdefgh",
                        "length": 2,
                        "flags": "r1"
                    }
                ]
            }
        ]
    }
}
"""

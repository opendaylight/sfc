__author__ = "Reinaldo Penno"
__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.2"
__email__ = "rapenno@gmail.com"
__email__ = "andrej.kincel@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 02/06/2015"


from common import *
import argparse
from sfc_classifier_regression_messages import *


if __name__ == "__main__":

    acl_type_dict = {"IPV4": IETF_ACL_JSON_IPV4, "IPV6": IETF_ACL_JSON_IPV6, "MAC": IETF_ACL_JSON_MAC}
    sf_type_dict = {"IPV4": SERVICE_FUNCTIONS_JSON_IPV4, "IPV6": SERVICE_FUNCTIONS_JSON_IPV6,
                    "IPV64": SERVICE_FUNCTIONS_JSON_IPV4, "IPV46": SERVICE_FUNCTIONS_JSON_IPV6,
                     "MAC": SERVICE_FUNCTIONS_JSON_MAC}
    sff_type_dict = {"IPV4": SERVICE_FUNCTION_FORWARDERS_JSON_IPV4, "IPV6": SERVICE_FUNCTION_FORWARDERS_JSON_IPV6,
                     "IPV46": SERVICE_FUNCTION_FORWARDERS_JSON_IPV4_6, "IPV64": SERVICE_FUNCTION_FORWARDERS_JSON_IPV6_4,
                     "MAC": SERVICE_FUNCTION_FORWARDERS_JSON_MAC}

    parser = argparse.ArgumentParser(description='SFC Agent',
                                     usage=("\npython3.4 sfc_classifier_regression "
                                            "--acl-type --dp-type"))

    parser.add_argument('--acl-type', choices=acl_type_dict.keys(),
                    help='Set ACL matches type [' + ' '.join(acl_type_dict.keys()) + ']',
                    required=True)
    parser.add_argument('--dp-type', choices=sf_type_dict.keys(),
                    help='Set data plane type [' + ' '.join(sf_type_dict.keys()) + ']',
                    required=True)
    args = parser.parse_args()

    delete_configuration()
    put_and_check(SF_URL, sf_type_dict[args.dp_type], sf_type_dict[args.dp_type])
    check(SFT_URL, SERVICE_FUNCTION_TYPE_JSON, "Checking Service Function Type...")
    put_and_check(METADATA_URL, METADATA_JSON, METADATA_JSON)
    put_and_check(SFF_URL, sff_type_dict[args.dp_type], sff_type_dict[args.dp_type])
    put_and_check(SFC_URL, SERVICE_CHAINS_JSON, SERVICE_CHAINS_JSON)
    put_and_check(SFP_URL, SERVICE_PATH_JSON, SERVICE_PATH_JSON)
    post_rpc(RSP_RPC_URL, RENDERED_SERVICE_PATH_RPC_REQ, RENDERED_SERVICE_PATH_RPC_RESP)
    check(RSP_URL, RENDERED_SERVICE_PATH_RESP_JSON, "Checking RSP...")
    check(SFF_OPER_URL, SERVICE_FUNCTION_FORWARDERS_OPER_JSON, "Checking SFF Operational State...")
    check(SF_OPER_URL, SERVICE_FUNCTION_OPER_JSON, "Checking SF Operational State...")
    put_and_check(IETF_ACL_URL, acl_type_dict[args.acl_type], acl_type_dict[args.acl_type])
    put_and_check(SCF_URL, SERVICE_CLASSIFIER_JSON, SERVICE_CLASSIFIER_JSON)

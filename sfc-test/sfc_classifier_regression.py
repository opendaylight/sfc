import common
import argparse
import sfc_classifier_regression_messages as sfc_crm

__author__ = "Reinaldo Penno"
__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.2"
__email__ = "rapenno@gmail.com"
__email__ = "andrej.kincel@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 02/06/2015"

if __name__ == "__main__":

    acl_type_dict = {
        "IPV4": sfc_crm.IETF_ACL_JSON_IPV4,
        "IPV6": sfc_crm.IETF_ACL_JSON_IPV6,
        "MAC": sfc_crm.IETF_ACL_JSON_MAC
    }
    sf_type_dict = {
        "IPV4": sfc_crm.SERVICE_FUNCTIONS_JSON_IPV4,
        "IPV6": sfc_crm.SERVICE_FUNCTIONS_JSON_IPV6,
        "IPV64": sfc_crm.SERVICE_FUNCTIONS_JSON_IPV4,
        "IPV46": sfc_crm.SERVICE_FUNCTIONS_JSON_IPV6,
        "MAC": sfc_crm.SERVICE_FUNCTIONS_JSON_MAC
    }
    sff_type_dict = {
        "IPV4": sfc_crm.SERVICE_FUNCTION_FORWARDERS_JSON_IPV4,
        "IPV6": sfc_crm.SERVICE_FUNCTION_FORWARDERS_JSON_IPV6,
        "IPV46": sfc_crm.SERVICE_FUNCTION_FORWARDERS_JSON_IPV4_6,
        "IPV64": sfc_crm.SERVICE_FUNCTION_FORWARDERS_JSON_IPV6_4,
        "MAC": sfc_crm.SERVICE_FUNCTION_FORWARDERS_JSON_MAC
    }

    parser = argparse.ArgumentParser(
        description='SFC Agent',
        usage=("\npython3.4 sfc_classifier_regression --acl-type --dp-type"))

    parser.add_argument(
        '--acl-type',
        choices=acl_type_dict.keys(),
        help='Set ACL matches type [' + ' '.join(acl_type_dict.keys()) + ']',
        required=True)
    parser.add_argument(
        '--dp-type',
        choices=sf_type_dict.keys(),
        help='Set data plane type [' + ' '.join(sf_type_dict.keys()) + ']',
        required=True)
    args = parser.parse_args()

    common.delete_configuration()
    common.put_and_check(
        common.SF_URL,
        sf_type_dict[args.dp_type],
        sf_type_dict[args.dp_type])
    common.check(
        common.SFT_URL,
        sfc_crm.SERVICE_FUNCTION_TYPE_JSON,
        "Checking Service Function Type...")
    common.put_and_check(
        common.METADATA_URL,
        sfc_crm.METADATA_JSON,
        sfc_crm.METADATA_JSON)
    common.put_and_check(
        common.SFF_URL,
        sff_type_dict[args.dp_type],
        sff_type_dict[args.dp_type])
    common.put_and_check(
        common.SFC_URL,
        sfc_crm.SERVICE_CHAINS_JSON,
        sfc_crm.SERVICE_CHAINS_JSON)
    common.put_and_check(
        common.SFP_URL,
        sfc_crm.SERVICE_PATH_JSON,
        sfc_crm.SERVICE_PATH_JSON)
    common.post_rpc(
        common.RSP_RPC_URL,
        sfc_crm.RENDERED_SERVICE_PATH_RPC_REQ,
        sfc_crm.RENDERED_SERVICE_PATH_RPC_RESP)
    common.check(
        common.RSP_URL,
        sfc_crm.RENDERED_SERVICE_PATH_RESP_JSON,
        "Checking RSP...")
    common.check(
        common.SFF_OPER_URL,
        sfc_crm.SERVICE_FUNCTION_FORWARDERS_OPER_JSON,
        "Checking SFF Operational State...")
    common.check(
        common.SF_OPER_URL,
        sfc_crm.SERVICE_FUNCTION_OPER_JSON,
        "Checking SF Operational State...")
    common.put_and_check(
        common.IETF_ACL_URL,
        acl_type_dict[args.acl_type],
        acl_type_dict[args.acl_type])
    common.put_and_check(
        common.SCF_URL,
        sfc_crm.SERVICE_CLASSIFIER_JSON,
        sfc_crm.SERVICE_CLASSIFIER_JSON)

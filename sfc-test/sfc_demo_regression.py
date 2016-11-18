import common
import sfc_demo_regression_messages as sfc_drm
import argparse

__author__ = "Reinaldo Penno"
__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.3"
__email__ = "rapenno@gmail.com"
__email__ = "andrej.kincel@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 02/06/2015"


def run_rest_regression(host_ip):
    common.delete_configuration()
    common.put_and_check(
        common.SF_URL,
        sfc_drm.SERVICE_FUNCTIONS_JSON.replace("{ip}", host_ip),
        sfc_drm.SERVICE_FUNCTIONS_JSON.replace("{ip}", host_ip))
    common.check(
        common.SFT_URL,
        sfc_drm.SERVICE_FUNCTION_TYPE_JSON,
        "Checking Service Function Type...")
    common.put_and_check(
        common.SFF_URL,
        sfc_drm.SERVICE_FUNCTION_FORWARDERS_JSON.replace("{ip}", host_ip),
        sfc_drm.SERVICE_FUNCTION_FORWARDERS_JSON.replace("{ip}", host_ip))
    common.put_and_check(
        common.SFC_URL,
        sfc_drm.SERVICE_CHAINS_JSON,
        sfc_drm.SERVICE_CHAINS_JSON)
    common.put_and_check(
        common.SFP_URL,
        sfc_drm.SERVICE_PATH_JSON,
        sfc_drm.SERVICE_PATH_JSON)
    common.post_rpc(
        common.RSP_RPC_URL,
        sfc_drm.RENDERED_SERVICE_PATH_RPC_REQ,
        sfc_drm.RENDERED_SERVICE_PATH_RPC_RESP)
    common.check(
        common.RSP_URL,
        sfc_drm.RENDERED_SERVICE_PATH_RESP_JSON,
        "Checking RSP...")
    common.check(
        common.SFF_OPER_URL,
        sfc_drm.SERVICE_FUNCTION_FORWARDERS_OPER_JSON,
        "Checking SFF Operational State...")
    common.check(
        common.SF_OPER_URL,
        sfc_drm.SERVICE_FUNCTION_OPER_JSON,
        "Checking SF Operational State...")
    common.put_and_check(
        common.IETF_ACL_URL,
        sfc_drm.IETF_ACL_JSON_IPV4,
        sfc_drm.IETF_ACL_JSON_IPV4)
    common.put_and_check(
        common.SCF_URL,
        sfc_drm.SERVICE_CLASSIFIER_JSON,
        sfc_drm.SERVICE_CLASSIFIER_JSON)


def main():
    host_ip = "127.0.0.1"

    #: setup parser -----------------------------------------------------------
    parser = argparse.ArgumentParser(
        description='SFC Demo Regression',
        usage=("\npython3.4 sfc_basic_rest_regression "
               "--local-ip=<Local IP address>"))

    parser.add_argument('--local-ip', action='store', help='Local IP Address')

    #: parse CMD arguments ----------------------------------------------------
    args = parser.parse_args()

    if args.local_ip is not None:
        host_ip = args.local_ip

    run_rest_regression(host_ip)


if __name__ == "__main__":
    main()

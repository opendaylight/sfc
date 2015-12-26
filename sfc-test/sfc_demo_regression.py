__author__ = "Reinaldo Penno"
__author__ = "Andrej Kincel"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.3"
__email__ = "rapenno@gmail.com"
__email__ = "andrej.kincel@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 02/06/2015"

from common import *
from sfc_demo_regression_messages import *
import argparse


def run_rest_regression(host_ip):
    delete_configuration()
    put_and_check(SF_URL, SERVICE_FUNCTIONS_JSON.replace("{ip}", host_ip),
                  SERVICE_FUNCTIONS_JSON.replace("{ip}", host_ip))
    check(SFT_URL, SERVICE_FUNCTION_TYPE_JSON, "Checking Service Function Type...")
    put_and_check(SFF_URL, SERVICE_FUNCTION_FORWARDERS_JSON.replace("{ip}", host_ip),
                  SERVICE_FUNCTION_FORWARDERS_JSON.replace("{ip}", host_ip))
    put_and_check(SFC_URL, SERVICE_CHAINS_JSON, SERVICE_CHAINS_JSON)
    put_and_check(SFP_URL, SERVICE_PATH_JSON, SERVICE_PATH_JSON)
    post_rpc(RSP_RPC_URL, RENDERED_SERVICE_PATH_RPC_REQ, RENDERED_SERVICE_PATH_RPC_RESP)
    check(RSP_URL, RENDERED_SERVICE_PATH_RESP_JSON, "Checking RSP...")
    check(SFF_OPER_URL, SERVICE_FUNCTION_FORWARDERS_OPER_JSON, "Checking SFF Operational State...")
    check(SF_OPER_URL, SERVICE_FUNCTION_OPER_JSON, "Checking SF Operational State...")
    put_and_check(IETF_ACL_URL, IETF_ACL_JSON_IPV4, IETF_ACL_JSON_IPV4)
    put_and_check(SCF_URL, SERVICE_CLASSIFIER_JSON, SERVICE_CLASSIFIER_JSON)


def main():
    host_ip = "127.0.0.1"

    #: setup parser -----------------------------------------------------------
    parser = argparse.ArgumentParser(description='SFC Demo Regression',
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

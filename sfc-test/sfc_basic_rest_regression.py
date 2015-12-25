__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.4"
__email__ = "rapenno@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 10/05/2014"


from common import *
from sfc_basic_rest_regression_messages import *
import argparse


def run_rest_regression():
    print("Starting Test Execution")
    delete_configuration()
    put_and_check(SF_URL, SERVICE_FUNCTIONS_JSON, SERVICE_FUNCTIONS_JSON)
    check(SFT_URL, SERVICE_FUNCTION_TYPE_JSON, "Checking Service Function Type...")
    put_and_check(SFF_URL, SERVICE_FUNCTION_FORWARDERS_JSON, SERVICE_FUNCTION_FORWARDERS_JSON)
    put_and_check(SFC_URL, SERVICE_CHAINS_JSON, SERVICE_CHAINS_JSON)
    put_and_check(SFP_URL, SERVICE_PATH_JSON, SERVICE_PATH_JSON)
    post_rpc(RSP_RPC_URL, RENDERED_SERVICE_PATH_RPC_PATH_1_REQ, RENDERED_SERVICE_PATH_RPC_PATH_1_RESP)
    check(RSP_URL, RENDERED_SERVICE_PATH_RESP_JSON, "Checking RSP...")
    check(SFF_OPER_URL, SERVICE_FUNCTION_FORWARDERS_OPER_JSON, "Checking SFF Operational State...")
    check(SF_OPER_URL, SERVICE_FUNCTION_OPER_JSON, "Checking SF Operational State...")
    put_and_check(IETF_ACL_URL, IETF_ACL_JSON, IETF_ACL_JSON)
    put_and_check(SFP_ONE_URL.format("Path-3-SFC2"), SERVICE_PATH_ADD_ONE_JSON, SERVICE_PATH_ADD_ONE_RESP_JSON)
    input("Press Enter to finish tests")


def main():

    run_karaf = False
    p = None

    #: setup parser -----------------------------------------------------------
    parser = argparse.ArgumentParser(description='SFC Basic RestConf Regression',
                                     usage=("\npython3.4 sfc_basic_rest_regression "
                                            "--run-karaf "))

    parser.add_argument('--run-karaf', action='store_true',
                        help='Create SFC Karaf instance automatically')

    #: parse CMD arguments ----------------------------------------------------
    args = parser.parse_args()

    if args.run_karaf:
        run_karaf = True

    try:
        if run_karaf:
            p = initialize_karaf()
            if check_sfc_initialized(p):
                run_rest_regression()
            else:
                print("Bypassing tests..")

        # bundle_pattern = 'list \| grep sfc-netconf\s+\w+\s\|\s(\w+)(.+)'
        # child.expect('opendaylight-user', timeout=10)
        # child.sendline('bundle:list | grep sfc-netconf')
        # child.expect(bundle_pattern)
        # netconf_state, right = child.match.groups()

        else:
            run_rest_regression()
    except KeyboardInterrupt:
        pass
    finally:
        if p:
            p.terminate(force=True)

if __name__ == "__main__":
    main()


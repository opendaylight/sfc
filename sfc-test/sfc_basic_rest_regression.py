import common
import sfc_basic_rest_regression_messages as sfc_brrm
import argparse

__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.4"
__email__ = "rapenno@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 10/05/2014"


def run_rest_regression():
    print("Starting Test Execution")
    common.delete_configuration()
    common.put_and_check(
        common.SF_URL,
        sfc_brrm.SERVICE_FUNCTIONS_JSON,
        sfc_brrm.SERVICE_FUNCTIONS_JSON)
    common.check(
        common.SFT_URL,
        sfc_brrm.SERVICE_FUNCTION_TYPE_JSON,
        "Checking Service Function Type...")
    common.put_and_check(
        common.SFF_URL,
        sfc_brrm.SERVICE_FUNCTION_FORWARDERS_JSON,
        sfc_brrm.SERVICE_FUNCTION_FORWARDERS_JSON)
    common.put_and_check(
        common.SFC_URL,
        sfc_brrm.SERVICE_CHAINS_JSON,
        sfc_brrm.SERVICE_CHAINS_JSON)
    common.put_and_check(
        common.SFP_URL,
        sfc_brrm.SERVICE_PATH_JSON,
        sfc_brrm.SERVICE_PATH_JSON)
    common.post_rpc(
        common.RSP_RPC_URL,
        sfc_brrm.RENDERED_SERVICE_PATH_RPC_PATH_1_REQ,
        sfc_brrm.RENDERED_SERVICE_PATH_RPC_PATH_1_RESP)
    common.check(
        common.RSP_URL,
        sfc_brrm.RENDERED_SERVICE_PATH_RESP_JSON,
        "Checking RSP...")
    common.check(
        common.SFF_OPER_URL,
        sfc_brrm.SERVICE_FUNCTION_FORWARDERS_OPER_JSON,
        "Checking SFF Operational State...")
    common.check(
        common.SF_OPER_URL,
        sfc_brrm.SERVICE_FUNCTION_OPER_JSON,
        "Checking SF Operational State...")
    common.put_and_check(
        common.IETF_ACL_URL,
        sfc_brrm.IETF_ACL_JSON,
        sfc_brrm.IETF_ACL_JSON)
    common.put_and_check(
        common.SFP_ONE_URL.format("Path-3-SFC2"),
        sfc_brrm.SERVICE_PATH_ADD_ONE_JSON,
        sfc_brrm.SERVICE_PATH_ADD_ONE_RESP_JSON)
    input("Press Enter to finish tests")


def main():

    run_karaf = False
    p = None

    #: setup parser -----------------------------------------------------------
    parser = argparse.ArgumentParser(
        description='SFC Basic RestConf Regression',
        usage=("\npython3.4 sfc_basic_rest_regression --run-karaf "))

    parser.add_argument('--run-karaf', action='store_true',
                        help='Create SFC Karaf instance automatically')

    #: parse CMD arguments ----------------------------------------------------
    args = parser.parse_args()

    if args.run_karaf:
        run_karaf = True

    try:
        if run_karaf:
            p = common.initialize_karaf()
            if common.check_sfc_initialized(p):
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

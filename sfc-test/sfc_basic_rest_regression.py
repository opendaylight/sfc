__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.2"
__email__ = "rapenno@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 10/05/2014"


from common import *
from sfc_basic_rest_regression_messages import *


if __name__ == "__main__":
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

    put_and_check(SFP_ONE_URL.format("Path-3-SFC2"), SERVICE_PATH_ADD_ONE_JSON, SERVICE_PATH_ADD_ONE_JSON)
    check(RSP_URL, RENDERED_SERVICE_PATH_ADD_ONE_JSON, "Checking RSP after adding another SFP...")
    # delete_and_check(SF_ONE_URL.format("SF1"), "Deleting SF {}".format("SF1"))
    # check(RSP_URL, RENDERED_SERVICE_PATH_DEL_ONE_JSON, "Checking RSP after deleting one SF...")
    # check(SFT_URL, SERVICE_FUNCTION_TYPE_DELETE_ONE_SF_JSON, "Checking Service Function Types after deleting on SF...")
    # delete_configuration()


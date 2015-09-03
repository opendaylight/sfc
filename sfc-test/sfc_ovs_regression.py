__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.1"
__email__ = "rapenno@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 04/14/2015"


from common import *
from sfc_ovs_regression_messages import *
import time


if __name__ == "__main__":

    delete_configuration()
    put_and_check(SF_URL, SERVICE_FUNCTIONS_JSON, SERVICE_FUNCTIONS_JSON)
    check(SFT_URL, SERVICE_FUNCTION_TYPE_JSON, "Checking Service Function Type...")
    put_and_check(SFF_URL, SERVICE_FUNCTION_FORWARDERS_JSON, SERVICE_FUNCTION_FORWARDERS_JSON)
    put_and_check(SFC_URL, SERVICE_CHAINS_JSON, SERVICE_CHAINS_JSON)
    put_and_check(SFP_URL, SERVICE_PATH_JSON, SERVICE_PATH_JSON)
    post_rpc(RSP_RPC_URL, RENDERED_SERVICE_PATH_RPC_REQ, RENDERED_SERVICE_PATH_RPC_RESP)
    check(RSP_URL, RENDERED_SERVICE_PATH_RESP_JSON, "Checking RSP...")

    #check(SFF_OPER_URL, SERVICE_FUNCTION_FORWARDERS_OPER_JSON, "Checking SFF Operational State...")
    #check(SF_OPER_URL, SERVICE_FUNCTION_OPER_JSON, "Checking SF Operational State...")


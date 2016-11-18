import common
import sfc_ovs_regression_messages as sfc_orm

__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.1"
__email__ = "rapenno@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 04/14/2015"


if __name__ == "__main__":

    common.delete_configuration()
    common.put_and_check(
        common.SF_URL,
        sfc_orm.SERVICE_FUNCTIONS_JSON,
        sfc_orm.SERVICE_FUNCTIONS_JSON)
    common.check(
        common.SFT_URL,
        sfc_orm.SERVICE_FUNCTION_TYPE_JSON,
        "Checking Service Function Type...")
    common.put_and_check(
        common.SFF_URL,
        sfc_orm.SERVICE_FUNCTION_FORWARDERS_JSON,
        sfc_orm.SERVICE_FUNCTION_FORWARDERS_JSON)
    common.put_and_check(
        common.SFC_URL,
        sfc_orm.SERVICE_CHAINS_JSON,
        sfc_orm.SERVICE_CHAINS_JSON)
    common.put_and_check(
        common.SFP_URL,
        sfc_orm.SERVICE_PATH_JSON,
        sfc_orm.SERVICE_PATH_JSON)
    common.post_rpc(
        common.RSP_RPC_URL,
        sfc_orm.RENDERED_SERVICE_PATH_RPC_REQ,
        sfc_orm.RENDERED_SERVICE_PATH_RPC_RESP)
    common.check(
        common.RSP_URL,
        sfc_orm.RENDERED_SERVICE_PATH_RESP_JSON,
        "Checking RSP...")

    '''
    common.check(
        common.SFF_OPER_URL,
        sfc_orm.SERVICE_FUNCTION_FORWARDERS_OPER_JSON,
        "Checking SFF Operational State...")
    '''
    '''
    common.check(
        common.SF_OPER_URL,
        sfc_orm.SERVICE_FUNCTION_OPER_JSON,
        "Checking SF Operational State...")
    '''

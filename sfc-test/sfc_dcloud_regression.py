__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.2"
__email__ = "rapenno@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 10/05/2014"

import requests
import json
import time
from sfc_dcloud_messages import *

putheaders = {'content-type': 'application/json'}
getheaders = {'Accept': 'application/json'}
# ODL IP:port
ODLIP = "127.0.0.1:8181"
# Static URLs for testing
SF_URL = "http://" + ODLIP + "/restconf/config/service-function:service-functions/"
SFC_URL = "http://" + ODLIP + "/restconf/config/service-function-chain:service-function-chains/"
SFF_URL = "http://" + ODLIP + "/restconf/config/service-function-forwarder:service-function-forwarders/"
SFT_URL = "http://" + ODLIP + "/restconf/config/service-function-type:service-function-types/"
SFP_URL = "http://" + ODLIP + "/restconf/config/service-function-path:service-function-paths/"
SFF_OPER_URL = "http://" + ODLIP + "/restconf/operational/service-function-forwarder:service-function-forwarders-state/"
SF_OPER_URL = "http://" + ODLIP + "/restconf/operational/service-function:service-functions-state/"
RSP_URL = "http://" + ODLIP + "/restconf/operational/rendered-service-path:rendered-service-paths/"
SFP_ONE_URL = "http://" + ODLIP + "/restconf/config/service-function-path:service-function-paths/" \
                                  "service-function-path/{}/"
SF_ONE_URL = "http://" + ODLIP + "/restconf/config/service-function:service-functions/service-function/{}/"

USERNAME = "admin"
PASSWORD = "admin"


def delete_configuration():
    s = requests.Session()
    print("Deleting previous config... \n")
    r = s.delete(SF_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("=>Deleted all Service Functions \n")
    else:
        print("=>Failure to delete SFs, response code = {} \n".format(r.status_code))
    r = s.delete(SFC_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
       print("=>Deleted all Service Function Chains \n")
    else:
       print("=>Failure to delete SFCs, response code = {} \n".format(r.status_code))
    r = s.delete(SFF_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("=>Deleted all Service Function Forwarders \n")
    else:
        print("=>Failure to delete SFFs, response code = {} \n".format(r.status_code))
    # r = s.delete(SFT_URL, stream=False, auth=(USERNAME, PASSWORD))
    # if r.status_code == 200:
    #     print("=>Deleted all Service Function Types \n")
    # else:
    #     print("=>Failure to delete SFTs, response code = {} \n". format(r.status_code))
    r = s.delete(SFP_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("=>Deleted all Service Function Paths \n")
    else:
        print("=>Failure to delete SFPs, response code = {} \n".format(r.status_code))


def put_and_check(url, json_req, json_resp):
    s = requests.Session()
    print("PUTing {} \n".format(url))
    r = s.put(url, data=json_req, headers=putheaders, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("Checking... \n")
        # Creation of SFPs is slow, need to pause here.
        time.sleep(2)
        r = s.get(url, stream=False, auth=(USERNAME, PASSWORD))
        if (r.status_code == 200) and (json.loads(r.text) == json.loads(json_resp)):
            print("=>Creation successfully \n")
        else:
            print("=>Creation did not pass check, error code: {}. If error code was 2XX it is "
                  "probably a false negative due to string compare \n".format(r.status_code))
    else:
        print("=>Failure \n")


def check(url, json_resp, message):
    s = requests.Session()
    print(message, "\n")
    r = s.get(url, stream=False, auth=(USERNAME, PASSWORD))
    if (r.status_code == 200) and (json.loads(r.text) == json.loads(json_resp)):
        print("=>Check successful \n")
    else:
        print("=>Check not successful, error code: {}. If error code was 2XX it is "
              "probably a false negative due to string compare \n".format(r.status_code))


def delete_and_check(url, message):
    s = requests.Session()
    print(message, "\n")
    r = s.delete(url, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("=>Check successful \n")
    else:
        print("=>Check not successful, error code: {} \n".format(r.status_code))

if __name__ == "__main__":
    delete_configuration()
    put_and_check(SF_URL, SERVICE_FUNCTIONS_JSON, SERVICE_FUNCTIONS_JSON)
    check(SFT_URL, SERVICE_FUNCTION_TYPE_JSON, "Checking Service Function Type...")
    put_and_check(SFF_URL, SERVICE_FUNCTION_FORWARDERS_JSON, SERVICE_FUNCTION_FORWARDERS_JSON)
    put_and_check(SFC_URL, SERVICE_CHAINS_JSON, SERVICE_CHAINS_JSON)
    put_and_check(SFP_URL, SERVICE_PATH_JSON, SERVICE_PATH_RESP_JSON)
    check(RSP_URL, RENDERED_SERVICE_PATH_RESP_JSON, "Checking RSP...")
    check(SFF_OPER_URL, SERVICE_FUNCTION_FORWARDERS_OPER_JSON, "Checking SFF Operational State...")
    check(SF_OPER_URL, SERVICE_FUNCTION_OPER_JSON, "Checking SF Operational State...")
    put_and_check(SFP_ONE_URL.format("Path-3-SFC2"), SERVICE_PATH_ADD_ONE_JSON, SERVICE_PATH_ADD_ONE_JSON)
    check(RSP_URL, RENDERED_SERVICE_PATH_ADD_ONE_JSON, "Checking RSP after adding another SFP...")
    delete_and_check(SF_ONE_URL.format("SF1"), "Deleting SF {}".format("SF1"))
    check(RSP_URL, RENDERED_SERVICE_PATH_DEL_ONE_JSON, "Checking RSP after deleting one SF...")
    check(SFT_URL, SERVICE_FUNCTION_TYPE_DELETE_ONE_SF_JSON, "Checking Service Function Types after deleting on SF...")
    #delete_configuration()


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

USERNAME = "admin"
PASSWORD = "admin"


def delete_configuration():
    s = requests.Session()
    print ("Deleting previous config... \n")
    r = s.delete(SF_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print ("=>Deleted all Service Functions \n")
    else:
        print ("=>Failure to delete SFs, response code = {} \n". format(r.status_code))
    r = s.delete(SFC_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print ("=>Deleted all Service Function Chains \n")
    else:
        print ("=>Failure to delete SFs, response code = {} \n". format(r.status_code))
    r = s.delete(SFF_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print ("=>Deleted all Service Function Forwarders \n")
    else:
        print ("=>Failure to delete SFFs, response code = {} \n". format(r.status_code))
    # r = s.delete(SFT_URL, stream=False, auth=(USERNAME, PASSWORD))
    # if r.status_code == 200:
    #     print ("=>Deleted all Service Function Types \n")
    # else:
    #     print ("=>Failure to delete SFTs, response code = {} \n". format(r.status_code))
    r = s.delete(SFP_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print ("=>Deleted all Service Function Paths \n")
    else:
        print ("=>Failure to delete SFPs, response code = {} \n". format(r.status_code))


def put_and_check(url, json_req, json_resp):
    s = requests.Session()
    print ("PUTing {} \n".format(url))
    r = s.put(url, data=json_req, headers=putheaders, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print ("Checking... \n")
        # Creation of SFPs is slow, need to pause here.
        time.sleep(2)
        r = s.get(url, stream=False, auth=(USERNAME, PASSWORD))
        if (r.status_code == 200) and (json.loads(r.text) == json.loads(json_resp)):
            print ("=>Creation successfully \n")
        else:
            print ("=>Creation did not pass check, error code: {} \n".format(r.status_code))
    else:
        print ("=>Failure \n")


if __name__ == "__main__":
    delete_configuration()
    put_and_check(SF_URL, SERVICE_FUNCTIONS_JSON, SERVICE_FUNCTIONS_JSON)
    #put_and_check(SFF_URL, SERVICE_FUNCTION_FORWARDERS_JSON, SERVICE_FUNCTION_FORWARDERS_JSON)
    #put_and_check(SFC_URL, SERVICE_CHAINS_JSON, SERVICE_CHAINS_JSON)
    #put_and_check(SFP_URL, SERVICE_PATH_JSON, SERVICE_PATH_RESP_JSON)


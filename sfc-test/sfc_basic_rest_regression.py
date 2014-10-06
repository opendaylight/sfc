__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__license__ = "New-style BSD"
__version__ = "0.2"
__email__ = "rapenno@gmail.com"
__status__ = "Tested with SFC-Karaf distribution as of 10/05/2014"

import requests
import json
import time
from sfc_basic_rest_regression_messages import *

putheaders = {'content-type': 'application/json'}
getheaders = {'Accept': 'application/json'}
# ODL IP:port
ODLIP = "127.0.0.1:8181"
# Static URLs for testing
GET_ALL_SF_URL = "http://" + ODLIP + "/restconf/config/service-function:service-functions/"
GET_ALL_SFC_URL = "http://" + ODLIP + "/restconf/config/service-function-chain:service-function-chains/"
GET_ALL_SFF_URL = "http://" + ODLIP + "/restconf/config/service-function-forwarder:service-function-forwarders/"
GET_ALL_SFT_URL = "http://" + ODLIP + "/restconf/config/service-function-type:service-function-types/"
GET_ALL_SFP_URL = "http://" + ODLIP + "/restconf/config/service-function-path:service-function-paths/"
PUT_ONE_SFP_URL = "http://" + ODLIP + "/restconf/config/service-function-path:service-function-paths/"
PUT_SFC3_URL = "http://" + ODLIP + "/restconf/config/service-function-chain:service-function-chains/" \
                                   "service-function-chain/SFC3/"

GETURL = "http://" + ODLIP + "/restconf/config/service-function:service-functions/service-function/%d/"
# Incremental PUT. This URL is for a list element
PUTURL = "http://" + ODLIP + "/restconf/config/service-function:service-functions/service-function/%d/"

USERNAME = "admin"
PASSWORD = "admin"


def get_initial_sf():
    s = requests.Session()
    print ("GETTing initially configured Service Functions \n")
    r = s.get(GET_ALL_SF_URL, stream=False, auth=(USERNAME, PASSWORD))
    if (r.status_code == 200) and (json.loads(r.text) == json.loads(GET_ALL_SF_RESP_JSON)):
        print ("=>Success \n")
    else:
        print ("=>Failure to get SFs \n")


def get_initial_sfc():
    s = requests.Session()
    print ("GETTing initially configured Service Functions Chains \n")
    r = s.get(GET_ALL_SFC_URL, stream=False, auth=(USERNAME, PASSWORD))
    if (r.status_code == 200) and (json.loads(r.text) == json.loads(GET_ALL_SFC_RESP_JSON)):
        print ("=>Success \n")
    else:
        print ("=>Failure to get SFCs \n")


def get_initial_sff():
    s = requests.Session()
    print ("GETTing initially configured Service Functions Forwarders \n")
    r = s.get(GET_ALL_SFF_URL, stream=False, auth=(USERNAME, PASSWORD))
    if (r.status_code == 200) and (json.loads(r.text) == json.loads(GET_ALL_SFF_RESP_JSON)):
        print ("=>Success \n")
    else:
        print ("=>Failure to get SFFs \n")


def get_initial_sft():
    s = requests.Session()
    print ("GETTing initially configured Service Functions Types \n")
    r = s.get(GET_ALL_SFT_URL, stream=False, auth=(USERNAME, PASSWORD))
    if (r.status_code == 200) and (json.loads(r.text) == json.loads(GET_ALL_SFT_RESP_JSON)):
        print ("=>Success \n")
    else:
        print ("=>Failure to get SFTs \n")


def put_one_sfp():
    s = requests.Session()
    print ("PUTing a single SFP \n")
    r = s.put(PUT_ONE_SFP_URL, data=PUT_ONE_SFP_JSON, headers=putheaders, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print ("Checking created SFP \n")
        # Creation of SFPs is slow, need to pause here.
        time.sleep(2)
        r = s.get(GET_ALL_SFP_URL, stream=False, auth=(USERNAME, PASSWORD))
        if (r.status_code == 200) and (json.loads(r.text) == json.loads(GET_ALL_SFP_RESP_JSON)):
            print ("=>SFP created successfully \n")
        else:
            print ("=>Created SFP did not pass check - Failure \n")

    else:
        print ("=>Failed to create SFP \n")


def put_sfc3():
    s = requests.Session()
    print ("PUTing a single SFC \n")
    r = s.put(PUT_SFC3_URL, data=PUT_SFC3_JSON, headers=putheaders, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print ("Checking created SFC \n")
        # Creation of SFPs is slow, need to pause here.
        time.sleep(2)
        r = s.get(GET_ALL_SFC_URL, stream=False, auth=(USERNAME, PASSWORD))
        if (r.status_code == 200) and (json.loads(r.text) == json.loads(GET_ALL_SFC3_RESP_JSON)):
            print ("=>SFC created successfully \n")
        else:
            print ("=>Created SFC did not pass check - Failure \n")
    else:
        print ("=>Failed to create SFC \n")


if __name__ == "__main__":
    get_initial_sf()
    get_initial_sfc()
    get_initial_sff()
    get_initial_sft()
    put_one_sfp()
    put_sfc3()

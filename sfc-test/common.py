__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "rapenno@gmail.com"
__status__ = "alpha"

import requests
import json
from subprocess import *
import pexpect
import time

put_json_headers = {'content-type': 'application/json'}
get_json_headers = {'Accept': 'application/json'}

post_xml_headers = {'content-type': 'application/xml', 'Accept': 'application/xml'}
get_xml_headers = {'Accept': 'application/xml'}


# ODL IP:port
ODLIP = "localhost:8181"
NETCONF_CONNECTOR_IP_PORT = "127.0.0.1:8181"
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
SFF_ONE_URL = "http://" + ODLIP + "/restconf/config/service-function-forwarder:service-function-forwarders/" \
                                  "service-function-forwarder/{}/"
IETF_ACL_URL = "http://" + ODLIP + "/restconf/config/ietf-acl:access-lists/"
RSP_RPC_URL = "http://" + ODLIP + "/restconf/operations/rendered-service-path:create-rendered-path"
SCF_URL = "http://" + ODLIP + "/restconf/config/service-function-classifier:service-function-classifiers/"
METADATA_URL = "http://" + ODLIP + "/restconf/config/service-function-metadata:service-function-metadata/"


NETCONF_CONNECTOR_URL = "http://" + NETCONF_CONNECTOR_IP_PORT + "/restconf/config/network-topology:network-topology/" \
                        "topology/topology-netconf/node/controller-config/yang-ext:mount/config:modules"

SFF_OVS_RPC_URL = "http://" + ODLIP + "/restconf/operations/service-function-forwarder-ovs:create-ovs-bridge"

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
    r = s.delete(SFP_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("=>Deleted all Service Function Paths \n")
    else:
        print("=>Failure to delete SFPs, response code = {} \n".format(r.status_code))
        
    r = s.delete(METADATA_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("=>Deleted all metadata \n")
    else:
        print("=>Failure to delete metadata, response code = {} \n".format(r.status_code))
        
    r = s.delete(IETF_ACL_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("=>Deleted all Access Lists \n")
    else:
        print("=>Failure to delete ACLs, response code = {} \n".format(r.status_code))
    r = s.delete(SCF_URL, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("=>Deleted all Service Classifiers \n")
    else:
        print("=>Failure to delete Classifiers, response code = {} \n".format(r.status_code))


def put_and_check(url, json_req, json_resp):
    s = requests.Session()
    print("PUTing {} \n".format(url))
    r = s.put(url, data=json_req, headers=put_json_headers, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("Checking... \n")
        # Creation of SFPs is slow, need to pause here.
        time.sleep(2)
        r = s.get(url, stream=False, auth=(USERNAME, PASSWORD))
        if (r.status_code == 200) and (ordered(json.loads(r.text)) == ordered(json.loads(json_resp))):
            print("=>Creation successfully \n")
        else:
            print("=>Creation did not pass check, error code: {}. If error code was 2XX it is "
                  "probably a false negative due to string compare \n".format(r.status_code))
    else:
        print("=>Failure, status code: {} \n".format(r.status_code))


def check(url, json_resp, message):
    s = requests.Session()
    print(message, "\n")
    r = s.get(url, stream=False, auth=(USERNAME, PASSWORD))
    if (r.status_code == 200) and (ordered(json.loads(r.text)) == ordered(json.loads(json_resp))):
        print("=>Check successful \n")
    else:
        print("=>Check not successful, error code: {}. If error code was 2XX it is "
              "probably a false negative due to string compare \n".format(r.status_code))


def post_netconf_rpc(url, json_input):
    s = requests.Session()
    print("POSTing RPC {} \n".format(url))
    r = s.post(url, data=json_input, headers=put_json_headers, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("=>RPC posted successfully \n")
    else:
        print("=>Failure, status code: {} \n".format(r.status_code))


def post_rpc(url, json_input, json_resp):
    s = requests.Session()
    print("POSTing RPC {} \n".format(url))
    r = s.post(url, data=json_input, headers=put_json_headers, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("Checking... \n")
        if (r.status_code == 200) and (json.loads(r.text) == json.loads(json_resp)):
            print("=>RPC posted successfully \n")
        else:
            print("=>RPC unsuccessful, error code: {}. If error code was 2XX it is "
                  "probably a false negative due to string compare \n".format(r.status_code))


def post_netconf_connector(url, xml_input):
    s = requests.Session()
    print("POSTing Netconf Connector {} \n".format(url))
    r = s.post(url, data=xml_input, headers=post_xml_headers, stream=False, auth=(USERNAME, PASSWORD), timeout=5)
    if r.status_code == 204:
        print("=>POST successful \n")
    else:
        print("=>Failure, status code: {} \n".format(r.status_code))


def delete_and_check(url, message):
    s = requests.Session()
    print(message, "\n")
    r = s.delete(url, stream=False, auth=(USERNAME, PASSWORD))
    if r.status_code == 200:
        print("=>Check successful \n")
    else:
        print("=>Check not successful, error code: {} \n".format(r.status_code))


def initialize_karaf():
    """
    Initializes Karaf

    :return child:  A spawn object

    """

    # Another option is to start Karaf server in background
    # and connect to remote server port
    # try:
    # process = Popen(
    # ['../sfc-karaf/target/assembly/bin/start'])
    #
    # except CalledProcessError as e:
    #     print(e.output)

    print("Starting SFC Karaf. This will take 2 minutes...")
    child = pexpect.spawn('../sfc-karaf/target/assembly/bin/karaf clean')
    # We wait 120 seconds for SFC karaf to start
    time.sleep(120)
    return child


def check_sfc_initialized(child):
    """
    Checks if SFC was initialized properly

    :param child: a pexpect handle
    :type child: a spawn object
    :return init_string:  "SfcOvsModule" if properly initialized, None otherwise

    """
    ret = False
    ovs_pattern = 'display \| grep \"SFC OVS module initialized\"\s{2}.+?(?=SfcOvsModule)(\w+)'
    karaf_cmd = 'log:display | grep \"SFC OVS module initialized\"'
    try:
        child.sendline(karaf_cmd)
        child.expect(ovs_pattern, timeout=5)
        init_string = child.match.groups()
        if init_string[0].decode("utf-8") == "SfcOvsModule":
            ret = True
    except pexpect.TIMEOUT:
        print("SFC not initialized properly, timeout waiting for command answer")
    except KeyError:
        print("SFC not initialized properly, empty answer from Karaf")
    return ret


# http://stackoverflow.com/questions/25851183/how-to-compare-two-json-objects-with-the-same-elements-in-a-different-order-equa


def ordered(obj):
    if isinstance(obj, dict):
        return sorted((k, ordered(v)) for k, v in obj.items())
    if isinstance(obj, list):
        return sorted(ordered(x) for x in obj)
    else:
        return obj
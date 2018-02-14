import argparse
import requests, json
from requests.auth import HTTPBasicAuth
import time
import sys
import os
import pprint
import socket

class ConfigBase:
    localIp = None
    controller = None
    DEFAULT_PORT = '8181'

    USERNAME = 'admin'
    PASSWORD = 'admin'

    SERVICE_FUNCTION = '/restconf/config/service-function:service-functions'
    SERVICE_FUNCTION_FORWARDER = '/restconf/config/service-function-forwarder:service-function-forwarders'
    SERVICE_FUNCTION_CHAIN ='/restconf/config/service-function-chain:service-function-chains'
    SERVICE_FUNCTION_PATH = '/restconf/config/service-function-path:service-function-paths'
    ACCESS_CONTROL_LIST='/restconf/config/ietf-access-control-list:access-lists'
    SERVICE_RENDERED_PATH = '/restconf/operations/rendered-service-path:create-rendered-path'
    SERVICE_CLASSIFICATION_FUNTION = '/restconf/config/service-function-classifier:service-function-classifiers'
    SERVICE_RENDERED_PATH_DEL = '/restconf/operations/rendered-service-path:delete-rendered-path'
    CONTEXT_METADATA = '/restconf/config/service-function-path-metadata:service-function-metadata'
    DISABLE_STATISTICS = '/restconf/operations/statistics-manager-control:change-statistics-work-mode/'
    SERVICE_FUNCTION_TYPE = '/restconf/config/service-function-type:service-function-types'



    deleteAll=False

    def readParameters(self):
        if len(sys.argv) <= 1:
            print "missing controller IP information"
            exit(1)
        print sys.argv[1]
        self.controller = sys.argv[1]
        self.local = self.getLocalIp()
        if len(sys.argv) > 2:
            self.deleteAll = bool(sys.argv[2])

    def getLocalIp(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            s.connect((self.controller, int(self.DEFAULT_PORT)))
        except:
            print "Can't connect to controller on %s:%s" % (self.controller, self.DEFAULT_PORT)
            raise
        self.localIp = s.getsockname()[0]
        s.close()

    def get(self, host, port, uri):
        url = 'http://' + host + ":" + port + uri
        r = requests.get(url, auth=HTTPBasicAuth(self.USERNAME, self.PASSWORD))
        jsondata = json.loads(r.text)
        #pprint(jsondata)
        #import pdb
        #pdb.set_trace()
        print jsondata
        return jsondata

    def getFlowOnJson(self, json):
        if 'flow-node-inventory:table' in json:
            for key1 in json['flow-node-inventory:table']:
                if 'flow' in key1:
                    for key2 in key1['flow']:
                        if 'flow-name' in key2:
                            return key2
        return None


    def searchAndDeleteMatchAny(self, json, uri, host, port):
        remaning = True
        while remaning:
            key2 = self.getFlowOnJson(json)
            print key2
            if key2 != None and key2['flow-name'] == 'MatchAny':
                uri2 = uri + '/flow/' + key2['id']
                self.delete(host, port, uri2)
            else:
                remaning = False

    def deleteFlows(self, urll, uri, host, port):
        remaning = True
        while remaning:
            r = requests.get(urll, auth=HTTPBasicAuth(self.USERNAME, self.PASSWORD))
            if r.status_code == 503:
                return;
            j = json.loads(r.text)
            key2 = self.getFlowOnJson(j)
            print key2
            if key2 != None:
                uri2 = uri + '/flow/' + key2['id']
                self.delete(host, port, uri2, True)
            else:
                remaning = False


    def searchVlanID(self, json):
        vlanid = []
        if 'flow-node-inventory:table' in json:
            for key1 in json['flow-node-inventory:table']:
                if 'flow' in key1:
                    for key2 in key1['flow']:
                        if 'flow-name' in key2:
                            if key2 != None and key2['flow-name'] == 'nextHop' and key2['priority'] == 350:
                                vlanid.append(key2['match']['vlan-match']['vlan-id']['vlan-id'])
        print vlanid
        if not vlanid:
            return None
        vl = min(int(s) for s in vlanid)
        print vl
        return vl

    def searchAndDeleteVlanID(self, json, uri, host, port):
        remaning = True
        while remaning:
            key2 = self.getFlowOnJson(json)
            if key2 != None and key2['flow-name'] == 'nextHop' and key2['priority'] == 450:
                uri2 = uri + '/flow/' + key2['id']
                self.delete(host, port, uri2)
            else:
                remaning = False



    def getVlanId(self, host, port, sw, table):
        uri = '/restconf/config/opendaylight-inventory:nodes/node/' + sw + '/flow-node-inventory:table/' + str(table)
        url = 'http://' + host + ":" + port + uri
        r = requests.get(url, auth=HTTPBasicAuth(self.USERNAME, self.PASSWORD))
        j = json.loads(r.text)
        return self.searchVlanID(j)


    def deleteAllFlows(self, host, port, sw):
        for table in range(1, 11):
            uri = '/restconf/config/opendaylight-inventory:nodes/node/' + sw + '/flow-node-inventory:table/' + str(table)
            url = 'http://' + host + ":" + port + uri
            print uri
            self.deleteFlows(url, uri, host, port)


    def getAndDel(self, host, port, sw, table):
        uri = '/restconf/config/opendaylight-inventory:nodes/node/' + sw + '/flow-node-inventory:table/'+str(table)
        url = 'http://' + host + ":" + port + uri
        r = requests.get(url, auth=HTTPBasicAuth(self.USERNAME, self.PASSWORD))
        j = json.loads(r.text)
        self.searchAndDeleteMatchAny(j, uri, host, port)

    def delRemainingVlanId(self, host, port, sw, table):
        uri = '/restconf/config/opendaylight-inventory:nodes/node/' + sw + '/flow-node-inventory:table/'+str(table)
        url = 'http://' + host + ":" + port + uri
        r = requests.get(url, auth=HTTPBasicAuth(self.USERNAME, self.PASSWORD))
        j = json.loads(r.text)
        self.searchAndDeleteVlanID(j, uri, host, port)


    def delete(self, host, port, uri, debug=False):
        '''Perform a DELETE rest operation, using the URL and data provided'''

        url = 'http://' + host + ":" + port + uri

        headers = {'Content-type': 'application/yang.data+json',
                   'Accept': 'application/yang.data+json'}
        if debug == True:
            print "DELETE %s" % url
        r = requests.delete(url, headers=headers, auth=HTTPBasicAuth(self.USERNAME, self.PASSWORD))
        if debug == True:
            print r.text
        #r.raise_for_status()


    def put(self, host, port, uri, data, debug=False):
        '''Perform a PUT rest operation, using the URL and data provided'''

        url = 'http://' + host + ":" + port + uri

        headers = {'Content-type': 'application/yang.data+json',
                   'Accept': 'application/yang.data+json'}
        if debug == True:
            print "PUT %s" % url
            print json.dumps(data, indent=4, sort_keys=True)
        r = requests.put(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(self.USERNAME, self.PASSWORD))
        if debug == True:
            print r.text
        r.raise_for_status()
        time.sleep(1)


    def post(self, host, port, uri, data, debug=False):
        '''Perform a POST rest operation, using the URL and data provided'''

        url = 'http://' + host + ":" + port + uri
        headers = {'Content-type': 'application/yang.data+json',
                   'Accept': 'application/yang.data+json'}
        if debug == True:
            print "POST %s" % url
            print json.dumps(data, indent=4, sort_keys=True)
        r = requests.post(url, data=json.dumps(data), headers=headers, auth=HTTPBasicAuth(self.USERNAME, self.PASSWORD))
        if debug == True:
            print r.text
        #r.raise_for_status()
        time.sleep(1)



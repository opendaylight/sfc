#!/usr/bin/python
from controllerConfigBase import ConfigBase
from subprocess import call
import time
import json


class sfcEncap:
    VLAN = 1
    MAC_CHAIN = 2

class odlConf(ConfigBase):
    sfcEncap = None

    def __init__(self, sfcEncap):
        self.sfcEncap = sfcEncap

    def clean(self, chainList, swList):

        for chain in chainList:
            for chainName, conf in chain.iteritems():
                delChain = {}
                delChain['input'] = {}
                delChain['input']['name'] = conf['rsp']['input']['name']
                self.post(self.controller, self.DEFAULT_PORT, self.SERVICE_RENDERED_PATH_DEL, delChain, True)
                delChainRev = {}
                delChainRev['input'] = {}
                delChainRev['input']['name'] = conf['rsp']['input']['name'] + "-Reverse"
                self.post(self.controller, self.DEFAULT_PORT, self.SERVICE_RENDERED_PATH_DEL, delChainRev, True)

        self.delete(self.controller, self.DEFAULT_PORT, self.SERVICE_CLASSIFICATION_FUNTION, True)
        self.delete(self.controller, self.DEFAULT_PORT, self.SERVICE_FUNCTION_PATH, True)
        self.delete(self.controller, self.DEFAULT_PORT, self.SERVICE_FUNCTION_CHAIN, True)
        self.delete(self.controller, self.DEFAULT_PORT, self.SERVICE_FUNCTION_FORWARDER, True)
        self.delete(self.controller, self.DEFAULT_PORT, self.SERVICE_FUNCTION, True)
        if self.sfcEncap == sfcEncap.MAC_CHAIN:
            self.delete(self.controller, self.DEFAULT_PORT, self.ACCESS_CONTROL_LIST, True)


        for sw in swList:
            for swTopo, conf in sw.iteritems():
                print conf['CONF']['service-function-forwarder'][0]['service-node']
                self.deleteAllFlows(self.controller, self.DEFAULT_PORT, conf['CONF']['service-function-forwarder'][0]['service-node'])
                call('ovs-ofctl -OOpenFlow13 del-flows %s' % (swTopo.name), shell=True)

    def sfConf(self, name, id, type, ip, sff, vlan, macs, ports, confSff):

        sf = {}
        sf['name'] = name
        sf['type'] = "service-function-type:" + type
        sf['ip-mgmt-address'] = ip[0]
        sf['sf-data-plane-locator'] = []

        i = 0
        for port, mac in zip(ports, macs):
            sfDpl = {}
            sfDpl['name'] = name + "-plane-" + str(i)
            sfDpl['service-function-forwarder'] = sff
            sfDpl['vlan-id'] = vlan
            if (i == 0):
                sfDpl['mac'] = mac
            else:  # if it is two arm (snort case) seting the scond arm mac equals to the src mac addres from the input
                sfDpl['mac'] = 'FF:00:00:00:FF:%s%d' % (id, port)

            i += 1
            sfDpl['transport'] = "service-locator:mac"
            sf['sf-data-plane-locator'].append(sfDpl)

            # sff connected conf
            sffSfDpl = {}
            sffSfDpl['name'] = sff + "-dpl-" + name + str(port)
            sffSfDpl['data-plane-locator'] = {}
            sffSfDpl['data-plane-locator']['vlan-id'] = (500 + int(id))
            sffSfDpl['data-plane-locator']['mac'] = 'FF:00:00:00:FF:%s%d' % (id, port)
            sffSfDpl['data-plane-locator']['transport'] = "service-locator:mac"
            sffSfDpl['service-function-forwarder-ofs:ofs-port'] = {}
            sffSfDpl['service-function-forwarder-ofs:ofs-port']['port-id'] = port

            confSff['service-function-forwarder'][0]['sff-data-plane-locator'].append(sffSfDpl)

            sfdict = {}

            sfdict['name'] = sf['name']  # must be the same name as SF name
            sfdict['sff-sf-data-plane-locator'] = {}
            sfdict['sff-sf-data-plane-locator']['sf-dpl-name'] = sfDpl['name']
            sfdict['sff-sf-data-plane-locator']['sff-dpl-name'] = sffSfDpl['name']

            confSff['service-function-forwarder'][0]['service-function-dictionary'].append(sfdict)
        sfs = {}
        sfs['service-function'] = []
        sfs['service-function'].append(sf)

        return sfs

    def sfType(self, type):
        sfType = {}
        sfType['type'] = type
        sfType['l2-transparent'] = True

        sfTypes = {}
        sfTypes['service-function-type'] = []
        sfTypes['service-function-type'].append(sfType)

        return sfTypes



    def sffConfBase(self, swName, id, uuid):

        print "config sff"
        print self.controller

        sff = {}
        sff['name'] = swName
        sff['service-node'] = "openflow:" + id
        sff['service-function-forwarder-ovs:ovs-bridge'] = {}
        sff['service-function-forwarder-ovs:ovs-bridge']['bridge-name'] = swName
        sff['service-function-forwarder-ovs:ovs-bridge']['openflow-node-id'] = "openflow:" + id
        #sff['service-function-forwarder-ovs:ovs-bridge']['uuid'] = uuid
        sff['service-function-forwarder-ovs:ovs-node'] = {}
        #sff['service-function-forwarder-ovs:ovs-node']['node-id'] = "/network-topology:network-topology/network-topology:topology[network-topology:topology-id='ovsdb:1']/network-topology:node[network-topology:node-id='ovsdb://uuid/"+uuid+"']"
        sff['sff-data-plane-locator'] = []

        sfdpl = {}

        sfdpl['name'] = swName + "-ip"
        sfdpl['data-plane-locator'] = {}
        sfdpl['data-plane-locator']['transport'] = "service-locator:vxlan-gpe"
        sfdpl['data-plane-locator']['port'] = 6633
        sfdpl['data-plane-locator']['ip'] = self.controller

        sff['sff-data-plane-locator'].append(sfdpl)

        sff['service-function-dictionary'] = []

        sff['connected-sff-dictionary'] = []

        sffs = {}
        sffs['service-function-forwarder'] = []
        sffs['service-function-forwarder'].append(sff)
        print json.dumps(sffs, indent=4)
        return sffs

    def appendSffConf(self, confSff1, p1, id1, confSff2, p2, id2):

        toSffDpl1 = {}
        toSffDpl1['name'] = "to-" + confSff2['service-function-forwarder'][0]['name']
        toSffDpl1['data-plane-locator'] = {}
        toSffDpl1['data-plane-locator']['vlan-id'] = '%s%s'% (id1, id2)
        toSffDpl1['data-plane-locator']['mac'] = 'AA:00:00:00:AA:%s%s' % (id1, id2)
        toSffDpl1['data-plane-locator']['transport'] = "service-locator:mac"
        toSffDpl1['service-function-forwarder-ofs:ofs-port'] = {}
        toSffDpl1['service-function-forwarder-ofs:ofs-port']['port-id'] = p1



        toSffDpl2 = {}
        toSffDpl2['name'] = "to-" + confSff1['service-function-forwarder'][0]['name']
        toSffDpl2['data-plane-locator'] = {}
        toSffDpl2['data-plane-locator']['vlan-id'] = '%s%s' % (id1, id2)
        toSffDpl2['data-plane-locator']['mac'] = 'AA:00:00:00:AA:%s%s' % (id1, id2)
        toSffDpl2['data-plane-locator']['transport'] = "service-locator:mac"
        toSffDpl2['service-function-forwarder-ofs:ofs-port'] = {}
        toSffDpl2['service-function-forwarder-ofs:ofs-port']['port-id'] = p2

        confSff1['service-function-forwarder'][0]['sff-data-plane-locator'].append(toSffDpl1)
        confSff2['service-function-forwarder'][0]['sff-data-plane-locator'].append(toSffDpl2)

        sffdpl1 = {}
        sffdpl1['name'] = confSff2['service-function-forwarder'][0]['name']
        sffdpl1['sff-sff-data-plane-locator'] = {}
        sffdpl1['sff-sff-data-plane-locator']['mac'] = 'AA:00:00:00:AA:%s%s' % (id1, id2)
        sffdpl1['sff-sff-data-plane-locator']['transport'] = "service-locator:mac"

        sffdpl2 = {}
        sffdpl2['name'] = confSff1['service-function-forwarder'][0]['name']
        sffdpl2['sff-sff-data-plane-locator'] = {}
        sffdpl2['sff-sff-data-plane-locator']['mac'] = 'AA:00:00:00:AA:%s%s' % (id1, id2)
        sffdpl2['sff-sff-data-plane-locator']['transport'] = "service-locator:mac"

        confSff1['service-function-forwarder'][0]['connected-sff-dictionary'].append(sffdpl1)
        confSff2['service-function-forwarder'][0]['connected-sff-dictionary'].append(sffdpl2)


    def appendSffTermination(self, confSff, port, id1, mac):
        termination = {}
        termination['name'] = confSff['service-function-forwarder'][0]['name'] + "-to-gateway"
        termination['data-plane-locator'] = {}
        termination['data-plane-locator']['mac'] = 'AA:00:00:00:AA:5%s' % (id1)
        termination['data-plane-locator']['transport'] = "service-locator:mac"
        termination['service-function-forwarder-termination:termination-point'] = {}
        termination['service-function-forwarder-termination:termination-point']['port-id'] = port
        termination['service-function-forwarder-termination:termination-point']['mac-address'] = mac

        confSff['service-function-forwarder'][0]['sff-data-plane-locator'].append(termination)


    def setChain(self, name, chainList):

        chain = {}
        chain['name'] = name
        chain['sfc-service-function'] = []

        for element in chainList:
            chainElement = {}
            chainElement['name'] = element
            chainElement['type'] = "service-function-type:" + element
            chainElement['order'] = chainList.index(element)
            chain['sfc-service-function'].append(chainElement)


        sfc = {}
        sfc['service-function-chain'] = []
        sfc['service-function-chain'].append(chain)

        return sfc


    def setChainPath(self, name, scf1, scf2):
        chainPath = {}
        chainPath['service-function-path'] = {}
        chainPath['service-function-path']['name'] = name + "-path"
        chainPath['service-function-path']['service-chain-name'] = name
        chainPath['service-function-path']['classifier'] = scf1
        chainPath['service-function-path']['symmetric-classifier'] = scf2
        chainPath['service-function-path']['symmetric'] = "true"#"true"
        chainPath['service-function-path']['transport-type'] = "service-locator:mac"
        if self.sfcEncap == sfcEncap.MAC_CHAIN:
            chainPath['service-function-path']['sfc-encapsulation'] = "service-locator:mac-chaining"

        return chainPath

    def setClassifier1(self, sff, aclName):

        classifier = {}
        classifier['service-function-classifier'] = {}
        classifier['service-function-classifier']['name'] = sff + "-classifier-1" + aclName
        classifier['service-function-classifier']['scl-service-function-forwarder'] = []

        scf = {}
        scf['name'] = sff
        scf['interface'] = "SFF1-eth1" #classifier just used to configure termination path (VLAN technique)

        classifier['service-function-classifier']['scl-service-function-forwarder'].append(scf)

        classifier['service-function-classifier']['acl'] = {}
        classifier['service-function-classifier']['acl']['name'] = aclName
        classifier['service-function-classifier']['acl']['type'] = "ietf-access-control-list:ipv4-acl"

        return classifier

    def setClassifier2(self, sff, aclName):

        classifier = {}
        classifier['service-function-classifier'] = {}
        classifier['service-function-classifier']['name'] = sff + "-classifier-2" + aclName
        classifier['service-function-classifier']['scl-service-function-forwarder'] = []

        scf = {}
        scf['name'] = sff
        scf['interface'] = "SFF1-eth2"  # classifier just used to configure termination path (VLAN technique)

        classifier['service-function-classifier']['scl-service-function-forwarder'].append(scf)

        classifier['service-function-classifier']['acl'] = {}
        classifier['service-function-classifier']['acl']['name'] = aclName
        classifier['service-function-classifier']['acl']['type'] = "ietf-access-control-list:ipv4-acl"

        return classifier

    def aclRuleUp(self, rsp, aclName):

        acl = {}
        acl['acl'] = {}
        acl['acl']['acl-type'] = "ietf-access-control-list:ipv4-acl"
        acl['acl']['acl-name'] = aclName

        acl['acl']['access-list-entries'] = {}
        acl['acl']['access-list-entries']['ace'] = []
        ace = {}
        ace['matches'] = {}
        ace['matches']['destination-ipv4-network'] = "10.0.0.2/32"
        ace['matches']['protocol'] = "17"
        #ace['matches']['source-port-range'] = {}
        #ace['matches']['source-port-range']['lower-port'] = "0"
        #ace['matches']['source-port-range']['upper-port'] = "65000"
        #ace['matches']['destination-port-range'] = {}
        #ace['matches']['destination-port-range']['lower-port'] = "0"
        #ace['matches']['destination-port-range']['upper-port'] = "65000"

        ace['actions'] = {}
        ace['actions']['service-function-acl:rendered-service-path'] = rsp
        ace['rule-name'] = rsp + ".rule"

        acl['acl']['access-list-entries']['ace'].append(ace)

        return acl

    def aclRuleUpTcp(self, rsp, aclName):

        acl = {}
        acl['acl'] = {}
        acl['acl']['acl-type'] = "ietf-access-control-list:ipv4-acl"
        acl['acl']['acl-name'] = aclName

        acl['acl']['access-list-entries'] = {}
        acl['acl']['access-list-entries']['ace'] = []
        ace = {}
        ace['matches'] = {}
        ace['matches']['destination-ipv4-network'] = "10.0.0.2/32"
        ace['matches']['protocol'] = "6"
        #ace['matches']['source-port-range'] = {}
        #ace['matches']['source-port-range']['lower-port'] = "1"
        #ace['matches']['source-port-range']['upper-port'] = "6000"
        #ace['matches']['destination-port-range'] = {}
        #ace['matches']['destination-port-range']['lower-port'] = "1"
        #ace['matches']['destination-port-range']['upper-port'] = "6000"

        ace['actions'] = {}
        ace['actions']['service-function-acl:rendered-service-path'] = rsp
        ace['rule-name'] = rsp + ".tcp.rule"

        acl['acl']['access-list-entries']['ace'].append(ace)

        return acl

    def aclRuleDownTcp(self, rsp, aclName):
        acl = {}
        acl['acl'] = {}
        acl['acl']['acl-type'] = "ietf-access-control-list:ipv4-acl"
        acl['acl']['acl-name'] = aclName

        acl['acl']['access-list-entries'] = {}
        acl['acl']['access-list-entries']['ace'] = []
        ace = {}
        ace['matches'] = {}
        ace['matches']['destination-ipv4-network'] = "10.0.0.1/32"
        ace['matches']['protocol'] = "6"
        #ace['matches']['source-port-range'] = {}
        #ace['matches']['source-port-range']['lower-port'] = "1"
        #ace['matches']['source-port-range']['upper-port'] = "6000"
        #ace['matches']['destination-port-range'] = {}
        #ace['matches']['destination-port-range']['lower-port'] = "1"
        #ace['matches']['destination-port-range']['upper-port'] = "6000"

        ace['actions'] = {}
        ace['actions']['service-function-acl:rendered-service-path'] = rsp + "-Reverse"
        ace['rule-name'] = rsp + ".tcp.rule"

        acl['acl']['access-list-entries']['ace'].append(ace)

        return acl


    def aclRuleDown(self, rsp, aclName):
        acl = {}
        acl['acl'] = {}
        acl['acl']['acl-type'] = "ietf-access-control-list:ipv4-acl"
        acl['acl']['acl-name'] = aclName

        acl['acl']['access-list-entries'] = {}
        acl['acl']['access-list-entries']['ace'] = []
        ace = {}
        ace['matches'] = {}
        ace['matches']['destination-ipv4-network'] = "10.0.0.1/32"
        ace['matches']['protocol'] = "17"
        #ace['matches']['source-port-range'] = {}
        #ace['matches']['source-port-range']['lower-port'] = "0"
        #ace['matches']['source-port-range']['upper-port'] = "65000"
        #ace['matches']['destination-port-range'] = {}
        #ace['matches']['destination-port-range']['lower-port'] = "0"
        #ace['matches']['destination-port-range']['upper-port'] = "65000"

        ace['actions'] = {}
        ace['actions']['service-function-acl:rendered-service-path'] = rsp + "-Reverse"
        ace['rule-name'] = rsp + ".rule"

        acl['acl']['access-list-entries']['ace'].append(ace)

        return acl


    def rederedRPC(self, chainPath):
        rsp = {}
        rsp['input'] = {}
        rsp['input']['name'] = chainPath + "-rend"
        rsp['input']['parent-service-function-path'] = chainPath
        rsp['input']['symmetric'] = "true"#"true"

        return rsp

    def disableStatistics (self):
        input = {}
        input['input'] = {}
        input['input']['mode'] = "FULLY_DISABLED"
        return  input




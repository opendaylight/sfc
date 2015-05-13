#! /usr/bin/env python
#
# Copyright (c) 2015 Qosmos.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import logging
from scapy.all import Ether, IP
from scapyclassifier.nsh import NSH

# For Matcher class
from scapyclassifier.classifiers.classifiers import Classifiers

__author__ = "Christophe Fontaine"
__copyright__ = "Copyright(c) 2015, Qosmos"
__version__ = "0.1"
__email__ = "christophe.fontaine@qosmos.com"
__status__ = "alpha"

DEFAULT_SERVICE_PATH = 0xFFFFFF
logger = logging.getLogger(__file__)


class ActionManager(object):
    def __init__(self):
        self._defaultAction = Actions()
        self.forceEmitPacket = False
        self.outSocket = None
        self._matchers = dict()

    def addMatcher(self, matcher, action, reversepathaction=None):
        self._matchers[matcher] = (action, reversepathaction)

    def resetActions(self):
        self._matchers = dict()

    def getAction(self, flow, direction):
        for matcher in self._matchers.keys():
            if matcher.isMatch(flow):
                (action, reversepathaction) = self._matchers[matcher]
                if direction == 0:
                    return action
                else:
                    return reversepathaction
        return None


class Actions(object):
    def __init__(self):
        self.isnsh = True
        self.outsocket = None
        self.sp = DEFAULT_SERVICE_PATH
        self.si = 1
        self.npc = 0
        self.nsc = 0
        self.spc = 0
        self.ssc = 0

    def buildPacket(self, pkt):
        if self.isnsh is True:
            outpkt = NSH(ServicePath=self.sp, ServiceIndex=self.si,
                         MDType=1, NextProto=3, Reserved=24,
                         NPC=self.npc, NSC=self.nsc,
                         SPC=self.spc, SSC=self.ssc)/pkt
        else:  # TODO: Build packet for MPLS, VLAN, GRE ...
            outpkt = pkt
        return outpkt


class Matcher(object):
    def __init__(self):
        self.rsp_name = None
        self.inputinterface = None
        self.applicationIds = []
        self.sourceIpV4 = None
        self.destIpV4 = None
        self.sourceIpV4Mask = None
        self.destIpV4Mask = None
        self.sourceIpV6 = None
        self.destIpV6 = None
        self.sourceIpV6Mask = None
        self.destIpV6Mask = None
        self.sourceportrange = (None, None)
        self.destportrange = (None, None)
        self.ipproto = None
        self.dscp = None
        self.sourcemac = None
        self.sourcemacmask = None
        self.destmac = None
        self.destmacmask = None

    @staticmethod
    def Build(matches):
        m = Matcher()
        # L7
        if 'service-function-acl:application-id' in matches:
            for appName in matches['service-function-acl:application-id']:
                c = Classifiers.GetClassifierFromName(appName)
                if c is not None:
                    m.applicationIds.append(c)

        # L4
        if 'source-ipv4-address' in matches:
            m.sourceIpV4 = matches['source-ipv4-address']
        if 'source-ipv4-address-mask' in matches:
            m.sourceIpV4Mask = matches['source-ipv4-address-mask']
        if 'dest-ipv4-address' in matches:
            m.destIpV4 = matches['dest-ipv4-address']
        if 'dest-ipv4-address-mask' in matches:
            m.destIpV4Mask = matches['dest-ipv4-address-mask']
        if 'source-ipv6-address' in matches:
            m.sourceIpV6 = matches['source-ipv6-address']
        if 'source-ipv6-address-mask' in matches:
            m.sourceIpV6Mask = matches['source-ipv6-address-mask']
        if 'dest-ipv6-address' in matches:
            m.destIpV6 = matches['dest-ipv6-address']
        if 'dest-ipv6-address-mask' in matches:
            m.destIpV6Mask = matches['dest-ipv6-address-mask']
        if 'source-port-range' in matches:
            low = None
            up = None
            if 'lower-port' in matches['source-port-range']:
                low = matches['source-port-range']['lower-port']
            if 'upper-port' in matches['source-port-range']:
                up = matches['source-port-range']['upper-port']
            m.sourceportrange = (low, up)

        if 'destination-port-range' in matches:
            low = None
            up = None
            if 'lower-port' in matches['destination-port-range']:
                low = matches['destination-port-range']['lower-port']
            if 'upper-port' in matches['destination-port-range']:
                up = matches['destination-port-range']['upper-port']
            m.destportrange = (low, up)

        if 'ip-protocol' in matches:
            m.ipproto = matches['ip-protocol']
        if 'dscp' in matches:
            m.dscp = matches['dscp']

        # L2
        if 'source-mac-address' in matches:
            m.sourcemac = matches['source-mac-address']
        if 'source-mac-address-mask' in matches:
            m.sourcemacmask = matches['source-mac-address-mask']
        if 'destination-mac-address' in matches:
            m.destmac = matches['destination-mac-address']
        if 'destination-mac-address-mask' in matches:
            m.destmacmask = matches['destination-mac-address-mask']

        # L1
        if 'input-interface' in matches:
            m.inputinterface = matches['input-interface']

    def isMatch(self, flow):
        pkt = flow.packets[0]
        if (flow.classifier is not None) and (len(self.applicationIds) > 0):
            if flow.classifier not in self.applicationIds:
                logger.debug("flow.classifier %s is not in matcher applicationIds ", flow.classifier)
                return False

        if self.sourceIpV4 is not None:
            if self.sourceIpV4Mask is None:
                if str(pkt[IP].src) != self.sourceIpV4:
                    logger.debug("srcIpV4 %s != %s", str(pkt[IP].src), self.sourceIpV4)
                    return False

        if self.destIpV4 is not None:
            if self.destIpV4Mask is None:
                if str(pkt[IP].dst) != self.destIpV4:
                    logger.debug("dstIpV4 %s != %s", str(pkt[IP].dst), self.destIpV4)
                    return False

        (low, up) = self.sourceportrange
        if low is not None:
            if pkt[IP].sport < low:
                logger.debug("sport %d not in range %d %d", pkt[IP].sport, low, up)
                return False

        if up is not None:
            if pkt[IP].sport > up:
                logger.debug("sport %d not in range %d %d", pkt[IP].sport, low, up)
                return False

        (low, up) = self.destportrange
        if low is not None:
            if pkt[IP].dport < low:
                logger.debug("dport %d not in range %d %d", pkt[IP].dport, low, up)
                return False

        if up is not None:
            if pkt[IP].dport > up:
                logger.debug("dport %d not in range %d %d", pkt[IP].dport, low, up)
                return False

        if self.ipproto is not None:
            if pkt[IP].proto != self.ipproto:
                logger.debug("ipproto %s not %d", pkt[IP].proto, self.ipproto)
                return False

        if self.dscp is not None:
            if pkt[IP].tos != self.dscp:
                logger.debug("DSCP %d not %d", pkt[IP].tos, self.dscp)
                return False

        if self.sourcemac is not None:
            if self.sourcemacmask is None:
                if str(pkt[Ether].src) != self.sourcemacmask:
                    return False

        if self.destmac is not None:
            if self.destmacmask is None:
                if str(pkt[Ether].dst) != self.destmac:
                    return False

        return True

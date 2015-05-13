#! /usr/bin/env python
#
# Copyright (c) 2015 Qosmos.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import logging
from scapy.all import IP, TCP
from .classifiers import Classifiers, Classifier

__author__ = "Christophe Fontaine"
__copyright__ = "Copyright(c) 2015, Qosmos"
__version__ = "0.1"
__email__ = "christophe.fontaine@qosmos.com"

logger = logging.getLogger('l7classifier')


class HTTPClassifier(Classifier):
    def __init__(self):
        super().__init__()
        self.name = 'HTTP'
        self.classId = 3
        self.selectorId = 80

    def classify(self, packetList):
        try:
            # each element is (pkt, direction)
            lasttuple = packetList[len(packetList)-1]
            last = lasttuple[0]
            p = str(last[IP][TCP].payload)
            if p.find('GET') >= 0 or p.find('POST') >= 0 or p.find('PUT') >= 0:
                logger.debug('HTTP: Pattern Found in %s ', p)
                return True
            return False
        except:
            return False


# Template for domain name based classification
class ServerClassifier(Classifier):
    def __init__(self):
        super().__init__()
        self.name = ''
        self.classId = 6
        self.selectorId = 0
        self.domainname = None

    def classify(self, packetList):
        try:
            lasttuple = packetList[len(packetList)-1]
            last = lasttuple[0]
            p = str(last[IP][TCP].payload)
            if self.domainname is not None and p.find(self.domainname) >= 0:
                logger.debug('HTTP: Pattern Found in %s ', p)
                return True
            return False
        except:
            return False


class Server1Classifier(ServerClassifier):
    def __init__(self):
        super().__init__()
        self.name = 'Server1'
        self.selectorId = 1
        self.domainname = 'server1'


class Server2Classifier(ServerClassifier):
    def __init__(self):
        super().__init__()
        self.name = 'Server2'
        self.selectorId = 2
        self.domainname = 'server2'


class Server3Classifier(ServerClassifier):
    def __init__(self):
        super().__init__()
        self.name = 'Server3'
        self.selectorId = 3
        self.domainname = 'server3'


_hc = HTTPClassifier()
Classifiers.register(_hc)
Classifiers.register_child(_hc, Server1Classifier())
Classifiers.register_child(_hc, Server2Classifier())
Classifiers.register_child(_hc, Server3Classifier())

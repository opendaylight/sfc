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


class SSHClassifier(Classifier):
    def __init__(self):
        super().__init__()
        self.name = 'SSH'
        self.classId = 3
        self.selectorId = 22

    def classify(self, packetList):
        try:
            # each element is (pkt, direction)
            lasttuple = packetList[len(packetList)-1]
            last = lasttuple[0]
            p = str(last[IP][TCP].payload)
            if p.find('ssh') >= 0 or p.find('SSH') >= 0:
                logger.debug('SSH: Pattern Found')
                return True
            return False
        except:
            return False

Classifiers.register(SSHClassifier())

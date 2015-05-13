#! /usr/bin/env python
#
# Copyright (c) 2015 Qosmos.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import os
import sys
import time
import hashlib
import threading
import logging
from collections import OrderedDict
from scapy.all import Ether, IP, TCP, UDP

# fix Python 3 relative imports inside packages
# CREDITS: http://stackoverflow.com/a/6655098/4183498
parent_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(1, parent_dir)
# from . import classifiers
from .classifiers.classifiers import Classifiers, Unknown

logger = logging.getLogger('l7classifier')


class FlowManager(object):
    def __init__(self):
        self.flows = OrderedDict()
        self.runSweeper = False
        self.lock = threading.RLock()
        self.startSweeper()

    @staticmethod
    def computeHashs(pkt):
        hash = hashlib.md5()
        hash2 = hashlib.md5()  # return path
        try:
            hash.update(pkt[IP].src)
            hash.update(pkt[IP].dst)
#            hash.update(pkt[IP].proto)
            hash2.update(pkt[IP].dst)
            hash2.update(pkt[IP].src)
#            hash.update(pkt[IP].proto)
            if pkt[IP].proto == 6:  # TCP
                hash.update(str(pkt[IP][TCP].sport))
                hash.update(str(pkt[IP][TCP].dport))
                hash2.update(str(pkt[IP][TCP].dport))
                hash2.update(str(pkt[IP][TCP].sport))
            elif pkt[Ether][IP].proto == 17:  # UDP
                hash.update(str(pkt[IP][UDP].sport))
                hash.update(str(pkt[IP][UDP].dport))
                hash2.update(str(pkt[IP][UDP].dport))
                hash2.update(str(pkt[IP][UDP].sport))
            return [hash.hexdigest(), hash2.hexdigest()]
        except:
            return ['', '']

    def startSweeper(self):
        if self.runSweeper is False:
            self.runSweeper = True
            threading.Thread(target=self.sweeper, name='Flows sweeper').start()

    def stopSweeper(self):
        self.runSweeper = False

    def sweeper(self):
        while self.runSweeper is True:
            time.sleep(1)
            self.lock.acquire(True)
            for flow in list(self.flows.values()):
                if time.time() > flow.timeout:
                    logger.info('Timeout for flow %d', flow.index)
                    flow.delete = True
                    self.delete(flow.hash)
            self.lock.release()

    def getFlow(self, pkt):
        hashs = FlowManager.computeHashs(pkt)
        self.lock.acquire(True)
        if hashs[0] in self.flows:
            fd = (self.flows.get(hashs[0]), 0)
        elif hashs[1] in self.flows:
            fd = (self.flows.get(hashs[1]), 1)
        else:
            self.flows[hashs[0]] = None
            flow = Flow(list(self.flows.keys()).index(hashs[0]), hashs[0])
            self.flows[hashs[0]] = flow
            fd = (flow, 0)
        self.lock.release()
        return fd

    def delete(self, hash):
        self.lock.acquire(True)
        if hash in self.flows:
            flow = self.flows.pop(hash)
            logger.debug('Del Flow [%s], packet count = %s' %
                         (str(flow.index), str(len(flow.packets))))
        self.lock.release()


class Flow(object):
    DEFAULT_TIMEOUT = 30.000  # time in s

    def __init__(self, index, hash):
        self.timeout = time.time() + Flow.DEFAULT_TIMEOUT
        self.index = index
        self.hash = hash
        self.classifier = None
        self.packets = []
        self.delete = False
        self.actions = None

    def update(self, pkt, direction):
        logger.debug('Flow %d pkt update', self.index)
        self.timeout = time.time() + Flow.DEFAULT_TIMEOUT

        if self.classifier is None:
            if len(self.packets) > 10:
                logger.info('Flow [%d]: Not classified ', self.index)
                self.classifier = Unknown()
            else:
                self.packets.append((pkt, direction))
                self.classifier = Classifiers.classify(self.packets)
                if self.classifier is not None:
                    self.packets = []  # free packet list
                    logger.info('Flow [%s]: classification result is "%s"' %
                                (str(self.index), str(self.classifier.name)))
        return (self.classifier is not None)

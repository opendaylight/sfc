#! /usr/bin/env python
#
# Copyright (c) 2015 Qosmos.  All rights reserved.
#
# This progractionManager and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
import os
import sys
import logging
import scapy  # flake8: noqa
from scapy.all import interact, conf, log_scapy, L2ListenSocket, L2Socket
from scapy.layers.inet import Ether

# fix Python 3 relative imports inside packages
# CREDITS: http://stackoverflow.com/a/6655098/4183498
parent_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(1, parent_dir)

import scapyclassifier  # flake8: noqa
__package__ = 'scapyclassifier'

from scapyclassifier.nsh import NSH
from scapyclassifier.flows import FlowManager
from scapyclassifier.actions import ActionManager
from scapyclassifier.classifiers import classifiers

__author__ = "Christophe Fontaine"
__copyright__ = "Copyright(c) 2015, Qosmos"
__version__ = "0.1"
__email__ = "christophe.fontaine@qosmos.com"
__status__ = "alpha"


logger = logging.getLogger('l7classifier')
logger.addHandler(log_scapy)
logger.setLevel(10)
ingress = "eth1"
egress = "vxlan0"


def processPacket(flowManager, actionManager, sendSocket, inpkt):
    logger.debug('---------------------------')
    # 1. Extract pkt as the pkt may be encapsulated in NSH
    if NSH in inpkt:
        if inpkt[NSH].ServiceIndex <= 1:
            return  # drop packet
        pkt = inpkt[NSH][Ether]
        logger.debug('pkt Received %s', inpkt[NSH].mysummary())
    else:
        pkt = inpkt
    # 2. get associated flow, update flow infos
    #     and update classification if required
    (flow, direction) = flowManager.getFlow(pkt)
    flow.update(pkt, direction)
    logger.debug('pkt Received %s direction: %d', pkt.summary(), direction)
    if flow.delete is True:
        flowManager.delete(flow.hash)
    logger.debug("Classification -- Apply Actions")

    if flow.actions is None:
        if (flow.classifier is None) or (flow.classifier is classifiers.Unknown):
            actions = actionManager.getAction(flow, direction)  # Get L2-L4 Action
            if NSH in inpkt:
                actions.sp = inpkt[NSH].ServicePath
                actions.si = inpkt[NSH].ServiceIndex - 1
                actions.npc = inpkt[NSH].NPC
                actions.nsc = inpkt[NSH].NSC
                actions.spc = inpkt[NSH].SPC
                actions.ssc = inpkt[NSH].SSC
        else:
            logger.debug("flow.classifier: " + str(flow.classifier.name))
            actions = actionManager.getAction(flow, direction)  # Get L2-L7 Action
            flow.actions = actions
    else:
        actions = flow.actions

    outpkt = actions.buildPacket(pkt)
    if actions.isnsh:
        logger.debug("{" + outpkt[NSH].mysummary() + "} " + outpkt[NSH].summary())
    if actions.outsocket is not None:
        actions.outsocket.send(outpkt)
    elif actionManager.forceEmitPacket is True:
        actionManager.outSocket.send(outpkt)
    else:
        logger.debug("Socket is null, dropping packet")


def startClassifier(flowManager=None, actionManager=None, iface_in=ingress, iface_out=egress):
    conf.verb = 0
    if flowManager is None:
        flowManager = FlowManager()
    if actionManager is None:
        actionManager = ActionManager()
        logger.info('No Action defined, will set all NSP to 0xFFFFFF')
        # Example : HTTP Flow C->S path 1, return path 2
        actionManager.setPathsForAppId((3 << 24) + 80, 1, 2)
    pkts = None
    try:
        logger.info("Classification starts")
        listenSocket = L2ListenSocket(iface=iface_in, nofilter=1)
        if actionManager.forceEmitPacket is True:
            actionManager.outSocket = L2Socket(iface=iface_out, nofilter=1)
        oldpkt = None
        while True:
            pkt = listenSocket.recv()
            if pkt != oldpkt:
                processPacket(flowManager, actionManager, pkt)
                oldpkt = pkt
    except Exception as e:
        logger.critical(str(e))
        raise e
    finally:
        logger.info("Classification ends")
        flowManager.stopSweeper()
        actionManager.resetActions()
        return pkts


def start():
    am = ActionManager()
    am.forceEmitPacket = True
    startClassifier(actionManager=am)

if __name__ == "__main__":
    interact(mydict=globals(), mybanner="""
    Scapy with VxLAN GPE and NSH Support
    - Use start() to start the L7 classifier
    """)

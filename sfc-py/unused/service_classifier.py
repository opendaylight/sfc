#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import struct
import socket
import logging
import binascii

from netfilterqueue import NetfilterQueue

from nsh.encode import build_packet
from nsh.common import VXLANGPE, BASEHEADER, CONTEXTHEADER


__author__ = "Jim Guichard"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "jguichar@cisco.com"
__status__ = "alpha"


"""
Service Classifier
"""

logger = logging.getLogger(__name__)


#: constants
classify_map = {"172.16.6.140": {"sff": "172.16.6.141", "port": "4789"}}

vxlan_values = VXLANGPE(int('00000100', 2), 0, 0x894F,
                        int('111111111111111111111111', 2), 64)
ctx_values = CONTEXTHEADER(0xffffffff, 0, 0xffffffff, 0)
base_values = BASEHEADER(0x1, int('01000000', 2), 0x6, 0x1, 0x1, 0x000001, 0x4)

# Testing: Setup linux with:
# iptables -I INPUT -d 172.16.6.140 -j NFQUEUE --queue_num 1
#
#


def process_and_accept(packet):
    packet.accept()
    data = packet.get_payload()
    address = int(binascii.hexlify(data[16:20]), 16)
    lookup = socket.inet_ntoa(struct.pack(">I", address))

    try:
        if classify_map[lookup]['sff'] != '':
            packet = build_packet(vxlan_values, base_values, ctx_values) + data

            logger.info(binascii.hexlify(data))
            logger.info(binascii.hexlify(packet))

            UDP_IP = classify_map[lookup]['sff']
            UDP_PORT = int(classify_map[lookup]['port'])
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

            try:
                sock.sendto(packet, (UDP_IP, UDP_PORT))

                if __debug__ is False:
                    logger.debug('Sending NSH encapsulated packet to SFF: %s',
                                 UDP_IP)

            except socket.error as exc:
                logger.exception('Socket Error: %s', exc)

            finally:
                sock.close()

    except KeyError as exc:
        logger.exception('Classification failed: %s', exc)

nfqueue = NetfilterQueue()
nfqueue.bind(1, process_and_accept)

try:
    nfqueue.run()
except KeyboardInterrupt:
    print

import getopt
import logging
import sys

__author__ = "Reinaldo Penno, Jim Guichard, Paul Quinn"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "repenno@cisco.com, jguichar@cisco.com, paulq@cisco.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

"""Service Function Forwarder (SFF) Client. This module
   is a classifier node. It builds the NSH packet and sends to the first SFF
   in the path"""

import asyncio
from ctypes import *
from nsh_decode import *
from nsh_encode import *

try:
    import signal
except ImportError:
    signal = None

sfp_topo = {}

# VXLAN, NSH Base Header, and NSH Context Header data structures.

class VXLANGPE(Structure):
    _fields_ = [("flags", c_ubyte),
                ("reserved", c_ubyte),
                ("protocol_type", c_ushort),
                ("vni", c_uint, 24),
                ("reserved2", c_uint, 8)]


class BASEHEADER(Structure):
    _fields_ = [("version", c_ushort, 2),
                ("flags", c_ushort, 8),
                ("length", c_ushort, 6),
                ("md_type", c_ubyte),
                ("next_protocol", c_ubyte),
                ("service_path", c_uint, 24),
                ("service_index", c_uint, 8)]


class CONTEXTHEADER(Structure):
    _fields_ = [("network_platform", c_uint),
                ("network_shared", c_uint),
                ("service_platform", c_uint),
                ("service_shared", c_uint)]

# Global flags used for indication of current packet processing status.

PACKET_CHAIN = 0b00000000  # Packet needs more processing within this SFF
PACKET_CONSUMED = 0b00000001  # Packet was sent to another SFF or service function
PACKET_ERROR = 0b00000010  # Packet will be dropped
SERVICEFUNCTION_INVALID = 0xDEADBEEF  # Referenced service function is invalid

# Client side code: Choose values for VXLAN, base NSH and context headers as part of packet generation

vxlan_values = VXLANGPE(int('00000100', 2), 0, 0x894F, int('111111111111111111111111', 2), 64)
ctx_values = CONTEXTHEADER(0xffffffff, 0, 0xffffffff, 0)
base_values = BASEHEADER(0x1, int('01000000', 2), 0x6, 0x1, 0x1, 0x000001, 0x3)

# Server side code: Store received values for VXLAN, base NSH and context headers data structures

server_vxlan_values = VXLANGPE()
server_ctx_values = CONTEXTHEADER()
server_base_values = BASEHEADER()


# Client side code: Build NSH packet encapsulated in VXLAN & NSH.


class MyUdpClient:
    def connection_made(self, transport):
        self.transport = transport
        # Building client packet to send to SFF
        packet = build_packet(vxlan_values, base_values, ctx_values)
        print('\nsending packet to SFF:\n', binascii.hexlify(packet))
        # Send the packet
        self.transport.sendto(packet)

    def datagram_received(self, data, addr):
        print('\nreceived packet from SFF:\n', binascii.hexlify(data))
        print('\n')
        # Decode all the headers
        decode_vxlan(data, server_vxlan_values)
        decode_baseheader(data, server_base_values)
        decode_contextheader(data, server_ctx_values)
        self.loop.stop()

    def connection_refused(self, exc):
        print('Connection refused:', exc)

    def connection_lost(self, exc):
        print('closing transport', exc)
        self.loop = asyncio.get_event_loop()
        self.loop.stop()

    def error_received(self, exc):
        print('Error received:', exc)

    def __init__(self, loop):
        self.transport = None
        self.loop = loop


# note using port 57444 but could be any port, just remove port syntax and
# update get_client_ip() to remove [0] from getsockname()
def start_client(loop, addr, myip, udpclient):
    t = asyncio.Task(loop.create_datagram_endpoint(
        lambda: udpclient, remote_addr=addr))
    loop.run_until_complete(t)


def main(argv):
    global base_values

    # Some Good defaults
    remote_sff_port = 4789
    remote_sff_ip = "127.0.0.1"
    local_ip = "0.0.0.0"
    sfp_id = 0x000001
    sfp_index = 3

    try:
        logging.basicConfig(level=logging.INFO)
        opt, args = getopt.getopt(argv, "h", ["help", "remote-sff-ip=", "remote-sff-port=", "sfp-id=", "sfp-index="])
    except getopt.GetoptError:
        print("sff_client --help | --remote-sff-ip | --remote-sff-port | sfp-id | sfp-index")
        sys.exit(2)

    for opt, arg in opt:
        if opt == "sff-port":
            remote_sff_port = arg
            continue

        if opt in ('-h', '--help'):
            print("sff_client --remote-sff-ip=<IP address of remote SFF> --remote-sff-port=<UDP port of remote SFF> "
                  "sfp-id=<Service Function Path id> sfp-index<SFP starting index>")
            sys.exit()

        if opt == "--sff-ip":
            remote_sff_ip = arg
            continue

        if opt == "--sfp-id":
            sfp_id = arg
            continue

        if opt == "--sfp-index":
            sfp_index = arg
            continue

    base_values = BASEHEADER(0x1, int('01000000', 2), 0x6, 0x1, 0x1, int(sfp_id), int(sfp_index))

    loop = asyncio.get_event_loop()
    # if signal is not None:
    # loop.add_signal_handler(signal.SIGINT, loop.stop)

    # Figure out a source IP address / source UDP port for the client connection
    udpclient = MyUdpClient(loop)
    start_client(loop, (remote_sff_ip, remote_sff_port), local_ip, udpclient)

    loop.run_forever()


if __name__ == "__main__":
    main(sys.argv[1:])


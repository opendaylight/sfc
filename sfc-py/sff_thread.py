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

"""Service Function Forwarder (SFF). This SFF is spawned in a thread
   by sfc_agent.py. """

import asyncio
import logging
import socket
from nsh_decode import *
from sff_globals import *

udpserver_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

try:
    import signal
except ImportError:
    signal = None

logger = logging.getLogger(__name__)


# Global flags used for indication of current packet processing status.

PACKET_CHAIN = 0b00000000  # Packet needs more processing within this SFF
PACKET_CONSUMED = 0b00000001  # Packet was sent to another SFF or service function
PACKET_ERROR = 0b00000010  # Packet will be dropped
SERVICE_HOP_INVALID = 0xDEADBEEF  # Referenced service function is invalid

# Server side code: Store received values for VXLAN, base NSH and context headers data structures

server_vxlan_values = VXLANGPE()
server_ctx_values = CONTEXTHEADER()
server_base_values = BASEHEADER()


def lookup_next_sf(service_path, service_index):
    next_hop = SERVICE_HOP_INVALID
    # First we determine the list of SFs in the received packet based on SPI value extracted from packet

    try:
        next_hop = data_plane_path[service_path][service_index]
    except KeyError:
        logger.error("Could not determine next service hop. SP: %d, SI: %d", service_path, service_index)
    return next_hop


def set_service_index(rw_data, service_index):
    rw_data[15] = service_index


def process_incoming_packet(data, addr):

    address = ()  # empty tuple

    logger.info("Processing packet from: %s", addr)
    # Copy payload into bytearray so it can be changed
    rw_data = bytearray(data)
    # Decode the incoming packet for debug purposes and to strip out various header values
    decode_vxlan(data, server_vxlan_values)
    decode_baseheader(data, server_base_values)
    decode_contextheader(data, server_ctx_values)
    # Lookup what to do with the packet based on Service Path Identifier (SPI)
    if server_base_values.service_index:
        logger.info("Looking up next service hop...")
        next_hop = lookup_next_sf(server_base_values.service_path, server_base_values.service_index)
        if next_hop != SERVICE_HOP_INVALID:
            address = next_hop['ip'], next_hop['port']
        else:
            # bye, bye packet
            logger.info("End of path")
            logger.info("Packet dump: %s", binascii.hexlify(rw_data))
            logger.info('service index end up as: %d', server_base_values.service_index)
            rw_data = find_payload_start_index()
            # Remove all SFC headers, leave only original packet
    else:
        logger.error("Loop Detected")
        logger.error("Packet dump: %s", binascii.hexlify(rw_data))
        rw_data.__init__()
        data = ""

    logger.info("Finished processing packet from: %s", addr)
    return rw_data, address


class MyUdpServer:
    """
    This is the main UDP server. It receives VXLAN GPE packets, calls
    packet processing function and finally sends them on their way
    """

    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        logger.info('Received packet from: %s', addr)
        # Process the incoming packet
        rw_data, address = process_incoming_packet(data, addr)
        if address:
            # send the packet to the next SFF based on address
            logger.info("Sending packets to: %s", address)
            self.transport.sendto(rw_data, address)
            logger.info("listening for NSH packets ...")

    def connection_refused(self, exc):
        logger.error('Connection refused:', exc)

    def connection_lost(self, exc):
        logger.error('stop', exc)

    def error_received(self, exc):
        logger.error('Error received:', exc)

    def __init__(self, loop):
        self.transport = None
        self.loop = loop


class ControlUdpServer:
    """
    This control server class listen on a socket for commands from the main process.
    For example, if a SFF is deleted the main program can send a command to
    this data plane thread to exit.
    """

    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        logger.info('Control Server Received packet from: %s', addr)
        self.loop.call_soon_threadsafe(self.loop.stop)
        #data = data.decode('utf-8')
        #print(data_plane_path)
        #sfp_topo = json.loads(data)
        #print(sfp_topo)
        #print(sfp_topo['3']['3'])


    def connection_refused(self, exc):
        logger.error('Connection refused: %s', exc)

    def connection_lost(self, exc):
        logger.error('stop: %s', exc)

    def __init__(self, loop):
        self.transport = None
        self.loop = loop


def start_server(loop, addr, udpserver, message):
    #t = asyncio.Task(loop.create_datagram_endpoint(
    #    lambda: udpserver, local_addr=addr))
    listen = loop.create_datagram_endpoint(lambda: udpserver, local_addr=addr)
    transport, protocol = loop.run_until_complete(listen)
    logger.info("Starting Service Function Forwarder (SFF)")
    print(message, addr)
    return transport


# The python agent uses this function as the thread start whenever it wants
# to create a SFF
def start_sff(sff_name, sff_ip, sff_port, sff_control_port, sff_thread):
    logger.info("Starting SFF thread \n")
    global udpserver_socket
    logging.basicConfig(level=logging.INFO)

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    # if signal is not None:
    # loop.add_signal_handler(signal.SIGINT, loop.stop)

    udpserver = MyUdpServer(loop)
    udpserver_transport = start_server(loop, (sff_ip, sff_port), udpserver, "Listening for NSH packets on port: ")
    udpserver_socket = udpserver_transport.get_extra_info('socket')
    sff_thread[sff_name]['socket'] = udpserver_socket
    control_udp_server = ControlUdpServer(loop)
    start_server(loop, (sff_ip, sff_control_port), control_udp_server, "Listening for Control messages on port: ")

    loop.run_forever()
    udpserver_socket.close()
    loop.close()



#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import logging
import asyncio
import binascii
import ipaddress

import nsh.decode as nsh_decode
from common.sfc_globals import sfc_globals
from nsh.service_index import process_service_index
from nsh.encode import add_sf_to_trace_pkt
from nsh.common import *  # noqa
import socket


__author__ = "Jim Guichard, Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.3"
__email__ = "jguichar@cisco.com, rapenno@gmail.com"
__status__ = "beta"

"""
All supported services
"""

logger = logging.getLogger(__name__)

#: Global flags used for indication of current packet processing status
# Packet needs more processing within this SFF
PACKET_CHAIN = 0b00000000
# Packet was sent to another SFF or service function
PACKET_CONSUMED = 0b00000001
# Packet will be dropped
PACKET_ERROR = 0b00000010
# Referenced service function is invalid
SERVICE_HOP_INVALID = 0xDEADBEEF

#: Services names
FWL = 'firewall'
NAT = 'napt44'  # TODO: should this be `napt44` or just `nat`?
DPI = 'dpi'
QOS = 'qos'
IDS = 'ids'
SF = 'sf'
SFF = 'sff'
CUDP = 'cudp'


def find_service(service_type):
    """Service dispatcher - get service class based on its type

    :param service_type: service type
    :type service_type: str

    :return `:class:Baseservice`

    """
    if service_type == FWL:
        return MyFwService
    elif service_type == NAT:
        return MyNatService
    elif service_type == DPI:
        return MyDpiService
    elif service_type == SFF:
        return MySffServer
    elif service_type == CUDP:
        return ControlUdpServer
    elif service_type == QOS or service_type == IDS:
        # return a generic service for currently unimplemented services
        return MyService
    else:
        raise ValueError('Service "%s" not supported' % service_type)


class BasicService(object):
    def __init__(self, loop):
        """
        Service Blueprint Class
        TODO: add arguments description
        """
        self.loop = loop
        self.transport = None
        self.server_vxlan_values = VXLANGPE()
        self.server_base_values = BASEHEADER()
        self.server_ctx_values = CONTEXTHEADER()
        self.server_trace_values = TRACEREQHEADER()

        # MUST be set by EACH descendant class
        self.service_type = None
        self.service_name = None

    def set_name(self, name):
        self.service_name = name

    def _decode_headers(self, data):
        """
        Procedure for decoding packet headers.

        Decode the incoming packet for debug purposes and to strip out various
        header values.

        """
        # decode vxlan-gpe header
        nsh_decode.decode_vxlan(data, self.server_vxlan_values)
        # decode NSH base header
        nsh_decode.decode_baseheader(data, self.server_base_values)
        # decode NSH context headers
        nsh_decode.decode_contextheader(data, self.server_ctx_values)
        # decode common trace header
        if nsh_decode.is_trace_message(data):
            nsh_decode.decode_trace_req(data, self.server_trace_values)

    def _process_incoming_packet(self, data, addr):
        """
        TODO: add docstring
        :param self: MySFFServer
        :type  self: MySFFServer
        :param data: UDP payload
        :type data: bytes
        :param addr: IP address and port to which data are passed
        :type addr: tuple (str, int)
        """
        logger.info('%s: Processing received packet', self.service_type)

        self._decode_headers(data)

        # TODO: should't this be converted before applying _decode_headers()
        # like in MyUdpServer?
        rw_data = bytearray(data)
        rw_data = process_service_index(rw_data, self.server_base_values)[0]

        return rw_data

    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        """
        TODO: add docstring
        :param self: MySFFServer
        :type  self: MySFFServer
        :param data: UDP payload
        :type data: bytes
        :param addr: IP address and port to which data are passed
        :type addr: tuple (str, int)

        """
        logger.info('%s service received packet from %s:', self.service_type, addr)
        logger.debug('%s %s', addr, binascii.hexlify(data))
        rw_data = self._process_incoming_packet(data, addr)

        if nsh_decode.is_data_message(data):
            logger.info('%s: Sending packets to %s', self.service_type, addr)
            self.transport.sendto(rw_data, addr)
        elif nsh_decode.is_trace_message(data):
            # Add SF information to packet
            if self.server_base_values.service_index == self.server_trace_values.sil:
                trace_pkt = add_sf_to_trace_pkt(rw_data, self.service_type, self.service_name)
                self.transport.sendto(trace_pkt, addr)
            else:
                self.transport.sendto(rw_data, addr)
                # Send packet back to SFF

    def process_trace_pkt(self, rw_data, data):
        logger.info('%s: Sending trace report packet', self.service_type)
        ipv6_addr = ipaddress.IPv6Address(data[
                                          NSH_OAM_TRACE_DEST_IP_REPORT_OFFSET:NSH_OAM_TRACE_DEST_IP_REPORT_OFFSET + NSH_OAM_TRACE_DEST_IP_REPORT_LEN])  # noqa
        if ipv6_addr.ipv4_mapped:
            ipv4_str_trace_dest_addr = str(ipaddress.IPv4Address(self.server_trace_values.ip_4))
            trace_dest_addr = (ipv4_str_trace_dest_addr, self.server_trace_values.port)
            logger.info("IPv4 destination:port address for trace reply: %s", trace_dest_addr)
            self.transport.sendto(rw_data, trace_dest_addr)
        else:
            ipv6_str_trace_dest_addr = str(ipaddress.IPv6Address(ipv6_addr))
            trace_dest_addr = (ipv6_str_trace_dest_addr, self.server_trace_values.port)
            logger.info("IPv6 destination address for trace reply: %s", trace_dest_addr)
            self.transport.sendto(rw_data, trace_dest_addr)

    @staticmethod
    def connection_refused(exc):
        logger.error('Connection refused: %s', exc)

    def connection_lost(self, exc):
        logger.warning('Closing transport', exc)
        loop = asyncio.get_event_loop()
        loop.stop()


class MyService(BasicService):
    def __init__(self, loop):
        super(MyService, self).__init__(loop)

        self.service_type = 'generic'


class MyFwService(BasicService):
    def __init__(self, loop):
        super(MyFwService, self).__init__(loop)

        self.service_type = FWL


class MyNatService(BasicService):
    def __init__(self, loop):
        super(MyNatService, self).__init__(loop)

        self.service_type = NAT


class MyDpiService(BasicService):
    def __init__(self, loop):
        super(MyDpiService, self).__init__(loop)

        self.service_type = DPI


class ControlUdpServer(BasicService):
    def __init__(self, loop):
        """
        This control server class listen on a socket for commands from the main
        process. For example, if a SFF is deleted the main program can send a
        command to this data plane thread to exit.
        """
        super(ControlUdpServer, self).__init__(loop)

        self.service_type = 'Control UDP Server'

    def datagram_received(self, data, addr):
        logger.info('%s received a packet from: %s', self.service_type, addr)
        self.loop.call_soon_threadsafe(self.loop.stop)
        # data = data.decode('utf-8')
        # print(data_plane_path)
        # sfp_topo = json.loads(data)
        # print(sfp_topo)
        # print(sfp_topo['3']['3'])

    def connection_lost(self, exc):
        logger.error('stop: %s', exc)


class MySffServer(BasicService):
    def __init__(self, loop):
        """
        This is the main SFF server. It receives VXLAN GPE packets, calls
        packet processing function and finally sends them on their way
        """
        super(MySffServer, self).__init__(loop)

        self.service_type = 'SFF Server'

    @staticmethod
    def _lookup_next_sf(service_path, service_index):
        """
        TODO:
        """
        next_hop = SERVICE_HOP_INVALID

        # First we determine the list of SFs in the received packet based on
        # SPI value extracted from packet
        try:
            local_data_plane_path = sfc_globals.get_data_plane_path()
            next_hop = local_data_plane_path[service_path][service_index]
        except KeyError:
            msg = ('Could not determine next service hop. SP: %d, SI: %d' %
                   (service_path, service_index))

            logger.error(msg)

        return next_hop

    def _process_incoming_packet(self, data, addr):
        """
        TODO: SFF main processing packet function
        :param self: MySFFServer
        :type  self: MySFFServer
        :param data: UDP payload
        :type data: bytes
        :param addr: IP address and port to which data are passed
        :type addr: tuple (str, int)
        """
        logger.info("%s: Processing packet from: %s", self.service_type, addr)

        address = ()

        # Copy payload into bytearray so it can be changed
        rw_data = bytearray(data)
        self._decode_headers(data)

        # Lookup what to do with the packet based on Service Path Identifier
        # (SPI)

        next_hop = self._lookup_next_sf(self.server_base_values.service_path,
                                        self.server_base_values.service_index)

        if nsh_decode.is_data_message(data):

            if next_hop != SERVICE_HOP_INVALID:
                address = next_hop['ip'], next_hop['port']
                logger.info("%s: Sending packets to: %s", self.service_type, address)
                # send the packet to the next SFF based on address
                self.transport.sendto(rw_data, address)
                logger.info("%s: Listening for NSH packets ...", self.service_type)
            elif self.server_base_values.service_index:
                # bye, bye packet
                logger.info("%s: End of path", self.service_type)
                logger.debug("%s: Packet dump: %s", self.service_type, binascii.hexlify(rw_data))
                logger.debug('%s: service index end up as: %d', self.service_type,
                             self.server_base_values.service_index)

                # Remove all SFC headers, leave only original packet
                inner_packet = rw_data[PAYLOAD_START_INDEX:]
                sock_raw = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_IP)
                host = socket.gethostbyname(socket.gethostname())
                sock_raw.bind((host, 0))
                sock_raw.setsockopt(socket.IPPROTO_IP, socket.IP_HDRINCL, 1)
                sock_raw.sendto(inner_packet)

            else:
                # SI = 0, loop detected
                logger.error("%s: Loop Detected", self.service_type)
                logger.error("%s: Packet dump: %s", self.service_type, binascii.hexlify(rw_data))

                rw_data.__init__()
                data = ""

        elif nsh_decode.is_trace_message(data):

            # Have to differentiate between no SPID and End of path
            if (self.server_trace_values.sil == self.server_base_values.service_index) or (
                        next_hop == SERVICE_HOP_INVALID):
                # End of trace
                super(MySffServer, self).process_trace_pkt(rw_data, data)

            else:
                # Trace will continue
                address = next_hop['ip'], next_hop['port']
                logger.info("%s: Sending trace packet to: %s", self.service_type, address)
                # send the packet to the next SFF based on address
                self.transport.sendto(rw_data, address)
                logger.info("%s: Listening for NSH packets ...", self.service_type)

        logger.info('%s: Finished processing packet from: %s', self.service_type, addr)
        return rw_data, address

    def datagram_received(self, data, addr):
        """
        TODO:
        :param self: MySFFServer
        :type  self: MySFFServer
        :param data: UDP payload
        :type data: bytes
        :param addr: IP address and port to which data are passed
        :type addr: tuple (str, int)
        """
        logger.info('%s: Received a packet from: %s', self.service_type, addr)

        rw_data, address = self._process_incoming_packet(data, addr)

    def connection_lost(self, exc):
        logger.error('stop', exc)

    @staticmethod
    def error_received(exc):
        logger.error('Error received:', exc)

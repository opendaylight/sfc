__author__ = "Jim Guichard, Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "jguichar@cisco.com, rapenno@gmail.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

"""
All supported services
"""


import logging
import asyncio
import binascii

import nsh.decode as nsh_decode
from common.sfc_globals import sfc_globals
from nsh.service_index import process_service_index

logger = logging.getLogger(__name__)

#: Decode vxlan-gpe, base NSH header and NSH context headers
server_vxlan_values = nsh_decode.VXLANGPE()
server_ctx_values = nsh_decode.CONTEXTHEADER()
server_base_values = nsh_decode.BASEHEADER()

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
NAT = 'nat'  # TODO: should this be `napt44` or just `nat`?
DPI = 'dpi'
QOS = 'qos'
IDS = 'ids'
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

        # MUST be set by EACH descendant class
        self.service_type = None

    def _decode_headers(self, data):
        """
        Procedure for decoding packet headers.

        Decode the incoming packet for debug purposes and to strip out various
        header values.

        """
        # decode vxlan-gpe header
        nsh_decode.decode_vxlan(data, server_vxlan_values)
        # decode NSH base header
        nsh_decode.decode_baseheader(data, server_base_values)
        # decode NSH context headers
        nsh_decode.decode_contextheader(data, server_ctx_values)

    def _process_incoming_packet(self, data):
        """
        TODO: add docstring, params descirptions
        """
        logger.info('Processing received packet')

        self._decode_headers(data)

        # TODO: should't this be converted before applying _decode_headers()
        # like in MyUdpServer?
        rw_data = bytearray(data)
        rw_data = process_service_index(rw_data, server_base_values)[0]

        return rw_data

    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        """
        TODO: add docstring

        :param data:
        :type data: bytes
        :param addr: IP address and port to which data are passed
        :type addr: tuple (str, int)

        """
        logger.info('%s service received packet from SFF:', self.service_type)
        logger.info(addr, binascii.hexlify(data))
        logger.info('Sending packets to %s', addr)

        rw_data = self._process_incoming_packet(data)
        self.transport.sendto(rw_data, addr)

    def connection_refused(self, exc):
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
        #data = data.decode('utf-8')
        #print(data_plane_path)
        #sfp_topo = json.loads(data)
        #print(sfp_topo)
        #print(sfp_topo['3']['3'])

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

    def _lookup_next_sf(self, service_path, service_index):
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
        TODO:
        """
        logger.info("Processing packet from: %s", addr)

        address = ()

        # Copy payload into bytearray so it can be changed
        rw_data = bytearray(data)
        self._decode_headers(data)

        # Lookup what to do with the packet based on Service Path Identifier
        # (SPI)

        if server_base_values.service_index:
            logger.info('Looking up next service hop ...')

            next_hop = self._lookup_next_sf(server_base_values.service_path,
                                            server_base_values.service_index)
            if next_hop != SERVICE_HOP_INVALID:
                address = next_hop['ip'], next_hop['port']
            else:
                # bye, bye packet
                logger.info("End of path")
                logger.info("Packet dump: %s", binascii.hexlify(rw_data))
                logger.info('service index end up as: %d',
                            server_base_values.service_index)

                # Remove all SFC headers, leave only original packet
                rw_data = nsh_decode.PAYLOAD_START_INDEX
        else:
            logger.error("Loop Detected")
            logger.error("Packet dump: %s", binascii.hexlify(rw_data))

            rw_data.__init__()
            data = ""

        logger.info('Finished processing packet from: %s', addr)
        return rw_data, address

    def datagram_received(self, data, addr):
        """
        TODO:
        """
        logger.info('Received a packet from: %s', addr)

        rw_data, address = self._process_incoming_packet(data, addr)
        if address:
            logger.info("Sending packets to: %s", address)
            # send the packet to the next SFF based on address
            self.transport.sendto(rw_data, address)
            logger.info("listening for NSH packets ...")

    def connection_lost(self, exc):
        logger.error('stop', exc)

    def error_received(self, exc):
        logger.error('Error received:', exc)

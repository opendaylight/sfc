#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import sys
import socket
import logging
import asyncio
import binascii
import ipaddress
import platform
import queue

from threading import Thread

from struct import pack, unpack

from ..common.sfc_globals import sfc_globals
from ..nsh.common import *  # noqa
from ..nsh import decode as nsh_decode
from ..nsh.encode import add_sf_to_trace_pkt
from ..nsh.service_index import process_service_index

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
NAT = 'napt44'
DPI = 'dpi'
QOS = 'qos'
IDS = 'ids'
SF = 'sf'
SFF = 'sff'
CUDP = 'cudp'

# For VxLAN-gpe
GPE_NP_NSH = 0x4
ETH_P_NSH_0 = 0x89
ETH_P_NSH_1 = 0x4f

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

        :param loop:
        :type loop: `:class:asyncio.unix_events._UnixSelectorEventLoop`

        """
        self.loop = loop
        self.transport = None
        self.server_vxlan_values = VXLANGPE()
        self.server_eth_before_nsh_values = ETHHEADER()
        self.is_eth_nsh = False
        self.server_base_values = BASEHEADER()
        self.server_ctx_values = CONTEXTHEADER()
        self.server_eth_values = ETHHEADER()
        self.server_trace_values = TRACEREQHEADER()

        # MUST be set by EACH descendant class
        self.service_type = None
        self.service_name = None

        self.packet_queue = queue.Queue()

        self.sending_thread = Thread(target=self.read_queue)
        self.sending_thread.daemon = True
        self.sending_thread.start()

    def set_name(self, name):
        self.service_name = name

    def get_name(self):
        """
        :return service name which is the same as SF/SFF name
        :rtype: str
        """
        return self.service_name

    def _decode_headers(self, data):
        """
        Procedure for decoding packet headers.

        Decode the incoming packet for debug purposes and to strip out various
        header values.

        """
        offset = 0
        # decode vxlan-gpe header
        nsh_decode.decode_vxlan(data, offset, self.server_vxlan_values)
        offset += 8
        #decode ETH header before NSH if exists
        if self.server_vxlan_values.next_protocol == GPE_NP_NSH:
            nsh_decode.decode_ethheader(data, offset, self.server_eth_before_nsh_values)
            if ((self.server_eth_before_nsh_values.ethertype0 == ETH_P_NSH_0) and (self.server_eth_before_nsh_values.ethertype1 == ETH_P_NSH_1)):
                self.is_eth_nsh = True
                offset += 14
            else:
                self.is_eth_nsh = False
        # decode NSH base header
        nsh_decode.decode_baseheader(data, offset, self.server_base_values)
        offset += 8
        # decode NSH context headers
        nsh_decode.decode_contextheader(data, offset, self.server_ctx_values)
        offset += 16
        # decode NSH eth headers
        nsh_decode.decode_ethheader(data, offset, self.server_eth_values)

        # decode common trace header
        if self.is_eth_nsh:
            offset = 8 + 14
        else:
            offset = 8 
        if nsh_decode.is_trace_message(data, offset):
            offset += 24
            nsh_decode.decode_trace_req(data, offset, self.server_trace_values)

    def _process_incoming_packet(self, data, addr):
        """
        Decode NSH headers and process service index

        :param data: packet payload
        :type data: bytes
        :param addr: IP address and port to which data are passed
        :type addr: tuple

        """
        logger.debug('%s: Processing received packet(basicservice) service name :%s',
                     self.service_type, self.service_name)

        self._decode_headers(data)
        if self.is_eth_nsh:
            offset = 8 + 14
        else:
            offset = 8
        rw_data = bytearray(data)
        rw_data, _ = process_service_index(rw_data, offset, self.server_base_values)
        sfc_globals.sf_processed_packets += 1

        return rw_data

    def _update_metadata(self, data,
                         network_platform=None, network_shared=None,
                         service_platform=None, service_shared=None):
        """
        Update NSH context header in received packet data

        :param data: packet data
        :type data: bytes
        :param network_platform: new network_platform value
        :type network_platform: int
        :param network_shared: new network_shared value
        :type network_shared: int
        :param service_platform: new service_platform value
        :type service_platform: int
        :param service_shared: new service_shared value
        :type service_shared: int

        :return bytearray

        """
        if network_platform is not None:
            self.server_ctx_values.network_platform = network_platform

        if network_shared is not None:
            self.server_ctx_values.network_shared = network_shared

        if service_platform is not None:
            self.server_ctx_values.service_platform = service_platform

        if service_shared is not None:
            self.server_ctx_values.service_shared = service_shared

        new_ctx_header = pack('!I I I I',
                              self.server_ctx_values.network_platform,
                              self.server_ctx_values.network_shared,
                              self.server_ctx_values.service_platform,
                              self.server_ctx_values.service_shared)

        data = bytearray(data)
        data[16:32] = new_ctx_header

        return data

    def connection_made(self, transport):
        self.transport = transport

    def datagram_received(self, data, addr):
        """
        Put received packet into the internal queue

        :param data: packet data
        :type data: bytes
        :param addr: IP address and port to which data are passed
        :type addr: tuple

        """
        logger.info('%s service received packet from %s:', self.service_type, addr)
        logger.debug('%s %s', addr, binascii.hexlify(data))
        packet = (data, addr)
        try:
            self.packet_queue.put_nowait(packet)
        except:
            msg = 'Putting into queue failed'
            # logger.info(msg)
            logger.exception(msg)

        if self.service_type == DPI:
            sfc_globals.sf_queued_packets += 1
        else:
            sfc_globals.sff_queued_packets += 1

    def process_datagram(self, data, addr):
        """
        Forward received packet accordingly based on its type

        :param data: packet data
        :type data: bytes
        :param addr: IP address and port to which data are passed
        :type addr: tuple

        """
        logger.info('%s service received packet from %s:', self.service_type, addr)
        logger.debug('%s %s', addr, binascii.hexlify(data))
        rw_data = self._process_incoming_packet(data, addr)
        if self.is_eth_nsh:
            offset = 8 + 14
        else:
            offset = 8
        if nsh_decode.is_data_message(data, offset):
            # Must send it to UDP port of VxLAN-gpe
            #if nsh_decode.is_vxlan_nsh_legacy_message(data, 0):
                # Disregard source port of received packet and send packet back to 6633
            addr_l = list(addr)
            addr_l[1] = 6633
            addr = tuple(addr_l)
            self.transport.sendto(rw_data, addr)
            logger.info('%s: sending packets to %s', self.service_type, addr)
        elif nsh_decode.is_trace_message(data, offset):
            # Add SF information to packet
            if self.server_base_values.service_index == self.server_trace_values.sil:
                trace_pkt = add_sf_to_trace_pkt(rw_data, self.service_type, self.service_name)
                self.transport.sendto(trace_pkt, addr)
            # Send packet back to SFF
            else:
                self.transport.sendto(rw_data, addr)

    def read_queue(self):
        """
        Read received packet from the internal queue

        """
        try:
            while True:
                packet = self.packet_queue.get(block=True)
                self.process_datagram(data=packet[0], addr=packet[1])
                self.packet_queue.task_done()
        except:
            msg = 'Reading from queue failed'
            logger.info(msg)
            logger.exception(msg)
            raise

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
        # super(ControlUdpServer, self).__init__(loop)
        self.loop = loop
        self.transport = None
        self.service_name = None
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

    def _lookup_next_sf(self, service_path, service_index):
        """
        Retrieve next SF locator info from SfcGlobals

        :param service_path: service path identifier
        :type service_path: int
        :param service_index: service index
        :type service_index: int

        :return dict or hex
        :rtype: tuple

        """
        next_hop = SERVICE_HOP_INVALID

        # First we determine the list of SFs in the received packet based on
        # SPI value extracted from packet
        try:
            local_data_plane_path = sfc_globals.get_data_plane_path()
            sff_name = super(MySffServer, self).get_name()
            next_hop = local_data_plane_path[sff_name][service_path][service_index]
        except KeyError:
            logger.error('Could not determine next service hop. SP: %d, SI: %d',
                         service_path, service_index)
            pass
        return next_hop

    @staticmethod
    def _get_packet_bearing(packet):
        """
        Parse a packet to get source and destination info

        CREDITS: http://www.binarytides.com/python-packet-sniffer-code-linux/

        :param packet: received packet (IP header and upper layers)
        :type packet: bytes

        :return dict or None

        """
        ip_header = packet[:20]
        iph = unpack('!BBHHHBBH4s4s', ip_header)

        protocol = iph[6]
        s_addr = socket.inet_ntoa(iph[8])
        d_addr = socket.inet_ntoa(iph[9])

        if protocol == 6:
            tcp_header = packet[20:40]
            protocolh = unpack('!HHLLBBHHH', tcp_header)

        elif protocol == 17:
            udp_header = packet[20:28]
            protocolh = unpack('!HHHH', udp_header)

        else:
            logger.error('Only TCP and UDP protocls are supported')
            return

        s_port = protocolh[0]
        d_port = protocolh[1]

        return {'s_addr': s_addr,
                's_port': s_port,
                'd_addr': d_addr,
                'd_port': d_port}

    def _process_incoming_packet(self, data, addr):
        """
        SFF main packet processing function

        :param data: UDP payload
        :type data: bytes
        :param addr: IP address and port to which data are passed
        :type addr: tuple (str, int)

        """
        # logger.debug("%s: mysff Processing packet from: %s", self.service_type, addr)
        address = ()
        rw_data = bytearray(data)
        self._decode_headers(data)

        sfc_globals.sff_processed_packets += 1
        # logger.info('*******(mysff server) received packets "%d"', sfc_globals.received_packets)

        # Lookup what to do with the packet based on Service Path Identifier
        next_hop = self._lookup_next_sf(self.server_base_values.service_path,
                                        self.server_base_values.service_index)
        if self.is_eth_nsh:
            offset = 8 + 14
        else:
            offset = 8
        if nsh_decode.is_data_message(data, offset):
            # send the packet to the next SFF based on address
            if next_hop != SERVICE_HOP_INVALID:
                address = next_hop['ip'], next_hop['port']
                # logger.info("%s: Sending packets to: %s", self.service_type, address)

                self.transport.sendto(rw_data, address)

            # send packet to its original destination
            elif self.server_base_values.service_index:
                # logger.info("%s: End of path", self.service_type)
                # logger.debug("%s: Packet dump: %s", self.service_type, binascii.hexlify(rw_data))
                # logger.debug('%s: service index end up as: %d', self.service_type,
                #             self.server_base_values.service_index)

                # Remove all SFC headers, leave only original packet
                if self.server_base_values.next_protocol == NSH_NEXT_PROTO_IPV4:
                    payload_start_index = PAYLOAD_START_INDEX_NSH_TYPE1
                elif self.server_base_values.next_protocol == NSH_NEXT_PROTO_ETH:
                    payload_start_index = PAYLOAD_START_INDEX_NSH_TYPE3
                else:
                    logger.error("\nCan not determine NSH next protocol\n")
                    return rw_data, address
                inner_packet = rw_data[payload_start_index:]
                if inner_packet:
                    # euid = os.geteuid()
                    # if euid != 0:
                    #     print("Script not started as root. Running sudo...")
                    #     args = ['sudo', sys.executable] + sys.argv + [os.environ]
                    #     # the next line replaces the currently-running process with the sudo
                    #     os.execlpe('sudo', *args)

                    # Reinaldo note:
                    # Unfortunately it has to be this way. Python has poor raw socket support in
                    # MacOS.  What happens is that MacoS will _always_ include the IP header unless you use
                    # socket option IP_HDRINCL
                    # https://developer.apple.com/library/mac/documentation/Darwin/Reference/ManPages/man4/ip.4.html
                    #
                    # But if you try to set this option at the Python level (instead of C level) it does not
                    # work. the only way around is to create a raw socket of type UDP and leave the IP header
                    # out when sending/building the packet.
                    sock_raw = None
                    bearing = self._get_packet_bearing(inner_packet)

                    try:
                        if platform.system() == "Darwin":
                            # Assuming IPv4 packet for now. Move pointer forward
                            inner_packet = rw_data[payload_start_index + IPV4_HEADER_LEN_BYTES:]
                            sock_raw = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_UDP)
                        else:
                            sock_raw = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
                    except socket.error as msg:
                        logger.error("Socket could not be created. Error Code : %s", msg)
                        sys.exit()

                    logger.info("End of Chain. Sending packet to %s %s", bearing['d_addr'], bearing['d_port'])
                    sock_raw.sendto(inner_packet, (bearing['d_addr'],
                                                   bearing['d_port']))

            # end processing as Service Index reaches zero (SI = 0)
            else:
                logger.error("%s: Loop Detected", self.service_type)
                logger.debug("%s: Packet dump: %s", self.service_type, binascii.hexlify(rw_data))

                rw_data.__init__()
                data = ""

        elif nsh_decode.is_trace_message(data, offset):
            # Have to differentiate between no SPID and End of path
            service_index = self.server_base_values.service_index
            if (self.server_trace_values.sil == service_index) or (next_hop == SERVICE_HOP_INVALID):
                # End of trace
                super(MySffServer, self).process_trace_pkt(rw_data, data)

            else:
                # Trace will continue
                address = next_hop['ip'], next_hop['port']
                # logger.info("%s: Sending trace packet to: %s", self.service_type, address)
                # send the packet to the next SFF based on address
                self.transport.sendto(rw_data, address)
                # logger.info("%s: Listening for NSH packets ...", self.service_type)

        # logger.info('%s: Finished processing packet from: %s', self.service_type, addr)
        return rw_data, address

    def process_datagram(self, data, addr):
        """
        Process received packet

        :param data: packet data
        :type data: bytes
        :param addr: IP address and port to which data are passed
        :type addr: tuple

        """
        # logger.info('%s: Received a packet from: %s', self.service_type, addr)

        self._process_incoming_packet(data, addr)

    def connection_lost(self, exc):
        logger.error('stop', exc)

    @staticmethod
    def error_received(exc):
        logger.error('Error received:', exc)

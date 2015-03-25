#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import getopt
import logging
import sys
import asyncio
from nsh.decode import *  # noqa
from nsh.encode import *  # noqa


__author__ = "Reinaldo Penno, Jim Guichard, Paul Quinn"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.3"
__email__ = "repenno@cisco.com, jguichar@cisco.com, paulq@cisco.com"
__status__ = "alpha"

"""Service Function Forwarder (SFF) Client. This module
   will initiate the path by building a NSH/VXLAN-GPE packet and sending to the first SFF
   in the path"""

logger = logging.getLogger(__name__)

try:
    import signal
except ImportError:
    signal = None


# Global flags used for indication of current packet processing status.

PACKET_CHAIN = 0b00000000  # Packet needs more processing within this SFF
PACKET_CONSUMED = 0b00000001  # Packet was sent to another SFF or service function
PACKET_ERROR = 0b00000010  # Packet will be dropped
SERVICEFUNCTION_INVALID = 0xDEADBEEF  # Referenced service function is invalid
RSP_STARTING_INDEX = 255


# Client side code: Build NSH packet encapsulated in VXLAN & NSH.

class MyUdpClient:
    def __init__(self, loop, encapsulate_type, encapsulate_header_values, base_header_values, ctx_header_values,
                 dest_addr, dest_port):
        self.transport = None
        self.loop = loop
        self.encapsulate_header_values = encapsulate_header_values
        self.base_header_values = base_header_values
        self.ctx_header_values = ctx_header_values
        self.server_vxlan_values = VXLANGPE()
        self.server_base_values = BASEHEADER()
        self.server_ctx_values = CONTEXTHEADER()
        self.dest_addr = dest_addr
        self.dest_port = dest_port
        self.encapsulate_type = encapsulate_type

    def connection_made(self, transport):
        self.transport = transport
        # Building client packet to send to SFF
        # packet = build_packet(self.vxlan_header_values, self.base_header_values, self.ctx_header_values)
        packet = build_packet(self.encapsulate_type, self.encapsulate_header_values, self.base_header_values,
                              self.ctx_header_values)
        # logger.info("Sending VXLAN-GPE/NSH packet to SFF: %s", (self.dest_addr, self.dest_port))
        logger.info("Sending %s packet to SFF: %s", self.encapsulate_type, (self.dest_addr, self.dest_port))
        logger.debug("Packet dump: %s", binascii.hexlify(packet))
        # Send the packet
        self.transport.sendto(packet, (self.dest_addr, self.dest_port))

    def datagram_received(self, data, addr):
        logger.info("Received packet from SFF: %s", addr)
        logger.debug("Packet dump: %s", binascii.hexlify(data))
        # Decode all the headers
        decode_vxlan(data, self.server_vxlan_values)
        decode_baseheader(data, self.server_base_values)
        decode_contextheader(data, self.server_ctx_values)
        self.loop.stop()

    @staticmethod
    def connection_refused(exc):
        logger.error('Connection refused:', exc)

    def connection_lost(self, exc):
        logger.error('closing transport', exc)
        self.loop = asyncio.get_event_loop()
        self.loop.stop()

    @staticmethod
    def error_received(exc):
        logger.error('Error received:', exc)


# Client side code: Build NSH packet encapsulated in GRE & NSH.
class MyGreClient:
    def __init__(self, loop, encapsulate_type, encapsulate_header_values, base_header_values, ctx_header_values,
                 dest_addr, dest_port):
        self.transport = None
        self.loop = loop
        # self.vxlan_header_values = vxlan_header_values
        self.encapsulate_header_values = encapsulate_header_values
        self.base_header_values = base_header_values
        self.ctx_header_values = ctx_header_values
        self.server_vxlan_values = VXLANGPE()
        self.server_base_values = BASEHEADER()
        self.server_ctx_values = CONTEXTHEADER()
        self.dest_addr = dest_addr
        self.dest_port = dest_port
        self.encapsulate_type = encapsulate_type

    def send_gre_nsh(self):
        # create a raw socket
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)

        except socket.error as msg:
            print('Socket could not be created. Error Code : ' + str(msg[0]) + ' Message ' + msg[1])
            sys.exit()

        # ip header fields
        ip_ihl = 5
        ip_ver = 4
        ip_tos = 0
        ip_tot_len = 0  # kernel will fill the correct total length
        ip_id = 54321  # Id of this packet
        ip_frag_off = 0
        ip_ttl = 255
        ip_proto = socket.IPPROTO_TCP
        ip_check = 0  # kernel will fill the correct checksum
        ip_saddr = socket.inet_aton(
            socket.gethostbyname(socket.gethostname()))  # Spoof the source ip address if you want to
        ip_daddr = socket.inet_aton(self.dest_addr)

        ip_ihl_ver = (ip_ver << 4) + ip_ihl

        # the ! in the pack format string means network order
        ip_header = struct.pack('!BBHHHBBH4s4s', ip_ihl_ver, ip_tos, ip_tot_len, ip_id, ip_frag_off, ip_ttl, ip_proto,
                                ip_check, ip_saddr, ip_daddr)

        # Building client packet to send to SFF
        gre_nsh_packet = build_packet(self.encapsulate_header_values, self.encapsulate_type, self.base_header_values,
                                      self.ctx_header_values)

        packet = ip_header + gre_nsh_packet

        # logger.info("Sending VXLAN-GPE/NSH packet to SFF: %s", (self.dest_addr, self.dest_port))
        logger.info("Sending %s packet to SFF: %s", self.encapsulate_type, (self.dest_addr, self.dest_port))
        logger.debug("Packet dump: %s", binascii.hexlify(packet))
        # logger.info("Packet dump: %s", binascii.hexlify(packet))

        s.sendto(packet, (self.dest_addr, self.dest_port ))

    def connection_made(self, transport):
        self.transport = transport
        # Building client packet to send to SFF
        # packet = build_packet(self.vxlan_header_values, self.base_header_values, self.ctx_header_values)
        packet = build_packet(self.encapsulate_header_values, self.encapsulate_type, self.base_header_values,
                              self.ctx_header_values)

        # logger.info("Sending VXLAN-GPE/NSH packet to SFF: %s", (self.dest_addr, self.dest_port))
        logger.info("Sending %s packet to SFF: %s", self.encapsulate_type, (self.dest_addr, self.dest_port))
        logger.debug("Packet dump: %s", binascii.hexlify(packet))
        logger.info("Packet dump: %s", binascii.hexlify(packet))

        # Send the packet
        self.transport.sendto(packet, (self.dest_addr, self.dest_port))


class MyTraceClient:
    def __init__(self, loop, vxlan_header_values, base_header_values, ctx_header_values, trace_req_header_values,
                 dest_addr, dest_port, num_trace_hops):
        self.transport = None
        self.loop = loop
        self.vxlan_header_values = vxlan_header_values
        self.base_header_values = base_header_values
        self.ctx_header_values = ctx_header_values
        self.trace_req_header_values = trace_req_header_values
        self.server_trace_values = TRACEREQHEADER()
        self.server_ctx_values = CONTEXTHEADER()
        self.server_vxlan_values = VXLANGPE()
        self.server_base_values = BASEHEADER()
        self.dest_addr = dest_addr
        self.dest_port = dest_port
        self.hop_count = 0
        self.num_trace_hops = num_trace_hops

    def connection_made(self, transport):
        self.transport = transport
        packet = build_trace_req_packet(self.vxlan_header_values, self.base_header_values, self.ctx_header_values,
                                        self.trace_req_header_values)
        # udp_socket = self.transport.get_extra_info('socket')
        print("Sending Trace packet to Service Path and Service Index: ({0}, {1})".format(
            self.base_header_values.service_path, self.base_header_values.service_index))
        logger.debug("Trace packet: %s", binascii.hexlify(packet))
        print("Trace response...")
        self.transport.sendto(packet, (self.dest_addr, self.dest_port))

    def datagram_received(self, data, addr):
        logger.debug("Received trace response pkt from: %s", addr)
        # Decode all the headers
        decode_vxlan(data, self.server_vxlan_values)
        decode_baseheader(data, self.server_base_values)
        decode_contextheader(data, self.server_ctx_values)
        service_type, service_name = decode_trace_resp(data, self.server_trace_values)
        print(
            "Service-hop: {0}. Service Type: {1}, Service Name: {2}, Address of Reporting SFF: {3}".format(
                self.hop_count, service_type, service_name, addr))
        self.hop_count += 1

        if self.num_trace_hops > self.hop_count:
            self.trace_req_header_values.sil -= 1
            self.send_packet((self.dest_addr, self.dest_port))
        else:
            print("Trace end \n")
            self.loop.stop()

    @staticmethod
    def connection_refused(exc):
        logger.error('Connection refused: %s', exc)

    def connection_lost(self, exc):
        logger.error('closing transport: %s', exc)
        self.loop = asyncio.get_event_loop()
        self.loop.stop()

    @staticmethod
    def error_received(exc):
        logger.error('Error received: %s', exc)

    def send_packet(self, dest_addr):
        packet = build_trace_req_packet(self.vxlan_header_values, self.base_header_values, self.ctx_header_values,
                                        self.trace_req_header_values)
        # logger.info("Sending Trace packet to: %s", dest_addr)
        self.transport.sendto(packet, dest_addr)

    def set_transport(self, transport):
        self.transport = transport


def start_client(loop, myaddr, dest_addr, udpclient):
    listen = loop.create_datagram_endpoint(
        lambda: udpclient, local_addr=myaddr)

    loop.run_until_complete(listen)
    # transport = loop.run_until_complete(listen)[0]
    # udpclient.set_transport(transport)
    # udpclient.send_packet(dest_addr)
    loop.run_forever()
    loop.close()


def main(argv):
    """
    Example:
    python3.4 sff_client.py --remote-sff-ip 10.0.1.41 --remote-sff-port 4789 --sfp-id 1 --sfp-index 255
    python3.4 sff_client.py --remote-sff-ip 10.0.1.4 --remote-sff-port 4789 --sfp-id 1 --sfp-index 255

    python3.4 sff_client.py --remote-sff-ip 10.0.1.4 --remote-sff-port 4789 --sfp-id 1 --sfp-index 255 --encapsulate gre
    python3.4 sff_client.py --remote-sff-ip 10.0.1.4 --remote-sff-port 4789 --sfp-id 1 --sfp-index 255 --encapsulate vxlan

    Trace Example:
    python3.4 sff_client.py --remote-sff-ip 10.0.1.41 --remote-sff-port 4789 --sfp-id 1 --sfp-index 255 \
                            --trace-req --num-trace-hops 1
    :param argv:
    :return:
    """
    global base_values

    # Some Good defaults
    remote_sff_port = 4789
    remote_sff_ip = "127.0.0.1"
    local_ip = "0.0.0.0"
    sfp_id = 0x000001
    sfp_index = 3
    trace_req = False
    num_trace_hops = 254
    encapsulate = 'vxlan-gpe'  # default to VXLAN-GPE

    try:
        logging.basicConfig(level=logging.INFO)
        opt, args = getopt.getopt(argv, "h",
                                  ["help", "remote-sff-ip=", "remote-sff-port=", "sfp-id=", "sfp-index=",
                                   "trace-req", "num-trace-hops=", "encapsulate="])
    except getopt.GetoptError:
        print(
            "sff_client --help | --remote-sff-ip | --remote-sff-port | --sfp-id | --sfp-index | "
            "--trace-req | --num-trace-hops | --encapsulate")
        sys.exit(2)

    for opt, arg in opt:
        if opt == "--remote-sff-port":
            remote_sff_port = arg
            continue

        if opt in ('-h', '--help'):
            print("sff_client --remote-sff-ip=<IP address of remote SFF> --remote-sff-port=<UDP port of remote SFF> "
                  "--sfp-id=<Service Function Path id> --sfp-index<SFP starting index> --num-trace-hops<number trace "
                  "hops> --encapsulate<transport encapsulation vxlan-gpe|gre")
            sys.exit()

        if opt == "--remote-sff-ip":
            remote_sff_ip = arg
            continue

        if opt == "--sfp-id":
            sfp_id = arg
            continue

        if opt == "--sfp-index":
            sfp_index = arg
            continue

        if opt == "--trace-req":
            trace_req = True
            continue

        if opt == "--num-trace-hops":
            num_trace_hops = arg
            continue

        if opt == "--encapsulate":
            encapsulate = arg
            continue

    loop = asyncio.get_event_loop()

    if trace_req:
        # MD-type 0x1, OAM set
        vxlan_header_values = VXLANGPE(int('00000100', 2), 0, 0x894F, int('111111111111111111111111', 2), 64)
        base_header_values = BASEHEADER(NSH_VERSION1, OAM_FLAG_AND_RESERVED, NSH_TYPE1_LEN, NSH_MD_TYPE1,
                                        NSH_NEXT_PROTO_OAM, int(sfp_id), int(sfp_index))
        ctx_header_values = CONTEXTHEADER(0, 0, 0, 0)
        trace_req_header_values = build_trace_req_header(OAM_TRACE_REQ_TYPE, 254,
                                                         remote_sff_ip, 55555)
        traceclient = MyTraceClient(loop, vxlan_header_values, base_header_values,
                                    ctx_header_values, trace_req_header_values, remote_sff_ip, int(remote_sff_port),
                                    int(num_trace_hops))

        start_client(loop, (str(ipaddress.IPv4Address(trace_req_header_values.ip_4)), 55555),
                     (remote_sff_ip, int(remote_sff_port)), (traceclient))
    else:
        if encapsulate == 'vxlan-gpe':
            vxlan_header_values = VXLANGPE(int('00000100', 2), 0, 0x894F, int('111111111111111111111111', 2), 64)
            base_values = BASEHEADER(NSH_VERSION1, int('00000000', 2), NSH_TYPE1_LEN, NSH_MD_TYPE1, NSH_NEXT_PROTO_IPV4,
                                     int(sfp_id), int(sfp_index))
            ctx_values = CONTEXTHEADER(0xffffffff, 0, 0xffffffff, 0)
            udpclient = MyUdpClient(loop, 'VXLAN/NSH', vxlan_header_values, base_values, ctx_values, remote_sff_ip,
                                    int(remote_sff_port))
            start_client(loop, (local_ip, 5000), (remote_sff_ip, remote_sff_port), udpclient)
        elif encapsulate == 'gre':
            # add GRE header
            gre_header_values = GREHEADER(int('1', 2), int('000000000000', 2), int('000', 2), 47,
                                          int('0000000000000000', 2), 0)

            base_values = BASEHEADER(NSH_VERSION1, int('00000000', 2), NSH_TYPE1_LEN, NSH_MD_TYPE1, NSH_NEXT_PROTO_IPV4,
                                     int(sfp_id), int(sfp_index))
            ctx_values = CONTEXTHEADER(0xffffffff, 0, 0xffffffff, 0)
            greclient = MyGreClient(loop, 'GRE/NSH', gre_header_values, base_values, ctx_values, remote_sff_ip,
                                    int(remote_sff_port))

            greclient.send_gre_nsh()
            # start_client(loop, (local_ip, 5000), (remote_sff_ip, remote_sff_port), greclient)
        else:
            print("--encapsulate must be specified, e.g. vxlan-gpe|gre")
            # loop.run_forever()


if __name__ == "__main__":
    main(sys.argv[1:])

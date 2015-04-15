#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import getopt
import sys
import asyncio
import os
import platform
from nsh.decode import *  # noqa
from nsh.encode import *  # noqa
import time


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

class MyNshBaseClass:
    def __init__(self):
        self.server_vxlan_values = VXLANGPE()
        self.server_base_values = BASEHEADER()
        self.server_ctx_values = CONTEXTHEADER()
        self.server_ethernet_values = ETHHEADER()
        self.server_trace_values = TRACEREQHEADER()


class MyVxlanGpeNshIpClient(MyNshBaseClass):
    def __init__(self, loop, encapsulate_type, encapsulate_header_values, base_header_values, ctx_header_values,
                 dest_addr, dest_port):
        super().__init__()
        self.transport = None
        self.loop = loop
        self.encapsulate_header_values = encapsulate_header_values
        self.base_header_values = base_header_values
        self.ctx_header_values = ctx_header_values
        # self.server_vxlan_values = VXLANGPE()
        # self.server_base_values = BASEHEADER()
        # self.server_ctx_values = CONTEXTHEADER()
        self.dest_addr = dest_addr
        self.dest_port = dest_port
        self.encapsulate_type = encapsulate_type

    def connection_made(self, transport):
        self.transport = transport
        # Building client packet to send to SFF
        packet = build_nsh_header(self.encapsulate_header_values,
                                  self.base_header_values,
                                  self.ctx_header_values)
        udp_packet = build_udp_packet(self.dest_addr, "10.0.1.1", 10000, self.dest_port, "test".encode('utf-8'))
        # logger.info("Sending VXLAN-GPE/NSH packet to SFF: %s", (self.dest_addr, self.dest_port))
        logger.info("Sending %s packet to SFF: %s", self.encapsulate_type, (self.dest_addr, self.dest_port))
        logger.debug("Packet dump: %s", binascii.hexlify(packet))
        # Send the packet
        self.transport.sendto(packet + udp_packet, (self.dest_addr, self.dest_port))

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


class MyVxlanGpeNshEthClient(MyNshBaseClass):
    def __init__(self, loop, encapsulate_type, ethernet_values, encapsulate_header_values, base_header_values,
                 ctx_header_values, dest_addr, dest_port):
        super().__init__()
        self.transport = None
        self.loop = loop
        self.ethernet_values = ethernet_values
        self.encapsulate_header_values = encapsulate_header_values
        self.base_header_values = base_header_values
        self.ctx_header_values = ctx_header_values
        # self.server_ethernet_values = ETHHEADER()
        # self.server_vxlan_values = VXLANGPE()
        # self.server_base_values = BASEHEADER()
        # self.server_ctx_values = CONTEXTHEADER()
        self.dest_addr = dest_addr
        self.dest_port = dest_port
        self.encapsulate_type = encapsulate_type

    def connection_made(self, transport):
        self.transport = transport
        # Building client dummy IP packet to send to SFF
        packet = build_nsh_eth_header(self.encapsulate_header_values,
                                      self.base_header_values,
                                      self.ctx_header_values,
                                      self.ethernet_values)
        # packet = build_vxlan_header(self.encapsulate_header_values,
        #                               self.ethernet_values)
        udp_inner_packet = build_udp_packet("172.20.33.195", "10.0.1.1", 10000, self.dest_port, "test".encode('utf-8'))
        gpe_nsh_ethernet_packet = packet + udp_inner_packet
        logger.debug("Ethernet dump: ", binascii.hexlify(gpe_nsh_ethernet_packet))
        logger.info("Sending %s packet to SFF: %s", self.encapsulate_type, (self.dest_addr, self.dest_port))
        # Send the packet
        try:
            self.transport.sendto(gpe_nsh_ethernet_packet, (self.dest_addr, self.dest_port))
        except socket.error as msg:
            print('Socket could not be created. Error Code : ' + str(msg))
            sys.exit()

    def datagram_received(self, data, addr):
        logger.info("Received packet from SFF: %s", addr)
        logger.debug("Packet dump: %s", binascii.hexlify(data))
        # Decode all the headers
        decode_vxlan(data, self.server_vxlan_values)
        decode_baseheader(data, self.server_base_values)
        decode_contextheader(data, self.server_ctx_values)
        self.loop.stop()

# Client side code: Build NSH packet encapsulated in GRE & NSH.

class MyGreClient:
    def __init__(self, loop, encapsulate_type, encapsulate_header_values, base_header_values, ctx_header_values,
                 dest_addr, dest_port):
        self.transport = None
        self.loop = loop
        self.encapsulate_header_values = encapsulate_header_values
        self.base_header_values = base_header_values
        self.ctx_header_values = ctx_header_values
        # Need to implement GRE decode in decode.py
        # self.server_gre_values = GREHEADER()
        self.server_base_values = BASEHEADER()
        self.server_ctx_values = CONTEXTHEADER()
        self.dest_addr = dest_addr
        # self.dest_port = dest_port
        self.encapsulate_type = encapsulate_type

    def send_gre_nsh(self):
        # create a raw socket
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_GRE)  # set port to GRE encapsulation

        except socket.error as msg:
            print('Socket could not be created. Error Code : ' + str(msg[0]) + ' Message ' + msg[1])
            sys.exit()

        # Building client packet to send to SFF
        gre_nsh_packet = build_nsh_header(self.encapsulate_header_values,
                                          self.base_header_values,
                                          self.ctx_header_values)

        logger.info("Sending %s packet to SFF: %s", self.encapsulate_type, self.dest_addr)
        logger.debug("Packet dump: %s", binascii.hexlify(gre_nsh_packet))

        s.sendto(gre_nsh_packet, (self.dest_addr, 0))


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
        packet = build_nsh_trace_header(self.vxlan_header_values,
                                        self.base_header_values,
                                        self.ctx_header_values,
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
        packet = build_nsh_trace_header(self.vxlan_header_values,
                                        self.base_header_values,
                                        self.ctx_header_values,
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
      --encapsulate gpe-nsh-ipv4
    python3.4 sff_client.py --remote-sff-ip 10.0.1.4 --remote-sff-port 4789 --sfp-id 1 --sfp-index 255

    Trace Example:
    python3.4 sff_client.py --remote-sff-ip 10.0.1.41 --remote-sff-port 4789 --sfp-id 1 --sfp-index 255 \
                            --trace-req --num-trace-hops 1
    :param argv:
    :return:
    """

    def handler(signum=None, frame=None):
        print("Signal handler called with signal {}".format(signum))
        loop.call_soon_threadsafe(loop.stop)
        time.sleep(1)
        loop.call_soon_threadsafe(loop.close)
        time.sleep(1)  # here check if process is done
        print("Wait done")
        sys.exit(0)

    # global base_values

    # Some Good defaults
    remote_sff_port = 4789
    remote_sff_ip = "127.0.0.1"
    local_ip = "0.0.0.0"
    sfp_id = 0x000001
    sfp_index = 3
    trace_req = False
    num_trace_hops = 254
    encapsulate = 'gpe-nsh-ipv4'  # make vxlan-gpe encapsulation default

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
                  "--sfp-id=<Service Function Path id> --sfp-index<SFP starting index> "
                  "--encapsulate=<gpe-nsh-ethernet|gre|gpe-nsh-ipv4>")
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
    for sig in [signal.SIGTERM, signal.SIGINT, signal.SIGHUP, signal.SIGQUIT]:
        signal.signal(sig, handler)

    # # create a raw socket
    # euid = os.geteuid()
    # sock_raw = None
    # if euid != 0:
    # print("Script not started as root. Running sudo...")
    # args = ['sudo', sys.executable] + sys.argv + [os.environ]
    # # the next line replaces the currently-running process with the sudo
    # os.execlpe('sudo', *args)
    #
    # if platform.system() == "Darwin":
    # try:
    # sock_raw = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_UDP)
    # # sock_raw = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
    #         # sock_raw.setsockopt(socket.IPPROTO_IP, socket.IP_HDRINCL, 1)
    #     except socket.error as msg:
    #         print("Socket could not be created. Error Code : {}".format(msg))
    #         sys.exit()
    # else:
    #     try:
    #         sock_raw = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_RAW)
    #     except socket.error as msg:
    #         print("Socket could not be created. Error Code : {}".format(msg))
    #         sys.exit()
    #
    # udp_packet = build_udp_packet("", "100.0.3.2", 10000, 4790, "test".encode('utf-8'))
    # try:
    #     sock_raw.sendto(udp_packet, ("100.0.3.2", 0))
    #     #sock_raw.sendto(udp_packet, int.from_bytes(socket.inet_aton("10.0.1.1"), byteorder='big'))
    # except socket.error as msg:
    #     print(msg)
    # sys.exit()

    if trace_req:
        # MD-type 0x1, OAM set
        vxlan_header_values = VXLANGPE(int('00000100', 2), 0, 0x894F, int('111111111111111111111111', 2), 64)
        # base_header_values = BASEHEADER(NSH_VERSION1, OAM_FLAG_AND_RESERVED, NSH_TYPE1_LEN, NSH_MD_TYPE1,
        #                                 NSH_NEXT_PROTO_OAM, int(sfp_id), int(sfp_index))
        base_header_values = BASEHEADER(int(sfp_id), int(sfp_index))
        ctx_header_values = CONTEXTHEADER()
        trace_req_header_values = build_trace_req_header(OAM_TRACE_REQ_TYPE, 254,
                                                         remote_sff_ip, 55555)
        traceclient = MyTraceClient(loop, vxlan_header_values, base_header_values,
                                    ctx_header_values, trace_req_header_values, remote_sff_ip, int(remote_sff_port),
                                    int(num_trace_hops))

        start_client(loop, (str(ipaddress.IPv4Address(trace_req_header_values.ip_4)), 55555),
                     (remote_sff_ip, int(remote_sff_port)), traceclient)
    else:
        if encapsulate == 'gpe-nsh-ipv4':
            vxlan_header_values = VXLANGPE()
            # base_values = BASEHEADER(NSH_VERSION1, NSH_FLAG_ZERO, NSH_TYPE1_LEN, NSH_MD_TYPE1, NSH_NEXT_PROTO_IPV4,
            #                          int(sfp_id), int(sfp_index))
            base_header_values = BASEHEADER(service_path=int(sfp_id), service_index=int(sfp_index))
            ctx_values = CONTEXTHEADER()
            udpclient = MyVxlanGpeNshIpClient(loop, 'VXLAN-GPE/NSH/IP v4', vxlan_header_values, base_header_values,
                                              ctx_values, remote_sff_ip, int(remote_sff_port))
            start_client(loop, (local_ip, 5000), (remote_sff_ip, remote_sff_port), udpclient)

        elif encapsulate == 'gre':
            # add GRE header
            gre_header_values = GREHEADER(int('0', 2), int('000000000000', 2), int('000', 2), 0x894F,
                                          int('0000000000000000', 2), 0)

            # base_values = BASEHEADER(NSH_VERSION1, int('00000000', 2), NSH_TYPE1_LEN, NSH_MD_TYPE1,
            # NSH_NEXT_PROTO_IPV4, int(sfp_id), int(sfp_index))
            base_header_values = BASEHEADER(int(sfp_id), int(sfp_index))
            ctx_values = CONTEXTHEADER()
            greclient = MyGreClient(loop, 'GRE/NSH', gre_header_values, base_header_values, ctx_values, remote_sff_ip,
                                    int(remote_sff_port))

            greclient.send_gre_nsh()

        elif encapsulate == 'gpe-nsh-ethernet':

            vxlan_header_values = VXLANGPE()
            # base_values = BASEHEADER(NSH_VERSION1, int('00000000', 2), NSH_TYPE1_LEN, NSH_MD_TYPE1,
            # NSH_NEXT_PROTO_ETH, int(sfp_id), int(sfp_index))
            base_header_values = BASEHEADER(service_path=int(sfp_id), service_index=int(sfp_index),
                                            proto=NSH_NEXT_PROTO_ETH)
            ctx_values = CONTEXTHEADER()

            ethernet_header_values = ETHHEADER(0x3c, 0x15, 0xc2, 0xc9, 0x4f, 0xbc, 0x08, 0x00, 0x27, 0xb6, 0xb0, 0x58,
                                               0x08, 0x00)

            udpclient = MyVxlanGpeNshEthClient(loop, 'VXLAN-GPE/NSH/Ethernet', ethernet_header_values,
                                               vxlan_header_values, base_header_values,
                                               ctx_values, remote_sff_ip,
                                               int(remote_sff_port))
            start_client(loop, (local_ip, 4790), (remote_sff_ip, remote_sff_port), udpclient)

        else:
            print("encapsulate must be specified, e.g. vxlan or gre")


if __name__ == "__main__":
    main(sys.argv[1:])

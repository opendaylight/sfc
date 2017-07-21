#!/usr/bin/env python3

# This program uses the hexdump module. Install it through pip (pip3 install
# hexdump) or download it at https://pypi.python.org/pypi/hexdump

# Take ideas from:
# https://cryptojedi.org/peter/teaching/netsec2014/sniffer.py
#
# Other tool that support NSH with UDP messages
# https://github.com/opendaylight/sfc/blob/master/sfc-test/nsh-tools/vxlan_tool.py

import hexdump
import socket
import argparse
import sys
import struct
import collections

from http.server import BaseHTTPRequestHandler
from http.client import HTTPResponse
from io import BytesIO

from uuid import getnode as get_mac

#===================================================================
def bytes_to_mac(bytesmac):
    return ":".join("{:02x}".format(x) for x in bytesmac)

def bytes_to_hex(bytes):
    return " ".join("{:02X}".format(x) for x in bytes)

#===================================================================

class MetaStruct(type):
    def __new__(cls, clsname, bases, dct):
        nt = collections.namedtuple(clsname, dct['fields'])
        def new(cls, record):
            return super(cls, cls).__new__(
                cls, *struct.unpack(dct['struct_fmt'], record))
        dct.update(__new__=new)
        return super(MetaStruct, cls).__new__(cls, clsname, (nt,), dct)
    def __str__(self):
        return "".join("{}({}) ".format(x, getattr(self, x)) for x in self._fields)
def pack_namedtuple(struct_fmt, nt):
    arg_values = []
    arg_values.append( struct_fmt )
    for x in nt._fields:
        arg_values.append( getattr(nt, x) )
    return struct.pack( *arg_values )

class StructEthHeader(object, metaclass=MetaStruct):
    fields = 'eth_dst eth_src eth_type'
    struct_fmt = '!6s6sH'
    def pack(self):
        return pack_namedtuple(self.struct_fmt, self)
    def __str__(self):
        return ("StructEthHeader(eth_dst=" + bytes_to_mac(getattr(self, 'eth_dst'))
            + ', eth_src=' + bytes_to_mac(getattr(self, 'eth_src'))
            + ', eth_type=' + str(getattr(self, 'eth_type')) + ")")

class StructNshHeader(object, metaclass=MetaStruct):
    fields = 'nsh_flags_length nsh_md_type nsh_np nsh_sph nsh_ctx1 nsh_ctx2 nsh_ctx3 nsh_ctx4'
    struct_fmt = '!HBBLLLLL'
    def pack(self):
        return pack_namedtuple(self.struct_fmt, self)
    def __str__(self):
        str1 = super().__str__()
        str2 = (' nsh_spi=' + str(self.get_nsh_spi())
            + ", nsh_si=" + str(self.get_nsh_si()))
        return str1 + str2
    def get_nsh_spi(self):
        return ((getattr(self, 'nsh_sph') & 0xFFFFFF00) >> 8)
    def get_nsh_si(self):
        return (getattr(self, 'nsh_sph') & 0x000000FF)
    def make_nsh_sph_with_spi(self, new_nsh_spi):
        return (new_nsh_spi << 8) + self.get_nsh_si()
    def make_nsh_sph_with_si(self, new_nsh_si):
        return (self.get_nsh_spi() << 8) + new_nsh_si
    def make_nsh_sph_with_spi_si(self, new_nsh_spi, new_nsh_si):
        return (new_nsh_spi << 8) + new_nsh_si


class StructUdpHeader(object, metaclass=MetaStruct):
    fields = 'udp_src_port udp_dst_port udp_data_length udp_checksum'
    struct_fmt = '!HHHH'
    def pack(self):
        return pack_namedtuple(self.struct_fmt, self)

class StructIpHeader(object, metaclass=MetaStruct):
    fields = 'ip_ver_ihl_type ip_total_length ip_id ip_flags_frag_offset ip_time2live ip_protocol ip_hdr_checksum ip_src ip_dst'
    struct_fmt = '!HHHHBBH4s4s'
    def pack(self):
        return pack_namedtuple(self.struct_fmt, self)
    def __str__(self):
        str_ip_src = socket.inet_ntoa(getattr(self, 'ip_src'))
        str_ip_dst = socket.inet_ntoa(getattr(self, 'ip_dst'))
        str1 = super().__str__()
        str2 = ' str_ip_src=' + str_ip_src + ", str_ip_dst=" + str_ip_dst
        return str1 + str2

class StructTcpHeaderWithoutOptions(object, metaclass=MetaStruct):
    fields = 'tcp_src_port tcp_dst_port tcp_seq_number tcp_ack tcp_byte_data_offset tcp_flags tcp_win_size tcp_checksum tcp_urgent_ptr'
    struct_fmt = '!HHLLBBHHH'
    def pack(self):
        return pack_namedtuple(self.struct_fmt, self)
    def __str__(self):
        str1 = super().__str__()
        (fin, syn, rst, psh, ack, urg) = parse_tcp_flags(getattr(self, 'tcp_flags'))
        tcp_flags_str = get_tcp_flags_str(fin, syn, rst, psh, ack, urg)
        str2 = ' tcp_flags_str=' + tcp_flags_str
        return str1 + str2


""" https://tools.ietf.org/html/rfc793
            96 bit pseudo header
    +--------+--------+--------+--------+
    |           Source Address          |
    +--------+--------+--------+--------+
    |         Destination Address       |
    +--------+--------+--------+--------+
    |  zero  |  PTCL  |    TCP Length   |
    +--------+--------+--------+--------+
  The TCP Length is the TCP header length plus the data length in
  octets (this is not an explicitly transmitted quantity, but is
  computed), and it does not count the 12 octets of the pseudo
  header.
"""
class StructPseudoHeader(object, metaclass=MetaStruct):
    fields = 'src dst zero protocol tcp_length'
    struct_fmt = '!4s4sBBH'
    def pack(self):
        return pack_namedtuple(self.struct_fmt, self)

#===================================================================

def print_frame(source, frame):
    print("Full frame: {}".format(source))
    hexdump.hexdump(frame)

def print_msg_hdr(outer_eth_header,
        nsh_header, eth_nsh_header,
        ip_header, udp_header,
        tcp_header_without_opt, tcp_options, tcp_payload):
    if outer_eth_header != None:
        print(str(StructEthHeader(outer_eth_header)))
    if nsh_header != None:
        print(str(StructNshHeader(nsh_header)))
    if eth_nsh_header != None:
        print(str(StructEthHeader(eth_nsh_header)))
    if ip_header != None:
        print(str(StructIpHeader(ip_header)))
    if udp_header != None:
        print(str(StructUdpHeader(udp_header)))
    if tcp_header_without_opt != None:
        str_tcp_options = ''
        if tcp_options != None:
            str_tcp_options = ' tcp_options=' + bytes_to_hex(tcp_options)
        print(str(StructTcpHeaderWithoutOptions(tcp_header_without_opt))
            + str_tcp_options)
    if tcp_payload != None:
        print('tcp_payload(' + str(tcp_payload) + ')')

#===================================================================

#####################################################################

"""Ethernet Frame consists of:
6 Byte Destination MAC address
6 Byte Source MAC address
2 Byte Ethertype
46 - 1500 Bytes Payload
"""
def parse_ethernet(frame):
    header_length = 14
    header = frame[:header_length]
    """
    ## In case 802.1Q tag compensation were required
    dst, src, type_code = struct.unpack("!6s6sH", header)
    if type_code == 0x8100:  # Encountered an 802.1Q tag, compensate.
        header_length = 18
        header = frame[:header_length]
        type_code = struct.unpack("!16xH", header)
    """
    payload = frame[header_length:]
    return header, payload

def make_ethernet_header_swap(header):
    outer_eth_header_nt = StructEthHeader(header)
    # Swap src <-> dst
    nt = outer_eth_header_nt._replace(
        eth_dst=getattr(outer_eth_header_nt, 'eth_src'),
        eth_src=getattr(outer_eth_header_nt, 'eth_dst'))
    return nt.pack()

def make_outer_ethernet_nsh_header(inner_eth_header):
    outer_eth_nsh_header_nt = StructEthHeader(inner_eth_header)

    ##???mac_src=getattr(outer_eth_nsh_header_nt, 'eth_src'),
    mac_src=struct.pack("!6s", bytes.fromhex(hex(get_mac())[2:]))
    # EtherType: "Network Service Header" 0x894F
    nt = outer_eth_nsh_header_nt._replace(
        eth_dst=getattr(outer_eth_nsh_header_nt, 'eth_dst'),
        eth_src=mac_src,
        eth_type=0x894F)
    return nt.pack()

#####################################################################
"""
https://www.ietf.org/id/draft-ietf-sfc-nsh-05.txt
NSH MD-type 1 -> four Context Headers 4-byte each
     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Ver|O|C|R|R|R|R|R|R|   Length  |  MD-type=0x1  | Next Protocol |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |          Service Path Identifer               | Service Index |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                Mandatory Context Header                       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                Mandatory Context Header                       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                Mandatory Context Header                       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                Mandatory Context Header                       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
"""
def parse_nsh(packet):
    header_length = 8
    context_length = 16
    header = packet[:header_length + context_length]
    payload = packet[header_length + context_length:]
    return header, payload

def make_nsh_decr_si(nsh_header):
    nt = StructNshHeader(nsh_header)
    # Decrement NSH Service Index
    nt = nt._replace( nsh_sph=nt.make_nsh_sph_with_si(nt.get_nsh_si() - 1) )
    return nt.pack()

def make_nsh_mdtype1(nsh_spi, nsh_si):
    # NSH MD-type 1 -> 8 bytes Base Header + four Context Headers 4-byte each
    nsh_header = bytes(8 + 16)
    # Version MUST be set to 0x0 by the sender, in this first revision of NSH.
    # For an MD Type of 0x1 (i.e. no variable length metadata is present),
    #  the C bit MUST be set to 0x0.
    # The Length MUST be of value 0x6 for MD Type equal to 0x1
    nt = StructNshHeader(nsh_header)
    nt = nt._replace(
        nsh_flags_length=0x6,
        nsh_md_type= 0x1, # MD Type = 0x1, four Context Headers
        nsh_np=0x3, # Ethernet
        nsh_sph=nt.make_nsh_sph_with_spi_si(nsh_spi, nsh_si),
        nsh_ctx1=0,
        nsh_ctx2=0,
        nsh_ctx3=0,
        nsh_ctx4=0
        )
    return nt.pack()

#####################################################################

"""Internet Header Format (RFC791)
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |Version|  IHL  |Type of Service|          Total Length         |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |         Identification        |Flags|      Fragment Offset    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |  Time to Live |    Protocol   |         Header Checksum       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                       Source Address                          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                    Destination Address                        |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                    Options                    |    Padding    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
"""

# Return header & payload
def parse_ip(packet):
    header_length_in_bytes = (packet[0] & 0x0F) * 4
    header = packet[:header_length_in_bytes]
    payload = packet[header_length_in_bytes:]
    return header, payload

def make_ip_header(header, new_ip_total_length):
    nt = StructIpHeader(header)
    # Change the Total Length
    nt = nt._replace( ip_total_length=new_ip_total_length )
    # Change the Header Checksum
    nt = nt._replace( ip_hdr_checksum=calculate_ip_checksum(nt.pack()) )
    return nt.pack()

#####################################################################

def parse_udp(packet):
    header_length = 8
    header = packet[:header_length]
    payload = packet[header_length:]
    return header, payload

#####################################################################

"""  TCP Header Format (RFC793)
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |          Source Port          |       Destination Port        |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                        Sequence Number                        |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                    Acknowledgment Number                      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |  Data |           |U|A|P|R|S|F|                               |
   | Offset| Reserved  |R|C|S|S|Y|I|            Window             |
   |       |           |G|K|H|T|N|N|                               |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |           Checksum            |         Urgent Pointer        |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                    Options                    |    Padding    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                             data                              |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
"""
def parse_tcp(packet):
    header_basic_length = 20
    header_without_options = packet[:header_basic_length]
    nt = StructTcpHeaderWithoutOptions(header_without_options)
    # Data Offset: 4 bits - The number of 32 bit words in the TCP Header.
    data_offset = getattr(nt, 'tcp_byte_data_offset') >> 4
    header_length = data_offset * 4
    options = packet[header_basic_length:header_length]
    payload = packet[header_length:]
    return header_without_options, options, payload

def make_ip_package(ip_header, header_without_options, options, payload):
    # Modify the TCP Checksum
    new_tcp_checksum = calculate_tcp_checksum(ip_header,
        header_without_options, options, payload)
    nt = StructTcpHeaderWithoutOptions(header_without_options)
    nt_new_header_without_options = nt._replace(
        tcp_checksum=new_tcp_checksum)

    new_ip_payload = nt_new_header_without_options.pack() + options + payload
    # Modify the IP Header (total_length and IP checksum
    new_ip_total_length = len(ip_header + new_ip_payload)
    new_ip_header = make_ip_header(ip_header, new_ip_total_length)

    return new_ip_header + new_ip_payload

def make_tpc_hdr_ack(header_without_options, port, num_bytes_added):
    nt = StructTcpHeaderWithoutOptions(header_without_options)
    new_tcp_ack = getattr(nt, 'tcp_ack')

    (tcp_fin_f, tcp_syn_f, tcp_rst_f, tcp_psh_f, tcp_ack_f,
        tcp_urg_f) = parse_tcp_flags(getattr(nt, 'tcp_flags'))

    # If packet does not belong to TCP 3-Way Handshake:
    # reduce the ACK according to the added bytes
    if ( (tcp_ack_f == True) and (tcp_syn_f == False) ):
        new_tcp_ack -= num_bytes_added
    nt_new_header_without_options = nt._replace(tcp_ack=new_tcp_ack)
    return nt_new_header_without_options.pack()


def make_tpc_hdr_seq(header_without_options, port, num_bytes_added):
    nt = StructTcpHeaderWithoutOptions(header_without_options)
    new_tcp_seq = getattr(nt, 'tcp_seq_number')

    (tcp_fin_f, tcp_syn_f, tcp_rst_f, tcp_psh_f, tcp_ack_f,
        tcp_urg_f) = parse_tcp_flags(getattr(nt, 'tcp_flags'))
    if ( (tcp_ack_f == True) and (tcp_syn_f == False) ):
        new_tcp_seq += num_bytes_added

    nt_new_header_without_options = nt._replace(tcp_seq_number=new_tcp_seq)
    return nt_new_header_without_options.pack()

def get_tpc_sync(header_without_options):
    nt = StructTcpHeaderWithoutOptions(header_without_options)
    (tcp_fin_f, tcp_syn_f, tcp_rst_f, tcp_psh_f, tcp_ack_f,
        tcp_urg_f) = parse_tcp_flags(getattr(nt, 'tcp_flags'))
    return tcp_syn_f

def goes_from_server_to_client(header_without_options, port):
    nt = StructTcpHeaderWithoutOptions(header_without_options)
    return ( port == getattr(nt, 'tcp_src_port') )
def goes_from_client_to_server(header_without_options, port):
    nt = StructTcpHeaderWithoutOptions(header_without_options)
    return ( port == getattr(nt, 'tcp_dst_port') )
def has_correct_port(header_without_options, port):
    nt = StructTcpHeaderWithoutOptions(header_without_options)
    return (( port == getattr(nt, 'tcp_src_port') ) or
        ( port == getattr(nt, 'tcp_dst_port') ) )

def parse_tcp_flags(flags):
    fin = (flags & 1) > 0
    syn = (flags & (1 << 1)) > 0
    rst = (flags & (1 << 2)) > 0
    psh = (flags & (1 << 3)) > 0
    ack = (flags & (1 << 4)) > 0
    urg = (flags & (1 << 5)) > 0
    return fin, syn, rst, psh, ack, urg

def get_tcp_flags_str(fin, syn, rst, psh, ack, urg):
    str = ""
    str += 'U' if urg else '-'
    str += 'A' if ack else '-'
    str += 'P' if psh else '-'
    str += 'R' if rst else '-'
    str += 'S' if syn else '-'
    str += 'F' if fin else '-'
    return str

#Base on #https://github.com/secdev/scapy/blob/master/scapy/utils.py
def calculate_checksum(pkt):
    import array
    if len(pkt) % 2 == 1:
        pkt += b'\0'
    s = sum(array.array("H", pkt))
    s = (s >> 16) + (s & 0xffff)
    s += s >> 16
    s = ~s
    if struct.pack("H",1) == "\x00\x01": # big endian
        return s & 0xffff
    else:
        return (((s>>8)&0xff)|s<<8) & 0xffff

def calculate_tcp_checksum(
        ip_header, header_without_options, options, payload):
    # create a 96 bit pseudo header
    ip_header_nt = StructIpHeader(ip_header)
    pseudo_header_nt = StructPseudoHeader(b'\x00' * 12)
    pseudo_header_nt = pseudo_header_nt._replace(
        src=getattr(ip_header_nt, 'ip_src'),
        dst=getattr(ip_header_nt, 'ip_dst'),
        zero=0,
        protocol=getattr(ip_header_nt, 'ip_protocol'),
        tcp_length=len(header_without_options + options + payload) )

    # skipping the checksum field itself
    header_without_options_nt = StructTcpHeaderWithoutOptions(header_without_options)
    header_without_options_nt = header_without_options_nt._replace(tcp_checksum=0)

    return calculate_checksum(
        pseudo_header_nt.pack()
        + header_without_options_nt.pack()
        + options
        + payload)

def calculate_ip_checksum(pkt):
    # skipping the checksum field itself
    pkt = pkt[0:10] + b'\0' + b'\0' + pkt[12:len(pkt)]
    return calculate_checksum(pkt)

def need_reset_tcp_connection(tcp_header_without_options):
    nt = StructTcpHeaderWithoutOptions(tcp_header_without_options)
    (tcp_fin_f, tcp_syn_f, tcp_rst_f, tcp_psh_f, tcp_ack_f,
        tcp_urg_f) = parse_tcp_flags(getattr(nt, 'tcp_flags'))
    return tcp_rst_f

#####################################################################
class HTTPRequest(BaseHTTPRequestHandler):
    def __init__(self, request_text):
        self.rfile = BytesIO(request_text)
        self.raw_requestline = self.rfile.readline()
        self.error_code = self.error_message = None
        self.parse_request()

    def send_error(self, code, message):
        self.error_code = code
        self.error_message = message
class HTTPResponseSocket():
    def __init__(self, response_text):
        self._file = BytesIO(response_text)
    def makefile(self, *args, **kwargs):
        return self._file

def modify_http_header(http_headers_items, key_enrich, value_enrich):
    return_http_header = bytearray(b'')
    updatedHeaderEnrichment = False
    hhe_key = key_enrich + ': '
    num_chars_separator = len(hhe_key) + len('\r\n')
    for k, v in http_headers_items:
        if k == key_enrich:
            separator = '#' * num_chars_separator
            v = v + separator + value_enrich
            updatedHeaderEnrichment = True
        line_text = k + ': ' + v + '\r\n'
        return_http_header.extend(line_text.encode('utf-8'))
    if not updatedHeaderEnrichment:
        line_text = hhe_key + value_enrich + '\r\n'
        return_http_header.extend(line_text.encode('utf-8'))
    return_http_header.extend('\r\n'.encode('utf-8'))
    return return_http_header

def processGetHttpRequest(http_request_text, key_enrich, value_enrich):
    return_http_request = bytearray(b'')
    request = None
    if (len(http_request_text) > 3):
        if (http_request_text[:3] ==  b'GET'):
            request = HTTPRequest(http_request_text)
            ##??? print ("HTTP GET")
    if (request != None):
        line_text = ( request.command + ' ' +
            request.path + ' ' + request.request_version + '\r\n' )
        return_http_request.extend(line_text.encode('utf-8'))
        if (request.error_code != None):
            print ("error_code: " + str(request.error_code))
            print ("error_message: " + str(request.error_message))
        else:
            return_http_request.extend(modify_http_header(
                request.headers.items(), key_enrich, value_enrich))

    return return_http_request

def processHttpResponse(http_response_text, key_enrich, value_enrich):
    return_http_response = bytearray(b'')
    response = None
    http_first_chars = b''
    if (len(http_response_text) > 4):
        http_first_chars = http_response_text[:4]
        if (http_first_chars ==  b'HTTP'):
            response = HTTPResponse(HTTPResponseSocket(http_response_text))
            response.begin()
            ##??? print (str(http_first_chars))

    if (response != None):
        str_version = "HTTP/1.0"
        if (response.version == 11):
            str_version = "HTTP/1.1"
        line_text = ( str_version + ' ' +
            str(response.status) + ' ' + str(response.reason) + '\r\n' )
        return_http_response.extend(line_text.encode('utf-8'))

        return_http_response.extend(
            modify_http_header(response.getheaders(), key_enrich, value_enrich))

        return_http_response.extend(response.read())

    return return_http_response


#####################################################################

def parse_frame(frame, port):
    pkt = None
    outer_eth_header = None
    outer_eth_payload = None
    nsh_header = None
    nsh_payload = None
    eth_nsh_header = None
    eth_nsh_payload = None
    next_eth_payload = None
    ip_header = None
    ip_payload = None
    udp_header = None
    udp_payload = None
    tcp_header_without_opt = None
    tcp_options = None
    tcp_payload = None

    continue_next = False
    reset_connection = False

    (outer_eth_header, outer_eth_payload) = parse_ethernet(frame)
    outer_eth_header_nt = StructEthHeader(outer_eth_header)
    outer_eth_type = getattr(outer_eth_header_nt, 'eth_type')

    if (outer_eth_type == 0x894F): # "Network Service Header" 0x894F
        # Eth + NSH
        (nsh_header, nsh_payload) = parse_nsh(outer_eth_payload)
        (eth_nsh_header, eth_nsh_payload) = parse_ethernet(nsh_payload)
        next_eth_payload = eth_nsh_payload
    elif (outer_eth_type == 0x0800): # EtherType: IPv4 0x0800
        next_eth_payload = outer_eth_payload
    else:
        next_eth_payload = outer_eth_payload
        # e.g. 0x0806 - Address Resolution Protocol (ARP)
        print("Frame with ethernet type {} received; skipping...".format(
            outer_eth_type))
        continue_next = True

    if not continue_next:
        (ip_header, ip_payload) = parse_ip(next_eth_payload)
        ip_header_nt = StructIpHeader(ip_header)
        ip_protocol = getattr(ip_header_nt, 'ip_protocol')

        if ip_protocol == 17:  # UDP is protocol 17
            (udp_header, udp_payload) = parse_udp(ip_payload)
            reset_connection = True
        elif ip_protocol == 6:  # TCP is protocol 6
            (tcp_header_without_opt, tcp_options, tcp_payload) = parse_tcp(ip_payload)
            reset_connection = need_reset_tcp_connection(tcp_header_without_opt)

            if (not has_correct_port(tcp_header_without_opt, port)):
                print("Frame with a non-treated TCP port; skipping...")
                continue_next = True
        elif ip_protocol == 1: # Internet Control Message Protocol (ICMP)
            print("Packet with ICMP protocol received; ...")
        else:
            print("Packet with protocol nr. {} received; skipping...".format(
                ip_protocol))
            continue_next = True

    if not continue_next:
        print_msg_hdr(outer_eth_header,
            nsh_header, eth_nsh_header,
            ip_header, udp_header,
            tcp_header_without_opt, tcp_options, tcp_payload)

    return(continue_next,
        reset_connection,
        outer_eth_header,
        outer_eth_payload,
        nsh_header,
        eth_nsh_header,
        next_eth_payload,
        ip_header,
        ip_payload,
        udp_header,
        udp_payload,
        tcp_header_without_opt,
        tcp_options,
        tcp_payload)


#####################################################################

def main_sf_hhe(args_interface, args_name, args_port):
    reset_connection = False
    # Listen IP traffic
    s = socket.socket(socket.AF_PACKET, socket.SOCK_RAW, socket.ntohs(0x0003))
    s.bind((args_interface, 0))

    ack_can_be_modified = False
    seq_can_be_modified = False
    while True:
        frame, source = s.recvfrom(65565)
        ##??? print_frame(source, frame)

        pkt = None

        (continue_next,
            reset_connection,
            outer_eth_header,
            outer_eth_payload,
            nsh_header,
            eth_nsh_header,
            next_eth_payload,
            ip_header,
            ip_payload,
            udp_header,
            udp_payload,
            tcp_header_without_opt,
            tcp_options,
            tcp_payload) = parse_frame(frame, args_port)

        if continue_next:
            continue

        new_ip_package = None
        new_tcp_header_without_opt = None
        new_tcp_payload = b''

        if tcp_header_without_opt != None:  # TCP protocol
            hhe_tag = 'HHE'
            extra_bytes_cte = len(hhe_tag + ': ') + len(args_name) + len('\r\n')

            if get_tpc_sync(tcp_header_without_opt):
                ack_can_be_modified = False
                seq_can_be_modified = False

            if goes_from_client_to_server(tcp_header_without_opt, args_port):
                new_tcp_payload = processGetHttpRequest(
                    tcp_payload, hhe_tag, args_name)

            if goes_from_server_to_client(tcp_header_without_opt, args_port):
                new_tcp_payload = processHttpResponse(
                    tcp_payload, hhe_tag, args_name)

            if ((new_tcp_payload != bytearray(b'')) and
                ( (len(new_tcp_payload) - len(tcp_payload)) != extra_bytes_cte )):
                print("ERROR Integrity Test: Wrong number of added bytes")
                sys.exit(-1)

            new_tcp_header_without_opt = tcp_header_without_opt
            if (ack_can_be_modified):
                print(" **** TCP CHANGING ACK")
                new_tcp_header_without_opt = make_tpc_hdr_ack(
                    new_tcp_header_without_opt, args_port, extra_bytes_cte )

            if (seq_can_be_modified):
                print(" **** TCP CHANGING SEQ")
                new_tcp_header_without_opt = make_tpc_hdr_seq(
                    new_tcp_header_without_opt, args_port, extra_bytes_cte )

            if (new_tcp_payload != bytearray(b'')):
                print(" **** TCP MODIFIED PAYLOAD")
                new_ip_package = make_ip_package(ip_header,
                    new_tcp_header_without_opt, tcp_options, new_tcp_payload)
            else:
                print(" **** TCP NON-MODIFIED PAYLOAD")
                new_ip_package = (ip_header
                    + new_tcp_header_without_opt + tcp_options + tcp_payload)

        elif udp_header != None:  # UDP protocol
            print(" **** UDP")
            new_ip_package = ip_header + udp_header + udp_payload
        elif ip_header != None:  # IP but NON TCP or UDP protocol
            print(" **** IP - NON TCP or UDP ")
            new_ip_package = ip_header + ip_payload
        else:
            print(" **** NON IP")

        # Build Ethernet packet
        outer_eth_header_swap = make_ethernet_header_swap(outer_eth_header)

        initial_package = None
        if (nsh_header != None):
            # Modify the original NSH (decrement NSH Service Index)
            outgoing_nsh_header = make_nsh_decr_si(nsh_header)
            initial_package = (outer_eth_header_swap + outgoing_nsh_header
                + eth_nsh_header)
        else:
            initial_package = outer_eth_header_swap

        if (new_ip_package != None):
            pkt = (initial_package + new_ip_package)
        else:
            pkt = (initial_package + next_eth_payload)

        ## Set when ACK & Sequence number has to be changed
        if (new_tcp_payload != bytearray(b'')):
            ack_can_be_modified = True
            if goes_from_server_to_client(tcp_header_without_opt, args_port):
                seq_can_be_modified = True

        ##???
        print_frame('Outgoing', pkt)

        # Send all data
        while pkt:
            sent = s.send(pkt)
            pkt = pkt[sent:]

        if reset_connection:
            print("XX Reset_connection XX")
            s.close()
            reset_connection = False
            s = socket.socket(socket.AF_PACKET, socket.SOCK_RAW, socket.ntohs(0x0003))
            s.bind((args_interface, 0))
            ack_can_be_modified = False
            seq_can_be_modified = False

        continue

#####################################################################

def main_sf_eth_nsh(args_interface, args_port, args_nsh_spi):
    reset_connection = False
    # Listen IP traffic
    s = socket.socket(socket.AF_PACKET, socket.SOCK_RAW, socket.ntohs(0x0003))
    s.bind((args_interface, 0))

    ack_can_be_modified = False
    seq_can_be_modified = False
    while True:
        frame, source = s.recvfrom(65565)

        pkt = None

        (continue_next,
            reset_connection,
            outer_eth_header,
            outer_eth_payload,
            nsh_header,
            eth_nsh_header,
            next_eth_payload,
            ip_header,
            ip_payload,
            udp_header,
            udp_payload,
            tcp_header_without_opt,
            tcp_options,
            tcp_payload) = parse_frame(frame, args_port)

        if continue_next:
            continue

        new_ip_package = None
        if tcp_header_without_opt != None:  # TCP protocol
            new_ip_package = (ip_header
                + tcp_header_without_opt + tcp_options + tcp_payload)
        elif udp_header != None:  # UDP protocol
            new_ip_package = ip_header + udp_header + udp_payload
        else:
            continue

        if (nsh_header != None):
            # REMOVE Ethernet+NSH Header
            pkt = (eth_nsh_header + new_ip_package)
        else:
            # ADD Outer Ethernet+NSH from received Ethernet
            outer_eth_nsh_header = make_outer_ethernet_nsh_header(
                outer_eth_header)
            # The first classifier (i.e. at the boundary of the NSH domain)
            # in the NSH Service Function Path, SHOULD set the SI to 255
            nsh_si = 255
            new_nsh_header = make_nsh_mdtype1(args_nsh_spi, nsh_si)
            pkt = (outer_eth_nsh_header + new_nsh_header
                + outer_eth_header + new_ip_package)

        ##???
        print_frame('Outgoing', pkt)

        # Send all data
        while pkt:
            sent = s.send(pkt)
            pkt = pkt[sent:]

        if reset_connection:
            print("XX Reset_connection XX")
            s.close()
            reset_connection = False
            s = socket.socket(socket.AF_PACKET, socket.SOCK_RAW, socket.ntohs(0x0003))
            s.bind((args_interface, 0))
            ack_can_be_modified = False
            seq_can_be_modified = False

        continue



if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Python3 script to emulate a Service Function HTTP Header Enrichment Node', prog='sf_hhe.py', usage='%(prog)s [options]', add_help=True)
    parser.add_argument('-i', '--interface',
                        help='Specify the interface to send/receive the packages')
    parser.add_argument('-n', '--name',
                        help='Name to be included in the HTTP Header')
    parser.add_argument('-p', '--port', type=int, default=8000,
                        help='Destination TCP Port to filter (default 8000)')

    parser.add_argument('-e', '--eth_nsh', action='store_true', default=False,
        help='Forward the packet adding the Ethernet NSH Header if needed)')
    parser.add_argument('-s', '--nsh_spi', type=int, default=1,
        help='Service Path Identifier to include in NSH (--eth_nsh required) (default=1)')

    args = parser.parse_args()

    if (((not args.eth_nsh) and ((args.name is None) or (args.port is None)))
        or (args.interface is None) ):
        parser.print_help()
        sys.exit(-1)

    ##???
    print("args.interface(" + str(args.interface) + ")")
    print("args.name(" + str(args.name) + ")")
    print("args.port(" + str(args.port) + ")")
    print("args.eth_nsh(" + str(args.eth_nsh) + ")")
    print("args.nsh_spi(" + str(args.nsh_spi) + ")")

    if (args.eth_nsh):
        main_sf_eth_nsh(args.interface, args.port, args.nsh_spi)
    else:
        main_sf_hhe(args.interface, args.name, args.port)

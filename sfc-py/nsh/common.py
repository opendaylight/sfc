from ctypes import Structure, c_ubyte, c_ushort, c_uint
import struct
import array


__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "rapenno@gmail.com"
__status__ = "alpha"


#: constants
PAYLOAD_START_INDEX = 32

NSH_TYPE1_DATA_PACKET = int('010000000000011000000001', 2)
NSH_TYPE1_LEN = 0x6
NSH_MD_TYPE1 = 0x1
NSH_VERSION1 = int('01', 2)
NSH_NEXT_PROTO_IPV4 = int('00000001', 2)
NSH_NEXT_PROTO_OAM = int('00000100', 2)
NSH_NEXT_PROTO_ETH = int('00000011', 2)
NSH_BASE_HEADER_START_OFFSET = 8

#: IP constants

IP_HEADER_LEN = 5
IPV4_VERSION = 4
IPV4_PACKET_ID = 54321
IPV4_TTL = 255
IPV4_TOS = 0
IPV4_IHL_VER = (IPV4_VERSION << 4) + IP_HEADER_LEN

#:  VXLAN-gpe constants
VXLAN_NEXT_PROTO_NSH = int('00000100', 2)

#: NSH OAM Constants
NSH_TYPE1_OAM_PACKET = int('01100000000001100000000100000100', 2)
OAM_VERSION_AND_FLAG = int('01100000', 2)
OAM_FLAG_AND_RESERVED = int('10000000', 2)
OAM_TRACE_REQ_TYPE = int('00000001', 2)
OAM_TRACE_RESP_TYPE = int('00000010', 2)
NSH_OAM_PKT_START_OFFSET = 32
NSH_OAM_TRACE_DEST_IP_REPORT_OFFSET = 36
NSH_OAM_TRACE_DEST_IP_REPORT_LEN = 16
NSH_OAM_TRACE_HDR_LEN = 20
NSH_OAM_TRACE_RESP_START_OFFSET = NSH_OAM_PKT_START_OFFSET + NSH_OAM_TRACE_HDR_LEN
NSH_OAM_TRACE_RESP_SF_TYPE_LEN_START_OFFSET = NSH_OAM_TRACE_RESP_START_OFFSET
NSH_OAM_TRACE_RESP_SF_TYPE_START_OFFSET = NSH_OAM_TRACE_RESP_SF_TYPE_LEN_START_OFFSET + 1


class VXLANGPE(Structure):
    _fields_ = [('flags', c_ubyte),
                ('reserved', c_uint, 16),
                ('protocol_type', c_uint, 8),
                ('vni', c_uint, 24),
                ('reserved2', c_uint, 8)]


class GREHEADER(Structure):
    _fields_ = [('c', c_uint, 1),
                ('reserved0', c_uint, 12),
                ('version', c_uint, 3),
                ('protocol_type', c_uint, 16),
                ('checksum', c_uint, 16),
                ('reserved1', c_uint, 16)]


class ETHHEADER(Structure):
    _fields_ = [('dmac0', c_ubyte),
                ('dmac1', c_ubyte),
                ('dmac2', c_ubyte),
                ('dmac3', c_ubyte),
                ('dmac4', c_ubyte),
                ('dmac5', c_ubyte),
                ('smac0', c_ubyte),
                ('smac1', c_ubyte),
                ('smac2', c_ubyte),
                ('smac3', c_ubyte),
                ('smac4', c_ubyte),
                ('smac5', c_ubyte),
                ('ethertype0', c_ubyte),
                ('ethertype1', c_ubyte)]


class BASEHEADER(Structure):
    _fields_ = [('version', c_ushort, 2),
                ('flags', c_ushort, 8),
                ('length', c_ushort, 6),
                ('md_type', c_ubyte),
                ('next_protocol', c_ubyte),
                ('service_path', c_uint, 24),
                ('service_index', c_uint, 8)]


class CONTEXTHEADER(Structure):
    _fields_ = [('network_platform', c_uint),
                ('network_shared', c_uint),
                ('service_platform', c_uint),
                ('service_shared', c_uint)]


class TRACEREQHEADER(Structure):
    _fields_ = [('oam_type', c_ubyte),
                ('sil', c_ubyte),
                ('port', c_ushort),
                ('ip_1', c_uint),
                ('ip_2', c_uint),
                ('ip_3', c_uint),
                ('ip_4', c_uint)]


class IPHEADER(Structure):

    _fields_ = [
        ('ip_ihl', c_ubyte),
        ('ip_ver', c_ubyte),
        ('ip_tos', c_ubyte),
        ('ip_tot_len', c_ushort),
        ('ip_id', c_ushort),
        ('ip_frag_offset', c_ushort),
        ('ip_ttl', c_ubyte),
        ('ip_proto', c_ubyte),
        ('ip_chksum', c_ushort),
        ('ip_saddr', c_uint),
        ('ip_daddr', c_uint)]

    def get_ip_header_pack(self):
        ip_header_pack = struct.pack('!B B H H H B B H I I', IPV4_IHL_VER, self.ip_tos, self.ip_tot_len, self.ip_id,
                                     self.ip_frag_offset, self.ip_ttl, self.ip_proto, self.ip_chksum, self.ip_saddr,
                                     self.ip_daddr)
        return ip_header_pack

        # in 6
        # {
        # /* Compute Internet Checksum for "count" bytes
        #          *         beginning at location "addr".
        #          */
        #     register long sum = 0;
        #
        #      while( count > 1 )  {
        #         /*  This is the inner loop */
        #             sum += * (unsigned short) addr++;
        #             count -= 2;
        #     }
        #
        #         /*  Add left-over byte, if any */
        #     if( count > 0 )
        #             sum += * (unsigned char *) addr;
        #
        #         /*  Fold 32-bit sum to 16 bits */
        #     while (sum>>16)
        #         sum = (sum & 0xffff) + (sum >> 16);
        #
        #     checksum = ~sum;
        # }

    def get_ip_checksum(self):
        """
        Function for IP header checksum calculation.

        """
        ip_header_pack = self.get_ip_header_pack()
        s = 0
        i = 0
        n = len(ip_header_pack) % 2
        for i in range(0, len(ip_header_pack) - n, 2):
            s += (ip_header_pack[i] << 8) + (ip_header_pack[i+1])
        if n:
            s += ip_header_pack[i+1]
        while s >> 16:
            # print("s >> 16: ", s >> 16)
            s = (s & 0xFFFF) + (s >> 16)
        # print("sum:", s)
        s = ~s & 0xffff
        return s

    def set_ip_checksum(self, checksum):
        self.ip_chksum = checksum
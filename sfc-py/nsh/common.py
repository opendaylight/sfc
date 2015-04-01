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
        ('v_hl', c_ubyte),
        ('tos', c_ubyte),
        ('len', c_ushort),
        ('id', c_ushort),
        ('off', c_ushort),
        ('ttl', c_ubyte),
        ('proto', c_ubyte),
        ('chksum', c_ushort),
        ('src', c_uint),
        ('dst', c_uint)]

    def ip_header_pack(self):
        self.ip_header_pack = struct.pack('!BBHHHBBH4s4s', self.v_hl, self.tos, self.len, self.id, self.off, self.ttl,
                                          self.proto, self.chksum, self.src, self.dst)

    def ip_checksum(self):
        ip_header_array_ushort = array.array("H", self.ip_header_pack)
        temp_sum = sum(ip_header_array_ushort)
        while temp_sum >> 16:
            temp_sum = (temp_sum >> 16) + (temp_sum & 0xffff)
        checksum = ~temp_sum
        return checksum
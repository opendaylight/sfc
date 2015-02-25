__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2015, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "rapenno@gmail.com"
__status__ = "alpha"

from ctypes import Structure, c_ubyte, c_ushort, c_uint


class VXLANGPE(Structure):
    _fields_ = [('flags', c_ubyte),
                ('reserved', c_ubyte),
                ('protocol_type', c_ushort),
                ('vni', c_uint, 24),
                ('reserved2', c_uint, 8)]


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

__author__ = "Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "rapenno@gmail.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

"""
    Provides a Function to fully encode VXLAN-GPE + NSH Base + Context Headers
"""

import struct


def build_packet(vxlan_header_values, base_header_values, ctx_header_values):
    # Build VXLAN header
    vxlan_header = struct.pack('!B B H I', vxlan_header_values.flags, vxlan_header_values.reserved, vxlan_header_values.protocol_type,
                               (vxlan_header_values.vni << 8) + vxlan_header_values.reserved2)
    # Build base NSH header
    base_header = struct.pack('!H B B I', (base_header_values.version << 14) + (base_header_values.flags << 6) + base_header_values.length,
                              base_header_values.md_type,
                              base_header_values.next_protocol, (base_header_values.service_path << 8) + base_header_values.service_index)
    # Build NSH context headers
    context_header = struct.pack('!I I I I', ctx_header_values.network_platform, ctx_header_values.network_shared,
                                 ctx_header_values.service_platform, ctx_header_values.service_shared)
    return vxlan_header + base_header + context_header

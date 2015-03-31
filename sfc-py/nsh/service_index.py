__author__ = "Jim Guichard"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "jguichar@cisco.com"
__status__ = "alpha"


#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


"""Network Service Header (NSH) Service Index Processing"""


def set_service_index(rw_data, service_index):
    """
    Set service index in received packet data

    :param rw_data: packet data
    :type rw_data: bytearray
    :param service_index: new service index
    :type service_index: int

    """
    rw_data[15] = service_index


def process_service_index(rw_data, server_base_values):
    """
    Manage service index - test if it's equal to zero or decrement it otherwise

    :param rw_data: packet data
    :type rw_data: bytearray
    :param server_base_values: NSH base header
    :type server_base_values: `:class:nsh.common.BASEHEADER`

    :return tuple

    """
    if server_base_values.service_index == 0:
        si_result = 0

    else:
        server_base_values.service_index -= 1
        set_service_index(rw_data, server_base_values.service_index)

        si_result = 1

    return rw_data, si_result

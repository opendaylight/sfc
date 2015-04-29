#
# Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


import pytest
import asyncio

from struct import pack

from sfc.common.services import BasicService


__author__ = 'Dusan Madar'
__email__ = 'madar.dusan@gmail.com'
__copyright__ = 'Copyright(c) 2015, Cisco Systems, Inc.'
__version__ = '0.1'
__status__ = 'alpha'


"""
SFC supported services tests
"""


def _nsh_ctx_header(nsh_ctx_values):
    """
    Helper - create NSH context header like bytes sequence

    :param nsh_ctx_values: values for `:class:nsm.common.CONTEXTHEADER`
    :type nsh_ctx_values: tuple of int

    :return bytes

    """
    return pack('!I I I I', *nsh_ctx_values)


@pytest.fixture
def basic_service():
    """
    Fixture - instantiates BasicService

    :return `:class:common.services.BasicService`

    """
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    return BasicService(loop)


@pytest.fixture
def packet_data():
    """
    Fixture - mock a NSH packet

    :return bytes

    """
    nsh_packet = (b'\x04\x00\x89O\x00\x05\x00@@\x06\x01\x01\x00\x00\x01\xff'
                  b'\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00'
                  b'\x00\x00E\x00\x00\x1d\xc6f@\x00@\x11vg\x7f\x00\x00\x01'
                  b'\x7f\x00\x00\x01>\x80\x1f\x9a\x00\t\x99\xbf\n')

    return nsh_packet


@pytest.mark.parametrize('new_meta', [(20, 20, 0, 20), (192, 2, 15, 80)])
def test_nsh_metadata_update(new_meta, basic_service, packet_data):
    """
    Test if SFC supported services are able to update NSH metadata, i.e.
    update NSH context header data.

    Pass: if new packet data are not equal to original packet data,
          if new NSH CTX header is not equal original NSH CTX header,
          if new CTX header is equal to expected CTX header
    Fail: if an exception is raised or the above condition fails

    """
    original_ctx_header = packet_data[16:32]
    new_ctx_header = _nsh_ctx_header(new_meta)

    original_packet_data = bytearray(packet_data)
    new_packet_data = basic_service._update_metadata(original_packet_data,
                                                     *new_meta)

    assert new_packet_data != packet_data
    assert new_ctx_header != original_ctx_header
    assert new_ctx_header == new_packet_data[16:32]

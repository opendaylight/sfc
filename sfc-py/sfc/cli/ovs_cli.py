#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import logging

from ..common.sfc_globals import OF_TABLEID


__author__ = "Paul Quinn"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.1"
__email__ = "paulq@cisco.com"
__status__ = "alpha"

tenant = 0x500
vxlannshport = 6633
logger = logging.getLogger(__name__)


def init_ovs():
    """"""
    init_cli = ('sudo ovs-vsctl add-port br-s vxlan-s -- set interface vxlan-s'
                ' type=vxlan options:key=flow options:remote_ip=flow '
                'options:dst_port=' + str(vxlannshport) +
                ' options:nsp=flow options:nsi=flow')
    logger.info("Initializing OVS: %s \n", init_cli)
    # subprocess.call([init_cli], shell=True)


def process_ovs_sff__cli(data_plane_path):
    # print('\nOVS cli module received data plane path: \n', data_plane_path)

    for key in data_plane_path:
        spi = key                       # store the SPI value
        rsp = data_plane_path[key]      # store the rendered service path
        create_of_sff_cli(spi, rsp)     # generate OF CLI

    return


def create_of_sff_cli(spi, rsp):
    of_cli = ('sudo ovs-ofctl add-flow br-s table=0, priority=200, '
              'tun_id=' + str(tenant) + ' nsp=0x' + str(spi) +
              ',actions=resubmit(,' + str(OF_TABLEID) + ')')

    logger.info(of_cli)
    # subprocess.call([of_cli], shell=True)
    of_cli = 'sudo ovs-ofctl add-flow br-s table=0,priority=100,actions=drop'
    logger.info(of_cli)
    # subprocess.call([of_cli], shell=True)

    for index in rsp:
        of_cli = ('sudo ovs-ofctl add-flow br-s table=' + str(OF_TABLEID) +
                  ', priority=100, nsp=0x' + str(index) + ',actions=set_field:' +
                  rsp[index]['ip'] + '->tun_dst,in_port')
        logger.info(of_cli)
        # subprocess.call([of_cli], shell=True)

    OF_TABLEID += 1  # noqa

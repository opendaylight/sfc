__author__ = "Jim Guichard, Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "jguichar@cisco.com, rapenno@gmail.com"
__status__ = "alpha"

#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

"""
TODO: edit this
"""


import socket
import logging
import asyncio

from common.services import UDP, CUDP, find_service

logger = logging.getLogger(__name__)


def start_server(loop, addr, udpserver, message):
    """
    TODO: add docstring

    :param loop:
    :type loop: `:class:asyncio.unix_events._UnixSelectorEventLoop`
    :param addr: IP address and port
    :type addr: tuple (str, int)
    :param udpserver: UDP server instance
    :type udpserver: `:class:ControlUdpServer`
    :param message: message which will displayed upon server start
    :type message: str

    :return `:class:asyncio.selector_events._SelectorDatagramTransport`

    """
    #t = asyncio.Task(loop.create_datagram_endpoint(
    #    lambda: udpserver, local_addr=addr))
    listen = loop.create_datagram_endpoint(lambda: udpserver, local_addr=addr)
    transport = loop.run_until_complete(listen)[0]

    logger.info(message + ' ' + str(addr[1]))

    return transport


def start_service(service_name, service_ip, service_port, service_type,
                  service_control_port, service_thread, service_start_message):
    """
    Start a service.

    The Python agent uses this function as the thread start whenever it wants
    to create a service.

    :param service_name: SF name
    :type service_name: str
    :param service_ip: SF IP address
    :type service_ip: str
    :param service_port: SF port
    :type service_port: int
    :param service_type: SF type
    :type service_type: str
    :param service_control_port: SF control port
    :type service_control_port: int
    :param service_thread:
    :type service_thread: `:class:threading.Thread`
    :param service_start_message: what will be logged upon a service start
    :type service_start_message: str

    """
    # TODO: is this even necessary?
    udpserver_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    logging.basicConfig(level=logging.INFO)

    service_class = find_service(service_type)
    logger.info('Starting %s service ...', service_type.upper())

    service = service_class(loop)
    udpserver_transport = start_server(loop,
                                       (service_ip, service_port),
                                       service,
                                       service_start_message)

    udpserver_socket = udpserver_transport.get_extra_info('socket')
    service_thread[service_name]['socket'] = udpserver_socket

    control_udp_server = find_service(CUDP)(loop)
    start_server(loop,
                 (service_ip, service_control_port),
                 control_udp_server,
                 'Listening for Control messages on port:')

    loop.run_forever()
    udpserver_socket.close()
    loop.close()


def start_sf(sf_name, sf_ip, sf_port, sf_type, sf_control_port, sf_thread):
    """
    Start a Service Function
    """
    logger.info('Starting Service Function')
    start_service(sf_name, sf_ip, sf_port, sf_type, sf_control_port,
                  sf_thread, 'Starting new Service Function ...')


def start_sff(sff_name, sff_ip, sff_port, sff_control_port, sff_thread):
    """
    Start a Service Function Forwarder
    """
    logger.info('Starting Service Function Forwarder')
    start_service(sff_name, sff_ip, sff_port, UDP, sff_control_port,
                  sff_thread, 'Listening for NSH packets on port:')

# This does not work in MacOS when SFF/SF are different python
# applications on the same machine
# def start_server(loop, addr, service, myip):
#     t = asyncio.Task(loop.create_datagram_endpoint(
#         service, local_addr=(myip, 57444), remote_addr=addr))
#     loop.run_until_complete(t)
#     print('Listening for packet on:', addr)

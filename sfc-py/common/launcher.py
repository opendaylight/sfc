#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import socket
import logging
import asyncio

from time import sleep
from threading import Thread
from common.sfc_globals import sfc_globals
from common.services import SF, SFF, CUDP, find_service


__author__ = "Jim Guichard, Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "jguichar@cisco.com, rapenno@gmail.com"
__status__ = "alpha"


"""
Manage service [and its' associated thread] starting and stopping
"""

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

#: constants
# WE assume agent and thread(s) are co-located
SFF_UDP_IP = "127.0.0.1"
DPCP = 'data_plane_control_port'


def _get_global_threads(service_type):
    """
    Get the global service threads dictionary.

    If service type is not a SFF then it must be a type of SF.

    :param service_type: service type (SF or SFF)
    :type service_type: str

    :return dict

    """
    if service_type == SFF:
        global_threads = sfc_globals.get_sff_threads()
    else:
        global_threads = sfc_globals.get_sf_threads()

    return global_threads


def _check_thread_state(service_type, service_name, thread):
    """
    Check service thread state.

    As per asynchronous nature of working with threads [and asyncio] wait till
    the thread is started, till the SfcGlobals data are updated and then insert
    the thread info.

    :param service_type: service type (SF or SFF)
    :type service_type: str
    :param service_name: service name
    :type service_name: str
    :param thread: service thread
    :type thread: `class:threading.Thread`

    """
    global_threads = _get_global_threads(service_type)

    while not thread.is_alive():
        sleep(0.1)

    while service_name not in global_threads:
        sleep(0.1)

    while not global_threads[service_name]:
        sleep(0.1)

    global_threads[service_name]['thread'] = thread


def start_server(loop, addr, udpserver):
    """
    TODO: add docstring

    :param loop:
    :type loop: `:class:asyncio.unix_events._UnixSelectorEventLoop`
    :param addr: IP address and port
    :type addr: tuple (str, int)
    :param udpserver: UDP server instance
    :type udpserver: `:class:ControlUdpServer`

    :return `:class:asyncio.selector_events._SelectorDatagramTransport`

    """
    listen = loop.create_datagram_endpoint(lambda: udpserver, local_addr=addr)
    transport = loop.run_until_complete(listen)[0]

    return transport


def start_service(service_name, service_ip, service_port, service_type):
    """
    Start a service and register it with SfcGlobals.

    Start both the target service and its control UPD server.

    NOTE: loop.run_forever() blocs!

    :param service_name: service name
    :type service_name: str
    :param service_ip: service IP address
    :type service_ip: str
    :param service_port: service port
    :type service_port: int
    :param service_type: service type
    :type service_type: str

    """
    service_threads = _get_global_threads(service_type)
    service_threads[service_name] = {}

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    service_class = find_service(service_type)
    service = service_class(loop)
    service.set_name(service_name)

    logger.info('Starting %s serving as %s at %s:%s',
                service_type.upper(), service_name, service_ip, service_port)

    udpserver_transport = start_server(loop,
                                       (service_ip, service_port),
                                       service)

    control_port = sfc_globals.get_data_plane_control_port()
    sfc_globals.set_data_plane_control_port(control_port + 1)

    udpserver_socket = udpserver_transport.get_extra_info('socket')

    service_threads[service_name]['socket'] = udpserver_socket
    service_threads[service_name][DPCP] = control_port

    logger.info('Starting control UDP server for %s at %s:%s',
                service_name, service_ip, control_port)

    control_udp_server = find_service(CUDP)(loop)
    start_server(loop,
                 (service_ip, control_port),
                 control_udp_server)

    loop.run_forever()
    udpserver_socket.close()
    loop.close()


def stop_service(service_type, service_name):
    """
    Stop a service.

    Stop a service by sending an ending message to its control UDP server.
    This will result in exiting the thread in which both are running.

    :param service_type: service type (SF or SFF)
    :type service_type: str
    :param service_name: what should be stopped - SF or SFF name
    :type service_name: str

    """
    service_threads = _get_global_threads(service_type)

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    # TODO: at this point it doesn't matter what the message body is ...
    sock.sendto(b"Kill thread",
                (SFF_UDP_IP, service_threads[service_name][DPCP]))

    thread = service_threads[service_name]['thread']
    while thread.is_alive():
        sleep(0.1)

    sock = service_threads[service_name]['socket']
    if not sock._closed:
        sock.close()

    service_threads.pop(service_name, None)


def start_sf(sf_name, sf_ip, sf_port, sf_type):
    """
    Start a Service Function.

    Stop an already running SF if it has the same name as the SF which is
    about to start. Then start the new one.

    """
    logger.info('Starting Service Function: %s', sf_name)

    sf_threads = _get_global_threads(sf_type)
    if sf_name in sf_threads:
        stop_service(sf_type, sf_name)

    sf_thread = Thread(target=start_service,
                       args=(sf_name, sf_ip, sf_port, sf_type))

    sf_thread.start()
    _check_thread_state(SF, sf_name, sf_thread)


def stop_sf(sf_name):
    """
    Stop a Service Function.
    """
    logger.info("Stopping Service Function: %s", sf_name)
    stop_service(SF, sf_name)


def start_sff(sff_name, sff_ip, sff_port):
    """
    Start a Service Function Forwarder.

    Stop an already running SFF if it has the same name as the SFF which is
    about to start. Then start the new one.

    """
    logger.info('Starting Service Function Forwarder: %s', sff_name)

    sff_threads = _get_global_threads(SFF)
    if sff_name in sff_threads:
        stop_service(SFF, sff_name)

    sff_thread = Thread(target=start_service,
                        args=(sff_name, sff_ip, sff_port, SFF))

    sff_thread.start()
    _check_thread_state(SFF, sff_name, sff_thread)


def stop_sff(sff_name):
    """
    Stop a Service Function Forwarder.
    """
    logger.info("Stopping Service Function Forwarder: %s", sff_name)
    stop_service(SFF, sff_name)


# This does not work in MacOS when SFF/SF are different python
# applications on the same machine
# def start_server(loop, addr, service, myip):
#     t = asyncio.Task(loop.create_datagram_endpoint(
#         service, local_addr=(myip, 57444), remote_addr=addr))
#     loop.run_until_complete(t)
#     print('Listening for packet on:', addr)

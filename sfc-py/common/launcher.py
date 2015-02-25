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
Manage service [and its' associated thread] starting and stopping
"""


import socket
import logging
import asyncio

from time import sleep
from threading import Thread
from common.sfc_globals import sfc_globals
from common.services import SF, SFF, CUDP, find_service

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

#: constants
# WE assume agent and thread and co-located
SFF_UDP_IP = "127.0.0.1"
DPCP = 'data_plane_control_port'


def _get_global_threads(service_type):
    """
    Get the global service threads dictionary

    :param service_type: service type (SF or SFF)
    :type service_type: str

    :return dict

    """
    if service_type == SF:
        global_threads = sfc_globals.get_sf_threads()
    elif service_type == SFF:
        global_threads = sfc_globals.get_sff_threads()

    return global_threads


def _check_thread_state(service_type, service_name, thread):
    """
    Check thread state and conduct appropriate action and log.

    As per asynchronous nature of working with threads [and asyncio] wait max.
    10 seconds till the thread is started, raise an exception otherwise.
    Wait till the global service (SF or SFF) threads dictionary is updated,
    then insert the thread info.

    :param service_type: service type (SF or SFF)
    :type service_type: str
    :param service_name: service name
    :type service_name: str
    :param thread: service thread
    :type thread: `class:threading.Thread`

    """
    timeout = 0
    while not thread.is_alive():
        sleep(0.1)

        timeout += 0.1
        if timeout > 10:
            global_threads = _get_global_threads(service_type)
            global_threads.pop(service_name, None)

            msg = "Failed to start thread %s" % service_name
            logger.error(msg)
            raise TimeoutError(msg)

    global_threads = _get_global_threads(service_type)
    while service_name not in global_threads:
        sleep(0.1)

        global_threads = _get_global_threads(service_type)

    while not global_threads[service_name]:
        sleep(0.1)

        global_threads = _get_global_threads(service_type)

    logger.info("Thread %s started successfully", service_name)
    global_threads[service_name]['thread'] = thread


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

    listen = loop.create_datagram_endpoint(lambda: udpserver, local_addr=addr)
    transport = loop.run_until_complete(listen)[0]

    if not message.endswith('...'):
        message += ' ' + str(addr[1])

    logger.info(message)

    return transport


def start_service(service_name, service_ip, service_port, service_type,
                  service_threads, service_message):
    """
    Start a service.

    :param service_name: service name
    :type service_name: str
    :param service_ip: service IP address
    :type service_ip: str
    :param service_port: service port
    :type service_port: int
    :param service_type: service type
    :type service_type: str
    :param service_threads: locally running service threads
    :type service_threads: dict
    :param service_message: what will be logged upon a service start
    :type service_message: str

    """
    logger.info('Starting %s service ...', service_type.upper())
    service_threads[service_name] = {}

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)

    service_class = find_service(service_type)
    service = service_class(loop)
    service.set_name(service_name)

    udpserver_transport = start_server(loop,
                                       (service_ip, service_port),
                                       service,
                                       service_message)

    control_port = sfc_globals.get_data_plane_control_port()
    sfc_globals.set_data_plane_control_port(control_port + 1)

    udpserver_socket = udpserver_transport.get_extra_info('socket')

    service_threads[service_name]['socket'] = udpserver_socket
    service_threads[service_name][DPCP] = control_port

    control_udp_server = find_service(CUDP)(loop)
    start_server(loop,
                 (service_ip, control_port),
                 control_udp_server,
                 'Listening for Control messages on port:')

    loop.run_forever()
    udpserver_socket.close()
    loop.close()


def stop_service(service_type, service_name):
    """
    Stop service thread.

    :param service_type: service type (SF or SFF)
    :type service_type: str
    :param service_name: what should be stopped - SF or SFF name
    :type service_name: str

    """
    # Yes, we will come up with a better protocol in the future ...
    message = "Kill thread".encode(encoding="UTF-8")

    global_threads = _get_global_threads(service_type)

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(message,
                (SFF_UDP_IP,
                 global_threads[service_name][DPCP]))

    thread = global_threads[service_name]['thread']
    if thread.is_alive():
        thread.join()

    if not thread.is_alive():
        logger.info('Service %s stopped', service_name)
        # We need to close the socket used by thread here as well or we get an
        # address reuse error. This is probably some bug in asyncio since it
        # should be enough for the SFF thread to close the socket.
        global_threads[service_name]['socket'].close()
        global_threads.pop(service_name, None)


def start_sf(sf_name, sf_ip, sf_port, sf_type):
    """
    Start a Service Function thread

    :return `class:threading.Thread`

    """
    logger.info('Starting Service Function: %s', sf_name)

    local_sf_threads = _get_global_threads(SF)
    sf_thread = Thread(target=start_service,
                       args=(sf_name, sf_ip, sf_port, sf_type,
                             local_sf_threads,
                             'Starting new Service Function ...'))

    sf_thread.start()
    _check_thread_state(SF, sf_name, sf_thread)

    return sf_thread


def stop_sf(sf_name):
    """
    Stop a Service Function thread
    """
    logger.info("Stopping Service Function: %s", sf_name)
    stop_service(SF, sf_name)


def start_sff(sff_name, sff_ip, sff_port):
    """
    Start a Service Function Forwarder thread

    :return `class:threading.Thread`

    """
    logger.info('Starting Service Function Forwarder: %s', sff_name)

    local_sff_threads = _get_global_threads(SFF)
    sff_thread = Thread(target=start_service,
                        args=(sff_name, sff_ip, sff_port, SFF,
                              local_sff_threads,
                              'Listening for NSH packets on port:'))

    sff_thread.start()
    _check_thread_state(SFF, sff_name, sff_thread)

    return sff_thread


def stop_sff(sff_name):
    """
    Stop a Service Function Forwarder thread
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

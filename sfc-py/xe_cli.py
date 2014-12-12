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

"""XE CLI processing module"""

import os
import paramiko


def process_received_service_path(spi, rsp):

    service_hops = []  # create list to hold service_hop entries extracted from SFP
    for key in rsp:
        ip_address = rsp[key]['ip']  # extract and store each IP address in the RSP
        service_hops.append('sff ' + ip_address)  # append each IP address in the RSP to the service hop list

    service_hops[::-1]  # reverse the order of the service_hops to get right XE syntax

    number_service_hops = len(service_hops)  # determine how many service hops in the service path
    number_of_nulls = 4 - number_service_hops  # determine how many 'nulls' to put into the XE cli syntax
    xe_cli = 'service-insertion service-path ' + str(spi)  # start the cli syntax adding service path id

    for i in range(number_of_nulls):
        xe_cli += ' null'  # enter the necessary number of 'nulls' into the xe_cli variable

    for i in range(number_service_hops):
        xe_cli += ' ' + service_hops[number_service_hops-1]  # enter service hop address into xe_cli variable
        number_service_hops -= 1  # decrement the counter

    print('\nCLI to be entered is: \n', xe_cli)
    return xe_cli


def send_command_and_wait_for_execution(channel, command, wait_string, should_print):
    # Send the su command
    channel.send(command)

    # Create a new receive buffer
    receive_buffer = ''

    while wait_string not in receive_buffer:
        # Flush the receive buffer
        receive_buffer = str(channel.recv(10000))

    # Print the receive buffer, if necessary
    if should_print:
        print(receive_buffer)

    return


def ssh_execute_cli(cli, sff_locator):
    remoteConnectionSetup = paramiko.SSHClient()
    remoteConnectionSetup.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    #  connect to the router - using a hardcoded IP address for now
    remoteConnectionSetup.connect(sff_locator, username='cisco', password='cisco', allow_agent=False, look_for_keys=False)

    sshChannel = remoteConnectionSetup.invoke_shell()  # invoke the shell so can send multiple commands

    # make sure in enable mode so we can configure the router
    send_command_and_wait_for_execution(sshChannel, "enable\n", "Password:", False)
    send_command_and_wait_for_execution(sshChannel, "cisco\n", "#", False)
    send_command_and_wait_for_execution(sshChannel, "terminal length 0\n", "#", False)
    send_command_and_wait_for_execution(sshChannel, "conf t\n", "#", False)
    send_command_and_wait_for_execution(sshChannel, cli + '\n', "#", False)
    remoteConnectionSetup.close()  # close the connection


def process_xe_cli(data_plane_path, my_sff_name, sfc_topo):

    sff_locator = sfc_topo[my_sff_name]['sff-data-plane-locator'][0]['data-plane-locator']['ip']

    for key in data_plane_path:
        spi = key  # store the SPI value
        rsp = data_plane_path[key]  # store the rendered service path
        cli_to_push = process_received_service_path(spi, rsp)  # process the cli
        ssh_execute_cli(cli_to_push, sff_locator)  # send cli to configure XE router
    return

#{254: {'port': 6633, 'ip': '2.2.2.2'}, 255: {'port': 6633, 'ip': '10.1.1.1'}}
# service-insertion service-path 3 null null null sf 21.0.0.24

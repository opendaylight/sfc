

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


def process_xe_cli(data_plane_path):
    print('\nXE module received data plane path: \n', data_plane_path)

    for key in data_plane_path:
        spi = key  # store the SPI value
        rsp = data_plane_path[key]  # store the rendered service path
        process_received_service_path(spi, rsp)  # process the cli
    return

#{254: {'port': 6633, 'ip': '2.2.2.2'}, 255: {'port': 6633, 'ip': '10.1.1.1'}}
# service-insertion service-path 3 null null null sf 21.0.0.24
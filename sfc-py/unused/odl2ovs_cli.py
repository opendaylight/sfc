#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import subprocess


__author__ = "Paul Quinn, Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "paulq@cisco.com, rapenno@gmail.com"
__status__ = "alpha"


def cli_local(sfdpinfo, vxlan, key):
    sfi = 0
    if len(sfdpinfo) > 1:
        for sf in sfdpinfo:
            part1 = 'ovs-vsctl add-port br-int ' + 'vxlan' + str(vxlan) + ' -- set interface vxlan' + str(vxlan) + \
                    ' options:dst_port=6633 type=vxlan options:remote_ip=' + sfdpinfo[sfi]['locator']
            part2 = ' options:key=' + str(key) + ' options:nsp=' + str(sfdpinfo[sfi]['pid']) + ' options:nsi=' + \
                    str(sfdpinfo[sfi]['index'])
            cli = part1 + part2
            print cli
            # subprocess.call([cli], shell=True)
            vxlan += 1
            sfi += 1
    else:
        print "No locally attached services on SFF"
    return vxlan


def ovs_cli_local(sfdpinfo, vxlan, key):
    sfi = 0
    if len(sfdpinfo) > 1:
        for sf in sfdpinfo:
            part1 = 'ovs-vsctl add-port br-int ' + 'vxlan' + str(vxlan) + '-- set interface vxlan' + str(vxlan) + \
                    ' options:dst_port=6633 type=vxlan options:remote_ip=' + sfdpinfo[sfi]['locator']
            part2 = ' options:key=' + str(key) + ' options:nsp=' + str(sfdpinfo[sfi]['pid']) + ' options:nsi=' + \
                    str(sfdpinfo[sfi]['index'])
            cli = part1 + part2
            subprocess.call([cli], shell=True)
            vxlan += 1
            sfi += 1
            print cli
    else:
        print "No locally attached services on SFF"
    return vxlan


def cli_nextsff(nextsffloc, nextsff, key, vxlan, pid):
    part1 = 'ovs-vsctl add-port br-tun ' + 'vxlan' + str(vxlan) + ' -- set interface vxlan' + str(vxlan) + \
            ' options:dst_port=6633 type=vxlan options:remote_ip=' + nextsffloc
    part2 = ' options:key=' + str(key) + ' options:nsp=' + str(pid) + ' options:nsi=' + \
            str(nextsff['sff-index'])
    cli = part1 + part2
    print cli
    # subprocess.call([cli], shell=True)
    vxlan += 1
    return


def ovs_cli_nextsff(nextsffloc, nextsff, key, vxlan, path):
    part1 = 'ovs-vsctl add-port br-tun ' + 'vxlan' + str(vxlan) + ' -- set interface vxlan' + str(vxlan) + \
            ' options:dst_port=6633 type=vxlan options:remote_ip=' + nextsffloc
    part2 = ' options:key=' + str(key) + ' options:nsp=' + str(path) + ' options:nsi=' + str(nextsff['sff-index'])
    cli = part1 + part2

    print cli
    subprocess.call([cli], shell=True)

    vxlan += 1
    return

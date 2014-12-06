__author__ = "Paul Quinn, Reinaldo Penno"
__copyright__ = "Copyright(c) 2014, Cisco Systems, Inc."
__version__ = "0.2"
__email__ = "paulq@cisco.com, rapenno@gmail.com"
__status__ = "alpha"


#
# Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import subprocess

ofclidict= {}
ofgtable = 0


def setupvxlan(key):

    tunpart1 = 'ovs-vsctl add-port br-s vxlan-s -- set interface vxlan-s type=vxlan options:dst_port=6633 options:remote_ip=flow'
    tunpart2 = ' options:key=' + str(key) + ' options:nsp=flow options:nsi=flow'
    tuncli = tunpart1 + tunpart2
    print tuncli

def cli_local(sfdpinfo, vxlan, key):
    global ofgtable
    global ofclidict
    # build table = 0 rules
    # there isn't an OF rule for this pathID yet
    for nsp in sfdpinfo:
        if nsp['pid'] not in ofclidict:
            ofgtable = ofgtable + 1
            ofclidict[nsp['pid']] = [{'table': ofgtable}]
            ofcli = 'ovs-ofctl add-flow br-s table=0, priority=200, tun_id=0x' + key + ',nsp=0x' + str(nsp['pid']) + ',actions=resubmit(,' + str(ofclidict[nsp['pid']][0]['table']) + ')'
            print ofcli
    # pathID already has table = 0 rule
    else:
        print "IN TABLE"

    # go through the locally connected services and create OF rules
    if len(sfdpinfo) > 1:
        for sf in sfdpinfo:
            ofcli = 'ovs-ofctl add-flow br-s table=' + str(ofclidict[sf['pid']][0]['table']) + ', priority=200, tun_id=0x' + key + ',nsp=0x' + str(sf['pid']) + \
                    ',nsi=0x' + str(sf['index']) + ',actions=set_field:' + sf['locator'] + '-\>tun_dst,in_port'
            print ofcli
#            #subprocess.call([cli], shell=True)
             #sfi += 1
    else:
        print "No locally attached services on SFF"

    #return vxlan


def ovs_cli_local(sfdpinfo, vxlan, key):
    global ofgtable
    global ofclidict
    # build table = 0 rules
    # there isn't an OF rule for this pathID yet
    for nsp in sfdpinfo:
        if nsp['pid'] not in ofclidict:
            ofgtable = ofgtable + 1
            ofclidict[nsp['pid']] = [{'table': ofgtable}]
            ofcli = 'ovs-ofctl add-flow br-s table=0, priority=200, tun_id=' + key + ',nsp=0x' + str(nsp['pid']) + ',actions=resubmit(,' + str(ofclidict[nsp['pid']][0]['table']) + ')'
            print ofcli
            subprocess.call([ofcli], shell=True)
    # pathID already has table = 0 rule
    else:
        print "IN TABLE"

    # go through the locally connected services and create OF rules
    if len(sfdpinfo) > 1:
        for sf in sfdpinfo:
            ofcli = 'ovs-ofctl add-flow br-s table=' + str(ofclidict[sf['pid']][0]['table']) + ', priority=200, tun_id=0x' + key + ',nsp=0x' + str(sf['pid']) + \
                    ',nsi=0x' + str(sf['index']) + ',actions=set_field:' + sf['locator'] + '-\>tun_dst,in_port'
            print ofcli
            subprocess.call([ofcli], shell=True)

    else:
        print "No locally attached services on SFF"

    #return vxlan


def cli_nextsff(nextsffloc, nextsff, key, vxlan, pid):
    global ofgtable
    if pid not in ofclidict:
        ofgtable = ofgtable + 1
        ofclidict[sfdpinfo[sfi]['pid']] = [{'table': ofgtable}]
        ofcli = 'ovs-ofctl add-flow br-s table=0, priority=200, tun_id=' + key + ',nsp=0x' + str(pid) + ',actions=resubmit(,' + str(ofclidict[pid][0]['table']) + ')'
        print ofcli

    ofcli = 'ovs-ofctl add-flow br-s table=' + str(ofclidict[pid][0]['table']) + ', priority=200, tun_id=0x' + key + ',nsp=0x' + str(pid) + ',nsi=0x' + str(nextsff['sff-index']) + ',actions=set_field:' + nextsffloc + '-\>tun_dst,in_port'
    print ofcli
    return


def ovs_cli_nextsff(nextsffloc, nextsff, key, vxlan, path):
    global ofgtable
    if pid not in ofclidict:
        ofgtable = ofgtable + 1
        ofclidict[sfdpinfo[sfi]['pid']] = [{'table': ofgtable}]
        ofcli = 'ovs-ofctl add-flow br-s table=0, priority=200, tun_id=0x' + key + ',nsp=0x' + str(pid) + ',actions=resubmit(,' + str(ofclidict[pid][0]['table']) + ')'
        print ofcli
        subprocess.call([ofcli], shell=True)

    ofcli = 'ovs-ofctl add-flow br-s table=' + str(ofclidict[pid][0]['table']) + ', priority=200, tun_id=0x' + key + ',nsp=0x' + str(pid) + ',nsi=0x' + str(nextsff['sff-index']) + ',actions=set_field:' + nextsffloc + '-\>tun_dst,in_port'
    print ofcli
    subprocess.call([ofcli], shell=True)


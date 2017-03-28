#! /usr/bin/env python
from scapy.all import *
import fcntl, socket, struct
import time


print 'starting sf ...'
tag = sys.argv[1]
print 'tag = ' + tag
logfile = open("/tmp/packets.log", "w")

def get_mac(iface='gw-eth0'):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    info = fcntl.ioctl(s.fileno(), 0x8927,  struct.pack('256s', iface[:15]))
    return ':'.join(['%02x' % ord(char) for char in info[18:24]])

mac = get_mac() #"00:00:00:00:00:ee" #
print 'mac = ' + mac
def ip_incoming(pkt):
    return pkt.src != mac and pkt.dst == mac and 'IP' in pkt and ('ICMP' in pkt or 'UDP' in pkt or 'TCP' in pkt)
def ip_callback(pkt):
    print "sniffado"
    pkt.dst = None
    pkt.src = get_mac()
    #time.sleep(1)
    #pkt.show()
    #send(pkt,iface="gw-eth0")
    send(pkt['IP'])

sniff(lfilter=ip_incoming, store=0, iface="gw-eth0", prn=ip_callback)


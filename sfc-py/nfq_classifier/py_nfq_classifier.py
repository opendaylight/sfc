from netfilterqueue import NetfilterQueue
from nsh_encode import *
import subprocess
import logging
import socket

logger = logging.getLogger('sfc.' + __name__)

# hint: to see the rules execute: 'iptables -S -t raw'
	
# common command executor	
def execute_cli(cli):
	logger.debug("execute_cli: %s", cli)
	subprocess.call([cli], shell=True)
	return
	
	
class NfqClassifierManager:
	
	# we use mark that will be equal to path_id
	def __init__(self):
		self.nfqueue = NetfilterQueue()
		self.clear_all_rules()
		return
		
		
	def __del__(self):
		self.clear_all_rules()
		# NetfilterQueue will destroy itself properly automatically
		return
		
	#helper
	def clear_all_rules(self):
		logger.info("clear_all_rules: Reset iptables rules.")
		# init map
		self.path_id_2_pfw_map = {}
		# clear all previous rules/sub-chains in 'raw' table
		cli = "iptables -t raw -F"
		execute_cli(cli)
		cli = "iptables -t raw -X"
		execute_cli(cli)		
		return	
	
	#helper
	def get_sub_chain_name(self, path_id):
		return "sfp-nfq-" + str(path_id)


	#mock method
	def get_path_id(self, path_name):
		if path_name == 'path-1':
			return 1
			
		if path_name == 'path-2':
			return 2
		
		return None
	
	#main callback for received packets
	def common_process_packet(self, packet):
		try:
			logger.debug("common_process_packet: received packet=%s, mark=%d", packet, packet.get_mark())
			
			mark = packet.get_mark()
			
			packet_forwarder = self.path_id_2_pfw_map[mark]
			
			#check
			if not packet_forwarder:
				logger.warn("common_process_packet: no packet forwarder for mark=%d, dropping the packet", mark)
				packet.drop()
				return
			
			packet_forwarder.process_packet(packet)
			return
		except Exception as e:
			logger.excetion('common_process_packet')
			print 
		
	def run(self):
		self.nfqueue.bind(1, self.common_process_packet)	
		self.nfqueue.run()
		return
		
	# compile_acl list
	def compile_acl(self, acl):
		logger.debug("compile_acl: acl=%s", acl)
		
		self.clear_all_rules()
		
		for acl_item in acl['access-lists']['access-list']:
			for ace in acl_item['access-list-entries']:
				self.compile_ace(ace)
		return
		
	# process_ace
	def compile_ace(self, ace):
		logger.debug("compile_ace: ace=%s", ace)
		
		path_name = ace['actions']['service-function-acl:service-function-path']
		path_id = self.get_path_id(path_name)
		
		if not path_id:
			logger.error("compile_ace: path_id not found for path_name=%s", path_name)
			return
			
		logger.debug("compile_ace: found path_id=%d", path_id)			
		
		#check if already initialized
		if not path_id in self.path_id_2_pfw_map:
			self.init_new_packet_forwarder(path_id)
			
		packet_forwarder = self.path_id_2_pfw_map[path_id]			
			
		self.add_iptables_classification_rule(ace, path_id)
		return
		
	def get_packet_forwarder(self, path_id):
		try:
			return self.path_id_2_pfw_map[path_id]
		except:
			logger.warn('get_packet_forwarder: None for path_id=%d', path_id)
			return None
			
		
	def init_new_packet_forwarder(self, path_id):
		sub_chain_name = self.get_sub_chain_name(path_id)
		
		# create sub-chain for the path, this way we can in future easily remove the rules for particular path
		cli = "iptables -t raw -N " + sub_chain_name
		execute_cli(cli)
		# insert jump to sub-chain
		cli = "iptables -t raw -I PREROUTING -j " + sub_chain_name
		execute_cli(cli)
		# add jump to queue 1 in case of match mark (ACL matching rules will have to be inserted before this one)
		cli = "iptables -t raw -A " + sub_chain_name + " -m mark --mark "  + str(path_id) + " -j NFQUEUE --queue-num 1"
		execute_cli(cli)		
		
		packet_forwarder = PacketForwarder(path_id)
		self.path_id_2_pfw_map[path_id] = packet_forwarder
		return
		
		
	# create iptables matches for sending packets to NFQ of given number
	def add_iptables_classification_rule(self, ace, path_id):
		assert ace
		assert path_id
		
		ace_matches = ace['matches']
		
		#dl_src
		dl_src = ''
		if ('source-mac-address' in ace_matches):
			dl_src = '-m mac --mac-source' + ace_matches['source-mac-address']
			if ('source-mac-address-mask' in ace_matches):
				print 'source-mac-address-mask match not implemented'

		#dl_dst	
		dl_dst = ''
		if ('destination-mac-address' in ace_matches):
			print 'destination-mac-address match not implemented'
		
		#nw_src/ipv6_src
		nw_src = ''
		if ('source-ipv4-address' in ace_matches):
			nw_src = ' -s ' + ace_matches['source-ipv4-address']
			
		ipv6_src = ''
		if ('source-ipv6-address' in ace_matches):
			ipv6_src = ' -s ' + ace_matches['source-ipv6-address']	
			
		#nw_dst/ipv6_dst		
		nw_dst = ''
		if ('destination-ipv4-address' in ace_matches):
			nw_dst = ' -d ' + ace_matches['destination-ipv4-address']
			
		ipv6_dst = ''
		if ('destination-ipv6-address' in ace_matches):
			ipv6_dst = ' -d ' + ace_matches['destination-ipv6-address']				

		# nw_proto --- TCP/UDP ....
		nw_proto = ''
		if ('ip-protocol' in ace_matches):
			if ace_matches['ip-protocol'] == 7:
				nw_proto = ' -p tcp'
			elif ace_matches['ip-protocol'] == 17:
				nw_proto = ' -p udp'
			else:
				print 'unknown ip-protocol'		
		
		# only lower transport port dst/src supported !!!! 
		tp_dst = ''
		if ('destination-port-range' in ace_matches):
			if ('lower-port' in ace_matches['destination-port-range']):
				if nw_proto == '':
					logger.error("add_iptables_classification_rule: processing 'destination-port-range'. ip-protocol must be specified")
					return
				tp_dst = ' --dport ' + str(ace_matches['destination-port-range']['lower-port'])
			
		tp_src = ''
		if ('source-port-range' in ace_matches):
			if ('lower-port' in ace_matches['source-port-range']):
				if nw_proto == '':
					logger.error("add_iptables_classification_rule: processing 'source-port-range'. ip-protocol must be specified")
					return
				tp_src = ' --sport ' + str(ace_matches['source-port-range']['lower-port'])	

		sub_chain_name = self.get_sub_chain_name(path_id)
		
		# !!!insert mark rule before the jump to queue rule!!!
		cli = "iptables -t raw -I " + sub_chain_name
		cli += nw_src + nw_dst + ipv6_src + ipv6_dst + nw_proto + tp_src + tp_dst
		cli += " -j MARK --set-mark " + str(path_id)
		
		execute_cli(cli)
		return

	# destroy PacketForwader with iptables rules and chains
	def destroy_packet_forwarder(self, path_id):
		# check
		assert path_id
		
		del self.path_id_2_pfw_map[path_id]
		
		sub_chain_name = get_sub_chain_name(path_id)

		# delete the jump to sub-chain
		cli = "iptables -t raw -D PREROUTING -j " + sub_chain_name
		execute_cli(cli)
		# delete rules in sub-chain
		cli = "iptables -t raw -F  " + sub_chain_name
		execute_cli(cli)	
		# delete sub-chain
		cli = "iptables -t raw -X " + sub_chain_name
		execute_cli(cli)		
		return
	
	
class PacketForwarder:
	def __init__(self, path_id):
		self.path_id = path_id
		self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
		self._fw_params_set = False
		self._tun_params_set = False
		return
		
	def __str__(self):
		return "PacketForwarder: path_id="  + str(self.path_id)
		
	def update_forwarding_params(self, forwarding_params):
		self.sff_ip_addr = forwarding_params['sff_ip_addr']
		self.sff_port = forwarding_params['sff_port']
		self._fw_params_set = True
		return
	
	def update_tunnel_params(self, tunnel_params):
		self.vxlan_values = tunnel_params['vxlan_values']
		self.base_values = tunnel_params['base_values']
		self.ctx_values = tunnel_params['ctx_values']
		self._tun_params_set = True		
		return

	def process_packet(self, orig_packet):
 		# check
		if not self._fw_params_set:
			logger.error('process_packet: Forwarding params not set for path_id=%d', self.path_id)
			return
			
		if not self._tun_params_set:
			logger.error('process_packet: Tunnel params not set for path_id=%d', self.path_id)
			return			
		
		logger.debug('process_packet: Forwarding packet to %s:%d', self.sff_ip_addr, self.sff_port)
		
		orig_payload = orig_packet.get_payload()	
		vxlan_packet = build_packet(self.vxlan_values, self.base_values, self.ctx_values) + orig_payload		
		self.socket.sendto(vxlan_packet, (self.sff_ip_addr, self.sff_port))
		orig_packet.drop() # ! drop original packet
		return

# test this module
if __name__ == "__main__":
	#configure sfc logger as parent logger
	console_handler = logging.StreamHandler()
	console_handler.setLevel(logging.DEBUG)
	console_handler.setFormatter(logging.Formatter('%(levelname)s[%(name)s] %(message)s'))
	
	sfc_logger = logging.getLogger('sfc')
	sfc_logger.setLevel(logging.DEBUG)
	
	sfc_logger.addHandler(console_handler)
	
	# test input ACL
	acl = {'access-lists': {u'access-list': [{u'acl-name': u'acl-1', u'access-list-entries': [{u'matches': {u'destination-port-range': {u'lower-port': 13200}, u'ip-protocol': 17, u'absolute': {u'active': True}}, u'actions': {u'service-function-acl:service-function-path': u'path-1'}, u'rule-name': u'ace-1'}, {u'matches': {u'destination-port-range': {u'lower-port': 14300}, u'ip-protocol': 17, u'absolute': {u'active': True}}, u'actions': {u'service-function-acl:service-function-path': u'path-2'}, u'rule-name': u'ace-2'}]}]}}

	nfq_manager = NfqManager()
	nfq_manager.compile_acl(acl)
	nfq_manager.run()
 

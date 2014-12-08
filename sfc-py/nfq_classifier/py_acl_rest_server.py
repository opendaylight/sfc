from flask import *
import Queue
import threading
import logging
import json
import sys

from py_nfq_classifier import *
from py_nsh_header_defs import *

logger = logging.getLogger('sfc.'  + __name__)

app = Flask(__name__)

# global ref to manager
nfq_class_manager = None


# mocks
path_forwarding_params = {
	1: {
		"sff_ip_addr": "192.168.1.1",
		"sff_port": 6633
	},
	2: {
		"sff_ip_addr": "192.168.1.1",
		"sff_port": 6633
	}
}

path_tunnel_params = {
	1: {
		"vxlan_values": VXLANGPE(int('00000100', 2), 0, 0x894F, int('111111111111111111111111', 2), 64),
		"base_values": BASEHEADER(0x1, int('01000000', 2), 0x6, 0x1, 0x1, 0x000001, 0x4),
		"ctx_values": CONTEXTHEADER(0xffffffff, 0, 0xffffffff, 0)
	},
	2: {
		"vxlan_values": VXLANGPE(int('00000100', 2), 0, 0x894F, int('111111111111111111111111', 2), 64),
		"base_values": BASEHEADER(0x1, int('01000000', 2), 0x6, 0x1, 0x1, 0x000002, 0x5),
		"ctx_values": CONTEXTHEADER(0xeeeeeeee, 0, 0xeeeeeeee, 0)
	}
}

@app.route('/acl/', methods=['PUT'])
def put_acl():
	try:
		logger.debug("put_acl: request=%s", request)
		
		global nfq_class_manager
		
		if not request.json:
			abort(400)
		else:
			acl = {
				'access-lists': request.get_json()['access-lists']
			}		
		
		logger.debug("put_acl: acl=%s", acl)
		
		nfq_class_manager.compile_acl(acl)

		# set mock params for path 1
		packet_forwarder = nfq_class_manager.get_packet_forwarder(1)
		packet_forwarder.update_forwarding_params(path_forwarding_params[1])
		packet_forwarder.update_tunnel_params(path_tunnel_params[1])

		# set mock params for path 2
		packet_forwarder = nfq_class_manager.get_packet_forwarder(2)
		packet_forwarder.update_forwarding_params(path_forwarding_params[2])
		packet_forwarder.update_tunnel_params(path_tunnel_params[2])	
		return "Hello", 201
	except Exception as e:
		logger.exception("put_acl")
		raise


def start_nfq_class_manager():
	global nfq_class_manager
	nfq_class_manager = NfqClassifierManager()
	nfq_class_manager.run()
	return
	
def main(argv):
	t = threading.Thread(target=start_nfq_class_manager, args = ())
	t.daemon = True
	t.start()

	app.run(host="0.0.0.0") # run Flask
	return


if __name__ == "__main__":
	#configure sfc logger as parent logger
	console_handler = logging.StreamHandler()
	console_handler.setLevel(logging.DEBUG)
	console_handler.setFormatter(logging.Formatter('%(levelname)s[%(name)s] %(message)s'))
	
	sfc_logger = logging.getLogger('sfc')
	sfc_logger.setLevel(logging.DEBUG)
	
	sfc_logger.addHandler(console_handler)	
	
	main(sys.argv[1:])
#! /usr/bin/env python

import os
import time
import requests
import json
import argparse

PUT  = 'PUT'
GET  = 'GET'
POST = 'POST'

class Context(object):
    def __init__(self):
        self.rest_path_prefix = 'restInput'
        self.rest_path_sf   =  'RestConf-SFs-HttpPut.json'
        self.rest_path_sfg  =  'RestConf-SFGs-HttpPut.json'
        self.rest_path_sfc  =  'RestConf-SFCs-HttpPut.json'
        self.rest_path_sff  =  'RestConf-SFFs-HttpPut.json'
        self.rest_path_sfp  =  'RestConf-SFPs-HttpPut.json'
        self.rest_path_acl  =  'RestConf-ACLs-HttpPut.json'
        self.rest_path_rsp  =  'RestConf-RSP-HttpPost.json'

        self.rest_url_sf    =  'config/service-function:service-functions/'
        self.rest_url_sfg   =  'config/service-function-group:service-function-groups/'
        self.rest_url_sfc   =  'config/service-function-chain:service-function-chains/'
        self.rest_url_sff   =  'config/service-function-forwarder:service-function-forwarders/'
        self.rest_url_sfp   =  'config/service-function-path:service-function-paths/'
        self.rest_url_rsp   =  'operational/rendered-service-path:rendered-service-paths/'
        self.rest_url_rsp_rpc = 'operations/rendered-service-path:create-rendered-path'
        self.rest_url_acl   =  'config/ietf-acl:access-lists/'
        self.rest_url_nodes =  'operational/opendaylight-inventory:nodes/'

        self.http_headers   =  {'Content-Type' : 'application/json', 'Cache-Control' : 'no-cache'}
        self.http_server    =  'localhost'
        self.url_base       =  ''
        self.http_port      =  '8181'
        self.interractive   =  True
        self.user           =  'admin'
        self.pw             =  'admin'
        self.batch_sf       =  False
        self.batch_sfg      =  False
        self.batch_sfc      =  False
        self.batch_sff      =  False
        self.batch_sfp      =  False
        self.batch_acl      =  False
        self.batch_rsp      =  False
        self.batch_query    =  False
        self.batch_nodes    =  False


def get_cmd_line(context):
    opts = argparse.ArgumentParser()

    # Batch or Interractive mode
    opts.add_argument('--interractive', '-i',
                      dest='interractive',
                      action='store_true',
                      help='Interractive mode, default')
    opts.add_argument('--batch', '-b',
                      dest='batch',
                      action='store_true',
                      help='Batch mode, overrides interractive mode')

    # Where to send the messages
    opts.add_argument('--http-server', '-s',
                      default=context.http_server,
                      dest='http_server',
                      help='HTTP server address')
    opts.add_argument('--http-port',
                      default=context.http_port,
                      dest='http_port',
                      help='HTTP server port')

    # Batch mode, which message(s) to send
    opts.add_argument('--send-sf', '-1',
                      dest='send_sf',
                      action='store_true',
                      help='Send an SF REST JSON PUT message')
    opts.add_argument('--send-sfc', '-2',
                      dest='send_sfc',
                      action='store_true',
                      help='Send an SFC REST JSON PUT message')
    opts.add_argument('--send-sff', '-3',
                      dest='send_sff',
                      action='store_true',
                      help='Send an SFF REST JSON PUT message')
    opts.add_argument('--send-sfp', '-4',
                      dest='send_sfp',
                      action='store_true',
                      help='Send an SFP REST JSON PUT message')
    opts.add_argument('--send-acl', '-5',
                      dest='send_acl',
                      action='store_true',
                      help='Send an ACL REST JSON PUT message')
    opts.add_argument('--send-rsp', '-6',
                      dest='send_rsp',
                      action='store_true',
                      help='Send an RSP REST JSON POST RPC message')
    opts.add_argument('--send-all', '-7',
                      dest='send_all',
                      action='store_true',
                      help='Send all (SF, SFF, SFC, SFP, RSP, ACL) REST JSON messages')
    opts.add_argument('--query-nodes', '-8',
                      dest='query_nodes',
                      action='store_true',
                      help='Query all Nodes connected to ODL')
    opts.add_argument('--send-sfg', '-9',
                      dest='send_sfg',
                      action='store_true',
                      help='Send an SFG REST JSON PUT message')
    opts.add_argument('--query-sfc', '-q',
                      dest='query_sfc',
                      action='store_true',
                      help='Query all SFC objects')

    # Paths to the rest JSON files
    opts.add_argument('--rest-path-prefix', '-prefix',
                      default=context.rest_path_prefix,
                      dest='rest_path_prefix',
                      help='Path prefix where the REST JSON files are located')
    opts.add_argument('--rest-path-sf', '-n',
                      default=context.rest_path_sf,
                      dest='rest_path_sf',
                      help='Name of the SF REST JSON file, relative to configured prefix')
    opts.add_argument('--rest-path-sfg', '-g',
                      default=context.rest_path_sfg,
                      dest='rest_path_sfg',
                      help='Name of the SFG REST JSON file, relative to configured prefix')
    opts.add_argument('--rest-path-sfc', '-c',
                      default=context.rest_path_sfc,
                      dest='rest_path_sfc',
                      help='Name of the SFC REST JSON file, relative to configured prefix')
    opts.add_argument('--rest-path-sff', '-f',
                      default=context.rest_path_sff,
                      dest='rest_path_sff',
                      help='Name of the SFF REST JSON file, relative toconfigured  prefix')
    opts.add_argument('--rest-path-sfp', '-p',
                      default=context.rest_path_sfp,
                      dest='rest_path_sfp',
                      help='Name of the SFP REST JSON file, relative to configured prefix')
    opts.add_argument('--rest-path-rsp', '-r',
                      default=context.rest_path_rsp,
                      dest='rest_path_rsp',
                      help='Name of the RSP REST JSON file, relative to configured prefix')
    opts.add_argument('--rest-path-acl', '-a',
                      default=context.rest_path_acl,
                      dest='rest_path_acl',
                      help='Name of the ACL REST JSON file, relative to configured prefix')

    args = opts.parse_args()

    context.http_server    =  args.http_server
    context.http_port      =  args.http_port
    context.rest_path_sf   =  os.path.join(args.rest_path_prefix, args.rest_path_sf)
    context.rest_path_sfg  =  os.path.join(args.rest_path_prefix, args.rest_path_sfg)
    context.rest_path_sfc  =  os.path.join(args.rest_path_prefix, args.rest_path_sfc)
    context.rest_path_sff  =  os.path.join(args.rest_path_prefix, args.rest_path_sff)
    context.rest_path_sfp  =  os.path.join(args.rest_path_prefix, args.rest_path_sfp)
    context.rest_path_acl  =  os.path.join(args.rest_path_prefix, args.rest_path_acl)
    context.rest_path_rsp  =  os.path.join(args.rest_path_prefix, args.rest_path_rsp)
    context.url_base       =  'http://%s:%s/restconf/' % (context.http_server, context.http_port)

    if args.batch:
        context.interractive = False
        if args.send_all:
            context.batch_sf       =  True
            context.batch_sfg      =  True
            context.batch_sfc      =  True
            context.batch_sff      =  True
            context.batch_sfp      =  True
            context.batch_rsp      =  True
            context.batch_acl      =  True
        else:
            context.batch_sf       =  args.send_sf
            context.batch_sfg      =  args.send_sfg
            context.batch_sfc      =  args.send_sfc
            context.batch_sff      =  args.send_sff
            context.batch_sfp      =  args.send_sfp
            context.batch_rsp      =  args.send_rsp
            context.batch_acl      =  args.send_acl
            context.batch_query    =  args.query_sfc
            context.batch_nodes    =  args.query_nodes

    # TODO we may not want to do this if the user only wants to send one message
    # Check that each of the files exists
    for path in [context.rest_path_sf, context.rest_path_sfc, context.rest_path_sff, context.rest_path_sfp]:
        if not os.path.exists(path):
            print 'ERROR REST JSON file does not exist: %s' % (path)
            return False

    for path in [context.rest_path_sf, context.rest_path_sfc, context.rest_path_sff, context.rest_path_sfp]:
        print '\tUsing REST file: %s' % path

    return True


def send_rest(context, operation, rest_url, rest_file=None):
    complete_url = '%s%s' % (context.url_base, rest_url)

    if rest_file:
        if not os.path.exists(rest_file):
            print 'REST file [%s] does not exists' % rest_file
            return

    try:
        if operation == GET:
            r = requests.get(url = complete_url,
                             headers = context.http_headers,
                             auth=(context.user, context.pw))

            print '\nHTTP GET %s\nresult: %s' % (rest_url, r.status_code)
            #if len(r.text) > 1:
            if r.status_code >= 200 and r.status_code <= 299:
                print json.dumps(json.loads(r.text), indent=4, separators=(',', ': '))

        elif operation == PUT:
            if not rest_file:
                print 'ERROR trying to PUT with empty REST file'
                return

            r = requests.put(url = complete_url,
                             auth=(context.user, context.pw),
                             data = json.dumps(json.load(open(rest_file, 'r'))),
                             headers = context.http_headers)
            print '\nHTTP PUT %s\nresult: %s' % (rest_url, r.status_code)

        elif operation == POST:
            if not rest_file:
                print 'ERROR trying to POST with empty REST file'
                return

            r = requests.post(url = complete_url,
                              auth=(context.user, context.pw),
                              data = json.dumps(json.load(open(rest_file, 'r'))),
                              headers = context.http_headers)
            print '\nHTTP POST %s\nresult: %s' % (rest_url, r.status_code)

        else:
            print 'ERROR: Invalid Operation: %s' % (operation)

    except requests.exceptions.ConnectionError as e:
        print 'ERROR connecting: %s' % (e)
    except:
        print 'ERROR unkown exception raised'


def batch(context):
    # The order of these if's is important
    # If send-all was set, then each of these needs to be sent, in order
    if context.batch_sf:
        send_rest(context, PUT, context.rest_url_sf,  context.rest_path_sf)
    if context.batch_sfg:
        send_rest(context, PUT, context.rest_url_sfg,  context.rest_path_sfg)
    if context.batch_sff:
        send_rest(context, PUT, context.rest_url_sff, context.rest_path_sff)
    if context.batch_sfc:
        send_rest(context, PUT, context.rest_url_sfc, context.rest_path_sfc)
    if context.batch_sfp:
        send_rest(context, PUT, context.rest_url_sfp, context.rest_path_sfp)
    if context.batch_rsp:
        send_rest(context, POST, context.rest_url_rsp_rpc, context.rest_path_rsp)
    if context.batch_acl:
        send_rest(context, PUT, context.rest_url_acl, context.rest_path_acl)

    if context.batch_query:
        send_rest(context, GET, context.rest_url_sf)
        send_rest(context, GET, context.rest_url_sfg)
        send_rest(context, GET, context.rest_url_sff)
        send_rest(context, GET, context.rest_url_sfc)
        send_rest(context, GET, context.rest_url_sfp)
        send_rest(context, GET, context.rest_url_rsp)
        send_rest(context, GET, context.rest_url_acl)
    elif context.batch_nodes:
        send_rest(context, GET, context.rest_url_nodes)


def CLI(context):
    option = '1'
    while option != '0':
        print '\n\nChoose Option to perform:'
        print '0) Quit'
        print '1) Send SF REST'
        print '2) Send SFC REST'
        print '3) Send SFF REST'
        print '4) Send SFP REST'
        print '5) Send RSP REST'
        print '6) Send ACL REST'
        print '7) Send all ordered: (SF, SFF, SFC, SFP, RSP, ACL)'
        print '8) Query all: (SF, SFF, SFC, SFP, RSP, ACL)'
        print '9) Query Nodes'
        print '10) Send SFG REST'

        option = raw_input('=> ')

        if option == '1':
            send_rest(context, PUT, context.rest_url_sf,  context.rest_path_sf)
        elif option == '2':
            send_rest(context, PUT, context.rest_url_sfc, context.rest_path_sfc)
        elif option == '3':
            send_rest(context, PUT, context.rest_url_sff, context.rest_path_sff)
        elif option == '4':
            send_rest(context, PUT, context.rest_url_sfp, context.rest_path_sfp)
        elif option == '5':
            send_rest(context, POST, context.rest_url_rsp_rpc, context.rest_path_rsp)
        elif option == '6':
            send_rest(context, PUT, context.rest_url_acl, context.rest_path_acl)
        elif option == '7':
            send_rest(context, PUT, context.rest_url_sf,  context.rest_path_sf)
            send_rest(context, PUT, context.rest_url_sff, context.rest_path_sff)
            send_rest(context, PUT, context.rest_url_sfc, context.rest_path_sfc)
            send_rest(context, PUT, context.rest_url_sfp, context.rest_path_sfp)
            time.sleep(1);
            send_rest(context, POST, context.rest_url_rsp_rpc, context.rest_path_rsp)
            # Need to wait until the SFC creates the RSP internally before sending the ACL
            print 'Sleeping 2 seconds while RSP being created'
            time.sleep(2);
            send_rest(context, PUT, context.rest_url_acl, context.rest_path_acl)
        elif option == '8':
            send_rest(context, GET, context.rest_url_sf)
            send_rest(context, GET, context.rest_url_sff)
            send_rest(context, GET, context.rest_url_sfc)
            send_rest(context, GET, context.rest_url_sfp)
            send_rest(context, GET, context.rest_url_rsp)
            send_rest(context, GET, context.rest_url_acl)
        elif option == '9':
            send_rest(context, GET, context.rest_url_nodes)
        elif option == '10':
            send_rest(context, PUT, context.rest_url_sfg, context.rest_url_sfg)
        elif option != '0':
            print 'ERROR: Invalid option %s' % (option)


def main():
    context = Context()
    if not get_cmd_line(context):
        return

    if context.interractive:
        CLI(context)
    else:
        batch(context)

if __name__ == '__main__':
    main()


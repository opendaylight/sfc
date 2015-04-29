#
# Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html


import os
import pytest
import socket
import requests
import subprocess

from time import sleep
from requests.exceptions import ConnectionError

from sfc.common.sfc_globals import sfc_globals
from sfc.common.classifier import NfqClassifier, IPV4, IPv6
from conftest import (SF_URL, SFC_URL, SFF_URL, SFP_URL, RSP_RPC_URL,
                      url_2_json_data, get_test_files, read_json, ODL_PORT)


__author__ = 'Dusan Madar'
__email__ = 'madar.dusan@gmail.com'
__copyright__ = 'Copyright(c) 2015, Cisco Systems, Inc.'
__version__ = '0.1'
__status__ = 'alpha'


"""
NFQ classifier tests

NOTES: - root privileges are required
         (as iptables requires it)
       - ODL running locally is required for some tests
         (these tests will be skipped if ODL is not running)
"""


#: constants
TEST_FILES_DIR = os.path.join(os.path.dirname(__file__), 'data')
TEST_FILES_COMMON = get_test_files()


def _odl_is_runing():
    """
    Helper - a dummy check if ODL is running on localhost

    :return str

    """
    odl_runing = False

    try:
        odl_port = ':{port}'.format(port=ODL_PORT)
        process = subprocess.Popen(['sudo', 'lsof', '-i', odl_port],
                                   stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)

        out, _ = process.communicate()
        out = out.strip().decode()

        if out:
            odl_runing = True
    except:
        pass
    finally:
        return odl_runing


def _send_udp_packet(s_ip, s_port, d_ip, d_port):
    """
    Helper - send an UDP packet

    :param s_ip: source IP
    :type s_ip: str
    :param s_port: source port
    :type s_port: int
    :param d_ip: destination IP
    :type d_ip: str
    :param d_port: destination port
    :type d_port: int

    """
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind((s_ip, s_port))
    sock.sendto(b'', (d_ip, d_port))


def _list_iptables_raw_table(ipv):
    """
    Helper - list content of iptables/ip6tables 'raw' table

    :param ipv: IP version
    :type ipv: int

    :return str

    """
    iptables = 'iptables'
    ip6tables = 'ip6tables'

    if (IPV4 in ipv) and (IPv6 in ipv):
        ip_tables = (iptables, ip6tables)
    elif IPV4 in ipv:
        ip_tables = (iptables,)
    elif IPv6 in ipv:
        ip_tables = (ip6tables,)
    else:
        raise ValueError('Unknown IP address version "%s"', ipv)

    output = ''
    for iptables_cmd in ip_tables:
        cmd = ['sudo', '-v', '-t', 'raw', '-L']
        cmd.insert(1, iptables_cmd)

        process = subprocess.Popen(cmd,
                                   stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)

        out, err = process.communicate()

        if process.returncode != 0:
            err = err.strip()
            raise OSError(err)

        out = out.strip()
        out = out.decode()
        output += out + '\n'

    return output


@pytest.fixture
def classifier():
    """
    Fixture - instantiates NfqClassifier and mock basic RSP related data

    :return `:class:common.classifier.NfqClassifier`

    """
    classifier = NfqClassifier()

    classifier.rsp_id = 1
    classifier.rsp_chain = 'test'

    return classifier


@pytest.fixture
def safe_classifier(request, classifier):
    """
    Fixture - instantiate NfqClassifier and clear **all** after test

    :param request: test function
    :type request: `:class:_pytest.python.SubRequest`
    :param classifier: classifier fixture
    :type classifier: `:class:common.classifier.NfqClassifier`

    :return `:class:common.classifier.NfqClassifier`

    """
    def finalizer():
        classifier.remove_all_rsps()

    request.addfinalizer(finalizer)
    return classifier


@pytest.fixture
def mock_data_store(request, classifier):
    """
    Fixture - mock classifier data-store items

    :param request: test function
    :type request: `:class:_pytest.python.SubRequest`
    :param classifier: safe_classifier fixture
    :type classifier: `:class:common.classifier.NfqClassifier`

    :return `:class:common.classifier.NfqClassifier`

    """
    def finalizer():
        classifier.rsp_2_sff = {}

    classifier.rsp_2_sff = {1: {'name': 'mock-rsp-1',
                                'chains': {'mock-chain-1': (4,)},
                                'sff': {'ip': '0.0.0.0',
                                        'port': 1111,
                                        'starting-index': 255,
                                        'transport-type': ''
                                        }
                                },
                            2: {'name': 'mock-rsp-2',
                                'chains': {'mock-chain-2': (6,)},
                                'sff': {'ip': '0.0.0.0',
                                        'port': 2222,
                                        'starting-index': 255,
                                        'transport-type': ''
                                        }
                                }
                            }

    request.addfinalizer(finalizer)
    return classifier


@pytest.fixture
def acl_test_files():
    """Fixture - get all testing *.json ACLs from data directory

    :returns dict

    """
    return get_test_files(TEST_FILES_DIR)


@pytest.fixture
def acl_data(request, acl_test_files):
    """Fixture - load ACL JSON to dict

    :param request: test function
    :type request: `:class:_pytest.python.SubRequest`
    :param acl_test_files: virl_test_files fixture
    :type acl_test_files: dict

    :returns dict

    """
    acl_name = request.getfuncargvalue('acl_json')

    try:
        acl_file_path = acl_test_files[acl_name]
    except KeyError:
        pytest.fail('Test file "data/%s.json" does not exist' % acl_name)

    acl_data = read_json(acl_file_path)
    return acl_data


@pytest.fixture
def ace_rule_cmd(safe_classifier, acl_data):
    """
    Fixture - get ACE from ACL and compose ip(6)tables command

    :param safe_classifier: safe_classifier fixture
    :type safe_classifier: `:class:common.classifier.NfqClassifier`
    :param acl_data: acl_data fixture
    :type acl_data: dict

    :returns list

    """
    ace_matches = (acl_data['access-lists']
                           ['access-list']
                           [0]
                           ['access-list-entries']
                           [0]
                           ['matches'])

    return safe_classifier.parse_ace(ace_matches)


@pytest.fixture
def clear_sfc_data():
    """
    Fixture - clear SFC data from ODL
    """
    session = requests.Session()

    try:
        for url in (SF_URL, SFC_URL, SFF_URL, SFP_URL):
            sleep(0.1)

            session.delete(url,
                           stream=False,
                           auth=sfc_globals.odl_credentials)
    except:
        pytest.fail('Failed to clear SFC data from ODL')


@pytest.fixture
def produce_rsp():
    """
    Fixture - create RSP in ODL
    """
    session = requests.Session()
    headers = {'content-type': 'application/json'}

    try:
        for url in (SF_URL, SFF_URL, SFC_URL, SFP_URL, RSP_RPC_URL):
            sleep(0.1)

            test_data = url_2_json_data[url]
            test_file_path = TEST_FILES_COMMON[test_data['file']]
            test_file_data = read_json(test_file_path, output=str)

            http_method = getattr(session, test_data['method'])
            response = http_method(url=url,
                                   stream=False,
                                   headers=headers,
                                   data=test_file_data,
                                   auth=sfc_globals.odl_credentials)

            if not response.ok:
                raise
    except:
        pytest.fail('Failed to create RSP')


#
# IP version determining
#
@pytest.mark.parametrize('ip', [('0.0.0.0', 4), ('0.0.0.0/0', 4),
                                ('127.0.0.1', 4), ('123.40.50.60/16', 4),
                                ('::', 6), ('::0', 6), ('::0/0', 6),
                                ('2001:cdba::3257:9652', 6),
                                ('2001:cdba:0000:0000:0000:0000:3257:9652', 6),
                                ])
def test_ip_version_setting(classifier, ip):
    """
    Test if classifier is able to determine IP version

    Pass: if correct IP version is set
    Fail: if an exception is raised or assert fails

    """
    assert classifier._get_current_ip_version(ip[0]) == ip[1]


@pytest.mark.parametrize('ip', ['0.0.a.0', '::/0', '21.db8::FFFF:192.168.0.5'])
def test_ip_version_setting_negative(classifier, ip):
    """
    Test if classifier raise ValueError for invalid IP address while trying to
    determine IP version

    Pass: if ValueError is raised
    Fail: if ValueError is not raised

    """
    with pytest.raises(ValueError):
        classifier._get_current_ip_version(ip[0])


#
# data-store RSP retrieval
#
@pytest.mark.parametrize('rsp_name', [('mock-rsp-1', tuple),
                                      ('mock-rsp-22', type(None))])
def test_data_store_rsp_retrieval(classifier, mock_data_store, rsp_name):
    """
    Test if classifier is able to retrieve RSP by its name from its local
    data-store

    Pass: if retrieved RSP data is of expected type
    Fail: if an exception is raised or the above condition fails

    """
    rsp_data = classifier._get_rsp_by_name(rsp_name[0])
    assert isinstance(rsp_data, rsp_name[1])


#
# ODL RSP retrieval
#
@pytest.mark.skipif(_odl_is_runing(), reason="ODL is running")
@pytest.mark.parametrize('rsp_name', ['rsp-1'])
def test_odl_rsp_retrieval_odl_not_running(classifier, rsp_name):
    """
    Test if classifier raises ConnectionError if ODL is not running

    Pass: if `requests.exceptions.ConnectionError` is raised
    Fail: if the above condition fails

    """
    with pytest.raises(ConnectionError):
        classifier._fetch_rsp_first_hop_from_odl(rsp_name)


@pytest.mark.skipif(not _odl_is_runing(), reason="ODL is not running")
@pytest.mark.parametrize('rsp_name', ['whatever'])
def test_odl_rsp_retrieval_no_rsp(capfd, classifier, rsp_name):
    """
    Test if classifier raises RuntimeError for RSP non-existing in ODL

    Pass: if `RuntimeError` is raised
          if 'operation-failed' string is in captured error log
    Fail: if the above conditions fails

    """
    with pytest.raises(RuntimeError):
        classifier._fetch_rsp_first_hop_from_odl(rsp_name)

    _, err = capfd.readouterr()
    assert 'operation-failed' in err


@pytest.mark.skipif(not _odl_is_runing(), reason="ODL is not running")
@pytest.mark.parametrize('rsp_name', ['SFC1-SFP1-Path-1'])
def test_odl_rsp_retrieval(capfd, clear_sfc_data, produce_rsp,
                           classifier, rsp_name):
    """
    Test if classifier is able to retrieve RSP first hop data from ODL

    Pass: if SFF data retrieved from ODl are equal to expected data
    Fail: if an exception is raised or the above condition fails

    """
    try:
        sff_data = classifier._fetch_rsp_first_hop_from_odl(rsp_name)
    except:
        _, err = capfd.readouterr()
        pytest.fail(err)

    assert sff_data == {'transport-type': 'service-locator:vxlan-gpe',
                        'path-id': 1,
                        'port': 30001,
                        'ip': '127.0.0.1',
                        'starting-index': 255}


#
# packet mark handling
#
@pytest.mark.parametrize('mock_rsp', [(1, (4,), 104),
                                      (20, (6,), 2006),
                                      (305, (4, 6), 30510)])
def test_packet_mark_composing(classifier, mock_rsp):
    """
    Test if classifier is able to compose packet mark

    Pass: if composed packet mark is equal to expected
    Fail: if an exception is raised or the above condition fails

    """
    classifier.rsp_id = mock_rsp[0]
    classifier.rsp_ipv = mock_rsp[1]

    expected_result = mock_rsp[2]
    assert classifier._compose_packet_mark() == expected_result


@pytest.mark.parametrize('mock_rsp', [(1, 4, 104),
                                      (20, 6, 2006),
                                      (305, 10, 30510)])
def test_packet_mark_decomposing(classifier, mock_rsp):
    """
    Test if classifier is able to decompose packet mark

    Pass: if decomposed packet mark is equal to expected data
    Fail: if an exception is raised or the above condition fails

    """
    expected_result = mock_rsp[0], mock_rsp[1]
    assert classifier._decompose_packet_mark(mock_rsp[2]) == expected_result


#
# ACE parsing
#
@pytest.mark.parametrize('acl_json', ['acl-ipv4'])
def test_parse_ace_ipv4(acl_json, acl_data, ace_rule_cmd, safe_classifier):
    """
    Test IPv4 related ACE parsing

    Pass: if correct rsp_ipv is set,
          if correct ACE rule command is constructed
    Fail: if an exception is raised or any assert fails

    """
    assert safe_classifier.rsp_ipv == (4,)

    assert ace_rule_cmd == ['-I', 'test',
                            '-s', '0.0.0.0/0',
                            '-d', '0.0.0.0/0',
                            '-j', 'MARK', '--set-mark', 104]


@pytest.mark.parametrize('acl_json', ['acl-ipv6'])
def test_parse_ace_ipv6(acl_json, acl_data, ace_rule_cmd, safe_classifier):
    """
    Test IPv6 related ACE parsing

    Pass: if correct rsp_ipv is set,
          if correct ACE rule command is constructed
    Fail: if an exception is raised or any assert fails

    """
    assert safe_classifier.rsp_ipv == (6,)

    assert ace_rule_cmd == ['-I', 'test',
                            '-s', '::0/0',
                            '-d', '::0/0',
                            '-j', 'MARK', '--set-mark', 106]


@pytest.mark.parametrize('acl_json', ['acl-mac'])
def test_parse_ace_mac(acl_json, acl_data, ace_rule_cmd, safe_classifier):
    """
    Test MAC related ACE parsing

    Pass: if correct rsp_ipv is set,
          if correct ACE rule command is constructed
    Fail: if an exception is raised or any assert fails

    """
    assert safe_classifier.rsp_ipv == (4, 6)

    assert ace_rule_cmd == ['-I', 'test',
                            '-m', 'mac', '--mac-source', '00:00:00:00:00:00',
                            '-j', 'MARK', '--set-mark', 110]


@pytest.mark.parametrize('acl_json', ['acl-port'])
def test_parse_ace_port(acl_json, acl_data, ace_rule_cmd, safe_classifier):
    """
    Test PORT related ACE parsing

    Pass: if correct rsp_ipv is set,
          if correct ACE rule command is constructed
    Fail: if an exception is raised or any assert fails

    """
    assert safe_classifier.rsp_ipv == (4, 6)

    assert ace_rule_cmd == ['-I', 'test',
                            '-p', 'udp', '--sport', '15000:20000',
                            '-j', 'MARK', '--set-mark', 110]


#
# ACL parsing and iptables rules management
#
@pytest.mark.skipif(not _odl_is_runing(), reason="ODL is not running")
@pytest.mark.parametrize('acl_json', ['acl-ipv4', 'acl-ipv6',
                                      'acl-mac', 'acl-port'])
def test_process_acl_1(capfd, acl_json, acl_data,
                       clear_sfc_data, produce_rsp, classifier):
    """
    Test if correct iptables rule is created and cleared afterwards using
    remove_all_rsps()

    Pass: if correct chain exists in iptables 'raw' table,
          if correct packet mark is applied in iptables 'raw' table
          if correct chain is removed from iptables 'raw' table
    Fail: if an exception is raised or any assert fails

    """
    try:
        classifier.process_acl(acl_data['access-lists'])
    except:
        _, err = capfd.readouterr()
        pytest.fail(err)

    iptables_content = _list_iptables_raw_table(classifier.rsp_ipv)
    assert classifier.rsp_chain in iptables_content
    assert hex(classifier.rsp_mark) in iptables_content

    classifier.remove_all_rsps()

    iptables_content = _list_iptables_raw_table(classifier.rsp_ipv)
    assert classifier.rsp_chain not in iptables_content


@pytest.mark.skipif(not _odl_is_runing(), reason="ODL is not running")
@pytest.mark.parametrize('acl_json', ['acl-ipv4', 'acl-ipv6',
                                      'acl-mac', 'acl-port'])
def test_process_acl_2(capfd, acl_json, acl_data,
                       clear_sfc_data, produce_rsp, classifier):
    """
    Test if correct iptables rule is created and cleared afterwards using
    remove_acl_rsps()

    Pass: if correct chain exists in iptables 'raw' table,
          if correct packet mark is applied in iptables 'raw' table
          if correct chain is removed from iptables 'raw' table
    Fail: if an exception is raised or any assert fails

    """
    try:
        classifier.process_acl(acl_data['access-lists'])
    except:
        _, err = capfd.readouterr()
        pytest.fail(err)

    rsp_ipv = classifier.rsp_ipv
    iptables_content = _list_iptables_raw_table(rsp_ipv)
    assert classifier.rsp_chain in iptables_content
    assert hex(classifier.rsp_mark) in iptables_content

    acl_data = {'access-list': [{'access-list-entries': [{'delete': True}],
                                 'acl-name': 'ACL1'}]}

    try:
        classifier.process_acl(acl_data)
    except:
        _, err = capfd.readouterr()
        pytest.fail(err)

    iptables_content = _list_iptables_raw_table(rsp_ipv)
    assert classifier.rsp_chain not in iptables_content


#
# packet sending
#
@pytest.mark.skipif(not _odl_is_runing(), reason="ODL is not running")
@pytest.mark.parametrize('acl_json', ['acl-ipv4'])
def test_packet_sending(capfd, acl_json, acl_data,
                        clear_sfc_data, produce_rsp, safe_classifier):
    """
    Test if (at least) 5 sent packets really traverses through NFQ

    Pass: if NFQ registers at least 5 packets
    Fail: if an exception is raised or the assert fails

    """
    try:
        safe_classifier.collect_packets()
        safe_classifier.process_acl(acl_data['access-lists'])

        for _ in range(0, 5):
            _send_udp_packet(s_ip='0.0.0.0', s_port=9999,
                             d_ip='0.0.0.0', d_port=8080)

        rsp_ipv = safe_classifier.rsp_ipv
        iptables_content = _list_iptables_raw_table(rsp_ipv)

        iptables_lines = iptables_content.splitlines()
        nfqueue = iptables_lines[-1].split()

        assert int(nfqueue[0]) >= 5

    except Exception as exc:
        pytest.fail(exc)

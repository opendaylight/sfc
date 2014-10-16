define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope;

    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($controller) {
      //  sfc.register.controller('rootSfcCtrl', function ($rootScope) {
      return $controller('rootSfcCtrl'); // registers required constants in $rootScope
    }));

    beforeEach(angular.mock.inject(function ($rootScope) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
    }));

    describe('acl.services', function () {

      describe("SfcAclHelper", function () {

        var SfcAclHelper;

        beforeEach(angular.mock.inject(function (_SfcAclHelper_) {
          SfcAclHelper = _SfcAclHelper_;
        }));

        it("addAce should add new item object to 'access-list-entries' array property", function () {
          scope.data = {};

          SfcAclHelper.addAce(scope);
          expect(scope.data['access-list-entries']).toEqual([
            {}
          ]);

          SfcAclHelper.addAce(scope);
          expect(scope.data['access-list-entries']).toEqual([
            {},
            {}
          ]);
        });

        it("removeAce should add new item object to 'access-list-entries' array property", function () {
          scope.data = {
            'access-list-entries': [0, 1, 2, 3]
          };

          SfcAclHelper.removeAce(0, scope);
          expect(scope.data['access-list-entries']).toEqual([1, 2, 3]);

          SfcAclHelper.removeAce(1, scope);
          expect(scope.data['access-list-entries']).toEqual([1, 3]);
        });

        it("valueOfAceType should return ip/eth according to properties matches object contains", function () {
          var matches, testedValue;

          // ip
          matches = {
            'destination-ipv4-address': 'dummy'
          };

          testedValue = SfcAclHelper.valueOfAceType(matches, rootScope);
          expect(testedValue).toEqual('ip');

          // eth
          matches = {
            'destination-mac-address': 'dummy'
          };

          testedValue = SfcAclHelper.valueOfAceType(matches, rootScope);
          expect(testedValue).toEqual('eth');
        });

        it("valueOfIpVersion should return ipv4/ipv6 according to properties matches object contains", function () {
          var matches, testedValue;

          // 4
          matches = {
            'destination-ipv4-address': 'dummy'
          };

          testedValue = SfcAclHelper.valueOfIpVersion(matches, rootScope);
          expect(testedValue).toEqual('IPv4');

          // 6
          matches = {
            'destination-ipv6-address': 'dummy'
          };

          testedValue = SfcAclHelper.valueOfIpVersion(matches, rootScope);
          expect(testedValue).toEqual('IPv6');
        });


        it("[source/destination]IpMacMaskToString should return correct address", function () {
          var matches, testedValue;

          // eth
          matches = {
            'source-mac-address': 'source-mac-address',
            'destination-mac-address': 'destination-mac-address'
          };
          // source
          testedValue = SfcAclHelper.sourceIpMacMaskToString(matches);
          expect(testedValue).toEqual('source-mac-address ');
          // destination
          testedValue = SfcAclHelper.destinationIpMacMaskToString(matches);
          expect(testedValue).toEqual('destination-mac-address ');

          // ipv4
          matches = {
            'source-ipv4-address': '1.1.1.1',
            'destination-ipv4-address': '2.2.2.2'
          };
          // source
          testedValue = SfcAclHelper.sourceIpMacMaskToString(matches);
          expect(testedValue).toEqual('1.1.1.1 ');
          // destination
          testedValue = SfcAclHelper.destinationIpMacMaskToString(matches);
          expect(testedValue).toEqual('2.2.2.2 ');

          // ipv6
          matches = {
            'source-ipv6-address': '1::1',
            'destination-ipv6-address': '2::2'
          };
          // source
          testedValue = SfcAclHelper.sourceIpMacMaskToString(matches);
          expect(testedValue).toEqual('1::1 ');
          // destination
          testedValue = SfcAclHelper.destinationIpMacMaskToString(matches);
          expect(testedValue).toEqual('2::2 ');
        });

        it("toString functions should return correct contents of matches properties", function () {
          var matches, testedValue;

          matches = {
            'flow-label': 'flow-label',
            'dscp': 'dscp',
            'ip-protocol': 'ip-protocol',
            'source-port-range': {
              'lower-port': 'source-lower-port',
              'upper-port': 'source-upper-port'
            },
            'destination-port-range': {
              'lower-port': 'destination-lower-port',
              'upper-port': 'destination-upper-port'
            }
          };

          // flowLabelToString
          testedValue = SfcAclHelper.flowLabelToString(matches);
          expect(testedValue).toEqual('flow-label');
          // dscpToString
          testedValue = SfcAclHelper.dscpToString(matches);
          expect(testedValue).toEqual('dscp');
          // ipProtocolToString
          testedValue = SfcAclHelper.ipProtocolToString(matches);
          expect(testedValue).toEqual('ip-protocol');
          // sourcePortRangeToString
          testedValue = SfcAclHelper.sourcePortRangeToString(matches['source-port-range']);
          expect(testedValue).toEqual('source-lower-port source-upper-port ');
          // destinationPortRangeToString
          testedValue = SfcAclHelper.destinationPortRangeToString(matches['destination-port-range']);
          expect(testedValue).toEqual('destination-lower-port destination-upper-port ');
        });

      });

    });

  });

}); //end define

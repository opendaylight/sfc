define(['app/sfc/sfc.test.module.loader', 'app/sfc/acl/acl.ip.v4.tpl.html', 'app/sfc/acl/acl.ip.v6.tpl.html', 'app/sfc/acl/acl.ethernet.tpl.html', 'app/sfc/acl/acl.ip.tpl.html'], function (sfc) {

  ddescribe('SFC - acl.directives', function () {
    var scope, compile;

    beforeEach(angular.mock.module('app.sfc'));
    beforeEach(angular.mock.module('src/app/sfc/acl/acl.ip.v4.tpl.html'));
    beforeEach(angular.mock.module('src/app/sfc/acl/acl.ip.v6.tpl.html'));
    beforeEach(angular.mock.module('src/app/sfc/acl/acl.ethernet.tpl.html'));
    beforeEach(angular.mock.module('src/app/sfc/acl/acl.ip.tpl.html'));

    beforeEach(angular.mock.inject(function ($rootScope, $compile) {
      scope = $rootScope.$new();
      compile = $compile;
    }));

    describe('directive: acl-ip-matches-v4', function () {

      beforeEach(function () {
        this.addMatchers(sfc.customJasmineMatchers);
      });

      var inputElement;

      var compileDirective = function (scope) {
        inputElement = compile('<acl-ip-matches-v4 id-suffix="0" matches="outer_matches" reset-on="ipv4-reset-event-key" not-reset-condition="not-reset"></acl-ip-matches-v4>')(scope);
      };

      it("should validate input and synchronize to matches model", function () {
        scope.outer_matches = {};

        compileDirective(scope);
        scope.$digest();

        var formCtrl = inputElement.isolateScope()['f_ipv4'];

        var sourceAddrInputCrtl = formCtrl['source-ipv4-address'];
        var destinationAddrInputCrtl = formCtrl['destination-ipv4-address'];

        sourceAddrInputCrtl.$setViewValue('10.10.10.10');
        destinationAddrInputCrtl.$setViewValue('20.20.20.20');

        expect(formCtrl.$invalid).toBeFalsy();

        expect(scope.outer_matches['source-ipv4-address']).toEqual('10.10.10.10');
        expect(scope.outer_matches['destination-ipv4-address']).toEqual('20.20.20.20');
      });


      it("should delete properties in matches model on registed event", function () {
        scope.outer_matches = {
          'destination-ipv4-address': 'test',
          'source-ipv4-address': 'test'
        };

        compileDirective(scope);
        scope.$digest();

        expect(scope.outer_matches).toContainProperty('destination-ipv4-address');
        expect(scope.outer_matches).toContainProperty('source-ipv4-address');

        scope.$broadcast('ipv4-reset-event-key', 'not-reset');

        expect(scope.outer_matches).toContainProperty('destination-ipv4-address');
        expect(scope.outer_matches).toContainProperty('source-ipv4-address');

        scope.$broadcast('ipv4-reset-event-key', 'whatever');

        expect(scope.outer_matches).not.toContainProperty('destination-ipv4-address');
        expect(scope.outer_matches).not.toContainProperty('source-ipv4-address');
      });

    });

    describe('directive: acl-ip-matches-v6', function () {

      beforeEach(function () {
        this.addMatchers(sfc.customJasmineMatchers);
      });

      var inputElement;

      var compileDirective = function (scope) {
        inputElement = compile('<acl-ip-matches-v6 id-suffix="0" matches="outer_matches" reset-on="ipv6-reset-event-key" not-reset-condition="not-reset"></acl-ip-matches-v6>')(scope);
      };

      it("should validate input and synchronize to matches model", function () {
        scope.outer_matches = {};

        compileDirective(scope);
        scope.$digest();

        var formCtrl = inputElement.isolateScope()['f_ipv6'];

        var sourceAddrInputCtrl = formCtrl['source-ipv6-address'];
        var destinationAddrInputCtrl = formCtrl['destination-ipv6-address'];

        sourceAddrInputCtrl.$setViewValue('FE80:0000:0000:0000:0202:B3FF:FE1E:8328');
        destinationAddrInputCtrl.$setViewValue('FE80:0000:0000:0000:0202:B3FF:FE1E:8329');

        expect(formCtrl.$invalid).toBeFalsy();

        expect(scope.outer_matches['source-ipv6-address']).toEqual('FE80:0000:0000:0000:0202:B3FF:FE1E:8328');
        expect(scope.outer_matches['destination-ipv6-address']).toEqual('FE80:0000:0000:0000:0202:B3FF:FE1E:8329');
      });


      it("should delete properties in matches model on registed event", function () {
        scope.outer_matches = {
          'destination-ipv6-address': 'test',
          'source-ipv6-address': 'test'
        };

        compileDirective(scope);
        scope.$digest();

        expect(scope.outer_matches).toContainProperty('destination-ipv6-address');
        expect(scope.outer_matches).toContainProperty('source-ipv6-address');

        scope.$broadcast('ipv6-reset-event-key', 'not-reset');

        expect(scope.outer_matches).toContainProperty('destination-ipv6-address');
        expect(scope.outer_matches).toContainProperty('source-ipv6-address');

        scope.$broadcast('ipv6-reset-event-key', 'whatever');

        expect(scope.outer_matches).not.toContainProperty('destination-ipv6-address');
        expect(scope.outer_matches).not.toContainProperty('source-ipv6-address');
      });

    });

    describe('directive: acl-ethernet-matches', function () {

      beforeEach(function () {
        this.addMatchers(sfc.customJasmineMatchers);
      });

      var inputElement;

      var compileDirective = function (scope) {
        inputElement = compile('<acl-ethernet-matches id-suffix="0" matches="outer_matches" reset-on="ipv6-reset-event-key" not-reset-condition="not-reset"></acl-ethernet-matches>')(scope);
      };

      it("should delete properties in matches model on registed event", function () {

        scope.outer_matches = {
          'destination-mac-address': 'test',
          'source-mac-address': 'test',
          'source-mac-address-mask': 'test',
          'destination-mac-address-mask': 'test'
        };

        compileDirective(scope);
        scope.$digest();

        expect(scope.outer_matches).toContainProperty('destination-mac-address-mask');
        expect(scope.outer_matches).toContainProperty('source-mac-address-mask');
        expect(scope.outer_matches).toContainProperty('source-mac-address');
        expect(scope.outer_matches).toContainProperty('destination-mac-address');

        scope.$broadcast('ipv6-reset-event-key', 'not-reset');

        expect(scope.outer_matches).toContainProperty('destination-mac-address-mask');
        expect(scope.outer_matches).toContainProperty('source-mac-address-mask');
        expect(scope.outer_matches).toContainProperty('source-mac-address');
        expect(scope.outer_matches).toContainProperty('destination-mac-address');

        scope.$broadcast('ipv6-reset-event-key', 'whatever');

        expect(scope.outer_matches).not.toContainProperty('destination-mac-address-mask');
        expect(scope.outer_matches).not.toContainProperty('source-mac-address-mask');
        expect(scope.outer_matches).not.toContainProperty('source-mac-address');
        expect(scope.outer_matches).not.toContainProperty('destination-mac-address');
      });

    });

    describe('directive: acl-ip-matches', function () {

      beforeEach(function () {
        this.addMatchers(sfc.customJasmineMatchers);
      });

      var inputElement;

      var compileDirective = function (scope) {
        inputElement = compile('<acl-ip-matches acl-constants="[\'ipv4\', \'ipv6\']" id-suffix="0" matches="outer_matches" reset-on="ip-reset-event-key" not-reset-condition="not-reset"></acl-ip-matches>')(scope);
      };

      it("should delete properties in matches model on registed event", function () {

        scope.outer_matches = {};

        compileDirective(scope);
        scope.$digest();

        var directiveScope = inputElement.isolateScope();
        var formCtrl = directiveScope['f_ip'];

        // should create property
        formCtrl['sourcePortRangeCheck'].$setViewValue(true);
        directiveScope.$digest();

        expect(scope.outer_matches).toContainProperty('source-port-range');

        // should create property
        formCtrl['destinationPortRangeCheck'].$setViewValue(true);
        directiveScope.$digest();

        expect(scope.outer_matches).toContainProperty('destination-port-range');

        // should call $broadcast
        spyOn(directiveScope, '$broadcast').andCallThrough();

        formCtrl['ace-ip-version'].$setViewValue('test_string');
        directiveScope.$digest();

        expect(directiveScope.$broadcast).toHaveBeenCalledWith('ace_ip_change', 'test_string');

        // not reset
        scope.$broadcast('ip-reset-event-key', 'not-reset');

        expect(scope.outer_matches).toContainProperty('source-port-range');
        expect(scope.outer_matches).toContainProperty('destination-port-range');

        // reset
        scope.$broadcast('ip-reset-event-key', 'whatever');

        expect(scope.outer_matches).not.toContainProperty('source-port-range');
        expect(scope.outer_matches).not.toContainProperty('destination-port-range');
      });

    });


  });
});
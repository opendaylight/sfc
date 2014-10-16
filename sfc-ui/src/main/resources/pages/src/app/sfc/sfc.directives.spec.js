define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, state, stateParams, compile, timeout;


    beforeEach(angular.mock.module('ui.router'));
    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.common.layout'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($controller, $q, $state, $stateParams, $rootScope, $compile, $timeout) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      state = $state;
      stateParams = $stateParams;
      compile = $compile;
      timeout = $timeout;
    }));

    describe('sfc.directives', function () {

      describe('dateAndTime', function () {
        var compileDirective, form;

        beforeEach(function () {
          compileDirective = function () {
            scope.data = {dateAndTime: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.dateAndTime" name="dateAndTime" date-and-time></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
        });

        it("should pass null into model if input is empty", function() {
          compileDirective();
          form.dateAndTime.$setViewValue('');
          scope.$digest();
          expect(form.dateAndTime.$valid).toBeTruthy();
          expect(scope.data.dateAndTime).toEqual(null);
          form.dateAndTime.$setViewValue(null);
          expect(form.dateAndTime.$valid).toBeTruthy();
          expect(scope.data.dateAndTime).toEqual(null);
        });

        it("should not pass bad dateAndTime into model", function() {
          compileDirective();
          form.dateAndTime.$setViewValue('2014-20-10T22:00');
          scope.$digest();
          expect(form.dateAndTime.$valid).toBeFalsy();
          expect(scope.data.dateAndTime).toBeUndefined();
          form.dateAndTime.$setViewValue('2014-20-10T22:00:00');
          scope.$digest();
          expect(form.dateAndTime.$valid).toBeFalsy();
          expect(scope.data.dateAndTime).toBeUndefined();
        });

        it("should pass correct dateAndTime into model", function() {
          compileDirective();
          form.dateAndTime.$setViewValue('2014-10-14T12:00:00Z');
          scope.$digest();
          expect(form.dateAndTime.$valid).toBeTruthy();
          expect(scope.data.dateAndTime).toBe('2014-10-14T12:00:00Z');
          form.dateAndTime.$setViewValue('2014-10-14T12:00:00.00+08:00');
          scope.$digest();
          expect(form.dateAndTime.$valid).toBeTruthy();
          expect(scope.data.dateAndTime).toBe('2014-10-14T12:00:00.00+08:00');
        });
      });

      describe('vlanId', function () {
        var compileDirective, form;

        beforeEach(function () {
          compileDirective = function () {
            scope.data = {vlanId: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.vlanId" name="vlanId" vlan-id></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
        });

        it("should pass null into model if input is empty", function() {
          compileDirective();
          form.vlanId.$setViewValue('');
          scope.$digest();
          expect(form.vlanId.$valid).toBeTruthy();
          expect(scope.data.vlanId).toEqual(null);
          form.vlanId.$setViewValue(null);
          expect(form.vlanId.$valid).toBeTruthy();
          expect(scope.data.vlanId).toEqual(null);
        });

        it("should not pass bad vlanId into model", function() {
          compileDirective();
          form.vlanId.$setViewValue('0');
          scope.$digest();
          expect(form.vlanId.$valid).toBeFalsy();
          expect(scope.data.vlanId).toBeUndefined();
          form.vlanId.$setViewValue('5000');
          scope.$digest();
          expect(form.vlanId.$valid).toBeFalsy();
          expect(scope.data.vlanId).toBeUndefined();
        });

        it("should pass correct vlanId into model", function() {
          compileDirective();
          form.vlanId.$setViewValue('4094');
          scope.$digest();
          expect(form.vlanId.$valid).toBeTruthy();
          expect(scope.data.vlanId).toBe('4094');
        });

      });

      describe('macAddress', function () {
        var compileDirective, form;

        beforeEach(function () {
          compileDirective = function () {
            scope.data = {macAddress: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.macAddress" name="macAddress" mac-address></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
        });

        it("should pass null into model if input is empty", function() {
          compileDirective();
          form.macAddress.$setViewValue('');
          scope.$digest();
          expect(form.macAddress.$valid).toBeTruthy();
          expect(scope.data.macAddress).toEqual(null);
          form.macAddress.$setViewValue(null);
          expect(form.macAddress.$valid).toBeTruthy();
          expect(scope.data.macAddress).toEqual(null);
        });

        it("should not pass bad macAddress into model", function() {
          compileDirective();
          form.macAddress.$setViewValue('00:AA:BB');
          scope.$digest();
          expect(form.macAddress.$valid).toBeFalsy();
          expect(scope.data.macAddress).toBeUndefined();
          form.macAddress.$setViewValue('aa:bb:cc:dd:ee:xx');
          scope.$digest();
          expect(form.macAddress.$valid).toBeFalsy();
          expect(scope.data.macAddress).toBeUndefined();
        });

        it("should pass correct macAddress into model", function() {
          compileDirective();
          form.macAddress.$setViewValue('aa:BB:CC:dd:EE:ff');
          scope.$digest();
          expect(form.macAddress.$valid).toBeTruthy();
          expect(scope.data.macAddress).toBe('aa:BB:CC:dd:EE:ff');
        });

      });

      describe('numberRange', function () {
        var compileDirective, form;

        beforeEach(function () {
          compileDirective = function () {
            scope.data = {numberRange: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.numberRange" name="numberRange" number-range="{from: 10, to:20}"></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
        });

        it("should pass null into model if input is empty", function() {
          compileDirective();
          form.numberRange.$setViewValue('');
          scope.$digest();
          expect(form.numberRange.$valid).toBeTruthy();
          expect(scope.data.numberRange).toEqual(null);
          form.numberRange.$setViewValue(null);
          expect(form.numberRange.$valid).toBeTruthy();
          expect(scope.data.numberRange).toEqual(null);
        });

        it("should not pass bad numberRange into model", function() {
          compileDirective();
          form.numberRange.$setViewValue('5');
          scope.$digest();
          expect(form.numberRange.$valid).toBeFalsy();
          expect(scope.data.numberRange).toBeUndefined();
          form.numberRange.$setViewValue('25');
          scope.$digest();
          expect(form.numberRange.$valid).toBeFalsy();
          expect(scope.data.numberRange).toBeUndefined();
        });

        it("should pass correct numberRange into model", function() {
          compileDirective();
          form.numberRange.$setViewValue('15');
          scope.$digest();
          expect(form.numberRange.$valid).toBeTruthy();
          expect(scope.data.numberRange).toBe('15');
        });

      });

      describe('uint8', function () {
        var compileDirective, form;

        beforeEach(function () {
          compileDirective = function () {
            scope.data = {uint8: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.uint8" name="uint8" uint8></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
        });

        it("should pass null into model if input is empty", function() {
          compileDirective();
          form.uint8.$setViewValue('');
          scope.$digest();
          expect(form.uint8.$valid).toBeTruthy();
          expect(scope.data.uint8).toEqual(null);
          form.uint8.$setViewValue(null);
          expect(form.uint8.$valid).toBeTruthy();
          expect(scope.data.uint8).toEqual(null);
        });

        it("should not pass bad uint8 into model", function() {
          compileDirective();
          form.uint8.$setViewValue('-10');
          scope.$digest();
          expect(form.uint8.$valid).toBeFalsy();
          expect(scope.data.uint8).toBeUndefined();
          form.uint8.$setViewValue('300');
          scope.$digest();
          expect(form.uint8.$valid).toBeFalsy();
          expect(scope.data.uint8).toBeUndefined();
        });

        it("should pass correct uint8 into model", function() {
          compileDirective();
          form.uint8.$setViewValue('255');
          scope.$digest();
          expect(form.uint8.$valid).toBeTruthy();
          expect(scope.data.uint8).toBe('255');
        });

      });

      describe('uint16', function () {
        var compileDirective, form;

        beforeEach(function () {
          compileDirective = function () {
            scope.data = {uint16: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.uint16" name="uint16" uint16></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
        });

        it("should pass null into model if input is empty", function() {
          compileDirective();
          form.uint16.$setViewValue('');
          scope.$digest();
          expect(form.uint16.$valid).toBeTruthy();
          expect(scope.data.uint16).toEqual(null);
          form.uint16.$setViewValue(null);
          expect(form.uint16.$valid).toBeTruthy();
          expect(scope.data.uint16).toEqual(null);
        });

        it("should not pass bad uint16 into model", function() {
          compileDirective();
          form.uint16.$setViewValue('-10');
          scope.$digest();
          expect(form.uint16.$valid).toBeFalsy();
          expect(scope.data.uint16).toBeUndefined();
          form.uint16.$setViewValue('123123');
          scope.$digest();
          expect(form.uint16.$valid).toBeFalsy();
          expect(scope.data.uint16).toBeUndefined();
        });

        it("should pass correct uint16 into model", function() {
          compileDirective();
          form.uint16.$setViewValue('65535');
          scope.$digest();
          expect(form.uint16.$valid).toBeTruthy();
          expect(scope.data.uint16).toBe('65535');
        });

      });

      describe('uint32', function () {
        var compileDirective, form;

        beforeEach(function () {
          compileDirective = function () {
            scope.data = {uint32: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.uint32" name="uint32" uint32></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
        });

        it("should pass null into model if input is empty", function() {
          compileDirective();
          form.uint32.$setViewValue('');
          scope.$digest();
          expect(form.uint32.$valid).toBeTruthy();
          expect(scope.data.uint32).toEqual(null);
          form.uint32.$setViewValue(null);
          expect(form.uint32.$valid).toBeTruthy();
          expect(scope.data.uint32).toEqual(null);
        });

        it("should not pass bad uint32 into model", function() {
          compileDirective();
          form.uint32.$setViewValue('-10');
          scope.$digest();
          expect(form.uint32.$valid).toBeFalsy();
          expect(scope.data.uint32).toBeUndefined();
          form.uint32.$setViewValue('1234567890123');
          scope.$digest();
          expect(form.uint32.$valid).toBeFalsy();
          expect(scope.data.uint32).toBeUndefined();
        });

        it("should pass correct uint32 into model", function() {
          compileDirective();
          form.uint32.$setViewValue('105000');
          scope.$digest();
          expect(form.uint32.$valid).toBeTruthy();
          expect(scope.data.uint32).toBe('105000');
        });

      });

      describe('port', function () {
        var compileDirective, form;

        beforeEach(function () {
          compileDirective = function () {
            scope.data = {port: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.port" name="port" port></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
        });

        it("should pass null into model if input is empty", function() {
          compileDirective();
          form.port.$setViewValue('');
          scope.$digest();
          expect(form.port.$valid).toBeTruthy();
          expect(scope.data.port).toEqual(null);
          form.port.$setViewValue(null);
          expect(form.port.$valid).toBeTruthy();
          expect(scope.data.port).toEqual(null);
        });

        it("should not pass bad port into model", function() {
          compileDirective();
          form.port.$setViewValue('-10');
          scope.$digest();
          expect(form.port.$valid).toBeFalsy();
          expect(scope.data.port).toBeUndefined();
          form.port.$setViewValue('123123');
          scope.$digest();
          expect(form.port.$valid).toBeFalsy();
          expect(scope.data.port).toBeUndefined();
        });

        it("should pass correct port into model", function() {
          compileDirective();
          form.port.$setViewValue('10500');
          scope.$digest();
          expect(form.port.$valid).toBeTruthy();
          expect(scope.data.port).toBe('10500');
        });

      });

      describe('ipAddress', function () {
        var compileDirective, compileDirectiveIPv4Prefix, compileDirectiveIPv6Prefix, form;

        beforeEach(function () {
          compileDirective = function () {
            scope.data = {ip: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.ip" name="ip" ip-address></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
          compileDirectiveIPv4Prefix = function () {
            scope.data = {ip: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.ip" name="ip" ip-address="{version: 4, prefix: true}"></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
          compileDirectiveIPv6Prefix = function () {
            scope.data = {ip: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.ip" name="ip" ip-address="{version: 6, prefix: true}"></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
        });

        it("should pass null into model if input is empty", function() {
          compileDirective();
          form.ip.$setViewValue('');
          scope.$digest();
          expect(form.ip.$valid).toBeTruthy();
          expect(scope.data.ip).toEqual(null);
          form.ip.$setViewValue(null);
          expect(form.ip.$valid).toBeTruthy();
          expect(scope.data.ip).toEqual(null);
        });

        it("should not pass bad IPv4 into model", function() {
          compileDirective();
          form.ip.$setViewValue('300.200.10.10');
          scope.$digest();
          expect(form.ip.$valid).toBeFalsy();
          expect(scope.data.ip).toBeUndefined();
          form.ip.$setViewValue('200.200.10');
          scope.$digest();
          expect(form.ip.$valid).toBeFalsy();
          expect(scope.data.ip).toBeUndefined();
        });

        it("should pass correct IPv4 into model", function() {
          compileDirective();
          form.ip.$setViewValue('200.200.10.10');
          scope.$digest();
          expect(form.ip.$valid).toBeTruthy();
          expect(scope.data.ip).toBe('200.200.10.10');
        });

        it("should not pass bad IPv4Prefix into model", function() {
          compileDirectiveIPv4Prefix();
          form.ip.$setViewValue('200.200.10.10/56');
          scope.$digest();
          expect(form.ip.$valid).toBeFalsy();
          expect(scope.data.ip).toBeUndefined();
        });

        it("should pass correct IPv4Prefix into model", function() {
          compileDirectiveIPv4Prefix();
          form.ip.$setViewValue('200.200.10.10/18');
          scope.$digest();
          expect(form.ip.$valid).toBeTruthy();
          expect(scope.data.ip).toBe('200.200.10.10/18');
        });

        it("should not pass bad IPv6 into model", function() {
          compileDirective();
          form.ip.$setViewValue('2001:0db8:85a3:xxxx:0000:8a2e:0370:7334');
          scope.$digest();
          expect(form.ip.$valid).toBeFalsy();
          expect(scope.data.ip).toBeUndefined();
          form.ip.$setViewValue('2001:');
          scope.$digest();
          expect(form.ip.$valid).toBeFalsy();
          expect(scope.data.ip).toBeUndefined();
        });

        it("should pass correct IPv6 into model", function() {
          compileDirective();
          form.ip.$setViewValue('2001:0db8:85a3:0000:0000:8a2e:0370:7334');
          scope.$digest();
          expect(form.ip.$valid).toBeTruthy();
          expect(scope.data.ip).toBe('2001:0db8:85a3:0000:0000:8a2e:0370:7334');
          form.ip.$setViewValue('2001::');
          scope.$digest();
          expect(form.ip.$valid).toBeTruthy();
          expect(scope.data.ip).toBe('2001::');
        });

        it("should not pass bad IPv6Prefix into model", function() {
          compileDirectiveIPv6Prefix();
          form.ip.$setViewValue('2001::/561');
          scope.$digest();
          expect(form.ip.$valid).toBeFalsy();
          expect(scope.data.ip).toBeUndefined();
        });

        it("should pass correct IPv6Prefix into model", function() {
          compileDirectiveIPv6Prefix();
          form.ip.$setViewValue('2001::/64');
          scope.$digest();
          expect(form.ip.$valid).toBeTruthy();
          expect(scope.data.ip).toBe('2001::/64');
        });
      });

      describe("easyEditableTextarea", function () {
        var compileDirective, textArea;

        beforeEach(function () {
          compileDirective = function () {
            scope.data = {name: 'sf1'};
            textArea = compile('<span easy-editable-textarea="data.name" buttons="no" e-required></span>')(scope);
            scope.$digest();
            $(document.body).append(textArea);
          };
        });

        it("should create editable textArea", function () {
          compileDirective();
          expect(textArea).toBeDefined();
          expect(textArea[0].classList.contains("editable")).toBeTruthy();
        });

        //TODO: event not fired
        it("should save on blur event", function () {
          compileDirective();
          textArea[0].click();
          expect(textArea[0].classList.contains("editable-hide")).toBeTruthy();
          textArea[0].blur();
          scope.$digest();
        });

        afterEach(function () {
          textArea.remove();
        });
      });

      describe("effectMe", function () {
        var compileDirective, td;

        beforeEach(function () {
          compileDirective = function () {
            scope.effectMe = {sfp1: '0'};
            td = compile('<td effect-me="effectMe.sfp1"></td>')(scope);
            scope.$digest();
          };
        });

        it("should play highligth effect on td", function () {
          compileDirective();
          expect(td).toBeDefined();
          td.scope().effectMe.sfp1 = 1;
          timeout.flush();
          expect(td.queue()).toContain("inprogress");
        });
      });

      describe("focusMe", function () {
        var compileDirective, input;

        it("should set focus on given input (static attribute 'true')", function () {
          compileDirective = function () {
            input = compile('<input focus-me="{{true}}"></td>')(scope);
            scope.$digest();
            $(document.body).append(input);
          };

          compileDirective();
          expect(input).toBeDefined();
          timeout.flush();
          expect(document.activeElement.outerHTML).toBe('<input focus-me="true" class="ng-scope">');
          input.remove();
        });

        //TODO: check watch
        it("should set focus on given input (dynamic attribute)", function () {
          compileDirective = function () {
            scope.focus = true;
            input = compile('<input focus-me="focus"></td>')(scope);
            scope.$digest();
            $(document.body).append(input);
          };

          compileDirective();
          expect(input).toBeDefined();
          timeout.flush();
          expect(document.activeElement.outerHTML).toBe('<input focus-me="focus" class="ng-scope">');
          input.blur();
          expect(document.activeElement.outerHTML).toNotBe('<input focus-me="focus" class="ng-scope">');
          input.remove();
        });
      });

    });
  });
});
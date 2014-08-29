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

      describe('ipAddress', function () {
        var compileDirective, form;

        beforeEach(function () {
          compileDirective = function () {
            scope.data = {ip: null};
            compile('<form name="sfForm"><input type="text" ng-model="data.ip" name="ip" ip-address></form>')(scope);
            scope.$digest();
            form = scope.sfForm;
          };
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
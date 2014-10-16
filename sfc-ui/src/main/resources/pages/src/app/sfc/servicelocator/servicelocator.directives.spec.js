define(['app/sfc/sfc.test.module.loader', 'app/sfc/servicelocator/servicelocator.tpl.html', 'app/sfc/servicelocator/servicelocator.ip.tpl.html', 'app/sfc/servicelocator/servicelocator.lisp.tpl.html',
  'app/sfc/servicelocator/servicelocator.mac.tpl.html', 'app/sfc/servicelocator/servicelocator.selector.tpl.html'], function (sfc) {

  ddescribe('SFC app', function () {
    var scope, compile, provide;

    beforeEach(angular.mock.module('app.sfc'));
    beforeEach(angular.mock.module('src/app/sfc/servicelocator/servicelocator.tpl.html'));
    beforeEach(angular.mock.module('src/app/sfc/servicelocator/servicelocator.ip.tpl.html'));
    beforeEach(angular.mock.module('src/app/sfc/servicelocator/servicelocator.lisp.tpl.html'));
    beforeEach(angular.mock.module('src/app/sfc/servicelocator/servicelocator.mac.tpl.html'));
    beforeEach(angular.mock.module('src/app/sfc/servicelocator/servicelocator.selector.tpl.html'));
    beforeEach(angular.mock.module(function ($provide) {
      provide = $provide;
    }));

    beforeEach(angular.mock.inject(function ($rootScope, $compile) {
      scope = $rootScope.$new();
      compile = $compile;
      this.addMatchers(sfc.customJasmineMatchers);
    }));


    var serviceLocatorConstants =
    {
      transport: ["vxlan-gpe", "gre", "other"],
      type: ["ip", "mac", "lisp"]
    };

    describe('servicelocator.directives', function () {

      describe('serviceLocator', function () {

        var inputElement;
        var compileDirective = function (scope) {
          inputElement = compile('<service-locator id-suffix="_0" locator="locator" constants="serviceLocatorConstants"></service-locator>')(scope);
        };

        it("should compile directive and set locator_type and transport to appropriate value", function () {
          scope.locator = {eid: '10.0.0.1', transport: serviceLocatorConstants.transport[0]};
          scope.serviceLocatorConstants = serviceLocatorConstants;

          compileDirective(scope);
          scope.$digest();

          var formCtrl = inputElement.isolateScope()['f_locator'];
          var locatorTypeInputCtrl = formCtrl['type'];
          var transportInputCtrl = formCtrl['transport'];

          expect(formCtrl.$invalid).toBeFalsy();
          expect(locatorTypeInputCtrl.$modelValue).toEqual(serviceLocatorConstants.type[2]);
          expect(locatorTypeInputCtrl.$viewValue).toEqual(serviceLocatorConstants.type[2]);
          expect(transportInputCtrl.$modelValue).toEqual(serviceLocatorConstants.transport[0]);
          expect(transportInputCtrl.$viewValue).toEqual(serviceLocatorConstants.transport[0]);
        });
      });

      describe('serviceLocatorIp', function () {

        beforeEach(function () {
          this.addMatchers(sfc.customJasmineMatchers);
        });

        var inputElement;

        var compileDirective = function (scope) {
          inputElement = compile('<service-locator-ip locator="service_locator" id-suffix="0" reset-on="service_locator_type_change" not-reset-condition="serviceLocatorConstants.type[0]"></service-locator-ip>')(scope);
        };

        it("should compile directive and set fields to appropriate values", function () {
          scope.service_locator = {ip: '10.0.0.1', port: '500'};
          scope.serviceLocatorConstants = serviceLocatorConstants;

          compileDirective(scope);
          scope.$digest();

          var formCtrl = inputElement.isolateScope()['f_locator_ip'];

          var ipAddrInputCtrl = formCtrl['data_plane_ip'];
          var portInputCtrl = formCtrl['data_plane_port'];

          expect(formCtrl.$invalid).toBeFalsy();
          expect(ipAddrInputCtrl.$modelValue).toEqual('10.0.0.1');
          expect(ipAddrInputCtrl.$viewValue).toEqual('10.0.0.1');
          expect(portInputCtrl.$modelValue).toEqual('500');
          expect(portInputCtrl.$viewValue).toEqual('500');
        });
      });

      describe('serviceLocatorLisp', function () {

        beforeEach(function () {
          this.addMatchers(sfc.customJasmineMatchers);
        });

        var inputElement;

        var compileDirective = function (scope) {
          inputElement = compile('<service-locator-lisp locator="service_locator" id-suffix="0" reset-on="service_locator_type_change" not-reset-condition="serviceLocatorConstants.type[2]"></service-locator-lisp>')(scope);
        };

        it("should compile directive and set fields to appropriate values", function () {
          scope.service_locator = {eid: '10.0.0.1'};
          scope.serviceLocatorConstants = serviceLocatorConstants;

          compileDirective(scope);
          scope.$digest();

          var formCtrl = inputElement.isolateScope()['f_locator_lisp'];

          var eidAddrInputCtrl = formCtrl['data_plane_eid'];

          expect(formCtrl.$invalid).toBeFalsy();
          expect(eidAddrInputCtrl.$modelValue).toEqual('10.0.0.1');
          expect(eidAddrInputCtrl.$viewValue).toEqual('10.0.0.1');

        });
      });

      describe('serviceLocatorMac', function () {

        beforeEach(function () {
          this.addMatchers(sfc.customJasmineMatchers);
        });

        var inputElement;

        var compileDirective = function (scope) {
          inputElement = compile('<service-locator-mac locator="service_locator" id-suffix="0" reset-on="service_locator_type_change" not-reset-condition="serviceLocatorConstants.type[1]"></service-locator-mac>')(scope);
        };

        it("should compile directive and set fields to appropriate values", function () {
          scope.service_locator = {mac: 'aa:bb:cc:dd:ee:ff', 'vlan-id': '10'};
          scope.serviceLocatorConstants = serviceLocatorConstants;

          compileDirective(scope);
          scope.$digest();

          var formCtrl = inputElement.isolateScope()['f_locator_mac'];

          var macAddrInputCtrl = formCtrl['data_plane_mac'];
          var vlanAddrInputCtrl = formCtrl['data_plane_vlan_id'];

          expect(formCtrl.$invalid).toBeFalsy();
          expect(macAddrInputCtrl.$modelValue).toEqual('aa:bb:cc:dd:ee:ff');
          expect(macAddrInputCtrl.$viewValue).toEqual('aa:bb:cc:dd:ee:ff');
          expect(vlanAddrInputCtrl.$modelValue).toEqual('10');
          expect(vlanAddrInputCtrl.$viewValue).toEqual('10');

        });
      });

      describe('serviceLocatorSelector', function () {
        var serviceFunctionSvc;

        beforeEach(function () {
          this.addMatchers(sfc.customJasmineMatchers);
          provide.decorator('ServiceFunctionSvc', function (){
            serviceFunctionSvc = {
              getItem: function (sfName, callback){
                return callback({'sf-data-plane-locator': [{name: 'loc1', eid: '10.0.0.1'}]});
              }
            };
            return serviceFunctionSvc;
          });
        });

        var inputElement;

        var compileDirective = function (scope) {
          inputElement = compile('<service-locator-selector function="function"></service-locator-selector>')(scope);
        };

        it("should compile directive and set fields to appropriate values", function () {
          scope.function = {name: 'sf1', 'sff-sf-data-plane-locator': {eid: '10.0.0.1'}};

          compileDirective(scope);
          scope.$digest();

          var formCtrl = inputElement.isolateScope()['f_sf_locator'];

          var locatorInputCtrl = formCtrl['function_data_plane_locator'];

          expect(formCtrl.$invalid).toBeFalsy();
          expect(locatorInputCtrl.$modelValue).toEqual('loc1');
          expect(locatorInputCtrl.$viewValue).toEqual('loc1');


        });
      });

    });
  });
});
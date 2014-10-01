define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, state, stateParams;
    var serviceLocatorHelperMock, serviceFunctionSvcMock, locator;
    var exampleData = {};

    beforeEach(angular.mock.module('ui.router'));
    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($state, $stateParams, $rootScope) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      state = $state;
      stateParams = $stateParams;
    }));

    beforeEach(function () {
      this.addMatchers(sfc.customJasmineMatchers);
      exampleData.sfs = [
        {"name": "sf1", "type": "firewall", "ip": "10.0.0.1",
          "sf-data-plane-locator": [{"name": "loc1", "service-function-forwarder": "sff1", "ip": '10.0.0.1'}]
        }
      ];
    });

    serviceLocatorHelperMock = {
      getLocatorType: function (serviceLocator) {
        return 'ip';
      },
      locatorToString: function (servicelocator, scope) {
        return 'testLocator';
      },
      getLocatorName: function (locator, locatorsList) {
        return exampleData.sfs[0]['sf-data-plane-locator'][0]['name'];
      }
    };

    serviceFunctionSvcMock = {
      getItem: function(sfName, callback) {
        return callback(exampleData.sfs[0]);
      }
    };

    locator = {
        'ip': 'test',
        'port': 'test',
        'mac': 'test',
        'vlan-id': 'test',
        'eid': 'test'
    };

    describe('servicelocator.controller', function () {

      describe('serviceLocatorCtrl', function () {

        var createServiceLocatorCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceLocatorCtrl = function () {
            return $controller('serviceLocatorCtrl', {$scope: scope, ServiceLocatorHelper: serviceLocatorHelperMock});
          };
        }));

        it("watch should be registered on service_locator and scope.locator_type set appropriately", function () {
          spyOn(serviceLocatorHelperMock, 'getLocatorType').andCallThrough();
          createServiceLocatorCtrl();
          expect(scope.$$watchers[0].exp).toEqual('service_locator');

          //check precondition
          scope.service_locator = undefined;
          scope.$digest();
          expect(scope.locator_type).toBeUndefined();
          expect(serviceLocatorHelperMock.getLocatorType).not.toHaveBeenCalled();

          //check regular function
          scope.service_locator = {name: 'testLocator'};
          scope.$digest();
          expect(scope.locator_type).toEqual('ip');
          expect(serviceLocatorHelperMock.getLocatorType).toHaveBeenCalledWith({name: 'testLocator'});
        });

      });

      describe('serviceLocatorCtrlIp', function () {

        var createServiceLocatorCtrlIp;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceLocatorCtrlIp = function () {
            return $controller('serviceLocatorCtrlIp', {$scope: scope});
          };
        }));

        it("should listen on scope.resetOn event and if matches condition, delete appropriate properties from locator", function () {
          scope.resetOn = 'resetEvent';
          scope.notResetCondition = 'notReset';
          scope['service_locator'] = locator;
          createServiceLocatorCtrlIp();

          scope.$broadcast('resetEvent', 'notReset');
          expect(scope['service_locator']).toContainProperty('ip');
          expect(scope['service_locator']).toContainProperty('port');

          scope.$broadcast('resetEvent', 'whatever');
          expect(scope['service_locator']).not.toContainProperty('ip');
          expect(scope['service_locator']).not.toContainProperty('port');
        });

      });

      describe('serviceLocatorCtrlMac', function () {

        var createServiceLocatorCtrlMac;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceLocatorCtrlMac = function () {
            return $controller('serviceLocatorCtrlMac', {$scope: scope});
          };
        }));

        it("should listen on scope.resetOn event and if matches condition, delete appropriate properties from locator", function () {
          scope.resetOn = 'resetEvent';
          scope.notResetCondition = 'notReset';
          scope['service_locator'] = locator;
          createServiceLocatorCtrlMac();

          scope.$broadcast('resetEvent', 'notReset');
          expect(scope['service_locator']).toContainProperty('mac');
          expect(scope['service_locator']).toContainProperty('vlan-id');

          scope.$broadcast('resetEvent', 'whatever');
          expect(scope['service_locator']).not.toContainProperty('mac');
          expect(scope['service_locator']).not.toContainProperty('vlan-id');
        });

      });

      describe('serviceLocatorCtrlLisp', function () {

        var createServiceLocatorCtrlLisp;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceLocatorCtrlLisp = function () {
            return $controller('serviceLocatorCtrlLisp', {$scope: scope});
          };
        }));

        it("should listen on scope.resetOn event and if matches condition, delete appropriate properties from locator", function () {
          scope.resetOn = 'resetEvent';
          scope.notResetCondition = 'notReset';
          scope['service_locator'] = locator;
          createServiceLocatorCtrlLisp();

          scope.$broadcast('resetEvent', 'notReset');
          expect(scope['service_locator']).toContainProperty('eid');

          scope.$broadcast('resetEvent', 'whatever');
          expect(scope['service_locator']).not.toContainProperty('ied');
        });

      });

      describe('serviceLocatorSelectorCtrl', function () {

        var createServiceLocatorSelectorCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceLocatorSelectorCtrl = function () {
            return $controller('serviceLocatorSelectorCtrl', {$scope: scope, ServiceLocatorHelper: serviceLocatorHelperMock, ServiceFunctionSvc: serviceFunctionSvcMock});
          };
        }));

        it("scope.sfLocators should be initialized as empty array and scope.sfLocatorsAvailable should be undefined", function () {
          createServiceLocatorSelectorCtrl();
          expect(scope.sfLocators).toEqual([]);
          expect(scope.sfLocatorsAvailable).toBeUndefined();
        });

        it("scope.locatorToString should be defined and call ServiceLocatorHelper function", function () {
          spyOn(serviceLocatorHelperMock, 'locatorToString').andCallThrough();
          createServiceLocatorSelectorCtrl();
          var string = scope.locatorToString({name: 'testLocator'});
          expect(string).toEqual('testLocator');
          expect(serviceLocatorHelperMock.locatorToString).toHaveBeenCalledWith({name: 'testLocator'}, scope);
        });

        it("scope.setSffSfLocator be defined should set SF locator inside SFFs function dictionary", function () {
          createServiceLocatorSelectorCtrl();
          var plainSf = {name: 'testSf'};
          scope.sfLocators = exampleData.sfs[0]['sf-data-plane-locator'];

          //regular function (deletes unwanted properties from locator)
          scope.setSffSfLocator(plainSf, 'loc1');
          expect(plainSf['sff-sf-data-plane-locator']).toEqual({'ip': '10.0.0.1'});

          //if no locator found
          var anotherPlainSf = {name: 'testSf'};
          scope.setSffSfLocator(anotherPlainSf, 'nonExistentLocator');
          expect(anotherPlainSf['sff-sf-data-plane-locator']).toEqual({});
        });

        it("watch should be registered on service_function and scope properties set accordingly", function () {
          spyOn(serviceFunctionSvcMock, 'getItem').andCallThrough();
          spyOn(serviceLocatorHelperMock, 'getLocatorName').andCallThrough();
          createServiceLocatorSelectorCtrl();
          expect(scope.$$watchers[0].exp).toEqual('service_function');

          //check precondition
          scope.service_function = {};
          scope.$digest();
          expect(serviceFunctionSvcMock.getItem).not.toHaveBeenCalled();
          expect(scope.sfLocators).toEqual([]);
          expect(scope.sfLocatorsAvailable).toBeUndefined();
          expect(serviceLocatorHelperMock.getLocatorName).not.toHaveBeenCalled();
          expect(scope.selected_locator).toBeUndefined();

          //check regular function
          scope.service_function = {"name": "sf1"};
          scope.$digest();
          expect(serviceFunctionSvcMock.getItem).toHaveBeenCalledWith('sf1', jasmine.any(Function));
          expect(scope.sfLocators).toEqual(exampleData.sfs[0]['sf-data-plane-locator']);
          expect(scope.sfLocatorsAvailable).toEqual('sf1');
          expect(serviceLocatorHelperMock.getLocatorName).toHaveBeenCalledWith(undefined, exampleData.sfs[0]['sf-data-plane-locator']);
          expect(scope.selected_locator).toEqual('loc1');

          //reset spies
          serviceFunctionSvcMock.getItem.reset();
          serviceLocatorHelperMock.getLocatorName.reset();

          //if locator is already available
          scope.service_function = {"name": "sf1", "type": "dpi"};
          scope.$digest();
          expect(scope.sfLocators).toEqual(exampleData.sfs[0]['sf-data-plane-locator']);
          expect(scope.sfLocatorsAvailable).toEqual('sf1');
          expect(serviceFunctionSvcMock.getItem).not.toHaveBeenCalled();
          expect(serviceLocatorHelperMock.getLocatorName).not.toHaveBeenCalled();

          //condition checking unsuccesfull request for sf data
          serviceFunctionSvcMock.getItem = function (sf, callback) {callback(undefined);};
          spyOn(serviceFunctionSvcMock, 'getItem').andCallThrough();
          scope.sfLocators = [];
          scope.sfLocatorsAvailable = undefined;
          scope.selected_locator = undefined;
          scope.service_function = {"name": "sfNonExistent"};
          scope.$digest();
          expect(serviceFunctionSvcMock.getItem).toHaveBeenCalledWith('sfNonExistent', jasmine.any(Function));
          expect(scope.sfLocators).toEqual([]);
          expect(scope.sfLocatorsAvailable).toBeUndefined();
          expect(scope.selected_locator).toBeUndefined();
        });

      });

    });
  });
});

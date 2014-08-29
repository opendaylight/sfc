define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, state, stateParams;
    var serviceFunctionSvcMock, serviceForwarderSvcMock;
    var modalDeleteSvcMock;
    var exampleData = {};

    serviceFunctionSvcMock = {
      getArray: function (callback) {
        return callback(exampleData.sfs);
      },
      deleteItem: function (sfName, callback) {
        exampleData.sfs.splice(0, 1);
        return callback();
      },
      putItem: function (sfName, callback) {
        return callback();
      },
      getItem: function(sfName, callback) {
        return callback(exampleData.sfs[0]);
      }
    };
    serviceForwarderSvcMock = {
      getArray: function (callback) {
        callback(exampleData.sffs);
      }
    };
    modalDeleteSvcMock = {
      open: function (snName, callback) {
        return callback('delete');
      }
    };

    beforeEach(function () {
      exampleData.sfs = [
        {"name": "sf1", "type": "firewall", "ip": "10.0.0.1"}
      ];
      exampleData.sffs = [
        {"name": "sff1", "service-node": "sn1"}
      ];
    });

    beforeEach(angular.mock.module('ui.router'));
    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.common.layout'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($controller, $q, $state, $stateParams, $rootScope) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      state = $state;
      stateParams = $stateParams;
    }));

    beforeEach(angular.mock.inject(function ($controller) {
      return $controller('rootSfcCtrl', {$scope: scope});
    }));

    describe('servicefunction.controller', function () {

      describe("serviceFunctionCtrl", function () {
        var createServiceFunctionCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceFunctionCtrl = function () {
            return $controller('serviceFunctionCtrl', {$scope: scope, ServiceFunctionSvc: serviceFunctionSvcMock, ModalDeleteSvc: modalDeleteSvcMock});
          };
        }));

        it("should call get Service Functions", function () {
          spyOn(serviceFunctionSvcMock, 'getArray').andCallThrough();
          createServiceFunctionCtrl();
          expect(serviceFunctionSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sfs).toEqual(exampleData.sfs);
        });

        it("should open modal dialog and delete service function", function () {
          spyOn(modalDeleteSvcMock, 'open').andCallThrough();
          spyOn(serviceFunctionSvcMock, 'deleteItem').andCallThrough();
          createServiceFunctionCtrl();
          scope.deleteSF(exampleData.sfs[0]);
          expect(modalDeleteSvcMock.open).toHaveBeenCalledWith("sf1", jasmine.any(Function));
          expect(serviceFunctionSvcMock.deleteItem).toHaveBeenCalledWith({"name": "sf1", "type": "firewall", "ip": "10.0.0.1"}, jasmine.any(Function));
          expect(scope.sfs).toEqual([]);
        });

        it("should open modal dialog, dismiss it and not delete service function", function () {
          modalDeleteSvcMock = {
            open: function (snName, callback) {
              return callback('cancel');
            }
          };
          spyOn(modalDeleteSvcMock, 'open').andCallThrough();
          spyOn(serviceFunctionSvcMock, 'deleteItem').andCallThrough();
          createServiceFunctionCtrl();
          scope.deleteSF(exampleData.sfs[0]);
          expect(modalDeleteSvcMock.open).toHaveBeenCalledWith("sf1", jasmine.any(Function));
          expect(serviceFunctionSvcMock.deleteItem).not.toHaveBeenCalledWith({"name": "sf1", "type": "firewall", "ip": "10.0.0.1"}, jasmine.any(Function));
          expect(scope.sfs).toEqual([exampleData.sfs[0]]);
        });

        it("$scope.editSF should be defined and accept paramater", function (){
          createServiceFunctionCtrl();
          spyOn(scope, 'editSF').andCallThrough();
          expect(scope.editSF).toBeDefined();
          scope.editSF(exampleData.sfs[0].name);
          expect(scope.editSF).toHaveBeenCalledWith(exampleData.sfs[0].name);
        });
      });

      describe("serviceFunctionCreateCtrl", function () {
        var createServiceFunctionCreateCtrl;

        beforeEach(function () {
          rootScope.translateLoadingEnd = true;
        });

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceFunctionCreateCtrl = function () {
            return $controller('serviceFunctionCreateCtrl', {$scope: scope, $state: state, ServiceFunctionSvc: serviceFunctionSvcMock, ServiceForwarderSvc: serviceForwarderSvcMock});
          };
        }));

        it("ensure that scope.data variable is properly initialized", function () {
          createServiceFunctionCreateCtrl();
          expect(scope.data).toEqual({"sf-data-plane-locator": {}});
        });

        it("should PUT Service function data to controller and transition to sfc.servicefunction", function () {
          spyOn(serviceFunctionSvcMock, 'putItem').andCallThrough();
          createServiceFunctionCreateCtrl();
          scope.data = exampleData.sfs[0];
          scope.submit();
          expect(serviceFunctionSvcMock.putItem).toHaveBeenCalledWith(scope.data, jasmine.any(Function));
//          rootScope.$digest();
//          expect(state.current.name).toBe('sfc.servicefunction');
        });
      });

      describe("serviceFunctionEditCtrl", function () {
        var createServiceFunctionEditCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceFunctionEditCtrl = function () {
            return $controller('serviceFunctionEditCtrl', {$scope: scope, $state: state, $stateParams: {sfName: exampleData.sfs[0].name},
              ServiceFunctionSvc: serviceFunctionSvcMock, ServiceForwarderSvc: serviceForwarderSvcMock});
          };
        }));

        it("should call get Service Function Forwarders", function () {
          spyOn(serviceForwarderSvcMock, 'getArray').andCallThrough();
          createServiceFunctionEditCtrl();
          expect(serviceForwarderSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sffs).toEqual(exampleData.sffs);
        });

        it("should get Service Function with specified name", function () {
          spyOn(serviceFunctionSvcMock, 'getItem').andCallThrough();
          createServiceFunctionEditCtrl();
//        expect(stateParams.sfName).toBe(exampleData.sfs[0].name);
          stateParams.sfName = exampleData.sfs[0].name;
          expect(serviceFunctionSvcMock.getItem).toHaveBeenCalledWith(stateParams.sfName, jasmine.any(Function));
          expect(scope.data).toEqual(exampleData.sfs[0]);
        });

        it("ensure that scope.data variable is initialized", function () {
          createServiceFunctionEditCtrl();
          expect(scope.data).toBeDefined();
        });

        it("should remove nonExistent SFF from SF DPlocator", function () {
          exampleData.sfs[0]= {'sf-data-plane-locator': {'service-function-forwarder': "SFF1"}};
          createServiceFunctionEditCtrl();
          expect(exampleData.sfs[0]['sf-data-plane-locator']['service-function-forwarder']).toBeUndefined();
        });

        it("should PUT Service Function data to controller and transition to sfc.servicefunction", function () {
          spyOn(serviceFunctionSvcMock, 'putItem').andCallThrough();
          createServiceFunctionEditCtrl();
          scope.data = exampleData.sfs[0];
          scope.sffs = exampleData.sffs;
          scope.submit();
          expect(serviceFunctionSvcMock.putItem).toHaveBeenCalledWith(scope.data, jasmine.any(Function));
//        rootScope.$digest();
//        expect(state.current.name).toBe('sfc.servicenode');
        });

      });
    });
  });
});

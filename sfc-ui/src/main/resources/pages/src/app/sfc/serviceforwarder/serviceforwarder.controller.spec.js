define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, state, stateParams, ngTableParams, filter;
    var serviceFunctionSvcMock, serviceForwarderSvcMock, serviceNodeSvcMock;
    var modalDeleteSvcMock, serviceForwarderHelperMock, serviceLocatorHelperMock;
    var exampleData = {};

    serviceForwarderHelperMock = {
      selectOptions: function () {

      },
      sffInterfaceToString: function() {
//        return "sfi1, sfi2, sfi3";
      },
      sffInterfaceToObjectArray: function (sffInterfaces) {
        return exampleData.functionDictionaryObjectArray[0]['sff-interfaces'];
      },
      sffInterfaceToStringArray: function (sffInterfaces) {
//        return exampleData.functionDictionaryArray[0]['sff-interfaces'];
      },
      sffDpLocatorToString: function (sffDpLocators) {},
      sffFunctionDictionaryToString: function (sffFunctionDicitionary) {},
      removeTemporaryPropertiesFromSf: function (sf) {
        delete sf.nonExistent;
      },
      removeNonExistentSn: function (sff, sns) {} ,
      addLocator: function (scope) {},
      removeLocator: function (index, scope) {},
      addFunction: function (scope) {},
      removeFunction: function (index, scope) {},
      sfChangeListener: function (choosenSf, scope) {},
      dpChangeListener: function (sf, scope) {},
      sfUpdate: function (sf, scope) {}
    };
    serviceFunctionSvcMock = {
      getArray: function (callback) {
        return callback(exampleData.sfs);
      }
    };
    serviceForwarderSvcMock = {
      getArray: function (callback) {
        return callback(exampleData.sffs);
      },
      getItem: function (sffName, callback) {
        return callback(exampleData.sffs[0]);
      },
      deleteItem: function (sffName, callback) {
        return callback();
      },
      putItem: function (sff, callback) {
        return callback();
      }
    };
    serviceNodeSvcMock = {
      getArray: function (callback) {
        return callback(exampleData.sns);
      }
    };
    modalDeleteSvcMock = {
      open: function (sffName, callback) {
        return callback('delete');
      }
    };

    beforeEach(function () {
      exampleData.sfs = [
        {"name": "sf1", "type": "firewall", "ip": "10.0.0.1",
          "sf-data-plane-locator": {"ip": "10.0.0.1", "port": "8000", "name": "dp1"}
        }
      ];
      exampleData.sffs = [
        {"name": "sff1", "service-node": "sn1",
        "sff-data-plane-locator": [
          {
            "data-plane-locator": {}
          }
        ],
        "service-function-dictionary": [
          {
            nonExistent : false,
            "sff-sf-data-plane-locator": {},
            "sff-interfaces": []
          }
        ]}
      ];
      exampleData.sns = [
        {"name": "sn1", "ip": "10.0.0.1", "service-function": ["sf1"]}
      ];
      exampleData.data = {
        "sff-data-plane-locator": [
          {
            "data-plane-locator": {}
          }
        ],
        "service-function-dictionary": [
          {
            nonExistent : false,
            "sff-sf-data-plane-locator": {},
            "sff-interfaces": []
          }
        ]
      };
      exampleData.functionDictionaryArray = [
        {"sff-interfaces": ["sfi1", "sfi2", "sfi3"]}
      ];
      exampleData.functionDictionaryObjectArray = [
        {"sff-interfaces": [
          {"sff-interface": "sfi1"},
          {"sff-interface": "sfi2"},
          {"sff-interface": "sfi3"}
        ]}
      ];
    });

    beforeEach(angular.mock.module('ui.router'));
    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.common.layout'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($controller, $q, $state, $stateParams, $rootScope, $filter, _ngTableParams_) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      state = $state;
      stateParams = $stateParams;
      ngTableParams = _ngTableParams_;
      filter = $filter;
    }));

    beforeEach(angular.mock.inject(function ($controller) {
      return $controller('rootSfcCtrl', {$scope: scope});
    }));

    describe('serviceforwarder.controller', function () {

      describe("serviceForwarderCtrl", function () {
        var createServiceForwarderCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceForwarderCtrl = function () {
            return $controller('serviceForwarderCtrl', {$scope: scope, $state: state, ServiceForwarderSvc: serviceForwarderSvcMock, ServiceForwarderHelper: serviceForwarderHelperMock,
              ServiceLocatorHelper: serviceLocatorHelperMock, ModalDeleteSvc: modalDeleteSvcMock, ngTableParams: ngTableParams, $filter: filter});
          };
        }));

        it("ensure that $scope.sffInterfaceToString function is defined", function () {
          createServiceForwarderCtrl();
          expect(scope.sffInterfaceToString).toBeDefined();
          expect(scope.sffInterfaceToString).toEqual(jasmine.any(Function));
        });

        it("should call get Service Function Forwarders", function () {
          spyOn(serviceForwarderSvcMock, 'getArray').andCallThrough();
          createServiceForwarderCtrl();
          expect(serviceForwarderSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sffs).toEqual(exampleData.sffs);
        });

        it("ensure that scope.tableParams are set", function () {
          createServiceForwarderCtrl();
          expect(scope.tableParams).toBeDefined();
          expect(scope.tableParams.getData).toBeDefined();
          expect(scope.tableParams.$params).toBeDefined();
        });

        it("should open modal dialog and delete SFF", function () {
          spyOn(modalDeleteSvcMock, 'open').andCallThrough();
          spyOn(serviceForwarderSvcMock, 'deleteItem').andCallThrough();
          createServiceForwarderCtrl();
          var dataToDelete = exampleData.sffs[0];
          scope.deleteSFF(exampleData.sffs[0]);
          expect(modalDeleteSvcMock.open).toHaveBeenCalledWith("sff1", jasmine.any(Function));
          expect(serviceForwarderSvcMock.deleteItem).toHaveBeenCalledWith(dataToDelete, jasmine.any(Function));
          expect(scope.sffs).toEqual([]);
        });

        it("should open modal dialog, dismiss it and not delete SFF", function () {
          modalDeleteSvcMock = {
            open: function (sffName, callback) {
              return callback('cancel');
            }
          };
          spyOn(modalDeleteSvcMock, 'open').andCallThrough();
          spyOn(serviceForwarderSvcMock, 'deleteItem').andCallThrough();
          createServiceForwarderCtrl();
          var dataToDelete = exampleData.sffs[0];
          scope.deleteSFF(exampleData.sffs[0]);
          expect(modalDeleteSvcMock.open).toHaveBeenCalledWith("sff1", jasmine.any(Function));
          expect(serviceForwarderSvcMock.deleteItem).not.toHaveBeenCalledWith(dataToDelete, jasmine.any(Function));
          expect(scope.sffs).toEqual([exampleData.sffs[0]]);
        });

        it("$scope.editSFF should be defined and accept paramater", function () {
          createServiceForwarderCtrl();
          spyOn(scope, 'editSFF').andCallThrough();
          expect(scope.editSFF).toBeDefined();
          scope.editSFF(exampleData.sffs[0].name);
          expect(scope.editSFF).toHaveBeenCalledWith(exampleData.sffs[0].name);
        });
      });

      describe("serviceForwarderCreateCtrl", function () {
        var createServiceForwarderCreateCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceForwarderCreateCtrl = function () {
            return $controller('serviceForwarderCreateCtrl', {$scope: scope, $state: state, ServiceNodeSvc: serviceNodeSvcMock, ServiceForwarderSvc: serviceForwarderSvcMock,
              ServiceForwarderHelper: serviceForwarderHelperMock, ServiceFunctionSvc: serviceFunctionSvcMock});
          };
        }));

        it("scope.selectOptions should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'selectOptions').andCallThrough();
          createServiceForwarderCreateCtrl();
          expect(serviceForwarderHelperMock.selectOptions).toHaveBeenCalledWith(scope);
        });

        it("scope.data variable should be properly initialized", function () {
          createServiceForwarderCreateCtrl();
          expect(scope.data).toEqual(exampleData.data);
        });

        it("scope.addLocator function should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'addLocator').andCallThrough();
          createServiceForwarderCreateCtrl();
          expect(scope.addLocator).toBeDefined();
          expect(scope.addLocator).toEqual(jasmine.any(Function));
          scope.addLocator();
          expect(serviceForwarderHelperMock.addLocator).toHaveBeenCalledWith(scope);
        });

        it("scope.removeLocator function should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'removeLocator').andCallThrough();
          createServiceForwarderCreateCtrl();
          expect(scope.removeLocator).toBeDefined();
          expect(scope.removeLocator).toEqual(jasmine.any(Function));
          scope.removeLocator(1);
          expect(serviceForwarderHelperMock.removeLocator).toHaveBeenCalledWith(1, scope);
        });

        it("scope.addFunction function should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'addFunction').andCallThrough();
          createServiceForwarderCreateCtrl();
          expect(scope.addFunction).toBeDefined();
          expect(scope.addFunction).toEqual(jasmine.any(Function));
          scope.addFunction();
          expect(serviceForwarderHelperMock.addFunction).toHaveBeenCalledWith(scope);
        });

        it("scope.removeFunction function should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'removeFunction').andCallThrough();
          createServiceForwarderCreateCtrl();
          expect(scope.removeFunction).toBeDefined();
          expect(scope.removeFunction).toEqual(jasmine.any(Function));
          scope.removeFunction(1);
          expect(serviceForwarderHelperMock.removeFunction).toHaveBeenCalledWith(1, scope);
        });

        it("scope.sfChangeListener function should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'sfChangeListener').andCallThrough();
          createServiceForwarderCreateCtrl();
          expect(scope.sfChangeListener).toBeDefined();
          expect(scope.sfChangeListener).toEqual(jasmine.any(Function));
          scope.sfChangeListener(exampleData.sfs[0]);
          expect(serviceForwarderHelperMock.sfChangeListener).toHaveBeenCalledWith(exampleData.sfs[0], scope);
        });

        it("should call get Service Nodes", function () {
          spyOn(serviceNodeSvcMock, 'getArray').andCallThrough();
          createServiceForwarderCreateCtrl();
          expect(serviceNodeSvcMock.getArray).toHaveBeenCalled();
          expect(scope.sns).toEqual(exampleData.sns);
        });

        it("should call get Service Functions", function () {
          spyOn(serviceFunctionSvcMock, 'getArray').andCallThrough();
          createServiceForwarderCreateCtrl();
          expect(serviceFunctionSvcMock.getArray).toHaveBeenCalled();
          expect(scope.sfs).toEqual(exampleData.sfs);
        });

        it("should reformat SFF function dictionary, PUT it into controller and transition to main.sfc.serviceforwarder", function () {
          spyOn(serviceForwarderSvcMock, 'putItem').andCallThrough();
          spyOn(serviceForwarderHelperMock, 'sffInterfaceToObjectArray').andCallThrough();
          spyOn(serviceForwarderHelperMock, 'removeTemporaryPropertiesFromSf').andCallThrough();
          createServiceForwarderCreateCtrl();
          scope.data = exampleData.sffs[0];
          scope.data['service-function-dictionary'] = exampleData.functionDictionaryArray;
          scope.submit();
          expect(serviceForwarderHelperMock.sffInterfaceToObjectArray).toHaveBeenCalledWith(["sfi1", "sfi2", "sfi3"]);
          expect(serviceForwarderHelperMock.removeTemporaryPropertiesFromSf).toHaveBeenCalledWith(exampleData.sffs[0]['service-function-dictionary'][0]);
          exampleData.sffs[0]['service-function-dictionary'] = exampleData.functionDictionaryObjectArray;
          expect(serviceForwarderSvcMock.putItem).toHaveBeenCalledWith(exampleData.sffs[0], jasmine.any(Function));
//          rootScope.$digest();
//          expect(state.current.name).toBe('main.sfc.serviceforwarder');
        });
      });

      describe("serviceForwarderEditCtrl", function () {
        var createServiceForwarderEditCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceForwarderEditCtrl = function () {
            return $controller('serviceForwarderEditCtrl', {$scope: scope, $state: state, $stateParams: {sffName: exampleData.sffs[0].name},
              ServiceNodeSvc: serviceNodeSvcMock, ServiceForwarderSvc: serviceForwarderSvcMock,
              ServiceForwarderHelper: serviceForwarderHelperMock, ServiceFunctionSvc: serviceFunctionSvcMock});
          };
        }));

        it("scope.selectOptions should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'selectOptions').andCallThrough();
          createServiceForwarderEditCtrl();
          expect(serviceForwarderHelperMock.selectOptions).toHaveBeenCalledWith(scope);
        });

        it("scope.data variable should be properly initialized", function () {
          createServiceForwarderEditCtrl();
          expect(scope.data).toEqual(exampleData.sffs[0]);
        });

        it("scope.addLocator function should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'addLocator').andCallThrough();
          createServiceForwarderEditCtrl();
          expect(scope.addLocator).toBeDefined();
          expect(scope.addLocator).toEqual(jasmine.any(Function));
          scope.addLocator();
          expect(serviceForwarderHelperMock.addLocator).toHaveBeenCalledWith(scope);
        });

        it("scope.removeLocator function should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'removeLocator').andCallThrough();
          createServiceForwarderEditCtrl();
          expect(scope.removeLocator).toBeDefined();
          expect(scope.removeLocator).toEqual(jasmine.any(Function));
          scope.removeLocator(1);
          expect(serviceForwarderHelperMock.removeLocator).toHaveBeenCalledWith(1, scope);
        });

        it("scope.addFunction function should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'addFunction').andCallThrough();
          createServiceForwarderEditCtrl();
          expect(scope.addFunction).toBeDefined();
          expect(scope.addFunction).toEqual(jasmine.any(Function));
          scope.addFunction();
          expect(serviceForwarderHelperMock.addFunction).toHaveBeenCalledWith(scope);
        });

        it("scope.removeFunction function should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'removeFunction').andCallThrough();
          createServiceForwarderEditCtrl();
          expect(scope.removeFunction).toBeDefined();
          expect(scope.removeFunction).toEqual(jasmine.any(Function));
          scope.removeFunction(1);
          expect(serviceForwarderHelperMock.removeFunction).toHaveBeenCalledWith(1, scope);
        });

        it("scope.sfChangeListener function should be defined and call SFF Helper function", function () {
          spyOn(serviceForwarderHelperMock, 'sfChangeListener').andCallThrough();
          createServiceForwarderEditCtrl();
          expect(scope.sfChangeListener).toBeDefined();
          expect(scope.sfChangeListener).toEqual(jasmine.any(Function));
          scope.sfChangeListener(exampleData.sfs[0]);
          expect(serviceForwarderHelperMock.sfChangeListener).toHaveBeenCalledWith(exampleData.sfs[0], scope);
        });

        it("should call get Service Nodes", function () {
          spyOn(serviceNodeSvcMock, 'getArray').andCallThrough();
          createServiceForwarderEditCtrl();
          expect(serviceNodeSvcMock.getArray).toHaveBeenCalled();
          expect(scope.sns).toEqual(exampleData.sns);
        });

        it("should call get Service Functions", function () {
          spyOn(serviceFunctionSvcMock, 'getArray').andCallThrough();
          createServiceForwarderEditCtrl();
          expect(serviceFunctionSvcMock.getArray).toHaveBeenCalled();
          expect(scope.sfs).toEqual(exampleData.sfs);
        });

        it("should call get Service Function Forwarder with sffName and do required actions", function () {
          spyOn(serviceForwarderSvcMock, 'getItem').andCallThrough();
          spyOn(serviceForwarderHelperMock, 'removeNonExistentSn').andCallThrough();
          spyOn(serviceForwarderHelperMock, 'sfUpdate').andCallThrough();
          stateParams.sffName = "sff1";
          createServiceForwarderEditCtrl();
          expect(serviceForwarderSvcMock.getItem).toHaveBeenCalledWith(stateParams.sffName, jasmine.any(Function));
          expect(scope.data).toEqual(exampleData.sffs[0]);
          expect(serviceForwarderHelperMock.removeNonExistentSn).toHaveBeenCalledWith(exampleData.sffs[0], exampleData.sns);
          expect(serviceForwarderHelperMock.sfUpdate).toHaveBeenCalledWith(exampleData.sffs[0]['service-function-dictionary'][0], scope);
        });

        it("should reformat SFF function dictionary, PUT it into controller and transition to main.sfc.serviceforwarder", function () {
          spyOn(serviceForwarderSvcMock, 'putItem').andCallThrough();
          spyOn(serviceForwarderHelperMock, 'sffInterfaceToObjectArray').andCallThrough();
          spyOn(serviceForwarderHelperMock, 'removeTemporaryPropertiesFromSf').andCallThrough();
          createServiceForwarderEditCtrl();
          scope.data = exampleData.sffs[0];
          scope.data['service-function-dictionary'] = exampleData.functionDictionaryArray;
          scope.submit();
          expect(serviceForwarderHelperMock.sffInterfaceToObjectArray).toHaveBeenCalledWith(["sfi1", "sfi2", "sfi3"]);
          expect(serviceForwarderHelperMock.removeTemporaryPropertiesFromSf).toHaveBeenCalledWith(exampleData.sffs[0]['service-function-dictionary'][0]);
          expect(exampleData.sffs[0]['service-function-dictionary']).toEqual(exampleData.functionDictionaryObjectArray);
          expect(serviceForwarderSvcMock.putItem).toHaveBeenCalledWith(exampleData.sffs[0], jasmine.any(Function));
//          rootScope.$digest();
//          expect(state.current.name).toBe('main.sfc.serviceforwarder');
        });
      });

    });
  });
});

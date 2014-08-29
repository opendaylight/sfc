define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  describe('SFC app', function () {
    var rootScope, scope, state, stateParams, ngTableParams, filter;
    var serviceFunctionSvcMock, serviceForwarderSvcMock, serviceNodeSvcMock;
    var modalDeleteSvcMock;
    var exampleData = {};

    serviceFunctionSvcMock = {
      getArray: function (callback) {
        return callback(exampleData.sfs);
      },
      getItem: function (sfName, callback) {
        return callback(exampleData.sfs[0]);
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
        {"name": "sf1", "type": "firewall", "ip": "10.0.0.1"}
      ];
      exampleData.sffs = [
        {"name": "sff1", "service-node": "sn1", "sff-interfaces": [
          {"sff-interface": 'sfi1'},
          {"sff-interface": 'sfi2'},
          {"sff-interface": 'sfi3'}
        ],
        "sff-data-plane-locator": [
          {
            "data-plane-locator": {}
          }
        ],
        "service-function-dictionary": [
          {
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
            "sff-sf-data-plane-locator": {},
            "sff-interfaces": []
          }
        ]
      };
      exampleData.functionDictionaryList = [
        {"sff-interfaces": ["sfi1", "sfi2", "sfi3"]},
        {"sff-interfaces": ["sfi4", "sfi5", "sfi6"]}
      ];
      exampleData.functionDictionaryListObject = [
        {"sff-interfaces": [
          {"sff-interface": "sfi1"},
          {"sff-interface": "sfi2"},
          {"sff-interface": "sfi3"}
        ]},
        {"sff-interfaces": [
          {"sff-interface": "sfi4"},
          {"sff-interface": "sfi5"},
          {"sff-interface": "sfi6"}
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
            return $controller('serviceForwarderCtrl', {$scope: scope, $state: state, ServiceForwarderSvc: serviceForwarderSvcMock, ServiceFunctionSvc: serviceFunctionSvcMock,
              ModalDeleteSvc: modalDeleteSvcMock, ngTableParams: ngTableParams, $filter: filter});
          };
        }));

        it("should call get Service Function Forwarders", function () {
          spyOn(serviceForwarderSvcMock, 'getArray').andCallThrough();
          createServiceForwarderCtrl();
          expect(serviceForwarderSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sffs).toEqual(exampleData.sffs);
        });

        it("ensure that scope.tableParams are set", function () {
          createServiceForwarderCtrl();
          expect(scope.tableParams).toBeDefined();
          expect(scope.tableParams.$params).toBeDefined();
        });

        it("should reformat interfaces array to string", function () {
          createServiceForwarderCtrl();
          var string = scope.sffInterfaceToString(exampleData.sffs[0]['sff-interfaces']);
          expect(string).toBe("sfi1, sfi2, sfi3");
          string = scope.sffInterfaceToString();
          expect(string).toBe("");
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
            return $controller('serviceForwarderCreateCtrl', {$scope: scope, $state: state, ServiceNodeSvc: serviceNodeSvcMock, ServiceForwarderSvc: serviceForwarderSvcMock});
          };
        }));

        it("should call get Service Nodes", function () {
          spyOn(serviceNodeSvcMock, 'getArray').andCallThrough();
          createServiceForwarderCreateCtrl();
          expect(serviceNodeSvcMock.getArray).toHaveBeenCalled();
          expect(scope.sns).toEqual(exampleData.sns);
        });

        it("scope.selectOptions variable should be initialized", function () {
          createServiceForwarderCreateCtrl();
          expect(scope.selectOptions).toBeDefined();
        });

        it("scope.data variable should be properly initialized", function () {
          createServiceForwarderCreateCtrl();
          expect(scope.data).toEqual(exampleData.data);
        });

        it("should add empty data plane locator to SFF", function () {
          createServiceForwarderCreateCtrl();
          expect(scope.data['sff-data-plane-locator'].length).toBe(1);
          scope.addLocator();
          expect(scope.data['sff-data-plane-locator'].length).toBe(2);
          expect(scope.data['sff-data-plane-locator']).toEqual([
            {"data-plane-locator": {}},
            {"data-plane-locator": {}}
          ]);
        });

        it("should remove data plane locator at given index from SFF", function () {
          createServiceForwarderCreateCtrl();
          expect(scope.data['sff-data-plane-locator'].length).toBe(1);
          scope.removeLocator(0);
          expect(scope.data['sff-data-plane-locator'].length).toBe(0);
          expect(scope.data['sff-data-plane-locator']).toEqual([]);
        });

        it("should add empty SF to service function dictionary in SFF", function () {
          createServiceForwarderCreateCtrl();
          expect(scope.data['service-function-dictionary'].length).toBe(1);
          scope.addFunction();
          expect(scope.data['service-function-dictionary'].length).toBe(2);
          expect(scope.data['service-function-dictionary']).toEqual([
            {
              "sff-sf-data-plane-locator": {},
              "sff-interfaces": []
            },
            {
              "sff-sf-data-plane-locator": {},
              "sff-interfaces": []
            }
          ]);
        });

        it("should remove SF from service function dictionary in SFF", function () {
          createServiceForwarderCreateCtrl();
          expect(scope.data['service-function-dictionary'].length).toBe(1);
          scope.removeFunction(0);
          expect(scope.data['service-function-dictionary'].length).toBe(0);
          expect(scope.data['service-function-dictionary']).toEqual([]);
        });

        it("should convert service function dictionaries string array to object array", function () {
          createServiceForwarderCreateCtrl();
          scope.sffInterfaceToObjectArray(exampleData.functionDictionaryList);
          expect(exampleData.functionDictionaryList.length).toBe(2);
          expect(exampleData.functionDictionaryList).toEqual(exampleData.functionDictionaryListObject);
        });

        it("should reformat SFF function dictionary, PUT it into controller and transition to main.sfc.serviceforwarder", function () {
          spyOn(serviceForwarderSvcMock, 'putItem').andCallThrough();
          createServiceForwarderCreateCtrl();
          spyOn(scope, 'sffInterfaceToObjectArray').andCallThrough();
          exampleData.sffs[0]['service-function-dictionary'] = exampleData.functionDictionaryList;
          scope.data = exampleData.sffs[0];
          scope.submit();
          expect(scope.sffInterfaceToObjectArray).toHaveBeenCalledWith(exampleData.functionDictionaryList);
          exampleData.sffs[0]['service-function-dictionary'] = exampleData.functionDictionaryListObject;
          expect(serviceForwarderSvcMock.putItem).toHaveBeenCalledWith(exampleData.sffs[0], jasmine.any(Function));
//          rootScope.$digest();
//          expect(state.current.name).toBe('main.sfc.serviceforwarder');
        });
      });

      describe("serviceForwarderEditCtrl", function () {
        var createServiceForwarderEditCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceForwarderEditCtrl = function () {
            return $controller('serviceForwarderEditCtrl', {$scope: scope, $state: state, ServiceNodeSvc: serviceNodeSvcMock, ServiceForwarderSvc: serviceForwarderSvcMock});
          };
        }));

        it("should call get Service Nodes", function () {
          spyOn(serviceNodeSvcMock, 'getArray').andCallThrough();
          createServiceForwarderEditCtrl();
          expect(serviceNodeSvcMock.getArray).toHaveBeenCalled();
          expect(scope.sns).toEqual(exampleData.sns);
        });

        it("should call get Service Forwarder with specified name", function () {
          spyOn(serviceForwarderSvcMock, 'getItem').andCallThrough();
          stateParams.sffName = exampleData.sffs[0].name;
          createServiceForwarderEditCtrl();
//        expect(stateParams.sffName).toBe(exampleData.sffs[0].name);
          expect(serviceForwarderSvcMock.getItem).toHaveBeenCalledWith(stateParams.sffName, jasmine.any(Function));
          expect(scope.data).toEqual(exampleData.sffs[0]);
        });

        it("scope.selectOptions variable should be initialized", function () {
          createServiceForwarderEditCtrl();
          expect(scope.selectOptions).toBeDefined();
        });

        it("scope.data variable should be loaded with specified SFF", function () {
          createServiceForwarderEditCtrl();
          expect(scope.data).toEqual(exampleData.sffs[0]);
        });

        it("should add empty data plane locator to SFF", function () {
          createServiceForwarderEditCtrl();
          expect(scope.data['sff-data-plane-locator'].length).toBe(1);
          scope.addLocator();
          expect(scope.data['sff-data-plane-locator'].length).toBe(2);
          expect(scope.data['sff-data-plane-locator']).toEqual([
            {"data-plane-locator": {}},
            {"data-plane-locator": {}}
          ]);
        });

        it("should remove data plane locator at given index from SFF", function () {
          createServiceForwarderEditCtrl();
          expect(scope.data['sff-data-plane-locator'].length).toBe(1);
          scope.removeLocator(0);
          expect(scope.data['sff-data-plane-locator'].length).toBe(0);
          expect(scope.data['sff-data-plane-locator']).toEqual([]);
        });

        it("should add empty SF to service function dictionary in SFF", function () {
          createServiceForwarderEditCtrl();
          expect(scope.data['service-function-dictionary'].length).toBe(1);
          scope.addFunction();
          expect(scope.data['service-function-dictionary'].length).toBe(2);
          expect(scope.data['service-function-dictionary']).toEqual([
            {
              "sff-sf-data-plane-locator": {},
              "sff-interfaces": []
            },
            {
              "sff-sf-data-plane-locator": {},
              "sff-interfaces": []
            }
          ]);
        });

        it("should remove SF from service function dictionary in SFF", function () {
          createServiceForwarderEditCtrl();
          expect(scope.data['service-function-dictionary'].length).toBe(1);
          scope.removeFunction(0);
          expect(scope.data['service-function-dictionary'].length).toBe(0);
          expect(scope.data['service-function-dictionary']).toEqual([]);
        });

        it("should convert service function dictionaries string array to object array", function () {
          createServiceForwarderEditCtrl();
          scope.sffInterfaceToObjectArray(exampleData.functionDictionaryList);
          expect(exampleData.functionDictionaryList.length).toBe(2);
          expect(exampleData.functionDictionaryList).toEqual(exampleData.functionDictionaryListObject);
        });

        it("should reformat SFF function dictionary, PUT it into controller and transition to main.sfc.serviceforwarder", function () {
          spyOn(serviceForwarderSvcMock, 'putItem').andCallThrough();
          createServiceForwarderEditCtrl();
          spyOn(scope, 'sffInterfaceToObjectArray').andCallThrough();
          exampleData.sffs[0]['service-function-dictionary'] = exampleData.functionDictionaryList;
          scope.data = exampleData.sffs[0];
          scope.submit();
          expect(scope.sffInterfaceToObjectArray).toHaveBeenCalledWith(exampleData.functionDictionaryList);
          exampleData.sffs[0]['service-function-dictionary'] = exampleData.functionDictionaryListObject;
          expect(serviceForwarderSvcMock.putItem).toHaveBeenCalledWith(exampleData.sffs[0], jasmine.any(Function));
//          rootScope.$digest();
//          expect(state.current.name).toBe('main.sfc.serviceforwarder');
        });
      });

    });
  });
});

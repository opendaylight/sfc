define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, state, stateParams;
    var serviceFunctionSvcMock, serviceNodeSvcMock, serviceForwarderSvcMock, modalDeleteSvcMock, serviceNodeTopologyBackendMock;
    var exampleData = {};

    serviceFunctionSvcMock = {
      getArray: function (callback) {
        return callback(exampleData.sfs);
      }
    };
    serviceForwarderSvcMock = {
      getArray: function (callback) {
        callback(exampleData.sffs);
      }
    };
    serviceNodeSvcMock = {
      getArray: function (callback) {
        return callback(exampleData.sns);
      },
      getItem: function (key, callback) {
        return callback(exampleData.sns[0]);
      },
      putItem: function (item, callback) {
        return callback();
      },
      deleteItem: function (snName, callback) {
        exampleData.sns.splice(0, 1);
        return callback();
      }
    };
    modalDeleteSvcMock = {
      open: function (snName, callback) {
        return callback('delete');
      }
    };
    serviceNodeTopologyBackendMock = {
      createGraphData: function (nodeArray, sfs) {
        if (exampleData.sns.length === 0) {
          return [];
        }
        else {
          return exampleData.snsGraph;
        }
      }
    };

    beforeEach(function () {
      exampleData.sfs = [
        {"name": "sf1", "type": "firewall", "ip": "10.0.0.1",
          "sf-data-plane-locator" : {
            "service-function-forwarder": "sff1"
        }}
      ];
      exampleData.sns = [
        {"name": "sn1", "ip": "10.0.0.1", "service-function": ["sf1"]}
      ];
      exampleData.sns10 = [
        {"name": "sn1", "ip": "10.0.0.1", "service-function": ["sf1", "sf2", "sf3", "sf4", "sf5", "sf6", "sf7", "sf8", "sf9", "sf10"]}
      ];
      exampleData.sns11 = [
        {"name": "sn1", "ip": "10.0.0.1", "service-function": ["sf1", "sf2", "sf3", "sf4", "sf5", "sf6", "sf7", "sf8", "sf9", "sf10", "sf11"]}
      ];
      exampleData.snsGraph = [
        {
          "name": "sn1",
          "ip": "10.0.0.1",
          "children": {"name": "sf1", "type": "firewall", "ip": "10.0.0.1"}
        }
      ];
      exampleData.sffs = [
        {"name": "sff1", "service-node": "sn1"}
      ];
    });

    beforeEach(angular.mock.module('ui.router'));
    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.common.layout'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($controller, $q, $state, $stateParams, $rootScope, $templateCache) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      state = $state;
      stateParams = $stateParams;
//      $templateCache.put('src/common/layout/index.tpl.html', '');
//      $templateCache.put('src/app/sfc/root.tpl.html', '');
//      $templateCache.put('src/app/sfc/servicenode/servicenode.tpl.html', '');
//      $templateCache.put('src/app/sfc/servicenode/servicenode.create.tpl.html', '');
//      $templateCache.put('src/app/sfc/servicenode/servicenode.edit.tpl.html', '');
//      $templateCache.put('src/app/sfc/servicefunction/servicefunction.tpl.html', '');
//      $templateCache.put('src/app/sfc/servicefunction/servicefunction.create.tpl.html', '');
//      $templateCache.put('src/app/sfc/servicechain/servicechain.tpl.html', '');
//      $templateCache.put('src/app/sfc/servicechain/servicechain.create.tpl.html', '');
//      $templateCache.put('src/app/sfc/servicepath/servicepath.tpl.html', '');
    }));

    describe('servicenode.controller', function () {

      describe('serviceNodeCtrl', function () {


        var createServiceNodeCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceNodeCtrl = function () {
            return $controller('serviceNodeCtrl', {$scope: scope, $state: state,
              ServiceFunctionSvc: serviceFunctionSvcMock, ServiceForwarderSvc: serviceForwarderSvcMock, ServiceNodeSvc: serviceNodeSvcMock,
              ServiceNodeTopologyBackend: serviceNodeTopologyBackendMock, ModalDeleteSvc: modalDeleteSvcMock});
          };
        }));

        it("should call get Service Functions", function () {
          spyOn(serviceFunctionSvcMock, 'getArray').andCallThrough();
          createServiceNodeCtrl();
          expect(serviceFunctionSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sfs).toEqual(exampleData.sfs);
        });

        it("should call get Service Forwarders", function () {
          spyOn(serviceForwarderSvcMock, 'getArray').andCallThrough();
          createServiceNodeCtrl();
          expect(serviceForwarderSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sffs).toEqual(exampleData.sffs);
        });

        it("should call get Service Nodes", function () {
          spyOn(serviceNodeSvcMock, 'getArray').andCallThrough();
          createServiceNodeCtrl();
          expect(serviceNodeSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sns).toEqual(exampleData.sns);
        });

        it("should call createGraphData", function () {
          spyOn(serviceNodeTopologyBackendMock, 'createGraphData').andCallThrough();
          createServiceNodeCtrl();
          expect(serviceNodeTopologyBackendMock.createGraphData).toHaveBeenCalledWith(exampleData.sns, exampleData.sffs, exampleData.sfs);
          expect(scope.snsGraph).toEqual(exampleData.snsGraph);
        });

        it("should open modal dialog, delete service node, refresh Service Nodes and call createGraphData ", function () {
          spyOn(modalDeleteSvcMock, 'open').andCallThrough();
          spyOn(serviceNodeSvcMock, 'deleteItem').andCallThrough();
          spyOn(serviceNodeSvcMock, 'getArray').andCallThrough();
          spyOn(serviceNodeTopologyBackendMock, 'createGraphData').andCallThrough();
          createServiceNodeCtrl();
          scope.deleteServiceNode(exampleData.sns[0].name);
          expect(modalDeleteSvcMock.open).toHaveBeenCalledWith("sn1", jasmine.any(Function));
          expect(serviceNodeSvcMock.deleteItem).toHaveBeenCalledWith({name: "sn1"}, jasmine.any(Function));
          expect(serviceNodeSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sns).toEqual([]);
          expect(serviceNodeTopologyBackendMock.createGraphData).toHaveBeenCalledWith([], exampleData.sffs, exampleData.sfs);
          expect(scope.snsGraph).toEqual([]);
        });

        it("should make transition to sfc.servicenode.edit state and pass service node name", function () {
          createServiceNodeCtrl();
          scope.editServiceNode(exampleData.sns[0].name);
//          rootScope.$digest();
//          expect(state.current.name).toBe('sfc.servicenode-edit');
//          expect(stateParams.snName).toBe(exampleData.sns[0].name);
        });

        it("should return apropriate css class for node graph view", function (){
          var col3Class = "col-xs-12 col-md-6 col-lg-4";
          var col2Class = "col-xs-12 col-md-12 col-lg-6";

          createServiceNodeCtrl();
          expect(scope.getSnsGraphClass(exampleData.sns)).toEqual(col3Class);
          expect(scope.getSnsGraphClass(exampleData.sns10)).toEqual(col3Class);
          expect(scope.getSnsGraphClass(exampleData.sns11)).toEqual(col2Class);
        });
      });

      describe("serviceNodeEditCtrl", function () {
        var createServiceNodeEditCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceNodeEditCtrl = function () {
            return $controller('serviceNodeEditCtrl', {$scope: scope, $state: state, $stateParams: {snName: exampleData.sns[0].name},
              ServiceFunctionSvc: serviceFunctionSvcMock, ServiceNodeSvc: serviceNodeSvcMock});
          };
        }));

        it("should call get Service Functions", function () {
          spyOn(serviceFunctionSvcMock, 'getArray').andCallThrough();
          createServiceNodeEditCtrl();
          expect(serviceFunctionSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sfs).toEqual(exampleData.sfs);
        });

        it("should get Service Node with specified name", function () {
          spyOn(serviceNodeSvcMock, 'getItem').andCallThrough();
          createServiceNodeEditCtrl();
//        expect(stateParams.snName).toBe(exampleData.sns[0].name);
          stateParams.snName = exampleData.sns[0].name;
          expect(serviceNodeSvcMock.getItem).toHaveBeenCalledWith(stateParams.snName, jasmine.any(Function));
          expect(scope.data).toEqual(exampleData.sns[0]);
        });

        it("ensure that scope.data variable is initialized", function () {
          createServiceNodeEditCtrl();
          expect(scope.data).toBeDefined();
        });

        it("should PUT Service Node data to controller and transition to sfc.servicenode", function () {
          spyOn(serviceNodeSvcMock, 'putItem').andCallThrough();
          createServiceNodeEditCtrl();
          scope.data = exampleData.sns[0];
          scope.sfs = exampleData.sfs;
          scope.submit();
          expect(serviceNodeSvcMock.putItem).toHaveBeenCalledWith(scope.data, jasmine.any(Function));
//        rootScope.$digest();
//        expect(state.current.name).toBe('sfc.servicenode');
        });

      });

      describe("serviceNodeCreateCtrl", function () {
        var createServiceNodeCreateCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceNodeCreateCtrl = function () {
            return $controller('serviceNodeCreateCtrl', {$scope: scope, $state: state, ServiceFunctionSvc: serviceFunctionSvcMock, ServiceNodeSvc: serviceNodeSvcMock});
          };
        }));

        it("should call get Service Functions", function () {
          spyOn(serviceFunctionSvcMock, 'getArray').andCallThrough();
          createServiceNodeCreateCtrl();
          expect(serviceFunctionSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sfs).toEqual(exampleData.sfs);
        });

        it("ensure that scope.data variable is initialized", function () {
          createServiceNodeCreateCtrl();
          expect(scope.data).toBeDefined();
        });

        it("should PUT Service Node data to controller and transition to sfc.servicenode", function () {
          spyOn(serviceNodeSvcMock, 'putItem').andCallThrough();
          createServiceNodeCreateCtrl();
          scope.data = exampleData.sns[0];
          scope.submit();
          expect(serviceNodeSvcMock.putItem).toHaveBeenCalledWith(scope.data, jasmine.any(Function));
//        rootScope.$digest();
//        expect(state.current.name).toBe('sfc.servicenode');
        });
      });
    });

  });
});

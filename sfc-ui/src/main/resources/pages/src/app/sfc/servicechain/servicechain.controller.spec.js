define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, state, stateParams, modalInstance, compile;
    var serviceFunctionSvcMock, serviceChainSvcMock, servicePathSvcMock;
    var modalDeleteSvcMock, modalSfNameSvcMock, modalSfpInstantiateSvc, modalInfoSvcMock, modalErrorSvcMock;
    var exampleData = {};
    var sfcState = {PERSISTED: "persisted", NEW: "new", EDITED: "edited"};

    serviceFunctionSvcMock = {
      getArray: function (callback) {
        return callback(exampleData.sfs);
      }
    };
    serviceChainSvcMock = {
      getArray: function (callback) {
        callback(exampleData.sfcs);
      },
      deleteItem: function (sfcName, callback) {
        exampleData.sfcs.splice(0, 1);
        return callback();
      },
      putItem: function (sfcName, callback) {
        return callback();
      },
      getItem: function (sfpName, callback) {
        return callback(exampleData.sfcs[0]);
      }
    };
    servicePathSvcMock = {
      putItem: function (sfpName, callback) {
        return callback();
      }
    };
    modalDeleteSvcMock = {
      open: function (snName, callback) {
        return callback('delete');
      }
    };
    modalSfNameSvcMock = {
      open: function (sfc, sf, callback) {
        return callback({"name": "egress-firewall", "type": "firewall"});
      }
    };
    modalSfpInstantiateSvc = {
      open: function (sfc, callback) {
        return callback({name: "testSfp", "service-chain-name": sfc.name});
      }
    };
    modalInfoSvcMock = {
      open: function (info) {

      }
    };
    modalErrorSvcMock = {
      open: function (error) {

      }
    };

    beforeEach(function () {
      exampleData.sfs = [
        {"name": "sf1", "type": "firewall", "ip": "10.0.0.1"}
      ];
      exampleData.sfcs = [
        {"name": "sfc1", "sfc-service-function": [
          {"name": "firewall", type: "firewall"}
        ]}
      ];
      exampleData.sfcsPreLoad = [
        {"name": "sfcToBeDeleted", "sfc-service-function": [
          {"name": "firewall", type: "firewall"}
        ]},
        {"name": "sfc2", "state": "edited", "sfc-service-function": [
          {"name": "firewall", type: "firewall"}
        ]}
      ];
      exampleData.sfcsWithTemporary = [
        {"name": "sfc2", "state": "edited", "sfc-service-function": [
          {"name": "firewall", type: "firewall"}
        ]},
        {"name": "sfc1", "state": "persisted", "sfc-service-function": [
          {"name": "firewall", type: "firewall"}
        ]}
      ];
    });

    beforeEach(angular.mock.module('ui.router'));
    beforeEach(angular.mock.module('pascalprecht.translate'));
    beforeEach(angular.mock.module('app.common.layout'));
    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($controller, $q, $state, $stateParams, $rootScope, $compile) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
      state = $state;
      stateParams = $stateParams;
      modalInstance = {
        close: jasmine.createSpy('modalInstance.close'),
        dismiss: jasmine.createSpy('modalInstance.dismiss'),
        result: {
          then: jasmine.createSpy('modalInstance.result.then')
        }
      };
      compile = $compile;
    }));

    beforeEach(angular.mock.inject(function ($controller) {
      return $controller('rootSfcCtrl', {$scope: scope});
    }));

    describe('servicechain.controller', function () {

      describe("serviceChainCtrl", function () {
        var createServiceChainCtrl /*,compileNgTable*/;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceChainCtrl = function () {
            return $controller('serviceChainCtrl', {$scope: scope, $rootScope: rootScope, ServiceFunctionSvc: serviceFunctionSvcMock,
              ServiceChainSvc: serviceChainSvcMock, ServicePathSvc: servicePathSvcMock,
              ModalDeleteSvc: modalDeleteSvcMock, ModalInfoSvc: modalInfoSvcMock, ModalErrorSvc: modalErrorSvcMock,
              ModalSfNameSvc: modalSfNameSvcMock, ModalSfpInstantiateSvc: modalSfpInstantiateSvc});
          };

//          compileNgTable = function () {
//            var element = compile('<table ng-table="tableParams"></table>')(scope);
//            scope.$digest();
//            console.log(element);
//          };
        }));

        it("ensure that scope.tableParamsSfType are set", function () {
          createServiceChainCtrl();
          expect(scope.tableParamsSfType).toBeDefined();
          expect(scope.tableParamsSfType.getData).toBeDefined();
          expect(scope.tableParamsSfType.$params).toBeDefined();
        });

        it("ensure that scope.tableParams are set", function () {
          createServiceChainCtrl();
          expect(scope.tableParams).toBeDefined();
          expect(scope.tableParams.getData).toBeDefined();
          expect(scope.tableParams.$params).toBeDefined();
        });

        it("ensure that this.sfcsWatcherRegistered variable is defined", function () {
          var thisCtrl = createServiceChainCtrl();
          expect(thisCtrl.sfcsWatcherRegistered).toBeDefined();
        });

        it("ensure that this.registerSfcsWatcher function is defined and registers watchCollection on sfcs", function () {
          spyOn(scope, '$watchCollection').andCallThrough();
          var thisCtrl = createServiceChainCtrl();
          scope.tableParams.settings().$scope = scope;
          expect(thisCtrl.registerSfcsWatcher).toEqual(jasmine.any(Function));
          expect(scope.$watchCollection).toHaveBeenCalledWith('sfcs', jasmine.any(Function));

          //test watchCollection with valid data
          spyOn(scope.tableParams, 'total').andCallThrough();
          spyOn(scope.tableParams, 'reload').andCallThrough();
          scope.sfcs = exampleData.sfcs;
          scope.$digest();
          expect(scope.tableParams.total).toHaveBeenCalledWith(1);
          expect(scope.tableParams.reload).toHaveBeenCalled();

          //test watchCollection with undefined
          scope.sfcs = undefined;
          scope.$digest();
        });

        it("ensure that scope.effectMe object is initialized", function () {
          createServiceChainCtrl();
          expect(scope.sfcEffectMe).toBeDefined();
        });

        it("ensure that (root)scope.sfcs variable is initialized", function () {
          createServiceChainCtrl();
          expect(rootScope.sfcs).toBeDefined();
          expect(scope.sfcs).toBeDefined();
        });

        it("ensure that scope.sortableOptions object is properly initialized", function () {
          createServiceChainCtrl();
          expect(scope.sortableOptions).toBeDefined();
          expect(scope.sortableOptions.start).toEqual(jasmine.any(Function));
          expect(scope.sortableOptions.helper).toEqual(jasmine.any(Function));
          expect(scope.sortableOptions.update).toEqual(jasmine.any(Function));
        });

        it("should call get Service Functions", function () {
          spyOn(serviceFunctionSvcMock, 'getArray').andCallThrough();
          createServiceChainCtrl();
          expect(serviceFunctionSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sfs).toEqual(exampleData.sfs);
        });

        it("should call get Service Chains, keep temporary and remove persisted data", function () {
          spyOn(serviceChainSvcMock, 'getArray').andCallThrough();
          rootScope.sfcs = exampleData.sfcsPreLoad;
          expect(scope.sfcs).toEqual(exampleData.sfcsPreLoad);
          createServiceChainCtrl();
          expect(serviceChainSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(rootScope.sfcs).toEqual(exampleData.sfcsWithTemporary);
          expect(scope.sfcs).toEqual(exampleData.sfcsWithTemporary);
        });

        it("should return current state of SFC", function () {
          createServiceChainCtrl();
          expect(scope.getSFCstate(exampleData.sfcs[0])).toBe(sfcState.PERSISTED);
          exampleData.sfcs[0].state = sfcState.EDITED;
          expect(scope.getSFCstate(exampleData.sfcs[0])).toBe(sfcState.EDITED);
        });

        it("should set SFC to proper state", function () {
          createServiceChainCtrl();
          scope.setSFCstate(exampleData.sfcs[0], sfcState.EDITED);
          expect(exampleData.sfcs[0].state).toBe(sfcState.EDITED);
          scope.setSFCstate(exampleData.sfcs[0], sfcState.NEW);
          expect(exampleData.sfcs[0].state).toBe(sfcState.NEW);
          scope.setSFCstate(exampleData.sfcs[0], sfcState.PERSISTED);
          expect(exampleData.sfcs[0].state).toBe(sfcState.NEW);
        });

        it("should check if SFC is in given state", function () {
          createServiceChainCtrl();
          //should return persisted even if there is not state set
          expect(scope.isSFCstate(exampleData.sfcs[0], sfcState.PERSISTED)).toBeTruthy();
          exampleData.sfcs[0].state = sfcState.PERSISTED;
          expect(scope.isSFCstate(exampleData.sfcs[0], sfcState.PERSISTED)).toBeTruthy();
          exampleData.sfcs[0].state = sfcState.EDITED;
          expect(scope.isSFCstate(exampleData.sfcs[0], sfcState.EDITED)).toBeTruthy();
          exampleData.sfcs[0].state = sfcState.NEW;
          expect(scope.isSFCstate(exampleData.sfcs[0], sfcState.NEW)).toBeTruthy();
          expect(scope.isSFCstate(exampleData.sfcs[0], sfcState.PERSISTED)).toBeFalsy();
        });

        it("should unset SFC state", function () {
          createServiceChainCtrl();
          exampleData.sfcs[0].state = sfcState.NEW;
          scope.unsetSFCstate(exampleData.sfcs[0]);
          expect(exampleData.sfcs[0].state).toBeUndefined();
        });


        it("should add SF into SFC['sfc-service-function']  (non-existent SF)", function () {
          createServiceChainCtrl();
          var emptySfc = {"name": "test SFC"};
          scope.onSFCdrop(exampleData.sfs[0], emptySfc);
          expect(emptySfc['sfc-service-function']).toBeDefined();
          expect(emptySfc['sfc-service-function'][0]).toEqual({"name": exampleData.sfs[0].type, "type": exampleData.sfs[0].type});
          expect(emptySfc.state).toBe(sfcState.EDITED);
        });

        it("should add SF into SFC['sfc-service-function']  (existent SF)", function () {
          spyOn(modalSfNameSvcMock, 'open').andCallThrough();
          createServiceChainCtrl();
          scope.onSFCdrop(exampleData.sfs[0], exampleData.sfcs[0]);
          expect(exampleData.sfcs[0]['sfc-service-function']).toBeDefined();
          expect(modalSfNameSvcMock.open).toHaveBeenCalledWith(exampleData.sfcs[0], exampleData.sfs[0], jasmine.any(Function));
          expect(exampleData.sfcs[0]['sfc-service-function'][1]).toEqual({"name": "egress-firewall", "type": exampleData.sfs[0].type});
          expect(exampleData.sfcs[0].state).toBe(sfcState.EDITED);
        });

        it("should open modal dialog and delete SFC from controller and scope", function () {
          spyOn(serviceChainSvcMock, 'deleteItem').andCallThrough();
          spyOn(modalDeleteSvcMock, 'open').andCallThrough();
          createServiceChainCtrl();
          rootScope.sfcs = exampleData.sfcs;
          expect(scope.sfcs).toEqual(exampleData.sfcs);
          scope.deleteSFC(rootScope.sfcs[0]);
          expect(modalDeleteSvcMock.open).toHaveBeenCalledWith("sfc1", jasmine.any(Function));
          expect(serviceChainSvcMock.deleteItem).toHaveBeenCalledWith({
              "name": "sfc1",
              "sfc-service-function": [
                {"name": "firewall", type: "firewall"}
              ], state: 'persisted'},
            jasmine.any(Function));
          expect(rootScope.sfcs).toEqual([]);
          expect(scope.sfcs).toEqual([]);
          expect(exampleData.sfcs).toEqual([]);
        });

        it("should remove SF from SFC", function () {
          createServiceChainCtrl();
          scope.removeSFfromSFC(exampleData.sfcs[0], 0);
          expect(exampleData.sfcs[0]['sfc-service-function']).toEqual([]);
          expect(exampleData.sfcs[0].state).toBe(sfcState.EDITED);
        });

        it("should PUT SFC to controller", function () {
          spyOn(serviceChainSvcMock, 'putItem').andCallThrough();
          createServiceChainCtrl();
          scope.persistSFC(exampleData.sfcs[0]);
          expect(exampleData.sfcs[0].state).toBeUndefined();
          expect(serviceChainSvcMock.putItem).toHaveBeenCalledWith(exampleData.sfcs[0], jasmine.any(Function));
        });

        it("should UNDO SFC creation", function () {
          createServiceChainCtrl();
          rootScope.sfcs = exampleData.sfcs;
          expect(scope.sfcs).toEqual(exampleData.sfcs);
          scope.undoSFCnew(exampleData.sfcs[0]);
          expect(rootScope.sfcs).toEqual([]);
          expect(scope.sfcs).toEqual([]);
        });

        it("should UNDO changes in SFC", function () {
          spyOn(serviceChainSvcMock, 'getItem').andCallThrough();
          createServiceChainCtrl();
          rootScope.sfcs = exampleData.sfcs;
          rootScope.sfcs[0].change = "someChanges";
          expect(scope.sfcs).toEqual(rootScope.sfcs);
          scope.undoSFCchanges(exampleData.sfcs[0]);
          expect(serviceChainSvcMock.getItem).toHaveBeenCalledWith(exampleData.sfcs[0].name, jasmine.any(Function));
          expect(rootScope.sfcs[0]).toEqual(exampleData.sfcs[0]);
          expect(scope.sfcs[0]).toEqual(exampleData.sfcs[0]);
        });

        it("should open modal dialog, let user type in SFP name, PUT this SFP to controller and open info dialog", function () {
          spyOn(modalSfpInstantiateSvc, 'open').andCallThrough();
          spyOn(servicePathSvcMock, 'putItem').andCallThrough();
          spyOn(modalInfoSvcMock, 'open').andCallThrough();
          createServiceChainCtrl();
          scope.deploySFC({name: "testSfc"});
          expect(modalSfpInstantiateSvc.open).toHaveBeenCalledWith({name: "testSfc"}, jasmine.any(Function));
          expect(servicePathSvcMock.putItem).toHaveBeenCalledWith({name: "testSfp", "service-chain-name": "testSfc"}, jasmine.any(Function));
          expect(modalInfoSvcMock.open).toHaveBeenCalled();
          expect(rootScope.sfpEffectMe["testSfp"]).toBe(1);
        });

        it("should open modal dialog, let user type in SFP name, fail PUT this SFP to controller and open error dialog", function () {
          servicePathSvcMock = {
            putItem: function (sfpName, callback) {
              return callback('error');
            }
          };
          spyOn(modalSfpInstantiateSvc, 'open').andCallThrough();
          spyOn(servicePathSvcMock, 'putItem').andCallThrough();
          spyOn(modalErrorSvcMock, 'open').andCallThrough();
          createServiceChainCtrl();
          scope.deploySFC({name: "testSfc"});
          expect(modalSfpInstantiateSvc.open).toHaveBeenCalledWith({name: "testSfc"}, jasmine.any(Function));
          expect(servicePathSvcMock.putItem).toHaveBeenCalledWith({name: "testSfp", "service-chain-name": "testSfc"}, jasmine.any(Function));
          expect(modalErrorSvcMock.open).toHaveBeenCalled();
        });
      });

      describe("serviceChainCreateCtrl", function () {
        var createServiceChainCreateCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServiceChainCreateCtrl = function () {
            return $controller('serviceChainCreateCtrl', {$scope: scope, $state: state});
          };
        }));

        it("ensure that scope.data is initialized", function () {
          createServiceChainCreateCtrl();
          expect(scope.data).toBeDefined();
        });

        it("this.symmetricToBoolean should convert symmetric string to bool", function () {
          var thisCtrl = createServiceChainCreateCtrl();
          exampleData.sfcs[0].symmetric = 'true';
          thisCtrl.symmetricToBoolean(exampleData.sfcs[0]);
          expect(exampleData.sfcs[0].symmetric === true).toBeTruthy();
          exampleData.sfcs[0].symmetric = 'false';
          thisCtrl.symmetricToBoolean(exampleData.sfcs[0]);
          expect(exampleData.sfcs[0].symmetric === true).toBeFalsy();
          expect(exampleData.sfcs[0].symmetric === false).toBeTruthy();
        });

        it("should create SFC, set is as new and transition to sfc.servicechain", function () {
          createServiceChainCreateCtrl();
//          state.transitionTo('sfc.servicechain.create');
//          rootScope.$digest();
//          expect(state.current.name).toBe('sfc.servicechain.create');
          scope.data.name = exampleData.sfcs[0].name;
          scope.submit();
          expect(scope.data).toBeDefined();
          expect(scope.data).toEqual({"sfc-service-function": [], "state": "new", "name": exampleData.sfcs[0].name});
          expect(rootScope.sfcs[0]).toEqual(scope.data);
          expect(scope.sfcs[0]).toEqual(scope.data);
//          rootScope.$digest();
//          expect(state.current.name).toBe('sfc.servicechain');
        });
      });

      describe("ModalSfpInstantiateCtrl", function () {
        var createModalSfpInstantiateCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createModalSfpInstantiateCtrl = function () {
            return $controller('ModalSfpInstantiateCtrl', {$scope: scope, $modalInstance: modalInstance, sfc: exampleData.sfcs[0]});
          };
        }));

        it("ensure that $scope.sfc contains sfc parameter", function () {
          createModalSfpInstantiateCtrl();
          expect(scope.sfc).toBe(exampleData.sfcs[0]);
        });

        it("ensure that $scope.data is properly initialized", function () {
          createModalSfpInstantiateCtrl();
          expect(scope.data).toBeDefined();
          expect(scope.data.name).toBe(exampleData.sfcs[0].name + "-");
        });

        it("ensure that scope.save and scope.dismiss functions are defined", function () {
          createModalSfpInstantiateCtrl();
          expect(scope.save).toBeDefined();
          expect(scope.dismiss).toBeDefined();
        });

        it("should close modalInstance with new SFP name", function () {
          createModalSfpInstantiateCtrl();
          scope.save();
          expect(modalInstance.close).toHaveBeenCalledWith({'name': exampleData.sfcs[0].name + "-", 'service-chain-name': exampleData.sfcs[0].name});
        });

        it("should dismiss modalInstance with 'cancel' reason", function () {
          createModalSfpInstantiateCtrl();
          scope.dismiss();
          expect(modalInstance.dismiss).toHaveBeenCalledWith('cancel');
        });
      });

      describe("ModalSfNameCtrl", function () {
        var createModalSfNameCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createModalSfNameCtrl = function () {
            return $controller('ModalSfNameCtrl', {$scope: scope, $modalInstance: modalInstance, sfc: exampleData.sfcs[0], sf: exampleData.sfs[0]});
          };
        }));

        it("ensure that $scope.sfc contains sfc parameter", function () {
          createModalSfNameCtrl();
          expect(scope.sfc).toBe(exampleData.sfcs[0]);
        });

        it("ensure that $scope.sf contains sf parameter", function () {
          createModalSfNameCtrl();
          expect(scope.sf).toBe(exampleData.sfs[0]);
        });

        it("ensure that scope.save and scope.dismiss functions are defined", function () {
          createModalSfNameCtrl();
          expect(scope.save).toBeDefined();
          expect(scope.dismiss).toBeDefined();
        });

        it("should close modalInstance with new SF abstract name", function () {
          createModalSfNameCtrl();
          scope.data = {prefix: 'prefix', sufix: 'sufix'};
          scope.save();
          //exampleData.sfs[0].name with prefix and sufix
          expect(modalInstance.close).toHaveBeenCalledWith(exampleData.sfs[0]);
        });

        it("should dismiss modalInstance with 'cancel' reason", function () {
          createModalSfNameCtrl();
          scope.dismiss();
          expect(modalInstance.dismiss).toHaveBeenCalledWith('cancel');
        });
      });
    });
  });
});

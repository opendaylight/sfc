define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope, state, stateParams, modalInstance;
    var serviceFunctionSvcMock, servicePathSvcMock, servicePathHelperMock, modalDeleteSvcMock, serviceForwarderSvcMock, servicePathModalSffSelectMock;
    var exampleData = {};
    var sfpState = {PERSISTED: "persisted", NEW: "new", EDITED: "edited"};

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
      }
    };
    servicePathHelperMock = {
      orderHopsInSFP: function (sfp) {},
      updateHopsOrderInSFP: function (sfp) {},
      updateStartingIndexOfSFP: function (sfp) {}
    };
    servicePathSvcMock = {
      getArray: function (callback) {
        callback(exampleData.sfps);
      },
      deleteItem: function (sfpName, callback) {
        exampleData.sfps.splice(0, 1);
        return callback();
      },
      putItem: function (sfpName, callback) {
        return callback();
      },
      getItem: function (sfpName, callback) {
        return callback(exampleData.sfps[0]);
      }
    };
    modalDeleteSvcMock = {
      open: function (snName, callback) {
        return callback('delete');
      }
    };
    serviceForwarderSvcMock = {
      getArray: function (callback) {
        callback(exampleData.sffs);
      }
    };
    servicePathModalSffSelectMock = {
      open: function (sfName, sffNameList, callback) {
        return callback({name :sffNameList[0]});
      }
    };


    beforeEach(function () {
      exampleData.sfs = [
        {"name": "sf1", "type": "firewall", "ip": "10.0.0.1"}
      ];
      exampleData.sfsWithNumberOfSffs = [
        {"name": "sf1", "type": "firewall", "ip": "10.0.0.1", 'sf-data-plane-locator': [
          {'service-function-forwarder': 'sff1'}, {'service-function-forwarder': 'sff2'}
        ]}
      ];
      exampleData.sfps = [
        {"name": "sfp1", "service-path-hop": [
          {"hop-number": 1, "service-function-name": "sf1"}
        ]}
      ];
      exampleData.sfpsPreLoad = [
        {"name": "sfpToBeDeleted", "service-path-hop": [
          {"hop-number": 1, "service-function-name": "sf1"}
        ]},
        {"name": "sfp2", "state": "edited", "service-path-hop": [
          {"hop-number": 2, "service-function-name": "sf2"}
        ]}
      ];
      exampleData.sfpsWithTemporary = [
        {"name": "sfp2", "state": "edited", "service-path-hop": [
          {"hop-number": 2, "service-function-name": "sf2"}
        ]},
        {"name": "sfp1", "state": "persisted", "service-path-hop": [
          {"hop-number": 1, "service-function-name": "sf1"}
        ]}
      ];
      exampleData.sffs = [
        {name: "sff1"},
        {name: "sff2"}
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
      modalInstance = {
        close: jasmine.createSpy('modalInstance.close'),
        dismiss: jasmine.createSpy('modalInstance.dismiss'),
        result: {
          then: jasmine.createSpy('modalInstance.result.then')
        }
      };
    }));

    beforeEach(angular.mock.inject(function ($controller) {
      return $controller('rootSfcCtrl', {$scope: scope});
    }));

    describe('servicepath.controller', function () {

      describe("servicePathCtrl", function () {
        var createServicePathCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServicePathCtrl = function () {
            return $controller('servicePathCtrl', {$scope: scope, $rootScope: rootScope, ServiceFunctionSvc: serviceFunctionSvcMock, ServiceForwarderSvc: serviceForwarderSvcMock,
              ServicePathSvc: servicePathSvcMock, ServicePathHelper: servicePathHelperMock, ServicePathModalSffSelect: servicePathModalSffSelectMock, ModalDeleteSvc: modalDeleteSvcMock});
          };
        }));

        it("ensure that scope.tableParamsSfName are set", function () {
          createServicePathCtrl();
          expect(scope.tableParamsSfName).toBeDefined();
          expect(scope.tableParamsSfName.getData).toBeDefined();
          expect(scope.tableParamsSfName.$params).toBeDefined();
        });

        it("ensure that scope.tableParamsSffName are set", function () {
          createServicePathCtrl();
          expect(scope.tableParamsSffName).toBeDefined();
          expect(scope.tableParamsSffName.getData).toBeDefined();
          expect(scope.tableParamsSffName.$params).toBeDefined();
        });

        it("ensure that scope.tableParams are set", function () {
          createServicePathCtrl();
          expect(scope.tableParams).toBeDefined();
          expect(scope.tableParams.getData).toBeDefined();
          expect(scope.tableParams.$params).toBeDefined();
        });

        it("ensure that this.sfpsWatcherRegistered variable is defined", function () {
          var thisCtrl = createServicePathCtrl();
          expect(thisCtrl.sfpsWatcherRegistered).toBeDefined();
        });

        it("ensure that this.registerSfpsWatcher function is defined and registers watchCollection on sfps", function () {
          spyOn(scope, '$watchCollection').andCallThrough();
          var thisCtrl = createServicePathCtrl();
          scope.tableParams.settings().$scope = scope;
          expect(thisCtrl.registerSfpsWatcher).toEqual(jasmine.any(Function));
          expect(scope.$watchCollection).toHaveBeenCalledWith('sfps', jasmine.any(Function));

          //test watchCollection with valid data
          spyOn(scope.tableParams, 'total').andCallThrough();
          spyOn(scope.tableParams, 'reload').andCallThrough();
          scope.sfps = exampleData.sfps;
          scope.$digest();
          expect(scope.tableParams.total).toHaveBeenCalledWith(1);
          expect(scope.tableParams.reload).toHaveBeenCalled();

          //test watchCollection with undefined
          scope.sfps = undefined;
          scope.$digest();
        });

        it("ensure that (root)scope.sfpEffectMe object is initialized", function () {
          createServicePathCtrl();
          expect(rootScope.sfpEffectMe).toBeDefined();
          expect(scope.sfpEffectMe).toBeDefined();
        });

        it("ensure that (root)scope.sfps variable is initialized", function () {
          createServicePathCtrl();
          expect(rootScope.sfps).toBeDefined();
          expect(scope.sfps).toBeDefined();
        });

        it("ensure that scope.sortableOptions variable is initialized", function () {
          createServicePathCtrl();
          expect(scope.sortableOptions).toBeDefined();
        });

        it("should call get Service Functions", function () {
          spyOn(serviceFunctionSvcMock, 'getArray').andCallThrough();
          createServicePathCtrl();
          expect(serviceFunctionSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sfs).toEqual(exampleData.sfs);
        });

        it("should call get Service Forwarders", function () {
          spyOn(serviceForwarderSvcMock, 'getArray').andCallThrough();
          createServicePathCtrl();
          expect(serviceForwarderSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(scope.sffs).toEqual(exampleData.sffs);
        });

        it("should call get Service Paths, keep temporary and remove persisted data", function () {
          spyOn(servicePathSvcMock, 'getArray').andCallThrough();
          rootScope.sfps = exampleData.sfpsPreLoad;
          expect(scope.sfps).toEqual(exampleData.sfpsPreLoad);
          createServicePathCtrl();
          expect(servicePathSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));
          expect(rootScope.sfps).toEqual(exampleData.sfpsWithTemporary);
          expect(scope.sfps).toEqual(exampleData.sfpsWithTemporary);
        });

        it("should return current state of SFP", function () {
          createServicePathCtrl();
          expect(scope.getSFPstate(exampleData.sfps[0])).toBe(sfpState.PERSISTED);
          exampleData.sfps[0].state = sfpState.EDITED;
          expect(scope.getSFPstate(exampleData.sfps[0])).toBe(sfpState.EDITED);
        });

        it("should set SFP to proper state", function () {
          createServicePathCtrl();
          scope.setSFPstate(exampleData.sfps[0], sfpState.EDITED);
          expect(exampleData.sfps[0].state).toBe(sfpState.EDITED);
          scope.setSFPstate(exampleData.sfps[0], sfpState.NEW);
          expect(exampleData.sfps[0].state).toBe(sfpState.NEW);
          scope.setSFPstate(exampleData.sfps[0], sfpState.PERSISTED);
          expect(exampleData.sfps[0].state).toBe(sfpState.NEW);
        });

        it("should check if SFP is in given state", function () {
          createServicePathCtrl();
          //should return persisted even if there is not state set
          expect(scope.isSFPstate(exampleData.sfps[0], sfpState.PERSISTED)).toBeTruthy();
          expect(scope.isSFPstate(exampleData.sfps[0], sfpState.EDITED)).toBeFalsy();
          expect(scope.isSFPstate(exampleData.sfps[0], sfpState.NEW)).toBeFalsy();
          exampleData.sfps[0].state = sfpState.PERSISTED;
          expect(scope.isSFPstate(exampleData.sfps[0], sfpState.PERSISTED)).toBeTruthy();
          exampleData.sfps[0].state = sfpState.EDITED;
          expect(scope.isSFPstate(exampleData.sfps[0], sfpState.EDITED)).toBeTruthy();
          exampleData.sfps[0].state = sfpState.NEW;
          expect(scope.isSFPstate(exampleData.sfps[0], sfpState.NEW)).toBeTruthy();
        });

        it("should unset SFP state", function () {
          createServicePathCtrl();
          exampleData.sfps[0].state = sfpState.NEW;
          scope.unsetSFPstate(exampleData.sfps[0]);
          expect(exampleData.sfps[0].state).toBeUndefined();
        });

        it("should add SF with SFF into SFP['service-path-hop']", function () {
          createServicePathCtrl();
          var emptySfp = {"name": "test SFP"};
          scope.onSFPdrop("sf_" + exampleData.sfs[0].name, emptySfp);
          expect(emptySfp['service-path-hop']).toBeDefined();
          expect(emptySfp['service-path-hop'][0]).toEqual({"service-function-name": exampleData.sfs[0].name});
          expect(emptySfp.state).toBe(sfpState.EDITED);
        });

        it("should open modal dialog allowing to choose SFF name during addition of SF into SFP['service-path-hop']", function (){
          spyOn(servicePathModalSffSelectMock, 'open').andCallThrough();
          createServicePathCtrl();
          var emptySfp = {"name": "test SFP"};
          scope.sfs = exampleData.sfsWithNumberOfSffs;
          scope.onSFPdrop("sf_" + scope.sfs[0].name, emptySfp);
          expect(servicePathModalSffSelectMock.open).toHaveBeenCalledWith(scope.sfs[0].name, ['sff1', 'sff2'], jasmine.any(Function));
          expect(emptySfp['service-path-hop']).toBeDefined();
          expect(emptySfp['service-path-hop'][0]).toEqual({"service-function-name": scope.sfs[0].name,
            'service-function-forwarder': scope.sfs[0]['sf-data-plane-locator'][0]['service-function-forwarder']});
          expect(emptySfp.state).toBe(sfpState.EDITED);
        });

        it("should add SFF into SFP['service-path-hop']", function () {
          createServicePathCtrl();
          var emptySfp = {"name": "test SFP"};
          scope.onSFPdrop("sff_" + exampleData.sffs[0].name, emptySfp);
          expect(emptySfp['service-path-hop']).toBeDefined();
          expect(emptySfp['service-path-hop'][0]).toEqual({"service-function-forwarder": exampleData.sffs[0].name});
          expect(emptySfp.state).toBe(sfpState.EDITED);
        });

        it("should open modal dialog and delete SFP from controller and scope", function () {
          spyOn(servicePathSvcMock, 'deleteItem').andCallThrough();
          spyOn(modalDeleteSvcMock, 'open').andCallThrough();
          createServicePathCtrl();
          rootScope.sfps = exampleData.sfps;
          expect(scope.sfps).toEqual(exampleData.sfps);
          var sfpToBeDeleted = scope.sfps[0];
          scope.deleteSFP(scope.sfps[0]);
          expect(modalDeleteSvcMock.open).toHaveBeenCalledWith("sfp1", jasmine.any(Function));
          expect(servicePathSvcMock.deleteItem).toHaveBeenCalledWith(sfpToBeDeleted, jasmine.any(Function));
          expect(rootScope.sfps).toEqual([]);
          expect(scope.sfps).toEqual([]);
          expect(exampleData.sfps).toEqual([]);
        });

        it("should remove SF from SFC", function () {
          createServicePathCtrl();
          scope.removeSFfromSFP(exampleData.sfps[0], 0);
          expect(exampleData.sfps[0]['service-path-hop']).toEqual([]);
          expect(exampleData.sfps[0].state).toBe(sfpState.EDITED);
        });

        it("should PUT SFP to controller", function () {
          spyOn(servicePathSvcMock, 'putItem').andCallThrough();
          createServicePathCtrl();
          scope.persistSFP(exampleData.sfps[0]);
          expect(exampleData.sfps[0].state).toBeUndefined();
          expect(servicePathSvcMock.putItem).toHaveBeenCalledWith(exampleData.sfps[0], jasmine.any(Function));
        });

        it("should get index of SF in scope.sfs array", function () {
          createServicePathCtrl();
          var index = scope.getSFindexInSFS(exampleData.sfs[0].name);
          expect(index).toBe(0);
        });

        it("should UNDO changes in SFP", function () {
          spyOn(servicePathSvcMock, 'getItem').andCallThrough();
          createServicePathCtrl();
          rootScope.sfps = exampleData.sfps;
          rootScope.sfps[0].change = "someChanges";
          expect(scope.sfps).toEqual(rootScope.sfps);
          scope.undoSFPchanges(exampleData.sfps[0]);
          expect(servicePathSvcMock.getItem).toHaveBeenCalledWith(exampleData.sfps[0].name, jasmine.any(Function));
          expect(rootScope.sfps[0]).toEqual(exampleData.sfps[0]);
          expect(scope.sfps[0]).toEqual(exampleData.sfps[0]);
        });

        it("this.collectDistinctSffsNamesFromSf should return array of Sff names", function () {
          var thisCtrl = createServicePathCtrl();
          //should not pass through precondition
          var result = thisCtrl.collectDistinctSffsNamesFromSf(undefined);
          expect(result).toEqual([]);

          //should return empty array
          result = thisCtrl.collectDistinctSffsNamesFromSf({name: "sf1"});
          expect(result).toEqual([]);

          //should return array with sffNames
          result = thisCtrl.collectDistinctSffsNamesFromSf({name: "sf1", 'sf-data-plane-locator':[
            {'service-function-forwarder': 'sff1'},
            {'service-function-forwarder': 'sff2'}
          ]});
          expect(result).toEqual(['sff1', 'sff2']);

        });

        it("scope.getHopClass should return string containing CSS class of Hop", function (){
          createServicePathCtrl();
          var returnedClass = scope.getHopClass({'service-function-name': 'sf1'});
          expect(returnedClass).toEqual('sf');
          returnedClass = scope.getHopClass({'service-function-forwarder': 'sff1'});
          expect(returnedClass).toEqual('sff');
          var emptyClass = scope.getHopClass({name: 'hopWihthoutSforSff'});
          expect(emptyClass).toBeUndefined();
        });

      });

      describe('servicePathModalSffSelectCtrl', function () {

        var createServicePathModalSffSelectCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createServicePathModalSffSelectCtrl = function () {
            return $controller('servicePathModalSffSelectCtrl', {$scope: scope, $modalInstance: modalInstance, sfName: 'sf1', sffNameList: ['sff1', 'sff2']});
          };
        }));

        it("scope.sfName and scope.sffNameList should equal passed parameters", function () {
          createServicePathModalSffSelectCtrl();
          expect(scope.sfName).toEqual('sf1');
          expect(scope.sffNameList).toEqual(['sff1', 'sff2']);
        });

        it("modalInstance close should be caled with object {name: this.sffName}", function () {
          createServicePathModalSffSelectCtrl();
          scope.sffName = 'sff1';
          scope.save();
          expect(modalInstance.close).toHaveBeenCalledWith({name: 'sff1'});
        });

        it("modalInstance dismiss should be caled with mesage 'cancel'", function () {
          createServicePathModalSffSelectCtrl();
          scope.dismiss();
          expect(modalInstance.dismiss).toHaveBeenCalledWith('cancel');
        });
      });
    });
  });
});

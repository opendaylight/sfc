define(['app/sfc/sfc.test.module.loader'], function (sfc) {

  ddescribe('SFC app', function () {
    var rootScope, scope;

    var SfcAclSvcMock, SfcAclHelperMock;
    var modalDeleteSvcMock, SfcAclModalMetadataMock;

    var exampleData = {};

    exampleData.acls = [
      {
        "acl-name": "acl-1",
        "access-list-entries": [
          {
            "rule-name": "rule-1",
            "matches": {"destination-ipv4-address": "8.1.9.1", "absolute": {"active": true}},
            "actions": {"service-function-acl:service-function-path": "path-1"}
          }
        ]
      }
    ];

    SfcAclSvcMock = {
      getArray: function (callback) {
        callback(exampleData.acls);
      },
      deleteItemByKey: function (itemKey, callback) {
        return callback();
      },
      putItem: function (item, callback) {
        return callback();
      },
      getItem: function (itemKey, callback) {
        return callback(exampleData.acls[0]);
      }
    };

    SfcAclHelperMock = {
      sourceIpMacMaskToString: function () {
        return "sourceIpMacMaskToString";
      },
      destinationIpMacMaskToString: function () {
        return "destinationIpMacMaskToString";
      },
      flowLabelToString: function () {
        return "flowLabelToString";
      },
      sourcePortRangeToString: function () {
        return "sourcePortRangeToString";
      },
      destinationPortRangeToString: function () {
        return "destinationPortRangeToString";
      },
      dscpToString: function () {
        return "dscpToString";
      },
      ipProtocolToString: function () {
        return "ipProtocolToString";
      },
      valueOfAceType: function(){
        return "valueOfAceType";
      },
      addAce : function(){
        return "addAce";
      },
      removeAce : function(){
        return "removeAce";
      }
    };

    modalDeleteSvcMock = {
      open: function (aclName, callback) {
        return callback('delete');
      }
    };

    SfcAclModalMetadataMock = {
      open: function (ace) {
        return ace;
      }
    };

    beforeEach(angular.mock.module('app.sfc'));

    beforeEach(angular.mock.inject(function ($controller, $q, $state, $stateParams, $rootScope) {
      rootScope = $rootScope;
      scope = $rootScope.$new();
    }));

//    beforeEach(angular.mock.inject(function ($controller) {
//      return $controller('rootSfcCtrl', {$scope: scope});
//    }));

    describe('acl.controller', function () {

      describe("sfcAclCtrl", function () {
        var createSfcAclCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          createSfcAclCtrl = function () {
            // sfc.register.controller('sfcAclCtrl', function ($scope, $state, SfcAclSvc, SfcAclHelper, SfcAclModalMetadata, ModalDeleteSvc, ngTableParams, $filter)
            return $controller('sfcAclCtrl', {
              $scope: scope,
              SfcAclSvc: SfcAclSvcMock,
              SfcAclHelper: SfcAclHelperMock,
              ModalDeleteSvc: modalDeleteSvcMock,
              SfcAclModalMetadata: SfcAclModalMetadataMock
            });
          };
        }));

        it("ensure that rows data is loaded according to example restconf source data", function () {
          spyOn(SfcAclSvcMock, 'getArray').andCallThrough();
          createSfcAclCtrl();
          expect(SfcAclSvcMock.getArray).toHaveBeenCalledWith(jasmine.any(Function));

          // data
          expect(scope.acls.length).toBe(1);

          var entry = scope.acls[0];

          expect(entry['source-ip-mac-mask-string']).toEqual("sourceIpMacMaskToString");
          expect(entry['destination-ip-mac-mask-string']).toEqual("destinationIpMacMaskToString");
          expect(entry['flow-label-string']).toEqual("flowLabelToString");
          expect(entry['source-port-range-string']).toEqual("sourcePortRangeToString");
          expect(entry['destination-port-range-string']).toEqual("destinationPortRangeToString");
          expect(entry['dscp-string']).toEqual("dscpToString");
          expect(entry['ip-protocol-string']).toEqual("ipProtocolToString");

          scope.tableParams.settings({'$scope': scope});

          scope.$digest();

          expect(scope.$data.length).toBe(1);
          expect(scope.$data[0]).toEqual(scope.acls[0]);
        });

        it("should open modal dialog and call REST service to delete ACL by acl-name", function () {
          createSfcAclCtrl();

          spyOn(modalDeleteSvcMock, 'open').andCallThrough();
          spyOn(SfcAclSvcMock, 'deleteItemByKey').andCallThrough();
          scope.deleteItem(scope.acls[0]);
          expect(modalDeleteSvcMock.open).toHaveBeenCalledWith("acl-1", jasmine.any(Function));
          expect(SfcAclSvcMock.deleteItemByKey).toHaveBeenCalledWith("acl-1", jasmine.any(Function));
        });

        it("should call open modal dialog for metadata display", function () {
          createSfcAclCtrl();

          spyOn(SfcAclModalMetadataMock, 'open').andCallThrough();
          scope.showMetadata(scope.acls[0]);
          expect(SfcAclModalMetadataMock.open).toHaveBeenCalledWith(scope.acls[0]);
        });

        it("editSF should call $state.transition", angular.mock.inject(function ($state) {
          createSfcAclCtrl();

          spyOn($state, 'transitionTo').andReturn("dummy");
          scope.editItem(scope.acls[0]);
          expect($state.transitionTo).toHaveBeenCalledWith('main.sfc.acl-create', {itemKey: 'acl-1'}, jasmine.any(Object));
        }));

      });

      describe("sfcAclCreateCtrl", function () {
        var createSfcAclCreateCtrl;

        beforeEach(angular.mock.inject(function ($controller) {
          // sfc.register.controller('sfcAclCreateCtrl', function ($scope, $rootScope, $state, $stateParams, SfcAclHelper, SfcAclSvc) {
          createSfcAclCreateCtrl = function () {
            return $controller('sfcAclCreateCtrl', {
              $scope: scope,
              $rootScope: rootScope,
              $stateParams : {itemKey: "acl-1"},
              SfcAclSvc: SfcAclSvcMock,
              SfcAclHelper: SfcAclHelperMock
            });
          };
        }));

        it("ensure that scope.data is initialized with example ACL", function () {
          createSfcAclCreateCtrl();
          expect(scope.data).toEqual(exampleData.acls[0]);
        });

        it("should call proper service functions", function () {
          createSfcAclCreateCtrl();

          spyOn(SfcAclHelperMock, 'valueOfAceType').andReturn("dummy");
          scope.valueOfAceType({});
          expect(SfcAclHelperMock.valueOfAceType).toHaveBeenCalledWith({}, rootScope);

          spyOn(SfcAclHelperMock, 'addAce').andReturn("dummy");
          scope.addAce();
          expect(SfcAclHelperMock.addAce).toHaveBeenCalledWith(scope);

          spyOn(SfcAclHelperMock, 'removeAce').andReturn("dummy");
          scope.removeAce(1);
          expect(SfcAclHelperMock.removeAce).toHaveBeenCalledWith(1, scope);
        });

        it("should call putItem service functions and transitionTo 'main.sfc.acl'", angular.mock.inject(function ($state) {
          createSfcAclCreateCtrl();

          spyOn(SfcAclSvcMock, 'putItem').andCallThrough();
          spyOn($state, 'transitionTo').andReturn("dummy");
          scope.submit();
          expect(SfcAclSvcMock.putItem).toHaveBeenCalledWith(scope.data, jasmine.any(Function));
          expect($state.transitionTo).toHaveBeenCalledWith('main.sfc.acl', null, jasmine.any(Object));
        }));
      });
    });

  });

}); //end define

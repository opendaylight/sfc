define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('serviceForwarderCtrl', function ($scope, $state, ServiceForwarderSvc, SfcServiceForwarderOvsSvc, ServiceForwarderHelper, ServiceLocatorHelper, ModalCreateOvsSvc, ModalInfoSvc, ModalErrorSvc, ModalDeleteSvc, SfcTableParamsSvc, ngTableParams, $filter, $timeout) {
    var thisCtrl = this;
    $scope.sffs = [];

    var NgTableParams = ngTableParams;

    SfcTableParamsSvc.initializeSvcForTable('serviceForwarderTable');
    $scope.tableParams = new NgTableParams({
        page: 1,            // show first page
        count: 10,          // count per page
        sorting: {
          name: 'asc'     // initial sorting
        }
        
      },
      {
        total: $scope.sffs.length,
        getData: function ($defer, params) {
          SfcTableParamsSvc.setFilterTableParams('serviceForwarderTable', params.filter());

          // use build-in angular filter
          var filteredData = SfcTableParamsSvc.checkAndSetFilterTableParams('serviceForwarderTable', $scope.tableParams) ?
            $filter('filter')($scope.sffs, params.filter()) :
            $scope.sffs;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        },
        counts: []
      });

    $scope.sffInterfaceToString = ServiceForwarderHelper.sffInterfaceToString;

    $scope.getLocatorTooltipText = function (locator) {
      return ServiceLocatorHelper.getLocatorTooltipText(locator, $scope);
    };

    $scope.getOVStooltipText = function (ovs) {
      return ServiceForwarderHelper.getOVStooltipText(ovs, $scope);
    };

    this.fetchData = function () {
      ServiceForwarderSvc.getArray(function (data) {
        $scope.sffs = data;
        _.each($scope.sffs, function (sff) {
          sff['sff-data-plane-locator-string'] = ServiceForwarderHelper.sffDpLocatorToString(sff['sff-data-plane-locator']);
          sff['service-function-dictionary-string'] = ServiceForwarderHelper.sffFunctionDictionaryToString(sff['service-function-dictionary']);
        });

        $scope.tableParams.reload();
      });
    };

    this.fetchData();

    $scope.deleteSFF = function deleteSFF(sff) {
      ModalDeleteSvc.open(sff.name, function (result) {
        if (result == 'delete') {
          //delete the row
          ServiceForwarderSvc.deleteItem(sff, function () {
            $scope.sffs.splice(_.indexOf($scope.sffs, sff), 1);
            $scope.tableParams.reload();
          });
        }
      });
    };

    $scope.cloneSFF = function cloneSFF(sff) {
      delete sff['sff-data-plane-locator-string'];
      delete sff['service-function-dictionary-string'];
      sff['name'] = sff['name'] + "_copy";
      $state.transitionTo('main.sfc.serviceforwarder-clone', {sff: JSON.stringify(sff)}, {
        location: true,
        inherit: true,
        relative: $state.$current,
        notify: true
      });
    };

    $scope.editSFF = function editSF(sffName) {
      $state.transitionTo('main.sfc.serviceforwarder-edit', {sffName: sffName}, {
        location: true,
        inherit: true,
        relative: $state.$current,
        notify: true
      });
    };

    $scope.deleteAll = function deleteAll() {
      ModalDeleteSvc.open($scope.$eval('"SFC_SERVICE_FORWARDERS" | translate'), function (result) {
        if (result == 'delete') {
          ServiceForwarderSvc.deleteAll(function () {
            thisCtrl.fetchData();
          });
        }
      });
    };

    $scope.rpcCreateOvsBridge = function () {
      ModalCreateOvsSvc.open(function(createOvsBridgeInput) {

        if (angular.isDefined(createOvsBridgeInput['name']) && angular.isDefined(createOvsBridgeInput['ovs-node']['ip'])) {
          SfcServiceForwarderOvsSvc.executeRpcOperation({input: createOvsBridgeInput}, 'create-ovs-bridge', null, function (result) {
            if (angular.isDefined(result) && result['result'] === true) {
        
              var modalBody = $scope.$eval('"SFC_FORWARDER_MODAL_CREATE_OVS_RPC_SUCCESS_BODY" | translate') + ": <b>'" + createOvsBridgeInput['name'] + "'</b>.";

              ModalInfoSvc.open({
                "head": $scope.$eval('"SFC_FORWARDER_MODAL_CREATE_OVS_RPC_SUCCESS_HEADER" | translate'),
                "body": modalBody
              });

              //reload the SFF table
              $timeout(function () {
                thisCtrl.fetchData();
              }, 500);

            }
            else if (angular.isDefined(result)) {
              ModalErrorSvc.open({
                head: $scope.$eval('"SFC_FORWARDER_MODAL_CREATE_OVS_RPC_FAIL_HEADER" | translate'),
                rpcError: result['response'] || ''
              });
            }
          });
        }
      });
    };

   });

  sfc.register.controller('serviceForwarderCreateCtrl', function ($scope, $state, $stateParams, ServiceNodeSvc, ServiceForwarderSvc, ServiceForwarderHelper, ServiceFunctionSvc) {

    $scope.selectOptions = ServiceForwarderHelper.selectOptions($scope);
    $scope.chosenSfDpls = {};

    if (angular.isDefined($stateParams.sffName) || angular.isDefined($stateParams.sff)) {
      // we'll wait for data to edit
      $scope.data = {};
    }
    else {
      // create initial data
      $scope.data = {
        "sff-data-plane-locator": [
          {
            "data-plane-locator": {},
            "ovs-options": {}
          }
        ],
        "service-function-dictionary": [
          {
            "nonExistent": false,
            "sff-sf-data-plane-locator": {},
            "sff-interfaces": []
          }
        ],
        "service-function-forwarder-ovs:ovs-bridge": {}
      };
    }

    ServiceNodeSvc.getArray(function (data) {
      $scope.sns = data;

      ServiceFunctionSvc.getArray(function (data) {
        $scope.sfs = data;
        
        if (angular.isDefined($stateParams.sff)) {
          $scope.data = JSON.parse($stateParams.sff);
          ServiceForwarderHelper.removeNonExistentSn($scope.data, $scope.sns);
          _.each($scope.data['service-function-dictionary'], function (sf) {
            ServiceForwarderHelper.sfUpdate(sf, $scope);
          });
        }
        else if (angular.isDefined($stateParams.sffName)) {
          ServiceForwarderSvc.getItem($stateParams.sffName, function (item) {
            $scope.data = item;
            ServiceForwarderHelper.removeNonExistentSn($scope.data, $scope.sns);
            _.each($scope.data['service-function-dictionary'], function (sf) {
              ServiceForwarderHelper.sfUpdate(sf, $scope);
              $scope.sfChangeListener(sf);
            });
          });
        }
      });
    });

    $scope.addLocator = function () {
      ServiceForwarderHelper.addLocator($scope);
    };

    $scope.removeLocator = function (index) {
      ServiceForwarderHelper.removeLocator(index, $scope);
    };

    $scope.addFunction = function () {
      ServiceForwarderHelper.addFunction($scope);
    };

    $scope.removeFunction = function (index) {
      ServiceForwarderHelper.removeFunction(index, $scope);
    };


    $scope.sfChangeListener = function (choosenSf) {
      ServiceForwarderHelper.sfChangeListener(choosenSf, $scope);
    };

    $scope.initOVSBridge = function(locator){
      locator['ovs-bridge'] = {};
      locator['ovs-options'] = {};
    }



    $scope.submit = function () {
      //reformat sff-interfaces string array to object array
      _.each($scope.data['service-function-dictionary'], function (sf) {
        sf['sff-interfaces'] = ServiceForwarderHelper.sffInterfaceToObjectArray(sf['sff-interfaces']);
        ServiceForwarderHelper.removeTemporaryPropertiesFromSf(sf);
      });

      ServiceForwarderSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.serviceforwarder', null, {
          location: true,
          inherit: true,
          relative: $state.$current,
          notify: true
        });
      });
    };
  });

  sfc.register.controller('ModalCreateOvsCtrl', function ($scope, $modalInstance) {

    $scope.save = function () {

      var createOvsBridgeInput = {};

      createOvsBridgeInput['name'] = this.data.name;
      createOvsBridgeInput['ovs-node'] = {};
      createOvsBridgeInput['ovs-node']['ip'] = this.data.ip_address;
      createOvsBridgeInput['ovs-node']['port'] = this.data.port;

      $modalInstance.close(createOvsBridgeInput);
    };

    $scope.dismiss = function () {
      $modalInstance.dismiss('cancel');
    };
  });

});
define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('serviceForwarderCtrl', function ($scope, $state, ServiceForwarderSvc, ServiceForwarderHelper, ModalDeleteSvc, ngTableParams, $filter) {
    var NgTableParams = ngTableParams;

    $scope.sffInterfaceToString = ServiceForwarderHelper.sffInterfaceToString;

    ServiceForwarderSvc.getArray(function (data) {
      $scope.sffs = data;
      _.each($scope.sffs, function (sff) {
        sff['sff-data-plane-locator-string'] = ServiceForwarderHelper.sffDpLocatorToString(sff['sff-data-plane-locator']);
        sff['service-function-dictionary-string'] = ServiceForwarderHelper.sffFunctionDictionaryToString(sff['service-function-dictionary']);
      });

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
            // use build-in angular filter
            var filteredData = params.filter() ?
              $filter('filter')($scope.sffs, params.filter()) :
              $scope.sffs;

            var orderedData = params.sorting() ?
              $filter('orderBy')(filteredData, params.orderBy()) :
              filteredData;

            params.total(orderedData.length);
            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
        });
    });

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

    $scope.editSFF = function editSF(sffName) {
      $state.transitionTo('main.sfc.serviceforwarder-edit', {sffName: sffName}, { location: true, inherit: true, relative: $state.$current, notify: true });
    };
  });

  sfc.register.controller('serviceForwarderCreateCtrl', function ($scope, $state, ServiceNodeSvc, ServiceForwarderSvc, ServiceForwarderHelper, ServiceFunctionSvc) {

    $scope.selectOptions = ServiceForwarderHelper.selectOptions($scope);

    $scope.data = {
      "sff-data-plane-locator": [
        {
          "data-plane-locator": {}
        }
      ],
      "service-function-dictionary": [
        {
          "nonExistent": false,
          "sff-sf-data-plane-locator": {},
          "sff-interfaces": []
        }
      ]
    };

    $scope.DpLocators = {};
    $scope.selectedDpLocator = {};

    ServiceNodeSvc.getArray(function (data) {
      $scope.sns = data;
    });

    ServiceFunctionSvc.getArray(function (data) {
      $scope.sfs = data;
    });

    $scope.addLocator = function () {
      ServiceForwarderHelper.addLocator($scope);
    };

    $scope.removeLocator = function (index) {
      ServiceForwarderHelper.removeLocator(index, $scope);
    };

    $scope.addFunction = function() {
      ServiceForwarderHelper.addFunction($scope);
    };

    $scope.removeFunction = function(index) {
      ServiceForwarderHelper.removeFunction(index, $scope);
    };

    $scope.sfChangeListener = function(choosenSf) {
      ServiceForwarderHelper.sfChangeListener(choosenSf, $scope);
    };

    $scope.dpChangeListener = function(sf) {
      ServiceForwarderHelper.dpChangeListener(sf, $scope);
    };

    $scope.submit = function () {
      //reformat sff-interfaces string array to object array
      _.each($scope.data['service-function-dictionary'], function(sf){
        sf['sff-interfaces'] = ServiceForwarderHelper.sffInterfaceToObjectArray(sf['sff-interfaces']);
        ServiceForwarderHelper.removeTemporaryPropertiesFromSf(sf);
      });

      ServiceForwarderSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.serviceforwarder', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };
  });

  sfc.register.controller('serviceForwarderEditCtrl', function ($scope, $state, $stateParams, ServiceNodeSvc, ServiceForwarderSvc, ServiceForwarderHelper, ServiceFunctionSvc) {

    $scope.selectOptions = ServiceForwarderHelper.selectOptions($scope);

    $scope.data = {
      "sff-data-plane-locator": [
        {
          "data-plane-locator": {}
        }
      ],
      "service-function-dictionary": [
        {
          "nonExistent": false,
          "sff-sf-data-plane-locator": {},
          "sff-interfaces": []
        }
      ]
    };

    $scope.DpLocators = {};
    $scope.selectedDpLocator = {};

    ServiceNodeSvc.getArray(function (data) {
      $scope.sns = data;

      ServiceFunctionSvc.getArray(function (data) {
        $scope.sfs = data;

        ServiceForwarderSvc.getItem($stateParams.sffName, function (item) {
          $scope.data = item;
          ServiceForwarderHelper.removeNonExistentSn($scope.data, $scope.sns);
          _.each($scope.data['service-function-dictionary'], function (sf) {
            ServiceForwarderHelper.sfUpdate(sf, $scope);
          });
        });
      });
    });

    $scope.addLocator = function () {
      ServiceForwarderHelper.addLocator($scope);
    };

    $scope.removeLocator = function (index) {
      ServiceForwarderHelper.removeLocator(index, $scope);
    };

    $scope.addFunction = function() {
      ServiceForwarderHelper.addFunction($scope);
    };

    $scope.removeFunction = function(index) {
      ServiceForwarderHelper.removeFunction(index, $scope);
    };

    $scope.sfChangeListener = function(choosenSf) {
      ServiceForwarderHelper.sfChangeListener(choosenSf, $scope);
    };

    $scope.dpChangeListener = function(sf) {
      ServiceForwarderHelper.dpChangeListener(sf, $scope);
    };

    $scope.submit = function () {
      //reformat sff-interfaces string array to object array
      _.each($scope.data['service-function-dictionary'], function(sf){
        sf['sff-interfaces'] = ServiceForwarderHelper.sffInterfaceToObjectArray(sf['sff-interfaces']);
        ServiceForwarderHelper.removeTemporaryPropertiesFromSf(sf);
      });

      ServiceForwarderSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.serviceforwarder', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };
  });
});
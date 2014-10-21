define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('sfcMetadataCtrl', function ($scope, $state, SfcContextMetadataSvc, SfcVariableMetadataSvc, ModalDeleteSvc, ngTableParams, $filter) {

    var thisCtrl = this;
    var NgTableParams = ngTableParams; // checkstyle

    this.getNgTableParams = function (dataKey) {
      return new NgTableParams({
          page: 1,          // show first page
          count: 10,        // count per page
          sorting: {
            name: 'asc'     // initial sorting
          }
        },
        {
          total: 0,
          getData: function ($defer, params) {
            // use build-in angular filter
            var filteredData = params.filter() ?
              $filter('filter')($scope[dataKey], params.filter()) :
              $scope[dataKey];

            var orderedData = params.sorting() ?
              $filter('orderBy')(filteredData, params.orderBy()) :
              filteredData;

            params.total(orderedData.length);
            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
        });
    };

    this.fetchContextMetadata = function () {
      $scope.contextMetadata = [];

      SfcContextMetadataSvc.getArray(function (data) {
        $scope.contextMetadata = data || [];
        $scope.tableParamsContext.reload();
      });
    };

    this.fetchVariableMetadata = function () {
      $scope.variableMetadata = [];

      SfcVariableMetadataSvc.getArray(function (data) {
        // expand variable metadata list into rows
        _.each(data, function (item) {
          if (!_.isEmpty(item['tlv-metadata'])) {
            _.each(item['tlv-metadata'], function (tlv) {
              tlv['name'] = item['name'];
              $scope.variableMetadata.push(tlv);
            });
          }
          else {
            var tlv = {}; // dummy entry
            tlv['name'] = item['name'];
            $scope.variableMetadata.push(tlv);
          }
        });

        $scope.tableParamsVariable.reload();
      });
    };

    this.fetchData = function () {
      this.fetchContextMetadata();
      this.fetchVariableMetadata();
    };

    this.init = function () {
      $scope.contextMetadata = [];
      $scope.variableMetadata = [];
      $scope.tableParamsContext = this.getNgTableParams('contextMetadata');
      $scope.tableParamsVariable = this.getNgTableParams('variableMetadata');
      this.fetchData();
    };

    //init controller
    this.init();

    $scope.deleteContextItem = function deleteContextItem(contextItem) {
      ModalDeleteSvc.open(contextItem['name'], function (result) {
        if (result == 'delete') {
          //delete the row
          SfcContextMetadataSvc.deleteItem(contextItem, function () {
            thisCtrl.fetchContextMetadata();
          });
        }
      });
    };

    $scope.editContextItem = function editContextItem(contextItem) {
      $state.transitionTo('main.sfc.metadata-context-create', {itemKey: contextItem['name']}, { location: true, inherit: true, relative: $state.$current, notify: true });
    };

    $scope.deleteVariableItem = function deleteVariableItem(variableItem) {
      ModalDeleteSvc.open(variableItem['name'], function (result) {
        if (result == 'delete') {
          //delete the row
          SfcVariableMetadataSvc.deleteItem(variableItem, function () {
            thisCtrl.fetchVariableMetadata();
          });
        }
      });
    };

    $scope.editVariableItem = function editVariableItem(variableItem) {
      $state.transitionTo('main.sfc.metadata-variable-create', {itemKey: variableItem['name']}, { location: true, inherit: true, relative: $state.$current, notify: true });
    };

    $scope.decimalToHex = function decimalToHex(decimalNumber) {
      return "0x" + decimalNumber.toString(16);
    };
  });

  sfc.register.controller('sfcMetadataVariableCreateCtrl', function ($scope, $rootScope, $state, $stateParams, SfcVariableMetadataSvc) {

    $scope.data = {'tlv-metadata': []}; // initial empty sub list data

    if ($stateParams.itemKey) {
      SfcVariableMetadataSvc.getItem($stateParams.itemKey, function (item) {
        $scope.data = item;
      });
    } else {
      $scope.data['tlv-metadata'].push({});
    }

    // form actions
    $scope.addTlv = function () {
      $scope.data['tlv-metadata'].push({});
    };

    $scope.removeTlv = function (index) {
      $scope.data['tlv-metadata'].splice(index, 1);
    };

    $scope.submit = function () {
      SfcVariableMetadataSvc.putItem($scope.data, function () {
        $state.transitionTo('main.sfc.metadata', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };

  });

  sfc.register.controller('sfcMetadataContextCreateCtrl', function ($scope, $rootScope, $state, $stateParams, SfcContextMetadataSvc) {

    $scope.data = []; // initial empty  list data

    if ($stateParams.itemKey) {
      SfcContextMetadataSvc.getItem($stateParams.itemKey, function (item) {
        $scope.data.push(item);
      });
    } else {
      $scope.data.push({});
    }

    // form actions
    $scope.addContextMetadata = function () {
      $scope.data.push({});
    };

    $scope.removeContextMetadata = function (index) {
      $scope.data.splice(index, 1);
    };

    $scope.submit = function () {
      var itemsToSubmit = $scope.data.length;
      _.each($scope.data, function (item) {
        SfcContextMetadataSvc.putItem(item, function () {
          itemsToSubmit--;
          if (itemsToSubmit===0) {
            $state.transitionTo('main.sfc.metadata', null, { location: true, inherit: true, relative: $state.$current, notify: true });
          }
        });
      });
    };

  });

});
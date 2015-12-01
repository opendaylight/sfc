define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('sfcMetadataCtrl', function ($scope, $state, SfcContextMetadataSvc, SfcVariableMetadataSvc, ModalDeleteSvc, SfcTableParamsSvc, ngTableParams, $filter) {

    var thisCtrl = this;
    var NgTableParams = ngTableParams; // checkstyle

    this.getNgTableParams = function (dataKey) {
      SfcTableParamsSvc.initializeSvcForTable(dataKey + 'Table');
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
            SfcTableParamsSvc.setFilterTableParams(dataKey + 'Table', params.filter());

            // use build-in angular filter
            var filteredData = SfcTableParamsSvc.checkAndSetFilterTableParams(dataKey + 'Table', $scope[dataKey + 'TableParams']) ?
              $filter('filter')($scope[dataKey], params.filter()) :
              $scope[dataKey];

            var orderedData = params.sorting() ?
              $filter('orderBy')(filteredData, params.orderBy()) :
              filteredData;

            params.total(orderedData.length);
            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          },
          counts: []
        });
    };

    this.fetchContextMetadata = function () {
      $scope.contextMetadata = [];

      SfcContextMetadataSvc.getArray(function (data) {
        $scope.contextMetadata = data || [];

        _.each($scope.contextMetadata, function (item) {
          item['context-header1'] = $scope.decimalToHex(item['context-header1']);
          item['context-header2'] = $scope.decimalToHex(item['context-header2']);
          item['context-header3'] = $scope.decimalToHex(item['context-header3']);
          item['context-header4'] = $scope.decimalToHex(item['context-header4']);
        });

        $scope.contextMetadataTableParams.reload();
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
              tlv['tlv-class'] = $scope.decimalToHex(tlv['tlv-class']);
              tlv['tlv-type'] = $scope.decimalToHex(tlv['tlv-type']);
              $scope.variableMetadata.push(tlv);
            });
          }
          else {
            var tlv = {}; // dummy entry
            tlv['name'] = item['name'];
            $scope.variableMetadata.push(tlv);
          }
        });

        $scope.variableMetadataTableParams.reload();
      });
    };

    this.fetchData = function () {
      this.fetchContextMetadata();
      this.fetchVariableMetadata();
    };

    this.init = function () {
      $scope.contextMetadata = [];
      $scope.variableMetadata = [];
      $scope.contextMetadataTableParams = this.getNgTableParams('contextMetadata');
      $scope.variableMetadataTableParams = this.getNgTableParams('variableMetadata');
      thisCtrl.fetchData();
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

    $scope.deleteAllContextMetadata = function deleteAllContextMetadata() {
      ModalDeleteSvc.open($scope.$eval('"SFC_ACL_SHORT_METADATA_CONTEXT" | translate'), function (result) {
        if (result == 'delete') {
          SfcContextMetadataSvc.deleteAll(function () {
            thisCtrl.fetchContextMetadata();
          });
        }
      });
    };

    $scope.editContextItem = function editContextItem(contextItem) {
      $state.transitionTo('main.sfc.metadata-context-edit', {itemKey: contextItem['name']}, { location: true, inherit: true, relative: $state.$current, notify: true });
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

    $scope.deleteAllVariableMetadata = function deleteAllVariableetadata() {
      ModalDeleteSvc.open($scope.$eval('"SFC_ACL_SHORT_METADATA_VARIABLE" | translate'), function (result) {
        if (result == 'delete') {
          SfcVariableMetadataSvc.deleteAll(function () {
            thisCtrl.fetchVariableMetadata();
          });
        }
      });
    };

    $scope.editVariableItem = function editVariableItem(variableItem) {
      $state.transitionTo('main.sfc.metadata-variable-edit', {itemKey: variableItem['name']}, { location: true, inherit: true, relative: $state.$current, notify: true });
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

}); // end define
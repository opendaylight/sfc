define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('sfcAclCtrl', function ($scope, $state, SfcAclSvc, SfcAclHelper, SfcAclModalMetadata, ModalDeleteSvc, ngTableParams, $filter) {

    var thisCtrl = this;

    $scope.acls = [];

    var NgTableParams = ngTableParams; // checkstyle

    $scope.tableParams = new NgTableParams({
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
            $filter('filter')($scope.acls, params.filter()) :
            $scope.acls;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        }
      });

    this.fetchData = function () {
      $scope.acls = [];
      SfcAclSvc.getArray(function (data) {
        // expand acl group into rows
        _.each(data, function (acl) {
          if (!_.isEmpty(acl['access-list-entries'])) {
            _.each(acl['access-list-entries'], function (entry) {
              entry['acl-name'] = acl['acl-name'];
              $scope.acls.push(entry);
              stringifyComposedProperties(entry);
            });
          } else {
            var entry = {}; // dummy entry
            entry['acl-name'] = acl['acl-name'];
            $scope.acls.push(entry);
          }
        });

        $scope.tableParams.reload();
      });
    };

    this.fetchData();

    function stringifyComposedProperties(entry){
      entry['source-ip-mac-mask-string'] = SfcAclHelper.sourceIpMacMaskToString(entry['matches']);
      entry['destination-ip-mac-mask-string'] = SfcAclHelper.destinationIpMacMaskToString(entry['matches']);
      entry['flow-label-string'] = SfcAclHelper.flowLabelToString(entry['matches']);
      entry['source-port-range-string'] = SfcAclHelper.sourcePortRangeToString(entry['matches']['source-port-range']);
      entry['destination-port-range-string'] = SfcAclHelper.destinationPortRangeToString(entry['matches']['destination-port-range']);
      entry['dscp-string'] = SfcAclHelper.dscpToString(entry['matches']);
      entry['ip-protocol-string'] = SfcAclHelper.ipProtocolToString(entry['matches']);
    }

    $scope.matchesToString = function (matches){
      return SfcAclHelper.matchesToString(matches, $scope);
    };

    $scope.metadataToString = function (matches){
      return SfcAclHelper.metadataToString(matches, $scope);
    };

    //table actions
    $scope.showMetadata = function (ace) {
      SfcAclModalMetadata.open(ace);
    };

    $scope.deleteItem = function deleteItem(ace) {
      ModalDeleteSvc.open(ace['acl-name'], function (result) {
        if (result == 'delete') {
          //delete the row
          SfcAclSvc.deleteItemByKey(ace['acl-name'], function () {
            thisCtrl.fetchData();
          });
        }
      });
    };

    $scope.editItem = function editItem(ace) {
      $state.transitionTo('main.sfc.acl-create', {itemKey: ace['acl-name']}, { location: true, inherit: true, relative: $state.$current, notify: true });
    };
  });

  sfc.register.controller('sfcAclCreateCtrl', function ($scope, $rootScope, $state, $stateParams, SfcAclHelper, SfcAclSvc) {

    $scope.data = {'access-list-entries': []}; // initial data

    if ($stateParams.itemKey) {
      SfcAclSvc.getItem($stateParams.itemKey, function (item) {
        $scope.data = item;
      });
    } else {
      $scope.data['access-list-entries'].push({});
    }

    $scope.valueOfAceType = function (matches) {
      return SfcAclHelper.valueOfAceType(matches, $rootScope);
    };

    // form actions
    $scope.addAce = function (){
      SfcAclHelper.addAce($scope);
    };

    $scope.removeAce = function (index){
      SfcAclHelper.removeAce(index, $scope);
    };

    $scope.submit = function (){
      SfcAclSvc.putItem($scope.data, function (){
        $state.transitionTo('main.sfc.acl', null, { location: true, inherit: true, relative: $state.$current, notify: true });
      });
    };

  });

  sfc.register.controller('sfcAclModalMetadataCtrl', function ($modalInstance, $scope, ace) {
    $scope.ace = ace;

    $scope.dismiss = function () {
      $modalInstance.dismiss('ok');
    };
  });
});
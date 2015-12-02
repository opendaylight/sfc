define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('sfcAclCtrl', function ($scope, $state, $sce, SfcAclSvc, SfcAclHelper, SfcIpFixHelper, SfcAclModalMetadata, ModalDeleteSvc, SfcTableParamsSvc, ngTableParams, $filter) {

    var thisCtrl = this;

    $scope.acls = [];

    var NgTableParams = ngTableParams; // checkstyle
    SfcTableParamsSvc.initializeSvcForTable('aclTable');

    $scope.tableParams = new NgTableParams({
        page: 1,          // show first page
        count: 10,        // count per page
        sorting: {
          'acl-name': 'asc'     // initial sorting
        }
      },
      {
        total: $scope.acls.length,
        getData: function ($defer, params) {
          SfcTableParamsSvc.setFilterTableParams('aclTable', params.filter());

          // use build-in angular filter
          var filteredData = SfcTableParamsSvc.checkAndSetFilterTableParams('aclTable', $scope.tableParams) ?
            $filter('filter')($scope.acls, params.filter()) :
            $scope.acls;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        },
        counts: []
      });

    this.fetchData = function () {
      $scope.acls = [];
      SfcAclSvc.getArray(function (data) {
        // expand acl group into rows
        _.each(data, function (acl) {
          if (!_.isEmpty(acl['access-list-entries']['ace'])) {
            _.each(acl['access-list-entries']['ace'], function (entry) {
              entry['acl-name'] = acl['acl-name'];
              $scope.acls.push(entry);
              thisCtrl.stringifyComposedProperties(entry);
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

    $scope.$on('reloadAcl', function () {
      thisCtrl.fetchData();
    });

    this.stringifyComposedProperties = function (entry){
      entry['source-ip-mac-mask-string'] = SfcAclHelper.sourceIpMacMaskToString(entry['matches']);
      entry['destination-ip-mac-mask-string'] = SfcAclHelper.destinationIpMacMaskToString(entry['matches']);
      entry['flow-label-string'] = SfcAclHelper.flowLabelToString(entry['matches']);
      entry['source-port-range-string'] = SfcAclHelper.sourcePortRangeToString(entry['matches']['source-port-range']);
      entry['destination-port-range-string'] = SfcAclHelper.destinationPortRangeToString(entry['matches']['destination-port-range']);
      entry['dscp-string'] = SfcAclHelper.dscpToString(entry['matches']);
      entry['ip-protocol-string'] = SfcAclHelper.ipProtocolToString(entry['matches']);
      entry['application-id-string'] = $sce.trustAsHtml(SfcIpFixHelper.appIdToString(entry['matches']));
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

    $scope.deleteAll = function deleteAll() {
      ModalDeleteSvc.open($scope.$eval('"SFC_ACCESS_LISTS" | translate'), function (result) {
        if (result == 'delete') {
          SfcAclSvc.deleteAll(function () {
            thisCtrl.fetchData();
          });
        }
      });
    };

    $scope.editItem = function editItem(ace) {
      $state.transitionTo('main.sfc.acl-edit', {itemKey: ace['acl-name']}, { location: true, inherit: true, relative: $state.$current, notify: true });
    };
  });

  sfc.register.controller('sfcAclCreateCtrl', function ($scope, $rootScope, $state, $stateParams, SfcAclHelper, SfcAclSvc, SfcContextMetadataSvc, SfcVariableMetadataSvc, SfcIpfixClassIdSvc, SfcIpfixAppIdSvc, SfcIpFixHelper) {

    $scope.data = {'access-list-entries': {'ace': []}}; // initial data

    SfcContextMetadataSvc.getArray(function (data) {
      $scope.contextMetadata = data;
    });

    SfcVariableMetadataSvc.getArray(function (data) {
      $scope.variableMetadata = data;
    });

    SfcIpfixClassIdSvc.getArray(function (data) {
      $scope.classid = data.sort(function(a, b) { return a.id - b.id; }); // sort by id
    });

    SfcIpfixAppIdSvc.getArray(function (data) {
      $scope.appid = data.sort(function(a, b) { return a['selector-id'] - b['selector-id']; }); // sort by id
    });

    if ($stateParams.itemKey) {
      SfcAclSvc.getItem($stateParams.itemKey, function (item) {
        $scope.data = item;
      });
    } else {
      $scope.data['access-list-entries']['ace'].push({});
    }

    Array.prototype.move = function(from, to) {
        this.splice(to, 0, this.splice(from, 1)[0]);
    };

    $scope.sortableOptions = {
      cursor: 'move',
      tolerance: 'pointer',
      start: function (e, ui) {
        $(e.target).data("ui-sortable").floating = true;
        $scope.oldIndex = ui.item.index();
      },
      update: function (e, ui) {
        $scope.data['access-list-entries']['ace'].move($scope.oldIndex, ui.item.index());
      }
    };

    $scope.valueOfAceType = function (matches) {
      return SfcAclHelper.valueOfAceType(matches, $rootScope);  // IP. ethernet IPFIX
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

  sfc.register.controller('sfcClassifierCtrl', function ($scope, $state, SfcClassifierSvc, ModalDeleteSvc, SfcTableParamsSvc, ngTableParams, $filter) {
    var thisCtrl = this;
    var NgTableParams = ngTableParams; // checkstyle

    $scope.classifiers = [];
    SfcTableParamsSvc.initializeSvcForTable('classifierTable');

    $scope.tableParams = new NgTableParams({
        page: 1,          // show first page
        count: 10,        // count per page
        sorting: {
          name: 'asc'     // initial sorting
        }
      },
      {
        total: $scope.classifiers.length,
        getData: function ($defer, params) {
          SfcTableParamsSvc.setFilterTableParams('classifierTable', params.filter());

          // use build-in angular filter
          var filteredData = SfcTableParamsSvc.checkAndSetFilterTableParams('classifierTable', $scope.tableParams) ?
            $filter('filter')($scope.classifiers, params.filter()) :
            $scope.classifiers;

          var orderedData = params.sorting() ?
            $filter('orderBy')(filteredData, params.orderBy()) :
            filteredData;

          params.total(orderedData.length);
          $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
        },
        counts: []
      });

    this.fetchData = function (){
      SfcClassifierSvc.getArray(function(data){
        $scope.classifiers = data || [];
        $scope.tableParams.reload();
      });
    };

    this.fetchData();

    $scope.getSffAttachmentPointTooltip = function(sff){
      var tooltip;

      if(angular.isDefined(sff['bridge'])){
        tooltip = $scope.$eval('"SFC_CLASSIFIER_ATTACHMENT_POINT_BRIDGE" | translate') + ": " + sff['bridge'];
      }
      else if(angular.isDefined(sff['interface'])){
        tooltip = $scope.$eval('"SFC_CLASSIFIER_ATTACHMENT_POINT_INTERFACE" | translate') + ": " + sff['interface'];
      }

      return tooltip;
    };

    $scope.deleteItem = function deleteItem(classifier) {
      ModalDeleteSvc.open(classifier['name'], function (result) {
        if (result == 'delete') {
          //delete the row
          SfcClassifierSvc.deleteItem(classifier, function () {
            thisCtrl.fetchData();
          });
        }
      });
    };

    $scope.deleteAll = function deleteAll() {
      ModalDeleteSvc.open($scope.$eval('"SFC_CLASSIFIERS" | translate'), function (result) {
        if (result == 'delete') {
          SfcClassifierSvc.deleteAll(function () {
            thisCtrl.fetchData();
          });
        }
      });
    };

    $scope.editItem = function editItem(classifier) {
      $state.transitionTo('main.sfc.classifier-edit', {itemKey: classifier['name']}, { location: true, inherit: true, relative: $state.$current, notify: true });
    };

  });

  sfc.register.controller('sfcClassifierCreateCtrl', function ($scope, $rootScope, $state, $stateParams, SfcClassifierSvc, SfcClassifierHelper, SfcAclSvc, ServicePathSvc, ServiceForwarderSvc){

    $scope.data = {'scl-service-function-forwarder': []};

    if ($stateParams.itemKey) {
      SfcClassifierSvc.getItem($stateParams.itemKey, function (item) {
        $scope.data = item;
      });
    } else {
      $scope.data['scl-service-function-forwarder'].push({});
    }

    ServicePathSvc.getArray(function (data){
      $scope.sfps = data;
    });

    SfcAclSvc.getArray(function (data){
      $scope.acls = data;
    });

    ServiceForwarderSvc.getArray(function (data){
      $scope.sffs = data;
    });

    // form actions
    $scope.addSff = function (){
      SfcClassifierHelper.addSff($scope);
    };

    $scope.removeSff = function (index){
      SfcClassifierHelper.removeSff(index, $scope);
    };

    $scope.submit = function (){
      SfcClassifierSvc.putItem($scope.data, function (){
        $scope.$emit('reloadAcl');
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
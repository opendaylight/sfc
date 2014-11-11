define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.controller('serviceChainCtrl', function ($scope, $rootScope, ServiceFunctionSvc, ServiceChainSvc, ServicePathSvc, SfpToClassifierMappingSvc, ModalDeleteSvc, ModalSfNameSvc, ModalSfpInstantiateSvc, ModalInfoSvc, ModalErrorSvc, ngTableParams, $filter) {

    var NgTableParams = ngTableParams; // checkstyle 'hack'
    var thisCtrl = this;

    SfpToClassifierMappingSvc.init();

    $scope.tableParams = new NgTableParams(
      {
        page: 1,            // show first page
        count: 10           // count per page
      },
      {
        total: 0, // wait for 'sfcs'
        getData: function ($defer, params) {
          if (params.total() > 0) {
            // use build-in angular filter
            var orderedData = params.sorting() ?
              $filter('orderBy')($scope.sfcs, params.orderBy()) :
              $scope.sfcs;

            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          } else {
            $defer.resolve([]);
          }
        }
      }
    );

    this.sfcsWatcherRegistered = false;

    this.registerSfcsWatcher = function () { // wait for combining unpersisted with persisted in getArray callback

      if (!this.sfcsWatcherRegistered) {
        $scope.$watchCollection('sfcs', function (newVal) {
          if (angular.isUndefined(newVal)) {
            return;
          }
          $scope.tableParams.total($scope.sfcs.length);
          $scope.tableParams.reload();
        });

        this.sfcsWatcherRegistered = true;
      }
    };

    this.sortSfItemsByOrder = function sortSfItemsByOrder(sfc) {
      // fix data if 'order' property is undefined - set order index as served by restconf
      var idx = 0;
      _.each(sfc['sfc-service-function'], function (sfOfSfc) {
        if (angular.isUndefined(sfOfSfc.order)) {
          sfOfSfc.order = idx;
        }
        idx++;
      });

      sfc['sfc-service-function'] = $filter('orderBy')(
        sfc['sfc-service-function'],
        function getOrder(arg1) {
          return arg1.order;
        },
        false);
    };

    $scope.sfcEffectMe = {};

    $scope.getSFCstate = function getSFCstate(sfc) {
      return sfc.state || $rootScope.sfcState.PERSISTED;
    };

    $scope.setSFCstate = function setSFCstate(sfc, newState) {
      if (angular.isDefined(sfc.state) && sfc.state === $rootScope.sfcState.NEW) {
        sfc.state = $rootScope.sfcState.NEW;
      }
      else {
        sfc.state = newState;
      }
    };

    $scope.unsetSFCstate = function unsetSFCstate(sfc) {
      delete sfc.state;
    };

    $scope.isSFCstate = function isSFCstate(sfc, state) {
      return $scope.getSFCstate(sfc) === state;
    };

    ServiceFunctionSvc.getArray(function (data) {
      $scope.sfs = data;

      $scope.tableParamsSfType = new NgTableParams({
          page: 1,            // show first page
          count: 50           // count per page
        },
        {
          counts: [], // hide page counts control
          total: 1,  // value less than count hide pagination
          getData: function ($defer, params) {
            // use build-in angular filter
            var orderedData = params.sorting() ?
              $filter('orderBy')($scope.sfs, params.orderBy()) :
              $scope.sfs;

            $defer.resolve(orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count()));
          }
        });
    });

    ServiceChainSvc.getArray(function (data) {
      //temporary SFCs are kept in rootScope, persisted SFCs should be removed from it
      var tempSfcs = [];
      _.each($rootScope.sfcs, function (sfc) {
        if ($scope.getSFCstate(sfc) !== $rootScope.sfcState.PERSISTED) {
          tempSfcs.push(sfc);
        }
      });

      //concat temporary with loaded data (dont add edited sfcs which are already in tempSfcs)
      if (angular.isDefined(data)) {
        _.each(data, function (sfc) {
          //if it is not in tempSfcs add it
          if (angular.isUndefined(_.findWhere(tempSfcs, {name: sfc.name}))) {
            thisCtrl.sortSfItemsByOrder(sfc);
            $scope.setSFCstate(sfc, $rootScope.sfcState.PERSISTED);
            tempSfcs.push(sfc);
          }
        });
      }
      $rootScope.sfcs = tempSfcs;

      thisCtrl.registerSfcsWatcher(); // this will launch watcher
    });

    $scope.undoSFCnew = function undoSFCnew(sfc) {
      $rootScope.sfcs.splice(_.indexOf($rootScope.sfcs, sfc), 1);
    };

    $scope.undoSFCchanges = function undoSFCchanges(sfc) {
      ServiceChainSvc.getItem(sfc.name, function (oldSfc) {
        var index = _.indexOf($rootScope.sfcs, sfc);
        $rootScope.sfcs.splice(index, 1);
        $rootScope.sfcs.splice(index, 0, oldSfc);
      });
    };

    $scope.sortableOptions = {
      connectWith: ".connected-apps-container",
      cursor: 'move',
      placeholder: 'place',
      tolerance: 'pointer',
      start: function (e, ui) {
        $(e.target).data("ui-sortable").floating = true;
      },
      helper: function (e, ui) {
        var elms = ui.clone();
        elms.find(".arrow-right-part").addClass("arrow-empty");
        elms.find(".arrow-left-part").addClass("arrow-empty");
        return elms;
      },
      update: function (e, ui) {
        //sfc[0]: name, sfc[1]: index in sfcs
        try {
          var sfc = $(e.target).closest('tr').attr('id').split("~!~");
          $scope.setSFCstate(_.findWhere($rootScope.sfcs, {"name": sfc[0]}), $rootScope.sfcState.EDITED);
        } catch (err) {
          ModalErrorSvc.open({"head": "Unknown", "body": err.message});
          //alert(err.message);
        }
      }
    };

    $scope.onSFCdrop = function onSFCdrop($sf, sfc) {
      if (sfc['sfc-service-function'] === undefined) {
        sfc['sfc-service-function'] = [];
      }

      //check if SF with SF.type already exists in chain
      var sfNameExists = _.findWhere(sfc['sfc-service-function'], {name: $sf.type});

      //if SF with this name already exists in SFC, we need to use another name
      if (angular.isDefined(sfNameExists)) {

        ModalSfNameSvc.open(sfc, $sf, function (newSf) {
          if (angular.isDefined(newSf.name)) {
            sfc['sfc-service-function'].push({name: newSf.name, type: newSf.type});
            $scope.setSFCstate(sfc, $rootScope.sfcState.EDITED);
          }
        });
      }
      else {
        sfc['sfc-service-function'].push({name: $sf.type, type: $sf.type});
        $scope.setSFCstate(sfc, $rootScope.sfcState.EDITED);
      }
    };

    $scope.deleteSFC = function deleteSFC(sfc) {
      ModalDeleteSvc.open(sfc.name, function (result) {
        if (result == 'delete') {
          //delete the row
          ServiceChainSvc.deleteItem(sfc, function () {
            $rootScope.sfcs.splice(_.indexOf($rootScope.sfcs, sfc), 1);
          });
        }
      });
    };

    $scope.removeSFfromSFC = function removeSFfromSFC(sfc, index) {
      sfc['sfc-service-function'].splice(index, 1);
      $scope.setSFCstate(sfc, $rootScope.sfcState.EDITED);
    };

    $scope.persistSFC = function persistSFC(sfc) {
      $scope.unsetSFCstate(sfc);

      // set order index
      var idx = 0;
      _.each(sfc['sfc-service-function'], function (sfOfSfc) {
        sfOfSfc.order = idx;
        idx++;
      });

      ServiceChainSvc.putItem(sfc, function () {
        $scope.sfcEffectMe[sfc.name] = 1;
      });

    };

    $scope.deploySFC = function deploySFC(sfc) {

      ModalSfpInstantiateSvc.open(sfc, function (sfp) {
        //if user entered name in modal dialog (and it's unique name)
        if (angular.isDefined(sfp.name)) {
          ServicePathSvc.putItem(sfp, function (result) {
            if (angular.isDefined(result)) {
              // error
              var response = result.response;
              console.log(response);
              ModalErrorSvc.open({
                head: $scope.$eval('"SFC_CHAIN_MODAL_PATH_FAIL_HEAD" | translate'),
                rpcError: response
              });
            } else {
              // ok
              $rootScope.sfpEffectMe[sfp.name] = 1; // schedule for effect of recently deployed
              var modalBody = $scope.$eval('"SFC_CHAIN_MODAL_PATH_SUCCESS_BODY" | translate') + ": <b>'" + sfp.name + "'</b>.";

              if (angular.isDefined(SfpToClassifierMappingSvc.getClassifier(sfp['name']))) {
                SfpToClassifierMappingSvc.persistClassifier(sfp['name']);
                modalBody += "<br/><br/>" + $scope.$eval('"SFC_CHAIN_MODAL_PATH_SUCESSS_BODY_CLASSIFIER" | translate') + ": <b>'" +
                  SfpToClassifierMappingSvc.getClassifier(sfp['name'])['name'] + "'</b>.";
              }

              ModalInfoSvc.open({
                "head": $scope.$eval('"SFC_CHAIN_MODAL_PATH_SUCCESS_HEAD" | translate'),
                "body": modalBody
              });
            }
          });
        }
      });
    };
  });

  sfc.register.controller('serviceChainCreateCtrl', function ($scope, $rootScope, $state) {
    $scope.data = {};

    var thisCtrl = this;

    this.symmetricToBoolean = function (sfc) {
      if (angular.isDefined(sfc.symmetric)) {
        sfc.symmetric = sfc.symmetric == "true" ? true : false;
      }
    };

    $scope.submit = function () {
      $scope.data['sfc-service-function'] = [];
      $scope.data['state'] = $rootScope.sfcState.NEW;
      thisCtrl.symmetricToBoolean($scope.data);

      $rootScope.sfcs.push($scope.data);
      $state.transitionTo('main.sfc.servicechain', null, {
        location: true,
        inherit: true,
        relative: $state.$current,
        notify: true
      });
    };
  });

  sfc.register.controller('ModalSfpInstantiateCtrl', function ($scope, $modalInstance, sfc, SfpToClassifierMappingSvc) {

    $scope.sfc = sfc;
    $scope.freeClassifiers = SfpToClassifierMappingSvc.getFreeClassifiers();
    $scope.data = {};
    $scope.data.name = sfc.name + "-";

    $scope.save = function () {
      var sfp = {};
      sfp.name = this.data.name;
      sfp['service-chain-name'] = sfc.name;

      SfpToClassifierMappingSvc.setClassifier(sfp['name'], this.data.classifierName);

      $modalInstance.close(sfp);
    };

    $scope.dismiss = function () {
      $modalInstance.dismiss('cancel');
    };
  });

  sfc.register.controller('ModalSfNameCtrl', function ($scope, $modalInstance, sfc, sf) {

    $scope.sfc = sfc;
    $scope.sf = sf;

    $scope.save = function () {
      var newSfName = "";
      if (this.data.prefix) {
        newSfName = (this.data.prefix + "-");
      }
      newSfName = newSfName.concat($scope.sf.type);
      if (this.data.sufix) {
        newSfName = newSfName.concat("-" + this.data.sufix);
      }

      $scope.sf.name = newSfName;
      $modalInstance.close(sf);
    };

    $scope.dismiss = function () {
      $modalInstance.dismiss('cancel');
    };
  });

});
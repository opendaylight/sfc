define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServicePathHelper', function ($filter, $rootScope) {
    var svc = {};

    svc.orderHopsInSFP = function (sfp) {
      var orderedHops = $filter('orderBy')(sfp['service-path-hop'], "+'hop-number'");
      sfp['service-path-hop'] = orderedHops;
    };

    svc.getCountOfSfsInSFP = function (sfp) {
      var count = 0;
      _.each(sfp['service-path-hop'], function(hop){
        if(angular.isDefined(hop['service-function-name'])){
          count++;
        }
      });
      return count;
    };

    svc.updateHopsOrderInSFP = function (sfp) {
      var serviceIndex = svc.getCountOfSfsInSFP(sfp);
      sfp['starting-index'] = serviceIndex;

      _.each(sfp['service-path-hop'], function (hop, index) {
        //first hop should have the biggest index = total number of SFs
        hop['service_index'] = serviceIndex;
        hop['hop-number'] = index;

        //if hop is SF it consumes service_index thus decrement it
        if(angular.isDefined(hop['service-function-name'])){
          //until it equals 1
          if (serviceIndex != 1){
            serviceIndex--;
          }
        }
      });
    };

    svc.setSFPstate = function (sfp, newState) {
      if (angular.isDefined(sfp.state) && sfp.state === $rootScope.sfpState.NEW) {
        sfp.state = $rootScope.sfpState.NEW;
      }
      else {
        sfp.state = newState;
      }
    };

    return svc;
  });

  sfc.register.factory('ServicePathModalSffSelect', function ($modal) {
    var svc = {};
    var modalInstance;

    svc.open = function (sfName, sffNameList, callback) {
      modalInstance = $modal.open({
        templateUrl: 'src/app/sfc/servicepath/servicepath.modal.sff.select.tpl.html',
        controller: 'servicePathModalSffSelectCtrl',
        resolve: {
          sfName: function () {
            return sfName;
          },
          sffNameList: function () {
            return sffNameList;
          }
        }
      });

      modalInstance.result.then(function (result) {
        callback(result);
      }, function (reason) {
        callback(reason);
      });

      return modalInstance;
    };
    return svc;
  });

  sfc.register.factory('SfpToClassifierMappingSvc', function (SfcClassifierSvc, $rootScope, ModalErrorSvc) {
    var svc = {};

    var allClassifiers = {};
    var originSfp2ClassMap = {};
    var sfp2ClassMap = {};
    var freeClassifiers = {};

    svc.isReady = false;

    svc.init = function () {
      allClassifiers = {};
      originSfp2ClassMap = {};
      sfp2ClassMap = {};
      freeClassifiers = {};

      SfcClassifierSvc.getArray(function (classifiers) {
        _.each(classifiers, function (classifier) {

          if (classifier['service-function-path']) {
            originSfp2ClassMap[classifier['service-function-path']] = classifier;
            sfp2ClassMap[classifier['service-function-path']] = classifier;
          } else {
            freeClassifiers[classifier['name']] = classifier;
          }

          allClassifiers[classifier['name']] = classifier;
        });

        svc.isReady = true;
        $rootScope.$broadcast('sfpToClassifierMappingReady');
      });
    };

    svc.getClassifier = function (sfpName) {
      return sfp2ClassMap[sfpName];
    };

    svc.setClassifier = function (sfpName, classifierName) {
      sfp2ClassMap[sfpName] = allClassifiers[classifierName];
    };

    svc.getOriginClassifier = function (sfpName) {
      return originSfp2ClassMap[sfpName];
    };

    svc.undoClassifierChange = function (sfpName) {
      sfp2ClassMap[sfpName] = originSfp2ClassMap[sfpName];
    };

    svc.getClassifierByName = function (classifierName) {
      return allClassifiers[classifierName];
    };

    svc.getFreeClassifiers = function () {
      return _.values(freeClassifiers);
    };

    svc.persistClassifier = function (sfpName) {

      var originClassifier = originSfp2ClassMap[sfpName];
      var newClassifier = sfp2ClassMap[sfpName];

      if (originClassifier === newClassifier) {
        return;  // no change
      }

      var persistNew = function () {
        if (newClassifier && freeClassifiers[newClassifier['name']]) {
          newClassifier['service-function-path'] = sfpName;
          SfcClassifierSvc.putItem(newClassifier, function () {
            delete freeClassifiers[newClassifier['name']];
            originSfp2ClassMap[sfpName] = newClassifier;
          });
        } else if(newClassifier) {
          svc.undoClassifierChange(sfpName);
          ModalErrorSvc.open({"head": "Cannot attach classifier '" + newClassifier['name'] + "'",
            "body": 'Classifier ' + newClassifier['name'] + ' is already attached to another SFP !'});
        }
      };

      if (originClassifier) {
        delete originClassifier['service-function-path'];
        SfcClassifierSvc.putItem(originClassifier, function () {
          freeClassifiers[originClassifier['name']] = originClassifier;
          delete originSfp2ClassMap[sfpName];
          persistNew();
        });
      } else {
        persistNew();
      }

    };

    return svc;
  });
});
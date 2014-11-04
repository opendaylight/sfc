define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServiceChainHelper', function ($rootScope) {
    var svc = {};

    svc.setSFCstate = function setSFCstate(sfc, newState) {
      if (angular.isDefined(sfc.state) && sfc.state === $rootScope.sfcState.NEW) {
        sfc.state = $rootScope.sfcState.NEW;
      }
      else {
        sfc.state = newState;
      }
    };

    return svc;
  });

  sfc.register.factory('SfcToClassifierMappingSvc', function (SfcClassifierSvc, $rootScope, ModalErrorSvc) {
    var svc = {};

    var allClassifiers = {};
    var originSfc2ClassMap = {};
    var sfc2ClassMap = {};
    var freeClassifiers = {};

    svc.isReady = false;

    svc.init = function () {
      allClassifiers = {};
      originSfc2ClassMap = {};
      sfc2ClassMap = {};
      freeClassifiers = {};

      SfcClassifierSvc.getArray(function (classifiers) {
        _.each(classifiers, function (classifier) {

          if (classifier['service-function-chain']) {
            originSfc2ClassMap[classifier['service-function-chain']] = classifier;
            sfc2ClassMap[classifier['service-function-chain']] = classifier;
          } else {
            freeClassifiers[classifier['name']] = classifier;
          }

          allClassifiers[classifier['name']] = classifier;
        });

        svc.isReady = true;
        $rootScope.$broadcast('sfcToClassifierMappingReady');
      });
    };

    svc.getClassifier = function (sfcName) {
      return sfc2ClassMap[sfcName];
    };

    svc.setClassifier = function (sfcName, classifierName) {
      sfc2ClassMap[sfcName] = allClassifiers[classifierName];
    };

    svc.getOriginClassifier = function (sfcName) {
      return originSfc2ClassMap[sfcName];
    };

    svc.undoClassifierChange = function (sfcName) {
      sfc2ClassMap[sfcName] = originSfc2ClassMap[sfcName];
    };

    svc.getClassifierByName = function (classifierName) {
      return allClassifiers[classifierName];
    };

    svc.getFreeClassifiers = function () {
      return _.values(freeClassifiers);
    };

    svc.persistClassifier = function (sfcName) {

      var originClassifier = originSfc2ClassMap[sfcName];
      var newClassifier = sfc2ClassMap[sfcName];

      if (originClassifier === newClassifier) {
        return;  // no change
      }

      var persistNew = function () {
        if (newClassifier && freeClassifiers[newClassifier['name']]) {
          newClassifier['service-function-chain'] = sfcName;
          SfcClassifierSvc.putItem(newClassifier, function () {
            delete freeClassifiers[newClassifier['name']];
            originSfc2ClassMap[sfcName] = newClassifier;
          });
        } else {
          svc.undoClassifierChange(sfcName);
          ModalErrorSvc.open({"head": "Cannot attach classifier '" + newClassifier['name'] + "'",
                              "body": 'Classifier ' + newClassifier['name'] + ' is already attached to another SFC !'});
        }
      };

      if (originClassifier) {
        delete originClassifier['service-function-chain'];
        SfcClassifierSvc.putItem(originClassifier, function () {
          freeClassifiers[originClassifier['name']] = originClassifier;
          delete originSfc2ClassMap[sfcName];
          persistNew();
        });
      } else {
        persistNew();
      }

    };

    return svc;
  });

  sfc.register.factory('ModalSfpInstantiateSvc', function ($modal) {
    var svc = {};
    var modalInstance;

    svc.open = function (sfc, callback) {
      modalInstance = $modal.open({
        templateUrl: 'src/app/sfc/servicechain/servicechain.modal.instantiate.tpl.html',
        controller: 'ModalSfpInstantiateCtrl',
        resolve: {
          sfc: function () {
            return sfc;
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

  sfc.register.factory('ModalSfNameSvc', function ($modal) {
    var svc = {};
    var modalInstance;

    svc.open = function (sfc, sf, callback) {
      modalInstance = $modal.open({
        templateUrl: 'src/app/sfc/servicechain/servicechain.modal.sfname.tpl.html',
        controller: 'ModalSfNameCtrl',
        resolve: {
          sfc: function () {
            return sfc;
          },
          sf: function () {
            return sf;
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

});
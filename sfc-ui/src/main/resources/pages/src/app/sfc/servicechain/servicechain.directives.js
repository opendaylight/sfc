define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('serviceChainClassifier', function () {
    return {
      restrict: 'E',
      templateUrl: 'src/app/sfc/servicechain/servicechain.classifier.tpl.html',
      scope: {
        sfc: "=",
        idSuffix: "@"
      },
      controller: function ($scope, SfcToClassifierMappingSvc) {
        $scope.popUpVisible = false;

        $scope.fetchClassifier = function () {
          return SfcToClassifierMappingSvc.getClassifier($scope.sfc['name']);
        };

        $scope.showPopUp = function () {
          $scope.popUpVisible = true;
        };

        $scope.closePopUp = function () {
          $scope.popUpVisible = false;
        };
      }
    };
  });

  sfc.register.directive('serviceChainClassifierPopUp', function () {
    return {
      restrict: 'E',
      templateUrl: 'src/app/sfc/servicechain/servicechain.popup.classifier.tpl.html',
      scope: {
        sfc: "=",
        idSuffix: "@",
        closePopUp: "&"
      },
      controller: function ($scope, $rootScope, ServiceChainHelper, SfcToClassifierMappingSvc) {
        var thisCtrl = this;

        $scope.classifier = SfcToClassifierMappingSvc.getClassifier($scope.sfc['name']);
        $scope.originClassifier = SfcToClassifierMappingSvc.getOriginClassifier($scope.sfc['name']);

        if(angular.isDefined($scope.classifier)){
          $scope.classifierName = $scope.classifier['name'];
        }

        $scope.freeClassifiers = SfcToClassifierMappingSvc.getFreeClassifiers();

        $scope.save = function () {
          SfcToClassifierMappingSvc.setClassifier($scope.sfc['name'], $scope.classifierName);
          ServiceChainHelper.setSFCstate($scope.sfc, $rootScope.sfcState.EDITED);
          $scope.closePopUp();
        };

        $scope.onClassifierChange = function (classifierName) {
          $scope.classifier = SfcToClassifierMappingSvc.getClassifierByName(classifierName);
        };

      }
    };
  });

});
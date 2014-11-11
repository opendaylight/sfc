define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('servicePathContextMetadataPopUp', function () {
    return {
      restrict: 'E',
      templateUrl: 'src/app/sfc/servicepath/servicepath.popup.context.metadata.tpl.html',
      scope: {
        sfpState: "=",
        sfp: "=",
        contextMetadata: "=",
        idSuffix: "@"
      },
      controller: function ($scope, ServicePathHelper) {
        var thisCtrl = this;
        $scope.popUpVisible = false;

        $scope.showPopUp = function () {
          $scope.selectedMetadata = $scope.sfp['context-metadata'];
          $scope.popUpVisible = true;
        };

        $scope.closePopUp = function () {
          $scope.popUpVisible = false;
        };

        $scope.save = function () {
          $scope.sfp['context-metadata'] = $scope.selectedMetadata;
          ServicePathHelper.setSFPstate($scope.sfp, $scope.sfpState.EDITED);
          $scope.popUpVisible = false;
        };

        $scope.$watch('selectedMetadata', function (newValue) {
          if (angular.isUndefined(newValue)){
            return;
          }
          $scope.data = _.findWhere($scope.contextMetadata, {name: newValue});
          $scope.data = thisCtrl.contextMetadataToHex($scope.data);
        });

        this.contextMetadataToHex = function contextMetadataToHex(data) {
          if(angular.isUndefined(data)){
            return;
          }

          var contextMetadata = {};
          var decimalToHex = function (decimal) {
            return "0x" + decimal.toString(16);
          };

          contextMetadata['context-header1'] = decimalToHex(data['context-header1']);
          contextMetadata['context-header2'] = decimalToHex(data['context-header2']);
          contextMetadata['context-header3'] = decimalToHex(data['context-header3']);
          contextMetadata['context-header4'] = decimalToHex(data['context-header4']);
          return contextMetadata;
        };

      }
    };
  });

  sfc.register.directive('servicePathClassifier', function () {
    return {
      restrict: 'E',
      templateUrl: 'src/app/sfc/servicepath/servicepath.classifier.tpl.html',
      scope: {
        sfp: "=",
        idSuffix: "@"
      },
      controller: function ($scope, SfpToClassifierMappingSvc) {
        $scope.popUpVisible = false;

        $scope.fetchClassifier = function () {
          return SfpToClassifierMappingSvc.getClassifier($scope.sfp['name']);
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

  sfc.register.directive('servicePathClassifierPopUp', function () {
    return {
      restrict: 'E',
      templateUrl: 'src/app/sfc/servicepath/servicepath.popup.classifier.tpl.html',
      scope: {
        sfp: "=",
        idSuffix: "@",
        closePopUp: "&"
      },
      controller: function ($scope, $rootScope, ServicePathHelper, SfpToClassifierMappingSvc) {
        var thisCtrl = this;

        $scope.classifier = SfpToClassifierMappingSvc.getClassifier($scope.sfp['name']);
        $scope.originClassifier = SfpToClassifierMappingSvc.getOriginClassifier($scope.sfp['name']);

        if(angular.isDefined($scope.classifier)){
          $scope.classifierName = $scope.classifier['name'];
        }

        $scope.freeClassifiers = SfpToClassifierMappingSvc.getFreeClassifiers();

        $scope.save = function () {
          SfpToClassifierMappingSvc.setClassifier($scope.sfp['name'], $scope.classifierName);
          ServicePathHelper.setSFPstate($scope.sfp, $rootScope.sfpState.EDITED);
          $scope.closePopUp();
        };

        $scope.onClassifierChange = function (classifierName) {
          $scope.classifier = SfpToClassifierMappingSvc.getClassifierByName(classifierName);
        };

      }
    };
  });
});
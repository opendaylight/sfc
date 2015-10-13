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
        classifierLeaf: "@",
        symmetric: "=",
        idSuffix: "@"
      },
      controller: function ($scope) {
        $scope.popUpVisible = false;

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
        classifierLeaf: "@",
        idSuffix: "@",
        closePopUp: "&"
      },
      controller: function ($scope, $rootScope, ServicePathHelper, SfcClassifierSvc) {
        var thisCtrl = this;

        this.getClassifierByName = function (classifiersArray, classifierName) {
          if(angular.isDefined(classifiersArray) && angular.isDefined(classifierName)){
            return _.findWhere(classifiersArray, {name: classifierName});
          }
          else {
            return undefined;
          }
        };

        $scope.classifiersArray = [];

        SfcClassifierSvc.getArray(function (data) {
          $scope.classifiersArray = data;
          $scope.classifier = thisCtrl.getClassifierByName($scope.classifiersArray, $scope.sfp[$scope.classifierLeaf]);
        });

        $scope.onClassifierChange = function (classifierName) {
          $scope.classifier = thisCtrl.getClassifierByName($scope.classifiersArray, classifierName);
        };

        $scope.save = function () {
          ServicePathHelper.setSFPstate($scope.sfp, $rootScope.sfpState.EDITED);
          $scope.closePopUp();
        };
      }
    };
  });
});
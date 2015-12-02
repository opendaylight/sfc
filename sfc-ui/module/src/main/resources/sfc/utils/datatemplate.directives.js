define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.directive('sfcDataTemplate', function () {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        'templateDataModel': '=',
        'defaultTemplateDataModel': '=',
        'dialogId': '@'
      },
      templateUrl: 'src/app/sfc/utils/datatemplate.tpl.html',
      controller: function ($scope, SfcDataTemplateSvc) {
        var thisCtrl = this;

        this.initCtrl = function () {
          SfcDataTemplateSvc.initializeSvcForDialog($scope.dialogId, $scope.templateDataModel, $scope.defaultTemplateDataModel);
          thisCtrl.fetchTemplates();

          //enable watch
          thisCtrl.toggleTemplateDataModelWatch();

          //template save not required on init
          $scope.templateSaveRequired = false;
        };

        this.fetchTemplates = function () {
          var dataTemplatesNamesArray = Object.keys(SfcDataTemplateSvc.getAllDataTemplates($scope.dialogId));

          $scope.dataTemplates = [];
          _.each(dataTemplatesNamesArray, function (dataTemplateName) {
            $scope.dataTemplates.push({name: dataTemplateName});
          });
        };

        this.toggleWatch = function(watchExpr, fn) {
          var watchFn;
          return function() {
            if (watchFn) {
              watchFn();
              watchFn = undefined;
              //console.log("Disabled watch " + watchExpr);
            } else {
              watchFn = $scope.$watch(watchExpr, fn, true);
              //console.log("Enabled watch " + watchExpr);
            }
          };
        };

        this.templateDataModelWatchExpr = "templateDataModel";

        this.templateDataModelWatchFunction = function (newVal, oldVal) {
          if ((!_.isEqual(newVal, oldVal)) && SfcDataTemplateSvc.isAllowedToModifyDataTemplate($scope.selectedDataTemplateName)) {
            $scope.templateSaveRequired = true;
          }
        };

        this.toggleTemplateDataModelWatch = thisCtrl.toggleWatch(thisCtrl.templateDataModelWatchExpr, thisCtrl.templateDataModelWatchFunction);

        this.initCtrl();

        $scope.saveTemplate = function (dataTemplateName) {
          SfcDataTemplateSvc.addDataTemplate($scope.dialogId, dataTemplateName, $scope.templateDataModel);
          thisCtrl.fetchTemplates();
          $scope.templateSaveRequired = false;
        };

        $scope.loadTemplate = function (dataTemplateName) {
          //disable watch before loading
          thisCtrl.toggleTemplateDataModelWatch();

          $scope.selectedDataTemplateName = dataTemplateName;
          var loadedTemplate = SfcDataTemplateSvc.getDataTemplate($scope.dialogId, dataTemplateName);
          //if template exists - load it
          if (dataTemplateName && angular.isDefined(loadedTemplate)) {
            $scope.templateDataModel = loadedTemplate;
            $scope.templateSaveRequired = false;

            //enable watch after load
            thisCtrl.toggleTemplateDataModelWatch();
          }
          //if not, do not overwrite data and require new template save
          else if (SfcDataTemplateSvc.isAllowedToModifyDataTemplate($scope.selectedDataTemplateName)) {
            $scope.templateSaveRequired = true;

            //enable watch after creation of new template
            thisCtrl.toggleTemplateDataModelWatch();
          }
        };

        $scope.removeTemplate = function (dataTemplateName) {
          SfcDataTemplateSvc.removeDataTemplate($scope.dialogId, dataTemplateName);
          //null the select box
          $scope.selectedDataTemplateNameInternalDataModel = null;
          thisCtrl.fetchTemplates();
        };

        $scope.isAllowedToRemoveTemplate = function (dataTemplateName) {
          return SfcDataTemplateSvc.isAllowedToModifyDataTemplate(dataTemplateName);
        };
      }
    };
  });

});
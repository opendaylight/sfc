define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('SfcDataTemplateSvc', function ($localStorage) {

    var svc = {};

    $localStorage.$default({
      dataTemplates: {}
    });

    svc.dataTemplates = $localStorage.dataTemplates;
    svc.initialDataTemplates = {};

    svc.initializeSvcForDialog = function (dialogId, initialDataTemplate, defaultDataTemplate) {
      if (angular.isUndefined(svc.dataTemplates[dialogId])) {
        svc.dataTemplates[dialogId] = {};
      }

      //store initial DataTemplate each time (because it must correspond to data model)
      svc.initialDataTemplates[dialogId] = initialDataTemplate;

      //store default DataTemplate each time (because it must correspond to data model)
      svc.addDataTemplate(dialogId, 'default', defaultDataTemplate);
    };

    svc.getDataTemplate = function (dialogId, dataTemplateName) {
      if (angular.isDefined(svc.dataTemplates[dialogId][dataTemplateName])) {
        return angular.copy(svc.dataTemplates[dialogId][dataTemplateName]);
      }
      else if (angular.isUndefined(dataTemplateName) || dataTemplateName == null) {
        return angular.copy(svc.initialDataTemplates[dialogId]);
      }
    };

    svc.getAllDataTemplates = function (dialogId) {
      return angular.copy(svc.dataTemplates[dialogId]);
    };

    svc.addDataTemplate = function (dialogId, dataTemplateName, dataTemplate) {
      svc.dataTemplates[dialogId][dataTemplateName] = angular.copy(dataTemplate);
    };

    svc.removeDataTemplate = function (dialogId, dataTemplateName) {
      if (svc.isAllowedToModifyDataTemplate(dataTemplateName)){
        delete svc.dataTemplates[dialogId][dataTemplateName];
      }
    };

    svc.isAllowedToModifyDataTemplate = function (dataTemplateName) {
      if (angular.isDefined(dataTemplateName) && dataTemplateName !== 'default' && !_.isEmpty(dataTemplateName)) {
        return true;
      }
      else {
        return false;
      }
    };

    return svc;
  });
});
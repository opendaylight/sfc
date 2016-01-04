define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.service('SfcSystemSvc', function (SfcRestangularSvc) {

    var authorizationString = "karaf:karaf";
    var contentType = "application/x-www-form-urlencoded";

    var svc = {};

    svc.getBasicAuthHeader = function () {
      return btoa(authorizationString);
    };

    svc.getCurrentBaseUrl = function () {
      var baseUrl = SfcRestangularSvc.getCurrentBaseUrl();
      var indexOfRestconf = baseUrl.indexOf("restconf");

      if (angular.isDefined(indexOfRestconf)) {
        return baseUrl.substr(0, indexOfRestconf);
      }

      return null;
    };

    svc.baseSystemRest = function () {
      return SfcRestangularSvc.getCurrentInstance().oneUrl('', svc.getCurrentBaseUrl() + "system/console/");
    };

    svc.getLogs = function (callback) {
      return svc.baseSystemRest().customPOST("traces=true", 'logs', null, {
        'Authorization': 'Basic ' + svc.getBasicAuthHeader(),
        'Content-type': contentType
      })
        .then(function (result) {
          if (angular.isDefined(result.data)) {
            _.each(result.data, function (item) {
              item['id'] = parseInt(item['id'], 10);
              item['received'] = new Date(item['received']).toLocaleString();
            });
            callback(result.data);
          }
          else {
            callback([]);
          }
        });
    };

    svc.getFeatures = function (callback) {
      return svc.baseSystemRest().customPOST(null, 'features', null, {'Authorization': 'Basic ' + svc.getBasicAuthHeader()})
        .then(function (result) {
          callback(result);
        });
    };

    svc.getSfcFeatures = function (data) {
      var sfcFeatures = _.filter(data['features'], function (item) {
        return item['name'].match(/sfc/);
      });

      _.each(sfcFeatures, function (feature) {
        svc.expandFeatureRepository(feature, data['repositories']);
      });

      return sfcFeatures;
    };

    svc.expandFeatureRepository = function (feature, repositories) {
      var featureRepository = _.findWhere(repositories, {name: feature['repository']});

      if (angular.isDefined(featureRepository)) {
        feature['repository-url'] = featureRepository['url'];
      }
    };

    svc.refreshRepository = function (repositoryUrl, callback) {
      return svc.baseSystemRest().customPOST({action: 'refreshRepository', url: repositoryUrl}, 'features', null,
        {Authorization: 'Basic ' + svc.getBasicAuthHeader(), 'Content-type': contentType})
        .then(function (result) {
          callback(result);
        });
    };

    svc.uninstallFeature = function (featureName, featureVersion, callback) {
      return svc.baseSystemRest().customPOST("action=uninstallFeature" + "&feature=" + featureName + "&version=" + featureVersion, 'features', null,
        {Authorization: 'Basic ' + svc.getBasicAuthHeader(), 'Content-type': contentType})
        .then(function (result) {
          callback(result);
        });
    };

    svc.installFeature = function (featureName, featureVersion, callback) {
      return svc.baseSystemRest().customPOST("action=installFeature" + "&feature=" + featureName + "&version=" + featureVersion, 'features', null,
        {Authorization: 'Basic ' + svc.getBasicAuthHeader(), 'Content-type': contentType})
        .then(function (result) {
          callback(result);
        });
    };

    return svc;
  });

  sfc.register.factory('SfcSystemModalException', function ($modal) {
    var svc = {};
    var modalInstance;

    svc.open = function (log) {
      modalInstance = $modal.open({
        templateUrl: 'src/app/sfc/system/system.modal.exception.tpl.html',
        controller: 'sfcSystemModalExceptionCtrl',
        size: 'lg',
        resolve: {
          log: function () {
            return log;
          }
        }
      });
    };

    return svc;
  });

  sfc.register.factory('SfcMdsalCheckSvc', function (ServiceFunctionSvc, ServiceFunctionTypeSvc, ServiceChainSvc,
                                                     ServicePathSvc, ServiceNodeSvc, ServiceForwarderSvc, SfcAclSvc,
                                                     SfcContextMetadataSvc, SfcVariableMetadataSvc, SfcClassifierSvc,
                                                     RenderedServicePathSvc) {

    var self = this;

    this.servicesCount = arguments.length;

    this.arguments = arguments;

    this.checkMdsalStatus = function (callback) {
      var mdsalStatusArray = [];

      _.each(self.arguments, function (argument) {
        argument.availabilityCheckFunction(function (data, response) {
          if (angular.isDefined(response)) {
            if (response.statusText == "Not Found") {
              response.statusText = response.statusText.concat(" (empty datastore or wrong RESTconf URL)");
            }

            mdsalStatusArray.push({
              moduleName: argument.modelUrl,
              containerName: argument.containerName,
              listName: argument.listName,
              status: response.status,
              statusText: response.statusText
            });
          }
          else if (angular.isDefined(data)) {
            mdsalStatusArray.push({
              moduleName: argument.modelUrl,
              containerName: argument.containerName,
              listName: argument.listName,
              status: "200",
              statusText: "OK"
            });
          }

          self.servicesCount--;
          if (self.servicesCount === 0) {
            self.servicesCount = self.arguments.length;
            return callback(mdsalStatusArray);
          }
        });
      });
    };

    return this;
  });
});
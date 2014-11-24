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

});
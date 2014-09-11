define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServicePathHelper', function ($filter) {
    var svc = {};

    svc.orderHopsInSFP = function (sfp) {
      var orderedHops = $filter('orderBy')(sfp['service-path-hop'], "-service_index");
      sfp['service-path-hop'] = orderedHops;
    };

    svc.updateHopsOrderInSFP = function (sfp) {
      _.each(sfp['service-path-hop'], function (hop, index) {
        //first hop should have the biggest index = total number of hops (length of array)
        hop['service_index'] = sfp['service-path-hop'].length - index;
        hop['hop-number'] = index;
      });
    };

    svc.updateStartingIndexOfSFP = function (sfp) {
      sfp['starting-index'] = sfp['service-path-hop'].length;
    };

    return svc;
  });
});
define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('ServiceNodeTopologyBackend', function ($rootScope) {
    var svc = {};

    svc.createGraphData = function (nodeArray, sffs, sfs) {
      var graphData = [];
      //foreach service node
      _.each(nodeArray, function (sn) {
        var nodeSffs = [];

        //get info about its service functions
        _.each(sn['service-function'], function (sfName) {
          var sf = _.clone(_.findWhere(sfs, {name: sfName}));

          if (angular.isDefined(sf)) {
            //add necessary graph info about SF
            sf.text = {x: -50, y: 20, align: "top"};
            sf.image = {url: "assets/images/Device_switch_3062_unknown_64.png", wh: "40", xy: "-20"};
            sf.tooltipHtml =
              "<p style='text-align: center;'>" +
              $rootScope.$eval(('"SFC_TOOLTIP_FUNCTION" | translate')) + ":" + "</p>" +
              $rootScope.$eval(('"SFC_TOOLTIP_NAME" | translate')) + ": " + sf.name + "<br/>" +
              $rootScope.$eval(('"SFC_TOOLTIP_IP" | translate')) + ": " + sf['ip-mgmt-address'] + "<br/>" +
              $rootScope.$eval(('"SFC_TOOLTIP_TYPE" | translate')) + ": " + sf.type + "<br/>";

            //assign sfs to service function forwarders
            //first check if SFF is in SN SFFS list
            var sffInSn = _.findWhere(sn['service-function-forwarder'], sf['sf-data-plane-locator']['service-function-forwarder']);
            var sff;

            //if resides in SN
            if (angular.isDefined(sffInSn)) {
              //determine if it is assigned to nodeSffs list which contains SFF graph data
              var sffInNodeSffs = _.findWhere(nodeSffs, {name: sffInSn});
              if (angular.isDefined(sffInNodeSffs)) {
                //if yes, then simply get reference to it
                sff = sffInNodeSffs;
              } else {
                //if not, get it from global SFFs list and put it into nodeSffs list
                sff = _.clone(_.findWhere(sffs, {name: sffInSn}));
                nodeSffs.push(sff);
              }

              //if forwarder is already augmented with graph data, push sf into its children element
              if (angular.isDefined(sff.image)) {
                sff.children.push(sf);
              }
              //if not, augment SFF and then push SF into it
              else {
                sff.nodeType = "sff";
                sff.text = {x: -50, y: -80, align: "bottom"};
                sff.image = {url: "assets/images/GenericSoftswitch.png", wh: "50", xy: "-25"};
                sff.tooltipHtml = "<p style='text-align: center;'>" +
                  $rootScope.$eval(('"SFC_TOOLTIP_FORWARDER" | translate')) + ":" + "</p>" +
                  $rootScope.$eval(('"SFC_TOOLTIP_NAME" | translate')) + ": " + sff['name'] + "<br/>";
                sff.children = [];

                sff.children.push(sf);
              }
            }
            else {
              nodeSffs.push(sf);
            }
          }
        });

        //create SN graphData
        var nodeData = {
          "name": sn['name'],
          "text": {x: -50, y: -80, align: "bottom"},
          "image": {url: "assets/images/ibm_FEP.png", wh: "60", xy: "-30"},
          "tooltipHtml": "<p style='text-align: center;'>" +
            $rootScope.$eval(('"SFC_TOOLTIP_NODE" | translate')) + ":" + "</p>" +
            $rootScope.$eval(('"SFC_TOOLTIP_NAME" | translate')) + ": " + sn['name'] + "<br/>" +
            $rootScope.$eval(('"SFC_TOOLTIP_IP" | translate')) + ": " + sn['ip-mgmt-address'] + "<br/>",
          "children": nodeSffs
        };

        graphData.push(nodeData);
      });
      return graphData;
    };

    svc.createGraphDataExperimental = function (nodeArray, sffs, sfs) {
      var graphData = [];
      //foreach service node
      _.each(nodeArray, function (sn) {
        var nodeSffs = [];
        var unAssignedSfs = _.clone(sn['service-function']);

        _.each(sn['service-function-forwarder'], function (sffName) {
          var sff = _.clone(_.findWhere(sffs, {name: sffName}));
          sff.nodeType = "sff";
          sff.text = {x: -50, y: -80, align: "bottom"};
          sff.image = {url: "assets/images/GenericSoftswitch.png", wh: "50", xy: "-25"};
          sff.tooltipHtml = "<p style='text-align: center;'>" +
            $rootScope.$eval(('"SFC_TOOLTIP_FORWARDER" | translate')) + ":" + "</p>" +
            $rootScope.$eval(('"SFC_TOOLTIP_NAME" | translate')) + ": " + sff['name'] + "<br/>";
          sff.children = [];

          _.each(sff['service-function-dictionary'], function (sf) {
            sf.text = {x: -50, y: 20, align: "top"};
            sf.image = {url: "assets/images/Device_switch_3062_unknown_64.png", wh: "40", xy: "-20"};
            sf.tooltipHtml =
              "<p style='text-align: center;'>" +
              $rootScope.$eval(('"SFC_TOOLTIP_FUNCTION" | translate')) + ":" + "</p>" +
              $rootScope.$eval(('"SFC_TOOLTIP_NAME" | translate')) + ": " + sf.name + "<br/>" +
              $rootScope.$eval(('"SFC_TOOLTIP_IP" | translate')) + ": " + sf['sff-sf-data-plane-locator']['ip'] + ":" + sf['sff-sf-data-plane-locator']['port'] + "<br/>" +
              $rootScope.$eval(('"SFC_TOOLTIP_TYPE" | translate')) + ": " + sf.type + "<br/>";

            sff.children.push(sf);

            var assignedSfIndex = _.indexOf(unAssignedSfs, sf.name);
            if (assignedSfIndex > -1) {
              unAssignedSfs.splice(assignedSfIndex, 1);
            }

          });
          nodeSffs.push(sff);
        });

        _.each(unAssignedSfs, function (sfName) {

          var sf = _.clone(_.findWhere(sfs, {name: sfName}));
          if (angular.isDefined(sf)) {
            //add necessary graph info about SF
            sf.text = {x: -50, y: 20, align: "top"};
            sf.image = {url: "assets/images/Device_switch_3062_unknown_64.png", wh: "40", xy: "-20"};
            sf.tooltipHtml =
              "<p style='text-align: center;'>" +
              $rootScope.$eval(('"SFC_TOOLTIP_FUNCTION" | translate')) + ":" + "</p>" +
              $rootScope.$eval(('"SFC_TOOLTIP_NAME" | translate')) + ": " + sf.name + "<br/>" +
              $rootScope.$eval(('"SFC_TOOLTIP_IP" | translate')) + ": " + sf['ip-mgmt-address'] + "<br/>" +
              $rootScope.$eval(('"SFC_TOOLTIP_TYPE" | translate')) + ": " + sf.type + "<br/>";

            nodeSffs.push(sf);
          }
        });

        //create SN graphData
        var nodeData = {
          "name": sn['name'],
          "text": {x: -50, y: -80, align: "bottom"},
          "image": {url: "assets/images/ibm_FEP.png", wh: "60", xy: "-30"},
          "tooltipHtml": "<p style='text-align: center;'>" +
            $rootScope.$eval(('"SFC_TOOLTIP_NODE" | translate')) + ":" + "</p>" +
            $rootScope.$eval(('"SFC_TOOLTIP_NAME" | translate')) + ": " + sn['name'] + "<br/>" +
            $rootScope.$eval(('"SFC_TOOLTIP_IP" | translate')) + ": " + sn['ip-mgmt-address'] + "<br/>",
          "children": nodeSffs
        };

        graphData.push(nodeData);
      });
      return graphData;
    };

    svc.createGraphDataExperimentalSFF = function (nodeArray, sffs, sfs) {
      var graphData = [];

      //foreach SFF
      _.each(sffs, function (sff) {
        var sn = _.clone(_.findWhere(nodeArray, {name: sff['service-node']}));

        //if SN is not in graphData -> add it with metadata
        if (angular.isDefined(sn) && !_.findWhere(graphData, {name: sn['name']})) {
          sn.text = {x: -50, y: -80, align: "bottom"};
          sn.image = {url: "assets/images/ibm_FEP.png", wh: "60", xy: "-30"};
          sn.tooltipHtml = "<p style='text-align: center;'>" +
            $rootScope.$eval(('"SFC_TOOLTIP_NODE" | translate')) + ":" + "</p>" +
            $rootScope.$eval(('"SFC_TOOLTIP_NAME" | translate')) + ": " + sn['name'] + "<br/>" +
            $rootScope.$eval(('"SFC_TOOLTIP_IP" | translate')) + ": " + sn['ip-mgmt-address'] + "<br/>";
          sn.children = [];

          graphData.push(sn);
        }
        else if(!_.findWhere(graphData, {name: sff['service-node']})){
          sn = {};
          sn.name = sff['service-node'];
          sn.text = {x: -50, y: -80, align: "bottom"};
          sn.image = {url: "assets/images/ibm_FEP.png", wh: "60", xy: "-30"};
          sn.tooltipHtml = "<p style='text-align: center;'>" +
            $rootScope.$eval(('"SFC_TOOLTIP_NODE" | translate')) + ":" + "</p>" +
            $rootScope.$eval(('"SFC_TOOLTIP_NAME" | translate')) + ": " + sn['name'] + "<br/>" +
            $rootScope.$eval(('"SFC_TOOLTIP_IP" | translate')) + ": " + "unknown" + "<br/>";
          sn.children = [];

          graphData.push(sn);
        }

        var tempSn = _.findWhere(graphData, {name: sff['service-node']});
        var tempSff = _.clone(sff);
        tempSff.text = {x: -50, y: -80, align: "bottom"};
        tempSff.image = {url: "assets/images/GenericSoftswitch.png", wh: "50", xy: "-25"};
        tempSff.tooltipHtml = "<p style='text-align: center;'>" +
          $rootScope.$eval(('"SFC_TOOLTIP_FORWARDER" | translate')) + ":" + "</p>" +
          $rootScope.$eval(('"SFC_TOOLTIP_NAME" | translate')) + ": " + sff['name'] + "<br/>";
        tempSff.children = [];

        tempSn.children.push(tempSff);

        _.each(tempSff['service-function-dictionary'], function (sf) {
          sf.text = {x: -50, y: 20, align: "top"};
          sf.image = {url: "assets/images/Device_switch_3062_unknown_64.png", wh: "40", xy: "-20"};
          sf.tooltipHtml =
            "<p style='text-align: center;'>" +
            $rootScope.$eval(('"SFC_TOOLTIP_FUNCTION" | translate')) + ":" + "</p>" +
            $rootScope.$eval(('"SFC_TOOLTIP_NAME" | translate')) + ": " + sf.name + "<br/>" +
            $rootScope.$eval(('"SFC_TOOLTIP_IP" | translate')) + ": " + sf['sff-sf-data-plane-locator']['ip'] + ":" + sf['sff-sf-data-plane-locator']['port'] + "<br/>" +
            $rootScope.$eval(('"SFC_TOOLTIP_TYPE" | translate')) + ": " + sf.type + "<br/>";

          tempSff.children.push(sf);

        });
      });
      return graphData;
    };

    svc._removeIds = function (element) {
      _.each(element.children, function (children) {
        if (angular.isDefined(children)) {
          svc._removeIds(children);
          delete children.id;
        }
      });
    };

    // Toggle children on click.
    svc._click = function (d) {
      if (d.children) {
        d._children = d.children;
        d.children = null;
      } else {
        d.children = d._children;
        d._children = null;
      }
    };

    svc._collapse = function (d) {
      if (d.children) {
        d._children = d.children;
        d._children.forEach(svc._collapse);
        d.children = null;
      }
    };

    svc.msieversion = function () {
      var ua = window.navigator.userAgent;
      var msie = ua.indexOf("MSIE ");

      // If Internet Explorer, return true
      if (msie > 0 || !!navigator.userAgent.match(/Trident.*rv\:11\./)) {
        return true;
      }
      // If another browser, return false
      else {
        return false;
      }
    };

    return svc;
  });

});


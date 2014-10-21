define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('sfcFormatMessage', function () {

    // message formatting helper function; replace placeholders ; usage:  format("... {0} {1}", arg0, arg1);
    var formatMessageFunc = function () {
      var template = arguments[0],
        templateArgs = arguments,
        stringify = function (obj) {
          if (typeof obj === 'function') {
            return obj.toString().replace(/ \{[\s\S]*$/, '');
          } else if (typeof obj === 'undefined') {
            return 'undefined';
          } else if (typeof obj !== 'string') {
            return angular.toJson(obj, true);
          }
          return obj;
        },
        message;

      message = template.replace(/\{\d+\}/g, function (match) {
        var index = +match.slice(1, -1), arg;

        if (index + 1 < templateArgs.length) {
          arg = templateArgs[index + 1];
          return stringify(arg);
        }

        return match;
      });

      return message;
    };

    return formatMessageFunc;
  });


  sfc.register.factory('SfcErrorSvc', function (sfcFormatMessage) {

    // constructor
    function SfcErrorSvc() {
      try {
        return new Error(sfcFormatMessage.apply(this, arguments));
      } catch (e) {
        // format failed
        return new Error(arguments);
      }
    }

    return SfcErrorSvc;
  });

  sfc.register.factory('SfcRestconfError', function () {

    // constructor
    function SfcRestconfError(response, modelUrl) {
      this.response = response;
      this.modelUrl = modelUrl;
    }

    return SfcRestconfError;
  });

  sfc.register.factory('SfcRestangularSvc', function (Restangular) {

    var svc = {};

    svc.newBaseUrl = "http://localhost:8181/restconf";

    svc.currentInstance = Restangular;

    svc.getCurrentInstance = function () { // apply change if necessary

      if (svc.newBaseUrl && svc.newBaseUrl != svc.currentInstance.configuration.baseUrl) {
        // renew if changed baseUrl
        svc.currentInstance = Restangular.withConfig(function (RestangularConfigurer) {
          RestangularConfigurer.setBaseUrl(svc.newBaseUrl);
        });
        svc.newBaseUrl = null;
      }
      return svc.currentInstance;
    };

    svc.changeBaseUrl = function (url) {
      svc.newBaseUrl = url;
    };

    svc.getCurrentBaseUrl = function () {
      return svc.getCurrentInstance().configuration.baseUrl;
    };

    return svc;
  });

// ******* SfcRestBaseSvc *********
  sfc.register.factory('SfcRestBaseSvc', function (SfcRestangularSvc, SfcRestconfError) {

    // constructor
    function SfcRestBaseSvc(_modelUrl, _containerName, _listName) {
      this.modelUrl = _modelUrl;
      this.containerName = _containerName;
      this.listName = _listName;
    }


    SfcRestBaseSvc.prototype.baseRest = function () {
      return SfcRestangularSvc.getCurrentInstance().one('config');
    };

    SfcRestBaseSvc.prototype.baseRpcRest = function () {
      return SfcRestangularSvc.getCurrentInstance().one('operations');
    };


    SfcRestBaseSvc.prototype.postRpc = function (input, operationName, params) {
      var headers = {
        "Content-Type": "application/yang.data+json",
        "Accept": "application/yang.data+json"
      };
      return this.baseRpcRest().customPOST(input, this.modelUrl + ':' + operationName, params, headers);
    };

    SfcRestBaseSvc.prototype.executeRpcOperation = function (input, operationName, params, callback) {
      var instance = this; // save 'this' to closure

      this.postRpc(input, operationName, params).then(function (result) {
        callback(result['output']); // return rpc response output
      }, /* on error*/ function (response) {
        callback(new SfcRestconfError(response)); // SfcRestconfError
      });
    };


    SfcRestBaseSvc.prototype.getListKeyFromItem = function (itemData) {
      return itemData['name']; // default
    };

    SfcRestBaseSvc.prototype.put = function (elem, key) {
      return this.baseRest().customPUT(elem, this.modelUrl + ':' + this.containerName + '/' + this.listName + '/' + key);
    };

    SfcRestBaseSvc.prototype.putContainer = function (containerElem) {
      return this.baseRest().customPUT(containerElem, this.modelUrl + ":" + this.containerName);
    };

    SfcRestBaseSvc.prototype._delete = function (key) {
      return this.baseRest().customDELETE(this.modelUrl + ':' + this.containerName + '/' + this.listName + '/' + key);
    };

    SfcRestBaseSvc.prototype.getOne = function (key) {
      return this.baseRest().customGET(this.modelUrl + ":" + this.containerName + '/' + this.listName + '/' + key);
    };

    SfcRestBaseSvc.prototype.getItem = function (key, callback) {
      var instance = this; // save 'this' to closure

      this.getOne(key).then(function (result) {
        var stripped = instance.stripNamespacePrefixes(result[instance.listName]);
        callback(stripped[0]); // return only nested object
      }, /* on error*/ function (response) {

        if (response.status = "404") {
          console.log("No data, returning empty item");
        } else {
          console.error("Error with status code ", response.status);
        }

        callback({}); // return empty item
      });
    };

    SfcRestBaseSvc.prototype.getAll = function () {
      return this.baseRest().customGET(this.modelUrl + ":" + this.containerName);
    };

    SfcRestBaseSvc.prototype.exportContainer = function (receiveCallback) {
      var instance = this;

      this.getAll().then(
        // success
        function (restangularObject) {
          var extracted = {};
          extracted[instance.containerName] = restangularObject[instance.containerName];
          receiveCallback(extracted);
        },
        // error
        function (errorResponse) {
          receiveCallback(new SfcRestconfError(errorResponse, instance.modelUrl));
        }
      );
    };

    SfcRestBaseSvc.prototype.getArray = function (callback) {
      var instance = this; // save 'this' to closure

      this.getAll().then(function (result) {
        var stripped = instance.stripNamespacePrefixes(result[instance.containerName][instance.listName]);
        callback(stripped); // return only nested array
      }, /* on error*/ function (response) {

        if (response.status = "404") {
          console.log("No data, returning empty array");
        } else {
          console.error("Error with status code ", response.status);
        }

        callback([]); // return empty array
      });
    };

    SfcRestBaseSvc.prototype.wrapInListname = function (item) {
      var result = {};
      result[this.listName] = item;
      return result;
    };

    SfcRestBaseSvc.prototype.checkRequired = function (item) {
      var key = this.getListKeyFromItem(item);

      if (!key || _.isEmpty(key)) {
        throw new Error('list key is undefined or empty');
      }
    };

    SfcRestBaseSvc.prototype.putItem = function (item, callback) {

      this.checkRequired(item);

      var wrappedElem = this.wrapInListname(item);

      this.put(wrappedElem, this.getListKeyFromItem(item)).then(function () {
        if (callback) {
          callback();
        }
      }, /* on error*/ function (response) {
        console.log("Error with status code", response.status, " while PUT");
        if (callback) {
          callback(response); // on REST error pass response
        }
      });
    };

    SfcRestBaseSvc.prototype.putContainerWrapper = function (containerData, callback) {

      this.putContainer(containerData).then(function () {
        if (callback) {
          callback();
        }
      }, /* on error*/ function (response) {
        console.log("Error with status code", response.status, " while PUT");
        if (callback) {
          callback(response, containerData); // on REST error pass response
        }
      });
    };

    SfcRestBaseSvc.prototype.deleteItem = function (item, callback) {

      this.checkRequired(item);

      this._delete(this.getListKeyFromItem(item)).then(function () {
        if (callback) {
          callback();
        }
      }, /* on error*/ function (response) {
        console.log("Error with status code", response.status, " while DELETE");
        if (callback) {
          callback(response); // on REST error pass response
        }
      });
    };

    // to be overriden
    SfcRestBaseSvc.prototype.stripNamespacePrefixes = function (itemArray) {
      // noop
      return itemArray;
    };

    return SfcRestBaseSvc;  // return uninstatiated prototype
  });


  // ******* ServiceFunctionSvc *********
  sfc.register.factory('ServiceFunctionSvc', function (SfcRestBaseSvc) {
    var modelUrl = 'service-function';
    var containerName = 'service-functions';
    var listName = 'service-function';

    // constructor
    function ServiceFunctionSvc() {
    }

    ServiceFunctionSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    // @override
    ServiceFunctionSvc.prototype.stripNamespacePrefixes = function (sfsArray) {

      var matcher = new RegExp("^service-function:");

      _.each(sfsArray, function (sf) {

        if (!_.isEmpty(sf.type)) {
          sf.type = sf.type.replace(matcher, "");
        }

        _.each(sf['sf-data-plane-locator'], function (locator) {
          if (!_.isEmpty(locator.transport)) {
            locator.transport = locator.transport.replace(matcher, "");
          }
        });
      });

      return sfsArray;
    };

    return new ServiceFunctionSvc();
  });


  // ******* ServiceChainSvc *********
  sfc.register.factory('ServiceChainSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'service-function-chain';
    var containerName = 'service-function-chains';
    var listName = 'service-function-chain';

    // constructor
    function ServiceChainSvc() {
    }

    ServiceChainSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    ServiceChainSvc.prototype.createInstance = function (sfcName, sfNames) {

      var sfsNested = [];

      _.each(sfNames, function (sfName) {
        sfsNested.push({"name": sfName});
      });

      return {
        "service-function": sfsNested,
        "name": sfcName
      };
    };

    ServiceChainSvc.prototype.deployChain = function (sfcName, callback) {

      var input = {
        "service-function-chain:input": {
          "name": sfcName
        }
      };

      this.executeRpcOperation(input, "instantiate-service-function-chain", undefined, callback);
    };

    return new ServiceChainSvc();
  });


  // ******* ServicePathSvc *********
  sfc.register.factory('ServicePathSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'service-function-path';
    var containerName = 'service-function-paths';
    var listName = 'service-function-path';

    // constructor
    function ServicePathSvc() {
    }

    ServicePathSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    /*  not used

     ServicePathSvc.prototype.createInstance = function (sfpName, sfNames) {

     var sfsNested = [];

     _.each(sfNames, function (sfName) {
     sfsNested.push(sfName);
     });

     return {
     "service-function-instance": sfsNested,
     "name": sfpName
     };
     };*/

    return new ServicePathSvc();
  });


  // *** ServiceNodeSvc **********************
  sfc.register.factory('ServiceNodeSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'service-node';
    var containerName = 'service-nodes';
    var listName = 'service-node';

    // constructor
    function ServiceNodeSvc() {
    }

    ServiceNodeSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);


    /*  not used

     ServiceNodeSvc.prototype.createInstance = function (name, type, transport, ipMgmtAddress, failmode, sfNamesArray) {

     // check is array
     if (_.isEmpty(sfNamesArray) || !_.isArray(sfNamesArray)) {
     throw new Error("Illegal argument: sfNamesArray is not an array");
     }

     // check function names
     _.each(sfNamesArray, function (_sfName) {
     if (_.isEmpty(_sfName) || typeof _sfName !== 'string') {
     throw new Error("Illegal argument: item in sfNamesArray");
     }
     });

     return {
     "service-function": sfNamesArray,
     "name": name,
     "ip-mgmt-address": ipMgmtAddress
     };
     };*/

    // @override
    ServiceNodeSvc.prototype.stripNamespacePrefixes = function (snArray) {

      var matcher = new RegExp("^service-node:");

      _.each(snArray, function (sn) {

        if (!_.isEmpty(sn.type)) {
          sn.type = sn.type.replace(matcher, "");
          sn.failmode = sn.failmode.replace(matcher, "");
          sn.transport = sn.transport.replace(matcher, "");
        }
      });

      return snArray;
    };

    return new ServiceNodeSvc();
  });

// ******* ServiceForwarderSvc *********
  sfc.register.factory('ServiceForwarderSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'service-function-forwarder';
    var containerName = 'service-function-forwarders';
    var listName = 'service-function-forwarder';

    // constructor
    function ServiceForwarderSvc() {
    }

    ServiceForwarderSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    // @override
    ServiceForwarderSvc.prototype.stripNamespacePrefixes = function (sffArray) {

      var matcher = new RegExp("^service-function-forwarder:");
      var serviceLocatorMatcher = new RegExp("^service-locator:");

      _.each(sffArray, function (sff) {
        if (!_.isEmpty(sff['sff-data-plane-locator'])) {

          _.each(sff['sff-data-plane-locator'], function (locator) {
            if (angular.isDefined(locator['data-plane-locator']['transport'])) {
//                locator['data-plane-locator']['transport'] = locator['data-plane-locator']['transport'].replace(matcher, "");
              locator['data-plane-locator']['transport'] = locator['data-plane-locator']['transport'].replace(serviceLocatorMatcher, "");
            }
          });
        }

        if (!_.isEmpty(sff['service-function-dictionary'])) {

          _.each(sff['service-function-dictionary'], function (dictionary) {
            if (angular.isDefined(dictionary['failmode'])) {
              dictionary['failmode'] = dictionary['failmode'].replace(matcher, "");
            }

            if (angular.isDefined(dictionary['sff-sf-data-plane-locator']['transport'])) {
              dictionary['sff-sf-data-plane-locator']['transport'] = dictionary['sff-sf-data-plane-locator']['transport'].replace(matcher, "");
            }
          });
        }

      });
      return sffArray;
    };

    return new ServiceForwarderSvc();
  });


  // *** SfcAclSvc **********************
  sfc.register.factory('SfcAclSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'ietf-acl';
    var containerName = 'access-lists';
    var listName = 'access-list';

    // constructor
    function SfcAclSvc() {
      this['subListName'] = "access-list-entry";
      this['subListKeyName'] = "rule-name";
    }

    SfcAclSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    // @override
    SfcAclSvc.prototype.stripNamespacePrefixes = function (aclArray) {

      _.each(aclArray, function (acl) {
        if (!_.isEmpty(acl['access-list-entries'])) {

          _.each(acl['access-list-entries'], function (ace) {
            var actions = ace['actions'];
            if (angular.isDefined(actions)) {
              if (angular.isDefined(actions["service-function-acl:service-function-path"])) {
                actions["service-function-path"] = actions["service-function-acl:service-function-path"];
                delete actions["service-function-acl:service-function-path"];
              }
            }
          });
        }
      });

      return aclArray;
    };

    // @override
    SfcAclSvc.prototype.getListKeyFromItem = function (itemData) {
      return itemData['acl-name'];
    };


    SfcAclSvc.prototype.deleteItemByKey = function (itemKey, callback) {
      var item ={};
      item['acl-name'] = itemKey;
      this.deleteItem(item, callback);
    };


    // sub item functions

    SfcAclSvc.prototype._deleteSub = function (key, subKey) {
      return this.baseRest().customDELETE(this.modelUrl + ':' + this.containerName + '/' + this.listName + '/' + key + '/' + this.subListName + '/' + subKey);
    };


    SfcAclSvc.prototype.deleteSubItemByKey = function (itemKey, subItemKey, callback) {
      this._deleteSub(itemKey, subItemKey).then(function () {
        if (callback) {
          callback();
        }
      }, /* on error*/ function (response) {
        console.log("Error with status code", response.status, " while DELETE");
        if (callback) {
          callback(response); // on REST error pass response
        }
      });
    };

    return new SfcAclSvc();
  });

// ******* SfcContextMetadataSvc *********
  sfc.register.factory('SfcContextMetadataSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'service-function-metadata';
    var containerName = 'service-function-metadata';
    var listName = 'context-metadata';

    // constructor
    function SfcContextMetadataSvc() {
    }

    SfcContextMetadataSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    return new SfcContextMetadataSvc();
  });

  // ******* SfcVariableMetadataSvc *********
  sfc.register.factory('SfcVariableMetadataSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'service-function-metadata';
    var containerName = 'service-function-metadata';
    var listName = 'variable-metadata';

    // constructor
    function SfcVariableMetadataSvc() {
    }

    SfcVariableMetadataSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    return new SfcVariableMetadataSvc();
  });


}); // end define
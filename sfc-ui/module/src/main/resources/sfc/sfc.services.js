define(['app/sfc/sfc.module'], function (sfc) {

  sfc.register.factory('SfcValidatorSvc', function () {

    var svc = {};

    svc.string = function (ctrl) {
      return function (viewValue) {
        return viewValue;
      };
    };

    svc['inet:port-number'] = function (ctrl) {
      return svc.port(ctrl);
    };

    svc.port = function (ctrl) {
      return function (viewValue) {
        if (viewValue === null || viewValue === "") {
          ctrl.$setValidity('port', true);
          return null;
        }
        else if (viewValue >= 0 && viewValue <= 65535) {
          ctrl.$setValidity('port', true);
          return viewValue;
        }
        else {
          ctrl.$setValidity('port', false);
          return undefined;
        }
      };
    };

    svc['inet:ip-address'] = function (ctrl) {
      return svc.ipAddress(ctrl);
    };

    svc.ipAddress = function (ctrl, params) {
      return function (viewValue) {
        if (viewValue === null || viewValue === "") {
          ctrl.$setValidity('ipAddress', true);
          return null;
        }
        else if (svc.__inet_pton(viewValue, params)) {
          ctrl.$setValidity('ipAddress', true);
          return viewValue;
        }
        else {
          ctrl.$setValidity('ipAddress', false);
          return undefined;
        }
      };
    };

    svc.__isHexadecimal = function (string) {
      return string.match(/^0x[0-9A-Fa-f]+$/) ? true : false;
    };

    svc.__inet_pton = function (a, params) {
      //  discuss at: http://phpjs.org/functions/inet_pton/
      // original by: Theriault
      //   example 1: inet_pton('::');
      //   returns 1: '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0'
      //   example 2: inet_pton('127.0.0.1');
      //   returns 2: '\x7F\x00\x00\x01'

      // enhanced by: Andrej Kincel (akincel@cisco.com)
      //    features: IPv4 regex checks for valid range

      var r, m, x, i, j, f = String.fromCharCode, prefix;

      if (params && params.prefix === true) {
        m = a.match(/(\/)([0-9]+)$/);
        if (m) {
          prefix = parseInt(m[2]);
          a = a.replace(/\/[0-9]+$/, ""); // trim prefix
        } else {
          return false;
        }
      }

      // IPv4
      if (!params || params.version != 6) {
        m = a.match(/^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/);
        if (m) {

          if (prefix && prefix > 32) {
            return false;
          }

          m = m[0].split('.');
          m = f(m[0]) + f(m[1]) + f(m[2]) + f(m[3]);
          // Return if 4 bytes, otherwise false.
          return m.length === 4 ? m : false;
        }
      }

      if (!params || params.version != 4) {
        // IPv6
        r = /^((?:[\da-fA-F]{1,4}(?::|)){0,8})(::)?((?:[\da-fA-F]{1,4}(?::|)){0,8})$/;
        m = a.match(r);
        if (m) {

          if (prefix && prefix > 128) {
            return false;
          }

          // Translate each hexadecimal value.
          for (j = 1; j < 4; j++) {
            // Indice 2 is :: and if no length, continue.
            if (j === 2 || m[j].length === 0) {
              continue;
            }
            m[j] = m[j].split(':');
            for (i = 0; i < m[j].length; i++) {
              m[j][i] = parseInt(m[j][i], 16);
              // Would be NaN if it was blank, return false.
              if (isNaN(m[j][i])) {
                // Invalid IP.
                return false;
              }
              m[j][i] = f(m[j][i] >> 8) + f(m[j][i] & 0xFF);
            }
            m[j] = m[j].join('');
          }
          x = m[1].length + m[3].length;
          if (x === 16) {
            return m[1] + m[3];
          }
          else if (m[2] !== undefined) {
            if (x < 16 && m[2].length > 0) {
              return m[1] + (new Array(16 - x + 1))
                  .join('\x00') + m[3];
            }
          }
        }
      }
      // Invalid IP.
      return false;
    };

    svc.dateAndTime = function (ctrl) {
      return function (viewValue) {
        if (viewValue === null || viewValue === "") {
          ctrl.$setValidity('dateAndTime', true);
          return null;
        }
        else if (viewValue.match(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[\+\-]\d{2}:\d{2})/)) {
          ctrl.$setValidity('dateAndTime', true);
          return viewValue;
        }
        else {
          ctrl.$setValidity('dateAndTime', false);
          return undefined;
        }
      };
    };

    svc.vlanId = function (ctrl) {
      return function (viewValue) {
        if (viewValue === null || viewValue === "") {
          ctrl.$setValidity('vlanId', true);
          return null;
        }
        else if (viewValue >= 1 && viewValue <= 4094) {
          ctrl.$setValidity('vlanId', true);
          return viewValue;
        }
        else {
          ctrl.$setValidity('vlanId', false);
          return undefined;
        }
      };
    };

    svc['yang:mac-address'] = function (ctrl) {
      return svc.macAddress(ctrl);
    };

    svc.macAddress = function (ctrl) {
      return function (viewValue) {
        if (viewValue === null || viewValue === "") {
          ctrl.$setValidity('macAddress', true);
          return null;
        }
        else if (viewValue.match(/^([0-9a-fA-F]{2}[:]){5}([0-9a-fA-F]{2})$/)) {
          ctrl.$setValidity('macAddress', true);
          return viewValue;
        }
        else {
          ctrl.$setValidity('macAddress', false);
          return undefined;
        }
      };
    };

    svc.numberRange = function (ctrl, params) {
      return function (viewValue) {
        if (viewValue === null || viewValue === "") {
          ctrl.$setValidity('numberRange', true);
          return null;
        }
        else if (svc.__isHexadecimal(viewValue)) {
          var decimal = parseInt(viewValue, 16);
          if (decimal >= params.from && decimal <= params.to) {
            ctrl.$setValidity('numberRange', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('numberRange', false);
            return undefined;
          }
        }
        else if (viewValue >= params.from && viewValue <= params.to) {
          ctrl.$setValidity('numberRange', true);
          return viewValue;
        }
        else {
          ctrl.$setValidity('numberRange', false);
          return undefined;
        }
      };
    };

    svc.uint8 = function (ctrl) {
      return function (viewValue) {
        if (viewValue === null || viewValue === "") {
          ctrl.$setValidity('uint8', true);
          return null;
        }
        else if (svc.__isHexadecimal(viewValue)) {
          var decimal = parseInt(viewValue, 16);
          if (decimal >= 0 && decimal <= 255) {
            ctrl.$setValidity('uint8', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('uint8', false);
            return undefined;
          }
        }
        else if (viewValue >= 0 && viewValue <= 255) {
          ctrl.$setValidity('uint8', true);
          return viewValue;
        }
        else {
          ctrl.$setValidity('uint8', false);
          return undefined;
        }
      };
    };

    svc.uint16 = function (ctrl) {
      return function (viewValue) {
        if (viewValue === null || viewValue === "") {
          ctrl.$setValidity('uint16', true);
          return null;
        }
        else if (svc.__isHexadecimal(viewValue)) {
          var decimal = parseInt(viewValue, 16);
          if (decimal >= 0 && decimal <= 65535) {
            ctrl.$setValidity('uint16', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('uint16', false);
            return undefined;
          }
        }
        else if (viewValue >= 0 && viewValue <= 65535) {
          ctrl.$setValidity('uint16', true);
          return viewValue;
        }
        else {
          ctrl.$setValidity('uint16', false);
          return undefined;
        }
      };
    };

    svc.uint32 = function (ctrl) {
      return function (viewValue) {
        if (viewValue === null || viewValue === "") {
          ctrl.$setValidity('uint32', true);
          return null;
        }
        else if (svc.__isHexadecimal(viewValue)) {
          var decimal = parseInt(viewValue, 16);
          if (decimal >= 0 && decimal <= 4294967295) {
            ctrl.$setValidity('uint32', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('uint32', false);
            return undefined;
          }
        }
        else if (viewValue >= 0 && viewValue <= 4294967295) {
          ctrl.$setValidity('uint32', true);
          return viewValue;
        }
        else {
          ctrl.$setValidity('uint32', false);
          return undefined;
        }
      };
    };

    svc.uint64 = function (ctrl) {
      return function (viewValue) {
        if (viewValue === null || viewValue === "") {
          ctrl.$setValidity('uint64', true);
          return null;
        }
        else if (svc.__isHexadecimal(viewValue)) {
          var decimal = parseInt(viewValue, 16);
          if (decimal >= 0 && decimal <= 18446744073709551615) {
            ctrl.$setValidity('uint64', true);
            return viewValue;
          }
          else {
            ctrl.$setValidity('uint64', false);
            return undefined;
          }
        }
        else if (viewValue >= 0 && viewValue <= 18446744073709551615) {
          ctrl.$setValidity('uint64', true);
          return viewValue;
        }
        else {
          ctrl.$setValidity('uint64', false);
          return undefined;
        }
      };
    };

    return svc;
  });

  sfc.register.factory('SfcTableParamsSvc', function () {

    var svc = {};

    svc.filterTableParams = {};

    svc.initializeSvcForTable = function (tableId) {
      if (angular.isUndefined(svc.filterTableParams[tableId])) {
        svc.filterTableParams[tableId] = {};
      }
    };

    svc.setFilterTableParams = function (tableId, newFilterTableParams) {
      if (!_.isEmpty(Object.keys(newFilterTableParams))) {
        svc.filterTableParams[tableId] = newFilterTableParams;
      }
    };

    svc.getFilterTableParams = function (tableId) {
      return svc.filterTableParams[tableId];
    };

    svc.checkAndSetFilterTableParams = function (tableId, tableParams) {
      tableParams.filter(svc.getFilterTableParams(tableId));

      return svc.filterTableParams[tableId] ? true : false;
    };

    return svc;
  });

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

  sfc.register.factory('SfcRestangularSvc', function (Restangular, $location, ENV) {

    var svc = {};

    // svc.newBaseUrl = $location.protocol() + "://" + $location.host() + ":" + $location.port() + "/restconf";
    svc.newBaseUrl = ENV.getBaseURL("MD_SAL") + "/restconf";

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
    function SfcRestBaseSvc(_modelUrl, _containerName, _listName, _availabilityCheckFunction) {
      this.modelUrl = _modelUrl;
      this.containerName = _containerName;
      this.listName = _listName;
      this.availabilityCheckFunction = this[_availabilityCheckFunction] || this['getArray'];
    }


    SfcRestBaseSvc.prototype.baseRest = function () {
      return SfcRestangularSvc.getCurrentInstance().one('config');
    };

    SfcRestBaseSvc.prototype.baseOperationalRest = function () {
      return SfcRestangularSvc.getCurrentInstance().one('operational');
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
      key = encodeURIComponent(key);
      return this.baseRest().customPUT(elem, this.modelUrl + ':' + this.containerName + '/' + this.listName + '/' + key);
    };

    SfcRestBaseSvc.prototype.putContainer = function (containerElem) {
      return this.baseRest().customPUT(containerElem, this.modelUrl + ":" + this.containerName);
    };

    SfcRestBaseSvc.prototype._delete = function (key) {
      key = encodeURIComponent(key);
      return this.baseRest().customDELETE(this.modelUrl + ':' + this.containerName + '/' + this.listName + '/' + key);
    };

    SfcRestBaseSvc.prototype._deleteAll = function () {
      return this.baseRest().customDELETE(this.modelUrl + ':' + this.containerName);
    };

    SfcRestBaseSvc.prototype.getOne = function (key) {
      key = encodeURIComponent(key);
      return this.baseRest().customGET(this.modelUrl + ":" + this.containerName + '/' + this.listName + '/' + key);
    };

    SfcRestBaseSvc.prototype.getOperationalOne = function (key) {
      key = encodeURIComponent(key);
      return this.baseOperationalRest().customGET(this.modelUrl + ":" + this.containerName + '/' + this.listName + '/' + key);
    };

    SfcRestBaseSvc.prototype.getItem = function (key, callback) {
      var instance = this; // save 'this' to closure
      key = encodeURIComponent(key);

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

    SfcRestBaseSvc.prototype.getOperationalItem = function (key, callback) {
      var instance = this; // save 'this' to closure
      key = encodeURIComponent(key);

      this.getOperationalOne(key).then(function (result) {
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

    SfcRestBaseSvc.prototype.getOperationalAll = function () {
      return this.baseOperationalRest().customGET(this.modelUrl + ":" + this.containerName);
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

        callback([], response); // return empty array
      });
    };

    SfcRestBaseSvc.prototype.getOperationalArray = function (callback) {
      var instance = this; // save 'this' to closure

      this.getOperationalAll().then(function (result) {
        var stripped = instance.stripNamespacePrefixes(result[instance.containerName][instance.listName]);
        callback(stripped); // return only nested array
      }, /* on error*/ function (response) {

        if (response.status = "404") {
          console.log("No data, returning empty array");
        } else {
          console.error("Error with status code ", response.status);
        }

        callback([], response); // return empty array
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

      item = this.addNamespacePrefixes(item);

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

    SfcRestBaseSvc.prototype.deleteAll = function (callback) {


      this._deleteAll().then(function () {
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

    // to be overriden
    SfcRestBaseSvc.prototype.addNamespacePrefixes = function (item) {
      // noop
      return item;
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

      var locatorMatcher = new RegExp("^service-locator:");
      var sfTypeMatcher = new RegExp("^service-function-type:");

      _.each(sfsArray, function (sf) {

        if (!_.isEmpty(sf.type)) {
          sf.type = sf.type.replace(sfTypeMatcher, "");
        }

        _.each(sf['sf-data-plane-locator'], function (locator) {
          if (!_.isEmpty(locator.transport)) {
            locator.transport = locator.transport.replace(locatorMatcher, "");
          }
        });
      });

      return sfsArray;
    };

    // @override
    ServiceFunctionSvc.prototype.addNamespacePrefixes = function (sf) {
      var sfTypeMatcher = new RegExp("^service-function-type:");
      var sfTypePrefix = "service-function-type:";
      var locatorMatcher = new RegExp("^service-locator:");
      var locatorPrefix = "service-locator:";

      if (angular.isDefined(sf['type']) && sf['type'].search(sfTypeMatcher) < 0) {
        // add prefix
        sf.type = sfTypePrefix + sf.type;
      }

      _.each(sf['sf-data-plane-locator'], function (locator) {
        if (angular.isDefined(locator['transport']) && locator['transport'].search(locatorMatcher) < 0) {
          locator['transport'] = locatorPrefix + locator['transport'];
        }
      });

      return sf;
    };

    return new ServiceFunctionSvc();
  });


// ******* ServiceFunctionTypeSvc *********
  sfc.register.factory('ServiceFunctionTypeSvc', function (SfcRestBaseSvc) {
    var modelUrl = 'service-function-type';
    var containerName = 'service-function-types';
    var listName = 'service-function-type';

    // constructor
    function ServiceFunctionTypeSvc() {
    }

    ServiceFunctionTypeSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    // @override
    ServiceFunctionTypeSvc.prototype.stripNamespacePrefixes = function (sfsArray) {

      var sfTypeMatcher = new RegExp("^service-function-type:");

      _.each(sfsArray, function (sf) {
        if (!_.isEmpty(sf.type)) {
          sf.type = sf.type.replace(sfTypeMatcher, "");
        }
      });

      return sfsArray;
    };

    return new ServiceFunctionTypeSvc();
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

    // @override
    ServiceChainSvc.prototype.stripNamespacePrefixes = function (sfcsArray) {
      var sfTypeMatcher = new RegExp("^service-function-type:");

      _.each(sfcsArray, function (sfc) {
        _.each(sfc['sfc-service-function'], function (sf) {
          if (!_.isEmpty(sf.type)) {
            sf.type = sf.type.replace(sfTypeMatcher, "");
          }
        });
      });

      return sfcsArray;
    };

    // @override
    ServiceChainSvc.prototype.addNamespacePrefixes = function (sfc) {
      var sfTypeMatcher = new RegExp("^service-function-type:");
      var sfTypePrefix = "service-function-type:";

      _.each(sfc['sfc-service-function'], function (sf) {
        //if there is no prefix add it
        if (angular.isDefined(sf['type']) && sf['type'].search(sfTypeMatcher) < 0) {
          // add prefix
          sf['type'] = sfTypePrefix + sf['type'];
        }
      });

      return sfc;
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
      var sfTypeMatcher = new RegExp("^service-function-type:");


      _.each(sffArray, function (sff) {
        if (!_.isEmpty(sff['sff-data-plane-locator'])) {

          _.each(sff['sff-data-plane-locator'], function (locator) {

            if (angular.isDefined(locator['data-plane-locator'] && locator['data-plane-locator']['transport'])) {
              locator['data-plane-locator']['transport'] = locator['data-plane-locator']['transport'].replace(serviceLocatorMatcher, "");
            }

            // strip namespace in 'ovs-bridge' property name
            if (locator['service-function-forwarder-ovs:ovs-bridge']) {
              locator['ovs-bridge'] = locator['service-function-forwarder-ovs:ovs-bridge'];
              delete(locator['service-function-forwarder-ovs:ovs-bridge']);
            }

            // strip namespace in 'ovs-options' property name
            if (locator['service-function-forwarder-ovs:ovs-options']) {
              locator['ovs-options'] = locator['service-function-forwarder-ovs:ovs-options'];
              delete(locator['service-function-forwarder-ovs:ovs-options']);
            }
          });
        }

        if (!_.isEmpty(sff['service-function-dictionary'])) {

          _.each(sff['service-function-dictionary'], function (dictionary) {

            if (angular.isDefined(dictionary['type'])) {
              dictionary['type'] = dictionary['type'].replace(sfTypeMatcher, "");
            }

            if (angular.isDefined(dictionary['failmode'])) {
              dictionary['failmode'] = dictionary['failmode'].replace(matcher, "");
            }

            // strip namespace in 'transport' property value
            if (angular.isDefined(dictionary['sff-sf-data-plane-locator'] && dictionary['sff-sf-data-plane-locator']['transport'])) {
              dictionary['sff-sf-data-plane-locator']['transport'] = dictionary['sff-sf-data-plane-locator']['transport'].replace(serviceLocatorMatcher, "");
            }

            // strip namespace in 'ovs-bridge' property name
            if (angular.isDefined(dictionary['sff-sf-data-plane-locator']['service-function-forwarder-ovs:ovs-bridge'])) {
              dictionary['sff-sf-data-plane-locator']['ovs-bridge'] = dictionary['sff-sf-data-plane-locator']['service-function-forwarder-ovs:ovs-bridge'];
              delete(dictionary['sff-sf-data-plane-locator']['service-function-forwarder-ovs:ovs-bridge']);
            }

            // strip namespace in 'ovs-options' property name
            if (angular.isDefined(dictionary['sff-sf-data-plane-locator']['service-function-forwarder-ovs:ovs-options'])) {
              dictionary['sff-sf-data-plane-locator']['ovs-options'] = dictionary['sff-sf-data-plane-locator']['service-function-forwarder-ovs:ovs-options'];
              delete(dictionary['sff-sf-data-plane-locator']['service-function-forwarder-ovs:ovs-options']);
            }
          });
        }

      });
      return sffArray;
    };

    // @override
    ServiceForwarderSvc.prototype.addNamespacePrefixes = function (sff) {
      var sfTypeMatcher = new RegExp("^service-function-type:");
      var sfTypePrefix = "service-function-type:";
      var locatorMatcher = new RegExp("^service-locator:");
      var locatorPrefix = "service-locator:";
      var sffMatcher = new RegExp("^sevice-function-forwarder:");
      var sffPrefix = "service-function-forwarder:";

      if (!_.isEmpty(sff['sff-data-plane-locator'])) {
        _.each(sff['sff-data-plane-locator'], function (locator) {

          if (angular.isDefined(locator['data-plane-locator'] &&
            angular.isDefined(locator['data-plane-locator']['transport']) &&
            locator['data-plane-locator']['transport'].search(locatorMatcher) < 0)) {
            locator['data-plane-locator']['transport'] = locatorPrefix + locator['data-plane-locator']['transport'];
          }
        });
      }

      if (!_.isEmpty(sff['service-function-dictionary'])) {
        _.each(sff['service-function-dictionary'], function (sf) {
          if (angular.isDefined(sf['type']) && sf['type'].search(sfTypeMatcher) < 0) {
            // add prefix
            sf['type'] = sfTypePrefix + sf['type'];
          }

          if (angular.isDefined(sf['failmode']) && sf['failmode'].search(sffMatcher) < 0) {
            sf['failmode'] = sffPrefix + sf['failmode'];
          }

          if (angular.isDefined(sf['sff-sf-data-plane-locator']) &&
            angular.isDefined(sf['sff-sf-data-plane-locator']['transport'] &&
            sf['sff-sf-data-plane-locator']['transport'].search(locatorMatcher) < 0)) {
            sf['sff-sf-data-plane-locator']['transport'] = locatorPrefix + sf['sff-sf-data-plane-locator']['transport'];
          }
        });

      }

      return sff;
    };

    return new ServiceForwarderSvc();
  });


// *** SfcAclSvc **********************
  sfc.register.factory('SfcAclSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'ietf-access-control-list';
    var containerName = 'access-lists';
    var listName = 'acl';

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

          _.each(acl['access-list-entries']['ace'], function (ace) {
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
      var item = {};
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

    var modelUrl = 'service-function-path-metadata';
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

    var modelUrl = 'service-function-path-metadata';
    var containerName = 'service-function-metadata';
    var listName = 'variable-metadata';

    // constructor
    function SfcVariableMetadataSvc() {
    }

    SfcVariableMetadataSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    return new SfcVariableMetadataSvc();
  });

// ******* SfcClassifierSvc *********
  sfc.register.factory('SfcClassifierSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'service-function-classifier';
    var containerName = 'service-function-classifiers';
    var listName = 'service-function-classifier';

    // constructor
    function SfcClassifierSvc() {
    }

    SfcClassifierSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    return new SfcClassifierSvc();
  });

  // ******* SfcIpfixClassIdSvc *********
  sfc.register.factory('SfcIpfixClassIdSvc', function (SfcRestBaseSvc) {
    var modelUrl = 'ipfix-application-information';
    var containerName = 'class-id-dictionary';
    var listName = 'class-id';

    // constructor
    function SfcIpfixClassIdSvc() {
    }

    SfcIpfixClassIdSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    return new SfcIpfixClassIdSvc();
  });

// ******* SfcIpfixAppIdSvc *********
  sfc.register.factory('SfcIpfixAppIdSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'ipfix-application-information';
    var containerName = 'application-id-dictionary';
    var listName = 'application-id';

    // constructor
    function SfcIpfixAppIdSvc() {
    }

    SfcIpfixAppIdSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    // @override
    SfcIpfixAppIdSvc.prototype.getListKeyFromItem = function (itemData) {
        return itemData['applicationName'];
    };

    return new SfcIpfixAppIdSvc();
  });

// ******* RenderedServicePathSvc *********
  sfc.register.factory('RenderedServicePathSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'rendered-service-path';
    var containerName = 'rendered-service-paths';
    var listName = 'rendered-service-path';
    var availabilityCheckFunction = 'getOperationalArray';

    // constructor
    function RenderedServicePathSvc() {
    }

    RenderedServicePathSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName, availabilityCheckFunction);

    return new RenderedServicePathSvc();
  });

// ******* SfcServiceFunctionScheduleTypeSvc *********
  sfc.register.factory('SfcServiceFunctionSchedulerTypeSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'service-function-scheduler-type';
    var containerName = 'service-function-scheduler-types';
    var listName = 'service-function-scheduler-type';

    // constructor
    function SfcServiceFunctionSchedulerTypeSvc() {
    }

    SfcServiceFunctionSchedulerTypeSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    // @override
    SfcServiceFunctionSchedulerTypeSvc.prototype.getListKeyFromItem = function (itemData) {
      return itemData['type'];
    };

    // @override
    SfcServiceFunctionSchedulerTypeSvc.prototype.addNamespacePrefixes = function (schedulerType) {
      var schedulerTypeMatcher = new RegExp("^service-function-scheduler-type:");
      var schedulerTypePrefix = "service-function-scheduler-type:";

      //add scheduler name (name should come from view, filling it in services does not give much sense)
      if (angular.isUndefined(schedulerType['name'])) {
          schedulerType['name'] = schedulerType['type'];
      }

      //set enabled to true
      if (angular.isUndefined(schedulerType['enabled'])) {
          schedulerType['enabled'] = true;
      }

      // add namespace prefix
      if (angular.isDefined(schedulerType['type']) && schedulerType['type'].search(schedulerTypeMatcher) < 0) {
        schedulerType['type'] = schedulerTypePrefix + schedulerType['type'];
      }

      return schedulerType;
    };

    // @override
    SfcServiceFunctionSchedulerTypeSvc.prototype.stripNamespacePrefixes = function (schedulerTypeArray) {

      var schedulerTypeMatcher = new RegExp("^service-function-scheduler-type:");

      _.each(schedulerTypeArray, function (schedulerType) {

        // remove namespace prefix
        if (angular.isDefined(schedulerType['type'])) {
          schedulerType['type'] = schedulerType['type'].replace(schedulerTypeMatcher, "");
        }

      });

      return schedulerTypeArray;
    };


    return new SfcServiceFunctionSchedulerTypeSvc();
  });

// ******* SfcServiceForwarderOvsSvc *********
  sfc.register.factory('SfcServiceForwarderOvsSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'service-function-forwarder-ovs';
    var containerName = 'none';
    var listName = 'none';

    // constructor
    function SfcServiceForwarderOvsSvc() {
    }

    SfcServiceForwarderOvsSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName);

    return new SfcServiceForwarderOvsSvc();
  });

// ******* SfcServiceFunctionStateSvc *********
  sfc.register.factory('SfcServiceFunctionStateSvc', function (SfcRestBaseSvc) {

    var modelUrl = 'service-function';
    var containerName = 'service-functions-state';
    var listName = 'service-function-state';
    var availabilityCheckFunction = 'getOperationalArray';

    // constructor
    function SfcServiceFunctionStateSvc() {
    }

    SfcServiceFunctionStateSvc.prototype = new SfcRestBaseSvc(modelUrl, containerName, listName, availabilityCheckFunction);

    return new SfcServiceFunctionStateSvc();
  });

})
; // end define

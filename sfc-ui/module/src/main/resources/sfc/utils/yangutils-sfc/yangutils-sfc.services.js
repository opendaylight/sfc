define(['app/sfc/utils/yangutils-sfc/yangutils-sfc.module'], function (yangUtilsSfc) {

    yangUtilsSfc.factory('YangUtilsRestangularSfc', ['Restangular', 'ENV', function (Restangular, ENV) {
        return Restangular.withConfig(function (RestangularConfig) {
            // RestangularConfig.setBaseUrl('dummyUrl');
            RestangularConfig.setBaseUrl(ENV.getBaseURL("MD_SAL"));
        });
    }]);

    yangUtilsSfc.factory('arrayUtilsSfc', function () {

        var arrayUtils = {};

        arrayUtils.getFirstElementByCondition = function(list, condition) {
            var selItems = list && condition ? list.filter(function(item) {
                return condition(item);
            }) : [];
            return (selItems.length ? selItems[0] : null);
        };

        return arrayUtils;
    });

    yangUtilsSfc.factory('pathUtilsSfc', function (arrayUtilsSfc) {

        var pathUtils = {},
            parentPath = '..';

        var PathElem = function (name, module, identifierName) {
            this.name = name;
            this.module = module;
            this.identifierName = identifierName;
            this.identifierValue = '';

            this.hasIdentifier = function () {
                return (identifierName ? true : false);
            };

            this.toString = function () {
                return (this.module ? this.module + ':' : '') + this.name + '/' + (this.hasIdentifier() ? this.identifierValue + '/' : '');
            };

            this.checkNode = function (node) {
                return (this.module ? this.module === node.module : true) && (this.name ? this.name === node.label : true);
            };
        };

        var getModuleNodePair = function (pathString, defaultModule) {
            return pathString.indexOf(':') > -1 ? pathString.split(':') : [defaultModule, pathString];
        };

        var isIdentifier = function (item) {
            return (item.indexOf('{') === item.indexOf('}')) === false;
        };

        var createPathElement = function (pathString, identifierString, prefixConverter, defaultModule) {
            var pair = getModuleNodePair(pathString, defaultModule),
                //module can be either prefix or module name, if it's module name we don't need to convert it
                module = (prefixConverter && pair[0] !== defaultModule ? prefixConverter(pair[0]) : pair[0]),
                name = pair[1];

            return new PathElem(name, module, identifierString);
        };

        pathUtils.search = function (node, path) {
            var pathElem = path.shift(),
                selNode = pathElem.name === parentPath ?
                node.parent :
                arrayUtilsSfc.getFirstElementByCondition(node.children, function (child) {
                    return pathElem.checkNode(child);
                });

            if (selNode !== null) {
                if (path.length) {
                    return pathUtils.search(selNode, path);
                } else {
                    return selNode;
                }
            } else {
                // console.warn('cannot find element ',pathElem,'in node',node);
                return null;
            }
        };

        pathUtils.translate = function (path, prefixConverter, defaultModule) {
            var lastUsedModule = defaultModule,
                pathStrArray = path.split('/').filter(function (item) {
                    return item !== '';
                }).slice(),
                result = pathStrArray.map(function (item, index) {
                    if (isIdentifier(item)) {
                        return null;
                    } else {
                        var identifier,
                                pathElem;

                        if (pathStrArray.length > index + 1 && isIdentifier(pathStrArray[index + 1])) {
                            identifier = pathStrArray[index + 1].slice(1, -1);
                        }

                        pathElem = createPathElement(item, identifier, prefixConverter, lastUsedModule);
                        // do we want to update? in api path should not, in other it shouldnt matter
                        // lastUsedModule = (pathElem.module ? pathElem.module : lastUsedModule); 

                        return pathElem;
                    }
                }).filter(function (item) {
                    return item !== null;
                });

                return result;
            };

        var trimPath = function(pathString) {
            var searchStr = 'restconf',
                output = pathString;

            if(pathString.indexOf(searchStr) > -1) {
                output = pathString.slice(pathString.indexOf(searchStr)+searchStr.length+1);
            }

            return output;
        };

        var deleteRevision = function(s) {
            return s.slice(0, s.indexOf(' rev.')).trim();
        };


        var expandTreeDataNode = function(treeApiNode, treeData) {
            var sel = treeData.filter(function(d) {
                return d.branch.uid === treeApiNode.uid;
            });

            if(sel.length === 1) {
                sel[0].branch.expanded = true;
            }
        };


        var getPathStrToArray = function(pathString) {
            return pathArray = trimPath(pathString).split('/').filter(function(elem) {
                    return elem !== '';
                }).map(function(elem) {
                    var pair = getModuleNodePair(elem, null);
                    return new PathElem(pair[1], pair[0], null);
                });
        };

        pathUtils.fillPath = function(pathArrayIn, pathString) {
            var pathArray = getPathStrToArray(pathString);

            for(var i = 0, j = 0; i < pathArrayIn.length; i++) {
                var pathElem = pathArrayIn[i],
                    inc = 1;
                    
                if(pathElem.hasIdentifier()) {
                    pathElem.identifierValue = pathArray[j+1].name;
                    inc = 2;
                }

                j = j + inc;
            }
        };

        var getActElementChild = function(actElem, childLabel) {
            var sel = actElem.children.filter(function(child) {
                    return child.label === childLabel;
                }),
                ret = sel.length === 1 ? sel[0] : null;

            return ret;
        };

        pathUtils.searchNodeByPath = function(pathString, treeApis, treeData) {
            var pathArray = getPathStrToArray(pathString),
                module = pathArray.length > 1 ? pathArray[1].module : null,
                selectedTreeApi = module ? treeApis.filter(function(treeApi) {
                    return deleteRevision(treeApi.label) === module;
                })[0] : null,
                retObj = null;

            if(selectedTreeApi && pathArray.length) {
                var actElem = selectedTreeApi;
                
                for(i = 0; i < pathArray.length && actElem; ) {
                    expandTreeDataNode(actElem, treeData);
                    actElem = getActElementChild(actElem, pathArray[i].name);

                    i = i + (actElem.identifier ? 2 : 1);
                }

                if(actElem) {
                    retObj = { indexApi: actElem.indexApi, indexSubApi: actElem.indexSubApi };
                }
            }

            return retObj;
        };

        pathUtils.__test = {
            PathElem: PathElem,
            getModuleNodePair: getModuleNodePair,
            isIdentifier: isIdentifier,
            createPathElement: createPathElement
        };

        return pathUtils;
    });

    yangUtilsSfc.factory('syncFactSfc', function ($timeout) {
        var timeout = 10000;

        var SyncObject = function () {
            this.runningRequests = [];
            this.reqId = 0;
            this.timeElapsed = 0;

            this.spawnRequest = function (digest) {
                var id = digest + (this.reqId++).toString();
                this.runningRequests.push(id);
                //console.debug('adding request ',id,' total running requests  = ',this.runningRequests);
                return id;
            };

            this.removeRequest = function (id) {
                var index = this.runningRequests.indexOf(id);

                if (index > -1) {
                    this.runningRequests.splice(index, 1);
                    //console.debug('removing request ',id,' remaining requests = ',this.runningRequests);
                } else {
                    console.warn('cannot remove request', id, 'from', this.runningRequests, 'index is', index);
                }
            };

            this.waitFor = function (callback) {
                var t = 1000,
                        processes = this.runningRequests.length,
                        self = this;

                if (processes > 0 && self.timeElapsed < timeout) {
                    // console.debug('waitin on',processes,'processes',this.runningRequests);
                    $timeout(function () {
                        self.timeElapsed = self.timeElapsed + t;
                        self.waitFor(callback);
                    }, t);
                } else {
                    callback();
                }
            };
        };

        return {
            generateObj: function () {
                return new SyncObject();
            }
        };
    });


    yangUtilsSfc.factory('custFunctSfc', function (reqBuilderSfc) {
        var CustFunctionality = function (label, node, callback, viewStr) {
            this.label = label;
            this.callback = callback;
            this.viewStr = viewStr;

            this.setCallback = function (callback) {
                this.callback = callback;
            };

            this.runCallback = function (args) {
                if (this.callback) {
                    this.callback(args);
                } else {
                    console.warn('no callback set for custom functionality', this.label);
                }
            };
        };

        custFunct = {};

        custFunct.createNewFunctionality = function (label, node, callback, viewStr) {
            if (node && callback) {
                return new CustFunctionality(label, node, callback, viewStr);
            } else {
                console.error('no node or callback is set for custom functionality');
            }
        };

        return custFunct;
    });


    yangUtilsSfc.factory('reqBuilderSfc', function () {

        var builder = {
            createObj: function () {
                return {};
            },
            createList: function () {
                return [];
            },
            insertObjToList: function (list, obj) {
                list.push(obj);
            },
            insertPropertyToObj: function (obj, propName, propData) {
                var data = propData ? propData : {},
                        name = propName;

                obj[name] = data;
            },
            resultToString: function (obj) {
                return JSON.stringify(obj, null, 4);
            }
        };

        return builder;

    });


    yangUtilsSfc.factory('typeWrapperSfc', function (restrictionsFactSfc) {
        var findLeafParent = function (node) {
            if (node.type === 'leaf') {
                return node;
            } else {
                if (node.parent) {
                    return findLeafParent(node.parent);
                } else {
                    return null;
                }
            }
        };

        var wrapper = {
            wrapAll: function (node) {
                if (node.type === 'type') {
                    this._setDefaultProperties(node);
                }

                if(this.hasOwnProperty(node.label)) {
                    this[node.label](node);
                }
            },
            _setDefaultProperties: function (node) {
                node.builtInChecks = [];
                node.errors = [];
                node.clear = function () {
                };
                node.fill = function () {
                };
                node.performRestrictionsCheck = function (value) {
                    var patternRestrictions = node.getChildren('pattern'),
                        patternCheck = function(value) {
                            return patternRestrictions.map(function(patternNode) {
                                return patternNode.restrictions[0];
                            }).some(function(patternRestriction) {
                                var condition = patternRestriction.check(value);
                                if(condition === false) {
                                    node.errors.push(patternRestriction.info);
                                }
                                return condition;
                            });
                        },
                        lengthRestrictions = node.getChildren('length'),
                        rangeRestrictions = node.getChildren('range'),
                        lengthRangeCheck = function(restrictionsContainers, value) {
                            return restrictionsContainers[0].restrictions.some(function(restriction) {
                                var condition = restriction.check(value);
                                if(condition === false) {
                                    node.errors.push(restriction.info);
                                }
                                return condition;
                            });
                        };
                    
                    var patternCondition = patternRestrictions.length ? patternCheck(value) : true,
                        lengthCondition = lengthRestrictions.length && value.length? lengthRangeCheck(lengthRestrictions, value.length) : true,
                        rangeCondition = rangeRestrictions.length ? lengthRangeCheck(rangeRestrictions, value) : true;

                    return patternCondition && lengthCondition && rangeCondition;
                };
                node.performBuildInChecks = function (value) {
                    return node.builtInChecks.length ? node.builtInChecks.every(function (restriction) {
                        var condition = restriction.check(value);
                        if(condition === false) {
                            node.errors.push(restriction.info);
                        }
                        return condition;
                    }) : true;
                };
                node.check = function (value) {
                    node.errors = [];
                    var condition = value !== '' ? node.performBuildInChecks(value) && node.performRestrictionsCheck(value) : true;
                    if(condition) {
                        node.errors = [];
                    }
                    return condition;
                };
            },
            // string: function (node) {
            // },
            // boolean: function (node) {
            // },
            enumeration: function (node) {
                node.selEnum = null;
                node.leafParent = findLeafParent(node);
                
                var childNames = [];
                node.getChildren('enum').forEach(function(child) {
                    childNames.push(child.label);
                });
                node.builtInChecks.push(restrictionsFactSfc.isInArray(childNames));

                node.setLeafValue = function (value) {
                    if(value) {
                        node.leafParent.value = value;
                    }
                };
                
                node.clear = function () {
                    node.selEnum = null;
                };

                node.fill = function (value) {
                    var selChild = node.getChildren('enum', value)[0];
                    node.selEnum = selChild ? selChild : null;
                };
            },
            bits: function (node) {
                var actBitsLen = 0,
                    i;

                node.leafParent = findLeafParent(node);
                node.maxBitsLen = node.getChildren('bit').length;
                node.bitsValues = [];

                node.builtInChecks.push(restrictionsFactSfc.getIsUNumberFnc());
                node.builtInChecks.push(restrictionsFactSfc.getMinMaxFnc(0, 18446744073709551615));

                for (i = 0; i < node.maxBitsLen; i++) {
                    node.bitsValues[i] = '';
                }

                node.clear = function () {
                    for (i = 0; i < node.bitsValues.length; i++) {
                        node.bitsValues[i] = 0;
                    }
                };

                node.fill = function () {
                    
                    var parseIntVal = parseInt(node.leafParent.value, 10),
                        intVal = isNaN(parseIntVal) === false ? parseIntVal : 0;
                    
                    node.bitsValues = intVal.toString(2).split('').slice().reverse();
                    actBitsLen = node.bitsValues.length;
                    
                    for (i = actBitsLen; i < node.maxBitsLen; i++) {
                        node.bitsValues.push('');
                    }
                };

                node.setLeafValue = function (values) {
                    var bitString = values.map(function (value) {
                        return value.length > 0 ? value : '0';
                    });

                    node.leafParent.value = parseInt(bitString.slice().reverse().join(''), 2).toString();
                };
            },
            // binary: function (node) {
            // },
            // leafref: function (node) {
            // },
            // identityref: function (node) {
            // },
            // empty: function (node) {
            // },
            union: function (node) {
                node.clear = function () {
                    node.getChildren('type').forEach(function(child) {
                        child.clear();
                    });
                };
                node.fill = function (value) {
                    node.getChildren('type').forEach(function(child) {
                        child.fill(value);
                    });
                };

                node.check = function (value) {
                    var condition = false;
                    node.getChildren('type').forEach(function (childType) {
                        var childCondition = childType.check(value);
                        condition = condition || childCondition;
                    });
                    return condition;
                };

                node.getChildren('type').forEach(function (childType) {
                    wrapper.wrapAll(childType);
                });
            },
            // 'instance-identifier': function (node) {
            // },
            decimal64: function (node) {
                node.builtInChecks.push(restrictionsFactSfc.getIsDecimalFnc());
            },
            int8: function (node) {
                node.builtInChecks.push(restrictionsFactSfc.getIsNumberFnc());
                node.builtInChecks.push(restrictionsFactSfc.getMinMaxFnc(-128, 127));
            },
            int16: function (node) {
                node.builtInChecks.push(restrictionsFactSfc.getIsNumberFnc());
                node.builtInChecks.push(restrictionsFactSfc.getMinMaxFnc(-32768, 32767));
            },
            int32: function (node) {
                node.builtInChecks.push(restrictionsFactSfc.getIsNumberFnc());
                node.builtInChecks.push(restrictionsFactSfc.getMinMaxFnc(-2147483648, 2147483647));
            },
            int64: function (node) {
                node.builtInChecks.push(restrictionsFactSfc.getIsNumberFnc());
                node.builtInChecks.push(restrictionsFactSfc.getMinMaxFnc(-9223372036854775808, 9223372036854775807));
            },
            uint8: function (node) {
                node.builtInChecks.push(restrictionsFactSfc.getIsUNumberFnc());
                node.builtInChecks.push(restrictionsFactSfc.getMinMaxFnc(0, 255));
            },
            uint16: function (node) {
                node.builtInChecks.push(restrictionsFactSfc.getIsUNumberFnc());
                node.builtInChecks.push(restrictionsFactSfc.getMinMaxFnc(0, 65535));
            },
            uint32: function (node) {
                node.builtInChecks.push(restrictionsFactSfc.getIsUNumberFnc());
                node.builtInChecks.push(restrictionsFactSfc.getMinMaxFnc(0, 4294967295));
            },
            uint64: function (node) {
                node.builtInChecks.push(restrictionsFactSfc.getIsUNumberFnc());
                node.builtInChecks.push(restrictionsFactSfc.getMinMaxFnc(0, 18446744073709551615));
            }
        };

        wrapper.__test = {
            findLeafParent: findLeafParent
        };

        return wrapper;

    });

    yangUtilsSfc.factory('nodeWrapperSfc', function (constantsSfc, $timeout, reqBuilderSfc, restrictionsFactSfc, typeWrapperSfc) {

        var comparePropToElemByName = function comparePropToElemByName(propName, elemName) {
            return (propName.indexOf(':') > -1 ? propName.split(':')[1] : propName) === elemName; //TODO also check by namespace - redundancy?
        };

        var equalArrays = function (arrA, arrB) {
            var match = (arrA.length === arrB.length) && arrA.length > 0;

            if (match) {
                var i = 0;
                while (i < arrA.length && match) {
                    var valMatch = arrA[i] === arrB[i];
                    match = match && valMatch;
                    i++;
                }
            }
            return match;
        };

        var equalListElems = function (listElemA, listElemB, refKey) {
            var getKeyValue = function (data, label, module) {
                    if (data && data.hasOwnProperty(label)) {
                        return data[label];
                    } else if (data && data.hasOwnProperty(module + ':' + label)) {
                        return data[module + ':' + label];
                    } else {
                        return null;
                    }
                },
                getKeyArrayValues = function (data, refKey) {
                    return refKey.map(function (key) {
                        return getKeyValue(data, key.label, key.module);
                    }).filter(function (item) {
                        return item !== null;
                    });
                },
                keyValuesA = getKeyArrayValues(listElemA, refKey);
                keyValuesB = getKeyArrayValues(listElemB, refKey);

            return equalArrays(keyValuesA, keyValuesB);
        };

        var checkListElemKeys = function (listData, refKey) {
            var doubleKeyIndexes = [],
                checkedElems = [];

            listData.forEach(function (item, index) {
                var duplitactes = checkedElems.filter(function (checked) {
                    var isDuplicate = equalListElems(item, checked.item, refKey);
                    if (isDuplicate && doubleKeyIndexes.indexOf(checked.index) === -1) {
                        doubleKeyIndexes.push(checked.index);
                    }
                    return isDuplicate;
                });

                if (duplitactes.length) {
                    //item is already in checkedElems so we don't need to push it again
                    doubleKeyIndexes.push(index);
                } else {
                    checkedElems.push({index: index, item: item});
                }
            });

            return doubleKeyIndexes;
        };

        var parseRestrictText = function (text) {
            return text.split('|').map(function (elem) {
                var subElems = elem.split('..');
                return subElems.length === 1 ? restrictionsFactSfc.getEqualsFnc(subElems[0]) :
                                               restrictionsFactSfc.getMinMaxFnc(subElems[0], subElems[1]);
            });
        };


        var getTypes = function (node) {
            var types = [];

            var getTypesRecursive = function (node, types) {
                types.push(node);

                node.getChildren('type').forEach(function (child) {
                    getTypesRecursive(child, types);
                });
            };

            node.getChildren('type').forEach(function (child) {
                //console.info('child', child);
                getTypesRecursive(child, types);
            });

            return types;
        };

        var wrapper = {
            wrap: function (node) {
                if (this.hasOwnProperty(node.type)) {
                    this[node.type](node);
                }
            },
            wrapAll: function (node) {
                var self = this;
                self.wrap(node);
                node.children.forEach(function (child) {
                    self.wrapAll(child);
                });
            },
            leaf: function (node) {
                node.value = '';
                node.valueIsValid = true;
                
                var typeChild = node.getChildren('type')[0];

                var fnToString = function (string) {
                    var valueStr = '';
                    try {
                        valueStr = string.toString();
                    } catch (e) {
                        console.warn('cannot convert value', node.value);
                    }
                    return valueStr;
                };

                node.buildRequest = function (builder, req) {
                    var valueStr = '';
                    valueStr = fnToString(node.value);

                    if (valueStr) {
                        builder.insertPropertyToObj(req, node.label, valueStr);
                        return true;
                    }
                    return false;
                };

                node.fill = function (name, data) {
                    var match = comparePropToElemByName(name, node.label);

                    if (match) {
                        node.value = data;
                        if (typeChild) {
                            typeChild.fill(node.value);
                        }
                    }
                    return match;
                };

                node.clear = function () {
                    node.value = '';
                    if (typeChild) {
                        typeChild.clear();
                    }
                };

                node.isFilled = function () {
                    var filled = fnToString(node.value) ? true : false;
                    return filled;
                };

                node.checkValueType = function () {
                    node.valueIsValid = typeChild ? typeChild.check(node.value) : true;
                };
            },
            type: function (node) {
                typeWrapperSfc.wrapAll(node);
            },
            length: function (node) {
                node.restrictions = parseRestrictText(node.label);
            },
            range: function (node) {
                node.restrictions = parseRestrictText(node.label);
            },
            pattern: function (node) {
                node.restrictions = [restrictionsFactSfc.getReqexpValidationFnc(node.label)];
            },
            // enum: function (node) {
            // },
            // bit: function (node) {
            // },
            // position: function (node) {
            // },
            container: function (node) {
                node.expanded = false;

                node.toggleExpand = function () {
                    node.expanded = !node.expanded;
                };

                node.buildRequest = function (builder, req) {
                    var added = false,
                            name = node.label,
                            objToAdd = builder.createObj(),
                            builderNodes = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    if (builderNodes.length) {
                        builderNodes.forEach(function (child) {
                            var childAdded = child.buildRequest(builder, objToAdd);
                            added = added || childAdded;
                        });
                    } else {
                        added = true;
                    }

                    if (added) {
                        builder.insertPropertyToObj(req, name, objToAdd);
                    }

                    return added;
                };

                node.fill = function (name, data) {
                    var match = comparePropToElemByName(name, node.label),
                            nodesToFill = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    if (match && nodesToFill.length) {
                        nodesToFill.forEach(function (child) {
                            for (var prop in data) {
                                child.fill(prop, data[prop]);
                            }
                        });

                        node.expanded = match;
                    }

                    return match;
                };

                node.clear = function () {
                    var nodesToClear = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);
                    node.nodeType = constantsSfc.NODE_UI_DISPLAY;

                    if (nodesToClear.length) {
                        nodesToClear.forEach(function (child) {
                            child.clear();
                        });
                    }
                };

                node.isFilled = function () {
                    return node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY).some(function (child) {
                        return child.isFilled();
                    });
                };
            },
            rpc: function (node) {
                node.expanded = true;
                node.buildRequest = function (builder, req) {
                    var added = false,
                        name = node.label,
                        builderNodes = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    if (builderNodes.length) {
                        builderNodes.forEach(function (child) {
                            var childAdded = child.buildRequest(builder, req);
                            added = added || childAdded;
                        });
                    } else {
                        added = true;
                    }

                    return added;
                };

                node.fill = function (name, data) {
                    var filled = false,
                        nodesToFill = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    nodesToFill.forEach(function (child) {
                        var childFilled = child.fill(name, data);
                        filled = filled || childFilled;
                    });

                    node.expanded = filled;

                    return filled;
                };

                node.clear = function () {
                    var nodesToClear = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);
                    
                    if (nodesToClear.length) {
                        nodesToClear.forEach(function (child) {
                            child.clear();
                        });
                    }
                };

                node.isFilled = function () {
                    return node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY).some(function (child) {
                        return child.isFilled();
                    });
                };

            },
            input: function (node) {
                node.expanded = true;
                node.buildRequest = function (builder, req) {
                    var added = false,
                        name = node.label,
                        objToAdd = builder.createObj(),
                        builderNodes = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    if (builderNodes.length) {

                        builderNodes.forEach(function (child) {
                            var childAdded = child.buildRequest(builder, objToAdd);
                            added = added || childAdded;
                        });
                    } else {
                        added = true;
                    }

                    if (added) {
                        builder.insertPropertyToObj(req, name, objToAdd);
                    }

                    return added;
                };

                node.fill = function (name, data) {
                    var match = comparePropToElemByName(name, node.label),
                        nodesToFill = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    if (match && nodesToFill.length) {
                        nodesToFill.forEach(function (child) {
                            for (var prop in data) {
                                child.fill(prop, data[prop]);
                            }
                        });
                        node.expanded = match;
                    }

                    return match;
                };

                node.clear = function () {
                    var nodesToClear = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);
                    
                    if (nodesToClear.length) {
                        nodesToClear.forEach(function (child) {
                            child.clear();
                        });
                    }
                };

                node.isFilled = function () {
                    return node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY).some(function (child) {
                        return child.isFilled();
                    });
                };

            },
            output: function (node) {
                node.expanded = true;
                node.buildRequest = function (builder, req) {
                    var added = false,
                        name = node.label,
                        objToAdd = builder.createObj(),
                        builderNodes = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    if (builderNodes.length) {
                        builderNodes.forEach(function (child) {
                            var childAdded = child.buildRequest(builder, objToAdd);
                            added = added || childAdded;
                        });
                    } else {
                        added = true;
                    }

                    if (added) {
                        builder.insertPropertyToObj(req, name, objToAdd);
                    }

                    return added;
                };

                node.fill = function (name, data) {
                    var match = comparePropToElemByName(name, node.label),
                        nodesToFill = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    if (match && nodesToFill.length) {
                        nodesToFill.forEach(function (child) {
                            for (var prop in data) {
                                child.fill(prop, data[prop]);
                            }
                        });
                        node.expanded = match;
                    }

                    return match;
                };

                node.clear = function () {
                    var nodesToClear = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    if (nodesToClear.length) {
                        nodesToClear.forEach(function (child) {
                            child.clear();
                        });
                    }
                };

                node.isFilled = function () {
                    return node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY).some(function (child) {
                        return child.isFilled();
                    });
                };

            },
            case: function (node) {
                node.buildRequest = function (builder, req) {
                    var added = false;

                    node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY).forEach(function (child) {
                        var childAdded = child.buildRequest(builder, req);
                        added = added || childAdded;
                    });

                    return added;
                };

                node.fill = function (name, data) {
                    var filled = false,
                        nodesToFill = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    nodesToFill.forEach(function (child) {
                        var childFilled = child.fill(name, data);
                        filled = filled || childFilled;
                    });

                    return filled;
                };

                node.clear = function () {
                    var nodesToClear = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    nodesToClear.forEach(function (child) {
                        child.clear();
                    });
                };

                node.isFilled = function () {
                    return node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY).some(function (child) {
                        return child.isFilled();
                    });
                };
            },
            choice: function (node) {
                node.choice = null;
                node.expanded = true;
                node.buildRequest = function (builder, req) {
                    var added = false;

                    if (node.choice) {
                        added = node.choice.buildRequest(builder, req);
                    }

                    return added;
                };

                node.fill = function (name, data) {
                    var filled = false,
                            nodesToFill = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    nodesToFill.forEach(function (child) {
                        var childFilled = child.fill(name, data);

                        if (childFilled) {
                            node.choice = child;
                        }

                        filled = filled || childFilled;
                        if (filled) {
                            return false;
                        }
                    });

                    return filled;
                };

                node.clear = function () {
                    node.nodeType = constantsSfc.NODE_UI_DISPLAY;

                    if (node.choice) {
                        node.choice.clear();
                        node.choice = null;
                    }
                };

                node.isFilled = function () {
                    return node.choice !== null;
                };
            },
            'leaf-list': function (node) {
                node.value = [];
                node.expanded = true;

                node.toggleExpand = function () {
                    node.expanded = !node.expanded;
                };

                node.addListElem = function () {
                    var newElement = {
                        value: ''
                    };
                    node.value.push(newElement);
                };

                node.removeListElem = function (elem) {
                    node.value.splice(node.value.indexOf(elem), 1);
                };

                node.buildRequest = function (builder, req) {

                    var valueArray = [];

                    for (var i = 0; i < node.value.length; i++) {
                        valueArray.push(node.value[i].value);
                    }

                    if (valueArray.length > 0) {
                        builder.insertPropertyToObj(req, node.label, valueArray);
                        return true;
                    }

                    return false;

                };


                node.fill = function (name, array) {
                    var match = comparePropToElemByName(name, node.label),
                            newLeafListItem;

                    if (match) {

                        for (var i = 0; i < array.length; i++) {
                            newLeafListItem = {
                                value: array[i]
                            };
                            node.value.push(newLeafListItem);
                        }

                    }
                    return match;
                };

                node.clear = function () {
                    node.nodeType = constantsSfc.NODE_UI_DISPLAY;
                    node.value = [];
                };

                node.isFilled = function () {
                    return node.value.length > 0;
                };

            },
            key: function (node) {
                // do this only on list, not on listElem because deepCopy on list doesn't copy property keys to listElem => don't do this when button for add new list is clicked
                if (node.parent.hasOwnProperty('refKey')) {
                    var keyLabels = node.label.split(' ');
                    node.parent.refKey = node.parent.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY).filter(function (child) {
                        return keyLabels.indexOf(child.label) > -1;
                    });
                }
            },
            list: function (node) {
                node.refKey = [];
                node.doubleKeyIndexes = [];
                node.actElemStructure = null;
                node.actElemIndex = -1;
                node.listData = [];
                node.filters = [];
                node.filteredListData = [];
                node.expanded = true;

                node.toggleExpand = function () {
                    node.expanded = !node.expanded;
                };

                node.createStructure = function () {
                    if (node.actElemStructure === null) {
                        var copy = node.deepCopy();
                        wrapper._listElem(copy);
                        node.actElemStructure = copy;
                        node.actElemStructure.getActElemIndex = node.getActElemIndex;
                    }
                };

                node.getActElemIndex = function() {
                    return node.actElemIndex;
                };

                node.addListElem = function () {
                    node.createStructure();
                    var newElemData = {};
                    node.listData.push(newElemData);
                    node.changeActElementData(node.listData.length - 1,true);
                };

                node.showListFilterWin = function () {
                    if(!(node.filters && node.filters.length)){
                        var newFilter = node.getChildrenForFilter().map(function(element){
                            var copy = element.deepCopyForFilter();
                            wrapper.wrapAll(copy);
                            return copy;
                        });
                        node.filters.push({name : 'Filter 1 name',filterNodes : newFilter});
                    }
                    console.info('showListFilterWin node',node,'node.filters',node.filters);
                };

                node.getFilterData = function (filterNodes) {
                    node.filters[node.currentFilter].filteredValues = node.filters[node.currentFilter].filterNodes.map(function(element){
                        var requestData = {};
                        element.buildRequest(reqBuilderSfc, requestData);
                        return requestData;
                    }).filter(function(item){
                        return $.isEmptyObject(item) === false;
                    });
                };


                node.switchFilter = function (showedFilter) {
                    node.getFilterData();
                    node.currentFilter = showedFilter;
                };

                node.createNewFilter = function () {
                    node.getFilterData();
                    var newFilter = node.getChildrenForFilter().map(function(element){
                        var copy = element.deepCopyForFilter();
                        wrapper.wrapAll(copy);
                        return copy;
                    });
                    node.filters.push({name : 'Filter ' + (node.filters.length+1) + ' name',filterNodes : newFilter});
                    node.switchFilter(node.filters.length-1);
                };

                node.getFilterResult = function(element,filterValue){
                    for (var i in filterValue){
                        if(typeof filterValue[i] == 'object'){
                            node.getFilterResult(element[i],filterValue[i]);
                        }else{
                            filterResult = element ? element[i] === filterValue[i] : false;
                        }
                    } 
                };

                node.applyFilter = function () {
                    node.getFilterData();

                    node.filteredListData = node.listData.slice().filter(function(element){
                        return node.filters.some(function(filter){
                            return filter.filteredValues.every(function(filterValue){
                                filterResult = null;
                                node.getFilterResult(element,filterValue);
                                return filterResult;
                            });
                        });
                    });

                    node.getActElementFilter();
                };

                node.clearFilterData = function (changeAct,filterForClear) {
                    if(filterForClear){
                        filterForClear--;
                        if(node.filters.length === 1){
                            node.filters = [];
                            var newFilter = node.getChildren('leaf',null,constantsSfc.NODE_UI_DISPLAY).map(function(element){
                              return element.deepCopy();
                             });
                            node.filters.push({name : 'Filter 1 name',filterNodes : newFilter});
                        }else{
                            node.filters.splice(filterForClear,1);
                            node.currentFilter = 0;
                        }
                    }else{
                        node.filters = [];
                        node.filteredListData = [];
                        node.currentFilter = 0;
                    }

                    if(changeAct){
                        node.getActElementFilter();
                    }

                };

                node.getActElementFilter = function () {

                    var actData = [];
                    
                    if(node.filteredListData && node.filteredListData.length){
                        node.actElemIndex = 0;
                        actData = node.filteredListData[node.actElemIndex];
                    }else{
                        node.actElemIndex = 0;
                        actData = node.listData[node.actElemIndex];
                    }
                    

                    node.actElemStructure.clear();
                    for (var prop in actData) {
                        node.actElemStructure.fillListElement(prop, actData[prop]);
                    }
                };

                node.buildActElemData = function () {
                    var list = [],
                            result;
                    if (node.actElemStructure) {
                        node.actElemStructure.listElemBuildRequest(reqBuilderSfc, list);
                        result = list[0] ? list[0] : {};
                    }
                    return result;
                };

                node.changeActElementData = function (index,fromAdd) {
                    var storeData = node.buildActElemData();
                    node.expanded = true;
                    // console.info('changeActElementData storeData',storeData);
                    if (node.actElemIndex > -1) { //we are changing already existing data
                        if(node.filteredListData && node.filteredListData.length){
                            node.listData[node.listData.indexOf(node.filteredListData[node.actElemIndex])] = storeData;
                            node.filteredListData[node.actElemIndex] = storeData;
                            if(fromAdd){
                               node.clearFilterData(true);
                            }
                        }else{
                            node.listData[node.actElemIndex] = storeData;
                        }
                    }
                    node.actElemIndex = index;

                    var actData = null;
                    if(!(node.filteredListData && node.filteredListData.length)){
                        actData = node.listData[node.actElemIndex];
                    }else{
                        actData = node.listData[node.listData.indexOf(node.filteredListData[node.actElemIndex])];
                    }
                    //vymazu sa filtre, ale nie su ulozene v listData a tam by ich bolo treba mat aby sa potom dali do actElemStructure
                    node.actElemStructure.clear();
                    for (var prop in actData) {
                        node.actElemStructure.fillListElement(prop, actData[prop]);
                    }

                    // console.info('changeActElementData node',node);
                };

                node.removeListElem = function (elemIndex,fromFilter) {

                    if(fromFilter){
                        elemIndex = node.listData.indexOf(node.filteredListData[elemIndex]);
                    }

                    node.listData.splice(elemIndex, 1);
                    node.actElemIndex = node.listData.length - 1;

                    if(fromFilter){
                        node.clearFilterData(true);
                    }

                    if (node.actElemIndex === -1) {
                        node.actElemStructure = null;
                    } else {
                        var actData = node.listData[node.actElemIndex];

                        node.actElemStructure.clear();
                        for (var prop in actData) {
                            node.actElemStructure.fillListElement(prop, actData[prop]);
                        }
                    }
                };

                node.buildRequest = function (builder, req) {
                    var added = false;

                    //store entered data
                    var storeData = node.buildActElemData();

                    if (node.actElemIndex > -1) {
                        if(node.filteredListData && node.filteredListData.length){
                            node.listData[node.listData.indexOf(node.filteredListData[node.actElemIndex])] = storeData;
                            node.filteredListData[node.actElemIndex] = storeData;
                        }else{
                            node.listData[node.actElemIndex] = storeData;
                        }
                    }

                    added = node.listData.filter(function (data) {
                        return $.isEmptyObject(data) === false;
                    }).length > 0;

                    var buildedDataCopy = node.listData.slice().map(function (item) {
                        if (item && item.hasOwnProperty('$$hashKey')) {
                            delete item['$$hashKey'];
                        }
                        return item;
                    });

                    // check of listElems keyValues duplicity
                    if(node.filteredListData && node.filteredListData.length){
                        node.doubleKeyIndexes = checkListElemKeys(node.filteredListData, node.refKey);
                    }else{
                        node.doubleKeyIndexes = checkListElemKeys(node.listData, node.refKey);
                    }

                    if (added) {
                        builder.insertPropertyToObj(req, node.label, buildedDataCopy);
                    }

                    return added;
                };

                node.fill = function (name, array) { //data is array

                    var match = comparePropToElemByName(name, node.label);

                    if (match && array.length) {
                        node.createStructure();
                        node.listData = array.slice();
                        node.actElemIndex = node.listData.length - 1;
                        for (var prop in node.listData[node.actElemIndex]) {
                            node.actElemStructure.fillListElement(prop, node.listData[node.actElemIndex][prop]);
                        }
                    }

                    return (match && array.length > 0);
                };

                node.clear = function () {
                    while (node.listData.length > 0) {
                        node.listData.pop();
                    }
                    while (node.filteredListData.length > 0) {
                        node.filteredListData.pop();
                    }
                    while (node.filters.length > 0) {
                        node.filters.pop();
                    }

                    node.actElemIndex = -1;
                    node.actElemStructure = null;
                    node.nodeType = constantsSfc.NODE_UI_DISPLAY;
                };

                node.isFilled = function () {
                    return node.listData.length > 0;
                };

                node.createListName = function (index) {
                    var name = '',
                        val = '';

                    if(node.filteredListData && node.filteredListData.length){
                        currentList = node.filteredListData;
                    }else{
                        currentList = node.listData;
                    }

                    if (index > -1) {
                        node.actElemStructure.refKey.forEach(function (key) {
                            var keyLabel = '';
                            if(index === node.getActElemIndex()) {
                                val = key.value !== '' ? key.label + ':' + key.value : '';
                            } else {
                                var prop = '';
                                if (!($.isEmptyObject(currentList[index]))) {
                                    if(currentList[index][key.label]) {
                                        prop = key.label;
                                    } else if(currentList[index][key.module + ':' + key.label]) {
                                        prop = key.module + ':' + key.label;
                                    }
                                    val = prop ? key.label + ':' + currentList[index][prop] : prop;
                                }
                            }

                            name = name ? (name + (val ? (' ' + val) : '')) : (name + (val ? (' <' + val) : ''));
                        });
                    }

                    if (name) {
                        name = name + '>';
                    }

                    return name;
                };
            },
            _listElem: function (node) {
                node.refKey = [];

                node.listElemBuildRequest = function (builder, req) {
                    var added = false,
                        objToAdd = builder.createObj();

                    node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY).forEach(function (child) {
                        var childAdded = child.buildRequest(builder, objToAdd);
                        added = added || childAdded;
                    });

                    if (added) {
                        builder.insertObjToList(req, objToAdd);
                    }

                    return added;
                };

                node.fillListElement = function (name, data) {
                    var filled = false;

                    node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY).forEach(function (child) {
                        var childFilled = child.fill(name, data);
                        filled = filled || childFilled;
                    });

                    return filled;
                };

                node.isFilled = function () {
                    return node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY).some(function (child) {
                        return child.isFilled();
                    });
                };

                node.clear = function () {
                    var nodesToClear = node.getChildren(null, null, constantsSfc.NODE_UI_DISPLAY);

                    if (nodesToClear.length) {
                        nodesToClear.forEach(function (child) {
                            child.clear();
                        });
                    }
                };

                node.children.forEach(function (child) {
                    wrapper.wrapAll(child);
                });
            }
        };

        wrapper.__test = {
            comparePropToElemByName: comparePropToElemByName,
            equalArrays: equalArrays,
            equalListElems: equalListElems,
            checkListElemKeys: checkListElemKeys,
            parseRestrictText: parseRestrictText,
            getTypes: getTypes
        };

        return wrapper;
    });

    yangUtilsSfc.factory('restrictionsFactSfc', function () {

        var RestrictionObject = function(fnc, info) {
            this.info = info;
            this.check = fnc;
        };

        var convertToInteger = function(value) {
            var strVal = typeof value === 'string' ? value : value.toString(),
                radix = strVal.indexOf('0x') === 0 ? 16 : strVal.indexOf('0') === 0 ? 8 : 10;

            return parseInt(strVal, radix);
        };

        var restrictions = {};

        restrictions.getEqualsFnc = function (target) {
            var intTarget = parseInt(target);
            
            return new RestrictionObject(
                function (value) {
                    var intVal = convertToInteger(value);
                    return intVal === intTarget;
                },
                'Value must be equal to '+target
            );
        };

        restrictions.getMinMaxFnc = function (min, max) {
            var intMin = parseInt(min),
                intMax = parseInt(max);
            
            return new RestrictionObject(
                function (value) {
                    var intVal = convertToInteger(value);
                    return (intMin <= intVal) && (intVal <= intMax);
                },
                'Value must be in between '+min+' and '+max
            );
        };

        restrictions.getReqexpValidationFnc = function (patternString) {
            return new RestrictionObject(
                function (value) {
                    var pattern = new RegExp(patternString);
                    return pattern.test(value.toString());
                },
                'Value must match '+patternString
            );
        };

        restrictions.getIsNumberFnc = function () {
            return new RestrictionObject(
                function (value) {
                    var pattern = new RegExp('^[+-]?((0x[0-9A-Fa-f]+)|(0[0-9]+)|([0-9]+))$');
                    return pattern.test(value.toString());
                },
                'Value must be number (+/-, 0x and 0) prefixed are permitted'
            );
        };

        restrictions.getIsUNumberFnc = function () {
            return new RestrictionObject(
                function (value) {
                    var pattern = new RegExp('^[+]?((0x[0-9A-Fa-f]+)|(0[0-9]+)|([0-9]+))$');
                    return pattern.test(value.toString());
                },
                'Value must be positive number (+, 0x and 0) prefixed are permitted'
            );
        };

        restrictions.getIsDecimalFnc = function () {
            return new RestrictionObject(
                function (value) {
                    var pattern = new RegExp("^[-]?[1-9]?[0-9]+[.|,]?[0-9]*$");
                    return pattern.test(value.toString());
                },
                'Value must be decimal number - prefix is permitted'
            );
        };

        restrictions.isInArray = function (array) {
            return new RestrictionObject(
                function (value) {
                    return array.some(function(arrVal) {
                        return arrVal === value;
                    });
                },
                'Value must be in ' + array.toString()
            );
        };


        return restrictions;
    });

    yangUtilsSfc.factory('yinParserSfc', function ($http, syncFactSfc, constantsSfc, arrayUtilsSfc, pathUtilsSfc) {
        var augmentType = 'augment';
        var path = './src/app/sfc/assets';

        var Module = function (name, revision, namespace) {
            this._name = name;
            this._revision = revision;
            this._namespace = namespace;
            this._statements = {};
            this._roots = [];
            this._augments = [];

            this.getRoots = function () {
                return this._roots;
            };

            this.getImportByPrefix = function (prefix) {
                var importNode = null;

                if (this._statements.hasOwnProperty('import')) {
                    importNode = this._statements.import.filter(function (importItem) {
                        return importItem._prefix === prefix;
                    })[0];
                }

                return importNode;
            };

            this.getRawAugments = function () {
                return this._augments;
            };

            this.getAugments = function () {
                var self = this;

                return this.getRawAugments().map(function (augNode) {
                    var prefixConverter = function (prefix) {
                        return self.getImportByPrefix(prefix).label;
                    };

                    augNode.path = pathUtilsSfc.translate(augNode.pathString, prefixConverter, self._name);

                    return new Augmentation(augNode);
                });
            };

            this.addChild = function (node) {
                if (!this._statements.hasOwnProperty(node.type)) {
                    this._statements[node.type] = [];
                }

                var duplicates = this._statements[node.type].filter(function (item) {
                    return node.label === item.label && node.nodeType === item.nodeType;
                });

                if (duplicates && duplicates.length > 0) {
                    console.warn('trying to add duplicate node', node, 'to module', this._statements);
                } else {
                    this._statements[node.type].push(node);

                    if (node.nodeType === constantsSfc.NODE_UI_DISPLAY || node.nodeType === constantsSfc.NODE_IDENTITY || node.nodeType === constantsSfc.NODE_LINK_TARGET) {
                        this._roots.push(node);
                    }

                    if (node.type === 'augment') {
                        this._augments.push(node);
                    }
                }
            };

            this.searchNode = function (type, name) {
                var searchResults = null,
                        searchedNode = null;

                if (this._statements[type]) {
                    searchResults = this._statements[type].filter(function (node) {
                        return name === node.label;
                    });
                }

                if (searchResults && searchResults.length === 0) {
                    console.warn('no nodes with type', type, 'and name', name, 'found in', this);
                } else if (searchResults && searchResults.length > 1) {
                    console.warn('multiple nodes with type', type, 'and name', name, 'found in', this);
                } else if (searchResults && searchResults.length === 1) {
                    searchedNode = searchResults[0];
                }

                return searchedNode;
            };
        };

        var Node = function (id, name, type, module, namespace, parent, nodeType, moduleRevision) {
            this.id = id;
            this.label = name;
            this.localeLabel = 'YANGUI_' + name.toUpperCase();
            this.type = type;
            this.module = module;
            this.children = [];
            this.parent = parent;
            this.nodeType = nodeType;
            this.namespace = namespace;
            this.moduleRevision = moduleRevision;
            this.currentFilter = 0;

            this.appendTo = function (parentNode) {
                parentNode.children.push(this);
                this.parent = parentNode;
            };

            this.addChild = function (node) {
                this.children.push(node);
                node.parent = this;
            };

            this.deepCopy = function deepCopy(additionalProperties) {
                var copy = new Node(this.id, this.label, this.type, this.module, this.namespace, null, this.nodeType, this.moduleRevision),
                    self = this;

                additionalProperties = additionalProperties || ['pathString'];

                additionalProperties.forEach(function(prop) {
                    if (prop !== 'children' && self.hasOwnProperty(prop) && copy.hasOwnProperty(prop) === false) {
                        copy[prop] = self[prop];
                    }
                });

                this.children.forEach(function (child) {
                    var childCopy = child.deepCopy();
                    childCopy.parent = copy;
                    copy.children.push(childCopy);
                });
                return copy;
            };

            this.getChildren = function (type, name, nodeType, property) {
                var filteredChildren = this.children.filter(function (item) {
                    return (name != null ? name === item.label : true) && (type != null ? type === item.type : true) && (nodeType != null ? nodeType === item.nodeType : true);
                });

                if (property) {
                    return filteredChildren.filter(function (item) {
                        return item.hasOwnProperty(property);
                    }).map(function (item) {
                        return item[property];
                    });
                } else {
                    return filteredChildren;
                }
            };

            this.childrenFilterConditions = function (children){
                var typesAllowed = ['case','choice','container','input','leaf','output','rpc'],
                    conditionTypes = function(item){
                        return typesAllowed.some(function(elem){
                            return elem === item.type;
                    });},
                    conditionEmptyChildren = function(item){
                        return item.children.some(function(child){
                            return (child.type != 'leaf-list' && child.type != 'list');
                    });},
                    conditionChildDescription = function(item){
                        return !(item.children.every(function(childDes){
                            return childDes.type == 'description';
                    }));};

                return children.filter(function(item){
                    if(item.parent.type == 'leaf' || item.parent.parent.type == 'leaf'){
                        return true;
                    }else{
                        return conditionTypes(item) && conditionEmptyChildren(item) && conditionChildDescription(item);
                    }
                });
            };

            this.getChildrenForFilter = function () {
                return this.childrenFilterConditions(this.getChildren(null,null,constantsSfc.NODE_UI_DISPLAY,null));
            };

            this.deepCopyForFilter = function deepCopyForFilter(additionalProperties) {

                var copy = new Node(this.id, this.label, this.type, this.module, this.namespace, null, this.nodeType, this.moduleRevision),
                    self = this;

                additionalProperties = additionalProperties || ['pathString'];

                additionalProperties.forEach(function(prop) {
                    if (prop !== 'children' && self.hasOwnProperty(prop) && copy.hasOwnProperty(prop) === false) {
                        copy[prop] = self[prop];
                    }
                });

                this.childrenFilterConditions(this.children).forEach(function (child) {
                    var childCopy = null;
                    if(child.type == 'leaf'){
                        childCopy = child.deepCopy();
                    }else{
                        childCopy = child.deepCopyForFilter();
                    }

                    childCopy.parent = copy;
                    copy.children.push(childCopy);
                });
                return copy;
            };
        };

        var Augmentation = function (node) {
            this.node = node;
            this.path = (node.path ? node.path : []);

            this.apply = function (nodeList) {
                var targetNode = this.getTargetNodeToAugment(nodeList);
                // console.info('applying augmentation',this.node.label,'-',this.getPathString());

                if (targetNode) {
                    this.node.children.forEach(function (child) {
                        child.appendTo(targetNode);
                    });
                } else {
                    console.warn(this.node.module + ' - can\'t find target node for augmentation ' + this.getPathString());
                }
            };

            this.getTargetNodeToAugment = function (nodeList) {
                return pathUtilsSfc.search({children: nodeList}, this.path.slice());
            };

            this.getPathString = function () {
                return this.path.map(function (elem) {
                    return elem.module + ':' + elem.name;
                }).join('/');
            };

        };

        var parentTag = function (xml) {
            if (xml.get(0).tagName.toLowerCase() === 'module') {
                return xml.get(0);
            } else {
                return parentTag(xml.parent());
            }
        };

        var parseYang = function parseYang(yinPath, callback, errorCbk) {
            var yangParser = new YangParser();

            $http.get(path + yinPath).success(function (data) {
                var moduleName = $($.parseXML(data).documentElement).attr('name'),
                    moduleNamespace = $($.parseXML(data)).find('namespace').attr('uri'),
                    moduleoduleRevision = $($.parseXML(data)).find('revision').attr('date'),
                    moduleObj = new Module(moduleName, moduleoduleRevision, moduleNamespace);

                yangParser.setCurrentModuleObj(moduleObj);
                yangParser.parse($.parseXML(data).documentElement, moduleObj);

                yangParser.sync.waitFor(function () {
                    callback(moduleObj);
                });
            }).error(function () {
                console.warn('can\'t find module: ' + yinPath);
                errorCbk();
                return null;
            });
        };

        var YangParser = function () {
            this.rootNodes = [];
            this.nodeIndex = 0;
            this.sync = syncFactSfc.generateObj();
            this.moduleObj = null;

            this.setCurrentModuleObj = function (moduleObj) {
                this.moduleObj = moduleObj;
            };

            this.createNewNode = function (name, type, parentNode, nodeType) {
                var node = new Node(this.nodeIndex++, name, type, this.moduleObj._name, this.moduleObj._namespace, parentNode, nodeType, this.moduleObj._revision);

                if (parentNode) {
                    parentNode.addChild(node);
                }

                return node;
            };

            this.parse = function (xml, parent) {
                var self = this;

                $(xml).children().each(function (_, item) {
                    var prop = item.tagName.toLowerCase();
                    if (self.hasOwnProperty(prop)) {
                        self[prop](item, parent);
                    } else {
                        // self.parse(this, parent);
                    }
                });
            };

            this.leaf = function (xml, parent) {
                var type = 'leaf',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_UI_DISPLAY,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this['leaf-list'] = function (xml, parent) {
                var type = 'leaf-list',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_UI_DISPLAY,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.container = function (xml, parent) {
                var type = 'container',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_UI_DISPLAY,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.choice = function (xml, parent) {
                var type = 'choice',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_UI_DISPLAY,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.case = function (xml, parent) {
                var type = 'case',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_UI_DISPLAY,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.list = function (xml, parent) {
                var type = 'list',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_UI_DISPLAY,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };


            this.key = function (xml, parent) {
                var type = 'key',
                    name = $(xml).attr('value'),
                    nodeType = constantsSfc.NODE_ALTER,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.description = function (xml, parent) {
                var type = 'description',
                    name = $(xml).children('text:first').text(),
                    nodeType = constantsSfc.NODE_ALTER,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.typedef = function (xml, parent, typedefName) {
                var type = 'typedef',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_LINK_TARGET,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.grouping = function (xml, parent, groupingName) {
                var type = 'grouping',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_LINK_TARGET,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.uses = function (xml, parent) {
                var type = 'uses',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_LINK,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.import = function (xml, parent) {
                var type = 'import',
                    name = $(xml).attr('module'),
                    nodeType = constantsSfc.NODE_ALTER,
                    node = this.createNewNode(name, type, parent, nodeType);

                node._prefix = $(xml).children('prefix:first').attr('value');
                node._revisionDate = $(xml).children('revision-date:first').attr('date');
            };

            this.augment = function (xml, parent) {
                var type = augmentType,
                    nodeType = constantsSfc.NODE_ALTER,
                    augmentIndentifier = $(xml).children("ext\\:augment-identifier:first").attr('identifier'),
                    name = augmentIndentifier ? augmentIndentifier : 'augment' + (this.nodeIndex + 1).toString(),
                    pathString = $(xml).attr('target-node'),
                    augmentRoot = this.createNewNode(name, type, parent, nodeType);

                augmentRoot.pathString = pathString;
                this.parse(xml, augmentRoot);
            };


            this.rpc = function (xml, parent) {
                var type = 'rpc',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_UI_DISPLAY,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.input = function (xml, parent) {
                var type = 'input',
                    name = 'input',
                    nodeType = constantsSfc.NODE_UI_DISPLAY,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.output = function (xml, parent) {
                var type = 'output',
                    name = 'output',
                    nodeType = constantsSfc.NODE_UI_DISPLAY,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.pattern = function (xml, parent) {
                var type = 'pattern',
                    name = $(xml).attr('value'),
                    nodeType = constantsSfc.NODE_RESTRICTIONS;

                this.createNewNode(name, type, parent, nodeType);
            };

            this.range = function (xml, parent) {
                var type = 'range',
                    name = $(xml).attr('value'),
                    nodeType = constantsSfc.NODE_RESTRICTIONS;

                this.createNewNode(name, type, parent, nodeType);
            };

            this.length = function (xml, parent) {
                var type = 'length',
                    name = $(xml).attr('value'),
                    nodeType = constantsSfc.NODE_RESTRICTIONS;

                this.createNewNode(name, type, parent, nodeType);
            };

            this.enum = function (xml, parent) {
                var type = 'enum',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_ALTER;

                this.createNewNode(name, type, parent, nodeType);
            };

            this.bit = function (xml, parent) {
                var type = 'bit',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_ALTER,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.position = function (xml, parent) {
                var type = 'position',
                    name = $(xml).attr('value'),
                    nodeType = constantsSfc.NODE_ALTER;

                this.createNewNode(name, type, parent, nodeType);
            };

            this.type = function (xml, parent) {
                var type = 'type',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_ALTER,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.identity = function (xml, parent) {
                var type = 'identity',
                    name = $(xml).attr('name'),
                    nodeType = constantsSfc.NODE_IDENTITY,
                    node = this.createNewNode(name, type, parent, nodeType);

                this.parse(xml, node);
            };

            this.base = function (xml, parent) {
                var type = 'base',
                  name = $(xml).attr('name'),
                  nodeType = constantsSfc.NODE_IDENTITY_BASE;

                  this.createNewNode(name, type, parent, nodeType);
            };
        };

        return {
            parse: parseYang,
            createNewNode: parseYang,
            __test: {
                path: path,
                parentTag: parentTag,
                yangParser: new YangParser(),
                Augmentation: Augmentation,
                Module: Module
            }
        };
    });

    yangUtilsSfc.factory('apiConnectorSfc', function ($http, syncFactSfc, arrayUtilsSfc, pathUtilsSfc, custFunctSfc) {
        var connector = {};

        var apiPathElemsToString = function (apiPathElems) {
            var s = apiPathElems.map(function (elem) {
                return elem.toString();
            }).join('');

            return s.slice(0, -1);
        };

        var SubApi = function (pathTemplateString, operations) {
            this.node = null;
            this.pathTemplateString = pathTemplateString;
            this.pathArray = [];
            this.operations = operations;
            this.custFunct = [];

            this.hasSetData = function () {
                return this.node !== null && this.pathArray.length > 0;
            };

            this.setNode = function (node) {
                this.node = node;
            };

            this.setPathArray = function (pathArray) {
                this.pathArray = pathArray;
            };

            this.buildApiRequestString = function () {
                return apiPathElemsToString(this.pathArray);
            };

            this.addCustomFunctionality = function (label, callback, viewStr) {
                var funct = custFunctSfc.createNewFunctionality(label, this.node, callback, viewStr);

                if (funct) {
                    this.custFunct.push(funct);
                }
            };
        };

        var parseApiPath = function (path) {
            var moduleIndexStart = path.lastIndexOf('/'),
                revisionIndexStart = path.lastIndexOf('(');

            return ({module: path.slice(moduleIndexStart + 1, revisionIndexStart), revision: path.slice(revisionIndexStart + 1, -1)});
        };

        connector.processApis = function (apis, callback) {
            var processedApis = [],
                sync = syncFactSfc.generateObj();

            apis.forEach(function (api) {
                var data = parseApiPath(api.path),
                    reqID = sync.spawnRequest(api.path),
                    apiObj = {
                        module: data.module,
                        revision: data.revision
                    };

                $http.get(api.path).success(function (data) {
                    var subApis = [];

                    data.apis.forEach(function (subApi) {
                        var operations = subApi.operations.map(function (item) {
                            return item.method;
                        }),
                                subApiElem = new SubApi(subApi.path, operations);

                        subApis.push(subApiElem);
                    });

                    apiObj.basePath = data.basePath;
                    apiObj.subApis = subApis;

                    processedApis.push(apiObj);
                    sync.removeRequest(reqID);
                }).error(function () {
                    sync.removeRequest(reqID);
                });
            });

            sync.waitFor(function () {
                callback(processedApis);
            });
        };

        var getRootNodeByPath = function (module, nodeLabel, nodeList) {
            var selNode = arrayUtilsSfc.getFirstElementByCondition(nodeList, function (item) {
                return item.module === module && item.label === nodeLabel; //&& item.revision === api.revision; //after revisions are implemented
            });

            if (!selNode) {
                console.warn('cannot find root node for module', module, 'label', nodeLabel);
            }
            return selNode;
        };

        connector.linkApisToNodes = function (apiList, nodeList) {
            return apiList.map(function (api) {

                api.subApis = api.subApis.map(function (subApi) {
                    var pathArray = pathUtilsSfc.translate(subApi.pathTemplateString, null, null),
                        rootNode = pathArray && pathArray.length > 1 ? getRootNodeByPath(pathArray[1].module, pathArray[1].name, nodeList) : null;

                    if (rootNode && pathArray) {
                        subApi.setNode(pathArray.length > 2 ? pathUtilsSfc.search(rootNode, pathArray.slice(2)) : rootNode);
                        subApi.setPathArray(pathArray);
                    }

                    return subApi;
                }).filter(function (subApi) {
                    return subApi.hasSetData();
                });

                return api;
            });
        };

        connector.createCustomFunctionalityApis = function (apis, module, revision, pathString, label, callback, viewStr) {
            apis = apis.map(function (item) {
                if ((module ? item.module === module : true) && (revision ? item.revision === revision : true)) {

                    item.subApis = item.subApis.map(function (subApi) {
                        if (subApi.pathTemplateString === pathString) {
                            subApi.addCustomFunctionality(label, callback, viewStr);
                        }

                        return subApi;
                    });
                }

                return item;
            });
        };

        connector.__test = {
            apiPathElemsToString: apiPathElemsToString,
            parseApiPath: parseApiPath,
            getRootNodeByPath: getRootNodeByPath,
            SubApi: SubApi
        };

        return connector;
    });

    yangUtilsSfc.factory('moduleConnectorSfc', function (constantsSfc) {

        var isBuildInType = function (type) {
            return ['int8', 'int16', 'int32', 'int64', 'uint8', 'uint16', 'uint32', 'uint64',
                    'decimal64', 'string', 'boolean', 'enumeration', 'bits', 'binary',
                    'leafref', 'identityref', 'empty', 'union', 'instance-identifier'].indexOf(type) > -1;
        };

        moduleConnector = {};

        var linkFunctions = {};
        linkFunctions.uses = function (usesNode, currentModule) {
            var targetType = 'grouping';
            return function (modules) {
                var data = findLinkedStatement(usesNode, targetType, currentModule, modules),
                    node = data.node,
                    module = data.module,
                    changed = false;

                if (node && module) {
                    usesNode.parent.children.splice(usesNode.parent.children.indexOf(usesNode), 1); //delete uses node
                    for (var i = 0; i < node.children.length; i++) {
                        applyLinks(node.children[i], module, modules);
                    }
                    appendChildren(usesNode.parent, node);
                    changed = true;
                }

                return changed;
            };
        };

        linkFunctions.type = function (typeNode, currentModule) {
            var targetType = 'typedef';

            if (isBuildInType(typeNode.label) === false) {
                return function (modules) {
                    var data = findLinkedStatement(typeNode, targetType, currentModule, modules),
                        node = data.node ? data.node.getChildren('type')[0] : null,
                        changed = false;

                    if (node) {
                        typeNode.parent.children.splice(typeNode.parent.children.indexOf(typeNode), 1); //delete referencing type node
                        typeNode.parent.addChild(node);
                        changed = true;
                    }

                    return changed;
                };
            } else {
                return function (modules) {
                    return false;
                };
            }
        };

        findLinkedStatement = function (node, targetType, currentModule, modules) {
            var sourceNode,
                sourceModule,
                link = node.label;

            if (link.indexOf(':') > -1) {
                var parts = link.split(':'),
                    targetImport = currentModule.getImportByPrefix(parts[0]);

                sourceModule = targetImport ? searchModule(modules, targetImport.label) : null;
                sourceNode = sourceModule ? sourceModule.searchNode(targetType, parts[1]) : null;
            } else {
                sourceModule = searchModule(modules, node.module);
                sourceNode = sourceModule ? sourceModule.searchNode(targetType, link) : null;
            }

            return {node: sourceNode, module: sourceModule};
        };

        var appendChildren = function (targetNode, sourceNode) {
            sourceNode.children.forEach(function (child) {
                targetNode.addChild(child);
            });
        };

        var searchModule = function (modules, moduleName, moduleRevision) {
            var searchResults = modules.filter(function (item) {
                    return (moduleName === item._name && (moduleRevision ? moduleRevision === item._revision : true));
                }),
                targetModule = (searchResults && searchResults.length) ? searchResults[0] : null;

            return targetModule;
        };
        var applyLinks = function (node, module, modules) {
            var changed = false;
            if (linkFunctions.hasOwnProperty(node.type)) { //applying link function to uses.node
                changed = linkFunctions[node.type](node, module)(modules);
            }

            for (var i = 0; i < node.children.length; i++) {
                if (applyLinks(node.children[i], module, modules)) {
                    i--; //need to repeat current index because we are deleting uses nodes, so in case there are more uses in row, it would skip second one
                }
            }

            return changed;
        };

        var interConnectModules = function (modules) {
            var rootNodes = [],
                augments = [];

            modules.forEach(function (module) {
                module.getRoots().concat(module.getRawAugments()).forEach(function (node) {
                    applyLinks(node, module, modules);
                });
            });

            modules.forEach(function (module) {
                module._roots = module.getRoots().map(function (node) {
                    copy = node.deepCopy();
                    return applyModuleName(copy, module._name);
                });

                module._augments = module.getRawAugments().map(function (node) {
                    copy = node.deepCopy();
                    return applyModuleName(copy, module._name);
                });
            });

            return modules;
        };

        var applyModuleName = function (node, module) {
            node.module = module;
            node.children.map(function (child) {
                return applyModuleName(child, module);
            });

            return node;
        };

        moduleConnector.processModuleObjs = function (modules) {
            var rootNodes = [],
                augments = [],
                connectedModules = interConnectModules(modules.slice());

            connectedModules.forEach(function (module) {
                rootNodes = rootNodes.concat(module.getRoots());
                augments = augments.concat(module.getAugments());
            });

            return {rootNodes: rootNodes, augments: augments};
        };

        moduleConnector.__test = {
            isBuildInType: isBuildInType,
            linkFunctions: linkFunctions,
            findLinkedStatement: findLinkedStatement,
            appendChildren: appendChildren,
            searchModule: searchModule,
            applyLinks: applyLinks,
            interConnectModules: interConnectModules,
            applyModuleName: applyModuleName
        };

        return moduleConnector;
    });

    yangUtilsSfc.factory('yangUtilsSfc', function ($http, yinParserSfc, nodeWrapperSfc, reqBuilderSfc, syncFactSfc, apiConnectorSfc, constantsSfc, pathUtilsSfc, moduleConnectorSfc, YangUtilsRestangularSfc) {

        var utils = {};

        // utils.exportModulesLocales = function(modules) {
        //     var obj = {},
        //         localeNodes = ['leaf','list','container','choice', 'leaf-list','rpc','input','output'];

        //     var process = function process(node) {
        //         if(localeNodes.indexOf(node.type) >= 0 && obj.hasOwnProperty(node.localeLabel) === false) {
        //             obj[node.localeLabel] = node.label;
        //         }

        //         node.children.forEach(function(child) {
        //             process(child);
        //         });
        //     };

        //     modules.forEach(function(module) {
        //         process(module);
        //     });

        //     return JSON.stringify(obj, null, 4);
        // };

        utils.generateNodesToApis = function (callback, errorCbk) {
            var allRootNodes = [],
                apiModules = [],
                topLevelSync = syncFactSfc.generateObj(),
                reqApis = topLevelSync.spawnRequest('apis'),
                reqAll = topLevelSync.spawnRequest('all');

            $http.get(YangUtilsRestangularSfc.configuration.baseUrl + '/apidoc/apis/').success(function (data) {
                apiConnectorSfc.processApis(data.apis, function (result) {
                    apiModules = result;
                    topLevelSync.removeRequest(reqApis);
                });
            }).error(function (result) {
                console.error('Error getting API data:', result);
                topLevelSync.removeRequest(reqApis);
            });

            $http.get(YangUtilsRestangularSfc.configuration.baseUrl + '/restconf/modules/').success(function (data) {
                utils.processModules(data.modules, function (result) {
                    allRootNodes = result.map(function (node) {
                        var copy = node.deepCopy();

                        nodeWrapperSfc.wrapAll(copy);
                        return copy;
                    });
                    topLevelSync.removeRequest(reqAll);
                });
            }).error(function (result) {
                console.error('Error getting API data:', result);
                topLevelSync.removeRequest(reqAll);
            });

            topLevelSync.waitFor(function () {
                try {
                    callback(apiConnectorSfc.linkApisToNodes(apiModules, allRootNodes), allRootNodes);
                } catch (e) {
                    errorCbk(e);
                    throw(e); //do not lose debugging info
                }
            });

        };

        utils.generateApiTreeData = function (apis, callback) {

            var dataTree = [],
                sync = syncFactSfc.generateObj(),
                add;

            dataTree = apis.map(function (item, indexApi) {

                var getApisAndPath = function (item, indexApi) {
                    var childrenArray = [];

                    item.subApis.map(function (itemSub, indexSubApi) {
                        var childIndex = 0;

                        var fillPath = function (path, array, indexSubApi, indexApi, itemSub) {
                            var existElemIndex = null,
                                existElem,
                                arrayIndex = null,
                                newElem = function (path, array) {
                                    var element = {};

                                    element.label = path[childIndex - 1].name;
                                    element.identifier = path[childIndex - 1].identifierName !== undefined ? ' {' + path[childIndex - 1].identifierName + '}' : '';
                                    element.children = [];
                                    array.push(element);
                                    return array;
                                };

                            childIndex++;
                            if (childIndex - 1 < path.length) {
                                if (array.length > 0) {
                                    existElem = false;

                                    array.map(function (arrayItem, index) {
                                        if (arrayItem.label === path[childIndex - 1].name) {
                                            existElem = true;
                                            existElemIndex = index;
                                        }
                                    });
                                    if (!existElem) {
                                        array = newElem(path, array);
                                    }
                                } else {
                                    array = newElem(path, array);
                                }
                                arrayIndex = existElemIndex !== null ? existElemIndex : array.length - 1;
                                var pathChildren = fillPath(path, array[arrayIndex].children, indexSubApi, indexApi, itemSub);
                                if (!pathChildren.length) {
                                    array[arrayIndex].indexApi = indexApi;
                                    array[arrayIndex].indexSubApi = indexSubApi;
                                    //array[arrayIndex].subApi = itemSub;
                                }
                                array[arrayIndex].children = pathChildren;
                                return array;
                            } else {
                                return [];
                            }
                        };

                        childrenArray = fillPath(itemSub.pathArray, childrenArray, indexSubApi, indexApi, itemSub);

                    });

                    return childrenArray;
                },
                        apisPath = getApisAndPath(item, indexApi);

                return {
                    label: item.module + ' rev.' + item.revision,
                    children: apisPath
                };
            });

            dataTree.forEach(function (item) {
                var findSupApi = function (treeElem) {
                        var apiInfo = null;
                        if (treeElem.hasOwnProperty('indexApi') && treeElem.hasOwnProperty('indexSubApi') && apis[treeElem.indexApi].subApis[treeElem.indexSubApi].operations.indexOf('PUT') > -1) {
                            apiInfo = {api: treeElem.indexApi, subApi: treeElem.indexSubApi};
                        } else if (treeElem.children.length && apiInfo === null) {
                            var searchResult = null;
                            for (var i = 0; i < treeElem.children.length && apiInfo === null; i++) {
                                apiInfo = findSupApi(treeElem.children[i]);
                            }
                        }
                        return apiInfo;
                    },
                    apiInfo = findSupApi(item),
                    checkAPIValidity = function (api, subApi) {
                        var fillDummyData = function (node) {
                            var leaves = node.getChildren('leaf'),
                                filled = false,
                                childFilled,
                                i;

                            if (leaves.length && node.type === 'list') {
                                node.addListElem();
                                node.actElemStructure.getChildren('leaf')[0].value = '0';
                                filled = true;
                            } else if (leaves.length) {
                                leaves[0].value = '0';
                                filled = true;
                            } else if (leaves.length === 0 && node.type === 'list') {
                                childFilled = false;
                                for (i = 0; i < node.actElemStructure.children.length && !childFilled; i++) {
                                    childFilled = fillDummyData(node.actElemStructure.children[i]);
                                }
                                filled = childFilled;
                            } else {
                                childFilled = false;
                                for (i = 0; i < node.children.length && !childFilled; i++) {
                                    childFilled = fillDummyData(node.children[i]);
                                }
                                filled = childFilled;
                            }

                            return filled;
                        },
                        requestData = {},
                        requestPath = api.basePath + '/' + subApi.buildApiRequestString(),
                        reqID = sync.spawnRequest(requestPath);

                        fillDummyData(subApi.node);
                        subApi.node.buildRequest(reqBuilderSfc, requestData);

                        $http({method: 'PUT', url: requestPath, data: requestData, headers: {"Content-Type": "application/yang.data+json"}}).
                            success(function (data) {
                                // console.debug('sending',reqBuilder.resultToString(requestData),'to',requestPath,'- success'); //TODO entry deletion?
                                sync.removeRequest(reqID);
                            }).
                            error(function (data, status) {
                                // console.debug('sending',reqBuilder.resultToString(requestData),'to',requestPath,'- error');
                                item.toRemove = true;
                                sync.removeRequest(reqID);
                            }
                        );
                    };

                // if (apiInfo) {
                //     checkAPIValidity(apis[apiInfo.api], apis[apiInfo.api].subApis[apiInfo.subApi]);
                // } else {
                //     item.toRemove = true;
                // }
            });

            sync.waitFor(function () {
                callback(dataTree.filter(function (item) {
                    return !item.hasOwnProperty('toRemove');
                }));
            });
        };

        utils.processModules = function (loadedModules, callback) {
            console.time('processModules');

            var modules = [],
                rootNodes = [],
                augments = [],
                syncModules = syncFactSfc.generateObj();

            loadedModules.module.forEach(function (module) {
                var reqId = syncModules.spawnRequest(module.name);

                yinParserSfc.parse('/yang2xml/' + module.name + '.yang.xml', function (module) {
                    modules.push(module);
                    syncModules.removeRequest(reqId);
                }, function () {
                    syncModules.removeRequest(reqId);
                });
            });

            syncModules.waitFor(function () {
                processedData = moduleConnectorSfc.processModuleObjs(modules);
                rootNodes = processedData.rootNodes;
                augments = processedData.augments;

                console.info(modules.length + ' modulesObj loaded', modules);
                console.info(rootNodes.length + ' rootNodes loaded', rootNodes);
                console.info(augments.length + ' augments loaded', augments);

                var sortedAugments = augments.sort(function (a, b) {
                    return a.path.length - b.path.length;
                });

                sortedAugments.map(function (elem) {
                    elem.apply(rootNodes);
                });

                callback(rootNodes);
                console.timeEnd('processModules');
            });
        };

        utils.getRequestString = function (node) {
            var request = reqBuilderSfc.createObj(),
                reqStr = '';

            node.buildRequest(reqBuilderSfc, request);

            if (request && $.isEmptyObject(request) === false) {
                reqStr = reqBuilderSfc.resultToString(request);
            }
            return reqStr;
        };
        
        utils.getPathString = function(basePath, selSubApi) {
            return basePath+'/'+selSubApi.buildApiRequestString();
        };

        utils.transformTopologyData = function (data) {
            var links = [],
                nodes = [],
                getNodeIdByText = function getNodeIdByText(inNodes, text) {
                    var nodes = inNodes.filter(function (item, index) {
                        return item.label === text;
                    }),
                            nodeId;

                    if (nodes.length > 0 && nodes[0]) {
                        nodeId = nodes[0].id;
                    } else {
                        return null;
                    }

                    return nodeId;
                };


            if (data['network-topology'] && data['network-topology'].topology.length) {
                var topoData = data['network-topology'].topology[0],
                        nodeId = 0,
                        linkId = 0;

                nodes = topoData.hasOwnProperty('node') ? topoData.node.map(function (nodeData) {
                    return {'id': (nodeId++).toString(), 'label': nodeData["node-id"], group: 'switch', value: 20, title: 'Name: <b>' + nodeData["node-id"] + '</b><br>Type: Switch'};
                }) : [];

                links = topoData.hasOwnProperty('link') ? topoData.link.map(function (linkData) {
                    var srcId = getNodeIdByText(nodes, linkData.source["source-node"]),
                            dstId = getNodeIdByText(nodes, linkData.destination["dest-node"]),
                            srcPort = linkData.source["source-tp"],
                            dstPort = linkData.destination["dest-tp"];
                    if (srcId != null && dstId != null) {
                        return {id: (linkId++).toString(), 'from': srcId, 'to': dstId, title: 'Source Port: <b>' + srcPort + '</b><br>Dest Port: <b>' + dstPort + '</b>'};
                    }
                }) : [];
            }

            return {nodes: nodes, links: links};
        };

        utils.__test = {
        };

        return utils;

    });

    yangUtilsSfc.factory('constantsSfc', function () {
        return  {
            NODE_UI_DISPLAY: 1,
            NODE_ALTER: 2,
            NODE_CONDITIONAL: 3,
            NODE_RESTRICTIONS: 4,
            NODE_LINK: 5,
            NODE_LINK_TARGET: 6,
            NODE_IDENTITY: 7
        };
    });
});

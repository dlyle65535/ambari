/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var App = require('app');

App.NodeManagersLiveView = App.TextDashboardWidgetView.extend({

  title: Em.I18n.t('dashboard.widgets.NodeManagersLive'),
  id: '19',

  model_type: 'yarn',

  hiddenInfo: function () {
    var nmActive = this.get('model.nodeManagersCountActive') == null ? Em.I18n.t('services.service.summary.notAvailable') : this.get('model.nodeManagersCountActive');
    var nmLost = this.get('model.nodeManagersCountLost') == null ? Em.I18n.t('services.service.summary.notAvailable') : this.get('model.nodeManagersCountLost');
    var nmUnhealthy = this.get('model.nodeManagersCountUnhealthy') == null ? Em.I18n.t('services.service.summary.notAvailable') : this.get('model.nodeManagersCountUnhealthy');
    var nmRebooted = this.get('model.nodeManagersCountRebooted') == null ? Em.I18n.t('services.service.summary.notAvailable'): this.get('model.nodeManagersCountRebooted');
    var nmDecom = this.get('model.nodeManagersCountDecommissioned') == null ? Em.I18n.t('services.service.summary.notAvailable') : this.get('model.nodeManagersCountDecommissioned');
    var result = [];
      result.pushObject(nmActive + " active");
      result.pushObject(nmLost + " lost");
      result.pushObject(nmUnhealthy + " unhealthy");
      result.pushObject(nmRebooted + " rebooted");
      result.pushObject(nmDecom + " decommissioned");
    return result;
  }.property('model.nodeManagersCountActive', 'model.nodeManagersCountLost',
    'model.nodeManagersCountUnhealthy', 'model.nodeManagersCountRebooted', 'model.nodeManagersCountDecommissioned'),
  hiddenInfoClass: "hidden-info-five-line",

  thresh1: 40,
  thresh2: 70,
  maxValue: 100,

  nodeManagersLive: function () {
    return this.get('model.nodeManagersStarted');
  }.property('model.nodeManagersStarted'),

  data: function () {
    var nodeManagers = this.get('model.nodeManagersTotal');
    var nodeManagersLive = this.get('nodeManagersLive');
    if (!nodeManagers) {
      return -1;
    } else {
      return (nodeManagersLive / nodeManagers).toFixed(2) * 100;
    }
  }.property('model.nodeManagersTotal', 'nodeManagersLive'),

  content: function () {
    return this.get('nodeManagersLive') + '/' + this.get('model.nodeManagersTotal');
  }.property('model.nodeManagersTotal', 'nodeManagersLive'),

  editWidget: function (event) {
    var parent = this;
    var max_tmp =  parseFloat(parent.get('maxValue'));
    var configObj = Ember.Object.create({
      thresh1: parent.get('thresh1') + '',
      thresh2: parent.get('thresh2') + '',
      hintInfo: Em.I18n.t('dashboard.widgets.hintInfo.hint1').format(max_tmp),
      isThresh1Error: false,
      isThresh2Error: false,
      errorMessage1: "",
      errorMessage2: "",
      maxValue: max_tmp,
      observeNewThresholdValue: function () {
        var thresh1 = this.get('thresh1');
        var thresh2 = this.get('thresh2');
        if (thresh1.trim() != "") {
          if (isNaN(thresh1) || thresh1 > max_tmp || thresh1 < 0) {
            this.set('isThresh1Error', true);
            this.set('errorMessage1', 'Invalid! Enter a number between 0 - ' + max_tmp);
          } else if (this.get('isThresh2Error') === false && parseFloat(thresh2)<= parseFloat(thresh1)){
            this.set('isThresh1Error', true);
            this.set('errorMessage1', 'Threshold 1 should be smaller than threshold 2 !');
          } else {
            this.set('isThresh1Error', false);
            this.set('errorMessage1', '');
          }
        } else {
          this.set('isThresh1Error', true);
          this.set('errorMessage1', 'This is required');
        }

        if (thresh2.trim() != "") {
          if (isNaN(thresh2) || thresh2 > max_tmp || thresh2 < 0) {
            this.set('isThresh2Error', true);
            this.set('errorMessage2', 'Invalid! Enter a number between 0 - ' + max_tmp);
          } else {
            this.set('isThresh2Error', false);
            this.set('errorMessage2', '');
          }
        } else {
          this.set('isThresh2Error', true);
          this.set('errorMessage2', 'This is required');
        }

        // update the slider handles and color
        if (this.get('isThresh1Error') === false && this.get('isThresh2Error') === false) {
          $("#slider-range").slider('values', 0 , parseFloat(thresh1));
          $("#slider-range").slider('values', 1 , parseFloat(thresh2));
        }
      }.observes('thresh1', 'thresh2')
    });

    var browserVerion = this.getInternetExplorerVersion();
    App.ModalPopup.show({
      header: 'Customize Widget',
      classNames: [ 'sixty-percent-width-modal-edit-widget'],
      bodyClass: Ember.View.extend({
        templateName: require('templates/main/dashboard/edit_widget_popup'),
        configPropertyObj: configObj
      }),
      primary: Em.I18n.t('common.apply'),
      onPrimary: function() {
        configObj.observeNewThresholdValue();
        if (!configObj.isThresh1Error && !configObj.isThresh2Error) {
          parent.set('thresh1', parseFloat(configObj.get('thresh1')) );
          parent.set('thresh2', parseFloat(configObj.get('thresh2')) );
          if (!App.get('testMode')) {
            //save to persit
            var big_parent = parent.get('parentView');
            big_parent.getUserPref(big_parent.get('persistKey'));
            var oldValue = big_parent.get('currentPrefObject');
            oldValue.threshold[parseInt(parent.id)] = [configObj.get('thresh1'), configObj.get('thresh2')];
            big_parent.postUserPref(big_parent.get('persistKey'),oldValue);
          }
          this.hide();
        }
      },

      didInsertElement: function () {
        var self = this;
        var handlers = [configObj.get('thresh1'), configObj.get('thresh2')];
        var colors = ['#B80000', '#FF8E00', '#95A800']; //color red, orange, green

        if (browserVerion == -1 || browserVerion > 9) {
          configObj.set('isIE9', false);
          configObj.set('isGreenOrangeRed', false);
          $("#slider-range").slider({
            range: true,
            min: 0,
            max: max_tmp,
            values: handlers,
            create: function (event, ui) {
              parent.updateColors(handlers,colors);
            },
            slide: function (event, ui) {
              parent.updateColors(ui.values,colors);
              configObj.set('thresh1', ui.values[0] + '');
              configObj.set('thresh2', ui.values[1] + '');
            },
            change: function (event, ui) {
              parent.updateColors(ui.values,colors);
            }
          });
        } else {
          configObj.set('isIE9', true);
          configObj.set('isGreenOrangeRed', false);
        }
      }
    });
  }

});

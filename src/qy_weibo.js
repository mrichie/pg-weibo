/*
 Copyright 2013-2014, QingYue Technology

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
(function(){

    document.addEventListener('bccoreready', onBCCoreReady, false);

    function onBCCoreReady(){
        var eventName = "org.qingyue.oad.ready";
        var oadManager = BC.oadManager = new BC.OadManager("org.qingyue.oad", eventName);
        BC.bluetooth.dispatchEvent(eventName);
    }

    var OadManager = BC.OadManager = BC.Plugin.extend({

        serviceUUID: 'f000ffc0-0451-4000-b000-000000000000',

        pluginInitialize : function(){
            BC.bluetooth.UUIDMap[this.serviceUUID] = BC.OadService;
        }

    });

    var UploadImage = BC.OadManager.UploadImage = function(success, error, filename){
        navigator.oad.uploadImage(success, error, filename);
    };

    var ValidateImage = BC.OadManager.ValidateImage = function(success, error, filename){
        navigator.oad.validateImage(success, error, filename);
    };

    var SetImageType = BC.OadManager.SetImageType = function(success, error, imgType){
        navigator.oad.setImageType(success, error, imgType);
    };

    var GetFWFiles = BC.OadManager.GetFWFiles = function(success, error){
        navigator.oad.getFWFiles(success, error);
    };

    var OadService = BC.OadService = BC.Service.extend({

        imageNotifyUUID: 'f000ffc1-0451-4000-b000-000000000000',

        imageBlockRequestUUID: 'f000ffc2-0451-4000-b000-000000000000',

        imgVersion: '0xFFFF',

        curImgType: null,

        configureProfile: function(){
            successFunc = this.writeSuccess;
            errorFunc = this.writeError;
            me = this;
            me.getCharacteristicByUUID(me.imageNotifyUUID)[0].subscribe(function(data){
                if(me.imgVersion == '0xFFFF'){
                    imgHdr = new Uint16Array(data.value.value);
                    imgType = imgHdr[0] & 0x01 ? 'B' : 'A';
                    me.curImgType = imgType;
                    BC.OadManager.SetImageType(function(msg){console.log(msg)}, null, data.value.getHexString());
                    me.imgVersion = data.value.getHexString();
                };
            });

            me.getCharacteristicByUUID(me.imageNotifyUUID)[0].write('hex',
                                                                        '0x00',
                                                                        successFunc,
                                                                        errorFunc);
            window.setTimeout(function(){
                me.getCharacteristicByUUID(me.imageNotifyUUID)[0].write('hex',
                                                                            '0x01',
                                                                            successFunc,
                                                                            errorFunc);
            }, 1500);
        },

        uploadImage: function(device, successCallBack, errorCallBack){
            if(this.curImgType == null)
                return;
            if(this.curImgType == 'A')
                var imgType = 'B';
            else
                var imgType = 'A';
            service = this;
            BC.OadManager.UploadImage(function(data){
                if(data.constructor == ArrayBuffer){
                    writeValue = data.getHexString();
                    if(data.byteLength == 12){
                        service.getCharacteristicByUUID(
                            service.imageNotifyUUID)[0].write('hex',
                                                              writeValue,
                                                              function(){ console.log('n w ok')},
                                                              function(){ console.log('b w error')});
                    }
                    else{
                        service.getCharacteristicByUUID(
                            service.imageBlockRequestUUID)[0].write('hex',
                                                              writeValue,
                                                              function(){ console.log('b w ok')},
                                                              function(){ console.log('b w error')});
                    };
                }else{
                    // show progress by data.secondsLeft
                    successCallBack(data);
                };
            }, function(msg){
                errorCallBack(msg);
            }, 'Image' + imgType + '.bin');
        },

        writeSuccess : function(){
            console.log('writeSuccess');
        },

        writeError : function(){
            console.log('writeFailed');
        },

    });


})();


ArrayBuffer.prototype.getHexString = function(){
    var length = this.byteLength;
    var dv = new DataView(this);
    var result = "";
    for (var i= 0; i < length; i++) {
        if(dv.getUint8(i) < 16){
            result += '0' + dv.getUint8(i).toString(16);
        }else{
            result += dv.getUint8(i).toString(16);
        }
    }
    return result;
};
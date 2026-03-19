var exec = require('cordova/exec');

var MLKitDocScanner = {
    scanDocument: function(options) {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, 'MLKitDocScannerPlugin', 'scanDocument', [options || {}]);
        });
    }
};

module.exports = MLKitDocScanner;
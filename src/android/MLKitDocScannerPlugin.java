package com.example.mlkit.docscanner;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;

import java.util.List;

public class MLKitDocScannerPlugin extends CordovaPlugin {

    private CallbackContext callbackContext;
    private ActivityResultLauncher<IntentSenderRequest> scannerLauncher;

    // Error constants
    private static final String ERROR_SCANNING_RESULT_NULL = "SCANNING_RESULT_NULL";
    private static final String ERROR_NO_PAGES_SCANNED = "NO_PAGES_SCANNED";
    private static final String ERROR_JSON_PROCESSING = "JSON_PROCESSING_ERROR";
    private static final String ERROR_SCANNER_START_FAILED = "SCANNER_START_FAILED";
    private static final String ERROR_SCANNING_CANCELLED = "SCANNING_CANCELLED";
    private static final String ERROR_SCANNING_FAILED = "SCANNING_FAILED";
    
    @Override
    protected void pluginInitialize() {
        AppCompatActivity activity = (AppCompatActivity) cordova.getActivity();
        scannerLauncher = activity.registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    GmsDocumentScanningResult scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.getData());
                    if (scanningResult != null) {
                        processResult(scanningResult);
                    } else {
                        sendError(ERROR_SCANNING_RESULT_NULL, "Scanning result is null");
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    sendError(ERROR_SCANNING_CANCELLED, "Scanning was cancelled by the user");
                } else {
                    sendError(ERROR_SCANNING_FAILED, "Scanning failed with result code: " + result.getResultCode());
                }
            }
        );
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        System.out.println("MLKitDocScannerPlugin: execute called with action: " + action);
        this.callbackContext = callbackContext;

        if (action.equals("scanDocument")) {
            System.out.println("MLKitDocScannerPlugin: scanDocument called");
            startDocumentScanner(args);
            return true;
        }

        return false;
    }

    private void startDocumentScanner(JSONArray args) {
        System.out.println("MLKitDocScannerPlugin: startDocumentScanner called");

        // Default values
        int pageLimit = 0;
        boolean includeJpeg = true;
        boolean includePdf = true;
        boolean galleryImport = true;

        // Parse arguments if provided
        if (args.length() > 0) {
            try {
                JSONObject options = args.getJSONObject(0);
                if (options.has("pageLimit")) {
                    pageLimit = options.getInt("pageLimit");
                }
                if (options.has("includeJpeg")) {
                    includeJpeg = options.getBoolean("includeJpeg");
                }
                if (options.has("includePdf")) {
                    includePdf = options.getBoolean("includePdf");
                }
            } catch (JSONException e) {
                System.err.println("MLKitDocScannerPlugin: Error parsing options: " + e.getMessage());
                sendError(ERROR_JSON_PROCESSING, "Error parsing options: " + e.getMessage());
                return;
            }
        }

        GmsDocumentScannerOptions.Builder optionsBuilder = new GmsDocumentScannerOptions.Builder();

        optionsBuilder.setGalleryImportAllowed(galleryImport);

        if (pageLimit > 0) {
            optionsBuilder.setPageLimit(pageLimit);
        }

        // Set result formats based on input
        if (includeJpeg && includePdf) {
            optionsBuilder.setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF);
        } else if (includeJpeg) {
            optionsBuilder.setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG);
        } else if (includePdf) {
            optionsBuilder.setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF);
        }

        GmsDocumentScannerOptions options = optionsBuilder.build();

        GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);
        scanner.getStartScanIntent(cordova.getActivity())
        .addOnSuccessListener(intentSender -> {
            System.out.println("MLKitDocScannerPlugin: Got start scan intent");
            scannerLauncher.launch(new IntentSenderRequest.Builder(intentSender).build());
        })
        .addOnFailureListener(e -> {
            System.out.println("MLKitDocScannerPlugin: Failed to start scanner: " + e.getMessage());
            sendError(ERROR_SCANNER_START_FAILED, "Failed to start scanner: " + e.getMessage());
        });
    }

    private void processResult(GmsDocumentScanningResult result) {
        if (result == null) {
            sendError(ERROR_SCANNING_RESULT_NULL, "Scanning result is null");
            return;
        }
        
        JSONObject resultJson = new JSONObject();
        try {
            List<GmsDocumentScanningResult.Page> pages = result.getPages();
            if (pages == null || pages.isEmpty()) {
                sendError(ERROR_NO_PAGES_SCANNED, "No pages were scanned");
                return;
            }
            JSONArray pagesJsonArray = new JSONArray();
            for (GmsDocumentScanningResult.Page page : pages) {
                pagesJsonArray.put(page.getImageUri());
            }
            resultJson.put("images", pagesJsonArray);
            
            // If the format is RESULT_FORMAT_PDF
            if (result.getPdf() != null) {
                resultJson.put("pdf", result.getPdf().getUri());
            }
            callbackContext.success(resultJson);
        } catch (JSONException e) {
            sendError(ERROR_JSON_PROCESSING, "Error processing result: " + e.getMessage());
        }
    }

    private void sendError(String errorCode, String errorMessage) {
        try {
            JSONObject error = new JSONObject();
            error.put("code", errorCode);
            error.put("message", errorMessage);
            callbackContext.error(error);
        } catch (JSONException e) {
            callbackContext.error("Error creating error object: " + e.getMessage());
        }
    }
}
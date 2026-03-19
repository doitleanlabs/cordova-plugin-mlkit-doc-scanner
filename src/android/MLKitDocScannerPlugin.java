package com.example.mlkit.docscanner;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    // Runtime options for current request
    private boolean includeJpeg = true;
    private boolean includePdf = true;
    private boolean returnBase64 = false;
    private int jpegQuality = 90;
    private String locale = "";
    private boolean openPreviewAfterScan = false;
    
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
        String scannerMode = "full";
        boolean returnBase64 = false;
        int jpegQuality = 90;
        boolean autoCrop = true;
        boolean autoEnhance = true;
        String locale = "";
        boolean openPreviewAfterScan = false;

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
                if (options.has("galleryImportAllowed")) {
                    galleryImport = options.getBoolean("galleryImportAllowed");
                }
                if (options.has("scannerMode")) {
                    scannerMode = options.getString("scannerMode");
                }
                if (options.has("returnBase64")) {
                    returnBase64 = options.getBoolean("returnBase64");
                }
                if (options.has("jpegQuality")) {
                    jpegQuality = options.getInt("jpegQuality");
                    if (jpegQuality < 0) {
                        jpegQuality = 0;
                    } else if (jpegQuality > 100) {
                        jpegQuality = 100;
                    }
                }
                if (options.has("autoCrop")) {
                    autoCrop = options.getBoolean("autoCrop");
                }
                if (options.has("autoEnhance")) {
                    autoEnhance = options.getBoolean("autoEnhance");
                }
                if (options.has("locale")) {
                    locale = options.getString("locale");
                }
                if (options.has("openPreviewAfterScan")) {
                    openPreviewAfterScan = options.getBoolean("openPreviewAfterScan");
                }
            } catch (JSONException e) {
                System.err.println("MLKitDocScannerPlugin: Error parsing options: " + e.getMessage());
                sendError(ERROR_JSON_PROCESSING, "Error parsing options: " + e.getMessage());
                return;
            }
        }

        this.includeJpeg = includeJpeg;
        this.includePdf = includePdf;
        this.returnBase64 = returnBase64;
        this.jpegQuality = jpegQuality;
        this.locale = locale;
        this.openPreviewAfterScan = openPreviewAfterScan;

        if (!autoCrop || !autoEnhance) {
            scannerMode = "base";
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

        if ("base".equalsIgnoreCase(scannerMode)) {
            optionsBuilder.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE);
        } else {
            optionsBuilder.setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL);
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
            JSONArray pagesBase64JsonArray = new JSONArray();
            for (GmsDocumentScanningResult.Page page : pages) {
                Uri imageUri = page.getImageUri();
                pagesJsonArray.put(imageUri != null ? imageUri.toString() : "");
                if (returnBase64 && includeJpeg && imageUri != null) {
                    pagesBase64JsonArray.put(uriToBase64(imageUri, true));
                }
            }
            if (includeJpeg) {
                resultJson.put("images", pagesJsonArray);
                if (returnBase64) {
                    resultJson.put("imagesBase64", pagesBase64JsonArray);
                }
            }
            
            // If the format is RESULT_FORMAT_PDF
            if (includePdf && result.getPdf() != null) {
                Uri pdfUri = result.getPdf().getUri();
                resultJson.put("pdf", pdfUri != null ? pdfUri.toString() : "");
                if (returnBase64 && pdfUri != null) {
                    resultJson.put("pdfBase64", uriToBase64(pdfUri, false));
                }
            }
            callbackContext.success(resultJson);
        } catch (JSONException e) {
            sendError(ERROR_JSON_PROCESSING, "Error processing result: " + e.getMessage());
        }
    }

    private String uriToBase64(Uri uri, boolean isImage) {
        try (InputStream inputStream = cordova.getActivity().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return "";
            }

            byte[] bytes;
            if (isImage) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap == null) {
                    return "";
                }
                ByteArrayOutputStream imageOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, imageOutputStream);
                bytes = imageOutputStream.toByteArray();
            } else {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                bytes = outputStream.toByteArray();
            }

            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (IOException e) {
            return "";
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
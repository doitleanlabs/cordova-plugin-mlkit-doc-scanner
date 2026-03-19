declare module '@dani.dev.pm/cordova-plugin-mlkit-doc-scanner' {

    /**
     * Options for scanning documents.
     */
    export interface ScanOptions {
        /**
         * The maximum number of pages to scan. (Default: no limit)
         */
        pageLimit?: number;
        
        /**
         * Whether to include JPEG images in the scan result. (Default: true)
         */
        includeJpeg?: boolean;
        
        /**
         * Whether to include a PDF in the scan result. (Default: true)
         */
        includePdf?: boolean;

        /**
         * Whether gallery import is enabled. (Default: true)
         * Android: supported.
         * iOS: accepted for API compatibility.
         */
        galleryImportAllowed?: boolean;

        /**
         * Scanner mode.
         * Android: supported (base/full).
         * iOS: accepted for API compatibility.
         */
        scannerMode?: 'base' | 'full';

        /**
         * Whether to return base64 content in addition to file URIs. (Default: false)
         */
        returnBase64?: boolean;

        /**
         * JPEG compression quality from 0 to 100. (Default: 90)
         */
        jpegQuality?: number;

        /**
         * Enable/disable automatic crop.
         * Android: influences scanner mode selection.
         * iOS: accepted for API compatibility.
         */
        autoCrop?: boolean;

        /**
         * Enable/disable automatic enhancement.
         * Android: influences scanner mode selection.
         * iOS: accepted for API compatibility.
         */
        autoEnhance?: boolean;

        /**
         * Preferred locale (e.g. pt-BR, en-US).
         * Currently accepted for API compatibility.
         */
        locale?: string;

        /**
         * Whether to open an additional preview after scan.
         * Currently accepted for API compatibility.
         */
        openPreviewAfterScan?: boolean;
    }

    /**
     * Result of a document scan.
     */
    export interface ScanResult {
        /**
         * Array of image URIs if JPEG format is included.
         */
        images?: string[];
        
        /**
         * URI of the PDF if PDF format is included.
         */
        pdf?: string;

        /**
         * Array of image base64 strings when returnBase64=true.
         */
        imagesBase64?: string[];

        /**
         * PDF base64 when returnBase64=true and PDF is enabled.
         */
        pdfBase64?: string;
    }

    /**
     * Enum representing possible error codes during document scanning.
     */
    export const enum ScanErrorCode {
        /**
         * Error code indicating that the scanning result is null.
         */
        SCANNING_RESULT_NULL = 'SCANNING_RESULT_NULL',
        
        /**
         * Error code indicating that no pages were scanned.
         */
        NO_PAGES_SCANNED = 'NO_PAGES_SCANNED',
        
        /**
         * Error code indicating a JSON processing error.
         */
        JSON_PROCESSING_ERROR = 'JSON_PROCESSING_ERROR',

        /**
         * Error code indicating that the scanner failed to start.
         */
        SCANNER_START_FAILED = 'SCANNER_START_FAILED',
        
        /**
         * Error code indicating that the scanning was cancelled.
         */
        SCANNING_CANCELLED = 'SCANNING_CANCELLED',
        
        /**
         * Error code indicating that the scanning failed.
         */
        SCANNING_FAILED = 'SCANNING_FAILED',

        /**
         * Error code indicating that document scanning is not supported on this device (iOS only).
         */
        SCANNER_NOT_SUPPORTED = 'SCANNER_NOT_SUPPORTED'
    }

    /**
     * Interface representing an error that occurred during document scanning.
     */
    export interface ScanError {
        /**
         * The error code.
         */
        code: ScanErrorCode;
        
        /**
         * The error message.
         */
        message: string;
    }

    /**
     * Interface for the ML Kit Document Scanner.
     */
    export interface MLKitDocScanner {
        /**
         * Scans a document with the given options.
         * @param options Options for scanning the document.
         * @returns A promise that resolves to the scan result.
         */
        scanDocument(options?: ScanOptions): Promise<ScanResult>;
    }

    const MLKitDocScanner: MLKitDocScanner;
    export default MLKitDocScanner;
}
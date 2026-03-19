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
        scanDocument(options: ScanOptions): Promise<ScanResult>;
    }

    const MLKitDocScanner: MLKitDocScanner;
    export default MLKitDocScanner;
}
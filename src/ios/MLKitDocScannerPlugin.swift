import Foundation
import VisionKit
import PDFKit
import UIKit

@objc(MLKitDocScannerPlugin)
class MLKitDocScannerPlugin: CDVPlugin, VNDocumentCameraViewControllerDelegate {

    private var callbackId: String?
    private var includeJpeg = true
    private var includePdf = true
    private var pageLimit = 0
    private var galleryImportAllowed = true
    private var scannerMode = "full"
    private var returnBase64 = false
    private var jpegQuality = 90
    private var autoCrop = true
    private var autoEnhance = true
    private var locale = ""
    private var openPreviewAfterScan = false

    @objc(scanDocument:)
    func scanDocument(_ command: CDVInvokedUrlCommand) {
        callbackId = command.callbackId

        includeJpeg = true
        includePdf = true
        pageLimit = 0
        galleryImportAllowed = true
        scannerMode = "full"
        returnBase64 = false
        jpegQuality = 90
        autoCrop = true
        autoEnhance = true
        locale = ""
        openPreviewAfterScan = false

        if let options = command.arguments.first as? [String: Any] {
            includeJpeg = options["includeJpeg"] as? Bool ?? true
            includePdf = options["includePdf"] as? Bool ?? true
            pageLimit = options["pageLimit"] as? Int ?? 0
            galleryImportAllowed = options["galleryImportAllowed"] as? Bool ?? true
            scannerMode = options["scannerMode"] as? String ?? "full"
            returnBase64 = options["returnBase64"] as? Bool ?? false
            jpegQuality = options["jpegQuality"] as? Int ?? 90
            autoCrop = options["autoCrop"] as? Bool ?? true
            autoEnhance = options["autoEnhance"] as? Bool ?? true
            locale = options["locale"] as? String ?? ""
            openPreviewAfterScan = options["openPreviewAfterScan"] as? Bool ?? false
        }

        if jpegQuality < 0 {
            jpegQuality = 0
        } else if jpegQuality > 100 {
            jpegQuality = 100
        }

        guard VNDocumentCameraViewController.isSupported else {
            sendError(code: "SCANNER_NOT_SUPPORTED", message: "Document scanning is not supported on this device")
            return
        }

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            let scanner = VNDocumentCameraViewController()
            scanner.delegate = self
            self.viewController.present(scanner, animated: true)
        }
    }

    func documentCameraViewController(_ controller: VNDocumentCameraViewController, didFinishWith scan: VNDocumentCameraScan) {
        controller.dismiss(animated: true) { [weak self] in
            self?.processScan(scan)
        }
    }

    func documentCameraViewControllerDidCancel(_ controller: VNDocumentCameraViewController) {
        controller.dismiss(animated: true) { [weak self] in
            self?.sendError(code: "SCANNING_CANCELLED", message: "Scanning was cancelled by the user")
        }
    }

    func documentCameraViewController(_ controller: VNDocumentCameraViewController, didFailWithError error: Error) {
        controller.dismiss(animated: true) { [weak self] in
            self?.sendError(code: "SCANNING_FAILED", message: error.localizedDescription)
        }
    }

    private func processScan(_ scan: VNDocumentCameraScan) {
        let count = pageLimit > 0 ? min(scan.pageCount, pageLimit) : scan.pageCount

        guard count > 0 else {
            sendError(code: "NO_PAGES_SCANNED", message: "No pages were scanned")
            return
        }

        let images = (0..<count).map { scan.imageOfPage(at: $0) }
        var result: [String: Any] = [:]

        if includeJpeg {
            let uris = images.enumerated().compactMap { (index, image) -> String? in
                guard let data = image.jpegData(compressionQuality: CGFloat(jpegQuality) / 100.0) else { return nil }
                let url = FileManager.default.temporaryDirectory.appendingPathComponent("scan_page_\(index + 1).jpg")
                try? data.write(to: url)
                return url.absoluteString
            }
            result["images"] = uris

            if returnBase64 {
                let base64Images = images.compactMap { image in
                    image.jpegData(compressionQuality: CGFloat(jpegQuality) / 100.0)?.base64EncodedString()
                }
                result["imagesBase64"] = base64Images
            }
        }

        if includePdf {
            let pdfDoc = PDFDocument()
            images.enumerated().forEach { index, image in
                if let page = PDFPage(image: image) {
                    pdfDoc.insert(page, at: index)
                }
            }
            let pdfUrl = FileManager.default.temporaryDirectory.appendingPathComponent("scan_document.pdf")
            if pdfDoc.write(to: pdfUrl) {
                result["pdf"] = pdfUrl.absoluteString
                if returnBase64, let pdfData = try? Data(contentsOf: pdfUrl) {
                    result["pdfBase64"] = pdfData.base64EncodedString()
                }
            }
        }

        // These options are currently accepted for API compatibility.
        // VisionKit does not expose explicit toggles for scanner mode, locale, crop/enhance,
        // gallery import, or an additional post-scan preview screen.
        _ = galleryImportAllowed
        _ = scannerMode
        _ = autoCrop
        _ = autoEnhance
        _ = locale
        _ = openPreviewAfterScan

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        commandDelegate.send(pluginResult, callbackId: callbackId)
    }

    private func sendError(code: String, message: String) {
        let result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: ["code": code, "message": message])
        commandDelegate.send(result, callbackId: callbackId)
    }
}

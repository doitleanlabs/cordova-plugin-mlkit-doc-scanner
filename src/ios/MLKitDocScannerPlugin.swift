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

    @objc(scanDocument:)
    func scanDocument(_ command: CDVInvokedUrlCommand) {
        callbackId = command.callbackId

        if let options = command.arguments.first as? [String: Any] {
            includeJpeg = options["includeJpeg"] as? Bool ?? true
            includePdf = options["includePdf"] as? Bool ?? true
            pageLimit = options["pageLimit"] as? Int ?? 0
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
                guard let data = image.jpegData(compressionQuality: 0.9) else { return nil }
                let url = FileManager.default.temporaryDirectory.appendingPathComponent("scan_page_\(index + 1).jpg")
                try? data.write(to: url)
                return url.absoluteString
            }
            result["images"] = uris
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
            }
        }

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result)
        commandDelegate.send(pluginResult, callbackId: callbackId)
    }

    private func sendError(code: String, message: String) {
        let result = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: ["code": code, "message": message])
        commandDelegate.send(result, callbackId: callbackId)
    }
}

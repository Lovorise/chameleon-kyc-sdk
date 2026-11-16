// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "ChameleonKYCUI",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(
            name: "ChameleonKYCUI",
            targets: ["ChameleonKYCUI"]
        )
    ],
    targets: [
        .target(
            name: "ChameleonKYCUI",
            path: "Sources",
            resources: [
                .copy("PrivacyInfo.xcprivacy")
            ]
        )
    ]
)

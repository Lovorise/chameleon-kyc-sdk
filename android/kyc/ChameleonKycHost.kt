package com.mtchship.chameleon.kyc

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.text.TextUtils
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.mtchship.chameleon.kyc.internal.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Arrays
import java.util.Locale
import java.util.UUID

enum class CaptureStep {
    CONSENT, COUNTRY, DOCUMENT, POA, BUSINESS_REGISTRY,
    NFC, LIVENESS, SELFIE, AGE_ESTIMATION, PORTRAIT_VIDEO, SCANNER, DONE
}

@Stable
class ChameleonSessionHandle internal constructor(private val onDestroy: () -> Unit) {
    var isDestroyed by mutableStateOf(false)
        private set

    fun destroy() {
        if (!isDestroyed) {
            isDestroyed = true
            onDestroy()
        }
    }
}

@Composable
fun rememberAutoContext(
    detectedCountryCode: String? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme()
): AutoContextResult {
    val locale = remember { Locale.getDefault().toLanguageTag() }
    val isRtl = remember(locale) {
        TextUtils.getLayoutDirectionFromLocale(Locale.forLanguageTag(locale)) == View.LAYOUT_DIRECTION_RTL
    }
    return remember(locale, isRtl, isDarkTheme, detectedCountryCode) {
        AutoContextResult(locale, isRtl, isDarkTheme, detectedCountryCode)
    }
}

@Composable
fun ChameleonKycHost(
    preset: VerificationPreset,
    transport: ChameleonTransport,
    modifier: Modifier = Modifier,
    locale: String? = null,
    detectedIpCountry: String? = null,
    countries: List<CountryOption>? = null,
    docKinds: List<DocumentKind>? = null,
    enableNfc: Boolean = true,
    labels: ChameleonLabels? = null,
    onStepChanged: ((CaptureStep) -> Unit)? = null,
    onComplete: (SignedReceipt) -> Unit,
    onError: ((ChameleonError) -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionId = remember { UUID.randomUUID().toString() }

    val nfcSupported = remember(enableNfc) {
        enableNfc && NfcAdapter.getDefaultAdapter(context) != null
    }
    val steps = remember(preset, nfcSupported) {
        PresetStateMachine.resolveSteps(preset, nfcSupported)
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var capturedFramesInStep by remember { mutableIntStateOf(0) }
    var statusHint by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            onError?.invoke(
                ChameleonError(ChameleonErrorCode.ERR_CAMERA_PERMISSION, "Camera permission required", false)
            )
        }
    }

    val currentStep = steps.getOrElse(currentStepIndex) { CaptureStep.DONE }
    val sessionHandle = remember { ChameleonSessionHandle(onDestroy = {}) }

    DisposableEffect(sessionHandle) {
        onDispose { sessionHandle.destroy() }
    }

    LaunchedEffect(currentStep) {
        capturedFramesInStep = 0
        statusHint = null
        onStepChanged?.invoke(currentStep)

        if (currentStep == CaptureStep.DONE && !sessionHandle.isDestroyed) {
            when (transport) {
                is ChameleonTransport.Cname -> {
                    runCatching { CnameIngressClient.finalizeSession(transport, sessionId) }
                        .onSuccess { onComplete(it) }
                        .onFailure {
                            onError?.invoke(
                                ChameleonError(ChameleonErrorCode.ERR_TRANSPORT, it.message ?: "Finalization failed", true)
                            )
                        }
                }
                is ChameleonTransport.Custom -> {
                    onComplete(SignedReceipt(sessionId, "", Instant.now().toString()))
                }
            }
        }
    }

    val isCameraStep = currentStep in setOf(
        CaptureStep.DOCUMENT, CaptureStep.POA, CaptureStep.BUSINESS_REGISTRY,
        CaptureStep.LIVENESS, CaptureStep.SELFIE, CaptureStep.AGE_ESTIMATION,
        CaptureStep.PORTRAIT_VIDEO, CaptureStep.SCANNER
    )

    LaunchedEffect(isCameraStep, hasCameraPermission) {
        if (isCameraStep && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val analyzer = remember(currentStep, isUploading) {
        YuvPreFlightAnalyzer(
            frameKindProvider = {
                if (!isCameraStep || isUploading || sessionHandle.isDestroyed) null
                else PresetStateMachine.frameKindForStep(currentStep)
            },
            onAcceptedFrame = { payload ->
                isUploading = true
                scope.launch {
                    try {
                        dispatchAndScrub(payload) { loaned ->
                            when (transport) {
                                is ChameleonTransport.Cname ->
                                    CnameIngressClient.uploadFrame(transport, sessionId, preset, loaned)
                                is ChameleonTransport.Custom ->
                                    transport.onFrameReady(loaned)
                            }
                        }
                        val target = PresetStateMachine.targetFramesForStep(preset, currentStep)
                        capturedFramesInStep += 1
                        if (capturedFramesInStep >= target) currentStepIndex += 1
                    } catch (err: Exception) {
                        onError?.invoke(
                            ChameleonError(ChameleonErrorCode.ERR_TRANSPORT, err.message ?: "Transport failed", true)
                        )
                    } finally {
                        isUploading = false
                    }
                }
            },
            onQualityWarning = { warning ->
                statusHint = warning.message
                onError?.invoke(warning)
            }
        )
    }

    val resolvedCountries = remember(countries, detectedIpCountry) {
        countries ?: listOf(
            CountryOption("CH", "Switzerland"),
            CountryOption("DE", "Germany"),
            CountryOption("AT", "Austria"),
            CountryOption("GB", "United Kingdom"),
            CountryOption("US", "United States")
        )
    }

    ChameleonStepScaffold(
        stepIndex = currentStepIndex,
        totalSteps = steps.size,
        onClose = {
            sessionHandle.destroy()
            onClose()
        },
        modifier = modifier
    ) {
        when (currentStep) {
            CaptureStep.CONSENT -> ConsentStepView(
                labels = labels,
                onAccept = { currentStepIndex += 1 },
                onDecline = {
                    sessionHandle.destroy()
                    onClose()
                }
            )
            CaptureStep.COUNTRY -> CountryStepView(
                countries = resolvedCountries,
                labels = labels,
                onSelect = { currentStepIndex += 1 }
            )
            CaptureStep.NFC -> {
                val activity = context as? Activity
                if (activity != null) {
                    DisposableEffect(activity) {
                        val reader = IsoDepNfcReader(
                            activity = activity,
                            onChipPayloadReady = { chipPayload ->
                                scope.launch {
                                    try {
                                        dispatchAndScrub(chipPayload) { loaned ->
                                            when (transport) {
                                                is ChameleonTransport.Cname ->
                                                    CnameIngressClient.uploadFrame(transport, sessionId, preset, loaned)
                                                is ChameleonTransport.Custom ->
                                                    transport.onFrameReady(loaned)
                                            }
                                        }
                                        currentStepIndex += 1
                                    } catch (e: Exception) {
                                        onError?.invoke(
                                            ChameleonError(ChameleonErrorCode.ERR_TRANSPORT, e.message ?: "NFC upload failed", true)
                                        )
                                    }
                                }
                            },
                            onNfcError = { err ->
                                statusHint = err.message
                                onError?.invoke(err)
                            }
                        )
                        reader.start()
                        onDispose { reader.stop() }
                    }
                }
                NfcAndProcessingView(
                    title = labels?.nfcScanPrompt ?: "Hold Document to Back of Device",
                    subtitle = statusHint ?: "Reading ICAO 9303 chip in volatile memory",
                    onSkip = { currentStepIndex += 1 }
                )
            }

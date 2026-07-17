package com.example.expensetracker.service

import ai.onnxruntime.OnnxTensor
import android.util.Log
import org.jtransforms.fft.FloatFFT_1D
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.PI
import kotlin.math.pow

/**
 * FunASR Paraformer-Large 离线语音识别引擎 — Fbank → LFR → CMVN → ONNX → BPE
 */
@Singleton
class FunAsrEngine @Inject constructor(private val modelManager: FunAsrModelManager) {

    companion object {
        private const val TAG = "FunAsrEngine"
        private const val SAMPLE_RATE = 16000
        private const val N_MELS = 80
        private const val FRAME_LENGTH = (SAMPLE_RATE * 25.0 / 1000).toInt()   // 400
        private const val FRAME_SHIFT  = (SAMPLE_RATE * 10.0 / 1000).toInt()   // 160
        private const val N_FFT = 512
        private const val PREEMPHASIS = 0.97f

        private const val LFR_M = 7
        private const val LFR_N = 6
        private const val LFR_DIM = N_MELS * LFR_M              // 560
    }

    private val hammingWindow by lazy {
        FloatArray(FRAME_LENGTH) { (0.54f - 0.46f * cos(2.0 * PI * it / (FRAME_LENGTH - 1))).toFloat() }
    }
    private val melFilterBank by lazy { createMelFilterBank() }
    private val fft by lazy { FloatFFT_1D(N_FFT.toLong()) }

    // ── 公有 API ──

    fun recognize(pcm: ShortArray): Result<String> = runCatching {
        // 1. Fbank (80维)
        val (fbank, numFbank) = extractFbank(pcm)
        if (numFbank == 0) throw Exception("音频过短")

        // 2. LFR (560维)
        val (lfr, numLfr) = applyLFR(fbank, numFbank)
        if (numLfr == 0) throw Exception("音频过短")

        // 3. CMVN
        applyCMVN(lfr, numLfr)

        // 4. ONNX 推理
        val tokenIds = runInference(lfr, numLfr)

        // 5. BPE → 文本
        val text = bpeDecode(tokenIds)
        if (text.isBlank()) throw Exception("未识别到文字")
        Log.d(TAG, "识别: \"$text\"")
        text
    }

    // ── Fbank (80维 log-Mel filterbank) ──

    private fun extractFbank(pcm: ShortArray): Pair<FloatArray, Int> {
        val numFrames = if (pcm.size < FRAME_LENGTH) 0
        else (pcm.size - FRAME_LENGTH) / FRAME_SHIFT + 1
        if (numFrames == 0) return Pair(FloatArray(0), 0)

        val fbank = FloatArray(numFrames * N_MELS)
        val fftBuf = FloatArray(N_FFT * 2)

        for (frame in 0 until numFrames) {
            val base = frame * FRAME_SHIFT
            fftBuf.fill(0f)

            // 加窗 + 预加重
            for (j in 0 until FRAME_LENGTH) {
                val sample = if (j + base == 0) pcm[0].toFloat() / 32768f
                else (pcm[base + j].toFloat() - PREEMPHASIS * pcm[base + j - 1].toFloat()) / 32768f
                fftBuf[j * 2] = sample * hammingWindow[j]
            }

            fft.complexForward(fftBuf)

            // 功率谱 → Mel → log
            val off = frame * N_MELS
            for (mel in 0 until N_MELS) {
                var sum = 0f
                val filter = melFilterBank[mel]
                for (k in 0..N_FFT / 2) {
                    sum += (fftBuf[k * 2].let { it * it } + fftBuf[k * 2 + 1].let { it * it }) / N_FFT * filter[k]
                }
                fbank[off + mel] = ln(sum.coerceAtLeast(1e-10f))
            }
        }
        return Pair(fbank, numFrames)
    }

    // ── LFR (lfr_m=7, lfr_n=6) ──

    private fun applyLFR(fbank: FloatArray, numFbank: Int): Pair<FloatArray, Int> {
        val numLfr = (numFbank + LFR_N - 1) / LFR_N
        val lfr = FloatArray(numLfr * LFR_DIM)

        for (i in 0 until numLfr) {
            val center = i * LFR_N
            val dst = i * LFR_DIM
            for (j in 0 until LFR_M) {
                var src = center + j - LFR_M / 2
                if (src < 0) src = 0
                if (src >= numFbank) src = numFbank - 1
                System.arraycopy(fbank, src * N_MELS, lfr, dst + j * N_MELS, N_MELS)
            }
        }
        return Pair(lfr, numLfr)
    }

    // ── CMVN ──

    private fun applyCMVN(lfr: FloatArray, numFrames: Int) {
        val mean = modelManager.getCmvnMean()
        val invstd = modelManager.getCmvnStd()
        for (f in 0 until numFrames) {
            val off = f * LFR_DIM
            for (d in 0 until LFR_DIM) lfr[off + d] = (lfr[off + d] - mean[d]) * invstd[d]
        }
    }

    // ── ONNX 推理 ──

    private fun runInference(features: FloatArray, numFrames: Int): LongArray {
        val session = modelManager.getSession()
        val env = modelManager.getEnvironment()

        val speechTensor = OnnxTensor.createTensor(env,
            FloatBuffer.wrap(features.copyOf(numFrames * LFR_DIM)),
            longArrayOf(1L, numFrames.toLong(), LFR_DIM.toLong()))

        val lengthsTensor = OnnxTensor.createTensor(env,
            IntBuffer.wrap(intArrayOf(numFrames)), longArrayOf(1L))

        val start = System.currentTimeMillis()
        val results = session.run(mapOf("speech" to speechTensor, "speech_lengths" to lengthsTensor))
        Log.d(TAG, "推理 ${System.currentTimeMillis() - start}ms (${numFrames}帧)")

        try {
            val logits = results.get(0) as? OnnxTensor
                ?: throw IllegalStateException("空输出")
            val shape = logits.info.shape
            val (tokens, vocab) = when (shape.size) {
                3 -> Pair(shape[1].toInt(), shape[2].toInt())
                2 -> Pair(shape[0].toInt(), shape[1].toInt())
                else -> throw IllegalStateException("未知shape: ${shape.contentToString()}")
            }
            val buf = logits.floatBuffer
            return LongArray(tokens) { t ->
                var max = 0; var maxV = Float.NEGATIVE_INFINITY
                for (v in 0 until vocab) {
                    val value = buf.get(t * vocab + v)
                    if (value > maxV) { maxV = value; max = v }
                }
                max.toLong()
            }
        } finally {
            speechTensor.close(); lengthsTensor.close(); results.close()
        }
    }

    // ── SentencePiece BPE 解码 ──

    private fun bpeDecode(tokenIds: LongArray): String {
        val vocab = modelManager.getTokenList()
        return buildString {
            for (id in tokenIds.map { it.toInt() }) {
                if (id <= 2 || id >= vocab.size) continue
                val token = vocab[id]
                if (token.endsWith("@@")) append(token.dropLast(2))
                else { append(' '); append(token) }
            }
        }.trim()
    }

    // ── Mel 滤波器组 ──

    private fun createMelFilterBank(): Array<FloatArray> {
        val numBins = N_FFT / 2 + 1
        val fMin = 2595.0 * log10(1.0 + 20.0 / 700.0)
        val fMax = 2595.0 * log10(1.0 + SAMPLE_RATE / 2.0 / 700.0)

        val bins = FloatArray(N_MELS + 2) { i ->
            val hz = 700.0 * (10.0.pow((fMin + (fMax - fMin) * i / (N_MELS + 1)) / 2595.0) - 1.0)
            hz.toFloat() * N_FFT / SAMPLE_RATE
        }
        return Array(N_MELS) { m ->
            FloatArray(numBins) { k ->
                val f = k.toFloat()
                when { f < bins[m] -> 0f; f <= bins[m + 1] -> (f - bins[m]) / (bins[m + 1] - bins[m])
                    f <= bins[m + 2] -> (bins[m + 2] - f) / (bins[m + 2] - bins[m + 1]); else -> 0f }
            }
        }
    }

    fun close() = modelManager.close()
}

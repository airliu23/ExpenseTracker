package com.example.expensetracker.service

import android.content.Context
import android.util.Log
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/** FunASR Paraformer-Large ONNX 模型管理 — assets 复制 / CMVN / Token / ONNX 会话 */
@Singleton
class FunAsrModelManager @Inject constructor(@ApplicationContext private val ctx: Context) {

    companion object {
        private const val TAG = "FunAsrModel"
        private const val ASSET_DIR = "funasr"
        private const val MODEL_DIR = "funasr_model"

        const val LFR_DIM = 560
    }

    private val dir get() = File(ctx.filesDir, MODEL_DIR)
    private fun file(name: String) = File(dir, name)

    private val assetFiles = mapOf(
        "paraformer_nano.onnx" to "paraformer_nano.onnx",
        "tokens.json" to "tokens.json",
        "cmvn_mean.bin" to "cmvn_mean.bin",
        "cmvn_invstd.bin" to "cmvn_invstd.bin"
    )

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var tokens: List<String> = emptyList()
    private var cmvnMean: FloatArray? = null
    private var cmvnStd: FloatArray? = null

    private val allExist get() = assetFiles.values.all { file(it).exists() }

    // ── 模型文件 ──

    suspend fun ensureModel(): Result<String> {
        if (allExist) return Result.success(dir.absolutePath)
        return withContext(Dispatchers.IO) {
            try {
                if (dir.exists()) dir.deleteRecursively()
                dir.mkdirs()
                for ((src, dst) in assetFiles) {
                    val out = file(dst)
                    ctx.assets.open("$ASSET_DIR/$src").use { input ->
                        out.outputStream().use { input.copyTo(it) }
                    }
                    Log.d(TAG, "复制: $src → ${out.length()}B")
                }
                Result.success(dir.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "复制失败: ${e.message}", e)
                dir.deleteRecursively()
                Result.failure(e)
            }
        }
    }

    // ── Token 表 ──

    fun loadTokenList(): List<String> {
        if (tokens.isNotEmpty()) return tokens
        val f = file("tokens.json")
        if (!f.exists()) return emptyList()
        val arr = JSONArray(f.readText())
        tokens = (0 until arr.length()).map { arr.getString(it) }
        Log.d(TAG, "Token: ${tokens.size}")
        return tokens
    }

    fun getTokenList() = tokens.ifEmpty { loadTokenList() }

    // ── CMVN ──

    fun loadCMVN(): Boolean {
        if (cmvnMean != null && cmvnStd != null) return true
        cmvnMean = readFloats(file("cmvn_mean.bin"), LFR_DIM)
        cmvnStd = readFloats(file("cmvn_invstd.bin"), LFR_DIM)
        return cmvnMean != null && cmvnStd != null
    }

    fun getCmvnMean() = cmvnMean ?: FloatArray(LFR_DIM)
    fun getCmvnStd() = cmvnStd ?: FloatArray(LFR_DIM)

    private fun readFloats(f: File, n: Int): FloatArray? {
        if (!f.exists() || f.length() != n * 4L) return null
        return ByteBuffer.wrap(f.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
            .asFloatBuffer().let { FloatArray(n).also { it2 -> it2.forEachIndexed { i, _ -> it2[i] = it.get(i) } } }
    }

    // ── ONNX Runtime ──

    fun loadSession(): OrtSession {
        ortSession?.let { return it }
        ortEnv = OrtEnvironment.getEnvironment()
        val opts = OrtSession.SessionOptions()
        try { opts.setIntraOpNumThreads(2) } catch (_: Exception) {}

        val path = file("paraformer_nano.onnx").absolutePath
        Log.d(TAG, "加载 ONNX: ${path}")
        ortSession = try {
            ortEnv!!.createSession(path, opts)
        } catch (e: Exception) {
            Log.w(TAG, "文件路径失败 → bytes: ${e.message}")
            ortEnv!!.createSession(File(path).readBytes(), opts)
        }
        Log.d(TAG, "ONNX 就绪")
        return ortSession!!
    }

    fun getSession() = ortSession ?: loadSession()
    fun getEnvironment() = ortEnv ?: run { loadSession(); ortEnv!! }

    fun close() {
        try { ortSession?.close(); ortSession = null } catch (_: Exception) {}
        try { ortEnv?.close(); ortEnv = null } catch (_: Exception) {}
    }
}

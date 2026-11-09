package com.hongducdev.toolkit.xposed

import android.app.Application
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import java.util.concurrent.Executors

/**
 * Hook áp dụng phông chữ tùy chỉnh làm font UI của hệ thống (SystemUI).
 *
 * Móc vào Typeface và TextView của tiến trình để thay thế toàn bộ font sans-serif/roboto/google-sans
 * bằng file font người dùng đã chọn. Mọi lỗi đều được bắt an toàn để không bao giờ gây crash.
 */
object SystemFontHook {
    private const val LOG_TAG = "ToolkitFontHook"
    private const val SETTINGS_URI = "content://${LunarSettingsProvider.AUTHORITY}"
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var activeCustomTypeface: Typeface? = null

    fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            installTypefaceHooks()
            installTextViewHooks()
            installApplicationHook(lpparam)
            Log.i(LOG_TAG, "Đã khởi tạo SystemFontHook cho ${lpparam.packageName}")
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "Lỗi khi cài đặt SystemFontHook", t)
            XposedBridge.log("$LOG_TAG: Lỗi khởi tạo: $t")
        }
    }

    private fun installApplicationHook(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                Application::class.java,
                "onCreate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val app = param.thisObject as? Application ?: return
                        executor.execute {
                            syncAndApplyFont(app)
                        }
                    }
                },
            )
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "Không thể hook Application.onCreate", t)
        }
    }

    private fun syncAndApplyFont(context: Context) {
        try {
            val uri = Uri.parse(SETTINGS_URI)
            val bundle =
                context.contentResolver.call(
                    uri,
                    LunarSettingsProvider.METHOD_GET_FONT_CONFIG,
                    null,
                    null,
                ) ?: return

            val isEnabled = bundle.getBoolean(LunarSettingsProvider.EXTRA_FONT_ENABLED, false)
            if (!isEnabled) {
                activeCustomTypeface = null
                Log.i(LOG_TAG, "Font UI tùy chỉnh đang tắt trong cài đặt")
                return
            }

            val fontVersion = bundle.getLong(LunarSettingsProvider.EXTRA_FONT_VERSION, 0L)
            val fontUriStr =
                bundle.getString(LunarSettingsProvider.EXTRA_FONT_URI)
                    ?: "$SETTINGS_URI/font"
            val fontUri = Uri.parse(fontUriStr)

            val fontDir =
                File(context.filesDir, "toolkit_fonts").apply {
                    if (!exists()) mkdirs()
                }
            val localFontFile = File(fontDir, "custom_system_font.ttf")
            val versionFile = File(fontDir, "font_version.txt")

            val currentVersion =
                if (versionFile.exists()) {
                    versionFile.readText().trim().toLongOrNull() ?: 0L
                } else {
                    0L
                }

            // Đồng bộ nếu chưa có file hoặc phiên bản font mới hơn
            if (!localFontFile.exists() || localFontFile.length() <= 0L || currentVersion != fontVersion) {
                Log.i(LOG_TAG, "Bắt đầu tải font mới (version $fontVersion) từ Toolkit...")
                context.contentResolver.openInputStream(fontUri)?.use { input ->
                    FileOutputStream(localFontFile).use { output ->
                        input.copyTo(output)
                    }
                }
                versionFile.writeText(fontVersion.toString())
                Log.i(LOG_TAG, "Đã lưu font mới thành công (${localFontFile.length()} bytes)")
            }

            if (localFontFile.exists() && localFontFile.length() > 0L) {
                val typeface = Typeface.createFromFile(localFontFile)
                if (typeface != null) {
                    activeCustomTypeface = typeface
                    applyToSystemFontMap(typeface)
                    Log.i(LOG_TAG, "Đã áp dụng Typeface tùy chỉnh cho SystemUI thành công!")
                }
            }
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "Lỗi khi đồng bộ hoặc áp dụng font", t)
            XposedBridge.log("$LOG_TAG: Lỗi đồng bộ font: $t")
        }
    }

    private fun applyToSystemFontMap(customTypeface: Typeface) {
        try {
            @Suppress("UNCHECKED_CAST")
            val currentMap =
                XposedHelpers.getStaticObjectField(
                    Typeface::class.java,
                    "sSystemFontMap",
                ) as? Map<String, Typeface>

            val newMap = HashMap<String, Typeface>()
            if (currentMap != null) {
                newMap.putAll(currentMap)
            }

            val regular = customTypeface
            val bold = Typeface.create(customTypeface, Typeface.BOLD)
            val medium =
                try {
                    Typeface.create(customTypeface, 500, false)
                } catch (_: Throwable) {
                    bold
                }
            val light =
                try {
                    Typeface.create(customTypeface, 300, false)
                } catch (_: Throwable) {
                    regular
                }
            val thin =
                try {
                    Typeface.create(customTypeface, 100, false)
                } catch (_: Throwable) {
                    light
                }
            val black =
                try {
                    Typeface.create(customTypeface, 900, false)
                } catch (_: Throwable) {
                    bold
                }

            // Ghi đè toàn bộ các họ font sans-serif & Google Sans
            val keysToReplace =
                listOf(
                    "sans-serif",
                    "sans-serif-regular",
                    "roboto",
                    "roboto-regular",
                    "google-sans",
                    "google-sans-regular",
                    "google-sans-text",
                    "gs-variable",
                    "sec-roboto-light",
                )
            for (key in keysToReplace) {
                newMap[key] = regular
            }

            newMap["sans-serif-medium"] = medium
            newMap["roboto-medium"] = medium
            newMap["google-sans-medium"] = medium

            newMap["sans-serif-bold"] = bold
            newMap["roboto-bold"] = bold
            newMap["google-sans-bold"] = bold

            newMap["sans-serif-light"] = light
            newMap["sans-serif-thin"] = thin
            newMap["sans-serif-black"] = black

            XposedHelpers.setStaticObjectField(
                Typeface::class.java,
                "sSystemFontMap",
                Collections.unmodifiableMap(newMap),
            )

            XposedHelpers.setStaticObjectField(Typeface::class.java, "DEFAULT", regular)
            XposedHelpers.setStaticObjectField(Typeface::class.java, "SANS_SERIF", regular)
            XposedHelpers.setStaticObjectField(Typeface::class.java, "DEFAULT_BOLD", bold)

            Log.i(LOG_TAG, "Đã cập nhật static fields Typeface.sSystemFontMap, DEFAULT, SANS_SERIF")
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "Không thể ghi đè Typeface.sSystemFontMap qua reflection", t)
        }
    }

    private fun installTypefaceHooks() {
        try {
            // Hook Typeface.create(String, Int)
            XposedHelpers.findAndHookMethod(
                Typeface::class.java,
                "create",
                String::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val customTf = activeCustomTypeface ?: return
                        val familyName = param.args[0] as? String
                        val style = param.args[1] as? Int ?: Typeface.NORMAL

                        if (familyName == null ||
                            familyName.isEmpty() ||
                            familyName.contains("sans", ignoreCase = true) ||
                            familyName.contains("roboto", ignoreCase = true) ||
                            familyName.contains("google", ignoreCase = true)
                        ) {
                            param.result = Typeface.create(customTf, style)
                        }
                    }
                },
            )

            // Hook Typeface.create(Typeface, Int)
            XposedHelpers.findAndHookMethod(
                Typeface::class.java,
                "create",
                Typeface::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val customTf = activeCustomTypeface ?: return
                        val family = param.args[0] as? Typeface
                        val style = param.args[1] as? Int ?: Typeface.NORMAL

                        if (family == null ||
                            family == Typeface.DEFAULT ||
                            family == Typeface.SANS_SERIF ||
                            family == customTf
                        ) {
                            param.result = Typeface.create(customTf, style)
                        }
                    }
                },
            )

            // Hook Typeface.create(Typeface, Int, Boolean)
            try {
                XposedHelpers.findAndHookMethod(
                    Typeface::class.java,
                    "create",
                    Typeface::class.java,
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val customTf = activeCustomTypeface ?: return
                            val family = param.args[0] as? Typeface
                            val weight = param.args[1] as? Int ?: 400
                            val italic = param.args[2] as? Boolean ?: false

                            if (family == null ||
                                family == Typeface.DEFAULT ||
                                family == Typeface.SANS_SERIF ||
                                family == customTf
                            ) {
                                param.result = Typeface.create(customTf, weight, italic)
                            }
                        }
                    },
                )
            } catch (_: Throwable) {
                // Phương thức này có thể khác signature trên một số phiên bản Android
            }

            // Hook Typeface.defaultFromStyle(Int)
            XposedHelpers.findAndHookMethod(
                Typeface::class.java,
                "defaultFromStyle",
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val customTf = activeCustomTypeface ?: return
                        val style = param.args[0] as? Int ?: Typeface.NORMAL
                        param.result = Typeface.create(customTf, style)
                    }
                },
            )
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "Không thể hook các phương thức Typeface", t)
        }
    }

    private fun installTextViewHooks() {
        try {
            // Hook TextView.onAttachedToWindow
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val tv = param.thisObject as? TextView ?: return
                        val customTf = activeCustomTypeface ?: return
                        val currentTf = tv.typeface
                        if (currentTf == Typeface.MONOSPACE) return
                        if (currentTf != customTf) {
                            val style = currentTf?.style ?: Typeface.NORMAL
                            tv.setTypeface(customTf, style)
                        }
                    }
                },
            )

            // Hook TextView.setTypeface(Typeface)
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "setTypeface",
                Typeface::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val customTf = activeCustomTypeface ?: return
                        val currentTf = param.args[0] as? Typeface
                        if (currentTf == Typeface.MONOSPACE) return
                        if (currentTf == null || currentTf == Typeface.DEFAULT || currentTf == Typeface.SANS_SERIF) {
                            param.args[0] = customTf
                        }
                    }
                },
            )

            // Hook TextView.setTypeface(Typeface, Int)
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "setTypeface",
                Typeface::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val customTf = activeCustomTypeface ?: return
                        val currentTf = param.args[0] as? Typeface
                        if (currentTf == Typeface.MONOSPACE) return
                        if (currentTf == null || currentTf == Typeface.DEFAULT || currentTf == Typeface.SANS_SERIF) {
                            param.args[0] = customTf
                        }
                    }
                },
            )
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "Không thể hook TextView", t)
        }
    }
}

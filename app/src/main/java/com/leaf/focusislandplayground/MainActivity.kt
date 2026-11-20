package com.leaf.focusislandplayground

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var titleField: EditText
    private lateinit var frontField: EditText
    private lateinit var contentField: EditText
    private lateinit var tickerField: EditText
    private lateinit var jsonField: EditText
    private lateinit var templateSpinner: Spinner
    private lateinit var os3Button: Button
    private lateinit var os2Button: Button
    private lateinit var previewTitle: TextView
    private lateinit var previewFront: TextView
    private lateinit var previewContent: TextView
    private lateinit var statusLabel: TextView
    private lateinit var templates: JSONArray
    private var activeIndex = 0
    private var selectedProtocol = 3
    private val purple = Color.rgb(161, 143, 255)
    private val mint = Color.rgb(115, 228, 194)
    private val bg = Color.rgb(8, 11, 18)
    private val card = Color.rgb(19, 23, 34)
    private val muted = Color.rgb(153, 164, 185)
    private val notificationChannel = "focus_island_playground"
    private val notificationId = 292
    private val schedulePrefs by lazy { getSharedPreferences("playground", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        templates = loadTemplates()
        buildUi()
        createNotificationChannel()
        refreshDeviceStatus()
        applyTemplate(0)
    }

    private fun loadTemplates(): JSONArray = try {
        val text = assets.open("templates.json").bufferedReader().use { it.readText() }
        JSONObject(text).getJSONArray("templates")
    } catch (_: Exception) {
        JSONArray()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(18), dp(18), dp(18), dp(30))
            fitsSystemWindows = true
        }
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(page, ViewGroup.LayoutParams(-1, -2))

        page.addView(label("FOCUS ISLAND LAB", 12, purple, true))
        page.addView(label("焦点岛 Playground", 30, Color.WHITE, true), space(0, 2, 0, 4))
        page.addView(label("HyperOS 通知模板 · JSON 实验台", 14, muted, false))
        page.addView(gap(18))

        val statusCard = cardBox()
        statusLabel = label("正在检测设备能力…", 13, Color.WHITE, false)
        statusCard.addView(label("设备兼容性", 16, Color.WHITE, true))
        statusCard.addView(statusLabel, space(0, 7, 0, 0))
        statusCard.addView(label("提示：超级岛效果受系统版本、通知权限及小米焦点通知白名单影响。", 12, muted, false), space(0, 8, 0, 0))
        page.addView(statusCard)
        page.addView(gap(16))

        val previewCard = cardBox()
        previewCard.addView(label("岛摘要预览", 16, Color.WHITE, true))
        previewCard.addView(label("本地示意图，不代表系统最终渲染", 12, muted, false), space(0, 3, 0, 12))
        val island = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.rgb(1, 2, 4), 28)
            setPadding(dp(18), dp(14), dp(18), dp(14))
            minimumHeight = dp(82)
        }
        previewFront = label("充电中", 11, mint, true)
        previewTitle = label("68%", 18, Color.WHITE, true)
        previewContent = label("预计 24 分钟充满", 12, muted, false)
        island.addView(previewFront)
        island.addView(previewTitle, space(0, 3, 0, 0))
        island.addView(previewContent, space(0, 2, 0, 0))
        previewCard.addView(island, LinearLayout.LayoutParams(-1, -2))
        page.addView(previewCard)
        page.addView(gap(16))

        val editorCard = cardBox()
        editorCard.addView(label("快速编辑", 17, Color.WHITE, true))
        editorCard.addView(label("选择模板后修改文案，再生成系统 JSON。", 12, muted, false), space(0, 4, 0, 10))
        templateSpinner = Spinner(this)
        val names = ArrayList<String>()
        for (i in 0 until templates.length()) names.add(templates.optJSONObject(i)?.optString("name", "模板 ${i + 1}") ?: "模板 ${i + 1}")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        templateSpinner.adapter = adapter
        templateSpinner.setSelection(0)
        templateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != activeIndex || jsonField.text.isNullOrBlank()) applyTemplate(position)
            }
        }
        editorCard.addView(templateSpinner, space(0, 0, 0, 8))
        editorCard.addView(label("目标格式", 12, muted, true), space(0, 2, 0, 5))
        val protocolRow = horizontalRow()
        os3Button = button("OS3 · 超级岛", purple) {
            selectedProtocol = 3
            updateProtocolButtons()
            generateJson(false)
        }
        os2Button = button("OS2 · 焦点通知", Color.rgb(47, 55, 75)) {
            selectedProtocol = 2
            updateProtocolButtons()
            generateJson(false)
        }
        protocolRow.addView(os3Button, weightParams())
        protocolRow.addView(gap(8, horizontal = true))
        protocolRow.addView(os2Button, weightParams())
        editorCard.addView(protocolRow, space(0, 0, 0, 5))
        titleField = addField(editorCard, "主要文本", "68%", false)
        frontField = addField(editorCard, "前置描述", "充电中", false)
        contentField = addField(editorCard, "补充内容", "预计 24 分钟充满", false)
        tickerField = addField(editorCard, "状态栏摘要", "正在充电", false)
        val generateButton = button("生成 JSON 预览", purple)
        generateButton.setOnClickListener { generateJson(true) }
        editorCard.addView(generateButton, space(0, 12, 0, 0))
        page.addView(editorCard)
        page.addView(gap(16))

        val jsonCard = cardBox()
        jsonCard.addView(label("JSON 编辑器", 17, Color.WHITE, true))
        jsonCard.addView(label("支持完整导入/导出；通知通过 miui.focus.param 发送。", 12, muted, false), space(0, 4, 0, 8))
        jsonField = EditText(this).apply {
            setTextColor(Color.rgb(218, 225, 240))
            setHintTextColor(Color.rgb(105, 116, 138))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.TOP or Gravity.START
            minLines = 10
            maxLines = 18
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = rounded(Color.rgb(10, 13, 20), 14)
            hint = "生成或粘贴 JSON…"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        jsonCard.addView(jsonField, LinearLayout.LayoutParams(-1, dp(236)))
        val fileActions = horizontalRow()
        fileActions.addView(button("导入 JSON", Color.rgb(47, 55, 75)) { importJson() }, weightParams())
        fileActions.addView(gap(8, horizontal = true))
        fileActions.addView(button("导出 JSON", Color.rgb(47, 55, 75)) { exportJson() }, weightParams())
        jsonCard.addView(fileActions, space(0, 10, 0, 0))
        val shareButton = button("分享 JSON", Color.rgb(47, 55, 75)) { shareJson() }
        jsonCard.addView(shareButton, space(0, 8, 0, 0))
        page.addView(jsonCard)
        page.addView(gap(16))

        val actionCard = cardBox()
        actionCard.addView(label("发送与延时", 17, Color.WHITE, true))
        actionCard.addView(label("排队使用 Android 闹钟；待机时实际触发时间可能略有偏差。", 12, muted, false), space(0, 4, 0, 10))
        val sendButton = button("立即发送通知", mint) { sendCurrent() }
        actionCard.addView(sendButton)
        val actionRow = horizontalRow()
        actionRow.addView(button("延时发送…", purple) { showScheduleDialog() }, weightParams())
        actionRow.addView(gap(8, horizontal = true))
        actionRow.addView(button("取消待发送", Color.rgb(54, 60, 76)) { cancelScheduled() }, weightParams())
        actionCard.addView(actionRow, space(0, 9, 0, 0))
        val settingsButton = button("打开应用通知设置", Color.rgb(47, 55, 75)) { openNotificationSettings() }
        actionCard.addView(settingsButton, space(0, 9, 0, 0))
        page.addView(actionCard)
        page.addView(gap(14))
        page.addView(label("模板字段基于 HyperOS 焦点通知与超级岛公开结构。系统白名单由设备厂商控制，本工具不绕过限制。", 11, muted, false))
        setContentView(scroll)
    }

    private fun applyTemplate(index: Int) {
        activeIndex = index.coerceIn(0, (templates.length() - 1).coerceAtLeast(0))
        val item = templates.optJSONObject(activeIndex) ?: JSONObject()
        titleField.setText(item.optString("title", "我的超级岛"))
        frontField.setText(item.optString("frontTitle", "自定义信息"))
        contentField.setText(item.optString("content", "编辑文案并生成 JSON"))
        tickerField.setText(item.optString("ticker", "焦点提醒"))
        generateJson(false)
    }

    private fun updateProtocolButtons() {
        os3Button.background = rounded(if (selectedProtocol >= 3) purple else Color.rgb(47, 55, 75), 14)
        os3Button.setTextColor(if (selectedProtocol >= 3) Color.rgb(16, 18, 27) else Color.WHITE)
        os2Button.background = rounded(if (selectedProtocol == 2) purple else Color.rgb(47, 55, 75), 14)
        os2Button.setTextColor(if (selectedProtocol == 2) Color.rgb(16, 18, 27) else Color.WHITE)
    }

    private fun generateJson(showToast: Boolean) {
        val item = templates.optJSONObject(activeIndex) ?: JSONObject()
        val title = titleField.text.toString().trim().ifEmpty { "我的超级岛" }
        val front = frontField.text.toString().trim().ifEmpty { "焦点提醒" }
        val content = contentField.text.toString().trim()
        val ticker = tickerField.text.toString().trim().ifEmpty { title }
        val color = "#9B8CFF"
        val base = JSONObject()
            .put("type", 1)
            .put("title", title)
            .put("subTitle", front)
            .put("content", content)
            .put("colorTitle", "#FFFFFF")
            .put("colorSubTitle", "#B9B7C9")
            .put("colorContent", "#C1C6D4")
        val island = JSONObject()
            .put("islandProperty", 1)
            .put("islandPriority", 2)
            .put("islandTimeout", 3600)
            .put("needCloseAnimation", true)
            .put("highlightColor", color)
        val icon = item.optString("icon", "miui.focus.pic_small")
        base.put("picFunction", icon)
        val big = JSONObject().put("textInfo", JSONObject()
            .put("frontTitle", front)
            .put("title", title)
            .put("content", content)
            .put("narrowFont", false)
            .put("showHighlightColor", true)
            .put("turnAnim", true))
        big.put("imageTextInfoLeft", JSONObject()
            .put("type", 1)
            .put("picInfo", JSONObject().put("type", 1).put("pic", icon).put("contentDescription", item.optString("name", "焦点岛图标")))
            .put("textInfo", JSONObject().put("frontTitle", front).put("title", title).put("content", content)))
        val component = item.optString("component", "text")
        var timerInfo: JSONObject? = null
        var progressInfo: JSONObject? = null
        var chatInfo: JSONObject? = null
        if (component == "progress") {
            val progress = item.optInt("progress", 0).coerceIn(0, 100)
            progressInfo = JSONObject().put("progress", progress).put("colorProgress", color).put("colorProgressEnd", "#70E1C3")
            big.put("progressTextInfo", JSONObject()
                .put("progressInfo", progressInfo)
                .put("textInfo", JSONObject().put("title", title).put("content", content).put("showHighlightColor", true)))
        } else if (component == "timer") {
            val now = System.currentTimeMillis()
            timerInfo = JSONObject().put("timerType", -1).put("timerWhen", now + 25 * 60 * 1000L).put("timerSystemCurrent", now).put("timerTotal", 25 * 60 * 1000L)
            big.put("sameWidthDigitInfo", JSONObject()
                .put("content", "剩余")
                .put("timerInfo", timerInfo)
                .put("showHighlightColor", true).put("turnAnim", false))
        } else if (component == "chat") {
            chatInfo = JSONObject().put("title", front).put("content", content).put("appIconPkg", packageName)
        }
        island.put("bigIslandArea", big)
        island.put("smallIslandArea", JSONObject().put("picInfo", JSONObject().put("type", 1).put("pic", icon)))
        val param = JSONObject()
            .put("protocol", selectedProtocol)
            .put("business", item.optString("business", "custom"))
            .put("enableFloat", true)
            .put("updatable", true)
            .put("ticker", ticker)
            .put("tickerPic", "miui.focus.pic_ticker")
            .put("timeout", 720)
            .put("showSmallIcon", true)
            .put("isShowNotification", true)
            .put("islandFirstFloat", true)
            .put("reopen", "reopen")
            .put("baseInfo", base)
        if (progressInfo != null && selectedProtocol < 3) param.put("progressInfo", progressInfo)
        if (timerInfo != null) param.put("timerInfo", timerInfo)
        if (chatInfo != null) param.put("chatInfo", chatInfo)
        if (selectedProtocol >= 3) param.put("param_island", island)
        jsonField.setText(JSONObject().put("param_v2", param).toString(2))
        previewTitle.text = title
        previewFront.text = front
        previewContent.text = content
        if (showToast) Toast.makeText(this, "JSON 已生成，可在编辑器中继续调整", Toast.LENGTH_SHORT).show()
    }

    private fun currentPayload(): String? {
        val raw = jsonField.text.toString().trim()
        return try {
            val json = JSONObject(raw)
            if (!json.has("param_v2")) {
                Toast.makeText(this, "JSON 顶层需包含 param_v2", Toast.LENGTH_LONG).show()
                null
            } else raw
        } catch (e: Exception) {
            Toast.makeText(this, "JSON 格式错误：${e.message ?: "无法解析"}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun sendCurrent() {
        val payload = currentPayload() ?: return
        if (!ensureNotificationPermission()) return
        postIslandNotification(this, payload, notificationId)
        Toast.makeText(this, "已提交通知；是否显示超级岛由系统策略决定", Toast.LENGTH_LONG).show()
    }

    private fun showScheduleDialog() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("10")
            hint = "分钟"
        }
        AlertDialog.Builder(this)
            .setTitle("延时发送")
            .setMessage("输入延迟分钟数（1–10080 分钟）。Android 待机策略可能造成少量延迟。")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("安排") { _, _ ->
                val minutes = input.text.toString().toLongOrNull()
                if (minutes == null || minutes !in 1L..10080L) {
                    Toast.makeText(this, "请输入 1 至 10080 的分钟数", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val payload = currentPayload() ?: return@setPositiveButton
                if (!ensureNotificationPermission()) return@setPositiveButton
                schedulePayload(payload, minutes)
            }
            .show()
    }

    private fun schedulePayload(payload: String, minutes: Long) {
        val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
        val oldCode = schedulePrefs.getInt("request_code", -1)
        if (oldCode >= 0) pendingIntent(oldCode, "")?.let { alarm.cancel(it); it.cancel() }
        val code = (System.currentTimeMillis() and 0x7fffffff).toInt().coerceAtLeast(1)
        val pi = pendingIntent(code, payload) ?: return
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minutes * 60_000L, pi)
        schedulePrefs.edit().putInt("request_code", code).apply()
        Toast.makeText(this, "已安排 ${minutes} 分钟后发送", Toast.LENGTH_LONG).show()
    }

    private fun pendingIntent(code: Int, payload: String): PendingIntent? {
        val intent = Intent(this, ScheduledIslandReceiver::class.java).setAction("com.leaf.focusislandplayground.SEND")
        if (payload.isNotEmpty()) intent.putExtra(ScheduledIslandReceiver.EXTRA_JSON, payload)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(this, code, intent, flags)
    }

    private fun cancelScheduled() {
        val code = schedulePrefs.getInt("request_code", -1)
        if (code < 0) {
            Toast.makeText(this, "当前没有已安排的发送", Toast.LENGTH_SHORT).show()
            return
        }
        val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
        pendingIntent(code, "")?.let { alarm.cancel(it); it.cancel() }
        schedulePrefs.edit().remove("request_code").apply()
        Toast.makeText(this, "已取消待发送任务", Toast.LENGTH_SHORT).show()
    }

    private fun ensureNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 80)
            Toast.makeText(this, "请允许通知权限后再次发送", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(notificationChannel, "焦点岛 Playground", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "用于预览和发送超级岛测试通知"
                enableVibration(true)
            })
        }
    }

    private fun importJson() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, 101)
    }

    private fun exportJson() {
        if (currentPayload() == null) return
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "focus-island-template.json")
        }
        startActivityForResult(intent, 102)
    }

    private fun shareJson() {
        val payload = currentPayload() ?: return
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, payload)
        }, "分享超级岛 JSON"))
    }

    @Deprecated("Using document intents for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        try {
            if (requestCode == 101) {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("文件无法读取")
                val json = JSONObject(text)
                if (!json.has("param_v2")) error("JSON 顶层缺少 param_v2")
                jsonField.setText(json.toString(2))
                val p = json.optJSONObject("param_v2")
                selectedProtocol = if ((p?.optInt("protocol", 3) ?: 3) >= 3) 3 else 2
                updateProtocolButtons()
                val base = p?.optJSONObject("baseInfo")
                val island = p?.optJSONObject("param_island")
                val big = island?.optJSONObject("bigIslandArea")
                val textInfo = big?.optJSONObject("textInfo") ?: big?.optJSONObject("progressTextInfo")?.optJSONObject("textInfo")
                titleField.setText(base?.optString("title", textInfo?.optString("title", "导入模板")) ?: "导入模板")
                frontField.setText(textInfo?.optString("frontTitle", base?.optString("subTitle", "自定义信息")) ?: "自定义信息")
                contentField.setText(base?.optString("content", textInfo?.optString("content", "")) ?: "")
                previewTitle.text = titleField.text
                previewFront.text = frontField.text
                previewContent.text = contentField.text
                Toast.makeText(this, "JSON 导入成功", Toast.LENGTH_SHORT).show()
            } else if (requestCode == 102) {
                val payload = currentPayload() ?: return
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) } ?: error("文件无法写入")
                Toast.makeText(this, "JSON 已导出", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "文件操作失败：${e.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        try { startActivity(intent) } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    private fun refreshDeviceStatus() {
        val protocol = try { Settings.System.getInt(contentResolver, "notification_focus_protocol", 0) } catch (_: Exception) { 0 }
        if (protocol == 2 || protocol == 3) selectedProtocol = protocol
        updateProtocolButtons()
        val island = try {
            val c = Class.forName("android.os.SystemProperties")
            c.getDeclaredMethod("getBoolean", String::class.java, Boolean::class.javaPrimitiveType).invoke(null, "persist.sys.feature.island", false) as Boolean
        } catch (_: Exception) { false }
        val notifications = if (Build.VERSION.SDK_INT >= 24) getSystemService(NotificationManager::class.java).areNotificationsEnabled() else true
        statusLabel.text = "Android ${Build.VERSION.RELEASE} · 通知 ${if (notifications) "已开启" else "未开启"}\n焦点协议版本：${if (protocol == 0) "未检测到 / 不可读取" else protocol} · 超级岛特性：${if (island) "已检测到" else "未检测到 / 不可读取"}"
        statusLabel.setTextColor(if (notifications) Color.rgb(211, 221, 240) else Color.rgb(255, 185, 150))
    }

    private fun postIslandNotification(context: Context, raw: String, id: Int) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val parsed = JSONObject(raw)
        val param = parsed.optJSONObject("param_v2") ?: JSONObject()
        val base = param.optJSONObject("baseInfo") ?: JSONObject()
        val title = base.optString("title", "焦点岛测试通知")
        val content = base.optString("content", param.optString("ticker", "Playground 通知"))
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, "focus_island_playground") else Notification.Builder(context)
        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(Notification.BigTextStyle().bigText(content))
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(false)
            .setOngoing(param.optBoolean("updatable", true))
        val extras = makeFocusExtras(context, parsed)
        builder.addExtras(extras)
        manager.notify(id, builder.build())
    }

    private fun cardBox(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(card, 20)
        setPadding(dp(16), dp(15), dp(16), dp(16))
    }

    private fun addField(parent: LinearLayout, caption: String, hintText: String, multiline: Boolean): EditText {
        parent.addView(label(caption, 12, muted, true), space(0, 10, 0, 5))
        val field = EditText(this).apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(105, 116, 138))
            textSize = 15f
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(10, 13, 20), 12)
            hint = hintText
            if (multiline) {
                minLines = 2
                gravity = Gravity.TOP or Gravity.START
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            } else {
                setSingleLine(true)
            }
        }
        parent.addView(field, LinearLayout.LayoutParams(-1, if (multiline) dp(76) else dp(48)))
        return field
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        includeFontPadding = true
    }

    private fun button(text: String, color: Int, action: () -> Unit = {}): Button = Button(this).apply {
        this.text = text
        textSize = 13f
        isAllCaps = false
        setTextColor(if (color == purple || color == mint) Color.rgb(16, 18, 27) else Color.WHITE)
        background = rounded(color, 14)
        minHeight = dp(48)
        setPadding(dp(9), dp(3), dp(9), dp(3))
        setOnClickListener { action() }
    }

    private fun horizontalRow() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun weightParams() = LinearLayout.LayoutParams(0, dp(48), 1f)
    private fun gap(height: Int, horizontal: Boolean = false): View = View(this).apply {
        layoutParams = if (horizontal) LinearLayout.LayoutParams(dp(height), 1) else LinearLayout.LayoutParams(1, dp(height))
    }
    private fun space(left: Int, top: Int, right: Int, bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply {
        setMargins(dp(left), dp(top), dp(right), dp(bottom))
    }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}

private fun makeFocusExtras(context: Context, payload: JSONObject): Bundle {
    val extras = Bundle().apply { putString("miui.focus.param", payload.toString()) }
    val pictures = Bundle()
    val placeholderIcon = Icon.createWithResource(context, R.drawable.ic_launcher)
    fun collect(value: Any?) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) collect(value.opt(keys.next()))
            }
            is JSONArray -> for (i in 0 until value.length()) collect(value.opt(i))
            is String -> if (value.startsWith("miui.focus.pic_")) pictures.putParcelable(value, placeholderIcon)
        }
    }
    collect(payload)
    if (!pictures.containsKey("miui.focus.pic_ticker")) pictures.putParcelable("miui.focus.pic_ticker", placeholderIcon)
    if (pictures.size() > 0) extras.putBundle("miui.focus.pics", pictures)
    return extras
}

class ScheduledIslandReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val raw = intent.getStringExtra(EXTRA_JSON) ?: return
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val channel = NotificationChannel("focus_island_playground", "焦点岛 Playground", NotificationManager.IMPORTANCE_HIGH)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        try {
            val id = (System.currentTimeMillis() and 0x7fffffff).toInt()
            val parsed = JSONObject(raw)
            val param = parsed.optJSONObject("param_v2") ?: JSONObject()
            val base = param.optJSONObject("baseInfo") ?: JSONObject()
            val title = base.optString("title", "焦点岛测试通知")
            val content = base.optString("content", param.optString("ticker", "Playground 通知"))
            val notification = Notification.Builder(context, "focus_island_playground")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(Notification.BigTextStyle().bigText(content))
                .setAutoCancel(true)
                .setOngoing(false)
                .addExtras(makeFocusExtras(context, parsed))
                .build()
            context.getSystemService(NotificationManager::class.java).notify(id, notification)
            context.getSharedPreferences("playground", Context.MODE_PRIVATE).edit().remove("request_code").apply()
        } catch (_: Exception) { }
    }
    companion object { const val EXTRA_JSON = "island_json" }
}

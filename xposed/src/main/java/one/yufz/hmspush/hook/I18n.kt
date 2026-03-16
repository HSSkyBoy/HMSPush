package one.yufz.hmspush.hook

import android.content.Context
import java.util.Locale

sealed interface I18n {
    companion object {
        fun get(context: Context): I18n {
            val locale = context.resources.configuration.locales.get(0)
            return when (locale.language) {
                Locale.CHINESE.language -> {
                    when (locale.country) {
                        "TW", "HK", "MO" -> tc
                        else -> sc
                    }
                }
                else -> en
            }
        }
    }

    val hmsCoreRunning: String
    val hmsCoreRunningState: String
    val dummyFragmentDesc: String
    val tipsOptimizeBattery: String
}

object sc : I18n {
    override val hmsCoreRunning = "HMS Core 正在后台运行"
    override val hmsCoreRunningState = "HMS Core 运行状态"
    override val dummyFragmentDesc = "这是一个空白页面，你可以将该页面在最近任务中锁定，以帮助 HMS Core 保持后台运行"
    override val tipsOptimizeBattery = "建议对 HMS Core 关闭电池优化，以帮助 HMS Core 保持后台运行"
}

object tc : I18n {
    override val hmsCoreRunning = "HMS Core 正在背景執行"
    override val hmsCoreRunningState = "HMS Core 執行狀態"
    override val dummyFragmentDesc = "這是一個空白頁面，您可以將此頁面在最近任務中鎖定，以幫助 HMS Core 保持在背景執行"
    override val tipsOptimizeBattery = "建議對 HMS Core 關閉電池最佳化，以幫助 HMS Core 保持在背景執行"
}

object en : I18n {
    override val hmsCoreRunning = "HMS Core is running in the background"
    override val hmsCoreRunningState = "HMS Core Running State"
    override val dummyFragmentDesc = "This is a blank page, you can lock this page in recent tasks to help HMS Core keep running in the background"
    override val tipsOptimizeBattery = "It is recommended to turn off battery optimization for HMS Core to help keep it running in the background"
}

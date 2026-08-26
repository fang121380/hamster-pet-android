package com.andrew.hamsterpet

object ChatResponder {
    fun replyFor(message: String, babyCount: Int = 0): String {
        val normalized = message.trim()
        if (normalized.isEmpty()) return "我在这里，慢慢说。"
        return when {
            normalized.contains("饿") || normalized.contains("吃") || normalized.contains("喂") ->
                "想吃一点点心，瓜子和苹果都喜欢。"
            normalized.contains("困") || normalized.contains("睡") || normalized.contains("累") ->
                "那就一起回小窝休息一会儿，我会安静陪着你。"
            normalized.contains("窝") || normalized.contains("家") ->
                "每次认真照料都会积累建设进度，小窝会越来越舒服。"
            normalized.contains("宝宝") || normalized.contains("幼崽") || normalized.contains("孩子") ->
                if (babyCount > 0) "家里现在有 $babyCount 只幼崽，我会认真照顾它们。"
                else "先把小窝升到二级，再提高亲密度，就能迎接幼崽啦。"
            normalized.contains("开心") || normalized.contains("高兴") ->
                "听见这句话，我的腮帮子都跟着鼓起来了。"
            normalized.contains("难过") || normalized.contains("烦") || normalized.contains("不开心") ->
                "靠近一点吧。今天不用急着变好，我会陪着你。"
            normalized.contains("你好") || normalized.contains("嗨") ->
                "你好呀，我一直在这里等你。"
            else -> listOf(
                "我在认真听。",
                "摸摸头，会把今天的好运分给你。",
                "等会儿要不要一起去小窝看看？",
            )[normalized.hashCode().and(Int.MAX_VALUE) % 3]
        }
    }
}

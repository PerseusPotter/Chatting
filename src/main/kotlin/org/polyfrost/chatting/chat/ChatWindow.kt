package org.polyfrost.chatting.chat

import club.sk1er.patcher.config.PatcherConfig
import net.minecraft.client.gui.ChatLine
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.GuiNewChat
import net.minecraft.util.ChatComponentText
import org.polyfrost.chatting.Chatting
import org.polyfrost.chatting.config.ChattingConfig
import org.polyfrost.chatting.utils.ModCompatHooks
import org.polyfrost.oneconfig.api.config.v1.annotations.Button
import org.polyfrost.oneconfig.api.config.v1.annotations.Slider
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.utils.v1.dsl.mc
import org.polyfrost.polyui.color.mutable
import org.polyfrost.polyui.color.rgba
import org.polyfrost.polyui.component.impl.Block
import org.polyfrost.polyui.unit.Vec2
import org.polyfrost.polyui.unit.Vec4
import net.minecraft.client.renderer.GlStateManager as GL

class ChatWindow : Hud<Block>() {

    override fun defaultPosition() = Vec2(2f, 1080f - 27f - 45f - 12f)

    override fun hasBackground() = false

    private val exampleList: List<ChatLine> = listOf(
        ChatLine(0, ChatComponentText("§bChatting"), 0),
        ChatLine(0, ChatComponentText(""), 0),
        ChatLine(0, ChatComponentText("§aThis is a movable chat"), 0),
        ChatLine(0, ChatComponentText("§eDrag me around!"), 0),
        ChatLine(0, ChatComponentText("Click to drag"), 0)
    )

    var isGuiIngame = false

    var wasInChatGui = false

    var normalScale = 1f
    var lastChatGuiScale = -1f
    var transferOverScale = false

    @Switch(
        title = "Custom Chat Height",
        description = "Set a custom height for the chat window. Allows for more customization than the vanilla chat height options."
    )
    var customChatHeight = false

    @Slider(
        min = 20F, max = 2160F, title = "Focused Height (px)",
        description = "The height of the chat window when focused."
    )
    var focusedHeight = 180

    @Slider(
        min = 20F, max = 2160F, title = "Unfocused Height (px)",
        description = "The height of the chat window when unfocused."
    )
    var unfocusedHeight = 90

    @Switch(
        title = "Custom Chat Width",
        description = "Set a custom width for the chat window. Allows for more customization than the vanilla chat width options."
    )
    var customChatWidth = false

    @Slider(
        min = 20F, max = 2160F, title = "Custom Width (px)",
        description = "The width of the chat window when focused."
    )
    var customWidth = 320

    @Switch(
        title = "Different Opacity When Open",
        description = "Change the opacity of the chat window when it is open."
    )
    var useDifferentOpenOpacity = false

    @Slider(
        min = 0F, max = 255F, title = "Open Background Opacity",
        description = "The opacity of the chat window when it is open."
    )
    var openOpacity = 120

    @Slider(
        min = 0F, max = 255F, title = "Open Border Opacity",
        description = "The opacity of the chat window border when it is open."
    )
    var openBorderOpacity = 255

    @Button(
        title = "Revert to Vanilla Chat Window",
        description = "Revert the chat window to the vanilla chat window, instead of the Chattings custom chat window.",
        text = "Revert"
    )
    val revertToVanilla = Runnable {
        if (isReal) {
            get().padding = Vec4.ZERO
            get().radii = floatArrayOf(0f)
        }
        ChattingConfig.smoothBG = false
    }

    @Button(
        title = "Revert to Chatting Chat Window",
        description = "Revert the chat window to the Chatting custom chat window, instead of the vanilla chat window.",
        text = "Revert"
    )
    val revertToChatting = Runnable {
        if (isReal) {
            get().padding = Vec4.of(5f, 5f, 5f, 5f)
            get().radii = floatArrayOf(5f)
        }
        ChattingConfig.smoothBG = true
    }

    fun renderExample(x: Float, y: Float, scaleX: Float, scaleY: Float) {
        if (!isReal) return
        GL.pushMatrix()
        GL.translate(x, y + scaleY, 0f)
        GL.scale(scaleX, scaleY, 1f)
        for (chat in exampleList) {
            ModCompatHooks.redirectDrawString(chat.chatComponent.formattedText, 0f, 0f, -1, chat)
            GL.translate(0f, 9f, 0f)
        }
        GL.popMatrix()
    }

    fun drawBackground(x: Float, y: Float, width: Float, height: Float, scale: Float) {
        if (Chatting.isPatcher && PatcherConfig.transparentChat) return
//        val animatingOpacity = wasInChatGui && (ChattingConfig.smoothBG && (previousAnimationWidth != width || previousAnimationHeight != height))
//        wasInChatGui = mc.currentScreen is GuiChat || animatingOpacity
//        previousAnimationWidth = width
//        previousAnimationHeight = height
//        val bgOpacity = openOpacity
//        val borderOpacity = openBorderOpacity
//        val tempBgAlpha = bgColor.alpha
//        val tempBorderAlpha = borderColor.alpha
//        bgColor.alpha = if (useDifferentOpenOpacity && wasInChatGui) bgOpacity else bgColor.alpha
//        borderColor.alpha = if (useDifferentOpenOpacity && wasInChatGui) borderOpacity else borderColor.alpha
//        super.drawBackground(x, y, width, height, scale)
//        bgColor.alpha = tempBgAlpha
//        borderColor.alpha = tempBorderAlpha
    }

    fun drawBG() {
//        animationWidth = widthAnimation.get()
//        animationHeight = heightAnimation.get()
//        width = position.width + (if (mc.ingameGUI.chatGUI.chatOpen && !Chatting.peeking && ChattingConfig.extendBG) ModCompatHooks.chatButtonOffset else 0) * scale
//        val heightEnd = if (height == 0) 0f else (height + paddingY * 2f) * scale
//        val duration = ChattingConfig.bgDuration
//        GL.enableAlpha()
//        GL.enableBlend()
//        if (width != widthAnimation.end) {
//            if (ChattingConfig.smoothBG) {
//                widthAnimation = EaseOutQuart(duration, animationWidth, width, false)
//            } else {
//                animationWidth = width
//            }
//        }
//        if (heightEnd != heightAnimation.end) {
//            if (ChattingConfig.smoothBG) {
//                heightAnimation = EaseOutQuart(duration, animationHeight, heightEnd, false)
//            } else {
//                animationHeight = heightEnd
//            }
//        }
//        if (animationHeight <= 0.3f || !background || HudCore.editing) return
//        nanoVG(true) {
//            val scale = UResolution.scaleFactor.toFloat()
//            drawBackground(position.x, position.bottomY - animationHeight + (if (UResolution.windowHeight % 2 == 1) scale - 1 else 0f) / scale, animationWidth, animationHeight, this@ChatWindow.scale)
//        }
//        GL.disableAlpha()
    }

    fun canShow(): Boolean {
        return true
//        showInChat = true
//        return isEnabled && (shouldShow() || Platform.getGuiPlatform().isInChat) && (isGuiIngame xor isCachingIgnored)
    }

    override fun category(): Category {
        TODO("Not yet implemented")
    }

    override fun create() = object : Block(color = rgba(0, 0, 0, 120f / 255f).mutable()) {
        override var width: Float
            get() = ((if (customChatWidth) Chatting.getChatWidth() else GuiNewChat.calculateChatboxWidth(mc.gameSettings.chatWidth)) + 4 + ModCompatHooks.chatHeadOffset) * get().scaleX
            set(value) {}
        override var height: Float
            get() = 9f * 5f * get().scaleY
            set(value) {}

        override fun render() {
            super.render()
            if(!isReal) renderExample(x, y, scaleX, scaleY)
        }
    }

    var backgroundOpacity: Float
        get() = get().color.alpha
        set(value) {
            get().color = get().color.mutable().also { it.alpha = value }
        }

    fun updateMCChatScale() {
        if (lastChatGuiScale != mc.gameSettings.chatScale) {
            lastChatGuiScale = mc.gameSettings.chatScale
//            scale = normalScale * mc.gameSettings.chatScale
        }
    }

    override fun multipleInstancesAllowed() = false
    override fun title() = "Chat Window"

    override fun update() = false

}
package org.polyfrost.chatting

import dev.deftu.omnicore.client.OmniDesktop
import net.minecraft.client.gui.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.settings.KeyBinding
import net.minecraft.client.shader.Framebuffer
import net.minecraftforge.common.MinecraftForge.EVENT_BUS
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.polyfrost.chatting.chat.*
import org.polyfrost.chatting.chat.ChatScrollingHook.shouldSmooth
import org.polyfrost.chatting.config.ChattingConfig
import org.polyfrost.chatting.hook.ChatLineHook
import org.polyfrost.chatting.mixin.GuiNewChatAccessor
import org.polyfrost.chatting.utils.ModCompatHooks
import org.polyfrost.chatting.utils.copyToClipboard
import org.polyfrost.chatting.utils.createBindFramebuffer
import org.polyfrost.chatting.utils.screenshot
import org.polyfrost.oneconfig.api.commands.v1.CommandManager
import org.polyfrost.oneconfig.api.ui.v1.Notifications
import org.polyfrost.oneconfig.utils.v1.dsl.mc
import org.polyfrost.oneconfig.utils.v1.dsl.openUI
import org.polyfrost.polyui.component.extensions.onClick
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*


@Mod(
    modid = Chatting.ID,
    name = Chatting.NAME,
    version = Chatting.VER,
    modLanguageAdapter = "cc.polyfrost.oneconfig.utils.KotlinLanguageAdapter"
)
object Chatting {

    val keybind = KeyBinding("Screenshot Chat", Keyboard.KEY_NONE, "Chatting")
    const val NAME = "@NAME@"
    const val VER = "@VER@"
    const val ID = "@ID@"
    var doTheThing = false
    var isPatcher = false
        private set
    var isBetterChat = false
        private set
    var isSkytils = false
        private set
    var isHychat = false
        private set

    var chatWindow = ChatWindow()

    private var lastPressed = false;
    var peeking = false
        get() = ChattingConfig.chatPeek && field

    private val fileFormatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH.mm.ss'.png'")

    val oldModDir = Paths.get("W-OVERFLOW", NAME)

    @Mod.EventHandler
    fun onInitialization(event: FMLInitializationEvent) {
        ChattingConfig
        if (!ChattingConfig.enabled) {
            ChattingConfig.enabled = true
            ChattingConfig.save()
        }
        if (!chatWindow.transferOverScale) {
            chatWindow.normalScale = chatWindow.scale
            chatWindow.transferOverScale = true
            ChattingConfig.save()
        }
        chatWindow.updateMCChatScale()
        CommandManager.register(CommandManager.literal("chatting").executes { ChattingConfig.openUI(); 1 })
        ClientRegistry.registerKeyBinding(keybind)
        EVENT_BUS.register(this)
        EVENT_BUS.register(ChatSpamBlock)
        ChatTabs.initialize()
        ChatShortcuts.initialize()
    }

    @Mod.EventHandler
    fun onPostInitialization(event: FMLPostInitializationEvent) {
        isPatcher = Loader.isModLoaded("patcher")
        isBetterChat = Loader.isModLoaded("betterchat")
        isSkytils = Loader.isModLoaded("skytils")
        isHychat = Loader.isModLoaded("hychat")
    }

    @Mod.EventHandler
    fun onForgeLoad(event: FMLLoadCompleteEvent) {
        if (ChattingConfig.informForAlternatives) {
            if (isHychat) {
                Notifications.enqueue(
                    Notifications.Type.Info,
                    NAME,
                    "Hychat can be removed as it is replaced by Chatting. Click here for more information.",
                ).onClick { OmniDesktop.browse(URI("https://microcontrollersdev.github.io/Alternatives/1.8.9/hychat")) }
            }
            if (isSkytils) {
                try {
                    skytilsCompat(Class.forName("gg.skytils.skytilsmod.core.Config"))
                } catch (e: Exception) {
                    e.printStackTrace()
                    try {
                        skytilsCompat(Class.forName("skytils.skytilsmod.core.Config"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (isBetterChat) {
                Notifications.enqueue(
                    Notifications.Type.Info,
                    NAME,
                    "BetterChat can be removed as it is replaced by Chatting. Click here to open your mods folder to delete the BetterChat file.",
                ).onClick {
                    OmniDesktop.open(File("./mods"))
                }
            }
        }
    }

    private fun skytilsCompat(skytilsClass: Class<*>) {
        val instance = skytilsClass.getDeclaredField("INSTANCE").get(null)
        val chatTabs = skytilsClass.getDeclaredField("chatTabs")
        chatTabs.isAccessible = true
        if (chatTabs.getBoolean(instance)) {
            Notifications.enqueue(
                Notifications.Type.Info,
                NAME,
                "Skytils' chat tabs can be disabled as it is replace by Chatting.\nClick here to automatically do this.",
            ).onClick {
                chatTabs.setBoolean(instance, false)
                ChattingConfig.chatTabs = true
                ChattingConfig.hypixelOnlyChatTabs = true
                ChattingConfig.save()
                skytilsClass.getMethod("markDirty").invoke(instance)
                skytilsClass.getMethod("writeData").invoke(instance)
                false
            }
        }
        val copyChat = skytilsClass.getDeclaredField("copyChat")
        copyChat.isAccessible = true
        if (copyChat.getBoolean(instance)) {
            Notifications.enqueue(
                Notifications.Type.Info,
                NAME,
                "Skytils' copy chat messages can be disabled as it is replace by Chatting.\nClick here to automatically do this.",
            ).onClick {
                copyChat.setBoolean(instance, false)
                skytilsClass.getMethod("markDirty").invoke(instance)
                skytilsClass.getMethod("writeData").invoke(instance)
                false
            }
        }
    }

    @SubscribeEvent
    fun onTickEvent(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START && mc.theWorld != null && mc.thePlayer != null && (mc.currentScreen == null || mc.currentScreen is GuiChat)) {
            if (doTheThing) {
                screenshotChat()
                doTheThing = false
            }

            if (mc.currentScreen is GuiChat) peeking = false

            if (peeking && ChattingConfig.peekScrolling) {
                var i = Mouse.getDWheel().coerceIn(-1..1)

                if (i != 0) {

                    if (!GuiScreen.isShiftKeyDown()) {
                        i *= 7
                    }

                    shouldSmooth = true
                    mc.ingameGUI.chatGUI.scroll(i)
                }
            }
        }
    }

    @SubscribeEvent
    fun peek(e: KeyInputEvent) {
        val key = ChattingConfig.chatPeekBind
        if (key.isActive != lastPressed && ChattingConfig.chatPeek) {
            lastPressed = key.isActive
            if (key.isActive) {
                peeking = !peeking
            } else if (!ChattingConfig.peekMode) {
                peeking = false
            }
            if (!peeking) mc.ingameGUI.chatGUI.resetScroll()
        }
    }

    fun getChatHeight(opened: Boolean): Int {
        return if (opened) chatWindow.focusedHeight else chatWindow.unfocusedHeight
    }

    fun getChatWidth(): Int {
        return chatWindow.customWidth
    }

    fun screenshotLine(line: ChatLine): BufferedImage? {
        return screenshot(
            linkedMapOf<ChatLine, String>().also { map ->
                val fullMessage = (line as ChatLineHook).`chatting$getFullMessage`()
                for (chatLine in (mc.ingameGUI.chatGUI as GuiNewChatAccessor).drawnChatLines) {
                    if ((chatLine as ChatLineHook).`chatting$getFullMessage`() == fullMessage) {
                        map[chatLine] = chatLine.chatComponent.formattedText
                    }
                }
            }
        )
    }

    private fun screenshotChat() {
        screenshotChat(0)
    }

    fun screenshotChat(scrollPos: Int) {
        val hud = mc.ingameGUI
        val chat = hud.chatGUI
        val chatLines = LinkedHashMap<ChatLine, String>()
        ChatSearchingManager.filterMessages(
            ChatSearchingManager.lastSearch,
            (chat as GuiNewChatAccessor).drawnChatLines
        )?.let { drawnLines ->
            val chatHeight =
                if (chatWindow.customChatHeight) getChatHeight(true) / 9 else GuiNewChat.calculateChatboxHeight(
                    mc.gameSettings.chatHeightFocused / 9
                )
            for (i in scrollPos until drawnLines.size.coerceAtMost(scrollPos + chatHeight)) {
                chatLines[drawnLines[i]] = drawnLines[i].chatComponent.formattedText
            }

            screenshot(chatLines)?.copyToClipboard()
        }
    }

    private fun screenshot(messages: HashMap<ChatLine, String>): BufferedImage? {
        if (messages.isEmpty()) {
            Notifications.enqueue(Notifications.Type.Warning, "Chatting", "Chat window is empty.")
            return null
        }
        if (!OpenGlHelper.isFramebufferEnabled()) {
            Notifications.enqueue(
                Notifications.Type.Error,
                "Chatting",
                "Screenshot failed, please disable “Fast Render” in OptiFine’s “Performance” tab."
            )
            return null
        }

        val fr: FontRenderer = ModCompatHooks.fontRenderer
        val width = messages.maxOf { fr.getStringWidth(it.value) + (if (ChattingConfig.showChatHeads && ((it.key as ChatLineHook).`chatting$hasDetected`() || ChattingConfig.offsetNonPlayerMessages)) 10 else 0) } + 4
        val fb: Framebuffer = createBindFramebuffer(width * 2, (messages.size * 9) * 2)
        val file = File(mc.mcDataDir, "screenshots/chat/" + fileFormatter.format(Date()))

        GlStateManager.scale(2f, 2f, 1f)
        messages.entries.forEachIndexed { i: Int, entry: MutableMap.MutableEntry<ChatLine, String> ->
            ModCompatHooks.redirectDrawString(entry.value, 0f, (messages.size - 1 - i) * 9f, 0xffffff, entry.key, true)
        }

        val image = fb.screenshot(file)
        mc.entityRenderer.setupOverlayRendering()
        mc.framebuffer.bindFramebuffer(true)
        Notifications.enqueue(
            Notifications.Type.Info,
            "Chatting",
            "Chat screenshotted successfully." + (if (ChattingConfig.copyMode != 1) "\nClick to open." else ""),
        ).onClick {
            if (!OmniDesktop.open(file)) {
                Notifications.enqueue(Notifications.Type.Error, "Chatting", "Could not browse!")
            }
        }
        return image
    }
}

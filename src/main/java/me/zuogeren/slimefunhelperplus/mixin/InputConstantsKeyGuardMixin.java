package me.zuogeren.slimefunhelperplus.mixin;

import com.mojang.blaze3d.platform.InputConstants;
//#if MC >= 260300
//$$ import java.nio.ByteBuffer;
//$$ import org.lwjgl.sdl.SDLKeyboard;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif
import org.spongepowered.asm.mixin.Mixin;

/**
 * 兜底拦截 26.3（SDL）下传入 {@link InputConstants#isKeyDown(int)} 的越界键码（如 {@code -1}）。
 *
 * <p>26.3 将未绑定键 {@code InputConstants.UNKNOWN} 从 GLFW 时代的 {@code -1} 改成了 {@code 0}，
 * {@code KeyMapping#shouldSetOnIngameFocus()} 只跳过 {@code 0}；若有模组仍用 {@code -1}（或旧键码）
 * 注册按键，{@code MouseHandler#grabMouse()} 调 {@code KeyMapping.setAll()} 时会在
 * {@code SDLKeyboard.SDL_GetKeyboardState().get(key)} 抛出 {@code IndexOutOfBoundsException}，
 * 导致鼠标无法捕获甚至直接崩溃。
 */
@Mixin(InputConstants.class)
public abstract class InputConstantsKeyGuardMixin {

    //#if MC >= 260300
    //$$ @Inject(method = "isKeyDown(I)Z", at = @At("HEAD"), cancellable = true, require = 0)
    //$$ private static void sfhplus$guardInvalidScancode(int key, CallbackInfoReturnable<Boolean> cir) {
    //$$     ByteBuffer keyboardState = SDLKeyboard.SDL_GetKeyboardState();
    //$$     if (keyboardState == null || key < 0 || key >= keyboardState.limit()) {
    //$$         cir.setReturnValue(false);
    //$$     }
    //$$ }
    //#endif
}

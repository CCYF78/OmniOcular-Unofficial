package me.exz.omniocular.mixins;

import java.util.ArrayList;
import java.util.regex.Matcher;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.angelica.client.font.BatchingFontRenderer;
import com.gtnewhorizons.angelica.mixins.interfaces.FontRendererAccessor;

import mcp.mobius.waila.api.IWailaTooltipRenderer;
import mcp.mobius.waila.api.SpecialChars;
import mcp.mobius.waila.api.impl.DataAccessorCommon;
import mcp.mobius.waila.api.impl.ModuleRegistrar;
import mcp.mobius.waila.overlay.DisplayUtil;
import mcp.mobius.waila.overlay.Tooltip;

/**
 * Makes Waila's right-aligned columns and tooltip sizing use the same glyph metrics as the
 * actual text rendering, and makes the alignment offset ignore non-rendering structural markers.
 *
 * Three patched defects against Waila 1.19.34's Tooltip (originally developed as the standalone
 * WailaAlignFix mod, merged here):
 * 1. column gap: last column is widened by maxStringW minus the summed column widths;
 * 2. segment overcount: the ALIGNRIGHT/ALIGNCENTER offset measures only the current
 * patternLineSplit segment instead of the whole column remainder;
 * 3. renderer-metric mismatch: with Angelica's font renderer active, text is measured through
 * BatchingFontRenderer.measure (the renderer's own stepper), because the vanilla width lookup
 * omits the per-glyph glyphSpacing that rendering adds.
 */
@Mixin(value = Tooltip.class, remap = false)
public abstract class MixinTooltip {

    @Shadow(remap = false)
    ArrayList<ArrayList<String>> lines;

    @Shadow(remap = false)
    ArrayList<ArrayList<Integer>> sizes;

    @Shadow(remap = false)
    ArrayList<Integer> columnsWidth;

    @Shadow(remap = false)
    ArrayList<Integer> columnsPos;

    @Shadow(remap = false)
    int maxStringW;

    @Shadow(remap = false)
    public static int TabSpacing;

    /** Cached once; the Angelica mixin interface only exists when Angelica is installed. */
    private static boolean omniocular$angelica;

    static {
        try {
            Class.forName("com.gtnewhorizons.angelica.mixins.interfaces.FontRendererAccessor");
            omniocular$angelica = true;
        } catch (ClassNotFoundException ignored) {
            omniocular$angelica = false;
        }
    }

    @Inject(method = "computeRenderables()V", at = @At("HEAD"), require = 0, remap = false)
    private void omniocular$stretchLastColumn(CallbackInfo ci) {
        final int n = this.columnsWidth.size();
        if (n < 2) return;

        int sum = 0;
        for (int i = 0; i < n; i++) sum += this.columnsWidth.get(i);
        sum += TabSpacing * (n - 1);

        final int gap = this.maxStringW - sum;
        if (gap > 0) {
            this.columnsWidth.set(n - 1, this.columnsWidth.get(n - 1) + gap);
        }
    }

    /**
     * Tooltip constructor: per-column sizes and the full-line width. Both are whole strings (no
     * segment scoping), measured with renderer metrics so the box and the column widths match
     * what is actually drawn.
     */
    @Redirect(
        method = "<init>(Ljava/util/List;Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lmcp/mobius/waila/overlay/DisplayUtil;getDisplayWidth(Ljava/lang/String;)I"),
        require = 0,
        remap = false)
    private int omniocular$measureWholeString(String s) {
        return omniocular$displayWidth(s);
    }

    /**
     * computeRenderables: both call sites feed the remainder of the column string starting at the
     * current segment; only the first patternLineSplit match of that string (the segment being
     * placed) actually renders from this offset, so measure only that segment.
     */
    @Redirect(
        method = "computeRenderables()V",
        at = @At(
            value = "INVOKE",
            target = "Lmcp/mobius/waila/overlay/DisplayUtil;getDisplayWidth(Ljava/lang/String;)I"),
        require = 0,
        remap = false)
    private int omniocular$measureCurrentSegmentOnly(String s) {
        final Matcher m = SpecialChars.patternLineSplit.matcher(s);
        final String segment = m.find() ? m.group() : s;
        // Literal "\n" (backslash + n) is a line-break marker, not rendered text; real newlines
        // are zero-width already, stripping them here too is harmless.
        return omniocular$displayWidth(
            segment.replace("\\n", "")
                .replace("\n", ""));
    }

    /**
     * Re-implementation of {@link DisplayUtil#getDisplayWidth} with the text term measured by the
     * renderer's own metrics when Angelica is present. Tooltip-renderer and icon size
     * contributions are kept identical to vanilla so sized rows (progress bars, item icons) do
     * not regress.
     */
    private static int omniocular$displayWidth(String s) {
        if (s == null || s.isEmpty()) return 0;

        int width = 0;
        Matcher renderMatcher = SpecialChars.patternRender.matcher(s);
        while (renderMatcher.find()) {
            IWailaTooltipRenderer renderer = ModuleRegistrar.instance()
                .getTooltipRenderer(renderMatcher.group("name"));
            if (renderer != null) {
                width += renderer.getSize(
                    renderMatcher.group("args")
                        .split(SpecialChars.WailaRendererComma),
                    DataAccessorCommon.instance).width;
            }
        }
        Matcher iconMatcher = SpecialChars.patternIcon.matcher(s);
        while (iconMatcher.find()) {
            width += 8;
        }

        // Keep MC style codes: the renderer applies bold (+1 px per glyph) from them, so the
        // measurement must see them too. Strip only Waila structural markers and line breaks.
        final String stripped = DisplayUtil.stripWailaSymbols(s)
            .replace("\\n", "")
            .replace("\n", "");
        return width + omniocular$textWidth(stripped);
    }

    private static int omniocular$textWidth(String stripped) {
        final FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        if (!omniocular$angelica || !(font instanceof FontRendererAccessor)) {
            return font.getStringWidth(stripped);
        }
        final BatchingFontRenderer batcher = ((FontRendererAccessor) font).angelica$getBatcher();
        if (batcher == null) {
            return font.getStringWidth(stripped);
        }
        return (int) Math.ceil(batcher.measure(stripped, 0, stripped.length(), font.getUnicodeFlag()));
    }
}

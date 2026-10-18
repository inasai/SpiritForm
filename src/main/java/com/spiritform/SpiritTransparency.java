package com.spiritform;

public class SpiritTransparency {

    /** Чи малюється зараз гравець у формі Привида (виставляє Mixin перед кожним рендером). */
    public static boolean RENDERING_SPIRIT_PLAYER = false;

    /**
     * Колір моделі у форматі ARGB. Перші два символи після 0x - прозорість:
     * FF непрозоре, 80 приблизно наполовину, 00 повністю невидиме.
     */
    public static final int GHOST_COLOR = 0x99FFFFFF;

    private SpiritTransparency() {
    }
}

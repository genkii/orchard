package de.minehackers.orchard.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/// Info screen for Mod Menu pointing at config files and server commands.
public final class OrchardConfigScreen extends Screen {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFA8A8A8;

    private static final String[] BODY_KEYS = {
        "orchard.config.about",
        "orchard.config.location"
    };
    private static final String[] COMMAND_KEYS = {
        "orchard.config.command.packs",
        "orchard.config.command.create",
        "orchard.config.command.reload"
    };

    private final Screen parent;

    public OrchardConfigScreen(Screen parent) {
        super(Component.translatable("orchard.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("orchard.config.done"),
                        button -> onClose())
                .bounds(width / 2 - 100, height - 32, 200, Button.DEFAULT_HEIGHT)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.centeredText(font, title, width / 2, 30, WHITE);

        int y = 55;
        for (String key : BODY_KEYS) {
            graphics.centeredText(font, Component.translatable(key), width / 2, y, WHITE);
            y += 12;
        }

        y += 10;
        graphics.centeredText(font, Component.translatable("orchard.config.header.commands"),
                width / 2, y, WHITE);
        y += 14;
        for (String key : COMMAND_KEYS) {
            graphics.centeredText(font, Component.translatable(key), width / 2, y, GREY);
            y += 12;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}

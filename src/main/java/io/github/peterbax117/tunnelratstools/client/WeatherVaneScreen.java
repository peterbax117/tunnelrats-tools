package io.github.peterbax117.tunnelratstools.client;

import io.github.peterbax117.tunnelratstools.TunnelRatsTools;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherEffect;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneMenu;
import io.github.peterbax117.tunnelratstools.weathervane.WeatherVaneStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Weather Vane GUI: effect buttons, Sky Shard slot, energy bar, Activate button and a status line. */
public class WeatherVaneScreen extends AbstractContainerScreen<WeatherVaneMenu>
{
    private static final ResourceLocation TEXTURE = TunnelRatsTools.loc("textures/gui/container/weather_vane.png");
    private static final int BUTTON_WIDTH = 41, WEATHER_BUTTON_WIDTH = 54, BUTTON_HEIGHT = 16, BUTTON_X = 6;
    private static final int TIME_ROW_Y = 18, WEATHER_ROW_Y = 37;
    private static final int BAR_X = 30, BAR_Y = 60, BAR_WIDTH = 94, BAR_HEIGHT = 14;
    private static final int ACTIVATE_X = 129, ACTIVATE_Y = 57, ACTIVATE_WIDTH = 41, ACTIVATE_HEIGHT = 20;
    private static final int TEXT_COLOR = 0x404040, READY_COLOR = 0x1E6B1E, BLOCKED_COLOR = 0xA02020;

    private final List<Button> effectButtons = new ArrayList<>();
    private Button activateButton;

    public WeatherVaneScreen(WeatherVaneMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 196;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        effectButtons.clear();
        int timeIndex = 0, weatherIndex = 0;
        for(WeatherEffect effect : WeatherEffect.values()) {
            // Four time buttons, then three wider weather buttons spanning the same row width.
            int column = effect.isTime() ? timeIndex++ : weatherIndex++;
            int y = effect.isTime() ? TIME_ROW_Y : WEATHER_ROW_Y;
            int width = effect.isTime() ? BUTTON_WIDTH : WEATHER_BUTTON_WIDTH;
            int x = effect.isTime() ? BUTTON_X + column * BUTTON_WIDTH : BUTTON_X + column * (WEATHER_BUTTON_WIDTH + 1);
            Button button = Button.builder(effect.displayName(), b -> click(effect.ordinal()))
                    .bounds(leftPos + x, topPos + y, width, BUTTON_HEIGHT).build();
            effectButtons.add(addRenderableWidget(button));
        }
        activateButton = addRenderableWidget(Button.builder(Component.translatable("gui.tunnelrats_tools.weather_vane.activate"),
                        b -> click(WeatherVaneMenu.ACTIVATE_BUTTON))
                .bounds(leftPos + ACTIVATE_X, topPos + ACTIVATE_Y, ACTIVATE_WIDTH, ACTIVATE_HEIGHT).build());
        updateButtons();
    }

    private void click(int id) {
        if(minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons() {
        for(WeatherEffect effect : WeatherEffect.values()) {
            Button button = effectButtons.get(effect.ordinal());
            button.active = menu.allows(effect);
            button.setTooltip(button.active ? null : Tooltip.create(Component.translatable("gui.tunnelrats_tools.weather_vane.effect_disabled")));
        }
        WeatherVaneStatus status = menu.status();
        activateButton.active = status == WeatherVaneStatus.READY;
        activateButton.setTooltip(status == WeatherVaneStatus.READY ? null : Tooltip.create(statusText(status)));
    }

    private Component statusText(WeatherVaneStatus status) {
        if(status == WeatherVaneStatus.COOLDOWN) {
            return Component.translatable("status.tunnelrats_tools.weather_vane.cooldown", menu.cooldownSeconds());
        }
        return status.message();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Gold outline around the selected effect.
        Button selected = effectButtons.get(menu.selected().ordinal());
        graphics.renderOutline(selected.getX() - 1, selected.getY() - 1, selected.getWidth() + 2, selected.getHeight() + 2, 0xFFE8B730);

        int capacity = menu.energyCapacity();
        int filled = capacity <= 0 ? 0 : (int) ((long) menu.energy() * BAR_WIDTH / capacity);
        if(filled > 0) {
            graphics.fillGradient(leftPos + BAR_X, topPos + BAR_Y, leftPos + BAR_X + filled, topPos + BAR_Y + BAR_HEIGHT, 0xFF6FD3FF, 0xFF2A7FC0);
        }
        int cost = menu.energyPerUse();
        if(capacity > 0 && cost > 0 && cost <= capacity) {
            // Tick mark at the energy one use costs.
            int x = leftPos + BAR_X + (int) ((long) cost * BAR_WIDTH / capacity);
            graphics.fill(x, topPos + BAR_Y, x + 1, topPos + BAR_Y + BAR_HEIGHT, 0xFFE8B730);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.tunnelrats_tools.weather_vane.selected", menu.selected().displayName()),
                8, 82, TEXT_COLOR, false);
        WeatherVaneStatus status = menu.status();
        graphics.drawString(font, statusText(status), 8, 92, status == WeatherVaneStatus.READY ? READY_COLOR : BLOCKED_COLOR, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if(isHovering(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.tunnelrats_tools.weather_vane.energy", menu.energy(), menu.energyCapacity()));
            if(menu.requiresCatalyst() && menu.energyPerUse() > 0) {
                lines.add(Component.translatable("gui.tunnelrats_tools.weather_vane.energy_cost", menu.energyPerUse()));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }
}

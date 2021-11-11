package net.runelite.client.plugins.gargoylefighter;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import com.openosrs.client.ui.overlay.components.table.TableAlignment;
import com.openosrs.client.ui.overlay.components.table.TableComponent;
import net.runelite.client.util.ColorUtil;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;
import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;

@Slf4j
@Singleton
class GargoyleFighterOverlay extends OverlayPanel
{
    private final Client client;
    private final GargoyleFighterPlugin plugin;
    private final GargoyleFighterConfig config;

    String timeFormat;

    @Inject
    private GargoyleFighterOverlay(final Client client, final GargoyleFighterPlugin plugin, final GargoyleFighterConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.BOTTOM_LEFT);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Gargoyle Overlay"));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {

        if (!config.showOverlay())
        {
            log.debug("Overlay conditions not met, not starting overlay");
            return null;
        }

        TableComponent tableComponent = new TableComponent();
        tableComponent.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT);

        tableComponent.addRow("Status:", plugin.status);

        if (plugin.run) {
            Duration duration = Duration.between(plugin.botTimer, Instant.now());
            timeFormat = (duration.toHours() < 1) ? "mm:ss" : "HH:mm:ss";
            tableComponent.addRow("Time running:", formatDuration(duration.toMillis(), timeFormat));
        } else {
            tableComponent.addRow("Time running:", "00:00");
        }

        if (!tableComponent.isEmpty())
        {
            panelComponent.setBackgroundColor(ColorUtil.fromHex("#B3121212")); //Material Dark default
            panelComponent.setPreferredSize(new Dimension(200, 200));
            panelComponent.setBorder(new Rectangle(5, 5, 5, 5));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Pinq's Gargoyle Fighter")
                    .color(ColorUtil.fromHex("#40C4FF"))
                    .build());
            if (plugin.run) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Active:")
                        .right(String.valueOf(plugin.run))
                        .rightColor(Color.GREEN)
                        .build());
            } else {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Active:")
                        .right(String.valueOf(plugin.run))
                        .rightColor(Color.RED)
                        .build());
            }
            panelComponent.getChildren().add(tableComponent);
        }
        return super.render(graphics);
    }
}
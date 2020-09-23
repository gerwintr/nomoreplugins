package net.runelite.client.plugins.mudrunecrafter;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import static net.runelite.api.MenuOpcode.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

import net.runelite.api.ItemID;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.components.table.TableAlignment;
import net.runelite.client.ui.overlay.components.table.TableComponent;
import net.runelite.client.util.ColorUtil;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

@Slf4j
@Singleton
class MudRunecrafterOverlay extends OverlayPanel
{
    private final Client client;
    private final MudRunecrafterPlugin plugin;
    private final MudRunecrafterConfig config;

    String timeFormat;
    private String infoStatus = "Starting...";
    private int suppliesCost = 155000; // ESTIMATE

    @Inject
    private MudRunecrafterOverlay(final Client client, final MudRunecrafterPlugin plugin, final MudRunecrafterConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.BOTTOM_LEFT);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Runecrafter Overlay"));
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

        //tableComponent.addRow("Active:", String.valueOf(plugin.run));


        tableComponent.addRow("Status:", plugin.status);

        if (plugin.run) {
            Duration duration = Duration.between(plugin.botTimer, Instant.now());
            timeFormat = (duration.toHours() < 1) ? "mm:ss" : "HH:mm:ss";
            tableComponent.addRow("Time running:", formatDuration(duration.toMillis(), timeFormat));
            tableComponent.addRow("Runes crafted:", String.valueOf(plugin.runesCrafted) + " (" + String.valueOf(plugin.getRunesPH() + ")"));
            tableComponent.addRow("Levels gained:", String.valueOf((client.getRealSkillLevel(Skill.RUNECRAFT)-plugin.initialLevel)));
            if (duration.toSeconds() != 0) {
                double hoursIn = duration.toSeconds()*0.000277777778;
                tableComponent.addRow("Gp/hr:", String.valueOf(Math.floor((plugin.mudRunePrice * plugin.runesCrafted / hoursIn)-suppliesCost)));
            } else {
                tableComponent.addRow("Gp/hr:", "N/A");
                tableComponent.addRow("Xp/hr:", "N/A");
            }
        } else {
            tableComponent.addRow("Time running:", "00:00");
            tableComponent.addRow("Runes crafted:", "N/A");
            tableComponent.addRow("Levels gained:", "N/A");
            tableComponent.addRow("Gp/hr:", "N/A");
            tableComponent.addRow("Xp/hr:", "N/A");
        }
        /*
        if (plugin.state != null)
        {
            if (!plugin.state.name().equals("TIMEOUT"))
            {
                infoStatus = plugin.state.name();
            }
        }
        tableComponent.addRow("Status:", infoStatus);
         */
        /*
        TableComponent tableMarksComponent = new TableComponent();
        tableMarksComponent.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT);
        tableMarksComponent.addRow("Collected:", String.valueOf(plugin.mogCollectCount));
        tableMarksComponent.addRow("Per hour:", String.valueOf(plugin.marksPerHour));

        TableComponent tableDelayComponent = new TableComponent();
        tableDelayComponent.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT);

        tableDelayComponent.addRow("Sleep delay:", plugin.sleepLength + "ms");
        tableDelayComponent.addRow("Tick delay:", String.valueOf(plugin.timeout));
        tableDelayComponent.addRow("Alch delay:", String.valueOf(plugin.alchTimeout));
         */

        if (!tableComponent.isEmpty())
        {
            panelComponent.setBackgroundColor(ColorUtil.fromHex("#B3121212")); //Material Dark default
            panelComponent.setPreferredSize(new Dimension(200, 200));
            panelComponent.setBorder(new Rectangle(5, 5, 5, 5));
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Pinq's Runecrafter")
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
            /*
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Marks of grace")
                    .color(ColorUtil.fromHex("#FFA000"))
                    .build());
            //panelComponent.getChildren().add(tableMarksComponent);
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Delays")
                    .color(ColorUtil.fromHex("#F8BBD0"))
                    .build());
            //panelComponent.getChildren().add(tableDelayComponent);
             */

        }
        return super.render(graphics);
    }
}
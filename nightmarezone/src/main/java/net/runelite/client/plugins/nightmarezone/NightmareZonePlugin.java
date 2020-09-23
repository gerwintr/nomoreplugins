/*
 * Copyright (c) 2018, James Swindle <wilingua@gmail.com>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.nightmarezone;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.menus.MenuManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.pf4j.Extension;

import java.time.Instant;

import static net.runelite.client.plugins.nightmarezone.NightmareZoneState.*;


@Extension
@PluginDescriptor(
        name = "Pinq's NMZ",
        description = "Does NMZ",
        tags = {"pinqer", "Fishing", "Tick", "Nightmare"},
        type = PluginType.SKILLING
)
@Slf4j
@PluginDependency(BotUtils.class)
public class NightmareZonePlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private NightmareZoneConfig config;

    @Inject
    private BotUtils utils;

    @Inject
    private KeyManager keyManager;

    @Inject
    private MenuManager menuManager;

    @Inject
    OverlayManager overlayManager;

    @Inject
    NightmareZoneOverlay overlay;

    @Inject
    ExecutorService executorService;

    Player player;
    LocalPoint beforeLoc = new LocalPoint(0, 0);
    MenuEntry targetMenu;
    Instant botTimer;
    int timeout;
    long sleepLength;
    NightmareZoneState state;
    String status;
    NPC minnowsSpot;
    boolean wait = true;
    Boolean shouldAbsorb;

    boolean run = false;
    private static final int[] NMZ_MAP_REGION = {9033};
    Collection<Integer> overloads;
    Collection<Integer> absorbtions;
    Collection<Integer> prayerpots;

    @Provides
    NightmareZoneConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(NightmareZoneConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        keyManager.registerKeyListener(hotkeyListener);
        botTimer = Instant.now();
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(hotkeyListener);
    }

    private NightmareZoneState getState() {
        if (timeout > 0 || !isInNightmareZone(client))
        {
            return TIMEOUT;
        }

        if (client.getBoostedSkillLevel(Skill.PRAYER) > 15 && config.prayerEnable()) {
            return PRAYPOT;
        }

        boolean PROTECT_MELEE = client.getVar(Prayer.PROTECT_FROM_MELEE.getVarbit()) != 0;
        if (!PROTECT_MELEE && config.prayerEnable()) {
            return CLICKPRAY;
        }

        //not overloaded
        if (client.getVar(Varbits.NMZ_OVERLOAD) == 0 && client.getBoostedSkillLevel(Skill.HITPOINTS) > 50) {
            return OVERLOAD;
        }

        if (client.getVar(Varbits.NMZ_ABSORPTION) < 200) {
            shouldAbsorb = true;
        } else if (client.getVar(Varbits.NMZ_ABSORPTION) > 800){
            shouldAbsorb = false;
        }

        if (shouldAbsorb) {
            return ABSORPTION;
        }

        if (client.getBoostedSkillLevel(Skill.HITPOINTS) > 1) {
            return ROCKCAKE;
        }

        return TIMEOUT;
    }

    @Subscribe
    private void onGameTick(GameTick event)
    {
        if (!run)
        {
            return;
        }
        player = client.getLocalPlayer();
        if (client != null && player != null && client.getGameState() == GameState.LOGGED_IN)
        {
            state = getState();
            switch (state)
            {
                case TIMEOUT:
                    log.info("TIMEOUT: " + timeout);
                    timeout--;
                    break;
                case OVERLOAD:
                    log.info("OVERLOAD");
                    WidgetItem overload = utils.getInventoryWidgetItem(overloads);
                    if (overload != null) {
                        targetMenu = new MenuEntry("", "", overload.getId(), MenuOpcode.ITEM_FIRST_OPTION.getId(), overload.getIndex(),
                                WidgetInfo.INVENTORY.getId(), false);
                        utils.delayMouseClick(overload.getCanvasBounds(), sleepDelay());
                    } else {
                        log.info("NO OVERLOADS");
                        run = false;
                    }
                    break;
                case ABSORPTION:
                    log.info("ABSORPTION");
                    WidgetItem absorb = utils.getInventoryWidgetItem(absorbtions);
                    if (absorb != null) {
                        targetMenu = new MenuEntry("", "", absorb.getId(), MenuOpcode.ITEM_FIRST_OPTION.getId(), absorb.getIndex(),
                                WidgetInfo.INVENTORY.getId(), false);
                        utils.delayMouseClick(absorb.getCanvasBounds(), sleepDelay());
                    } else {
                        log.info("NO ABSORBTIONS");
                        run = false;
                    }
                    break;
                case ROCKCAKE:
                    log.info("ROCKCAKE");
                    WidgetItem rockcake = utils.getInventoryWidgetItem(ItemID.DWARVEN_ROCK_CAKE_7510);
                    if (rockcake != null) {
                        targetMenu = new MenuEntry("", "", rockcake.getId(), MenuOpcode.ITEM_FIRST_OPTION.getId(), rockcake.getIndex(),
                                WidgetInfo.INVENTORY.getId(), false);
                        utils.delayMouseClick(rockcake.getCanvasBounds(), sleepDelay());
                    } else {
                        log.info("NO ROCKCAKE");
                        run = false;
                    }
                    break;
                case PRAYPOT:
                    log.info("PRAYPOT");
                    WidgetItem praypot = utils.getInventoryWidgetItem(ItemID.DWARVEN_ROCK_CAKE_7510);
                    if (praypot != null) {
                        targetMenu = new MenuEntry("", "", praypot.getId(), MenuOpcode.ITEM_FIRST_OPTION.getId(), praypot.getIndex(),
                                WidgetInfo.INVENTORY.getId(), false);
                        utils.delayMouseClick(praypot.getCanvasBounds(), sleepDelay());
                    } else {
                        log.info("NO PRAYER POTIONS LEFT");
                        run = false;
                    }
                    break;
                case CLICKPRAY:
                    log.info("CLICKPRAY");
                    Widget prayer_widget = client.getWidget(WidgetInfo.PRAYER_PROTECT_FROM_MELEE);
                    if (prayer_widget != null) {
                        targetMenu = new MenuEntry("Activate", prayer_widget.getName(), 1, MenuOpcode.CC_OP.getId(),
                                -1, prayer_widget.getId(), false);
                        utils.delayClickRandomPointCenter(-75,75, sleepDelay());
                    } else {
                        log.info("PRAYER NOT HIGH ENOUGH LEVEL");
                        run = false;
                    }
                    break;
            }
            beforeLoc = player.getLocalLocation();
        }
    }

    private long sleepDelay() {
        sleepLength = utils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
        return sleepLength;
    }

    public static boolean isInNightmareZone(Client client)
    {
        if (client.getLocalPlayer() == null)
        {
            return false;
        }

        // NMZ and the KBD lair uses the same region ID but NMZ uses planes 1-3 and KBD uses plane 0
        return client.getLocalPlayer().getWorldLocation().getPlane() > 0 && Arrays.equals(client.getMapRegions(), NMZ_MAP_REGION);
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (targetMenu == null) {
            log.info("Modified MenuEntry is null");
        } else {
            log.info("MenuEntry string event: " + targetMenu.toString());
            event.setMenuEntry(targetMenu);
            targetMenu = null; //this allows the player to interact with the client without their clicks being overridden
        }
    }

    public HotkeyListener hotkeyListener = new HotkeyListener(() -> config.toggleKey()) {
        @Override
        public void hotkeyPressed() {
            executorService.submit(() ->
            {
                run = !run;
                if (!run) {
                    log.info("Stopped");
                    status = "Starting...";
                }
                if (run) {
                    overloads.add(ItemID.OVERLOAD_1);
                    overloads.add(ItemID.OVERLOAD_2);
                    overloads.add(ItemID.OVERLOAD_3);
                    overloads.add(ItemID.OVERLOAD_4);
                    absorbtions.add(ItemID.ABSORPTION_1);
                    absorbtions.add(ItemID.ABSORPTION_2);
                    absorbtions.add(ItemID.ABSORPTION_3);
                    absorbtions.add(ItemID.ABSORPTION_4);
                    prayerpots.add(ItemID.PRAYER_POTION1);
                    prayerpots.add(ItemID.PRAYER_POTION2);
                    prayerpots.add(ItemID.PRAYER_POTION3);
                    prayerpots.add(ItemID.PRAYER_POTION4);
                    log.info("Started");
                }
            });
        }
    };
}

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
package net.runelite.client.plugins.tickfishing;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
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

import java.util.concurrent.ExecutorService;

import org.pf4j.Extension;

import java.time.Instant;


@Extension
@PluginDescriptor(
        name = "Pinq's 3 Tick Fishing",
        description = "Does 3 Tick Fishing Flawlessly",
        tags = {"pinqer", "Fishing", "3 Tick", "Tick"},
        type = PluginType.SKILLING
)
@Slf4j
@PluginDependency(BotUtils.class)
public class TickFishingPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private TickFishingConfig config;

    @Inject
    private BotUtils utils;

    @Inject
    private KeyManager keyManager;

    @Inject
    private MenuManager menuManager;

    @Inject
    OverlayManager overlayManager;

    @Inject
    TickFishingOverlay overlay;

    @Inject
    ExecutorService executorService;

    Player player;
    LocalPoint beforeLoc = new LocalPoint(0, 0);
    MenuEntry targetMenu;
    Instant botTimer;
    long time1;
    long time2;
    int tickTiming = 0;
    int xpGained = 0;
    int initialLevel = 0;
    int tickDelay;
    long sleepLength;
    String status;
    NPC targetNPC;

    boolean run = false;
    boolean thread = false;

    @Provides
    TickFishingConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TickFishingConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        keyManager.registerKeyListener(hotkeyListener);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        tickTiming = 0;
        keyManager.unregisterKeyListener(hotkeyListener);
    }

    @Subscribe
    public void onGameTick(GameTick tick) {

        player = client.getLocalPlayer();

        if (!run) {
            return;
        }
        if (utils.isMoving()) {
            return;
        }
        //MenuEntry string event: MenuEntry(option=Fish, target=Fishing Spot, identifier=13362, opcode=9, param0=0, param1=0, forceLeftClick=false)


        if (tickDelay > 0) {
            tickDelay--;
            return;
        } else {
            tickDelay = 0; // Loops every one tick
        }

        tickTiming++;

        if (tickTiming > 3) {
            tickTiming = 1;
        }

        // First Tick
        if (tickTiming == 1) {
            clickTar();
            return;
        }

        // Second Tick
        if (tickTiming == 2) {
            executorService.submit(() ->
            {
                clickHerb();
                dropFish();
            });
            return;
        }

        // Third Tick
        if (tickTiming == 3) {
            targetNPC = utils.findNearestNpc(Integer.parseInt(config.npcID())); // A Fishing Spot Is An NPC & Fly Fishing Spot ID = 1526
            clickFishingSpot();
            return;
        }
    }

    private void clickTar() {
        status = "Clicking Tar";
        executorService.submit(() ->
        {
            log.info("Beginning of clickTar()");
            try {
                Thread.sleep(sleepDelay());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("End of clickTar() Sleep");
            WidgetItem swampTar = utils.getInventoryWidgetItem(ItemID.SWAMP_TAR);
            if (swampTar != null) {
                targetMenu = new MenuEntry("Use", "Swamp Tar", swampTar.getId(), MenuOpcode.ITEM_USE.getId(),
                        swampTar.getIndex(), 9764864, false);
                utils.clickRandomPointCenter(-100, 100);
            } else {
                log.info("Out of Swamp Tar, Plugin Stopped");
                run = false;
            }
        });
    }

    private void clickHerb() {
        status = "Clicking Herb";
        log.info("Beginning of clickHerb()");
        try {
            Thread.sleep(sleepDelay());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("End of clickHerb() Sleep");
        WidgetItem herb = utils.getInventoryWidgetItem(ItemID.GUAM_LEAF);
        if (herb != null) {
            targetMenu = new MenuEntry("Use Swamp Tar -> Guam Leaf", "Guam Leaf", herb.getId(), MenuOpcode.ITEM_USE_ON_WIDGET_ITEM.getId(),
                    herb.getIndex(), 9764864, false);
            utils.clickRandomPointCenter(-100, 100);
        } else {
            log.info("Out of Herb Leaf, Plugin Stopped");
            run = false;
        }
    }

    private void dropFish() {
        status = "Dropping Fish";
        log.info("Beginning of dropFish()");
        try {
            Thread.sleep(sleepDelay());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("End of dropFish() Sleep");
        WidgetItem fish = utils.getInventoryWidgetItem(utils.stringToIntList(config.dropIDs()));
        if (fish != null) {
            targetMenu = new MenuEntry("Drop", "Fish", fish.getId(), MenuOpcode.ITEM_DROP.getId(),
                    fish.getIndex(), 9764864, false);
            utils.clickRandomPointCenter(-100, 100);
        }
    }

    private void clickFishingSpot() {
        status = "Clicking Fish";
        executorService.submit(() ->
        {
            log.info("Beginning of clickFishingSpot()");
            try {
                Thread.sleep(sleepDelay());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("End of clickFishingSpot() Sleep");
            if (targetNPC != null) {
                targetMenu = new MenuEntry("Fish", "Fishing Spot", targetNPC.getIndex(), MenuOpcode.NPC_FIRST_OPTION.getId(),
                        0, 0, false);
                utils.clickRandomPointCenter(-100, 100);
            } else {
                log.info("No Fishing Spots Found");
                //run = false;
            }
        });
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        /*
        log.info(event.getTarget() + " " + event.getIdentifier());
        if (event.getIdentifier() == 1939 || targetMenu.getIdentifier() == 1939) {
            time2 = System.currentTimeMillis();
            log.info("Click delay (Fishing Spot -> Swamp-tar): " + (time2-time1) + " ms");
            time1 = System.currentTimeMillis();
        }
        if (event.getIdentifier() == 249 || targetMenu.getIdentifier() == 249) {
            time2 = System.currentTimeMillis();
            log.info("Click delay (Swamp -> Guam): " + (time2-time1) + " ms");
            time1 = System.currentTimeMillis();
        }
        if (event.getIdentifier() == 335 || targetMenu.getIdentifier() == 335) {
            time2 = System.currentTimeMillis();
            log.info("Click delay (Guam -> Drop Fish): " + (time2-time1) + " ms");
            time1 = System.currentTimeMillis();
        }
        if (event.getIdentifier() == 17529 || targetMenu.getIdentifier() == 17529) {
            time2 = System.currentTimeMillis();
            log.info("Click delay (Drop -> Fishing Spot & Guam -> Fishing Spot): " + (time2-time1) + " ms");
            time1 = System.currentTimeMillis();
        }
         */

        if (targetMenu == null) {
            //log.info("Modified MenuEntry is null");
        } else {
            log.info("MenuEntry string event: " + targetMenu.toString());
            event.setMenuEntry(targetMenu);
            targetMenu = null; //this allows the player to interact with the client without their clicks being overridden
        }
    }

    private long sleepDelay() {
        sleepLength = utils.randomDelay(false, 100, 300, 30, 200);
        return sleepLength;
    }

    public HotkeyListener hotkeyListener = new HotkeyListener(() -> config.toggleKey()) {
        @Override
        public void hotkeyPressed() {
            run = !run;
            if (!run) {
                log.info("Stopped");
                tickTiming = 0;
                status = "Starting...";
            }
            if (run) {
                log.info("Started");
                initialLevel = client.getRealSkillLevel(Skill.FISHING);
                botTimer = Instant.now();
            }
        }
    };
}

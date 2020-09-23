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
package net.runelite.client.plugins.fishingtrawler;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
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


@Extension
@PluginDescriptor(
        name = "Pinq's fishing trawler",
        description = "Does 3 Tick Fishing Flawlessly",
        tags = {"pinqer", "Fishing", "3 Tick", "Tick"},
        type = PluginType.SKILLING
)
@Slf4j
@PluginDependency(BotUtils.class)
public class FishingTrawlerPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private FishingTrawlerConfig config;

    @Inject
    private BotUtils utils;

    @Inject
    private KeyManager keyManager;

    @Inject
    private MenuManager menuManager;

    @Inject
    OverlayManager overlayManager;

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
    boolean atBoat = false;
    Point clickPoint;
    GameObject boat;
    boolean shouldClickNet = false;
    boolean clickedNet = false;
    private static final int[] FILLED_MAP_REGION = {8011};
    int clickedNetCount = 0;
    Collection<Integer> bail;

    boolean run = false;
    boolean thread = false;

    @Provides
    FishingTrawlerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(FishingTrawlerConfig.class);
    }

    @Override
    protected void startUp() {
        keyManager.registerKeyListener(hotkeyListener);
    }

    @Override
    protected void shutDown() {
        keyManager.unregisterKeyListener(hotkeyListener);
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        player = client.getLocalPlayer();

        if (utils.isMoving(beforeLoc) || utils.isAnimating()) {
            beforeLoc = player.getLocalLocation();
            return;
        }
        beforeLoc = player.getLocalLocation();

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        WorldPoint leakWorldPoint = new WorldPoint(1887, 4827, 0);
        GameObject leak = utils.findNearestGameObjectWithin(player.getWorldLocation(), 20, 37355);
        if (isInFilledShip(client)) {
            utils.sendGameMessage("in filled ship");
            log.info("is in ffilled ship");
            bail.add(583);
            bail.add(585);
            WidgetItem bailingbucket = utils.getInventoryWidgetItem(bail);
            if (bailingbucket != null) {
                targetMenu = new MenuEntry("", "", bailingbucket.getId(), MenuOpcode.ITEM_FIRST_OPTION.getId(), bailingbucket.getIndex(),
                        WidgetInfo.INVENTORY.getId(), false);
                utils.delayMouseClick(bailingbucket.getCanvasBounds(), sleepDelay());
                tickDelay = utils.getRandomIntBetweenRange(2,5);
            } else {
                utils.sendGameMessage("Bailing bucket is null");
            }
            return;
        }
        if (leak != null) { // in minigame
            log.info(leak.getWorldLocation().toString());
            targetMenu = new MenuEntry("", "", leak.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(),
                    leak.getSceneMinLocation().getX(), leak.getSceneMinLocation().getY(), false);
            utils.delayMouseClick(leak.getConvexHull().getBounds(), sleepDelay());
            tickDelay = utils.getRandomIntBetweenRange(0, 3);
            shouldClickNet = true;
            clickedNetCount = 0;
        } else { // not in minigame
            log.info("is null");
            Widget depositAllWidget = client.getWidget(367, 19);
            GameObject net = utils.findNearestGameObject(2483);
            GameObject gangplank = utils.findNearestGameObject(4977);
            if (depositAllWidget != null) {
                log.info("DEPOSIT_ALL_WIDGET");
                utils.delayMouseClick(depositAllWidget.getBounds(), sleepDelay());
                tickDelay = 6;
                shouldClickNet = false;
                return;
            }
            if (shouldClickNet) {
                if (net != null) {
                    targetMenu = new MenuEntry("", "", net.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(),
                            net.getSceneMinLocation().getX(), net.getSceneMinLocation().getY(), false);
                    utils.delayMouseClick(net.getConvexHull().getBounds(), sleepDelay());
                    clickedNetCount++;
                    if (clickedNetCount > 10) {
                        shouldClickNet = false;
                        clickedNetCount = 0;
                    }
                }
                return;
            }
            if (gangplank != null) {
                targetMenu = new MenuEntry("", "", gangplank.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(),
                        gangplank.getSceneMinLocation().getX(), gangplank.getSceneMinLocation().getY(), false);
                utils.delayMouseClick(gangplank.getConvexHull().getBounds(), sleepDelay());
                return;
            }
        }
    }
//2020-08-23 18:07:33 [Client] INFO  n.r.c.p.p.PestControlPlugin - Inspect
//2020-08-23 18:07:33 [Client] INFO  n.r.c.p.p.PestControlPlugin - <col=ffff>Trawler net
//2020-08-23 18:07:33 [Client] INFO  n.r.c.p.p.PestControlPlugin - 3
//2020-08-23 18:07:33 [Client] INFO  n.r.c.p.p.PestControlPlugin - 2483
//2020-08-23 18:07:33 [Client] INFO  n.r.c.p.p.PestControlPlugin - 50
//2020-08-23 18:07:33 [Client] INFO  n.r.c.p.p.PestControlPlugin - 55


    //                GameObject gangplank = utils.findNearestGameObject(4977);
    //                targetMenu = new MenuEntry("", "", gangplank.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(),
    //                        gangplank.getSceneMinLocation().getX(), gangplank.getSceneMinLocation().getY(), false);
    //                utils.delayClickRandomPointCenter(-75, 75, sleepDelay());

    public void click() {
        targetMenu = new MenuEntry("", "", boat.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(),
                boat.getSceneMinLocation().getX(), boat.getSceneMinLocation().getY(), false);
        if (targetMenu.getMenuOpcode() != MenuOpcode.GAME_OBJECT_FIRST_OPTION) {
            return;
        }
        utils.delayClickRandomPointCenter(-100, 100, sleepDelay());
    }

    //37351 leak
    //37352 repaired
    NullObjectID yeet;

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {

        if (targetMenu == null) {
            //log.info("Modified MenuEntry is null");
        } else {
            log.info("MenuEntry string event: " + targetMenu.toString());
            event.setMenuEntry(targetMenu);
            targetMenu = null; //this allows the player to interact with the client without their clicks being overridden
        }
    }

    private long sleepDelay() {
        sleepLength = utils.randomDelay(false, 300, 500, 30, 400);
        return sleepLength;
    }

    private boolean isInFilledShip(Client client) {
        if (client.getLocalPlayer() == null) {
            return false;
        }

        // Filled ship uses map region 8011
        return Arrays.equals(client.getMapRegions(), FILLED_MAP_REGION);
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
                    log.info("Started");
                    botTimer = Instant.now();
                }
            });
        }
    };
}

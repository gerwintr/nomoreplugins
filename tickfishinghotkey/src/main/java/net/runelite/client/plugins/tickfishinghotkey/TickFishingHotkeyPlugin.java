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
package net.runelite.client.plugins.tickfishinghotkey;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import net.runelite.api.Point;
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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;

import org.pf4j.Extension;

import java.time.Instant;


@Extension
@PluginDescriptor(
        name = "Pinq's Herb-Tar hotkey",
        description = "Does 3 Tick Fishing Flawlessly with hotkey",
        tags = {"pinqer", "Fishing", "3 Tick", "Tick"},
        type = PluginType.SKILLING
)
@Slf4j
@PluginDependency(BotUtils.class)
public class TickFishingHotkeyPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private TickFishingHotkeyConfig config;

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

    boolean run = false;
    boolean thread = false;

    @Provides
    TickFishingHotkeyConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(TickFishingHotkeyConfig.class);
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
    }

    private void clickTar() {
        WidgetItem swampTar = utils.getInventoryWidgetItem(ItemID.SWAMP_TAR);
        if (swampTar != null) {
            targetMenu = new MenuEntry("Use", "Swamp Tar", swampTar.getId(), MenuOpcode.ITEM_USE.getId(),
                    swampTar.getIndex(), 9764864, false);
            click();
        }
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
            click();
        }
    }

    public void click() {
        Point pos = client.getMouseCanvasPosition();

        if (client.isStretchedEnabled()) {
            final Dimension stretched = client.getStretchedDimensions();
            final Dimension real = client.getRealDimensions();
            final double width = (stretched.width / real.getWidth());
            final double height = (stretched.height / real.getHeight());
            final Point point = new Point((int) (pos.getX() * width), (int) (pos.getY() * height));
            client.getCanvas().dispatchEvent(new MouseEvent(client.getCanvas(), 501, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, 1));
            client.getCanvas().dispatchEvent(new MouseEvent(client.getCanvas(), 502, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, 1));
            client.getCanvas().dispatchEvent(new MouseEvent(client.getCanvas(), 500, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, 1));
            return;
        }

        client.getCanvas().dispatchEvent(new MouseEvent(client.getCanvas(), 501, System.currentTimeMillis(), 0, pos.getX(), pos.getY(), 1, false, 1));
        client.getCanvas().dispatchEvent(new MouseEvent(client.getCanvas(), 502, System.currentTimeMillis(), 0, pos.getX(), pos.getY(), 1, false, 1));
        client.getCanvas().dispatchEvent(new MouseEvent(client.getCanvas(), 500, System.currentTimeMillis(), 0, pos.getX(), pos.getY(), 1, false, 1));
    }

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
        sleepLength = utils.randomDelay(false, 100, 300, 30, 200);
        return sleepLength;
    }

    public HotkeyListener hotkeyListener = new HotkeyListener(() -> config.toggleKey()) {
        @Override
        public void hotkeyPressed() {
            executorService.submit(() ->
            {
                clickTar();
                clickHerb();
            });
        }
    };
}

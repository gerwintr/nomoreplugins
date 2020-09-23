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
package net.runelite.client.plugins.minnowsbot;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.menus.MenuManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.ui.overlay.OverlayManager;
//import net.runelite.client.plugins.chinbreakhandler.ChinBreakHandler;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.time.Instant;
import java.util.concurrent.ExecutorService;

import static net.runelite.client.plugins.minnowsbot.MinnowsBotState.*;


@Extension
@PluginDescriptor(
        name = "Pinq's Minnows",
        description = "Clicks minnows",
        tags = {"pinqer", "Fishing", "Tick"},
        type = PluginType.SKILLING
)
@Slf4j
@PluginDependency(BotUtils.class)
public class MinnowsBotPlugin extends Plugin implements KeyListener {

    @Inject
    private Client client;

    @Inject
    private MinnowsBotConfig config;

    @Inject
    private BotUtils utils;

    @Inject
    private KeyManager keyManager;

    @Inject
    private MenuManager menuManager;

    @Inject
    OverlayManager overlayManager;

    @Inject
    MinnowsBotOverlay overlay;

 //   @Inject
 //   ChinBreakHandler chinBreakHandler;

    @Inject
    ExecutorService executorService;

    Player player;
    LocalPoint beforeLoc = new LocalPoint(0, 0);
    MenuEntry targetMenu;
    Instant botTimer;
    int timeout;
    long sleepLength;
    long sleepLength2;
    MinnowsBotState state;
    String status;
    NPC minnowsSpot;
    boolean wait = true;
    private static final int[] FILLED_MAP_REGION = {8011};

    boolean run = false;

    @Provides
    MinnowsBotConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MinnowsBotConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        keyManager.registerKeyListener(this);
     //   chinBreakHandler.registerPlugin(this);
        botTimer = Instant.now();
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        keyManager.unregisterKeyListener(this);
     //   chinBreakHandler.unregisterPlugin(this);
    }

    private MinnowsBotState getState() {
        if (timeout > 0)
        {
            utils.handleRun(20, 20);
            return TIMEOUT;
        }

        if (player.getInteracting() == null) {
            status = "Clicking Fishing Spot";
            return FISH;
        }

        if (player.getInteracting() != null) {
            if (minnowsSpot.getModel().getModelHeight() > 10) {
                status = "Moving Fishing Spot";
                return MOVE_SPOT;
            }
        }

      //  if (chinBreakHandler.shouldBreak(this))
      //  {
      //      return HANDLE_BREAK;
      //  }

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
            if (client.getWidget(WidgetInfo.LEVEL_UP_SKILL) != null) {
                minnowsSpot = utils.findNearestNpc(7730, 7731);
                clickSpot();
            }
            state = getState();
            switch (state)
            {
                case TIMEOUT:
                    log.info("TIMEOUT");
                    timeout--;
                    break;
                case FISH:
                    log.info("FISH");
                    minnowsSpot = utils.findNearestNpc(7730, 7731); // East Minnow Spot Ids
                    targetMenu = new MenuEntry("", "", minnowsSpot.getIndex(), MenuOpcode.NPC_FIRST_OPTION.getId(),
                            0, 0, false);
                    utils.delayMouseClick(minnowsSpot.getConvexHull().getBounds(), sleepDelay2());
                    timeout = 6;
                    wait = true;
                    break;
                case MOVE_SPOT:
                    log.info("MOVE_SPOT");
                    if (((NPC) player.getInteracting()).getId() == 7730) {
                        minnowsSpot = utils.findNearestNpc(7731);
                        clickSpot();
                        timeout = utils.getRandomIntBetweenRange(6,8);
                    } else if (((NPC) player.getInteracting()).getId() == 7731) {
                        minnowsSpot = utils.findNearestNpc(7730);
                        clickSpot();
                        timeout = utils.getRandomIntBetweenRange(6,8);
                    }
                    break;
            }
            beforeLoc = player.getLocalLocation();
        }
    }

    public void clickSpot() {
        targetMenu = new MenuEntry("", "", minnowsSpot.getIndex(), MenuOpcode.NPC_FIRST_OPTION.getId(),
                0, 0, false);
        utils.delayMouseClick(minnowsSpot.getConvexHull().getBounds(), sleepDelay());
    }

    private long sleepDelay() {
        sleepLength = utils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
        return sleepLength;
    }

    private long sleepDelay2() {
        sleepLength = utils.randomDelay(false, 1300, 1700, 50, 1550);
        return sleepLength;
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


    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        final int keycode = config.toggleKey().getKeyCode();
        log.info(String.valueOf(keycode) + " : " + e.getKeyCode());

        if (e.getKeyCode() == keycode && run)
        {
            //chinBreakHandler.stopPlugin(this);
            log.info("Stopped");
            run = false;
        }
        else if (e.getKeyCode() == keycode && !run)
        {
          //  chinBreakHandler.startPlugin(this);
            log.info("Started");
            run = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}

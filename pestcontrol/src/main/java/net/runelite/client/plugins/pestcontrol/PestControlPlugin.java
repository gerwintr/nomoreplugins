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
package net.runelite.client.plugins.pestcontrol;

import com.google.inject.Provides;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageManager;
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
import org.pf4j.Extension;


@Extension
@PluginDescriptor(
        name = "Pinqs Pest Control",
        description = "description. description. description.",
        tags = {"pinqer"},
        type = PluginType.MINIGAME
)
@Slf4j
@PluginDependency(BotUtils.class)
public class PestControlPlugin extends Plugin {
    private static final int[] PEST_CONTROL_MAP_REGION = {10536};

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private PestControlConfig config;

    @Inject
    private BotUtils utils;

    @Inject
    private Notifier notifier;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private KeyManager keyManager;

    @Inject
    ExecutorService executorService;
    Player player;
    NPC currentNPC;
    MenuEntry targetMenu;
    boolean run;
    int tickDelay = 0;
    boolean threadFix = true;
    int count = 0;
    LocalPoint beforeLoc = new LocalPoint(0, 0);

    @Provides
    PestControlConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(PestControlConfig.class);
    }

    @Override
    protected void startUp() {
        keyManager.registerKeyListener(hotkeyListener);
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void shutDown() {
        currentNPC = null;
        targetMenu = null;
        keyManager.unregisterKeyListener(hotkeyListener);
        executorService.shutdown();
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        player = client.getLocalPlayer();

        // Ensures there's only one thread running at the same time
        if (threadFix = false) {
        	return;
		} else {
        	threadFix = false;
		}

        // Essentially makes the loop slower when it needs to be
        if (tickDelay > 0) {
            tickDelay--;
            return;
        }
		if (client != null && player != null && client.getGameState() == GameState.LOGGED_IN && run) {
			if (!utils.isAnimating() && !utils.isMoving(beforeLoc)) {
				if (!isInPestControl() && !isInBoat()) {
						GameObject gangplank = utils.findNearestGameObject(25631);
						if (gangplank != null) {
							targetMenu = new MenuEntry("", "gangplank", gangplank.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(),
									gangplank.getSceneMinLocation().getX(), gangplank.getSceneMinLocation().getY(), false);
							utils.delayClickRandomPointCenter(-100, 100, utils.getRandomIntBetweenRange(100, 200));
						}
				} else if (isInPestControl()) {
				    if (isInNewGame()) {
				        log.info("new game");
                    }
                    NPC target = utils.findNearestNpc("Brawler", "Defiler", "Ravager", "Shifter", "Torcher");
                    // Simulate a loop because sleeps doesn't work without freezing the fkn client
                    if (count < 7 && target == null && !isInNewGame()) {
                        log.info("Coudn't find a target, trying again: " + count);
                        target = utils.findNearestNpc("Brawler", "Defiler", "Ravager", "Shifter", "Torcher");
                        count++;
                        return;
                    }
                    count = 0;
                    // Priority check for NPCs
					NPC nearbyBrawler = utils.findNearestNpc("Brawler");
					if (nearbyBrawler != null) {
						target = nearbyBrawler;
					}
					NPC priorityTarget = utils.findNearestNpc(NpcID.PORTAL, NpcID.PORTAL_1740, NpcID.PORTAL_1741, NpcID.PORTAL_1742,
							NpcID.SPINNER, NpcID.SPINNER_1710, NpcID.SPINNER_1711, NpcID.SPINNER_1712, NpcID.SPINNER_1713); //Colored portals & spinners
					if (priorityTarget != null) {
						target = priorityTarget;
					}

					if (target != null) { // Found a target
						if (player.getInteracting() != null) { // If player is interacting
							currentNPC = (NPC) player.getInteracting();
							if (currentNPC != null && currentNPC.getHealthRatio() == -1) {
								log.info("Interacting and NPC has no health bar, finding new NPC");
								targetMenu = new MenuEntry("", target.getName() + "(" + target.getId() + ")", target.getIndex(), MenuOpcode.NPC_SECOND_OPTION.getId(),
										0, 0, false);
								utils.delayClickRandomPointCenter(-100, 100, utils.getRandomIntBetweenRange(100, 200));
							}
						} else {
							log.info("Attacking new target");
							targetMenu = new MenuEntry("", target.getName() + "(" + target.getId() + ")", target.getIndex(), MenuOpcode.NPC_SECOND_OPTION.getId(),
									0, 0, false);
							utils.delayClickRandomPointCenter(-100, 100, utils.getRandomIntBetweenRange(100, 200));
						}
					} else { // No targets found, open gate
						WallObject gate = utils.findNearestWallObject(14235, 14233);
						if (gate != null) {
							log.info("No NPCs found, opening nearest gate");
							targetMenu = new MenuEntry("", "gate", gate.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(),
									gate.getLocalLocation().getSceneX(), gate.getLocalLocation().getSceneY(), false);
							utils.delayClickRandomPointCenter(-100, 100, utils.getRandomIntBetweenRange(100, 200));
						}
					}
				}
			}
		}
        beforeLoc = client.getLocalPlayer().getLocalLocation(); // Updates location so that utils.ismoving(beforeloc) works
		tickDelay = utils.getRandomIntBetweenRange(1,2); // Makes sure the loop only happens every 1 to 2 ticks.
		threadFix = true;
    }

    /*
     * Other functions
     */

    private boolean isInBoat() {
        Widget boatWidget = client.getWidget(WidgetInfo.PEST_CONTROL_BOAT_INFO);
        return boatWidget != null;
    }

    private boolean isInPestControl() {
        return Arrays.equals(client.getMapRegions(), PEST_CONTROL_MAP_REGION);
    }

    private boolean isInNewGame() {
        Widget dialog = client.getWidget(WidgetInfo.DIALOG_NPC);
        return dialog != null;
    }


    public HotkeyListener hotkeyListener = new HotkeyListener(() -> config.toggleKey()) {
        @Override
        public void hotkeyPressed() {
            run = !run;
            if (!run) {
                log.info("Stopped");
            }
            if (run) {
                log.info("Started");
            }
        }
    };

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (config.logParams()) {
            log.info(event.getOption());
            log.info(event.getTarget());
            log.info(String.valueOf(event.getOpcode()));
            log.info(String.valueOf(event.getIdentifier()));
            log.info(String.valueOf(event.getParam0()));
            log.info(String.valueOf(event.getParam1()));
        }
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
}

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
package net.runelite.client.plugins.gargoylefighter;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.iutils.CalculationUtils;
import net.runelite.client.plugins.iutils.InterfaceUtils;
import net.runelite.client.plugins.iutils.InventoryUtils;
import net.runelite.client.plugins.iutils.MenuUtils;
import net.runelite.client.plugins.iutils.MouseUtils;
import net.runelite.client.plugins.iutils.NPCUtils;
import net.runelite.client.plugins.iutils.ObjectUtils;
import net.runelite.client.plugins.iutils.PlayerUtils;
import net.runelite.client.plugins.iutils.WalkUtils;
import net.runelite.client.plugins.iutils.iUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import org.pf4j.Extension;

import java.time.Instant;


@Extension
@PluginDescriptor(
        name = "Pinq's fighter",
        description = "gargoyle",
        tags = {"pinqer"}
)


@Slf4j
@PluginDependency(iUtils.class)
public class GargoyleFighterPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private GargoyleFighterConfig config;

    @Inject
    private iUtils iUtils;

    @Inject
    private KeyManager keyManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MouseUtils mouseUtils;

    @Inject
    private PlayerUtils playerUtils;

    @Inject
    private InventoryUtils inventoryUtils;

    @Inject
    private ObjectUtils objectUtils;

    @Inject
    private InterfaceUtils interfaceUtils;

    @Inject
    private CalculationUtils calculationUtils;

    @Inject
    private MenuUtils menuUtils;

    @Inject
    private NPCUtils npcUtils;

    @Inject
    private WalkUtils walkUtils;

    @Inject
    private Notifier notifier;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    GargoyleFighterOverlay overlay;

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
    NPC currentNPC;
    Boolean setHighAlch = false;
    int alchTimeout;
    int pickupId = 0;
    TileItem item;
    Tile tile;
    Collection<Integer> alchIds; // alch items to alch

    boolean run = false;
    boolean thread = false;

    @Provides
    GargoyleFighterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(GargoyleFighterConfig.class);
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
        log.info(String.valueOf(sleepDelay()));

        if (!run) {
            return;
        }

        if (!playerUtils.isAnimating() && !playerUtils.isMoving(beforeLoc)) {
            log.info("Moving");
            beforeLoc = client.getLocalPlayer().getLocalLocation();
            return;
        }
        beforeLoc = client.getLocalPlayer().getLocalLocation();

        if (tickDelay > 0) {
            log.info("Timeout");
            tickDelay--;
            return;
        } else {
            tickDelay = calculationUtils.getRandomIntBetweenRange(0, 1);
        }

        //Alch Code
        if (inventoryUtils.containsItem(getAlchItem())) {
            status = "Alching";
            log.info("Alching Item");
            highAlchItem();
            return;
        }

        // Combat code
        NPC target = npcUtils.findNearestAttackableNpcWithin(player.getWorldLocation(), 40, "gargoyle", false);
        if (target != null) { // Found a target
            log.info("Found target");
            NPC targetingCheck = npcUtils.findNearestNpcTargetingLocal("gargoyle", false);
            if (player.getInteracting() != null) { // If player is interacting
                log.info("Interacting");
                currentNPC = (NPC) player.getInteracting();
                log.info(String.valueOf(currentNPC.getHealthRatio()));
                if (currentNPC.getHealthRatio() < 3) {
                    tickDelay = calculationUtils.getRandomIntBetweenRange(5,7);
                }
                ; // do nothing
            } else if (targetingCheck == null) {
                status = "Attacking Gargoyle";
                log.info("Attacking new target");
                targetMenu = new MenuEntry("", target.getName() + "(" + target.getId() + ")", target.getIndex(), MenuAction.NPC_SECOND_OPTION.getId(),
                        0, 0, false);
                mouseUtils.delayClickRandomPointCenter(-100, 100, sleepDelay());
                tickDelay = calculationUtils.getRandomIntBetweenRange(4, 6);
            }
        }

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
        sleepLength = calculationUtils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
        return sleepLength;
    }

    private int tickDelay() {
        int tickLength = (int) calculationUtils.randomDelay(false, 2, 6, 1, 4);
        log.debug("tick delay for {} ticks", tickLength);
        return tickLength;
    }

    private void highAlchItem() {
        if (!setHighAlch) {
            targetMenu = new MenuEntry("Cast", "<col=00ff00>High Level Alchemy</col>", 0,
                    MenuAction.WIDGET_TYPE_2.getId(), -1, 14286887, false);
            Widget spellWidget = client.getWidget(WidgetInfo.SPELL_HIGH_LEVEL_ALCHEMY);
            if (spellWidget != null) {
                mouseUtils.delayClickRandomPointCenter(-100, 100, sleepDelay());
            } else {
                mouseUtils.delayClickRandomPointCenter(-100, 100, sleepDelay());
            }
            setHighAlch = true;
        } else {
            WidgetItem alchItem = inventoryUtils.getWidgetItem(getAlchItem());
            targetMenu = new MenuEntry("Cast", "<col=00ff00>High Level Alchemy</col><col=ffffff> ->",
                    alchItem.getId(),
                    MenuAction.ITEM_USE_ON_WIDGET.getId(),
                    alchItem.getIndex(), 9764864,
                    false);
            mouseUtils.delayClickRandomPointCenter(-100, 100, sleepDelay());
            setHighAlch = false;
            tickDelay = calculationUtils.getRandomIntBetweenRange(4, 5);
        }
    }

    private int getAlchItem() { // ALCH LIST
        if (inventoryUtils.containsItem(ItemID.ADAMANT_PLATELEGS)) {
            return ItemID.ADAMANT_PLATELEGS;
        } else if (inventoryUtils.containsItem(ItemID.RUNE_FULL_HELM)) {
            return ItemID.RUNE_FULL_HELM;
        } else if (inventoryUtils.containsItem(ItemID.RUNE_2H_SWORD)) {
            return ItemID.RUNE_2H_SWORD;
        } else if (inventoryUtils.containsItem(ItemID.ADAMANT_BOOTS)) {
            return ItemID.ADAMANT_BOOTS;
        } else if (inventoryUtils.containsItem(ItemID.RUNE_BATTLEAXE)) {
            return ItemID.RUNE_BATTLEAXE;
        } else if (inventoryUtils.containsItem(ItemID.RUNE_PLATELEGS)) {
            return ItemID.RUNE_PLATELEGS;
        } else if (inventoryUtils.containsItem(ItemID.MYSTIC_ROBE_TOP_DARK)) {
            return ItemID.MYSTIC_ROBE_TOP_DARK;
        } else if (inventoryUtils.containsItem(ItemID.SHIELD_LEFT_HALF)) {
            return ItemID.SHIELD_LEFT_HALF;
        } else if (inventoryUtils.containsItem(ItemID.DRAGON_SPEAR)) {
            return ItemID.DRAGON_SPEAR;
        } else if (inventoryUtils.containsItem(ItemID.RUNE_SPEAR)) {
            return ItemID.RUNE_SPEAR;
        } else if (inventoryUtils.containsItem(ItemID.UNCUT_SAPPHIRE)) {
            return ItemID.UNCUT_SAPPHIRE;
        } else if (inventoryUtils.containsItem(ItemID.UNCUT_EMERALD)) {
            return ItemID.UNCUT_EMERALD;
        } else if (inventoryUtils.containsItem(ItemID.UNCUT_RUBY)) {
            return ItemID.UNCUT_RUBY;
        } else if (inventoryUtils.containsItem(ItemID.NATURE_TALISMAN)) {
            return ItemID.NATURE_TALISMAN;
        } else if (inventoryUtils.containsItem(ItemID.UNCUT_DIAMOND)) {
            return ItemID.UNCUT_DIAMOND;
        } else if (inventoryUtils.containsItem(ItemID.RUNE_JAVELIN)) {
            return ItemID.RUNE_JAVELIN;
        }
        return 0;
    }

    @Subscribe
    private void onNpcLootReceived(NpcLootReceived npcLootReceived) {
        status = "loot detected";
        log.info(npcLootReceived.getItems().toString());
        npcLootReceived.getItems().forEach(itemNPC ->
                {
                    targetMenu = new MenuEntry("", "", itemNPC.getId(), 20, itemNPC.getLocation().getSceneX(), itemNPC.getLocation().getSceneY(), false);
                    mouseUtils.delayClickRandomPointCenter(-100, 100, sleepDelay());
                }
        );
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

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
package net.runelite.client.plugins.mudrunecrafter;

import com.google.inject.Provides;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;

import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.Notifier;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
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
        name = "Pinq's Mud Runecrafter",
        description = "Crafts Mud Runes at Earth Altar",
        tags = {"pinqer"},
        type = PluginType.MINIGAME
)
@Slf4j
@PluginDependency(BotUtils.class)
public class MudRunecrafterPlugin extends Plugin {
    private static final int[] EARTH_ALTAR_MAP_REGION = {10571};
    private static final int[] MYSTERIOUS_RUINS_MAP_REGION = {13110};

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MudRunecrafterConfig config;

    @Inject
    private BotUtils utils;

    @Inject
    private Notifier notifier;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private KeyManager keyManager;

    @Inject
    private ItemManager itemManager;


    @Inject
    MudRunecrafterOverlay overlay;

    @Inject
    ExecutorService executorService;
    Player player;
    MenuEntry targetMenu;
    boolean run;
    int tickDelay = 0;
    boolean threadFix = true;
    boolean setTalisman = false;
    boolean clickedRuins = false;
    int clickedRuinsResetCount = 0;
    String status = "Starting...";
    int runesCrafted = 0;
    int xpGained = 0;
    int initialLevel = 0;
    int mudRunePrice = 0;
    Instant botTimer;
    Set<Integer> BINDING_NECKLACE = Set.of(ItemID.BINDING_NECKLACE);
    Set<Integer> STAMINA_POTIONS = Set.of(ItemID.STAMINA_POTION1, ItemID.STAMINA_POTION2, ItemID.STAMINA_POTION3, ItemID.STAMINA_POTION4);
    LocalPoint beforeLoc = new LocalPoint(0, 0);

    @Provides
    MudRunecrafterConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(MudRunecrafterConfig.class);
    }

    @Override
    protected void startUp() {
        overlayManager.add(overlay);
        setTalisman = false;
        keyManager.registerKeyListener(hotkeyListener);
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void shutDown() {
        runesCrafted = 0;
        overlayManager.remove(overlay);
        botTimer = null;
        run = false;
        setTalisman = false;
        targetMenu = null;
        keyManager.unregisterKeyListener(hotkeyListener);
        executorService.shutdown();
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        player = client.getLocalPlayer();
        clickedRuinsResetCount++;
        if (clickedRuinsResetCount > 15) {
            clickedRuins = false;
            clickedRuinsResetCount = 0;
        }

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
        } else {
            tickDelay = 0; // Makes sure the loop only happens every 1 ticks.
        }

        if (client != null && player != null && client.getGameState() == GameState.LOGGED_IN && run) {
            mudRunePrice = itemManager.getItemPrice(ItemID.MUD_RUNE);
            if (utils.isMoving(beforeLoc) && !isAtMysteriousRuin() && !utils.isBankOpen()) {
                threadFix = true;
                beforeLoc = client.getLocalPlayer().getLocalLocation(); // Updates location so that utils.ismoving(beforeloc) works
                return;
            }
            beforeLoc = client.getLocalPlayer().getLocalLocation(); // Updates location so that utils.ismoving(beforeloc) works

            if (utils.isBankOpen() && !utils.inventoryContains(ItemID.PURE_ESSENCE)) {
                handleBank();
                threadFix = true;
                return;
            }

            if (isInAltarRoom()) {
                if (utils.inventoryContains(ItemID.PURE_ESSENCE)) {
                    log.info("set Talisman? = " + String.valueOf(setTalisman));
                    if (setTalisman) {
                        status = "Earth Altar";
                        log.info("Clicking earth altar");
                        GameObject earthAltar = utils.findNearestGameObject(ObjectID.ALTAR_34763);
                        if (earthAltar != null) {
                            targetMenu = new MenuEntry("Use", "<col=ff9040>Water talisman<col=ffffff> -> <col=ffff>Altar",
                                    earthAltar.getId(), MenuOpcode.ITEM_USE_ON_GAME_OBJECT.getId(), earthAltar.getSceneMinLocation().getX(),
                                    earthAltar.getSceneMinLocation().getY(), false);
                            utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                            runesCrafted += 26;
                            xpGained += 247;
                            setTalisman = false;
                            threadFix = true;
                            return;
                        }
                    }
                    if (!setTalisman) {
                        status = "Water Talisman";
                        log.info("Clicking water talisman");
                        WidgetItem waterTalisman = utils.getInventoryWidgetItem(ItemID.WATER_TALISMAN);
                        if (waterTalisman != null) {
                            targetMenu = new MenuEntry("Use", "Water talisman", ItemID.WATER_TALISMAN, MenuOpcode.ITEM_USE.getId(),
                                    waterTalisman.getIndex(), 9764864, false);
                            utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                            tickDelay = utils.getRandomIntBetweenRange(0, 1);
                            setTalisman = true;
                            threadFix = true;
                            return;
                        }
                    }
                } else {
                    status = "Teleporting";
                    log.info("Teleporting to varrock");
                    WidgetItem varrockTele = utils.getInventoryWidgetItem(ItemID.VARROCK_TELEPORT);
                    if (varrockTele != null) {
                        targetMenu = new MenuEntry("Break", "Break", ItemID.VARROCK_TELEPORT, MenuOpcode.ITEM_FIRST_OPTION.getId(),
                                varrockTele.getIndex(), 9764864, false);
                        utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                        tickDelay = utils.getRandomIntBetweenRange(3, 5);
                        threadFix = true;
                        return;
                    } else {
                        log.info("Out of Varrock Teleports");
                        run = false;
                    }
                }
                threadFix = true;
                return;
            }

            if (utils.inventoryContains(ItemID.PURE_ESSENCE) && utils.inventoryContains(ItemID.WATER_TALISMAN) && utils.inventoryContains(ItemID.WATER_RUNE)) {
                if (clickedRuins) {
                    return;
                }
                GameObject mysteriousRuins = utils.findNearestGameObject(34816);
                if (mysteriousRuins != null) {
                    status = "Mysterious Ruins";
                    log.info("Entering Mysterious Ruins");
                    targetMenu = new MenuEntry("Enter", "Mysterious ruins", 34816, MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(),
                            mysteriousRuins.getSceneMinLocation().getX(), mysteriousRuins.getSceneMinLocation().getY(), false);
                    utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                    tickDelay = utils.getRandomIntBetweenRange(3, 5);
                    clickedRuins = true;
                } else {
                    status = "Walking to ruins";
                    log.info("Walking to Ruins");
                    walkToRuins();
                }
                threadFix = true;
                return;
            }

            if (!utils.isBankOpen()) {
                status = "Opening Bank";
                log.info("Opening Bank");
                openBank();
                threadFix = true;
                return;
            }
        }
        beforeLoc = client.getLocalPlayer().getLocalLocation(); // Updates location so that utils.ismoving(beforeloc) works
        threadFix = true;
    }

    /*
     * Other functions
     */

    public void walkToRuins() {
        WorldPoint aroundTreesAtMysteriousRuins = new WorldPoint(3296, 3463, 0);
        GameObject oakTree = utils.findNearestGameObjectWithin(aroundTreesAtMysteriousRuins, 6, 1278, 1278);
        if (oakTree != null) {
            targetMenu = new MenuEntry("Chop down", "Oak", oakTree.getId(), MenuOpcode.GAME_OBJECT_FIRST_OPTION.getId(),
                    oakTree.getSceneMinLocation().getX(), oakTree.getSceneMinLocation().getY(), false);
            utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
        } else {
            log.info("Can't find Oak Tree");
        }
    }

    public void handleBank() {
        if (utils.inventoryContains(ItemID.MUD_RUNE)) {
            status = "Depositing Mud Runes";
            log.info("Depositing Mud Runes");
            WidgetItem mudRune = utils.getInventoryWidgetItem(ItemID.MUD_RUNE);
            if (mudRune != null) {
                targetMenu = new MenuEntry("", "", 8, MenuOpcode.CC_OP.getId(), mudRune.getIndex(),
                        983043, false);
                utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                tickDelay = utils.getRandomIntBetweenRange(0, 1);
            }
        } else if (!utils.isItemEquipped(BINDING_NECKLACE)) {
            if (!utils.inventoryContains(ItemID.BINDING_NECKLACE)) {
                status = "Withdrawing Binding Necklace";
                log.info("Withdrawing Binding Necklace");
                Widget bindingNeck = utils.getBankItemWidget(ItemID.BINDING_NECKLACE);
                if (bindingNeck != null) {
                    targetMenu = new MenuEntry("", "", 1, MenuOpcode.CC_OP.getId(), bindingNeck.getIndex(),
                            786444, false);
                    utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                    tickDelay = utils.getRandomIntBetweenRange(0, 1);
                } else {
                    log.info("Out of Binding Necklaces");
                    run = false;
                }
            } else {
                status = "Equiping Binding Necklace";
                log.info("Equiping Binding Necklace");
                WidgetItem bindingNeck = utils.getInventoryWidgetItem(ItemID.BINDING_NECKLACE);
                if (bindingNeck != null) {
                    targetMenu = new MenuEntry("Wear", "Binding necklace", 9, MenuOpcode.CC_OP.getId(), bindingNeck.getIndex(),
                            983043, false);
                    utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                    tickDelay = utils.getRandomIntBetweenRange(0, 1);
                }
            }
        } else if (client.getEnergy() < 60) {
            if (!utils.inventoryContains(STAMINA_POTIONS)) {
                status = "Withdrawing Stamina potion";
                log.info("Withdrawing Stamina potion");
                Widget stamPot = utils.getBankItemWidgetAnyOf(STAMINA_POTIONS);
                if (stamPot != null) {
                    targetMenu = new MenuEntry("", "", 1, MenuOpcode.CC_OP.getId(), stamPot.getIndex(),
                            786444, false);
                    utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                    tickDelay = utils.getRandomIntBetweenRange(0, 1);
                } else {
                    log.info("Out of Stamina Potions");
                    run = false;
                }
            } else {
                status = "Drinking Stamina Potion";
                log.info("Drinking Stamina Potion");
                WidgetItem stamPot = utils.getInventoryWidgetItem(STAMINA_POTIONS);
                if (stamPot != null) {
                    targetMenu = new MenuEntry("Drink", "Stamina potion", 9, MenuOpcode.CC_OP.getId(),
                            stamPot.getIndex(), 983043, false);
                    utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                    tickDelay = utils.getRandomIntBetweenRange(0, 1);
                }
            }
        } else if (utils.inventoryContains(STAMINA_POTIONS) || utils.inventoryContains(ItemID.VIAL)) {
            status = "Depositing Stamina Potion";
            log.info("Depositing Stamina Potion or vial");
            if (utils.inventoryContains(STAMINA_POTIONS)) {
                WidgetItem stamPotUsed = utils.getInventoryWidgetItem(STAMINA_POTIONS);
                if (stamPotUsed != null) {
                    targetMenu = new MenuEntry("Deposit-All", "Stamina potion(3)", 8, MenuOpcode.CC_OP.getId(), stamPotUsed.getIndex(),
                            983043, false);
                    utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                    tickDelay = utils.getRandomIntBetweenRange(0, 1);
                }
            } else if (utils.inventoryContains(ItemID.VIAL)) {
                status = "Depositing Vial";
                WidgetItem vialUsed = utils.getInventoryWidgetItem(ItemID.VIAL);
                if (vialUsed != null) {
                    targetMenu = new MenuEntry("Deposit-All", "Vial", 8, MenuOpcode.CC_OP.getId(), vialUsed.getIndex(),
                            983043, false);
                    utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                    tickDelay = utils.getRandomIntBetweenRange(0, 1);
                }
            }
        } else if (!utils.inventoryContains(ItemID.WATER_TALISMAN)) {
            status = "Withdrawing 1 Water Talisman";
            log.info("Withdrawing 1 Water Talisman");
            Widget waterTalisman = utils.getBankItemWidget(ItemID.WATER_TALISMAN);
            if (waterTalisman != null) {
                targetMenu = new MenuEntry("", "", 1, MenuOpcode.CC_OP.getId(), waterTalisman.getIndex(),
                        786444, false);
                utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                tickDelay = utils.getRandomIntBetweenRange(0, 1);
            } else {
                log.info("Out of water talisman");
                run = false;
            }
        } else if (!utils.inventoryContains(ItemID.PURE_ESSENCE)) {
            status = "Withdrawing Pure Essence";
            log.info("Withdrawing pure essence");
            Widget pureEssence = utils.getBankItemWidget(ItemID.PURE_ESSENCE);
            if (pureEssence != null) {
                targetMenu = new MenuEntry("", "", 7, MenuOpcode.CC_OP.getId(), pureEssence.getIndex(),
                        786444, false);
                utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
                tickDelay = utils.getRandomIntBetweenRange(0, 1);
            } else {
                log.info("Out of pure essence");
                run = false;
            }
        }
    }

    public long getRunesPH()
    {
        Duration timeSinceStart = Duration.between(botTimer, Instant.now());
        if (!timeSinceStart.isZero())
        {
            return (int) ((double) runesCrafted * (double) Duration.ofHours(1).toMillis() / (double) timeSinceStart.toMillis());
        }
        return 0;
    }

    public void openBank() {
        WorldPoint varrockEastBank = new WorldPoint(3256, 3420, 0);
        GameObject bank = utils.findNearestGameObjectWithin(varrockEastBank, 1, 10583);
        if (bank != null) {
            targetMenu = new MenuEntry("Bank", "Bank booth", bank.getId(), 4,
                    bank.getSceneMinLocation().getX(), bank.getSceneMinLocation().getY(), false);
            utils.delayClickRandomPointCenter(-75, 75, utils.getRandomIntBetweenRange(100, 200));
        } else {
            log.info("Bank is null");
        }
    }

    private boolean isAtMysteriousRuin() {
        GameObject mysteriousRuins = utils.findNearestGameObject(34816);
        return mysteriousRuins != null;
    }

    private boolean isInAltarRoom() {
        GameObject earthAltar = utils.findNearestGameObject(ObjectID.ALTAR_34763);
        if (earthAltar != null) {
            clickedRuins = false;
        }
        return earthAltar != null;
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
                initialLevel = client.getRealSkillLevel(Skill.RUNECRAFT);
                botTimer = Instant.now();
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

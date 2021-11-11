/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
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

import net.runelite.client.config.*;


@ConfigGroup("PestControl")
public interface PestControlConfig extends Config
{
	@ConfigTitle(
			keyName = "delayConfig",
			name = "Sleep Delay Configuration",
			description = "Configure how the bot handles sleep delays",
			position = 2
	)
	String delayConfig = "delayConfig";


	@Range(
			min = 0,
			max = 550
	)
	@ConfigItem(
			keyName = "sleepMin",
			name = "Sleep Min",
			description = "",
			position = 2,
			section = "delayConfig"
	)
	default int sleepMin()
	{
		return 60;
	}

	@Range(
			min = 0,
			max = 550
	)
	@ConfigItem(
			keyName = "sleepMax",
			name = "Sleep Max",
			description = "",
			position = 3,
			section = "delayConfig"
	)
	default int sleepMax()
	{
		return 350;
	}

	@Range(
			min = 0,
			max = 550
	)
	@ConfigItem(
			keyName = "sleepTarget",
			name = "Sleep Target",
			description = "",
			position = 4,
			section = "delayConfig"
	)
	default int sleepTarget()
	{
		return 100;
	}

	@Range(
			min = 0,
			max = 550
	)
	@ConfigItem(
			keyName = "sleepDeviation",
			name = "Sleep Deviation",
			description = "",
			position = 5,
			section = "delayConfig"
	)
	default int sleepDeviation()
	{
		return 10;
	}

	@ConfigItem(
			keyName = "sleepWeightedDistribution",
			name = "Sleep Weighted Distribution",
			description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
			position = 6,
			section = "delayConfig"
	)
	default boolean sleepWeightedDistribution()
	{
		return false;
	}

	@ConfigItem(
			keyName = "tickDelayMin",
			name = "Game Tick Min",
			description = "",
			position = 8,
			section = "delayTickConfig"
	)
	default int tickDelayMin()
	{
		return 1;
	}

	@Range(
			min = 0,
			max = 10
	)
	@ConfigItem(
			keyName = "tickDelayMax",
			name = "Game Tick Max",
			description = "",
			position = 9,
			section = "delayTickConfig"
	)
	default int tickDelayMax()
	{
		return 3;
	}

	@Range(
			min = 0,
			max = 10
	)
	@ConfigItem(
			keyName = "tickDelayTarget",
			name = "Game Tick Target",
			description = "",
			position = 10,
			section = "delayTickConfig"
	)
	default int tickDelayTarget()
	{
		return 2;
	}

	@Range(
			min = 0,
			max = 10
	)
	@ConfigItem(
			keyName = "tickDelayDeviation",
			name = "Game Tick Deviation",
			description = "",
			position = 11,
			section = "delayTickConfig"
	)
	default int tickDelayDeviation()
	{
		return 1;
	}

	@ConfigItem(
			keyName = "tickDelayWeightedDistribution",
			name = "Game Tick Weighted Distribution",
			description = "Shifts the random distribution towards the lower end at the target, otherwise it will be an even distribution",
			position = 12,
			section = "delayTickConfig"
	)
	default boolean tickDelayWeightedDistribution()
	{
		return false;
	}

	@ConfigItem(
			keyName = "instructionsTitle",
			name = "Instructions",
			description = "Instructions Title",
			position = 13
	)
	default boolean instructionsTitle()
	{
		return false;
	}

	@ConfigItem(
			keyName = "instructions",
			name = "",
			description = "Instructions. Don't enter anything into this field",
			position = 14,
			section = "instructionsTitle"
	)
	default String instructions()
	{
		return "Stand somewhere on Pest Control Island when pressing the hotkey you have bound in the config to start the plugin.\n\nThis plugin works best with ranged but melee also works.";
	}

	@ConfigTitle(
			keyName = "firstTitle",
			name = "Pest Control Config",
			description = "",
			position = 15
	)
	String firstTitle = "firstTitle";

	@ConfigItem(
			keyName = "boatSelect",
			name = "Boat",
			description = "Choose which boat to enter",
			section = "firstTitle",
			position = 16
	)
	default Boat boatSelect()
	{
		return Boat.NOVICE;
	}

	@ConfigItem(
			keyName = "toggleKey",
			name = "Toggle Key",
			description = "Key to toggle on / off",
			section = "firstTitle",
			position = 17
	)
	default Keybind toggleKey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
			keyName = "extraTitle",
			name = "Extra Config",
			description = "Don't use these unless requested.",
			position = 51
	)
	default boolean extraTitle()
	{
		return false;
	}

	@ConfigItem(
			keyName = "logParams",
			name = "Log MenuEntry Parameters",
			description = "Logs MenuEntry options",
			section = "extraTitle",
			position = 52
	)
	default boolean logParams()
	{
		return false;
	}
}
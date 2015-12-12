package com.brianstempin.vindiniumclient.bot.advanced.murderbot;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.BotUtils;
import com.brianstempin.vindiniumclient.bot.advanced.Mine;
import com.brianstempin.vindiniumclient.bot.advanced.murderbot.AdvancedMurderBot.DijkstraResult;
import com.brianstempin.vindiniumclient.dto.GameState;

public class AttendedMineDecisioner implements
		Decision<AdvancedMurderBot.GameContext, BotMove> {

	private static final Logger logger = LogManager
			.getLogger(UnattendedMineDecisioner.class);

	private final Decision<AdvancedMurderBot.GameContext, BotMove> noGoodMineDecision;

	public AttendedMineDecisioner(
			Decision<AdvancedMurderBot.GameContext, BotMove> noGoodMineDecision) {
		this.noGoodMineDecision = noGoodMineDecision;
	}

	@Override
	public BotMove makeDecision(AdvancedMurderBot.GameContext context) {

		Map<GameState.Position, DijkstraResult> dijkstraResultMap = context
				.getDijkstraResultMap();
		GameState.Hero me = context.getGameState().getMe();

		// A good target is the closest unattended mine
		Mine targetMine = null;

		for (Mine mine : context.getGameState().getMines().values()) {
			DijkstraResult mineDijkstraResult = dijkstraResultMap.get(mine
					.getPosition());
			if (targetMine == null && mineDijkstraResult != null) {
				if (mine.getOwner() != null)
					if (mine.getOwner().getId() != me.getId() || !mine.getOwner().getName().equalsIgnoreCase("akshays"))
						targetMine = mine;
			} else if (mineDijkstraResult != null
					&& dijkstraResultMap.get(targetMine.getPosition())
							.getDistance() > dijkstraResultMap.get(
							mine.getPosition()).getDistance()
					&& (mine.getOwner() == null || mine.getOwner().getId() != me
							.getId())) {
				targetMine = mine;
			}
		}

		if (targetMine != null) {

			/*
			 * // Is it safe to move?
			 * if(BotUtils.getHeroesAround(context.getGameState(),
			 * context.getDijkstraResultMap(), 2).size() > 0) {
			 * logger.info("Mine found, but another hero is too close."); return
			 * noGoodMineDecision.makeDecision(context); }
			 */

			GameState.Position currentPosition = targetMine.getPosition();
			DijkstraResult currentResult = dijkstraResultMap
					.get(currentPosition);
			while (currentResult.getDistance() > 1) {
				currentPosition = currentResult.getPrevious();
				currentResult = dijkstraResultMap.get(currentPosition);
			}

			logger.info("Found a suitable non-abandoned mine to go after");
			return BotUtils.directionTowards(context.getGameState().getMe()
					.getPos(), currentPosition);
		} else {
			logger.info("No suitable mine found.  Deferring.");
			return noGoodMineDecision.makeDecision(context);
		}
	}
}

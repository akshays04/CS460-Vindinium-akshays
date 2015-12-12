package com.brianstempin.vindiniumclient.bot.advanced.murderbot;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.BotUtils;
import com.brianstempin.vindiniumclient.bot.advanced.Pub;
import com.brianstempin.vindiniumclient.dto.GameState;

public class StayDecisioner implements Decision<AdvancedMurderBot.GameContext, BotMove> {

    private static final Logger logger = LogManager.getLogger(StayDecisioner.class);
    
    private final Decision<AdvancedMurderBot.GameContext, BotMove> noStayDecision;

    public StayDecisioner(Decision<AdvancedMurderBot.GameContext, BotMove> noStayDecision) {
        this.noStayDecision = noStayDecision;
    }

    @Override
    public BotMove makeDecision(AdvancedMurderBot.GameContext context) {
        logger.info("Need to heal; running to nearest pub.");

        Map<GameState.Position, AdvancedMurderBot.DijkstraResult> dijkstraResultMap = context.getDijkstraResultMap();

        // Run to the nearest pub
        Pub nearestPub = null;
        AdvancedMurderBot.DijkstraResult nearestPubDijkstraResult = null;
        for(Pub pub : context.getGameState().getPubs().values()) {
            AdvancedMurderBot.DijkstraResult dijkstraToPub = dijkstraResultMap.get(pub.getPosition());
            if(dijkstraToPub != null) {
                if(nearestPub == null || nearestPubDijkstraResult.getDistance() >
                    dijkstraToPub.getDistance()) {
                    nearestPub = pub;
                    nearestPubDijkstraResult = dijkstraResultMap.get(pub.getPosition());
                }
            }
        }

        if(nearestPub == null && context.getGameState().getMe().getMineCount()>= (context.getGameState().getMines().size()/2))
            return BotMove.STAY;
        else
        	return noStayDecision.makeDecision(context);
    }
}

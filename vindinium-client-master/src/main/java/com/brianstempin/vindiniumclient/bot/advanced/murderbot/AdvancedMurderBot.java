package com.brianstempin.vindiniumclient.bot.advanced.murderbot;

import com.brianstempin.vindiniumclient.bot.BotMove;
import com.brianstempin.vindiniumclient.bot.advanced.AdvancedBot;
import com.brianstempin.vindiniumclient.bot.advanced.AdvancedGameState;
import com.brianstempin.vindiniumclient.bot.advanced.Vertex;
import com.brianstempin.vindiniumclient.dto.GameState;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * An improvement upon com.brianstempin.vindiniumClient.bot.simple.MurderBot
 *
 * This class uses a built-in static method to perform the path search via Dijkstra and uses a simple version of
 * behavior trees to determine its next action.
 */
public class AdvancedMurderBot implements AdvancedBot {

    public static class GameContext {
        private final AdvancedGameState gameState;
        private final Map<GameState.Position, DijkstraResult> dijkstraResultMap;

        public GameContext(AdvancedGameState gameState, Map<GameState.Position, DijkstraResult> dijkstraResultMap) {
            this.gameState = gameState;
            this.dijkstraResultMap = dijkstraResultMap;
        }

        public AdvancedGameState getGameState() {
            return gameState;
        }

        public Map<GameState.Position, DijkstraResult> getDijkstraResultMap() {
            return dijkstraResultMap;
        }
    }

    /**
     * Represents the result of a Dijkstra search for a given position
     */
    public static class DijkstraResult {
        private int distance;
        private GameState.Position previous;

        public DijkstraResult(int distance, GameState.Position previous) {
            this.distance = distance;
            this.previous = previous;
        }

        public int getDistance() {
            return distance;
        }

        public GameState.Position getPrevious() {
            return previous;
        }
    }

    /*public static synchronized Map<GameState.Position, DijkstraResult> oldDijkstraSearch(AdvancedGameState gameState) {
        Map<GameState.Position, DijkstraResult> result = new HashMap<>();

        DijkstraResult startingResult = new DijkstraResult(0, null);
        Queue<GameState.Position> queue = new ArrayBlockingQueue<>(gameState.getBoardGraph().size());
        queue.add(gameState.getMe().getPos());
        result.put(gameState.getMe().getPos(), startingResult);

        while(!queue.isEmpty()) {
            GameState.Position currentPosition = queue.poll();
            DijkstraResult currentResult = result.get(currentPosition);
            Vertex currentVertext = gameState.getBoardGraph().get(currentPosition);

            // If there's a bot here, then this vertex goes nowhere
            if(gameState.getHeroesByPosition().containsKey(currentPosition)
                    && !currentPosition.equals(gameState.getMe().getPos()))
                continue;

            int distance = currentResult.getDistance() + 1;

            for(Vertex neighbor : currentVertext.getAdjacentVertices()) {
                DijkstraResult neighborResult = result.get(neighbor.getPosition());
                if(neighborResult == null) {
                    neighborResult = new DijkstraResult(distance, currentPosition);
                    result.put(neighbor.getPosition(), neighborResult);
                    queue.remove(neighbor.getPosition());
                    queue.add(neighbor.getPosition());
                } else if(neighborResult.distance > distance) {
                    DijkstraResult newNeighborResult = new DijkstraResult(distance, currentPosition);
                    result.put(neighbor.getPosition(), newNeighborResult);
                    queue.remove(neighbor.getPosition());
                    queue.add(neighbor.getPosition());
                }
            }
        }

        return result;
    }*/
    
    public static synchronized Map<GameState.Position, DijkstraResult> dijkstraSearch(AdvancedGameState gameState) {
        Map<GameState.Position, DijkstraResult> result = new HashMap<>();
        final Map<GameState.Position, Double> distanceMap = new HashMap<GameState.Position, Double>();
        //Map<GameState.Position, Boolean> inQueue = new HashMap<>(); 
        Map<GameState.Position, Boolean> visitedMap = new HashMap<>();

        DijkstraResult startingResult = new DijkstraResult(0, null);
        //Queue<GameState.Position> queue = new ArrayBlockingQueue<>(gameState.getBoardGraph().size());
        //Queue<GameState.Position> pQueue = new PriorityQueue<GameState.Position>(gameState.getBoardGraph().size());
        PriorityBlockingQueue<GameState.Position> pQueue = new PriorityBlockingQueue<GameState.Position>(gameState.getBoardGraph().size(),
    	        new Comparator<GameState.Position>()
                {
                    public int compare(GameState.Position p, GameState.Position q)
                    {
                        return Double.compare(distanceMap.get(p), distanceMap.get(q));
                    	
                    }
                } );
        distanceMap.put(gameState.getMe().getPos(), 0.0);
        //inQueue.put(gameState.getMe().getPos(), true);
        pQueue.add(gameState.getMe().getPos());
        result.put(gameState.getMe().getPos(), startingResult);

        
        double alt = 0;
        while(!pQueue.isEmpty()) {
            GameState.Position currentPosition = pQueue.poll();
            DijkstraResult currentResult = result.get(currentPosition);
            Vertex currentVertext = gameState.getBoardGraph().get(currentPosition);
            visitedMap.put(currentPosition, true);

            // If there's a bot here, then this vertex goes nowhere
            if(gameState.getHeroesByPosition().containsKey(currentPosition)
                    && !currentPosition.equals(gameState.getMe().getPos()))
                continue;

            int distance = currentResult.getDistance() + 1;

            for(Vertex neighbor : currentVertext.getAdjacentVertices()) {
            	if(!visitedMap.containsKey(neighbor.getPosition())){
            		DijkstraResult neighborResult = result.get(neighbor.getPosition());
            		if(neighborResult == null) {
            			alt = distance + heauristic(currentPosition, neighbor.getPosition());
                        distanceMap.put(neighbor.getPosition(), alt);
                        neighborResult = new DijkstraResult(distance, currentPosition);
                        result.put(neighbor.getPosition(), neighborResult);
                        pQueue.remove(neighbor.getPosition());
                        pQueue.add(neighbor.getPosition());
                    } else if(distanceMap.get(neighbor.getPosition()) > alt) {
                        DijkstraResult newNeighborResult = new DijkstraResult(distance, currentPosition);
                        result.put(neighbor.getPosition(), newNeighborResult);
                        distanceMap.put(neighbor.getPosition(), alt);
                        pQueue.remove(neighbor.getPosition());
                        pQueue.add(neighbor.getPosition());
                    }
            		
            	}
            }
        }

        return result;
    }
    
    public static double heauristic(GameState.Position node, GameState.Position dest) {
		double x = (Math.pow((node.getX() - dest.getX()), 2));
		double y = (Math.pow((node.getY() - dest.getY()), 2));
		return Math.sqrt(x + y);
	}

    private final Decision<GameContext, BotMove> decisioner;

    public AdvancedMurderBot() {

        // Chain decisioners together
        SquatDecisioner squatDecisioner = new SquatDecisioner();
        UnattendedMineDecisioner unattendedMineDecisioner = new UnattendedMineDecisioner(squatDecisioner);
        //BotTargetingDecisioner botTargetingDecisioner = new BotTargetingDecisioner(unattendedMineDecisioner);
        AttendedMineDecisioner attendedMineDecisioner = new AttendedMineDecisioner(unattendedMineDecisioner);
        BotTargetingDecisioner botTargetingDecisioner = new BotTargetingDecisioner(attendedMineDecisioner);
        EnRouteLootingDecisioner enRouteLootingDecisioner = new EnRouteLootingDecisioner(botTargetingDecisioner);

        HealDecisioner healDecisioner = new HealDecisioner();
        CombatOutcomeDecisioner combatOutcomeDecisioner = new CombatOutcomeDecisioner(botTargetingDecisioner,
                botTargetingDecisioner);
        CombatEngagementDecisioner combatEngagementDecisioner = new CombatEngagementDecisioner(combatOutcomeDecisioner,
                healDecisioner);
        BotWellnessDecisioner botWellnessDecisioner = new BotWellnessDecisioner(enRouteLootingDecisioner, combatEngagementDecisioner);
        StayDecisioner stayDecisioner = new StayDecisioner(botWellnessDecisioner);

        //this.decisioner = botWellnessDecisioner;
        this.decisioner = stayDecisioner;
    }

    @Override
    public BotMove move(AdvancedGameState gameState) {

        Map<GameState.Position, DijkstraResult> dijkstraResultMap = dijkstraSearch(gameState);

        GameContext context = new GameContext(gameState, dijkstraResultMap);
        return this.decisioner.makeDecision(context);
    }

    @Override
    public void setup() {
        // No-op
    }

    @Override
    public void shutdown() {
        // No-op
    }
}

// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class TestMaker {

    private static final int NUM_GAMES = 20;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<Future<GameResult>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_GAMES; i++) {
            futures.add(executor.submit(new GameTask(i)));
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.MINUTES);

        int xWins = 0;
        int oWins = 0;
        int draws = 0;

        for (Future<GameResult> f : futures) {
            GameResult result = f.get();
            if (result.winner == Player.X) {
                xWins++;
            } else if (result.winner == Player.O) {
                oWins++;
            } else {
                draws++;
            }
            System.out.println("Game " + result.gameId + " finished. Winner: " +
                    (result.winner == null ? "Draw" : result.winner));
        }

        System.out.println("Summary of " + NUM_GAMES + " games:");
        System.out.println("X wins: " + xWins);
        System.out.println("O wins: " + oWins);
        System.out.println("Draws: " + draws);
    }

    
    static class GameTask implements Callable<GameResult> {
        private final int gameId;

        GameTask(int gameId) {
            this.gameId = gameId;
        }

        @Override
        public GameResult call() {
            
            Board board = new Board();
            Player nextPlayer = Player.X; 
            AIPlayer aiPlayerX = new AIPlayer(Player.X);
            AIPlayer aiPlayerO = new AIPlayer(Player.O);

            Game game = new Game(board, nextPlayer, false, true);
            game.aiPlayerX = aiPlayerX;
            game.aiPlayerO = aiPlayerO;

            while (!game.isGameOver) {
                if (nextPlayer == Player.X) {
                    int aiMove = aiPlayerX.chooseBestMove(board);
                    board.makeMove(aiMove, Player.X);
                } else {
                    int aiMove = aiPlayerO.chooseBestMove(board);
                    board.makeMove(aiMove, Player.O);
                }

                
                if (board.hasWon(nextPlayer)) {
                    game.isGameOver = true;
                    game.winner = nextPlayer;
                    game.winningLine = board.getWinningLine(nextPlayer);
                } else if (board.isFull()) {
                    game.isGameOver = true;
                    game.winner = null; 
                }

                nextPlayer = nextPlayer.other();
                game.nextPlayer = nextPlayer;
            }

            return new GameResult(gameId, game.winner);
        }
    }

    static class GameResult {
        int gameId;
        Player winner;

        GameResult(int gameId, Player winner) {
            this.gameId = gameId;
            this.winner = winner;
        }
    }
}
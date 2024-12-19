// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;

public class TTT3 {

    private static final int GLOBAL_TMS = 50;
    private static int PORT = 7474;
    private static final String BOARD_STATE_FILE = "board_state.txt";

    static class GameConfig {
        Board initialBoard;
        Player nextPlayer;
        boolean isPlayingAgainstAI;
        boolean isTwoPlayerGame;
        Player humanPlayer;
        AIPlayer aiPlayerX;
        AIPlayer aiPlayerO;

        public GameConfig(Board board, Player nextPlayer,
                          boolean isPlayingAgainstAI, boolean isTwoPlayerGame,
                          Player humanPlayer, AIPlayer aiPlayerX, AIPlayer aiPlayerO) {
            this.initialBoard = board;
            this.nextPlayer = nextPlayer;
            this.isPlayingAgainstAI = isPlayingAgainstAI;
            this.isTwoPlayerGame = isTwoPlayerGame;
            this.humanPlayer = humanPlayer;
            this.aiPlayerX = aiPlayerX;
            this.aiPlayerO = aiPlayerO;
        }
    }

    public static void main(String[] args) {

        GameConfig config = initialSetup(args);

        while (true) {
            try {
                runGame(config);
                Thread.sleep(GLOBAL_TMS * 50);
            } catch (Throwable t) {
                System.out.println("An unexpected error occurred: " + t.getMessage());
                t.printStackTrace();
                System.out.println("Attempting to recover from last saved state...");
                PORT++;
            }
        }
    }

    private static GameConfig initialSetup(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Board board;
        Player nextPlayer;

        if (args.length == 1 && (!args[0].equalsIgnoreCase("reset"))) {
            String boardString = args[0];
            try {
                board = Board.valueOf(boardString);
                int xCount = Bit.countOnes(board.xPositions);
                int oCount = Bit.countOnes(board.oPositions);

                if (xCount == oCount) {
                    nextPlayer = Player.X;
                } else if (xCount == oCount + 1) {
                    nextPlayer = Player.O;
                } else {
                    System.out.println("The board is not valid: incorrect number of moves. Starting fresh game...");
                    board = new Board();
                    nextPlayer = Player.X;
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid board configuration: " + e.getMessage() + ". Starting fresh game...");
                board = new Board();
                nextPlayer = Player.X;
            }
        } else {

            if(args.length == 1 && args[0].equalsIgnoreCase("reset")) {
                clearSavedBoardState();
            }
            board = new Board();
            nextPlayer = Player.X;
        }

        System.out.println("Select game mode:");
        System.out.println("1. Player vs AI");
        System.out.println("2. Player vs Player");
        System.out.println("3. AI vs AI");
        System.out.print("Enter choice (1/2/3): ");

        int gameMode;
        try {
            gameMode = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Starting default Player vs AI game.");
            gameMode = 1;
        }

        AIPlayer aiPlayerX = null;
        AIPlayer aiPlayerO = null;
        Player humanPlayer = null;
        boolean isPlayingAgainstAI = false;
        boolean isTwoPlayerGame = false;

        switch (gameMode) {
            case 1:
                isPlayingAgainstAI = true;
                humanPlayer = null;
                System.out.println("Please open the web interface and choose your side (X or O).");
                break;
            case 2:
                isTwoPlayerGame = true;
                System.out.println("Starting a two-player game.");
                break;
            case 3:
                aiPlayerX = new AIPlayer(Player.X);
                aiPlayerO = new AIPlayer(Player.O);
                System.out.println("Starting an AI vs AI game.");
                break;
            default:
                System.out.println("Invalid choice. Defaulting to Player vs AI.");
                isPlayingAgainstAI = true;
                humanPlayer = null;
                System.out.println("Please open the web interface and choose your side (X or O).");
                break;
        }

        scanner.close();

        return new GameConfig(board, nextPlayer, isPlayingAgainstAI, isTwoPlayerGame, humanPlayer, aiPlayerX, aiPlayerO);
    }

    private static void runGame(GameConfig config) {
        while (true) {
            Board board = loadBoardState();
            Player nextPlayer;

            if (board != null) {
                int xCount = Bit.countOnes(board.xPositions);
                int oCount = Bit.countOnes(board.oPositions);
                if (xCount == oCount) {
                    nextPlayer = Player.X;
                } else if (xCount == oCount + 1) {
                    nextPlayer = Player.O;
                } else {
                    board = new Board();
                    nextPlayer = Player.X;
                }
                System.out.println("Recovered board from saved state. Resuming...");
            } else {
                board = new Board();
                nextPlayer = config.nextPlayer;
            }

            Game game = new Game(board, nextPlayer, config.isPlayingAgainstAI,
                    (config.aiPlayerX != null || config.aiPlayerO != null));
            game.aiPlayerX = config.aiPlayerX;
            game.aiPlayerO = config.aiPlayerO;
            game.humanPlayer = config.humanPlayer;

            NettyHandler netHandler;
            String url = "http://localhost:" + PORT;
            try {
                netHandler = new NettyHandler(game, PORT, config);
                netHandler.start();
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(new URI(url));
                    } else {
                        System.out.println("BROWSE action is not supported!");
                    }
                } else {
                    System.out.println("Desktop is not supported on this platform!");
                }
            } catch (IOException | URISyntaxException e) {
                System.out.println("Failed to start the server: " + e.getMessage());
                return;
            }

            if (config.isPlayingAgainstAI && config.humanPlayer == null) {
                System.out.println("Waiting for player side choice via the web...");
                while (config.humanPlayer == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("Interrupted while waiting for player side choice.");
                        return;
                    }
                }

                game.humanPlayer = config.humanPlayer;
                Player aiSide = game.humanPlayer.other();
                if (aiSide == Player.X && config.aiPlayerX == null) {
                    config.aiPlayerX = new AIPlayer(Player.X);
                } else if (aiSide == Player.O && config.aiPlayerO == null) {
                    config.aiPlayerO = new AIPlayer(Player.O);
                }
                game.aiPlayerX = config.aiPlayerX;
                game.aiPlayerO = config.aiPlayerO;
                game.aiSide = aiSide;
                System.out.println("Player side chosen: " + config.humanPlayer);
            }

            // Game loop
            while (true) {
                System.out.println("\nMove #" + (Bit.countOnes(board.xPositions) + Bit.countOnes(board.oPositions) + 1));
                System.out.println("Current board state:\n");
                System.out.println(board);
                System.out.println("\nNext player: " + game.nextPlayer);

                if (game.isGameOver) {
                    break;
                }

                if ((game.nextPlayer == game.humanPlayer || config.isTwoPlayerGame) && !game.isGameOver) {
                    System.out.println("Waiting for move via the web visualizer...");
                    waitForMove(netHandler);
                    int move = netHandler.getSelectedMove();
                    netHandler.resetSelectedMove();

                    if (board.isValidMove(move)) {
                        board.makeMove(move, game.nextPlayer);
                        saveBoardState(board);
                    } else {
                        System.out.println("Invalid move selected: " + move + ". Please try again.");
                        continue;
                    }
                } else if (!game.isGameOver) {

                    AIPlayer currentAIPlayer = (game.nextPlayer == Player.X) ? config.aiPlayerX : config.aiPlayerO;
                    if (currentAIPlayer != null) {
                        System.out.println("AI (" + game.nextPlayer + ") is thinking...");
                        int aiMove = currentAIPlayer.chooseBestMove(board);
                        board.makeMove(aiMove, game.nextPlayer);
                        System.out.println("AI (" + game.nextPlayer + ") chose position: " + aiMove);
                        saveBoardState(board);

                        if (config.aiPlayerX != null && config.aiPlayerO != null) {
                            try {
                                Thread.sleep(GLOBAL_TMS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                System.out.println("Game interrupted.");
                                netHandler.stop(0);
                                return;
                            }
                        }
                    } else {
                        System.out.println("No AI player found for " + game.nextPlayer + ".");
                        continue;
                    }
                }

                if (board.hasWon(game.nextPlayer)) {
                    System.out.println("Player " + game.nextPlayer + " wins!");
                    game.isGameOver = true;
                    game.winner = game.nextPlayer;
                    game.winningLine = board.getWinningLine(game.nextPlayer);
                    break;
                } else if (board.isFull()) {
                    System.out.println("It's a draw!");
                    game.isGameOver = true;
                    game.winner = null;
                    break;
                }

                game.nextPlayer = game.nextPlayer.other();
            }

            game.isGameOver = true;
            game.nextPlayer = game.nextPlayer.other();
            if (config.isPlayingAgainstAI) {
                config.humanPlayer = null;
                config.aiPlayerX = null;
                config.aiPlayerO = null;
            }

            try {
                Thread.sleep(GLOBAL_TMS * 2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted while waiting to stop the server.");
            }
            System.out.println("Game over.");
            clearSavedBoardState();
            netHandler.waitForRestart();
            netHandler.stop(0);
        }
    }

    private static void waitForMove(NettyHandler netHandler) {
        while (netHandler.getSelectedMove() == -1) {
            try {
                Thread.sleep(GLOBAL_TMS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Game interrupted.");
                netHandler.stop(0);
                return;
            }
        }
    }

    private static void saveBoardState(Board board) {
        try (FileWriter fw = new FileWriter(BOARD_STATE_FILE)) {
            fw.write(board.toString());
        } catch (IOException e) {
            System.out.println("Failed to save board state: " + e.getMessage());
        }
    }

    private static Board loadBoardState() {
        File file = new File(BOARD_STATE_FILE);
        if (!file.exists()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            int c;
            while ((c = br.read()) != -1) {
                sb.append((char)c);
            }
        } catch (IOException e) {
            System.out.println("Failed to load saved board state: " + e.getMessage());
            return null;
        }

        String boardStr = sb.toString();
        if (boardStr.trim().isEmpty()) {
            return null;
        }
        try {
            return Board.valueOf(boardStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Saved board state invalid. Starting fresh...");
            return null;
        }
    }

    private static void clearSavedBoardState() {
        File file = new File(BOARD_STATE_FILE);
        if (file.exists()) {
            if (!file.delete()) {
                System.out.println("Warning: Could not delete saved board state file.");
            }
        }
    }
}

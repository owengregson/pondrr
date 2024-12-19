// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

public class Game {
    public Board board;
    public Player nextPlayer;
    public boolean isGameOver;
    public Player winner;
    public Line winningLine;
    public boolean isPlayingAgainstAI;
    public AIPlayer aiPlayerX;
    public AIPlayer aiPlayerO;
    public Player humanPlayer;
    public Player aiSide;

    public Game(Board board, Player nextPlayer, boolean isPlayingAgainstAI, boolean hasAIPlayer) {
        this.board = board;
        this.nextPlayer = nextPlayer;
        this.isPlayingAgainstAI = isPlayingAgainstAI;
        this.isGameOver = false;
        this.winner = null;
        this.winningLine = null;
        this.aiPlayerX = null;
        this.aiPlayerO = null;
        this.humanPlayer = null;
        this.aiSide = null;
    }

    public String stats() {
        if (this.aiPlayerX != null) {
            return this.aiPlayerX.stats();
        } else if (this.aiPlayerO != null) {
            return this.aiPlayerO.stats();
        } else {
            return "Pending...";
        }
    }
}
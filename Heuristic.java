// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import java.util.*;

public class Heuristic {
    // central positions
    private static final int BOARD_SIZE = Coordinate.NCubed;
    private static final int LINE_LENGTH = Coordinate.N;

    private static final Set<Integer> CENTER_POSITIONS = new HashSet<>(Arrays.asList(
            21, 22, 25, 26,
            29, 30, 33, 34
    ));

    // middle plane corner positions
    private static final Set<Integer> PCORNER_POSITIONS = new HashSet<>(Arrays.asList(
            16, 19, 28, 31,
            32, 35, 44, 47
    ));

    // board corner positions
    private static final Set<Integer> BCORNER_POSITIONS = new HashSet<>(Arrays.asList(
            0, 3, 12, 15,
            48, 51, 60, 63
    ));

    private static long computePositionMask(Set<Integer> positions) {
        long mask = 0L;
        for (int pos : positions) {
            mask = Bit.set(mask, pos);
        }
        return mask;
    }

    private static final long CENTER_POSITIONS_MASK = computePositionMask(CENTER_POSITIONS);
    private static final long PCORNER_POSITIONS_MASK = computePositionMask(PCORNER_POSITIONS);
    private static final long BCORNER_POSITIONS_MASK = computePositionMask(BCORNER_POSITIONS);

    private static final long FULL_MASK = -1L >>> (64 - BOARD_SIZE);
    private static final int[][] LINES_BY_POSITION = new int[BOARD_SIZE][];

    static {
        // Pre-compute all lines that touch each position. This greatly speeds up
        // fork/double-threat detection because we avoid repeatedly walking the
        // global Line list.
        List<Integer>[] temp = new ArrayList[BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            temp[i] = new ArrayList<>();
        }

        for (int i = 0; i < Line.lines.length; i++) {
            long mask = Line.lines[i].positions();
            long bits = mask;
            while (bits != 0) {
                int pos = Long.numberOfTrailingZeros(bits);
                temp[pos].add(i);
                bits &= (bits - 1);
            }
        }

        for (int i = 0; i < BOARD_SIZE; i++) {
            LINES_BY_POSITION[i] = temp[i].stream().mapToInt(Integer::intValue).toArray();
        }
    }


    /**
     * Evaluates the board from the perspective of the specified player.
     *
     * @param board  The current board state.
     * @param player The player for whom the evaluation is being performed.
     * @return An integer score representing the desirability of the board state.
     */
    public static long evaluate(Board board, Player player, Weights w) {
        long score = 0;
        Player opponent = player.other();

        long playerPositions = (player == Player.X) ? board.xPositions : board.oPositions;
        long opponentPositions = (player == Player.X) ? board.oPositions : board.xPositions;
        long occupiedPositions = playerPositions | opponentPositions;

        for (Line line : Line.lines) {
            long linePositions = line.positions();

            long playerLinePositions = linePositions & playerPositions;
            long opponentLinePositions = linePositions & opponentPositions;

            int playerCount = Bit.countOnes(playerLinePositions);
            int opponentCount = Bit.countOnes(opponentLinePositions);
            int emptyCount = LINE_LENGTH - playerCount - opponentCount;

            // blocked lines do not contribute; they cannot yield a win for either side.
            if (playerCount > 0 && opponentCount > 0) {
                score -= w.BLOCKED_LINE_PENALTY;
                continue;
            }

            if (playerCount > 0) {
                int centerCount = Bit.countOnes(playerLinePositions & CENTER_POSITIONS_MASK);
                score += getScore(playerCount, centerCount, emptyCount, w);
            } else if (opponentCount > 0) {
                int centerCount = Bit.countOnes(opponentLinePositions & CENTER_POSITIONS_MASK);
                score -= getScore(opponentCount, centerCount, emptyCount, w) * w.OPPONENT_SCORE_MULTIPLIER;
            } else {
                // fully open lines favour the side to move slightly
                score += w.OPEN_LINE_BONUS;
            }

            if (opponentCount == LINE_LENGTH - 1 && emptyCount == 1) {
                score -= w.IMMEDIATE_THREAT_PENALTY;
            }

            if (playerCount == LINE_LENGTH - 1 && emptyCount == 1) {
                score += w.IMMEDIATE_WIN_BONUS;
            }
        }

        // positional control
        int playerCenterControl = Bit.countOnes(playerPositions & CENTER_POSITIONS_MASK);
        int opponentCenterControl = Bit.countOnes(opponentPositions & CENTER_POSITIONS_MASK);
        score += playerCenterControl * w.CENTER_CONTROL_MULTIPLIER;
        score -= opponentCenterControl * w.OPPONENT_CENTER_CONTROL_MULTIPLIER;

        int playerPCorners = Bit.countOnes(playerPositions & PCORNER_POSITIONS_MASK);
        int opponentPCorners = Bit.countOnes(opponentPositions & PCORNER_POSITIONS_MASK);
        score += playerPCorners * w.PCORNER_CONTROL_MULTIPLIER;
        score -= opponentPCorners * w.OPPONENT_PCORNER_CONTROL_MULTIPLIER;

        int playerBCorners = Bit.countOnes(playerPositions & BCORNER_POSITIONS_MASK);
        int opponentBCorners = Bit.countOnes(opponentPositions & BCORNER_POSITIONS_MASK);
        score += playerBCorners * w.BCORNER_CONTROL_MULTIPLIER;
        score -= opponentBCorners * w.OPPONENT_BCORNER_CONTROL_MULTIPLIER;

        // fork pressure and defensive awareness
        int playerForks = evaluatePotentialForks(playerPositions, occupiedPositions, opponentPositions);
        int opponentForks = evaluatePotentialForks(opponentPositions, occupiedPositions, playerPositions);
        score += (long) playerForks * w.PLAYER_FORKS_MULTIPLIER;
        score -= (long) opponentForks * w.OPPONENT_FORKS_MULTIPLIER;

        int playerDoubleThreats = evaluateDoubleThreats(playerPositions, opponentPositions, occupiedPositions);
        int opponentDoubleThreats = evaluateDoubleThreats(opponentPositions, playerPositions, occupiedPositions);
        score += (long) playerDoubleThreats * w.DOUBLE_THREAT_BONUS;
        score -= (long) opponentDoubleThreats * w.OPPONENT_DOUBLE_THREAT_PENALTY;

        int playerImmediateWinningSquares = countImmediateWinningSquares(playerPositions, opponentPositions, occupiedPositions);
        int opponentImmediateWinningSquares = countImmediateWinningSquares(opponentPositions, playerPositions, occupiedPositions);
        score += (long) playerImmediateWinningSquares * w.IMMEDIATE_WIN_SQUARE_BONUS;
        score -= (long) opponentImmediateWinningSquares * w.OPPONENT_IMMEDIATE_WIN_SQUARE_PENALTY;

        int blockedThreatIntersections = evaluateBlockedIntersections(playerPositions, opponentPositions);
        score += (long) blockedThreatIntersections * w.BLOCKED_THREAT_INTERSECTION_BONUS;

        // Opponent's ability to fork next move (prevents tunnel vision)
        int opponentPotentialForks = evaluateOpponentPotentialForks(board, opponent, occupiedPositions);
        score -= (long) opponentPotentialForks * w.OPPONENT_POTENTIAL_FORKS_PENALTY;

        // Mobility: prefer states with more options and space to maneuver.
        int mobility = Coordinate.NCubed - Bit.countOnes(occupiedPositions);
        score += mobility * w.MOBILITY_MULTIPLIER;

        return score;
    }

    private static int getScore(int count, int centerCount, int emptyCount, Weights w) {
        int baseScore = switch (count) {
            case 1 -> w.SCORE_ONE;
            case 2 -> w.SCORE_TWO;
            case 3 -> w.SCORE_THREE;
            case 4 -> w.SCORE_FOUR;
            default -> 0;
        };

        baseScore += centerCount * w.CENTER_MULTIPLIER;

        if (count == 2 && emptyCount == 2) {
            baseScore += w.OPEN_TWO_BONUS;
        }

        if (emptyCount == 1 && count == 2) {
            // almost a fork point
            baseScore += w.NEAR_FORK_BONUS;
        }

        if (count == 3 && emptyCount == 1) {
            baseScore += w.CLOSED_THREE_BONUS;
        }

        return baseScore;
    }

    /**
     * Evaluates potential forks for the player.
     *
     * @param playerPositions   Bitmask of player's positions.
     * @param occupiedPositions Bitmask of occupied positions.
     * @return The number of potential forks.
     */
    private static int evaluatePotentialForks(long playerPositions, long occupiedPositions, long opponentPositions) {
        int forkCount = 0;

        long emptySpaces = ~occupiedPositions & FULL_MASK;
        while (emptySpaces != 0) {
            int pos = Long.numberOfTrailingZeros(emptySpaces);
            emptySpaces &= (emptySpaces - 1);

            int supportingLines = 0;
            for (int lineIdx : LINES_BY_POSITION[pos]) {
                Line line = Line.lines[lineIdx];
                long mask = line.positions();
                if ((mask & opponentPositions) != 0) {
                    continue; // blocked
                }
                int playerCount = Bit.countOnes(mask & playerPositions);
                int emptyCount = LINE_LENGTH - playerCount - Bit.countOnes(mask & opponentPositions);
                if (playerCount == 2 && emptyCount == 2) {
                    supportingLines++;
                }
            }

            if (supportingLines >= 2) {
                forkCount++;
            }
        }

        return forkCount;
    }

    private static int countImmediateWinningSquares(long playerPositions, long opponentPositions, long occupiedPositions) {
        int winningSquares = 0;

        long emptySpaces = ~occupiedPositions & FULL_MASK;
        while (emptySpaces != 0) {
            int pos = Long.numberOfTrailingZeros(emptySpaces);
            emptySpaces &= (emptySpaces - 1);

            int winningLines = 0;
            for (int lineIdx : LINES_BY_POSITION[pos]) {
                Line line = Line.lines[lineIdx];
                long mask = line.positions();
                if ((mask & opponentPositions) != 0) {
                    continue;
                }
                int playerCount = Bit.countOnes(mask & playerPositions);
                int emptyCount = LINE_LENGTH - playerCount - Bit.countOnes(mask & opponentPositions);
                if (playerCount == LINE_LENGTH - 1 && emptyCount == 1) {
                    winningLines++;
                }
            }

            if (winningLines > 0) {
                winningSquares += winningLines;
            }
        }

        return winningSquares;
    }

    /**
     * Evaluates the opponent's potential to create forks in their next move.
     *
     * @param board             The current board state.
     * @param opponent          The opponent player.
     * @param occupiedPositions Bitmask of occupied positions.
     * @return The number of potential forks the opponent can create in their next move.
     */
    private static int evaluateOpponentPotentialForks(Board board, Player opponent, long occupiedPositions) {
        int potentialForks = 0;

        // simulate the opponent's move
        long emptyPositions = ~occupiedPositions & FULL_MASK;

        while (emptyPositions != 0) {
            int position = Long.numberOfTrailingZeros(emptyPositions);
            emptyPositions &= (emptyPositions - 1);

            long newOpponentPositions = (opponent == Player.X)
                    ? Bit.set(board.xPositions, position)
                    : Bit.set(board.oPositions, position);
            long newOccupiedPositions = occupiedPositions | Bit.positionMask(position);
            long playerPositions = (opponent == Player.X) ? board.oPositions : board.xPositions;

            int forks = evaluatePotentialForks(newOpponentPositions, newOccupiedPositions, playerPositions);
            if (forks > 0) {
                potentialForks += forks;
            }
        }

        return potentialForks;
    }

    private static int evaluateDoubleThreats(long playerPositions, long opponentPositions, long occupiedPositions) {
        int doubleThreats = 0;

        long emptySpaces = ~occupiedPositions & FULL_MASK;
        while (emptySpaces != 0) {
            int pos = Long.numberOfTrailingZeros(emptySpaces);
            emptySpaces &= (emptySpaces - 1);

            int threatLines = 0;
            for (int lineIdx : LINES_BY_POSITION[pos]) {
                Line line = Line.lines[lineIdx];
                long mask = line.positions();
                if ((mask & opponentPositions) != 0) {
                    continue;
                }
                int playerCount = Bit.countOnes(mask & playerPositions);
                int emptyCount = LINE_LENGTH - playerCount - Bit.countOnes(mask & opponentPositions);
                int newCount = playerCount + 1;
                int newEmpty = emptyCount - 1;
                if (newCount == 4) {
                    // winning immediately is even better than a fork
                    threatLines += 2;
                } else if (newCount == 3 && newEmpty == 1) {
                    threatLines++;
                }
            }

            if (threatLines >= 2) {
                doubleThreats++;
            }
        }

        return doubleThreats;
    }

    private static int evaluateBlockedIntersections(long playerPositions, long opponentPositions) {
        int blocked = 0;

        long tempPlayer = playerPositions;
        while (tempPlayer != 0) {
            int pos = Long.numberOfTrailingZeros(tempPlayer);
            tempPlayer &= (tempPlayer - 1);

            int blockedLines = 0;
            for (int lineIdx : LINES_BY_POSITION[pos]) {
                Line line = Line.lines[lineIdx];
                long mask = line.positions();
                long playerMask = mask & playerPositions;
                long opponentMask = mask & opponentPositions;
                if (playerMask == Bit.positionMask(pos) && Bit.countOnes(opponentMask) == LINE_LENGTH - 2) {
                    int occupiedCount = Bit.countOnes(playerMask | opponentMask);
                    int emptyCount = LINE_LENGTH - occupiedCount;
                    if (emptyCount == 1) {
                        blockedLines++;
                    }
                }
            }

            if (blockedLines > 1) {
                // occupying this square simultaneously disrupted multiple near-threats
                blocked++;
            }
        }

        return blocked;
    }
}
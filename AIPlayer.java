// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class AIPlayer {
    private static final boolean DEBUG = false;
    private static final int MAX_DEPTH = 32;
    private static final double TIME_LIMIT_SECONDS = 13.8;
    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;
    private static final long MAX_TIME = (long)(TIME_LIMIT_SECONDS * NANOSECONDS_PER_SECOND);
    private static final long WIN_SCORE = 1_000_000_000L;
    private static final long FULL_MASK = -1L >>> (64 - Coordinate.NCubed);
    private static final int LINE_LENGTH = Coordinate.N;
    private static final boolean USE_FIXED_SEED = false;
    private static final long FIXED_SEED = 2398558964L;
    private static final long GLOBAL_SEED = USE_FIXED_SEED ? FIXED_SEED : System.nanoTime();

    private static final ThreadLocal < Random > threadRandom = ThreadLocal.withInitial(() ->
            new Random(GLOBAL_SEED + Thread.currentThread().threadId()));

    private static final int CONVERGENCE_THRESHOLD = 12;

    private static final long[][] ZOBRIST_TABLE = new long[Coordinate.NCubed][2];
    private static final long ZOBRIST_SIDE = threadRandom.get().nextLong();
    private static final long EMPTY_ZOBRIST = threadRandom.get().nextLong();

    private static final int[][] OPTIMAL_PAIRS = {
            {
                    37,
                    42
            },
            {
                    38,
                    41
            },
            {
                    21,
                    26
            },
            {
                    22,
                    25
            }
    };

    static {
        // Initialize Zobrist table
        for (int pos = 0; pos < Coordinate.NCubed; pos++) {
            ZOBRIST_TABLE[pos][0] = threadRandom.get().nextLong(); // player X
            ZOBRIST_TABLE[pos][1] = threadRandom.get().nextLong(); // player O
        }
    }

    private static class TranspositionEntry {
        final long value;
        final int depth;
        final int flag; // 0: exact, 1: lower bound, 2: upper bound
        final int bestMove;

        TranspositionEntry(long value, int depth, int flag, int bestMove) {
            this.value = value;
            this.depth = depth;
            this.flag = flag;
            this.bestMove = bestMove;
        }
    }

    static class MoveEvaluation {
        final int move;
        final long value;

        MoveEvaluation(int move, long value) {
            this.move = move;
            this.value = value;
        }
    }

    private static class MinimaxTask extends RecursiveTask < MoveEvaluation > {
        private final Board board;
        private final int move;
        private final int depth;
        private final Player aiPlayer;
        private final long endTime;
        private final AIPlayer aiInstance;

        MinimaxTask(Board b, int move, int depth, Player ai, long endTime, AIPlayer aiInstance) {
            this.board = new Board();
            this.board.xPositions = b.xPositions;
            this.board.oPositions = b.oPositions;
            this.move = move;
            this.depth = depth;
            this.aiPlayer = ai;
            this.endTime = endTime;
            this.aiInstance = aiInstance;
        }

        @Override
        protected MoveEvaluation compute() {
            if (System.nanoTime() >= endTime || aiInstance.timeUp) {
                aiInstance.timeUp = true;
                return null;
            }
            board.makeMove(move, aiPlayer);
            long val = aiInstance.minimax(board, depth - 1, Long.MIN_VALUE, Long.MAX_VALUE, aiPlayer.other(), 1);
            if (DEBUG) {
                System.out.println("[DEBUG] Parallel task move: " + move + " depth: " + depth + " val: " + val);
            }
            return new MoveEvaluation(move, val);
        }
    }

    private final Player aiPlayer;
    private final Weights weights;
    private final int maxDepth;
    private final long maxTime;
    private final Player opponent;
    private volatile long endTime;
    private volatile boolean timeUp;
    private volatile int bestMoveSoFar;
    private volatile int lastDepth;
    private int lastBestMove = -1;
    private int consistentMoveCount = 0;
    private final ConcurrentHashMap < Long, TranspositionEntry > transpositionTable = new ConcurrentHashMap < > ();
    private final AtomicLong nodeCount = new AtomicLong(0);
    private final AtomicLong transpositionHits = new AtomicLong(0);
    private final AtomicLong transpositionMisses = new AtomicLong(0);
    private final AtomicLong totalSearches = new AtomicLong(0);
    private final AtomicLong totalNodesEvaluated = new AtomicLong(0);
    private final int[][] killerMoves = new int[MAX_DEPTH + 1][2];
    private final int[][] historyHeuristic = new int[2][Coordinate.NCubed];

    public AIPlayer(Player aiPlayer, int maxDepth) {
        this.aiPlayer = aiPlayer;
        this.opponent = aiPlayer.other();
        this.weights = new Weights(true);

        this.maxDepth = Math.max(1, Math.min(maxDepth, MAX_DEPTH));
        this.maxTime = 999_999_999_999L; // effectively no time limit
    }

    public AIPlayer(Player aiPlayer, Weights weights) {
        this.aiPlayer = aiPlayer;
        this.opponent = aiPlayer.other();
        this.weights = weights;
        this.maxDepth = MAX_DEPTH;
        this.maxTime = MAX_TIME;
    }

    public AIPlayer(Player aiPlayer) {
        this.aiPlayer = aiPlayer;
        this.opponent = aiPlayer.other();
        this.weights = new Weights(true);
        this.maxDepth = MAX_DEPTH;
        this.maxTime = MAX_TIME;
    }

    public int chooseBestMove(Board board) {
        if (DEBUG) {
            System.out.println("[DEBUG] Starting chooseBestMove.");
        }

        Board mBoard = new Board();
        mBoard.xPositions = board.xPositions;
        mBoard.oPositions = board.oPositions;

        bestMoveSoFar = -1;
        timeUp = false;
        endTime = System.nanoTime() + maxTime;
        resetMoveHeuristics();

        nodeCount.set(0);
        transpositionHits.set(0);
        transpositionMisses.set(0);
        totalSearches.incrementAndGet();

        final Thread progressBarThread = getProgressBarThread();

        try (ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors())) {

            if (DEBUG) {
                System.out.println("[DEBUG] Board initial state: ");
                System.out.println("[DEBUG] X positions: " + mBoard.xPositions);
                System.out.println("[DEBUG] O positions: " + mBoard.oPositions);
                System.out.println("[DEBUG] Total moves so far: " + mBoard.totalMoves());
            }

            if (mBoard.isEmpty()) {
                int i1 = threadRandom.get().nextInt(OPTIMAL_PAIRS.length);
                int i2 = threadRandom.get().nextInt(OPTIMAL_PAIRS[i1].length);
                int firstOptimalMove = OPTIMAL_PAIRS[i1][i2];

                timeUp = true;
                joinProgressBarThread(progressBarThread);
                forkJoinPool.shutdownNow();

                if (DEBUG) {
                    System.out.println("[DEBUG] Board empty, choosing optimal first move: " + firstOptimalMove);
                }
                return firstOptimalMove;
            }

            if (mBoard.totalMoves() == 1) {
                for (int[] pair: OPTIMAL_PAIRS) {
                    final int moveA = pair[0];
                    final int moveB = pair[1];

                    if (mBoard.get(moveA) == opponent) {
                        timeUp = true;
                        joinProgressBarThread(progressBarThread);
                        forkJoinPool.shutdownNow();
                        if (DEBUG) {
                            System.out.println("[DEBUG] Opponent took " + moveA + ", responding with " + moveB);
                        }
                        return moveB;
                    } else if (mBoard.get(moveB) == opponent) {
                        timeUp = true;
                        joinProgressBarThread(progressBarThread);
                        forkJoinPool.shutdownNow();
                        if (DEBUG) {
                            System.out.println("[DEBUG] Opponent took " + moveB + ", responding with " + moveA);
                        }
                        return moveA;
                    }
                }
            }

            int immediateWinMove = findImmediateWinMove(mBoard, aiPlayer);
            if (immediateWinMove != -1) {
                Thread.sleep(340);
                timeUp = true;
                joinProgressBarThread(progressBarThread);
                forkJoinPool.shutdownNow();
                if (DEBUG) {
                    System.out.println("[DEBUG] Found immediate win move: " + immediateWinMove);
                }
                return immediateWinMove;
            }

            int immediateBlockMove = findImmediateBlockMove(mBoard, opponent);
            if (immediateBlockMove != -1) {
                Thread.sleep(180);
                timeUp = true;
                joinProgressBarThread(progressBarThread);
                forkJoinPool.shutdownNow();
                if (DEBUG) {
                    System.out.println("[DEBUG] Found immediate block move: " + immediateBlockMove);
                }
                return immediateBlockMove;
            }

            for (int depth = 1; depth <= maxDepth; depth++) {
                if (DEBUG) {
                    System.out.println("[DEBUG] Starting depth " + depth);
                }

                long bestVal = Long.MIN_VALUE;
                List < Integer > bestMovesD = new ArrayList < > ();
                List < Integer > availMoves = mBoard.getAvailableMoves();

                Map < Integer, Long > moveScores = new HashMap < > ();
                for (int mv: availMoves) {
                    mBoard.makeMove(mv, aiPlayer);
                    long eval = Heuristic.evaluate(mBoard, aiPlayer, weights);
                    moveScores.put(mv, eval);
                    mBoard.undoMove(mv);
                }
                availMoves.sort((m1, m2) -> Long.compare(moveScores.get(m2), moveScores.get(m1)));

                if (DEBUG) {
                    System.out.println("[DEBUG] Depth " + depth + " available moves: " + availMoves.size());
                    System.out.println("[DEBUG] Move scores: " + moveScores);
                }

                List < MoveEvaluation > results = new ArrayList < > ();
                long currentTime = System.nanoTime();
                boolean parallel = shouldParallelize(depth, maxDepth, availMoves, currentTime, endTime);

                if (DEBUG) {
                    System.out.println("[DEBUG] Depth " + depth + " parallel decision: " + parallel);
                }

                if (!timeUp && parallel && !availMoves.isEmpty()) {
                    if (DEBUG) {
                        System.out.println("[DEBUG] Depth " + depth + " - Running moves in parallel.");
                    }

                    List < MinimaxTask > tasks = new ArrayList < > ();
                    for (int mv: availMoves) {
                        if (System.nanoTime() >= endTime) {
                            timeUp = true;
                            lastDepth = depth - 1;
                            break;
                        }
                        Board newBrd = new Board();
                        newBrd.xPositions = mBoard.xPositions;
                        newBrd.oPositions = mBoard.oPositions;
                        tasks.add(new MinimaxTask(newBrd, mv, depth, aiPlayer, endTime, this));
                    }

                    for (MinimaxTask t: tasks) {
                        t.fork();
                    }

                    for (MinimaxTask t: tasks) {
                        MoveEvaluation res = t.join();
                        if (res != null) {
                            results.add(res);
                        }
                    }

                } else {
                    if (DEBUG) {
                        System.out.println("[DEBUG] Depth " + depth + " - Running moves sequentially.");
                    }

                    for (int mv: availMoves) {
                        if (System.nanoTime() >= endTime) {
                            timeUp = true;
                            lastDepth = depth - 1;
                            break;
                        }
                        Board newBrd = new Board();
                        newBrd.xPositions = mBoard.xPositions;
                        newBrd.oPositions = mBoard.oPositions;
                        newBrd.makeMove(mv, aiPlayer);
                        long val = minimax(newBrd, depth - 1, Long.MIN_VALUE, Long.MAX_VALUE, aiPlayer.other(), 1);
                        results.add(new MoveEvaluation(mv, val));
                    }
                }

                if (timeUp) {
                    if (DEBUG) {
                        System.out.println("[DEBUG] Time up during depth " + depth);
                    }
                    lastDepth = depth - 1;
                    break;
                }

                for (MoveEvaluation r: results) {
                    if (r.value > bestVal) {
                        bestVal = r.value;
                        bestMovesD.clear();
                        bestMovesD.add(r.move);
                    } else if (r.value == bestVal) {
                        bestMovesD.add(r.move);
                    }
                }

                int bestMoveAtDepth = bestMovesD.isEmpty() ? -1 :
                        bestMovesD.get(threadRandom.get().nextInt(bestMovesD.size()));

                if (bestMoveAtDepth == lastBestMove) {
                    consistentMoveCount++;
                    if (consistentMoveCount >= CONVERGENCE_THRESHOLD) {
                        if (DEBUG) {
                            System.out.println("[DEBUG] Converged at depth " + depth + ", best move: " + bestMoveAtDepth);
                        }
                        lastDepth = depth;
                        break;
                    }
                } else {
                    consistentMoveCount = 0;
                }
                lastBestMove = bestMoveAtDepth;

                if (!timeUp && bestMoveAtDepth != -1) {
                    bestMoveSoFar = bestMoveAtDepth;
                    if (DEBUG) {
                        System.out.println("[DEBUG] Depth " + depth + " best move: " + bestMoveSoFar + " value: " + bestVal);
                    }
                    lastDepth = depth;
                } else {
                    lastDepth = depth;
                    break;
                }
            }

            timeUp = true;
            totalNodesEvaluated.addAndGet(nodeCount.get());
            joinProgressBarThread(progressBarThread);
            forkJoinPool.shutdownNow();

            if (DEBUG) {
                System.out.println("[DEBUG] Best move after search: " + bestMoveSoFar);
                System.out.println("[DEBUG] Node count: " + nodeCount.get());
                System.out.println("[DEBUG] Transposition hits: " + transpositionHits.get());
                System.out.println("[DEBUG] Transposition misses: " + transpositionMisses.get());
                System.out.println("[DEBUG] Memory used: " + formatMemory(getMemoryUsed()));
                System.out.println("[DEBUG] Heap size: " + formatMemory(Runtime.getRuntime().totalMemory()));
            }

            return bestMoveSoFar;
        } catch (Exception e) {
            System.out.println("[FATAL] Could not allocate CPU cores for ForkJoinPool parallelization.\n" +
                    Arrays.toString(e.getStackTrace()));
            System.exit(1);
            return -1;
        }
    }

    private void joinProgressBarThread(Thread progressBarThread) {
        try {
            progressBarThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void resetMoveHeuristics() {
        for (int d = 0; d < killerMoves.length; d++) {
            killerMoves[d][0] = -1;
            killerMoves[d][1] = -1;
        }
        for (int p = 0; p < historyHeuristic.length; p++) {
            Arrays.fill(historyHeuristic[p], 0);
        }
    }

    private boolean shouldParallelize(int depth, int maxDepth, List < Integer > availMoves, long currentTime, long endTime) {
        long timeRemaining = endTime - currentTime;
        int movesCount = availMoves.size();

        boolean shallowDepth = (maxDepth - depth) < (maxDepth / 4);
        boolean manyMoves = movesCount > 15;
        boolean plentyOfTime = timeRemaining > (maxTime / 2);

        int conditionsMet = 0;
        if (shallowDepth) conditionsMet++;
        if (manyMoves) conditionsMet++;
        if (plentyOfTime) conditionsMet++;

        return conditionsMet >= 2;
    }

    private long getMemoryUsed() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private String formatMemory(long memory) {
        final String[] categories = {
                "B",
                "KB",
                "MB",
                "GB"
        };
        int category = 0;
        double mem = memory;
        while (mem > 1024 && category < categories.length - 1) {
            mem /= 1024;
            category++;
        }
        return String.format("%.2f %s", mem, categories[category]);
    }

    private long computeZobristHash(Board b, Player toMove) {
        long hash = EMPTY_ZOBRIST;
        long xs = b.xPositions;
        while (xs != 0) {
            int pos = Long.numberOfTrailingZeros(xs);
            hash ^= ZOBRIST_TABLE[pos][0];
            xs &= (xs - 1);
        }
        long os = b.oPositions;
        while (os != 0) {
            int pos = Long.numberOfTrailingZeros(os);
            hash ^= ZOBRIST_TABLE[pos][1];
            os &= (os - 1);
        }
        if (toMove == aiPlayer) {
            hash ^= ZOBRIST_SIDE;
        }
        return hash;
    }

    private long minimax(Board b, int depth, long alpha, long beta, Player toMove, int ply) {
        if (System.nanoTime() >= endTime) {
            timeUp = true;
            return 0; // Neutral if time is up
        }

        nodeCount.incrementAndGet();
        long alphaOrig = alpha;
        long zHash = computeZobristHash(b, toMove);
        TranspositionEntry entry = transpositionTable.get(zHash);
        Integer ttMove = null;
        if (entry != null && entry.depth >= depth) {
            transpositionHits.incrementAndGet();
            ttMove = entry.bestMove >= 0 ? entry.bestMove : null;
            if (entry.flag == 0) {
                return entry.value;
            } else if (entry.flag == 1) {
                alpha = Math.max(alpha, entry.value);
            } else if (entry.flag == 2) {
                beta = Math.min(beta, entry.value);
            }
            if (alpha >= beta) {
                return entry.value;
            }
        } else {
            transpositionMisses.incrementAndGet();
        }

        if (checkWinner(b, toMove.other())) {
            long winScore = (toMove.other() == aiPlayer ? WIN_SCORE : -WIN_SCORE) + depth;
            transpositionTable.put(zHash, new TranspositionEntry(winScore, depth, 0, -1));
            return winScore;
        }

        if (depth == 0 || b.isFull()) {
            long eval = Heuristic.evaluate(b, aiPlayer, weights);
            int flag = (eval <= alpha) ? 2 : (eval >= beta) ? 1 : 0;
            transpositionTable.put(zHash, new TranspositionEntry(eval, depth, flag, -1));
            return eval;
        }

        int[] moves = new int[Coordinate.NCubed];
        int moveCount = generateMoves(b, moves);
        orderMoves(b, toMove, depth, moves, moveCount, ttMove);

        boolean maximizing = toMove == aiPlayer;
        long bestVal = maximizing ? Long.MIN_VALUE : Long.MAX_VALUE;
        int bestMove = -1;

        for (int i = 0; i < moveCount && !timeUp; i++) {
            int mv = moves[i];
            b.makeMove(mv, toMove);
            long eval;
            if (checkWinner(b, toMove)) {
                eval = (toMove == aiPlayer ? WIN_SCORE : -WIN_SCORE) + depth;
            } else {
                eval = minimax(b, depth - 1, alpha, beta, toMove.other(), ply + 1);
            }
            b.undoMove(mv);

            if (maximizing) {
                if (eval > bestVal) {
                    bestVal = eval;
                    bestMove = mv;
                }
                alpha = Math.max(alpha, eval);
                if (alpha >= beta) {
                    updateKiller(mv, ply);
                    updateHistory(toMove, mv, depth);
                    break;
                }
            } else {
                if (eval < bestVal) {
                    bestVal = eval;
                    bestMove = mv;
                }
                beta = Math.min(beta, eval);
                if (alpha >= beta) {
                    updateKiller(mv, ply);
                    updateHistory(toMove, mv, depth);
                    break;
                }
            }
        }

        int fType = (bestVal <= alphaOrig) ? 2 : (bestVal >= beta) ? 1 : 0;
        transpositionTable.put(zHash, new TranspositionEntry(bestVal, depth, fType, bestMove));
        return bestVal;
    }

    private void updateKiller(int move, int depth) {
        if (move == killerMoves[depth][0] || move == killerMoves[depth][1]) {
            return;
        }
        killerMoves[depth][1] = killerMoves[depth][0];
        killerMoves[depth][0] = move;
    }

    private void updateHistory(Player mover, int move, int depth) {
        int idx = mover == aiPlayer ? 0 : 1;
        historyHeuristic[idx][move] += depth * depth;
    }

    private boolean isKiller(int move, int depth) {
        return killerMoves[depth][0] == move || killerMoves[depth][1] == move;
    }

    private int generateMoves(Board b, int[] buffer) {
        long empty = ~(b.xPositions | b.oPositions) & FULL_MASK;
        int count = 0;
        while (empty != 0) {
            int pos = Long.numberOfTrailingZeros(empty);
            buffer[count++] = pos;
            empty &= (empty - 1);
        }
        return count;
    }

    private void orderMoves(Board b, Player toMove, int depth, int[] moves, int moveCount, Integer ttMove) {
        long[] scores = new long[moveCount];
        int playerIdx = (toMove == aiPlayer) ? 0 : 1;
        for (int i = 0; i < moveCount; i++) {
            int mv = moves[i];
            long score = historyHeuristic[playerIdx][mv];
            if (ttMove != null && mv == ttMove) {
                score += 1_000_000_0L;
            }
            if (isKiller(mv, depth)) {
                score += 500_000L;
            }
            b.makeMove(mv, toMove);
            if (checkWinner(b, toMove)) {
                score += WIN_SCORE / 10;
            } else {
                score += Heuristic.evaluate(b, aiPlayer, weights) / 50;
            }
            b.undoMove(mv);
            scores[i] = score;
        }

        // simple insertion sort to avoid allocations
        for (int i = 1; i < moveCount; i++) {
            int m = moves[i];
            long s = scores[i];
            int j = i - 1;
            while (j >= 0 && scores[j] < s) {
                scores[j + 1] = scores[j];
                moves[j + 1] = moves[j];
                j--;
            }
            scores[j + 1] = s;
            moves[j + 1] = m;
        }
    }

    private boolean checkWinner(Board b, Player p) {
        long pPositions = (p == Player.X) ? b.xPositions : b.oPositions;
        for (Line line: Line.lines) {
            if ((pPositions & line.positions()) == line.positions()) {
                return true;
            }
        }
        return false;
    }

    private int findImmediateWinMove(Board b, Player p) {
        long pPositions = (p == Player.X) ? b.xPositions : b.oPositions;
        long oPositions = (p == Player.X) ? b.oPositions : b.xPositions;
        long occupied = pPositions | oPositions;

        for (Line line: Line.lines) {
            long linePos = line.positions();
            long pLinePos = linePos & pPositions;
            long emptyLinePos = linePos & ~occupied;

            int pCount = Bit.countOnes(pLinePos);
            int emptyCount = Bit.countOnes(emptyLinePos);

            if (pCount == 3 && emptyCount == 1) {
                return Long.numberOfTrailingZeros(emptyLinePos);
            }
        }

        return -1;
    }

    private int findImmediateBlockMove(Board b, Player opp) {
        return findImmediateWinMove(b, opp);
    }

    /**
     * Starts a progress bar thread that shows time usage if not in debug mode.
     */
    private Thread getProgressBarThread() {
        Thread t = new Thread(() -> {
            boolean isTimeMode = !(maxTime > MAX_TIME);
            long startTime = System.nanoTime();
            while (!timeUp && System.nanoTime() < endTime) {
                long elapsed = System.nanoTime() - startTime;
                int progress = (int)((elapsed * 100) / maxTime);
                if (isTimeMode && !DEBUG) {
                    System.out.print("\rProgress: " + progress + "%");
                }
                try {
                    Thread.sleep(45);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (isTimeMode && !DEBUG) {
                System.out.print("\rProgress: 100%\n");
            }
        });
        t.start();
        return t;
    }

    public String stats() {
        long memoryUsed = getMemoryUsed();
        long heapSize = Runtime.getRuntime().totalMemory();

        long searches = totalSearches.get();
        double averageNodesPerSearch = searches > 0 ? (double) totalNodesEvaluated.get() / searches : 0.0;

        Map < String, Object > statsMap = new LinkedHashMap < > ();
        statsMap.put("nodeCount", nodeCount.get());
        statsMap.put("lastDepth", lastDepth);
        statsMap.put("transpositionHits", transpositionHits.get());
        statsMap.put("transpositionMisses", transpositionMisses.get());
        statsMap.put("transpositionTableSize", transpositionTable.size());
        statsMap.put("bestMoveSoFar", bestMoveSoFar);
        statsMap.put("lastBestMove", lastBestMove);
        statsMap.put("consistentMoveCount", consistentMoveCount);
        statsMap.put("memoryUsed", formatMemory(memoryUsed));
        statsMap.put("heapSize", formatMemory(heapSize));
        statsMap.put("totalSearches", searches);
        statsMap.put("totalNodesEvaluated", totalNodesEvaluated.get());
        statsMap.put("averageNodesPerSearch", averageNodesPerSearch);

        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        Iterator < Map.Entry < String, Object >> iterator = statsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry < String, Object > entry = iterator.next();
            jsonBuilder.append("\"").append(entry.getKey()).append("\": ");

            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                jsonBuilder.append(value);
            } else {
                jsonBuilder.append("\"").append(value.toString()).append("\"");
            }

            if (iterator.hasNext()) {
                jsonBuilder.append(", ");
            }
        }
        jsonBuilder.append("}");

        return jsonBuilder.toString();
    }
}
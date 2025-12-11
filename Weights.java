// Owen Gregson
// Artificial Intelligence
// TTT Checkpoint #3
// Dec 18, 2024

import java.util.Random;

public class Weights {
    public int SCORE_ONE = 10;
    public int SCORE_TWO = 100;
    public int SCORE_THREE = 1000;
    public int SCORE_FOUR = 100000;

    // pattern texture
    public int OPEN_TWO_BONUS = 180;
    public int CLOSED_THREE_BONUS = 850;

    // board texture
    public int BLOCKED_LINE_PENALTY = 6;
    public int OPEN_LINE_BONUS = 12;
    public int NEAR_FORK_BONUS = 75;

    // any centerpiece
    public int CENTER_MULTIPLIER = 34;

    // opponent creates a line
    public int OPPONENT_SCORE_MULTIPLIER = 2;

    // immediate threat or win
    public int IMMEDIATE_THREAT_PENALTY = 20000;
    public int IMMEDIATE_WIN_BONUS = 20000;
    public int IMMEDIATE_WIN_SQUARE_BONUS = 14000;
    public int OPPONENT_IMMEDIATE_WIN_SQUARE_PENALTY = 18000;

    // mobility and shape awareness
    public int MOBILITY_MULTIPLIER = 8;

    // central control
    public int CENTER_CONTROL_MULTIPLIER = 88;
    public int OPPONENT_CENTER_CONTROL_MULTIPLIER = 112;

    // corner control
    public int PCORNER_CONTROL_MULTIPLIER = 14;
    public int OPPONENT_PCORNER_CONTROL_MULTIPLIER = 22;
    public int BCORNER_CONTROL_MULTIPLIER = 24;
    public int OPPONENT_BCORNER_CONTROL_MULTIPLIER = 28;

    // fork maker
    public int PLAYER_FORKS_MULTIPLIER = 3470;

    // fork stopper
    public int OPPONENT_FORKS_MULTIPLIER = 982;
    public int OPPONENT_POTENTIAL_FORKS_PENALTY = 4850;
    public int DOUBLE_THREAT_BONUS = 7500;
    public int OPPONENT_DOUBLE_THREAT_PENALTY = 9500;
    public int BLOCKED_THREAT_INTERSECTION_BONUS = 3200;
    private static final Random rand = new Random();

    public Weights(boolean fixed) {
        // You can initialize defaults or randomize here
        if(!fixed) randomize();
    }

    public Weights(String load) {
        // string format: Weights{S1=3, S2=114, S3=201, S4=100000, O2B=120, CTB=800, CM=54, OSM=2, ITP=95160, IWB=98840, IWSB=14000, OIWS=18000, MOB=8, CCM=179, OCCM=220, Pcc=29, Opc=11, Bc=39, Obc=25, Pfm=2949, Ofm=603, OPFP=1220, DTB=7500, ODTP=9500, BTIB=3200}
        String[] parts = load.replace("{","").replace("}","").split(", ");
            for (String part : parts) {
            String[] pair = part.split("=");
            switch (pair[0]) {
            case "S1": SCORE_ONE = Integer.parseInt(pair[1]); break;
            case "S2": SCORE_TWO = Integer.parseInt(pair[1]); break;
            case "S3": SCORE_THREE = Integer.parseInt(pair[1]); break;
            case "S4": SCORE_FOUR = Integer.parseInt(pair[1]); break;
            case "O2B": OPEN_TWO_BONUS = Integer.parseInt(pair[1]); break;
            case "CTB": CLOSED_THREE_BONUS = Integer.parseInt(pair[1]); break;
            case "BLP": BLOCKED_LINE_PENALTY = Integer.parseInt(pair[1]); break;
            case "OLB": OPEN_LINE_BONUS = Integer.parseInt(pair[1]); break;
            case "NFB": NEAR_FORK_BONUS = Integer.parseInt(pair[1]); break;
            case "CM": CENTER_MULTIPLIER = Integer.parseInt(pair[1]); break;
            case "OSM": OPPONENT_SCORE_MULTIPLIER = Integer.parseInt(pair[1]); break;
            case "ITP": IMMEDIATE_THREAT_PENALTY = Integer.parseInt(pair[1]); break;
            case "IWB": IMMEDIATE_WIN_BONUS = Integer.parseInt(pair[1]); break;
            case "IWSB": IMMEDIATE_WIN_SQUARE_BONUS = Integer.parseInt(pair[1]); break;
            case "OIWS": OPPONENT_IMMEDIATE_WIN_SQUARE_PENALTY = Integer.parseInt(pair[1]); break;
            case "MOB": MOBILITY_MULTIPLIER = Integer.parseInt(pair[1]); break;
            case "CCM": CENTER_CONTROL_MULTIPLIER = Integer.parseInt(pair[1]); break;
            case "OCCM": OPPONENT_CENTER_CONTROL_MULTIPLIER = Integer.parseInt(pair[1]); break;
            case "Pcc": PCORNER_CONTROL_MULTIPLIER = Integer.parseInt(pair[1]); break;
            case "Opc": OPPONENT_PCORNER_CONTROL_MULTIPLIER = Integer.parseInt(pair[1]); break;
            case "Bc": BCORNER_CONTROL_MULTIPLIER = Integer.parseInt(pair[1]); break;
            case "Obc": OPPONENT_BCORNER_CONTROL_MULTIPLIER = Integer.parseInt(pair[1]); break;
            case "Pfm": PLAYER_FORKS_MULTIPLIER = Integer.parseInt(pair[1]); break;
            case "Ofm": OPPONENT_FORKS_MULTIPLIER = Integer.parseInt(pair[1]); break;
            case "OPFP": OPPONENT_POTENTIAL_FORKS_PENALTY = Integer.parseInt(pair[1]); break;
            case "DTB": DOUBLE_THREAT_BONUS = Integer.parseInt(pair[1]); break;
            case "ODTP": OPPONENT_DOUBLE_THREAT_PENALTY = Integer.parseInt(pair[1]); break;
            case "BTIB": BLOCKED_THREAT_INTERSECTION_BONUS = Integer.parseInt(pair[1]); break;
            }
        }
    }

    public void randomize() {
        SCORE_ONE = randInRange(1, 20);
        SCORE_TWO = randInRange(20, 200);
        SCORE_THREE = randInRange(200, 2000);
        //SCORE_FOUR = randInRange(50000, 150000);
        OPEN_TWO_BONUS = randInRange(80, 280);
        CLOSED_THREE_BONUS = randInRange(600, 1200);

        BLOCKED_LINE_PENALTY = randInRange(1, 20);
        OPEN_LINE_BONUS = randInRange(5, 30);
        NEAR_FORK_BONUS = randInRange(20, 120);

        CENTER_MULTIPLIER = randInRange(10, 60);
        OPPONENT_SCORE_MULTIPLIER = randInRange(1, 5);
        //IMMEDIATE_THREAT_PENALTY = randInRange(50000, 200000);
        //IMMEDIATE_WIN_BONUS = randInRange(50000, 200000);
        IMMEDIATE_WIN_SQUARE_BONUS = randInRange(8000, 20000);
        OPPONENT_IMMEDIATE_WIN_SQUARE_PENALTY = randInRange(12000, 24000);

        MOBILITY_MULTIPLIER = randInRange(1, 14);
        CENTER_CONTROL_MULTIPLIER = randInRange(50, 220);
        OPPONENT_CENTER_CONTROL_MULTIPLIER = randInRange(50, 220);

        PCORNER_CONTROL_MULTIPLIER = randInRange(10, 40);
        OPPONENT_PCORNER_CONTROL_MULTIPLIER = randInRange(10, 40);
        BCORNER_CONTROL_MULTIPLIER = randInRange(10, 40);
        OPPONENT_BCORNER_CONTROL_MULTIPLIER = randInRange(10, 40);

        PLAYER_FORKS_MULTIPLIER = randInRange(1000, 4000);
        OPPONENT_FORKS_MULTIPLIER = randInRange(500, 2500);
        OPPONENT_POTENTIAL_FORKS_PENALTY = randInRange(1000, 7000);
        DOUBLE_THREAT_BONUS = randInRange(3000, 10000);
        OPPONENT_DOUBLE_THREAT_PENALTY = randInRange(5000, 12000);
        BLOCKED_THREAT_INTERSECTION_BONUS = randInRange(1200, 5200);
    }

    private int randInRange(int min, int max) {
        return min + rand.nextInt(max - min + 1);
    }

    public Weights copy() {
        Weights w = new Weights(true);
        w.SCORE_ONE = SCORE_ONE;
        w.SCORE_TWO = SCORE_TWO;
        w.SCORE_THREE = SCORE_THREE;
        w.SCORE_FOUR = SCORE_FOUR;
        w.OPEN_TWO_BONUS = OPEN_TWO_BONUS;
        w.CLOSED_THREE_BONUS = CLOSED_THREE_BONUS;
        w.BLOCKED_LINE_PENALTY = BLOCKED_LINE_PENALTY;
        w.OPEN_LINE_BONUS = OPEN_LINE_BONUS;
        w.NEAR_FORK_BONUS = NEAR_FORK_BONUS;
        w.CENTER_MULTIPLIER = CENTER_MULTIPLIER;
        w.OPPONENT_SCORE_MULTIPLIER = OPPONENT_SCORE_MULTIPLIER;
        w.IMMEDIATE_THREAT_PENALTY = IMMEDIATE_THREAT_PENALTY;
        w.IMMEDIATE_WIN_BONUS = IMMEDIATE_WIN_BONUS;
        w.IMMEDIATE_WIN_SQUARE_BONUS = IMMEDIATE_WIN_SQUARE_BONUS;
        w.OPPONENT_IMMEDIATE_WIN_SQUARE_PENALTY = OPPONENT_IMMEDIATE_WIN_SQUARE_PENALTY;
        w.MOBILITY_MULTIPLIER = MOBILITY_MULTIPLIER;
        w.CENTER_CONTROL_MULTIPLIER = CENTER_CONTROL_MULTIPLIER;
        w.OPPONENT_CENTER_CONTROL_MULTIPLIER = OPPONENT_CENTER_CONTROL_MULTIPLIER;
        w.PCORNER_CONTROL_MULTIPLIER = PCORNER_CONTROL_MULTIPLIER;
        w.OPPONENT_PCORNER_CONTROL_MULTIPLIER = OPPONENT_PCORNER_CONTROL_MULTIPLIER;
        w.BCORNER_CONTROL_MULTIPLIER = BCORNER_CONTROL_MULTIPLIER;
        w.OPPONENT_BCORNER_CONTROL_MULTIPLIER = OPPONENT_BCORNER_CONTROL_MULTIPLIER;
        w.PLAYER_FORKS_MULTIPLIER = PLAYER_FORKS_MULTIPLIER;
        w.OPPONENT_FORKS_MULTIPLIER = OPPONENT_FORKS_MULTIPLIER;
        w.OPPONENT_POTENTIAL_FORKS_PENALTY = OPPONENT_POTENTIAL_FORKS_PENALTY;
        w.DOUBLE_THREAT_BONUS = DOUBLE_THREAT_BONUS;
        w.OPPONENT_DOUBLE_THREAT_PENALTY = OPPONENT_DOUBLE_THREAT_PENALTY;
        w.BLOCKED_THREAT_INTERSECTION_BONUS = BLOCKED_THREAT_INTERSECTION_BONUS;
        return w;
    }

    public static Weights crossover(Weights p1, Weights p2) {
        // Uniform crossover
        Weights child = p1.copy();
        if (rand.nextBoolean()) child.SCORE_ONE = p2.SCORE_ONE;
        if (rand.nextBoolean()) child.SCORE_TWO = p2.SCORE_TWO;
        if (rand.nextBoolean()) child.SCORE_THREE = p2.SCORE_THREE;
        if (rand.nextBoolean()) child.SCORE_FOUR = p2.SCORE_FOUR;
        if (rand.nextBoolean()) child.OPEN_TWO_BONUS = p2.OPEN_TWO_BONUS;
        if (rand.nextBoolean()) child.CLOSED_THREE_BONUS = p2.CLOSED_THREE_BONUS;
        if (rand.nextBoolean()) child.BLOCKED_LINE_PENALTY = p2.BLOCKED_LINE_PENALTY;
        if (rand.nextBoolean()) child.OPEN_LINE_BONUS = p2.OPEN_LINE_BONUS;
        if (rand.nextBoolean()) child.NEAR_FORK_BONUS = p2.NEAR_FORK_BONUS;

        if (rand.nextBoolean()) child.CENTER_MULTIPLIER = p2.CENTER_MULTIPLIER;
        if (rand.nextBoolean()) child.OPPONENT_SCORE_MULTIPLIER = p2.OPPONENT_SCORE_MULTIPLIER;
        if (rand.nextBoolean()) child.IMMEDIATE_THREAT_PENALTY = p2.IMMEDIATE_THREAT_PENALTY;
        if (rand.nextBoolean()) child.IMMEDIATE_WIN_BONUS = p2.IMMEDIATE_WIN_BONUS;
        if (rand.nextBoolean()) child.IMMEDIATE_WIN_SQUARE_BONUS = p2.IMMEDIATE_WIN_SQUARE_BONUS;
        if (rand.nextBoolean()) child.OPPONENT_IMMEDIATE_WIN_SQUARE_PENALTY = p2.OPPONENT_IMMEDIATE_WIN_SQUARE_PENALTY;

        if (rand.nextBoolean()) child.MOBILITY_MULTIPLIER = p2.MOBILITY_MULTIPLIER;

        if (rand.nextBoolean()) child.CENTER_CONTROL_MULTIPLIER = p2.CENTER_CONTROL_MULTIPLIER;
        if (rand.nextBoolean()) child.OPPONENT_CENTER_CONTROL_MULTIPLIER = p2.OPPONENT_CENTER_CONTROL_MULTIPLIER;

        if (rand.nextBoolean()) child.PCORNER_CONTROL_MULTIPLIER = p2.PCORNER_CONTROL_MULTIPLIER;
        if (rand.nextBoolean()) child.OPPONENT_PCORNER_CONTROL_MULTIPLIER = p2.OPPONENT_PCORNER_CONTROL_MULTIPLIER;
        if (rand.nextBoolean()) child.BCORNER_CONTROL_MULTIPLIER = p2.BCORNER_CONTROL_MULTIPLIER;
        if (rand.nextBoolean()) child.OPPONENT_BCORNER_CONTROL_MULTIPLIER = p2.OPPONENT_BCORNER_CONTROL_MULTIPLIER;

        if (rand.nextBoolean()) child.PLAYER_FORKS_MULTIPLIER = p2.PLAYER_FORKS_MULTIPLIER;
        if (rand.nextBoolean()) child.OPPONENT_FORKS_MULTIPLIER = p2.OPPONENT_FORKS_MULTIPLIER;
        if (rand.nextBoolean()) child.OPPONENT_POTENTIAL_FORKS_PENALTY = p2.OPPONENT_POTENTIAL_FORKS_PENALTY;
        if (rand.nextBoolean()) child.DOUBLE_THREAT_BONUS = p2.DOUBLE_THREAT_BONUS;
        if (rand.nextBoolean()) child.OPPONENT_DOUBLE_THREAT_PENALTY = p2.OPPONENT_DOUBLE_THREAT_PENALTY;
        if (rand.nextBoolean()) child.BLOCKED_THREAT_INTERSECTION_BONUS = p2.BLOCKED_THREAT_INTERSECTION_BONUS;

        return child;
    }

    public void mutate(double mutationRate) {
        if (rand.nextDouble() < mutationRate) SCORE_ONE += randInRange(-5,5);
        if (rand.nextDouble() < mutationRate) SCORE_TWO += randInRange(-50,50);
        if (rand.nextDouble() < mutationRate) SCORE_THREE += randInRange(-200,200);
        if (rand.nextDouble() < mutationRate) SCORE_FOUR += randInRange(-10000,10000);
        if (rand.nextDouble() < mutationRate) OPEN_TWO_BONUS += randInRange(-40,40);
        if (rand.nextDouble() < mutationRate) CLOSED_THREE_BONUS += randInRange(-120,120);
        if (rand.nextDouble() < mutationRate) BLOCKED_LINE_PENALTY += randInRange(-5,5);
        if (rand.nextDouble() < mutationRate) OPEN_LINE_BONUS += randInRange(-5,5);
        if (rand.nextDouble() < mutationRate) NEAR_FORK_BONUS += randInRange(-20,20);

        if (rand.nextDouble() < mutationRate) CENTER_MULTIPLIER += randInRange(-5,5);
        if (rand.nextDouble() < mutationRate) OPPONENT_SCORE_MULTIPLIER += randInRange(-2,2);
        if (rand.nextDouble() < mutationRate) IMMEDIATE_THREAT_PENALTY += randInRange(-10000,10000);
        if (rand.nextDouble() < mutationRate) IMMEDIATE_WIN_BONUS += randInRange(-10000,10000);
        if (rand.nextDouble() < mutationRate) IMMEDIATE_WIN_SQUARE_BONUS += randInRange(-4000,4000);
        if (rand.nextDouble() < mutationRate) OPPONENT_IMMEDIATE_WIN_SQUARE_PENALTY += randInRange(-4000,4000);

        if (rand.nextDouble() < mutationRate) MOBILITY_MULTIPLIER += randInRange(-3,3);

        if (rand.nextDouble() < mutationRate) CENTER_CONTROL_MULTIPLIER += randInRange(-10,10);
        if (rand.nextDouble() < mutationRate) OPPONENT_CENTER_CONTROL_MULTIPLIER += randInRange(-10,10);

        if (rand.nextDouble() < mutationRate) PCORNER_CONTROL_MULTIPLIER += randInRange(-5,5);
        if (rand.nextDouble() < mutationRate) OPPONENT_PCORNER_CONTROL_MULTIPLIER += randInRange(-5,5);
        if (rand.nextDouble() < mutationRate) BCORNER_CONTROL_MULTIPLIER += randInRange(-5,5);
        if (rand.nextDouble() < mutationRate) OPPONENT_BCORNER_CONTROL_MULTIPLIER += randInRange(-5,5);

        if (rand.nextDouble() < mutationRate) PLAYER_FORKS_MULTIPLIER += randInRange(-500,500);
        if (rand.nextDouble() < mutationRate) OPPONENT_FORKS_MULTIPLIER += randInRange(-200,200);
        if (rand.nextDouble() < mutationRate) OPPONENT_POTENTIAL_FORKS_PENALTY += randInRange(-1000,1000);
        if (rand.nextDouble() < mutationRate) DOUBLE_THREAT_BONUS += randInRange(-1000,1000);
        if (rand.nextDouble() < mutationRate) OPPONENT_DOUBLE_THREAT_PENALTY += randInRange(-1000,1000);
        if (rand.nextDouble() < mutationRate) BLOCKED_THREAT_INTERSECTION_BONUS += randInRange(-800,800);
    }

    @Override
    public String toString() {
        return "Weights{" +
                "S1=" + SCORE_ONE +
                ", S2=" + SCORE_TWO +
                ", S3=" + SCORE_THREE +
                ", S4=" + SCORE_FOUR +
                ", O2B=" + OPEN_TWO_BONUS +
                ", CTB=" + CLOSED_THREE_BONUS +
                ", BLP=" + BLOCKED_LINE_PENALTY +
                ", OLB=" + OPEN_LINE_BONUS +
                ", NFB=" + NEAR_FORK_BONUS +
                ", CM=" + CENTER_MULTIPLIER +
                ", OSM=" + OPPONENT_SCORE_MULTIPLIER +
                ", ITP=" + IMMEDIATE_THREAT_PENALTY +
                ", IWB=" + IMMEDIATE_WIN_BONUS +
                ", IWSB=" + IMMEDIATE_WIN_SQUARE_BONUS +
                ", OIWS=" + OPPONENT_IMMEDIATE_WIN_SQUARE_PENALTY +
                ", MOB=" + MOBILITY_MULTIPLIER +
                ", CCM=" + CENTER_CONTROL_MULTIPLIER +
                ", OCCM=" + OPPONENT_CENTER_CONTROL_MULTIPLIER +
                ", Pcc=" + PCORNER_CONTROL_MULTIPLIER +
                ", Opc=" + OPPONENT_PCORNER_CONTROL_MULTIPLIER +
                ", Bc=" + BCORNER_CONTROL_MULTIPLIER +
                ", Obc=" + OPPONENT_BCORNER_CONTROL_MULTIPLIER +
                ", Pfm=" + PLAYER_FORKS_MULTIPLIER +
                ", Ofm=" + OPPONENT_FORKS_MULTIPLIER +
                ", OPFP=" + OPPONENT_POTENTIAL_FORKS_PENALTY +
                ", DTB=" + DOUBLE_THREAT_BONUS +
                ", ODTP=" + OPPONENT_DOUBLE_THREAT_PENALTY +
                ", BTIB=" + BLOCKED_THREAT_INTERSECTION_BONUS +
                '}';
    }
}

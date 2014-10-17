package game2048;

import ucb.util.CommandArgs;

import game2048.gui.Game;
import static game2048.Main.Side.*;

/** The main class for the 2048 game.
 *  @author Nick Dill
 */
public class Main {

    /** Size of the board: number of rows and of columns. */
    static final int SIZE = 4;
    /** Tile required to win and end the game. */
    static final int WIN = 2048;
    /** Number of squares on the board. */
    static final int SQUARES = SIZE * SIZE;

    /** Symbolic names for the four sides of a board. */
    static enum Side { NORTH, EAST, SOUTH, WEST };

    /** The main program.  ARGS may contain the options --seed=NUM,
     *  (random seed); --log (record moves and random tiles
     *  selected.); --testing (take random tiles and moves from
     *  standard input); and --no-display. */
    public static void main(String... args) {
        CommandArgs options =
            new CommandArgs("--seed=(\\d+) --log --testing --no-display",
                            args);
        if (!options.ok()) {
            System.err.println("Usage: java game2048.Main [ --seed=NUM ] "
                               + "[ --log ] [ --testing ] [ --no-display ]");
            System.exit(1);
        }

        Main game = new Main(options);
        while (game.play()) {
            /* No action */
        }
        System.exit(0);
    }

    /** A new Main object using OPTIONS as options (as for main). */
    Main(CommandArgs options) {
        boolean log = options.contains("--log"),
            display = !options.contains("--no-display");
        long seed = !options.contains("--seed") ? 0 : options.getLong("--seed");
        _testing = options.contains("--testing");
        _game = new Game("2048", SIZE, seed, log, display, _testing);
    }

    /** Play one game of 2048, updating the maximum score. Return true
     *  iff play should continue with another game, or false to exit. */
    boolean play() {
        while (true) {
            setRandomPiece();
            if (gameOver()) {
                _maxScore = Math.max(_maxScore, _score);
                _game.setScore(_score, _maxScore);
                _game.endGame();
            }

        GetMove:
            while (true) {
                String key = _game.readKey();
                switch (key) {
                case "Up": case "Down": case "Left": case "Right":
                case "\u2191": case "\u2193": case "\u2190": case "\u2192":
                    if (!gameOver() && tiltBoard(keyToSide(key))) {
                        break GetMove;
                    }
                    break;
                case "New Game":
                    _game.clear();
                    _count = 0;
                    _score = 0;
                    clearBoard();
                    return true;
                case "Quit":
                    return false;
                default:
                    break;
                }
            }
        }
    }
    /** Iteraters through each tile of the board setting it to 0
        for a new game. */
    void clearBoard() {
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                _board[r][c] = 0;
            }
        }
    }
    /** Return true iff the current game is over (no more moves
     *  possible). */
    boolean gameOver() {
        if (_count == SIZE * SIZE) {
            return fullBoard(false);
        } else {
            for (int r = 0; r < SIZE; r += 1) {
                for (int c = 0; c < SIZE; c += 1) {
                    if (_board[r][c] == WIN) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    /** Return if there are any moves left only if the board is full.
     *  X defaults to false, but can be set true if a tile is 2048.*/
    boolean fullBoard(boolean x) {
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                if (_board[r][c] == WIN) {
                    return true;
                }
                if (r < SIZE - 1 && c < SIZE - 1 && _board[r][c] > 0) {
                    if (_board[r][c] == _board[r + 1][c]
                        || _board[r][c] == _board[r][c + 1]) {
                        return false;
                    }
                } else {
                    if (r == SIZE - 1 && c < SIZE - 1) {
                        if (_board[r][c] == _board[r][c + 1]) {
                            return false;
                        }
                    } else if (r < SIZE - 1 && c == SIZE - 1) {
                        if (_board[r][c] == _board[r + 1][c]) {
                            return false;
                        }
                    }
                }
            }
        }
        _maxScore = Math.max(_score, _maxScore);
        return true;
    }

    /** Add a tile to a random, empty position, choosing a value (2 or
     *  4) at random.  Has no effect if the board is currently full. */
    void setRandomPiece() {
        _game.setScore(_score, _maxScore);
        if (_count == SQUARES) {
            return;
        } else {
            if (_count == 0) {
                checker();
                checker();
            } else {
                checker();
            }
            _game.displayMoves();
        }
    }
    /** Continues to generate random tiles until the tile exists on a
        nonoccupied coordinate. */
    void checker() {
        _game.setScore(_score, _maxScore);
        int[] t = _game.getRandomTile();
        if (_board[t[1]][t[2]] == 0) {
            _game.addTile(t[0], t[1], t[2]);
            _board[t[1]][t[2]] = t[0];
            _count++;
        } else {
            checker();
        }
    }


    /** Perform the result of tilting the board toward SIDE.
     *  Returns true iff the tilt changes the board. **/
    boolean tiltBoard(Side side) {
        int[][] board = new int[SIZE][SIZE];
        boolean[][] merges = new boolean[SIZE][SIZE];
        boolean moves = false;
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                board[r][c] =
                    _board[tiltRow(side, r, c)][tiltCol(side, r, c)];
                merges[r][c] = true;
                if (r > 0) {
                    if (board[r][c] != 0) {
                        int x = r; int y = c;
                        while (x > 0 && board[x - 1][y] == 0) {
                            board[x - 1][y] = board[x][y];
                            board[x][y] = 0; x--; moves = true;
                        }
                        if (x > 0) {
                            if (board[x - 1][y] == board[x][y]
                                && merges[x - 1][y]) {
                                _game.mergeTile(board[x][y], 2 * board[x][y],
                                                tiltRow(side, r, c),
                                                tiltCol(side, r, c),
                                                tiltRow(side, x - 1, y),
                                                tiltCol(side, x - 1, y));
                                board[x - 1][y] = 2 * board[x][y];
                                _score += 2 * board[x][y];
                                if (2 * board[x][y] == WIN) {
                                    _maxScore = Math.max(_score, _maxScore);
                                    _game.setScore(_score, _maxScore);
                                    _game.endGame();
                                }
                                board[x][y] = 0; moves = true;
                                merges[x - 1][y] = false; _count--;
                            } else {
                                _game.moveTile(board[x][y], tiltRow(side, r, c),
                                               tiltCol(side, r, c),
                                               tiltRow(side, x, y),
                                               tiltCol(side, x, y));
                            }
                        } else {
                            _game.moveTile(board[x][y], tiltRow(side, r, c),
                                           tiltCol(side, r, c),
                                           tiltRow(side, x, y),
                                           tiltCol(side, x, y));
                        }
                    }
                }
            }
        }
        _game.setScore(_score, _maxScore); _game.displayMoves();
        for (int r = 0; r < SIZE; r += 1) {
            for (int c = 0; c < SIZE; c += 1) {
                _board[tiltRow(side, r, c)][tiltCol(side, r, c)]
                    = board[r][c];
                merges[r][c] = true;
            }
        }
        return moves;
    }

    /** Return the row number on a playing board that corresponds to row R
     *  and column C of a board turned so that row 0 is in direction SIDE (as
     *  specified by the definitions of NORTH, EAST, etc.).  So, if SIDE
     *  is NORTH, then tiltRow simply returns R (since in that case, the
     *  board is not turned).  If SIDE is WEST, then column 0 of the tilted
     *  board corresponds to row SIZE - 1 of the untilted board, and
     *  tiltRow returns SIZE - 1 - C. */
    int tiltRow(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return r;
        case EAST:
            return c;
        case SOUTH:
            return SIZE - 1 - r;
        case WEST:
            return SIZE - 1 - c;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the column number on a playing board that corresponds to row
     *  R and column C of a board turned so that row 0 is in direction SIDE
     *  (as specified by the definitions of NORTH, EAST, etc.). So, if SIDE
     *  is NORTH, then tiltCol simply returns C (since in that case, the
     *  board is not turned).  If SIDE is WEST, then row 0 of the tilted
     *  board corresponds to column 0 of the untilted board, and tiltCol
     *  returns R. */
    int tiltCol(Side side, int r, int c) {
        switch (side) {
        case NORTH:
            return c;
        case EAST:
            return SIZE - 1 - r;
        case SOUTH:
            return SIZE - 1 - c;
        case WEST:
            return r;
        default:
            throw new IllegalArgumentException("Unknown direction");
        }
    }

    /** Return the side indicated by KEY ("Up", "Down", "Left",
     *  or "Right"). */
    Side keyToSide(String key) {
        switch (key) {
        case "Up":
            return NORTH;
        case "Down":
            return SOUTH;
        case "Left":
            return WEST;
        case "Right":
            return EAST;
        case "\u2191":
            return NORTH;
        case "\u2192":
            return EAST;
        case "\u2193":
            return SOUTH;
        case "\u2190":
            return WEST;
        default:
            throw new IllegalArgumentException("unknown key designation");
        }
    }

    /** Represents the board: _board[r][c] is the tile value at row R,
     *  column C, or 0 if there is no tile there. */
    private final int[][] _board = new int[SIZE][SIZE];

    /** True iff --testing option selected. */
    private boolean _testing;
    /** THe current input source and output sink. */
    private Game _game;
    /** The score of the current game, and the maximum final score
     *  over all games in this session. */
    private int _score, _maxScore;
    /** Number of tiles on the board. */
    private int _count;
}

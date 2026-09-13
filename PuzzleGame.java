import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * 数字滑动拼图（登录态 + 盘面尺寸 + 排布难度 + 圆角美化）
 * 盘面：3x3 / 4x4 / 5x5；排布难度：简单 / 中等 / 困难
 *
 * 排布难度的均匀性保证：
 * 1) 从已解状态做"不回头"随机游走（不会刚把方块推走又推回来，白走的步数少）；
 * 2) 每个候选盘面都要通过"难度验收"才会采用：
 *    - 3x3 用 IDA* 求出精确最优解距，落在档位区间内才采用；
 *    - 4x4/5x5 用启发式 h = 曼哈顿距离 + 2×线性冲突，落在档位区间内才采用；
 * 3) 多次抽样直到命中区间，保证同一档位内初始盘面难度基本一致，而不是全凭运气。
 */
public class PuzzleGame extends JFrame {

    private static final int EMPTY = 0;
    private static final Color BG_COLOR   = new Color(245, 247, 252);
    private static final Color TILE_BG    = new Color(79, 137, 225);
    private static final Color TILE_HOVER = new Color(100, 155, 235);
    private static final Color TILE_PRESS = new Color(60, 120, 210);
    private static final Color EMPTY_BG   = new Color(225, 230, 240);
    private static final Color ACCENT     = new Color(255, 185, 60);
    private static final Color CHIP_BG        = new Color(225, 231, 242);
    private static final Color CHIP_HOVER     = new Color(235, 240, 248);
    private static final Color CHIP_PRESS     = new Color(208, 217, 232);
    private static final Color CHIP_TEXT      = new Color(60, 80, 110);

    public static final int LEVEL_EASY = 0;
    public static final int LEVEL_MEDIUM = 1;
    public static final int LEVEL_HARD = 2;
    private static final String[] LEVEL_NAMES = {"简单", "中等", "困难"};

    /**
     * 排布参数表：[盘面尺寸-1][档位] = {游走步数, 难度下限, 难度上限}
     * 3x3 的区间按 IDA* 最优解距（理论上限 31）；
     * 4x4 / 5x5 的区间按启发式 h（曼哈顿 + 2×线性冲突）。
     * 区间由抽样校准得到（同档位内难度集中、三档之间明显拉开）。
     */
    private static final int[][][] LEVEL_CONFIG = {
        // 3x3：简单约 6-11 步可解，中等 16-21 步，困难 25-31 步
        { {  9,  6, 11}, { 24, 16, 21}, { 70, 25, 31} },
        // 4x4：按启发式 h 分档
        { { 20, 10, 16}, { 48, 22, 30}, {160, 36, 46} },
        // 5x5：按启发式 h 分档
        { { 25, 14, 22}, { 70, 34, 44}, {250, 58, 72} }
    };
    private static final int MAX_SHUFFLE_TRIES = 80;

    private int size;
    private int level;
    private int total;
    private int[] board;
    private RoundedButton[] buttons;
    private RoundedButton[] sizeButtons;
    private RoundedButton[] levelButtons;
    private JLabel stepLabel;
    private JLabel bestLabel;
    private int steps;
    private int best = -1;
    private String username;

    public PuzzleGame(String username) {
        this(username, 3, LEVEL_MEDIUM);
    }

    public PuzzleGame(String username, int size, int level) {
        this.username = username;
        this.size = size;
        this.level = level;
        this.total = size * size;
        this.board = new int[total];

        setTitle(buildTitle());
        setSize(540, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setIconImages(LoginFrame.loadIcons());
        getContentPane().setBackground(BG_COLOR);

        // ---------- 第一行：用户信息 / 步数 / 最佳 + 重新开始 ----------
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(BG_COLOR);
        topPanel.setBorder(new EmptyBorder(18, 24, 8, 24));

        JPanel leftInfo = new JPanel();
        leftInfo.setBackground(BG_COLOR);
        leftInfo.setLayout(new BoxLayout(leftInfo, BoxLayout.Y_AXIS));

        JLabel userLabel = new JLabel("👤 " + username);
        userLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        userLabel.setForeground(new Color(100, 110, 130));

        stepLabel = new JLabel("步数：0", SwingConstants.LEFT);
        stepLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        stepLabel.setForeground(new Color(40, 50, 80));

        bestLabel = new JLabel("最佳：—", SwingConstants.LEFT);
        bestLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        bestLabel.setForeground(ACCENT);

        leftInfo.add(userLabel);
        leftInfo.add(Box.createVerticalStrut(4));
        leftInfo.add(stepLabel);
        leftInfo.add(bestLabel);

        RoundedButton restartBtn = new RoundedButton("重新开始", TILE_BG, TILE_HOVER, TILE_PRESS);
        restartBtn.setForeground(Color.WHITE);
        restartBtn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        restartBtn.addActionListener(e -> shuffleBoard());

        topPanel.add(leftInfo, BorderLayout.CENTER);
        topPanel.add(restartBtn, BorderLayout.EAST);

        // ---------- 第二行：盘面选择 + 排布难度选择 ----------
        JPanel selectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        selectPanel.setBackground(BG_COLOR);
        selectPanel.setBorder(new EmptyBorder(2, 18, 10, 18));

        selectPanel.add(sectionLabel("盘面"));
        sizeButtons = new RoundedButton[3];
        for (int i = 0; i < 3; i++) {
            final int s = i + 3;
            RoundedButton b = makeChip((s) + "×" + s);
            b.addActionListener(e -> switchSize(s));
            sizeButtons[i] = b;
            selectPanel.add(b);
        }

        selectPanel.add(Box.createHorizontalStrut(16));
        selectPanel.add(sectionLabel("排布"));
        levelButtons = new RoundedButton[3];
        for (int i = 0; i < 3; i++) {
            final int lv = i;
            RoundedButton b = makeChip(LEVEL_NAMES[i]);
            b.addActionListener(e -> chooseLevel(lv));
            levelButtons[i] = b;
            selectPanel.add(b);
        }
        refreshChipStyles();

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBackground(BG_COLOR);
        north.add(topPanel);
        north.add(selectPanel);
        add(north, BorderLayout.NORTH);

        // ---------- 棋盘 ----------
        JPanel gridOuter = new JPanel(new BorderLayout());
        gridOuter.setBackground(BG_COLOR);
        gridOuter.setBorder(new EmptyBorder(6, 30, 20, 30));

        int cellSize = size == 3 ? 120 : (size == 4 ? 90 : 72);
        buttons = new RoundedButton[total];
        JPanel gridPanel = new JPanel(new GridLayout(size, size, 6, 6));
        gridPanel.setBackground(BG_COLOR);
        int fontSize = size == 3 ? 44 : (size == 4 ? 32 : 26);
        for (int i = 0; i < total; i++) {
            RoundedButton b = new RoundedButton("", TILE_BG, TILE_HOVER, TILE_PRESS);
            b.setForeground(Color.WHITE);
            b.setFont(new Font("微软雅黑", Font.BOLD, fontSize));
            b.setFocusable(false);
            b.setPreferredSize(new Dimension(cellSize, cellSize));
            final int idx = i;
            b.addActionListener(e -> movePiece(idx));
            buttons[i] = b;
            gridPanel.add(b);
        }
        gridOuter.add(gridPanel, BorderLayout.CENTER);
        add(gridOuter, BorderLayout.CENTER);

        shuffleBoard();
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        lbl.setForeground(new Color(130, 138, 155));
        lbl.setBorder(new EmptyBorder(0, 6, 0, 2));
        return lbl;
    }

    private RoundedButton makeChip(String text) {
        RoundedButton b = new RoundedButton(text, CHIP_BG, CHIP_HOVER, CHIP_PRESS);
        b.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        b.setForeground(CHIP_TEXT);
        b.setFocusable(false);
        return b;
    }

    private void refreshChipStyles() {
        for (int i = 0; i < sizeButtons.length; i++) {
            boolean selected = (i + 3) == size;
            sizeButtons[i].setNormalBg(selected ? TILE_BG : CHIP_BG,
                                      selected ? TILE_HOVER : CHIP_HOVER,
                                      selected ? TILE_PRESS : CHIP_PRESS);
            sizeButtons[i].setForeground(selected ? Color.WHITE : CHIP_TEXT);
        }
        for (int i = 0; i < levelButtons.length; i++) {
            boolean selected = i == level;
            levelButtons[i].setNormalBg(selected ? TILE_BG : CHIP_BG,
                                        selected ? TILE_HOVER : CHIP_HOVER,
                                        selected ? TILE_PRESS : CHIP_PRESS);
            levelButtons[i].setForeground(selected ? Color.WHITE : CHIP_TEXT);
        }
    }

    private String buildTitle() {
        return "🧩 数字拼图 · " + username + " · " + size + "x" + size + " · 排布" + LEVEL_NAMES[level];
    }

    private void switchSize(int newSize) {
        if (newSize == this.size) { shuffleBoard(); return; }
        dispose();
        SwingUtilities.invokeLater(() -> new PuzzleGame(username, newSize, level).setVisible(true));
    }

    private void chooseLevel(int newLevel) {
        if (newLevel == this.level) { shuffleBoard(); return; }
        this.level = newLevel;
        this.best = -1; // 排布难度变了，旧纪录不可比
        setTitle(buildTitle());
        refreshChipStyles();
        shuffleBoard();
    }

    /**
     * 按当前排布难度生成均匀的初始盘面：
     * 反复做"不回头随机游走"得到候选盘面，用难度指标验收，命中区间才采用；
     * 始终未命中时，采用离区间中心最近的候选（极端兜底）。
     */
    private void shuffleBoard() {
        int[] cfg = LEVEL_CONFIG[size - 3][level];
        int walkMoves = cfg[0];
        int low = cfg[1];
        int high = cfg[2];
        int center = (low + high) / 2;

        Random rand = new Random();
        int[] fallback = null;
        int fallbackGap = Integer.MAX_VALUE;

        for (int attempt = 0; attempt < MAX_SHUFFLE_TRIES; attempt++) {
            int[] candidate = randomWalk(walkMoves, rand);
            if (isBoardSolved(candidate)) continue;

            int h = heuristic(candidate);
            boolean accepted;
            if (size == 3) {
                accepted = acceptByExactDistance(candidate, h, low, high);
            } else {
                accepted = h >= low && h <= high;
            }

            if (accepted) {
                board = candidate;
                steps = 0;
                updateUI();
                return;
            }
            int gap = Math.abs(h - center);
            if (gap < fallbackGap) {
                fallbackGap = gap;
                fallback = candidate;
            }
        }

        board = fallback != null ? fallback : randomWalk(walkMoves, rand);
        steps = 0;
        updateUI();
    }

    /**
     * 3x3 精确验收：判定最优解距是否落在 [low, high]。
     * 困难档只需证明"至少要 low 步"（8 数字拼图任意可达状态最优解距 <= 31 = high）。
     */
    private boolean acceptByExactDistance(int[] candidate, int h, int low, int high) {
        if (level == LEVEL_HARD) {
            if (h >= low) return true;                 // h 是下界：h>=low 必然解距>=low
            return boundedSolve(candidate, low - 1) == -1; // low-1 步内解不开 => 解距>=low
        }
        if (h > high) return false;                    // h 是下界：超了 upper 必然太难
        int dist = boundedSolve(candidate, high);
        return dist != -1 && dist >= low;
    }

    /**
     * 从已解状态执行 moves 次随机合法移动，且不立即撤销上一步。
     * 由已解状态可达 => 盘面 100% 有解。
     */
    private int[] randomWalk(int moves, Random rand) {
        int[] b = new int[total];
        for (int i = 0; i < total - 1; i++) b[i] = i + 1;
        b[total - 1] = EMPTY;

        int empty = total - 1;
        int prevEmpty = -1;
        for (int m = 0; m < moves; m++) {
            List<Integer> neighbors = neighborsOf(empty);
            if (prevEmpty >= 0 && neighbors.size() > 1) {
                neighbors.remove((Integer) prevEmpty);
            }
            int pick = neighbors.get(rand.nextInt(neighbors.size()));
            b[empty] = b[pick];
            b[pick] = EMPTY;
            prevEmpty = empty;
            empty = pick;
        }
        return b;
    }

    private List<Integer> neighborsOf(int idx) {
        List<Integer> list = new ArrayList<>();
        int r = idx / size, c = idx % size;
        if (r > 0) list.add((r - 1) * size + c);
        if (r < size - 1) list.add((r + 1) * size + c);
        if (c > 0) list.add(r * size + (c - 1));
        if (c < size - 1) list.add(r * size + (c + 1));
        return list;
    }

    /** 难度指标：曼哈顿距离 + 2×线性冲突（可采纳启发式，h <= 真实最优解距） */
    private int heuristic(int[] b) {
        int h = 0;
        for (int i = 0; i < total; i++) {
            if (b[i] == EMPTY) continue;
            int goal = b[i] - 1;
            h += Math.abs(i / size - goal / size) + Math.abs(i % size - goal % size);
        }
        // 行内线性冲突：两个本应在同一行的方块，左右顺序颠倒
        for (int r = 0; r < size; r++) {
            for (int c1 = 0; c1 < size; c1++) {
                int t1 = b[r * size + c1];
                if (t1 == EMPTY || (t1 - 1) / size != r) continue;
                for (int c2 = c1 + 1; c2 < size; c2++) {
                    int t2 = b[r * size + c2];
                    if (t2 == EMPTY || (t2 - 1) / size != r) continue;
                    if ((t1 - 1) % size > (t2 - 1) % size) h += 2;
                }
            }
        }
        // 列内线性冲突
        for (int c = 0; c < size; c++) {
            for (int r1 = 0; r1 < size; r1++) {
                int t1 = b[r1 * size + c];
                if (t1 == EMPTY || (t1 - 1) % size != c) continue;
                for (int r2 = r1 + 1; r2 < size; r2++) {
                    int t2 = b[r2 * size + c];
                    if (t2 == EMPTY || (t2 - 1) % size != c) continue;
                    if ((t1 - 1) / size > (t2 - 1) / size) h += 2;
                }
            }
        }
        return h;
    }

    // ================= 3x3 IDA* 有界求解 =================
    // 仅用于 3x3 难度验收：返回 <= cutoff 的最优解距；若最优解距 > cutoff 返回 -1。

    private static final int IDA_INF = Integer.MAX_VALUE;
    private static final int IDA_FOUND = -2;
    private int[] solverBoard;

    private int boundedSolve(int[] src, int cutoff) {
        this.solverBoard = src.clone();
        int empty = -1;
        for (int i = 0; i < 9; i++) {
            if (solverBoard[i] == EMPTY) { empty = i; break; }
        }
        int bound = Math.min(heuristic(solverBoard), cutoff + 1);
        while (bound <= cutoff) {
            int t = idaDfs(0, bound, -1, empty);
            if (t == IDA_FOUND) return bound;
            if (t == IDA_INF || t > cutoff) return -1;
            bound = t;
        }
        return -1;
    }

    /** 方向：上/下/左/右，opposite[d] 为其反方向（用于不回退） */
    private int idaDfs(int g, int bound, int prevDir, int empty) {
        int h = heuristic(solverBoard);
        if (h == 0) return IDA_FOUND;
        int f = g + h;
        if (f > bound) return f;

        int r = empty / 3, c = empty % 3;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        int[] opposite = {1, 0, 3, 2};
        int min = IDA_INF;

        for (int d = 0; d < 4; d++) {
            if (prevDir >= 0 && d == opposite[prevDir]) continue;
            int nr = r + dirs[d][0], nc = c + dirs[d][1];
            if (nr < 0 || nr > 2 || nc < 0 || nc > 2) continue;
            int next = nr * 3 + nc;

            solverBoard[empty] = solverBoard[next];
            solverBoard[next] = EMPTY;
            int t = idaDfs(g + 1, bound, d, next);
            solverBoard[next] = solverBoard[empty];
            solverBoard[empty] = EMPTY;

            if (t == IDA_FOUND) return IDA_FOUND;
            if (t < min) min = t;
        }
        return min;
    }

    private void movePiece(int idx) {
        int emptyIdx = findEmpty();
        if (emptyIdx == -1) return;
        if (!isAdjacent(idx, emptyIdx)) return;

        int tmp = board[idx];
        board[idx] = board[emptyIdx];
        board[emptyIdx] = tmp;
        steps++;
        updateUI();

        if (isBoardSolved(board)) {
            boolean newBest = (best == -1 || steps < best);
            if (newBest) best = steps;
            String msg = "🎉 恭喜 " + username + " 完成！\n步数：" + steps
                    + (newBest ? "\n✨ 新纪录！" : "\n当前最佳：" + best);
            JOptionPane.showMessageDialog(this, msg, "胜利", JOptionPane.INFORMATION_MESSAGE);
            shuffleBoard();
        }
    }

    private int findEmpty() {
        for (int i = 0; i < total; i++) if (board[i] == EMPTY) return i;
        return -1;
    }

    private boolean isAdjacent(int a, int b) {
        int ra = a / size, ca = a % size;
        int rb = b / size, cb = b % size;
        return (Math.abs(ra - rb) + Math.abs(ca - cb)) == 1;
    }

    private boolean isBoardSolved(int[] b) {
        for (int i = 0; i < total - 1; i++) if (b[i] != i + 1) return false;
        return b[total - 1] == EMPTY;
    }

    private void updateUI() {
        for (int i = 0; i < total; i++) {
            if (board[i] == EMPTY) {
                buttons[i].setText("");
                buttons[i].setEnabled(false);
                buttons[i].setNormalBg(EMPTY_BG, EMPTY_BG, EMPTY_BG);
            } else {
                buttons[i].setText(String.valueOf(board[i]));
                buttons[i].setEnabled(true);
                buttons[i].setNormalBg(TILE_BG, TILE_HOVER, TILE_PRESS);
            }
        }
        stepLabel.setText("步数：" + steps);
        bestLabel.setText("最佳：" + (best == -1 ? "—" : best));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 数字滑动拼图（带登录态 + 难度选择 + 圆角美化）
 * 难度：3x3（简单）/ 4x4（标准）
 */
public class PuzzleGame extends JFrame {

    private static final int EMPTY = 0;
    private static final Color BG_COLOR   = new Color(245, 247, 252);
    private static final Color TILE_BG    = new Color(79, 137, 225);
    private static final Color TILE_HOVER = new Color(100, 155, 235);
    private static final Color TILE_PRESS = new Color(60, 120, 210);
    private static final Color EMPTY_BG   = new Color(225, 230, 240);
    private static final Color ACCENT     = new Color(255, 185, 60);

    private int size;
    private int total;
    private int[] board;
    private RoundedButton[] buttons;
    private JLabel stepLabel;
    private JLabel bestLabel;
    private int steps;
    private int best = -1;
    private String username;

    public PuzzleGame(String username) {
        this(username, 3);
    }

    public PuzzleGame(String username, int size) {
        this.username = username;
        this.size = size;
        this.total = size * size;
        this.board = new int[total];

        setTitle("🧩 数字拼图 · " + username + " · " + size + "x" + size);
        setSize(540, 640);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_COLOR);

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

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(BG_COLOR);

        RoundedButton easyBtn = new RoundedButton("简单 3×3",
                new Color(200, 220, 240), new Color(215, 230, 250), new Color(180, 200, 220));
        easyBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        easyBtn.setForeground(new Color(60, 80, 110));
        easyBtn.addActionListener(e -> switchSize(3));

        RoundedButton hardBtn = new RoundedButton("标准 4×4",
                new Color(200, 220, 240), new Color(215, 230, 250), new Color(180, 200, 220));
        hardBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        hardBtn.setForeground(new Color(60, 80, 110));
        hardBtn.addActionListener(e -> switchSize(4));

        RoundedButton restartBtn = new RoundedButton("重新开始", TILE_BG, TILE_HOVER, TILE_PRESS);
        restartBtn.setForeground(Color.WHITE);
        restartBtn.setFont(new Font("微软雅黑", Font.BOLD, 13));
        restartBtn.addActionListener(e -> shuffleBoard());

        btnPanel.add(easyBtn);
        btnPanel.add(hardBtn);
        btnPanel.add(restartBtn);

        topPanel.add(leftInfo, BorderLayout.CENTER);
        topPanel.add(btnPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        JPanel gridOuter = new JPanel(new BorderLayout());
        gridOuter.setBackground(BG_COLOR);
        gridOuter.setBorder(new EmptyBorder(10, 30, 20, 30));

        int cellSize = size == 3 ? 120 : 90;
        buttons = new RoundedButton[total];
        JPanel gridPanel = new JPanel(new GridLayout(size, size, 6, 6));
        gridPanel.setBackground(BG_COLOR);
        for (int i = 0; i < total; i++) {
            RoundedButton b = new RoundedButton("", TILE_BG, TILE_HOVER, TILE_PRESS);
            b.setForeground(Color.WHITE);
            b.setFont(new Font("微软雅黑", Font.BOLD, size == 3 ? 44 : 32));
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

    private void switchSize(int newSize) {
        if (newSize == this.size) { shuffleBoard(); return; }
        dispose();
        SwingUtilities.invokeLater(() -> new PuzzleGame(username, newSize).setVisible(true));
    }

    /**
     * 打乱：从已解状态执行 15~20 次随机合法移动
     * 好处：100% 有解，且打乱程度可控（难度低）
     */
    private void shuffleBoard() {
        for (int i = 0; i < total - 1; i++) board[i] = i + 1;
        board[total - 1] = EMPTY;

        Random rand = new Random();
        int moves = 15 + rand.nextInt(6);
        for (int m = 0; m < moves; m++) {
            int emptyIdx = findEmpty();
            List<Integer> neighbors = neighborsOf(emptyIdx);
            int pick = neighbors.get(rand.nextInt(neighbors.size()));
            int tmp = board[emptyIdx];
            board[emptyIdx] = board[pick];
            board[pick] = tmp;
        }
        steps = 0;
        updateUI();
    }

    private int findEmpty() {
        for (int i = 0; i < total; i++) if (board[i] == EMPTY) return i;
        return -1;
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

    private void movePiece(int idx) {
        int emptyIdx = findEmpty();
        if (emptyIdx == -1) return;
        if (!isAdjacent(idx, emptyIdx)) return;

        int tmp = board[idx];
        board[idx] = board[emptyIdx];
        board[emptyIdx] = tmp;
        steps++;
        updateUI();

        if (isSolved()) {
            boolean newBest = (best == -1 || steps < best);
            if (newBest) best = steps;
            String msg = "🎉 恭喜 " + username + " 完成！\n步数：" + steps
                    + (newBest ? "\n✨ 新纪录！" : "\n当前最佳：" + best);
            JOptionPane.showMessageDialog(this, msg, "胜利", JOptionPane.INFORMATION_MESSAGE);
            shuffleBoard();
        }
    }

    private boolean isAdjacent(int a, int b) {
        int ra = a / size, ca = a % size;
        int rb = b / size, cb = b % size;
        return (Math.abs(ra - rb) + Math.abs(ca - cb)) == 1;
    }

    private boolean isSolved() {
        for (int i = 0; i < total - 1; i++) if (board[i] != i + 1) return false;
        return board[total - 1] == EMPTY;
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

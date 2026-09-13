import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

/**
 * 3x3 数字滑动拼图小游戏
 * 点击与空位相邻的方块进行移动，将数字 1~8 按顺序排列即为胜利
 */
public class PuzzleGame extends JFrame {

    private static final int SIZE = 3;          // 网格大小（3x3）
    private static final int TOTAL = SIZE * SIZE; // 总方块数
    private static final int EMPTY = 0;          // 空位标记

    private int[] board;                         // 拼图面板数据（1~8 + 0 空位）
    private JButton[] buttons;                   // 方块按钮
    private JLabel stepLabel;                    // 步数显示
    private int steps;                           // 当前步数

    public PuzzleGame() {
        // 窗口基础设置
        setTitle("数字拼图小游戏");
        setSize(360, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 居中显示

        board = new int[TOTAL];
        buttons = new JButton[TOTAL];
        steps = 0;

        // 顶部：步数 + 重开按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        stepLabel = new JLabel("步数：0", SwingConstants.CENTER);
        stepLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        JButton restartBtn = new JButton("重新开始");
        restartBtn.addActionListener(e -> shuffleBoard());
        topPanel.add(stepLabel, BorderLayout.CENTER);
        topPanel.add(restartBtn, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 中间：3x3 拼图面板
        JPanel gridPanel = new JPanel(new GridLayout(SIZE, SIZE, 4, 4));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (int i = 0; i < TOTAL; i++) {
            buttons[i] = new JButton();
            buttons[i].setFont(new Font("微软雅黑", Font.BOLD, 40));
            buttons[i].setFocusable(false);
            final int idx = i;
            buttons[i].addActionListener(e -> movePiece(idx));
            gridPanel.add(buttons[i]);
        }
        add(gridPanel, BorderLayout.CENTER);

        shuffleBoard();
    }

    /**
     * 打乱拼图面板，确保打乱后的状态有解
     */
    private void shuffleBoard() {
        // 初始化为有序状态
        for (int i = 0; i < TOTAL - 1; i++) {
            board[i] = i + 1;
        }
        board[TOTAL - 1] = EMPTY;

        Random rand = new Random();
        // Fisher-Yates 洗牌
        do {
            for (int i = TOTAL - 1; i > 0; i--) {
                int j = rand.nextInt(i + 1);
                int tmp = board[i];
                board[i] = board[j];
                board[j] = tmp;
            }
        } while (!isSolvable() || isSolved()); // 必须有解且不是已经胜利的状态

        steps = 0;
        updateUI();
    }

    /**
     * 判断当前打乱的拼图是否有解（3x3 网格：逆序数为偶数时有解）
     */
    private boolean isSolvable() {
        int inversions = 0;
        for (int i = 0; i < TOTAL - 1; i++) {
            for (int j = i + 1; j < TOTAL; j++) {
                if (board[i] != EMPTY && board[j] != EMPTY && board[i] > board[j]) {
                    inversions++;
                }
            }
        }
        return inversions % 2 == 0;
    }

    /**
     * 点击方块：若与空位相邻则交换，刷新 UI，检查胜利
     */
    private void movePiece(int idx) {
        int emptyIdx = findEmpty();
        if (emptyIdx == -1) return;

        // 判断 idx 和 emptyIdx 是否在 3x3 网格中相邻（上下左右）
        if (isAdjacent(idx, emptyIdx)) {
            // 交换空位和点击的方块
            int tmp = board[idx];
            board[idx] = board[emptyIdx];
            board[emptyIdx] = tmp;

            steps++;
            updateUI();

            if (isSolved()) {
                JOptionPane.showMessageDialog(this, "恭喜！用了 " + steps + " 步完成！",
                        "胜利", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    /** 找到空位（值为 0）在数组中的下标 */
    private int findEmpty() {
        for (int i = 0; i < TOTAL; i++) {
            if (board[i] == EMPTY) return i;
        }
        return -1;
    }

    /** 判断两个下标在 3x3 网格中是否相邻 */
    private boolean isAdjacent(int a, int b) {
        int rowA = a / SIZE, colA = a % SIZE;
        int rowB = b / SIZE, colB = b % SIZE;
        return (Math.abs(rowA - rowB) + Math.abs(colA - colB)) == 1;
    }

    /** 判断拼图是否完成：1~8 按顺序，空位在最后 */
    private boolean isSolved() {
        for (int i = 0; i < TOTAL - 1; i++) {
            if (board[i] != i + 1) return false;
        }
        return board[TOTAL - 1] == EMPTY;
    }

    /** 刷新所有按钮文字和步数显示 */
    private void updateUI() {
        for (int i = 0; i < TOTAL; i++) {
            if (board[i] == EMPTY) {
                buttons[i].setText("");
                buttons[i].setEnabled(false);
                buttons[i].setBackground(Color.LIGHT_GRAY);
            } else {
                buttons[i].setText(String.valueOf(board[i]));
                buttons[i].setEnabled(true);
                buttons[i].setBackground(null);
            }
        }
        stepLabel.setText("步数：" + steps);
    }

    public static void main(String[] args) {
        // 在事件调度线程中启动 Swing
        SwingUtilities.invokeLater(() -> new PuzzleGame().setVisible(true));
    }
}

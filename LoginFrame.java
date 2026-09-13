import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class LoginFrame extends JFrame {

    private static final Color BG_COLOR        = new Color(240, 244, 250);
    private static final Color PRIMARY          = new Color(79, 137, 225);
    private static final Color PRIMARY_HOVER    = new Color(100, 155, 235);
    private static final Color PRIMARY_PRESS    = new Color(60, 120, 210);
    private static final Color SECONDARY        = new Color(200, 210, 225);
    private static final Color SECONDARY_HOVER  = new Color(215, 223, 235);
    private static final Color SECONDARY_PRESS  = new Color(180, 192, 210);
    private static final Map<String, String> USERS = new HashMap<>();

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel messageLabel;

    public LoginFrame() {
        setTitle("数字拼图 - 登录");
        setSize(480, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setIconImages(loadIcons());
        getContentPane().setBackground(BG_COLOR);

        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(BG_COLOR);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBorder(new EmptyBorder(30, 0, 10, 0));
        JLabel logo = new JLabel("🧩 数字拼图", SwingConstants.CENTER);
        logo.setFont(new Font("微软雅黑", Font.BOLD, 32));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = new JLabel("滑动方块，找回你的记忆", SwingConstants.CENTER);
        subtitle.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        subtitle.setForeground(new Color(120, 130, 150));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.add(logo);
        titlePanel.add(Box.createVerticalStrut(6));
        titlePanel.add(subtitle);
        add(titlePanel, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(0, 60, 30, 60));
        mainPanel.add(buildLoginPanel(), "login");
        mainPanel.add(buildRegisterPanel(), "register");
        cardLayout.show(mainPanel, "login");
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel buildLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 5, 6, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        gbc.gridy = 0;
        JLabel uLbl = new JLabel("用户名");
        uLbl.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        p.add(uLbl, gbc);
        usernameField = styledTextField();
        gbc.gridy = 1; gbc.weightx = 1.0;
        p.add(usernameField, gbc);

        gbc.gridy = 2; gbc.weightx = 0;
        JLabel pLbl = new JLabel("密码");
        pLbl.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        p.add(pLbl, gbc);
        passwordField = styledPasswordField();
        gbc.gridy = 3; gbc.weightx = 1.0;
        p.add(passwordField, gbc);

        messageLabel = new JLabel(" ", SwingConstants.CENTER);
        messageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        messageLabel.setForeground(Color.RED);
        gbc.gridy = 4; gbc.weightx = 0;
        p.add(messageLabel, gbc);

        RoundedButton loginBtn = new RoundedButton("登  录", PRIMARY, PRIMARY_HOVER, PRIMARY_PRESS);
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFont(new Font("微软雅黑", Font.BOLD, 15));
        loginBtn.addActionListener(e -> doLogin());
        gbc.gridy = 5; gbc.weightx = 1.0;
        p.add(loginBtn, gbc);

        RoundedButton switchBtn = new RoundedButton("没有账号？去注册", SECONDARY, SECONDARY_HOVER, SECONDARY_PRESS);
        switchBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        switchBtn.setForeground(new Color(80, 90, 110));
        switchBtn.addActionListener(e -> {
            setTitle("数字拼图 - 注册");
            cardLayout.show(mainPanel, "register");
            messageLabel.setText(" ");
        });
        gbc.gridy = 6;
        p.add(switchBtn, gbc);
        return p;
    }

    private JPanel buildRegisterPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;

        JTextField uF = styledTextField();
        JPasswordField pF = styledPasswordField();
        JPasswordField cF = styledPasswordField();
        JLabel msg = new JLabel(" ", SwingConstants.CENTER);
        msg.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        msg.setForeground(Color.RED);

        gbc.gridy = 0; p.add(label("用户名"), gbc);
        gbc.gridy = 1; gbc.weightx = 1.0; p.add(uF, gbc);
        gbc.gridy = 2; gbc.weightx = 0; p.add(label("密码（≥3位）"), gbc);
        gbc.gridy = 3; gbc.weightx = 1.0; p.add(pF, gbc);
        gbc.gridy = 4; gbc.weightx = 0; p.add(label("确认密码"), gbc);
        gbc.gridy = 5; gbc.weightx = 1.0; p.add(cF, gbc);
        gbc.gridy = 6; gbc.weightx = 0; p.add(msg, gbc);

        RoundedButton regBtn = new RoundedButton("注  册", PRIMARY, PRIMARY_HOVER, PRIMARY_PRESS);
        regBtn.setForeground(Color.WHITE);
        regBtn.setFont(new Font("微软雅黑", Font.BOLD, 15));
        regBtn.addActionListener(e -> {
            String u = uF.getText().trim();
            String pw = new String(pF.getPassword());
            String cf = new String(cF.getPassword());
            if (u.isEmpty() || pw.isEmpty()) { msg.setText("用户名和密码不能为空"); return; }
            if (pw.length() < 3) { msg.setText("密码至少3位"); return; }
            if (!pw.equals(cf)) { msg.setText("两次密码不一致"); return; }
            if (USERS.containsKey(u)) { msg.setText("用户名已存在"); return; }
            USERS.put(u, pw);
            msg.setForeground(new Color(34, 139, 34));
            msg.setText("✅ 注册成功！");
            uF.setText(""); pF.setText(""); cF.setText("");
            Timer t = new Timer(700, ev -> {
                setTitle("数字拼图 - 登录");
                cardLayout.show(mainPanel, "login");
                usernameField.setText(u);
                passwordField.requestFocus();
                messageLabel.setText(" ");
            });
            t.setRepeats(false);
            t.start();
        });
        gbc.gridy = 7; gbc.weightx = 1.0; p.add(regBtn, gbc);

        RoundedButton backBtn = new RoundedButton("已有账号？返回登录", SECONDARY, SECONDARY_HOVER, SECONDARY_PRESS);
        backBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        backBtn.setForeground(new Color(80, 90, 110));
        backBtn.addActionListener(e -> {
            setTitle("数字拼图 - 登录");
            cardLayout.show(mainPanel, "login");
            msg.setText(" ");
        });
        gbc.gridy = 8; p.add(backBtn, gbc);
        return p;
    }

    private void doLogin() {
        String u = usernameField.getText().trim();
        String p = new String(passwordField.getPassword());
        if (u.isEmpty() || p.isEmpty()) { messageLabel.setText("请输入用户名和密码"); return; }
        if (!USERS.containsKey(u)) { messageLabel.setText("用户不存在，请先注册"); return; }
        if (!USERS.get(u).equals(p)) { messageLabel.setText("密码错误"); return; }
        dispose();
        SwingUtilities.invokeLater(() -> new PuzzleGame(u).setVisible(true));
    }

    /** Load multi-size window/taskbar icons from jar classpath. */
    static java.util.List<java.awt.Image> loadIcons() {
        java.util.List<java.awt.Image> icons = new java.util.ArrayList<>();
        String[] resources = {
                "/assets/icon-16.png", "/assets/icon-32.png",
                "/assets/icon-64.png", "/assets/icon-256.png"
        };
        for (String res : resources) {
            java.net.URL url = LoginFrame.class.getResource(res);
            if (url != null) {
                icons.add(new ImageIcon(url).getImage());
            }
        }
        return icons;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        return l;
    }
    private JTextField styledTextField() {
        JTextField f = new JTextField();
        f.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        f.setBackground(Color.WHITE);
        return f;
    }
    private JPasswordField styledPasswordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        f.setBackground(Color.WHITE);
        return f;
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
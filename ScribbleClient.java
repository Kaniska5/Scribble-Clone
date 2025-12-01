import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.border.*;

public class ScribbleClient extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerId;
    private String playerName;
    private String currentRoom;
    private boolean isDrawing = false;
    private Map<String, String> idToName = new HashMap<>();
    
    // UI Components
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private DrawingCanvas canvas;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JTextField guessInput;
    private JPanel playerListPanel;
    private JPanel toolPanel;
    private JLabel wordLabel;
    private JLabel timerLabel;
    private JPanel wordChoicePanel;
    private JButton[] wordButtons;
    private JPanel scorePanel;
    private Color selectedColor = Color.BLACK;
    private int brushSize = 5;
    
    // Avatar customization
    private Color avatarColor = new Color(255, 87, 51);
    private String avatarAccessory = "none";

    public ScribbleClient() {
        setTitle("Scribble.io");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initUI();
        showLoginScreen();
        
        setVisible(true);
    }

    private void initUI() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        add(mainPanel);
    }

    private void showLoginScreen() {
        JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        JLabel titleLabel = new JLabel("Scribble.io");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(new Color(50, 50, 200));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        loginPanel.add(titleLabel, gbc);
        
        JLabel nameLabel = new JLabel("Enter Your Name:");
        nameLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        loginPanel.add(nameLabel, gbc);
        
        JTextField nameField = new JTextField(20);
        nameField.setFont(new Font("Arial", Font.PLAIN, 16));
        gbc.gridx = 1;
        loginPanel.add(nameField, gbc);
        
        // Avatar customization
        JLabel avatarLabel = new JLabel("Choose Avatar Color:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        loginPanel.add(avatarLabel, gbc);
        
        JPanel colorPanel = new JPanel();
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, 
                         Color.ORANGE, Color.PINK, Color.CYAN, Color.MAGENTA};
        for (Color c : colors) {
            JButton colorBtn = new JButton();
            colorBtn.setPreferredSize(new Dimension(30, 30));
            colorBtn.setBackground(c);
            colorBtn.addActionListener(e -> avatarColor = c);
            colorPanel.add(colorBtn);
        }
        gbc.gridx = 1;
        loginPanel.add(colorPanel, gbc);
        
        JButton connectButton = new JButton("Connect");
        connectButton.setFont(new Font("Arial", Font.BOLD, 16));
        connectButton.setPreferredSize(new Dimension(200, 40));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        loginPanel.add(connectButton, gbc);
        
        connectButton.addActionListener(e -> {
            playerName = nameField.getText().trim();
            if (!playerName.isEmpty()) {
                connectToServer();
            } else {
                JOptionPane.showMessageDialog(this, "Please enter a name!");
            }
        });
        
        nameField.addActionListener(e -> connectButton.doClick());
        
        mainPanel.add(loginPanel, "LOGIN");
        cardLayout.show(mainPanel, "LOGIN");
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5555);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            out.println("SET_NAME|" + playerName);
            out.println("SET_AVATAR|" + String.format("#%02x%02x%02x", 
                avatarColor.getRed(), avatarColor.getGreen(), avatarColor.getBlue()) + 
                "|" + avatarAccessory);
            
            new Thread(this::receiveMessages).start();
            showLobbyScreen();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Cannot connect to server!");
            e.printStackTrace();
        }
    }

    private void showLobbyScreen() {
        JPanel lobbyPanel = new JPanel(new BorderLayout(10, 10));
        lobbyPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Game Lobby - Welcome " + playerName + "!");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        lobbyPanel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        
        JButton createButton = new JButton("Create Private Room");
        createButton.setFont(new Font("Arial", Font.BOLD, 18));
        createButton.addActionListener(e -> createRoom(true));
        centerPanel.add(createButton);
        
        JButton createPublicButton = new JButton("Create Public Room");
        createPublicButton.setFont(new Font("Arial", Font.BOLD, 18));
        createPublicButton.addActionListener(e -> createRoom(false));
        centerPanel.add(createPublicButton);
        
        JButton joinButton = new JButton("Join Room by Code");
        joinButton.setFont(new Font("Arial", Font.BOLD, 18));
        joinButton.addActionListener(e -> joinRoomDialog());
        centerPanel.add(joinButton);
        
        JButton listButton = new JButton("Browse Public Rooms");
        listButton.setFont(new Font("Arial", Font.BOLD, 18));
        listButton.addActionListener(e -> out.println("LIST_ROOMS"));
        centerPanel.add(listButton);
        
        lobbyPanel.add(centerPanel, BorderLayout.CENTER);
        
        mainPanel.add(lobbyPanel, "LOBBY");
        cardLayout.show(mainPanel, "LOBBY");
    }

    private void createRoom(boolean isPrivate) {
        String config = isPrivate + ",8,3,80";
        out.println("CREATE_ROOM|" + config);
    }

    private void joinRoomDialog() {
        String code = JOptionPane.showInputDialog(this, "Enter Room Code:");
        if (code != null && !code.trim().isEmpty()) {
            out.println("JOIN_ROOM|" + code.trim());
        }
    }

    private void showGameRoom() {
        JPanel gamePanel = new JPanel(new BorderLayout(5, 5));
        gamePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Top panel with word and timer
        JPanel topPanel = new JPanel(new BorderLayout());
        wordLabel = new JLabel("Waiting for game to start...");
        wordLabel.setFont(new Font("Arial", Font.BOLD, 20));
        wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(wordLabel, BorderLayout.CENTER);
        
        timerLabel = new JLabel("Time: --");
        timerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        topPanel.add(timerLabel, BorderLayout.EAST);
        
        gamePanel.add(topPanel, BorderLayout.NORTH);
        
        // Center panel with canvas and tools
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        canvas = new DrawingCanvas();
        canvas.setPreferredSize(new Dimension(800, 600));
        canvas.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        centerPanel.add(canvas, BorderLayout.CENTER);
        
        // Drawing tools
        toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolPanel.setBackground(Color.LIGHT_GRAY);
        toolPanel.setEnabled(false);
        
        // Color palette
        Color[] colors = {Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, 
                         Color.YELLOW, Color.ORANGE, Color.PINK, Color.CYAN,
                         Color.MAGENTA, Color.GRAY, Color.WHITE};
        for (Color c : colors) {
            JButton colorBtn = new JButton();
            colorBtn.setPreferredSize(new Dimension(30, 30));
            colorBtn.setBackground(c);
            colorBtn.addActionListener(e -> {
                selectedColor = c;
                canvas.setColor(c);
            });
            toolPanel.add(colorBtn);
        }
        
        toolPanel.add(new JSeparator(SwingConstants.VERTICAL));
        
        // Brush sizes
        JLabel sizeLabel = new JLabel("Size:");
        toolPanel.add(sizeLabel);
        String[] sizes = {"Small", "Medium", "Large"};
        int[] sizeValues = {3, 8, 15};
        for (int i = 0; i < sizes.length; i++) {
            int size = sizeValues[i];
            JButton sizeBtn = new JButton(sizes[i]);
            sizeBtn.addActionListener(e -> {
                brushSize = size;
                canvas.setBrushSize(size);
            });
            toolPanel.add(sizeBtn);
        }
        
        toolPanel.add(new JSeparator(SwingConstants.VERTICAL));
        
        JButton clearBtn = new JButton("Clear Canvas");
        clearBtn.addActionListener(e -> canvas.clear());
        toolPanel.add(clearBtn);
        
        JButton fillBtn = new JButton("Fill");
        fillBtn.addActionListener(e -> canvas.setFillMode(!canvas.fillMode));
        toolPanel.add(fillBtn);
        
        centerPanel.add(toolPanel, BorderLayout.SOUTH);
        gamePanel.add(centerPanel, BorderLayout.CENTER);
        
        // Right panel with players, guess, chat, scores
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setPreferredSize(new Dimension(300, 0));
        
        // Players section
        JPanel playersSection = new JPanel(new BorderLayout());
        playersSection.setBorder(BorderFactory.createTitledBorder("Players"));
        playersSection.setMaximumSize(new Dimension(300, 150));
        playerListPanel = new JPanel();
        playerListPanel.setLayout(new BoxLayout(playerListPanel, BoxLayout.Y_AXIS));
        JScrollPane playerScroll = new JScrollPane(playerListPanel);
        playersSection.add(playerScroll, BorderLayout.CENTER);
        rightPanel.add(playersSection);
        
        // Guess section
        JPanel guessSection = new JPanel(new BorderLayout());
        guessSection.setBorder(BorderFactory.createTitledBorder("Guess the Word"));
        guessSection.setMaximumSize(new Dimension(300, 60));
        guessInput = new JTextField();
        guessInput.setEnabled(false);
        guessInput.addActionListener(e -> {
            String guess = guessInput.getText().trim();
            if (!guess.isEmpty() && !isDrawing) {
                out.println("GUESS|" + guess);
                guessInput.setText("");
            }
        });
        guessSection.add(guessInput, BorderLayout.CENTER);
        rightPanel.add(guessSection);
        
        // Chat section
        JPanel chatSection = new JPanel(new BorderLayout());
        chatSection.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatSection.setMaximumSize(new Dimension(300, 200));
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatSection.add(chatScroll, BorderLayout.CENTER);
        
        chatInput = new JTextField();
        chatInput.addActionListener(e -> {
            String message = chatInput.getText().trim();
            if (!message.isEmpty()) {
                out.println("CHAT|" + message);
                chatInput.setText("");
            }
        });
        chatSection.add(chatInput, BorderLayout.SOUTH);
        rightPanel.add(chatSection);
        
        // Score panel
        scorePanel = new JPanel();
        scorePanel.setLayout(new BoxLayout(scorePanel, BoxLayout.Y_AXIS));
        scorePanel.setBorder(BorderFactory.createTitledBorder("Scores"));
        scorePanel.setMaximumSize(new Dimension(300, 150));
        JScrollPane scoreScroll = new JScrollPane(scorePanel);
        rightPanel.add(scoreScroll);
        
        gamePanel.add(rightPanel, BorderLayout.EAST);
        
        // Bottom panel with game controls
        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton startBtn = new JButton("Start Game");
        startBtn.addActionListener(e -> out.println("START_GAME"));
        bottomPanel.add(startBtn);
        
        JButton configBtn = new JButton("Configure");
        configBtn.addActionListener(e -> showConfigDialog());
        bottomPanel.add(configBtn);
        
        JButton leaveBtn = new JButton("Leave Room");
        leaveBtn.addActionListener(e -> {
            showLobbyScreen();
            cardLayout.show(mainPanel, "LOBBY");
        });
        bottomPanel.add(leaveBtn);
        
        gamePanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // Word choice panel (hidden initially)
        wordChoicePanel = new JPanel(new GridLayout(1, 3, 10, 10));
        wordChoicePanel.setBorder(new EmptyBorder(20, 50, 20, 50));
        wordChoicePanel.setVisible(false);
        wordButtons = new JButton[3];
        for (int i = 0; i < 3; i++) {
            final int index = i;
            wordButtons[i] = new JButton();
            wordButtons[i].setFont(new Font("Arial", Font.BOLD, 18));
            wordButtons[i].addActionListener(e -> {
                out.println("SELECT_WORD|" + index);
                wordChoicePanel.setVisible(false);
            });
            wordChoicePanel.add(wordButtons[i]);
        }
        
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(new OverlayLayout(layeredPane));
        layeredPane.add(gamePanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(wordChoicePanel, JLayeredPane.PALETTE_LAYER);
        
        mainPanel.add(layeredPane, "GAME");
    }

    private void showConfigDialog() {
        JPanel configPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        configPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        configPanel.add(new JLabel("Number of Rounds:"));
        JSpinner roundsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        configPanel.add(roundsSpinner);
        
        configPanel.add(new JLabel("Draw Time (seconds):"));
        JSpinner timeSpinner = new JSpinner(new SpinnerNumberModel(80, 30, 180, 10));
        configPanel.add(timeSpinner);
        
        configPanel.add(new JLabel("Max Players:"));
        JSpinner playersSpinner = new JSpinner(new SpinnerNumberModel(8, 2, 12, 1));
        configPanel.add(playersSpinner);
        
        configPanel.add(new JLabel("Custom Words (comma-separated):"));
        JTextField customWordsField = new JTextField();
        configPanel.add(customWordsField);
        
        int result = JOptionPane.showConfirmDialog(this, configPanel, 
            "Room Configuration", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String customWords = customWordsField.getText().trim().replace(",", ";");
            String config = roundsSpinner.getValue() + "," + 
                          timeSpinner.getValue() + "," + 
                          playersSpinner.getValue() + "," + 
                          customWords;
            out.println("CONFIGURE|" + config);
        }
    }

    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                processServerMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server");
        }
    }

    private void processServerMessage(String message) {
        String[] parts = message.split("\\|");
        String command = parts[0];
        
        SwingUtilities.invokeLater(() -> {
            switch (command) {
                case "NAME_SET":
                    playerId = parts[1];
                    break;
                case "ROOM_CREATED":
                case "ROOM_JOINED":
                    currentRoom = parts[1];
                    showGameRoom();
                    cardLayout.show(mainPanel, "GAME");
                    chatArea.append("Joined room: " + currentRoom + "\n");
                    break;
                case "ROOM_LIST":
                    showRoomList(parts);
                    break;
                case "PLAYER_JOINED":
                    chatArea.append(parts[1] + " joined the game!\n");
                    break;
                case "PLAYER_LEFT":
                    chatArea.append("A player left the game.\n");
                    break;
                case "PLAYER_LIST":
                    idToName.clear();
                    for (int i = 1; i < parts.length; i++) {
                        String[] playerInfo = parts[i].split(",");
                        if (playerInfo.length > 1) {
                            idToName.put(playerInfo[0], playerInfo[1]);
                        }
                    }
                    updatePlayerList(parts);
                    break;
                case "GAME_START":
                    chatArea.append("=== GAME STARTING ===\n");
                    chatArea.append("Playing " + parts[1] + " rounds\n");
                    canvas.clear();
                    break;
                case "ROUND_START":
                    chatArea.append("\n--- Round " + parts[1] + " of " + parts[2] + " ---\n");
                    canvas.clear();
                    isDrawing = false;
                    toolPanel.setEnabled(false);
                    guessInput.setEnabled(false);
                    break;
                case "DRAWER":
                    String drawerId = parts[1];
                    String drawerName = parts[2];
                    if (drawerId.equals(playerId)) {
                        chatArea.append("YOU are drawing!\n");
                        isDrawing = true;
                        canvas.setEnabled(true);
                        toolPanel.setEnabled(true);
                        guessInput.setEnabled(false);
                    } else {
                        chatArea.append(drawerName + " is drawing\n");
                        isDrawing = false;
                        canvas.setEnabled(false);
                        toolPanel.setEnabled(false);
                        guessInput.setEnabled(true);
                    }
                    break;
                case "CHOOSE_WORD":
                    showWordChoices(parts);
                    break;
                case "WORD_SELECTED":
                    wordLabel.setText("Word: " + parts[1] + " (" + parts[2] + " letters)");
                    break;
                case "TIMER":
                    timerLabel.setText("Time: " + parts[1] + "s");
                    break;
                case "HINT":
                    wordLabel.setText("Word: " + parts[1]);
                    break;
                case "DRAW":
                    canvas.drawFromNetwork(parts[1]);
                    break;
                case "CHAT":
                    chatArea.append(parts[1] + ": " + parts[2] + "\n");
                    break;
                case "CORRECT_GUESS":
                    chatArea.append("✓ " + parts[2] + " guessed correctly! (+" + parts[3] + " points)\n");
                    break;
                case "ROUND_END":
                    chatArea.append("Round ended! The word was: " + parts[1] + "\n");
                    wordLabel.setText("The word was: " + parts[1]);
                    canvas.setEnabled(false);
                    toolPanel.setEnabled(false);
                    guessInput.setEnabled(false);
                    break;
                case "SCORES":
                    updateScores(parts);
                    break;
                case "GAME_END":
                    chatArea.append("\n=== GAME OVER ===\n");
                    chatArea.append("Winner: " + parts[1] + " with " + parts[2] + " points!\n");
                    wordLabel.setText("Game Over! Winner: " + parts[1]);
                    JOptionPane.showMessageDialog(this, 
                        "Game Over!\nWinner: " + parts[1] + "\nScore: " + parts[2]);
                    break;
                case "ERROR":
                    JOptionPane.showMessageDialog(this, parts[1]);
                    break;
            }
        });
    }

    private void showRoomList(String[] parts) {
        if (parts.length == 1) {
            JOptionPane.showMessageDialog(this, "No public rooms available");
            return;
        }
        
        String[] rooms = new String[parts.length - 1];
        for (int i = 1; i < parts.length; i++) {
            String[] roomInfo = parts[i].split(",");
            rooms[i - 1] = "Room " + roomInfo[0] + " - Players: " + roomInfo[1];
        }
        
        String selected = (String) JOptionPane.showInputDialog(this,
            "Select a room to join:", "Public Rooms",
            JOptionPane.PLAIN_MESSAGE, null, rooms, rooms[0]);
        
        if (selected != null) {
            String roomCode = selected.split(" ")[1];
            out.println("JOIN_ROOM|" + roomCode);
        }
    }

    private void showWordChoices(String[] parts) {
        wordChoicePanel.setVisible(true);
        for (int i = 0; i < 3 && i + 1 < parts.length; i++) {
            wordButtons[i].setText(parts[i + 1]);
        }
    }

    private void updatePlayerList(String[] parts) {
        playerListPanel.removeAll();
        for (int i = 1; i < parts.length; i++) {
            String[] playerInfo = parts[i].split(",");
            JPanel playerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            JPanel avatarPanel = new JPanel();
            avatarPanel.setPreferredSize(new Dimension(20, 20));
            avatarPanel.setBackground(Color.decode(playerInfo[2]));
            avatarPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            playerPanel.add(avatarPanel);
            
            JLabel nameLabel = new JLabel(playerInfo[1]);
            playerPanel.add(nameLabel);
            
            playerListPanel.add(playerPanel);
        }
        playerListPanel.revalidate();
        playerListPanel.repaint();
    }

    private void updateScores(String[] parts) {
        scorePanel.removeAll();
        
        Map<String, Integer> scores = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String[] scoreInfo = parts[i].split(",");
            scores.put(scoreInfo[0], Integer.parseInt(scoreInfo[1]));
        }
        
        scores.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(entry -> {
                String name = idToName.getOrDefault(entry.getKey(), "Unknown");
                JLabel scoreLabel = new JLabel(name + ": " + entry.getValue() + " pts");
                scoreLabel.setFont(new Font("Arial", Font.PLAIN, 14));
                scorePanel.add(scoreLabel);
            });
        
        scorePanel.revalidate();
        scorePanel.repaint();
    }

    class DrawingCanvas extends JPanel {
        private java.util.List<DrawPoint> points = new ArrayList<>();
        private Point lastPoint = null;
        private Color currentColor = Color.BLACK;
        private int currentBrushSize = 5;
        boolean fillMode = false;

        public DrawingCanvas() {
            setBackground(Color.WHITE);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!isEnabled()) return;
                    lastPoint = e.getPoint();
                    if (fillMode) {
                        fill(e.getX(), e.getY());
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    lastPoint = null;
                }
            });
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!isEnabled() || fillMode) return;
                    Point current = e.getPoint();
                    if (lastPoint != null) {
                        DrawPoint dp = new DrawPoint(lastPoint.x, lastPoint.y, 
                            current.x, current.y, currentColor, currentBrushSize);
                        points.add(dp);
                        repaint();
                        
                        String drawData = String.format("%d,%d,%d,%d,%d,%d,%d,%d",
                            lastPoint.x, lastPoint.y, current.x, current.y,
                            currentColor.getRed(), currentColor.getGreen(), 
                            currentColor.getBlue(), currentBrushSize);
                        out.println("DRAW|" + drawData);
                    }
                    lastPoint = current;
                }
            });
        }

        public void setColor(Color c) {
            currentColor = c;
        }

        public void setBrushSize(int size) {
            currentBrushSize = size;
        }

        public void setFillMode(boolean mode) {
            fillMode = mode;
        }

        public void clear() {
            points.clear();
            repaint();
        }

        private void fill(int x, int y) {
            Graphics g = getGraphics();
            g.setColor(currentColor);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }

        public void drawFromNetwork(String data) {
            String[] parts = data.split(",");
            if (parts.length >= 8) {
                int x1 = Integer.parseInt(parts[0]);
                int y1 = Integer.parseInt(parts[1]);
                int x2 = Integer.parseInt(parts[2]);
                int y2 = Integer.parseInt(parts[3]);
                Color c = new Color(Integer.parseInt(parts[4]),
                                   Integer.parseInt(parts[5]),
                                   Integer.parseInt(parts[6]));
                int size = Integer.parseInt(parts[7]);
                
                DrawPoint dp = new DrawPoint(x1, y1, x2, y2, c, size);
                points.add(dp);
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                               RenderingHints.VALUE_ANTIALIAS_ON);
            
            for (DrawPoint dp : points) {
                g2d.setColor(dp.color);
                g2d.setStroke(new BasicStroke(dp.brushSize, 
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(dp.x1, dp.y1, dp.x2, dp.y2);
            }
        }
    }

    static class DrawPoint {
        int x1, y1, x2, y2, brushSize;
        Color color;

        DrawPoint(int x1, int y1, int x2, int y2, Color color, int brushSize) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.color = color;
            this.brushSize = brushSize;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ScribbleClient());
    }
}
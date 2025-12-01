import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ScribbleServer {
    private static final int PORT = 5555;
    private static Map<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static Set<String> profanityList = new HashSet<>(Arrays.asList(
        "badword1", "badword2", "inappropriate"
    ));

    public static void main(String[] args) {
        System.out.println("Scribble.io Server starting on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String playerName;
        private String playerId;
        private String currentRoom;
        private String avatarColor = "#FF5733";
        private String avatarAccessory = "none";

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.playerId = UUID.randomUUID().toString();
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    processMessage(message);
                }
            } catch (IOException e) {
                System.out.println("Client disconnected");
            } finally {
                cleanup();
            }
        }

        private void processMessage(String message) {
            String[] parts = message.split("\\|", 3);
            String command = parts[0];

            switch (command) {
                case "SET_NAME":
                    playerName = parts[1];
                    sendMessage("NAME_SET|" + playerId);
                    break;
                case "SET_AVATAR":
                    avatarColor = parts[1];
                    if (parts.length > 2) avatarAccessory = parts[2];
                    break;
                case "CREATE_ROOM":
                    createRoom(parts[1]);
                    break;
                case "JOIN_ROOM":
                    joinRoom(parts[1]);
                    break;
                case "LIST_ROOMS":
                    listRooms();
                    break;
                case "START_GAME":
                    startGame();
                    break;
                case "DRAW":
                    broadcastToRoom("DRAW|" + parts[1]);
                    break;
                case "GUESS":
                    handleGuess(parts[1]);
                    break;
                case "CHAT":
                    handleChat(parts[1]);
                    break;
                case "SELECT_WORD":
                    selectWord(Integer.parseInt(parts[1]));
                    break;
                case "CONFIGURE":
                    configureRoom(parts[1]);
                    break;
            }
        }

        private void createRoom(String config) {
            String[] settings = config.split(",");
            String roomCode = generateRoomCode();
            GameRoom room = new GameRoom(roomCode, this, settings);
            rooms.put(roomCode, room);
            currentRoom = roomCode;
            sendMessage("ROOM_CREATED|" + roomCode);
            room.addPlayer(this);
        }

        private void joinRoom(String roomCode) {
            GameRoom room = rooms.get(roomCode);
            if (room != null && room.players.size() < room.maxPlayers) {
                currentRoom = roomCode;
                room.addPlayer(this);
                sendMessage("ROOM_JOINED|" + roomCode);
            } else {
                sendMessage("ERROR|Room not found or full");
            }
        }

        private void listRooms() {
            StringBuilder roomList = new StringBuilder("ROOM_LIST");
            for (GameRoom room : rooms.values()) {
                if (!room.isPrivate) {
                    roomList.append("|").append(room.roomCode).append(",")
                           .append(room.players.size()).append("/").append(room.maxPlayers);
                }
            }
            sendMessage(roomList.toString());
        }

        private void startGame() {
            GameRoom room = rooms.get(currentRoom);
            if (room != null && room.host == this) {
                room.startGame();
            }
        }

        private void handleGuess(String guess) {
            GameRoom room = rooms.get(currentRoom);
            if (room != null && room.gameActive) {
                room.processGuess(this, guess);
            }
        }

        private void handleChat(String msg) {
            if (containsProfanity(msg)) {
                sendMessage("ERROR|Message blocked");
                return;
            }
            broadcastToRoom("CHAT|" + playerName + "|" + msg);
        }

        private void selectWord(int index) {
            GameRoom room = rooms.get(currentRoom);
            if (room != null) {
                room.selectWord(this, index);
            }
        }

        private void configureRoom(String config) {
            GameRoom room = rooms.get(currentRoom);
            if (room != null && room.host == this) {
                room.configure(config);
            }
        }

        private void broadcastToRoom(String message) {
            GameRoom room = rooms.get(currentRoom);
            if (room != null) {
                room.broadcast(message);
            }
        }

        void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        private void cleanup() {
            clients.remove(this);
            if (currentRoom != null) {
                GameRoom room = rooms.get(currentRoom);
                if (room != null) {
                    room.removePlayer(this);
                }
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String generateRoomCode() {
            return String.format("%04d", new Random().nextInt(10000));
        }

        private boolean containsProfanity(String text) {
            String lower = text.toLowerCase();
            for (String word : profanityList) {
                if (lower.contains(word)) return true;
            }
            return false;
        }
    }

    static class GameRoom {
        String roomCode;
        ClientHandler host;
        List<ClientHandler> players = new CopyOnWriteArrayList<>();
        boolean gameActive = false;
        boolean isPrivate;
        int maxPlayers = 8;
        int rounds = 3;
        int drawTime = 80;
        int currentRound = 0;
        int currentPlayerIndex = 0;
        String currentWord = "";
        String[] wordChoices;
        Map<String, Integer> scores = new ConcurrentHashMap<>();
        Map<String, Boolean> hasGuessed = new ConcurrentHashMap<>();
        Timer roundTimer;
        List<String> wordPool;
        Set<String> customWords = new HashSet<>();
        String language = "EN";

        public GameRoom(String code, ClientHandler host, String[] settings) {
            this.roomCode = code;
            this.host = host;
            this.isPrivate = Boolean.parseBoolean(settings[0]);
            if (settings.length > 1) this.maxPlayers = Integer.parseInt(settings[1]);
            if (settings.length > 2) this.rounds = Integer.parseInt(settings[2]);
            if (settings.length > 3) this.drawTime = Integer.parseInt(settings[3]);
            initializeWordPool();
        }

        private void initializeWordPool() {
            wordPool = new ArrayList<>(Arrays.asList(
                "cat", "dog", "house", "tree", "car", "sun", "moon", "star",
                "computer", "phone", "book", "chair", "table", "window", "door",
                "apple", "banana", "pizza", "coffee", "airplane", "bicycle",
                "guitar", "piano", "flower", "mountain", "ocean", "river",
                "elephant", "giraffe", "penguin", "butterfly", "robot", "castle",
                "rainbow", "umbrella", "glasses", "camera", "rocket", "astronaut"
            ));
        }

        void addPlayer(ClientHandler player) {
	    players.add(player);
       	    scores.put(player.playerId, 0);
    
            // Broadcast to others that a new player joined (exclude new player)
            for (ClientHandler p : players) {
                if (p != player) {
                     p.sendMessage("PLAYER_JOINED|" + player.playerName + "|" + player.playerId + "|" + player.avatarColor + "|" + player.avatarAccessory);
                }
            }
    
            // Send full player list and scores only to the new player
            player.sendMessage(buildPlayerList());
            player.sendMessage(buildScores());
        }


        void removePlayer(ClientHandler player) {
            players.remove(player);
            scores.remove(player.playerId);
            broadcast("PLAYER_LEFT|" + player.playerId);
            if (players.isEmpty()) {
                rooms.remove(roomCode);
                if (roundTimer != null) roundTimer.cancel();
            } else if (player == host && !players.isEmpty()) {
                host = players.get(0);
                broadcast("NEW_HOST|" + host.playerId);
            }
            sendPlayerList();
        }

        void startGame() {
            if (players.size() < 2) {
                host.sendMessage("ERROR|Need at least 2 players");
                return;
            }
            gameActive = true;
            currentRound = 0;
            currentPlayerIndex = 0;
            for (String id : scores.keySet()) {
                scores.put(id, 0);
            }
            broadcast("GAME_START|" + rounds);
            nextRound();
        }

        void nextRound() {
            if (currentRound >= rounds) {
                endGame();
                return;
            }
            currentRound++;
            hasGuessed.clear();
            broadcast("ROUND_START|" + currentRound + "|" + rounds);
            
            ClientHandler drawer = players.get(currentPlayerIndex);
            broadcast("DRAWER|" + drawer.playerId + "|" + drawer.playerName);
            
            wordChoices = selectRandomWords();
            drawer.sendMessage("CHOOSE_WORD|" + String.join("|", wordChoices));
            
            startRoundTimer();
        }

        private String[] selectRandomWords() {
            List<String> pool = new ArrayList<>(wordPool);
            pool.addAll(customWords);
            Collections.shuffle(pool);
            return new String[]{pool.get(0), pool.get(1), pool.get(2)};
        }

        void selectWord(ClientHandler player, int index) {
            if (players.get(currentPlayerIndex) != player) return;
            
            currentWord = wordChoices[index];
            String masked = getMaskedWord();
            broadcast("WORD_SELECTED|" + masked + "|" + currentWord.length());
            
            if (roundTimer != null) roundTimer.cancel();
            startDrawingTimer();
        }

        private void startRoundTimer() {
            if (roundTimer != null) roundTimer.cancel();
            roundTimer = new Timer();
            roundTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    autoSelectWord();
                }
            }, 15000);
        }

        private void startDrawingTimer() {
            if (roundTimer != null) roundTimer.cancel();
            roundTimer = new Timer();
            
            long startTime = System.currentTimeMillis();
            roundTimer.scheduleAtFixedRate(new TimerTask() {
                int elapsed = 0;
                @Override
                public void run() {
                    elapsed++;
                    broadcast("TIMER|" + (drawTime - elapsed));
                    
                    if (elapsed % 20 == 0 && hasGuessed.size() < players.size() - 1) {
                        revealHint();
                    }
                    
                    if (elapsed >= drawTime || hasGuessed.size() >= players.size() - 1) {
                        endRound();
                        cancel();
                    }
                }
            }, 1000, 1000);
        }

        private void autoSelectWord() {
            currentWord = wordChoices[0];
            String masked = getMaskedWord();
            broadcast("WORD_SELECTED|" + masked + "|" + currentWord.length());
            startDrawingTimer();
        }

        void processGuess(ClientHandler player, String guess) {
            if (hasGuessed.containsKey(player.playerId)) return;
            if (players.get(currentPlayerIndex) == player) return;
            
            String cleanGuess = guess.trim().toLowerCase();
            String cleanWord = currentWord.toLowerCase();
            
            if (cleanGuess.equals(cleanWord)) {
                hasGuessed.put(player.playerId, true);
                int points = calculatePoints();
                scores.put(player.playerId, scores.get(player.playerId) + points);
                
                broadcast("CORRECT_GUESS|" + player.playerId + "|" + player.playerName + "|" + points);
                sendScores();
                
                if (hasGuessed.size() >= players.size() - 1) {
                    endRound();
                }
            } else {
                broadcast("CHAT|" + player.playerName + "|" + guess);
            }
        }

        private int calculatePoints() {
            int basePoints = 100;
            int guessCount = hasGuessed.size();
            return Math.max(50, basePoints - (guessCount * 10));
        }

        private void revealHint() {
            String masked = getMaskedWord();
            broadcast("HINT|" + masked);
        }

        private String getMaskedWord() {
            int revealed = Math.min(currentWord.length() / 3, hasGuessed.size() / 2);
            char[] masked = new char[currentWord.length()];
            Arrays.fill(masked, '_');
            
            Random rand = new Random();
            Set<Integer> positions = new HashSet<>();
            while (positions.size() < revealed && positions.size() < currentWord.length()) {
                positions.add(rand.nextInt(currentWord.length()));
            }
            
            for (int pos : positions) {
                masked[pos] = currentWord.charAt(pos);
            }
            
            return new String(masked);
        }

        private void endRound() {
            if (roundTimer != null) roundTimer.cancel();
            broadcast("ROUND_END|" + currentWord);
            
            ClientHandler drawer = players.get(currentPlayerIndex);
            int drawerPoints = hasGuessed.size() * 20;
            scores.put(drawer.playerId, scores.get(drawer.playerId) + drawerPoints);
            
            sendScores();
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            
            Timer nextRoundTimer = new Timer();
            nextRoundTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    nextRound();
                }
            }, 5000);
        }

        private void endGame() {
            gameActive = false;
            String winner = "";
            int maxScore = 0;
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                if (entry.getValue() > maxScore) {
                    maxScore = entry.getValue();
                    winner = getPlayerName(entry.getKey());
                }
            }
            broadcast("GAME_END|" + winner + "|" + maxScore);
            sendScores();
        }

        private String getPlayerName(String playerId) {
            for (ClientHandler p : players) {
                if (p.playerId.equals(playerId)) return p.playerName;
            }
            return "Unknown";
        }

        void configure(String config) {
            String[] parts = config.split(",");
            if (parts.length > 0) rounds = Integer.parseInt(parts[0]);
            if (parts.length > 1) drawTime = Integer.parseInt(parts[1]);
            if (parts.length > 2) maxPlayers = Integer.parseInt(parts[2]);
            if (parts.length > 3) {
                String[] words = parts[3].split(";");
                customWords.addAll(Arrays.asList(words));
            }
            broadcast("CONFIG_UPDATED|" + config);
        }

        void sendPlayerList() {
            StringBuilder list = new StringBuilder("PLAYER_LIST");
            for (ClientHandler p : players) {
                list.append("|").append(p.playerId).append(",")
                    .append(p.playerName).append(",")
                    .append(p.avatarColor).append(",")
                    .append(p.avatarAccessory);
            }
            broadcast(list.toString());
        }

        void sendScores() {
            StringBuilder scoreList = new StringBuilder("SCORES");
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                scoreList.append("|").append(entry.getKey()).append(",").append(entry.getValue());
            }
            broadcast(scoreList.toString());
        }

        void broadcast(String message) {
            for (ClientHandler player : players) {
                player.sendMessage(message);
            }
        }
    }
}
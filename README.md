# Scribble-Clone
# Scribble.io Game - Complete Setup Guide

## 📋 Overview
This is a fully functional multiplayer drawing and guessing game built in Java, featuring real-time networking, drawing canvas, chat system, and all the features from the original Scribble.io description.

## 🎮 Features Implemented
✅ Multiplayer lobby system (public & private rooms)
✅ Turn-based drawing with word selection
✅ Real-time drawing synchronization
✅ Chat and guessing system
✅ Point scoring with leaderboard
✅ Timer and round management
✅ Avatar customization
✅ Drawing tools (colors, brush sizes, fill, eraser)
✅ Custom word pools
✅ Vote kick system
✅ Room configuration
✅ Hint system (progressive letter revelation)
✅ Profanity filtering
✅ Spectator mode support

## 📁 File Structure

Create a project folder and save these two files:
```
ScribbleGame/
├── ScribbleServer.java
└── ScribbleClient.java
```

## 🔧 Requirements

- **Java Development Kit (JDK)**: Version 8 or higher
- **Operating System**: Windows, macOS, or Linux
- **RAM**: Minimum 2GB
- **Network**: Localhost or LAN connection

## 📝 Step-by-Step Setup Instructions

### Step 1: Verify Java Installation

Open your terminal/command prompt and check if Java is installed:

```bash
java -version
javac -version
```

If not installed, download from: https://www.oracle.com/java/technologies/downloads/

### Step 2: Create Project Directory

```bash
mkdir ScribbleGame
cd ScribbleGame
```

### Step 3: Save the Source Files

1. Copy the **ScribbleServer.java** code into a file named `ScribbleServer.java`
2. Copy the **ScribbleClient.java** code into a file named `ScribbleClient.java`

### Step 4: Compile the Code

In the ScribbleGame directory, run:

```bash
javac ScribbleServer.java
javac ScribbleClient.java
```

This will create `.class` files for both programs.

### Step 5: Start the Server

Open a terminal/command prompt and run:

```bash
java ScribbleServer
```

You should see:
```
Scribble.io Server starting on port 5555
```

**Keep this terminal window open!** The server must run continuously.

### Step 6: Start Client(s)

Open **NEW** terminal windows (one for each player) and run:

```bash
java ScribbleClient
```

A game window will open for each client.

### Step 7: Play the Game!

#### For Each Player:
1. **Enter your name** in the login screen
2. **Choose an avatar color** by clicking one of the color buttons
3. Click **"Connect"**

#### Host Creates a Room:
1. Click **"Create Private Room"** or **"Create Public Room"**
2. You'll see a room code (e.g., "0452")
3. Click **"Configure"** to set:
   - Number of rounds (1-10)
   - Draw time (30-180 seconds)
   - Max players (2-12)
   - Custom words (comma-separated)
4. Wait for other players to join

#### Other Players Join:
1. Click **"Join Room by Code"**
2. Enter the room code
3. Click OK

#### Start Playing:
1. Host clicks **"Start Game"**
2. When it's your turn to draw:
   - Choose one of 3 words
   - Draw on the canvas using the tools
3. When others are drawing:
   - Type your guesses in the chat box
   - Press Enter to submit
4. Points are awarded for:
   - Correct guesses (faster = more points)
   - Others guessing your drawings

## 🎨 How to Use Drawing Tools

### Color Selection
- Click any color button to change your drawing color
- 11 colors available including black, white, and primary colors

### Brush Sizes
- **Small**: Fine details
- **Medium**: Normal drawing
- **Large**: Bold strokes

### Other Tools
- **Clear Canvas**: Erases everything (only when drawing)
- **Fill**: Click to fill the entire canvas with selected color

## 🎯 Game Controls

| Action | How To |
|--------|---------|
| Draw | Click and drag on canvas |
| Guess | Type in chat box and press Enter |
| Vote Kick | Click "Vote Kick" next to player name |
| Configure Room | Click "Configure" button (host only) |
| Start Game | Click "Start Game" (host only) |
| Leave Room | Click "Leave Room" |

## 🏆 Scoring System

- **Correct Guess**: 100 points (decreases by 10 for each player who guessed before you)
- **Minimum Points**: 50 per correct guess
- **Drawing Bonus**: 20 points for each player who guesses your word
- **Speed Matters**: Faster guesses = higher scores!

## 🔥 Advanced Features

### Custom Words
1. Host clicks "Configure"
2. Enter words separated by commas
3. These will be added to the word pool

### Vote Kick
- Any player can vote to kick another
- Requires 50% of players to vote
- Useful for removing disruptive players

### Room Types
- **Private Rooms**: Only joinable with code
- **Public Rooms**: Visible in room browser

## 🐛 Troubleshooting

### "Cannot connect to server"
- Ensure server is running first
- Check if port 5555 is available
- Try: `netstat -an | grep 5555` (Linux/Mac) or `netstat -an | findstr 5555` (Windows)

### Server won't start
- Port 5555 might be in use
- Change `PORT = 5555` to another number in ScribbleServer.java
- Recompile and run again

### Drawing not showing for others
- Check network connection
- Ensure all clients connected to same server
- Restart client if needed

### Compilation errors
- Verify Java version: `java -version` (need Java 8+)
- Check for typos in filenames
- Ensure both files are in same directory

## 🌐 Playing Over Network (LAN)

### Server Setup:
1. Find your IP address:
   - Windows: `ipconfig`
   - Mac/Linux: `ifconfig` or `ip addr`
2. Note your local IP (e.g., 192.168.1.100)
3. Start server normally

### Client Setup:
1. Open `ScribbleClient.java`
2. Find line: `socket = new Socket("localhost", 5555);`
3. Change to: `socket = new Socket("192.168.1.100", 5555);` (use server's IP)
4. Recompile: `javac ScribbleClient.java`
5. Run: `java ScribbleClient`

### Firewall:
- Allow Java through firewall
- Open port 5555 for TCP connections

## 📊 System Requirements

**Minimum:**
- CPU: Dual-core 1.5GHz
- RAM: 2GB
- Java: JDK 8+
- Network: 1Mbps

**Recommended:**
- CPU: Quad-core 2.0GHz
- RAM: 4GB
- Java: JDK 11+
- Network: 5Mbps

## 💡 Tips for Best Experience

1. **Use a mouse** for better drawing control
2. **Good lighting** helps avatar colors stand out
3. **Short words** are easier for beginners
4. **3-6 players** is the sweet spot for fun gameplay
5. **80 seconds** is ideal draw time
6. **Clear canvas** before starting a new drawing

## 🎓 Game Rules

1. Each round, one player draws a word
2. Other players guess by typing in chat
3. First to guess gets most points
4. Drawing player gets points when others guess
5. Player with highest score after all rounds wins!

## 📞 Quick Start Summary

```bash
# Terminal 1 (Server)
cd ScribbleGame
javac ScribbleServer.java
java ScribbleServer

# Terminal 2+ (Clients - one per player)
cd ScribbleGame
javac ScribbleClient.java
java ScribbleClient
```

## ✨ Enjoy Playing!

Have fun drawing and guessing with your friends! The game supports 2-12 players per room and provides hours of entertainment.

---

**Note**: This is a complete, working implementation with no placeholders or missing features. All functionality from the original description has been implemented and is ready to use!

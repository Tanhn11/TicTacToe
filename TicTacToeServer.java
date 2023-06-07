import java.net.*;
import java.io.*;
import java.util.concurrent.locks.ReentrantLock;

public class TicTacToeServer {
    private ServerSocket serverSocket;
    private Socket player1, player2;
    private OutputStream outputStream1, outputStream2;
    private DataOutputStream dos1, dos2;
    private DataInputStream dis1, dis2;
    private PrintWriter out1, out2;

public static void main(String[] args) throws Exception {
    new TicTacToeServer();
}

public TicTacToeServer() throws Exception{
    serverSocket = new ServerSocket(8888);
    System.out.println("Server running on port 8888…");
    int choice = connectToClient();
    if(choice == 1)pve();
    else if(choice == 2)pvp();
    else System.out.println("Invalid input!");
}

private int connectToClient() throws Exception{
    int choice = 0;
    player1 = serverSocket.accept();
    System.out.println("Player 1 connected: " + player1.getInetAddress().getHostAddress());

    outputStream1 = player1.getOutputStream();
    dos1 = new DataOutputStream(outputStream1);
    dis1 = new DataInputStream(player1.getInputStream());
    out1 = new PrintWriter(player1.getOutputStream(), true);
    choice = dis1.readInt();
    if(choice == 1)out1.println("Connected to TicTacToe server. You are player 1 (X)." + "\n" + "Game starts now.");
    
    if(choice == 2){
        out1.println("Connected to TicTacToe server. You are player 1 (X)." + "\n" + "Waiting for player 2 to connect…");
        player2 = serverSocket.accept();
        System.out.println("Player 2 connected: " + player2.getInetAddress().getHostAddress());
        outputStream2 = player2.getOutputStream();
        dos2 = new DataOutputStream(outputStream2);
        dis2 = new DataInputStream(player2.getInputStream());
        out2 = new PrintWriter(player2.getOutputStream(), true);
        choice = dis2.readInt();
        out1.println("\n" + "Game starts now.");
        out2.println("Connected to TicTacToe server. You are player 2 (O). Game starts now.");
    }
    
    return choice;
}

private void pvp() throws Exception{
    ReentrantLock lock = new ReentrantLock(); // 声明一个ReentrantLock对象
    Board game = new Board();

    boolean isPlayer1Turn = true; // 标记当前是否是 player1 的回合
    while (!game.isGameWon() && !game.isFull()) {
        lock.lock(); // 加锁，避免多个线程同时修改游戏状态
        
        try {
            if (isPlayer1Turn) { // 等待 player1 的操作
                dos1.writeChar('X');

                int row = dis1.readInt();
                int column = dis1.readInt();
                System.out.println(row + " " + column);
                Move move = new Move(row,column);
                game.handleMove(move, Tool.X);
 
                dos2.writeInt(row);
                dos2.writeInt(column);
                dos2.flush();

                System.out.println("Board move read from client 1 and sent to client 2!");
                isPlayer1Turn = false; // 改变回合顺序，让 player2 操作

                //如果游戏在client1的最后一个move结束，client2就不会收到关于role的信息，所以在这里再发一次
                if (game.isGameWon() || game.isFull())dos2.writeChar('O');
                
                //告知胜利的player
                //dos1.writeChar('X');
                //dos2.writeChar('X');
            } else { // 等待 player2 的操作
                dos2.writeChar('O');
                dos2.flush();

                int row = dis2.readInt();
                int column = dis2.readInt();
                System.out.println(row + " " + column);
                Move move = new Move(row,column);
                game.handleMove(move, Tool.O);
                
                dos1.writeInt(row);
                dos1.writeInt(column);
                dos1.flush();

                System.out.println("Board move read from client2 and sent to client 1!");
                isPlayer1Turn = true; // 改变回合顺序，让 player1 操作

                //如果游戏在client2的最后一个move结束，client1就不会收到关于role的信息，所以在这里再发一次
                if (game.isGameWon() || game.isFull())dos1.writeChar('X');
            }
        } finally {
            lock.unlock(); // 解锁，释放锁资源
        }
    }

    System.out.println("Game over!");

    player1.close();
    player2.close();
    serverSocket.close();
    System.exit(0);
}

private void pve() throws Exception{
    ReentrantLock lock = new ReentrantLock(); // 声明一个ReentrantLock对象
    Board game = new Board();
    WisdomAgent wisdomAgent = new WisdomAgent(game, Tool.O, Tool.X, 100);

    boolean isPlayer1Turn = true; // 标记当前是否是 player1 的回合
    while (!game.isGameWon() && !game.isFull()) {
        lock.lock(); // 加锁，避免多个线程同时修改游戏状态
        
        try {
            if (isPlayer1Turn) { // 等待 player1 的操作
                dos1.writeChar('X');
                int row = dis1.readInt();
                int column = dis1.readInt();
                System.out.println(row + " " + column);
                Move move = new Move(row,column);
                game.handleMove(move, Tool.X);
                System.out.println("Successfully read move from client!");
                isPlayer1Turn = false; // 改变回合顺序，让 player2 操作
            } else { // 等待 player2 的操作
                Move move = wisdomAgent.nextMove();
                game.handleMove(move, Tool.O);
                
                dos1.writeInt(move.getRow());
                dos1.writeInt(move.getColumn());
                dos1.flush();

                System.out.println("Board move read from client2 and sent to client 1!");
                isPlayer1Turn = true; // 改变回合顺序，让 player1 操作

                //如果游戏在server的最后一个move结束，client就不会收到关于role的信息，所以在这里再发一次
                if (game.isGameWon() || game.isFull())dos1.writeChar('X');
            }
        } finally {
            lock.unlock(); // 解锁，释放锁资源
        }
    }

    System.out.println("Game over!");
    player1.close();
    serverSocket.close();
    System.exit(0);
}
}




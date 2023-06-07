import java.net.*;
import java.io.*;
import java.util.Scanner;

public class TicTacToeClient {
    private DataOutputStream dos;
    private DataInputStream dis;
    private BufferedReader in;
    private Socket socket;
    private int port = 8888;
    private String serverIP;
    private boolean first = false;
    private Tool player, opponent;
    private char playerC;

    public TicTacToeClient(){
        System.out.print("Enter IP address of server: ");
        Scanner scanner = new Scanner(System.in);
        serverIP = scanner.nextLine();
        
        connectToServer();

        try {
            System.out.println("Choice: 1. Person vs Computer  2. Person vs Person ");
            int choice = scanner.nextInt();
            if(choice == 1 || choice == 2){
                dos.writeInt(choice);
                showGreetings();
                startGame();
            }else{
                System.out.println("Invalid input!");
            }
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket(serverIP, port);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Connected to the server: " + socket.getInetAddress());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // 异常发生时退出程序
        }
    }

    private void showGreetings() throws IOException{
        char[] buffer = new char[1024];
        int bytesRead;
        String greetings = "";
        if ((bytesRead = in.read(buffer)) != -1) {
            greetings = new String(buffer, 0, bytesRead);
            if(greetings.contains("player 1")){
                first = true;
            }
            while(!greetings.contains("start")){
                System.out.println(greetings);
                if ((bytesRead = in.read(buffer)) != -1) {
                    greetings = new String(buffer, 0, bytesRead);
                }
                if(greetings.contains("player 1")){
                    first = true;
                }
            }
        }
        System.out.println(greetings);
    }

    private void startGame() throws Exception{
        Scanner scanner = new Scanner(System.in);
        Board game = new Board();

        while (!game.isGameWon() && !game.isFull()) { 
            if(first){
                getRole();  //读取player角色
                game.show();   //显示当前游戏状态        
                
                //等待玩家输入  
                System.out.println("Enter the row (1-3) for player:");
                int row = scanner.nextInt();
                System.out.println("Enter the column (1-3) for player:");
                int column = scanner.nextInt();
                Move move = new Move(row, column);
                sendMoveToServer(row, column); //将输入的Move发送给服务器
                game.setMove(move, player); //处理player的move
                game.show();
                if(game.isGameWon() || game.isFull())break;

                System.out.println("Waiting for the other player to move");            
                row = dis.readInt();
                column = dis.readInt();
                move = new Move(row, column); // 接收服务器的回应,处理opponent的move
                game.handleMove(move, opponent);
                game.show();
            }else{
                System.out.println("Waiting for the other player to move");               
                int row = dis.readInt();
                int column = dis.readInt();
                Move move = new Move(row, column); // 接收服务器的回应,处理opponent的move               
                getRole();    //读取player角色
                game.handleMove(move, opponent);
                game.show(); 
                if(game.isGameWon() || game.isFull())break;
                
                System.out.println("Enter the row (1-3) for player:");
                row = scanner.nextInt();
                System.out.println("Enter the column (1-3) for player:");
                column = scanner.nextInt();
                move = new Move(row, column);
                sendMoveToServer(row, column); //将输入的Move发送给服务器
                game.setMove(move, player); //处理player的move
                game.show();
            }
        }
        System.out.println("Game over!");
        scanner.close();
        socket.close();
       // System.exit(0);
    }

    private void sendMoveToServer(int row, int column) {
        try {
            dos.writeInt(row);
            dos.writeInt(column);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1); // 异常发生时退出程序
        }
    }

    private void getRole() throws IOException{
        //读取player角色
        playerC = dis.readChar();
        if(playerC == 'X'){
            player = Tool.X;
            opponent = Tool.O;
        }else if(playerC== 'O'){
            player = Tool.O;
            opponent = Tool.X;
        }else{
            player = Tool.EMPTY;
            opponent = Tool.EMPTY; 
        }
    }

    public static void main(String[] args) throws Exception{
        new TicTacToeClient();
    }
}

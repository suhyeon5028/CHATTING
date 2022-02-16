package site.metacoding.chat_v2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MyServerSocket {

    // 리스너(연결받기) - 메인스레드
    ServerSocket serverSocket;
    List<고객전담스레드> 고객리스트;

    // 서버는 메시지 받아서 보내기(클라이언트 수마다)

    public MyServerSocket() {
        try {
            serverSocket = new ServerSocket(2000);
            고객리스트 = new ArrayList<>(); // 동기화(동시접근 가능)가 처리된 ArrayList
            while (true) {
                Socket socket = serverSocket.accept(); // main 스레드
                System.out.println("클라이언트 연결됨");
                고객전담스레드 t = new 고객전담스레드(socket); // t가 가비지컬렉션 되기때문에
                고객리스트.add(t); // 고객리스트에 t를 담아놓음

                System.out.println("고객리스트 크기 : " + 고객리스트.size());
                new Thread(t).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 내부 클래스
    class 고객전담스레드 implements Runnable {

        Socket socket;
        BufferedReader reader;
        BufferedWriter writer;
        boolean isLogin = true;

        public 고객전담스레드(Socket socket) {
            this.socket = socket;

            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (isLogin) {
                try {
                    String inputData = reader.readLine();
                    System.out.println("from 클라이언트 : " + inputData);

                    // 메시지 받았으니까 List<고객전담스레드> 고객리스트 <== 여기에 담긴
                    // 모든 클라이언트에게 메시지 전송 (for문 돌려서)
                    for (고객전담스레드 t : 고객리스트) { // 왼쪽 : 컬렉션 타입, 오른쪽 : 컬렉션
                        if (t != this) { // 나 자신이 아닐때만
                            t.writer.write(inputData + "\n");
                            t.writer.flush();
                        }
                        // 고객리스트.get(i).writer.write(inputData + "\n");
                    }

                } catch (Exception e) {
                    try {
                        System.out.println("통신실패 : " + e.getMessage());
                        // insert into -> 한방에 100개씩 bulk collector
                        isLogin = false;
                        고객리스트.remove(this);
                        reader.close();
                        writer.close();
                        socket.close();
                    } catch (Exception e1) {
                        System.out.println("연결해제 프로세스 실패 : " + e1.getMessage());
                    }
                }
            }
        }

    }

    // 192.168.0.132 (쌤이랑 연결 IP)
    public static void main(String[] args) {
        new MyServerSocket();
    }
}

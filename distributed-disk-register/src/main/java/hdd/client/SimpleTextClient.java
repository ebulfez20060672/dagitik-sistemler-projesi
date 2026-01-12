package hdd.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class SimpleTextClient {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 6666; // Lider Sunucunun Portu

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println(">>> Lider Sunucuya Bağlandı (Port: " + port + ")");
            System.out.println(">>> Komut Formatları:");
            System.out.println("    1. SET <id> <mesaj>");
            System.out.println("    2. GET <id>");
            System.out.println(">>> Çıkış için 'exit' yazın.\n");

            while (true) {
                System.out.print("> Komut Giriniz: ");
                String command = scanner.nextLine();

                if ("exit".equalsIgnoreCase(command)) {
                    break;
                }

                out.println(command);
                String response = in.readLine();
                System.out.println("< Liderden Cevap: " + response);
            }

        } catch (Exception e) {
            System.err.println("Bağlantı Hatası: Lider Sunucu (6666) açık mı?");
            e.printStackTrace();
        }
    }
}
package hdd.member;

import com.hdd.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LeaderServer {

    private static final int LEADER_PORT = 6666; // İstemci bu porta bağlanacak
    private static int TOLERANCE_LEVEL = 1;
    
    // Üyelerin listesi (IP ve Port bilgileri)
    private static final List<MemberClientStub> members = new ArrayList<>();
    
    // Hangi mesajın hangi üyelerde olduğunu tutan basit bir indeks
    private static final Map<String, List<Integer>> messageIndex = new ConcurrentHashMap<>();

    // Round-Robin dağıtım için sayaç
    private static int rrCounter = 0;

    public static void main(String[] args) {
        System.out.println(">>> Lider Sunucu Başlatılıyor...");
        
        // 1. tolerance.conf Oku
        loadToleranceConfig();

        // 2. Üyeleri Tanı (Şimdilik manuel ekliyoruz)
        // DİKKAT: MemberServer'ı hangi portta başlattıysanız buraya onu yazın (Genelde 5555)
        addMember("localhost", 5555); 
        addMember("localhost", 5556);

        // 3. İstemciyi Dinlemeye Başla (Socket)
        try (ServerSocket serverSocket = new ServerSocket(LEADER_PORT)) {
            System.out.println(">>> Lider (Port: " + LEADER_PORT + ") İstemcileri Bekliyor...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[Client İstegi]: " + line);
                String[] parts = line.split(" ", 3);
                String command = parts[0];

                if ("SET".equalsIgnoreCase(command) && parts.length >= 3) {
                    String msgId = parts[1];
                    String content = parts[2];
                    boolean success = distributeMessage(msgId, content);
                    out.println(success ? "OK" : "ERROR");

                } else if ("GET".equalsIgnoreCase(command) && parts.length >= 2) {
                    String msgId = parts[1];
                    String content = fetchMessage(msgId);
                    out.println(content != null ? content : "NOT_FOUND");

                } else {
                    out.println("UNKNOWN_COMMAND");
                }
            }
        } catch (IOException e) {
            System.err.println("İstemci bağlantı hatası: " + e.getMessage());
        }
    }

    // Mesajı tolerans sayısı kadar üyeye dağıt (SET İşlemi)
    private static boolean distributeMessage(String msgId, String content) {
        if (members.isEmpty()) {
            System.err.println("HATA: Hiç aktif üye yok!");
            return false;
        }

        int successCount = 0;
        List<Integer> savedMemberIndices = new ArrayList<>();

        // Round Robin Mantığı: Kaldığımız yerden devam et
        int startNode = rrCounter % members.size();
        
        // Tolerance sayısı kadar (veya mevcut üye sayısı kadar) döngü
        int loopCount = Math.min(TOLERANCE_LEVEL, members.size());

        for (int i = 0; i < loopCount; i++) {
            int memberIndex = (startNode + i) % members.size();
            MemberClientStub member = members.get(memberIndex);

            try {
                // gRPC Çağrısı
                SaveResponse response = member.stub.saveMessage(SaveRequest.newBuilder()
                        .setMessageId(msgId)
                        .setContent(content)
                        .build());
                
                if (response.getSuccess()) {
                    successCount++;
                    savedMemberIndices.add(memberIndex);
                    System.out.println(" -> Mesaj üyeye (" + member.host + ":" + member.port + ") yazıldı.");
                }
            } catch (Exception e) {
                System.err.println(" -> Üye erişim hatası (" + member.port + "): " + e.getMessage());
            }
        }
        
        // Bir sonraki mesaj için sayacı ilerlet
        rrCounter++;

        if (successCount >= 1) { // En az 1 yere yazıldıysa başarılı say
            messageIndex.put(msgId, savedMemberIndices);
            return true;
        }
        return false;
    }

    // Mesajı üyelerden getir (GET İşlemi)
    private static String fetchMessage(String msgId) {
        List<Integer> memberIndices = messageIndex.get(msgId);
        if (memberIndices == null || memberIndices.isEmpty()) return null;

        // Mesajı tutan üyeleri sırayla dene (Hata toleransı burada devreye girer)
        for (int index : memberIndices) {
            if (index < members.size()) {
                MemberClientStub member = members.get(index);
                try {
                    GetResponse response = member.stub.getMessage(GetRequest.newBuilder()
                            .setMessageId(msgId)
                            .build());
                    
                    if (response.getFound()) {
                        System.out.println(" -> Mesaj üyeden (" + member.port + ") okundu.");
                        return response.getContent();
                    }
                } catch (Exception e) {
                    System.err.println(" -> Üye çökmüş olabilir (" + member.port + "), diğerine geçiliyor...");
                }
            }
        }
        return null;
    }

    private static void loadToleranceConfig() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("tolerance.conf"));
            if (!lines.isEmpty()) {
                TOLERANCE_LEVEL = Integer.parseInt(lines.get(0).trim());
                System.out.println("Tolerans Seviyesi: " + TOLERANCE_LEVEL);
            }
        } catch (Exception e) {
            System.out.println("tolerance.conf bulunamadı, varsayılan (1) kullanılıyor.");
        }
    }

    private static void addMember(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);
        members.add(new MemberClientStub(host, port, stub));
        System.out.println("Üye Eklendi: " + host + ":" + port);
    }

    // Yardımcı Sınıf
    static class MemberClientStub {
        String host;
        int port;
        FamilyServiceGrpc.FamilyServiceBlockingStub stub;

        public MemberClientStub(String host, int port, FamilyServiceGrpc.FamilyServiceBlockingStub stub) {
            this.host = host;
            this.port = port;
            this.stub = stub;
        }
    }
}
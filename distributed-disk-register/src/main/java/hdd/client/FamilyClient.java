package hdd.client;

import com.hdd.grpc.*; // Proto'dan üretilen yeni sınıflar
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class FamilyClient {
    public static void main(String[] args) {
        // Hedef sunucu (Lider/Üye) bilgileri
        String host = "localhost";
        int port = 5555; // MemberServer'ın çalıştığı port

        System.out.println("İstemci başlatılıyor, hedeflenen sunucu: " + host + ":" + port);

        // 1. Kanal (Bağlantı) Oluştur
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        // 2. Stub (İstemci Nesnesi) Oluştur
        FamilyServiceGrpc.FamilyServiceBlockingStub stub = FamilyServiceGrpc.newBlockingStub(channel);

        try {
            // --- TEST 1: CHAT (Şablonun özelliği) ---
            System.out.println("--- Chat Testi ---");
            stub.sendChat(ChatMessage.newBuilder()
                    .setFromHost("Client-01")
                    .setFromPort(0)
                    .setText("Merhaba Dünya! Ben bir istemciyim.")
                    .build());
            System.out.println("Chat mesajı gönderildi.");

            // --- TEST 2: ÖDEV ÖZELLİĞİ (SET - Mesaj Kaydet) ---
            System.out.println("\n--- Save (SET) Testi ---");
            SaveResponse saveResponse = stub.saveMessage(SaveRequest.newBuilder()
                    .setMessageId("msg-100")
                    .setContent("Bu dağıtık sistemde saklanacak kritik bir veri.")
                    .build());
            
            System.out.println("Kayıt Başarılı mı?: " + saveResponse.getSuccess());

            // --- TEST 3: ÖDEV ÖZELLİĞİ (GET - Mesaj Oku) ---
            System.out.println("\n--- Get (GET) Testi ---");
            GetResponse getResponse = stub.getMessage(GetRequest.newBuilder()
                    .setMessageId("msg-100")
                    .build());

            if (getResponse.getFound()) {
                System.out.println("Veri Bulundu: " + getResponse.getContent());
            } else {
                System.out.println("Veri Bulunamadı.");
            }

        } catch (Exception e) {
            System.err.println("Bir hata oluştu: " + e.getMessage());
            e.printStackTrace();
        } finally {
            channel.shutdown();
        }
    }
}
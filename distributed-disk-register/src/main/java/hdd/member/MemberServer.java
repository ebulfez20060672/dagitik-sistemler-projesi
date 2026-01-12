package hdd.member;

import com.hdd.grpc.*;
import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class MemberServer {

    public static void main(String[] args) throws IOException, InterruptedException {
        // Varsayılan port 5555, ancak parametre gelirse onu kullan (5556, 5557 vb.)
        int port = 5555;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        
        // Her sunucu için porta özel bir klasör oluştur: data_5555, data_5556...
        String dataDir = "data_" + port;
        new File(dataDir).mkdirs(); // Klasörü oluştur

        // Servisi başlatırken bu klasör yolunu içeri gönderiyoruz
        Server server = ServerBuilder.forPort(port)
                .addService(new FamilyServiceImpl(dataDir))
                .build()
                .start();

        System.out.println("MemberServer (" + port + ") başlatıldı. (Kayıt Yeri: " + dataDir + ")");
        server.awaitTermination();
    }

    static class FamilyServiceImpl extends FamilyServiceGrpc.FamilyServiceImplBase {

        private final String myDataDir;

        // Kurucu metod (Constructor): Verinin nereye yazılacağını alır
        public FamilyServiceImpl(String dataDir) {
            this.myDataDir = dataDir;
        }

        // --- DISK I/O İLE KAYDETME (Zero Copy / NIO) ---
        @Override
        public void saveMessage(SaveRequest request, StreamObserver<SaveResponse> responseObserver) {
            // Dosya yolunu dinamik olarak belirle
            String fileName = this.myDataDir + "/" + request.getMessageId() + ".txt";
            boolean success = true;
            String error = "";

            try (RandomAccessFile writer = new RandomAccessFile(fileName, "rw");
                 FileChannel channel = writer.getChannel()) {
                
                // Veriyi diske yaz
                byte[] data = request.getContent().getBytes(StandardCharsets.UTF_8);
                ByteBuffer buffer = ByteBuffer.wrap(data);
                channel.write(buffer);
                
                System.out.println("[DISK-YAZ] Dosya oluşturuldu: " + fileName);
                
            } catch (IOException e) {
                success = false;
                error = e.getMessage();
                System.err.println("Disk Hatası: " + e.getMessage());
            }

            SaveResponse response = SaveResponse.newBuilder()
                    .setSuccess(success)
                    .setErrorMessage(error)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        // --- DISK I/O İLE OKUMA ---
        @Override
        public void getMessage(GetRequest request, StreamObserver<GetResponse> responseObserver) {
            String fileName = this.myDataDir + "/" + request.getMessageId() + ".txt";
            String content = "";
            boolean found = false;

            File file = new File(fileName);
            if (file.exists()) {
                try (RandomAccessFile reader = new RandomAccessFile(fileName, "r");
                     FileChannel channel = reader.getChannel()) {
                    
                    // Dosyadan veriyi oku
                    ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
                    channel.read(buffer);
                    buffer.flip(); // Okuma moduna geç
                    
                    content = StandardCharsets.UTF_8.decode(buffer).toString();
                    found = true;
                    System.out.println("[DISK-OKU] Dosya okundu: " + fileName);

                } catch (IOException e) {
                    System.err.println("Okuma Hatası: " + e.getMessage());
                }
            }

            GetResponse response = GetResponse.newBuilder()
                    .setFound(found)
                    .setContent(content)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

        // --- ŞABLON ZORUNLULUKLARI (Hata vermemesi için boş bıraktık) ---
        @Override
        public void joinFamily(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
            responseObserver.onNext(JoinResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        }

        @Override
        public void iAmAlive(AliveRequest request, StreamObserver<AliveResponse> responseObserver) {
             responseObserver.onNext(AliveResponse.newBuilder().setAck(true).build());
             responseObserver.onCompleted();
        }

        @Override
        public void sendChat(ChatMessage request, StreamObserver<Empty> responseObserver) {
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        }
    }
}
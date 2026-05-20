package auction_system;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.ItemType;
import auction_system.common.protocol.Request;
import auction_system.server.util.GsonUtil;
import com.google.gson.JsonElement;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class TestCreateAuction {

    // SỬA LỖI 1: Thêm 'static' vào đây
    public static String readMessage(DataInputStream in) throws IOException {
        int length = in.readInt();

        byte[] data = new byte[length];
        in.readFully(data);

        return new String(data, "UTF-8");
    }

    // SỬA LỖI 1: Thêm 'static' vào đây
    public   static void writeMessage(DataOutputStream out, String message) throws IOException {
        byte[] data = message.getBytes("UTF-8");

        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    public static void main(String[] args) {

        String serverAddress = "127.0.0.1";
        int port = 3636;

        try (
                Socket socket = new Socket(serverAddress, port);

                DataOutputStream out = new DataOutputStream(
                        new BufferedOutputStream(socket.getOutputStream())
                );

                DataInputStream in = new DataInputStream(
                        new BufferedInputStream(socket.getInputStream())
                )
        ) {

            String imageBase64 = Files.readString(
                    Path.of("image_base64.txt")
            );

            AuctionDTO auctionDTO = new AuctionDTO();

            auctionDTO.setSellerId(1);
            auctionDTO.setName("pagini");
            auctionDTO.setDescription("Xe thuy dien");
            auctionDTO.setType(ItemType.VEHICLE);

            auctionDTO.setStartingPrice(1000000.0);

            auctionDTO.setStartTime(LocalDateTime.now());

            auctionDTO.setEndTime(
                    LocalDateTime.now().plusDays(3)
            );

            auctionDTO.setImagebase64(imageBase64);

            JsonElement dataElement = GsonUtil
                    .getGson()
                    .toJsonTree(auctionDTO);

            Request request = new Request(
                    Action.CREATE_AUCTION,
                    dataElement
            );

            String jsonRequest = GsonUtil.toJson(request);

            System.out.println("========== SEND ==========");
            System.out.println(jsonRequest);
            writeMessage(out, jsonRequest);

            // SỬA LỖI 2: Thay in.readUTF() bằng hàm readMessage(in)
            String jsonResponse = readMessage(in);

            System.out.println("========== RESPONSE ==========");
            System.out.println(jsonResponse);

        } catch (Exception e) {

            System.out.println("Cannot create auction");

            e.printStackTrace();
        }
    }
}
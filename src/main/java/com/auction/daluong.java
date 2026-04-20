package com.auction;

import com.auction.model.*;
import com.auction.util.IdGenerator;

public class daluong {
    public static void main(String[] args) {
        // Tạo dữ liệu giả
        Seller seller = new Seller("Chuong", "1234", "chuong@");
        Item item = new Electronics("Laptop", "Gaming Laptop", 1000, "Dell", 25);
        Auction auction = new Auction(item, seller);
        auction.startAuction(); // chuyển từ OPEN sang RUNNING

        Bidder bidder1 = new Bidder("Alice", "pass", "alice@ex.com");
        Bidder bidder2 = new Bidder("Bob", "pass", "bob@ex.com");

        // Tạo task đặt giá
        Runnable task = () -> {
            try {
                // Mỗi luồng đặt giá ngẫu nhiên từ 1000 đến 1050
                double amount = 1000 + Math.random() * 50;
                auction.placeBid(bidder1, amount); // dùng cùng bidder hoặc xen kẽ
            } catch (Exception e) {
                System.out.println(Thread.currentThread().getName() + " failed: " + e.getMessage());
            }
        };

        // Tạo 5 luồng cùng chạy
        for (int i = 0; i < 5; i++) {
            Thread t = new Thread(task);
            t.start();
        }
        auction.closeAuction();
    }
}
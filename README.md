# 🖥️ RemoteX - Remote Desktop với Giám Sát Bảo Mật AI

## 📖 TỔNG QUAN

RemoteX là phần mềm Remote Desktop đơn giản được tích hợp tính năng giám sát bảo mật sử dụng AI. Phần mềm cho phép:

- ✅ Điều khiển máy tính từ xa
- ✅ Chat realtime
- ✅ **Giám sát bảo mật bằng AI (Tùy chọn)**
- ✅ Tự động ngắt kết nối khi phát hiện nguy hiểm

## 🎯 TÍNH NĂNG CHÍNH

### 1. Remote Desktop

- Chia sẻ màn hình với FPS cao
- Điều khiển chuột và bàn phím từ xa
- Tối ưu băng thông với delta frames

### 2. Chat Realtime

- Chat window có thể ẩn/hiện
- Timestamp cho mỗi tin nhắn
- Giao diện thân thiện

### 3. **Giám Sát Bảo Mật AI**

- Phát hiện hoạt động nguy hiểm realtime
- 4 mức cảnh báo: CRITICAL, HIGH, MEDIUM, LOW
- **Tự động ngắt kết nối** khi CRITICAL/HIGH
- **Auto-close cảnh báo** sau 10s với MEDIUM/LOW

## 🚀 CÀI ĐẶT NHANH

### Bước 1: Cài đặt Services (Chỉ 1 lần)

```bash
# Mở CMD với quyền Administrator
cd remote
java -jar remote.jar --setup
```

### Bước 2: Khởi động Server Relay

```bash
cd remote.server
java -jar remote.server.jar
```

### Bước 3: Chạy Client

```bash
cd remote
java -jar remote.jar
```

## 📦 CẤU TRÚC DỰ ÁN

```
RemoteX/
├── remote/                      # Client application
│   ├── src/main/java/
│   │   ├── client/             # Viewer logic
│   │   ├── server/             # Sharer logic
│   │   ├── common/             # Chat window
│   │   ├── main/               # Main entry point
│   │   └── monitor/            # 🆕 Monitoring module
│   │       ├── ml/             # AI detection
│   │       ├── log/            # Log processing
│   │       ├── alert/          # Alert handling
│   │       ├── config/         # Configuration
│   │       └── setup/          # Installation
│   └── src/main/resources/     # AI models & configs
├── remote.server/              # Relay server
│   └── src/main/java/server/
└── HUONG_DAN_GIAM_SAT.md      # Chi tiết về monitoring

```

## 💡 SỬ DỤNG

### Bên Cho Phép Điều Khiển:

1. Nhập ID và Mật khẩu
2. ✅ **Tick "Bật giám sát bảo mật (AI)"** (nếu muốn)
3. Click "Cho phép điều khiển"

### Bên Điều Khiển:

1. Nhập ID đối tác và Mật khẩu
2. Click "Bắt đầu điều khiển"

## 🚨 MỨC ĐỘ CẢNH BÁO

| Mức độ       | Icon | Hành động           | Ví dụ                       |
| ------------ | ---- | ------------------- | --------------------------- |
| **CRITICAL** | 🔴   | **Auto disconnect** | Ransomware, Backdoor        |
| **HIGH**     | 🟠   | **Auto disconnect** | PowerShell script nguy hiểm |
| **MEDIUM**   | 🟡   | Cảnh báo 10s        | CMD commands bất thường     |
| **LOW**      | 🟢   | Cảnh báo 10s        | Process startup             |

## 🔧 YÊU CẦU HỆ THỐNG

- **OS:** Windows 10/11
- **Java:** JDK 8 trở lên
- **RAM:** Tối thiểu 4GB
- **Network:** TCP ports 5000, 6000, 7000

## 📊 KIẾN TRÚC

```
┌───────────────┐         ┌──────────────┐         ┌────────────────┐
│   Sharer      │◄───────►│ Relay Server │◄───────►│    Viewer      │
│  (Server)     │         │  (Port 5000, │         │   (Client)     │
│               │         │   6000, 7000)│         │                │
│  + Monitoring │         └──────────────┘         │  + Alert UI    │
│  + AI Model   │                                  │  + Auto Close  │
└───────────────┘                                  └────────────────┘
        ↓
    [Sysmon]
        ↓
   [Winlogbeat]
        ↓
    [AI Model]
        ↓
  [Alert Service] ──(Chat Socket)──> [Viewer]
```

## 🔐 BẢO MẬT

- ✅ AI model chạy local (không gửi data ra ngoài)
- ✅ Encrypted socket communication
- ✅ Auto-disconnect khi phát hiện nguy hiểm cao
- ✅ Logs chỉ lưu local

## 📚 TÀI LIỆU

- [Hướng dẫn giám sát chi tiết](HUONG_DAN_GIAM_SAT.md)
- [Cập nhật auto-disconnect](CAP_NHAT_AUTO_DISCONNECT.md)

## 🐛 TROUBLESHOOTING

### Monitoring không hoạt động?

1. Kiểm tra Services đã cài đặt chưa:

   ```bash
   java -jar remote.jar --setup
   ```

2. Kiểm tra Services có chạy không:

   - Mở `services.msc`
   - Tìm "Sysmon" và "Winlogbeat"
   - Start nếu chưa chạy

3. Kiểm tra logs:
   ```
   C:\ProgramData\winlogbeat\logs
   ```

### Không kết nối được?

1. Kiểm tra Server Relay đã chạy chưa
2. Kiểm tra firewall có block không
3. Đảm bảo ports 5000, 6000, 7000 available

## 🎓 DÀNH CHO GIÁO DỤC

**LƯU Ý QUAN TRỌNG:**

- Đây là đồ án sinh viên
- Chỉ dùng cho mục đích học tập/test
- KHÔNG sử dụng cho môi trường production
- Code đã được đơn giản hóa để dễ hiểu

## 📝 CHANGELOG

### Version 1.0 (Nov 2025)

- ✅ Tích hợp monitoring module
- ✅ Thêm checkbox "Bật giám sát"
- ✅ Auto-disconnect cho CRITICAL/HIGH
- ✅ Auto-close alert sau 10s cho MEDIUM/LOW
- ✅ Đơn giản hóa UI (bỏ AlertPanel phức tạp)

## 👥 CREDITS

**Phát triển bởi:** [Tên sinh viên]  
**Giảng viên hướng dẫn:** [Tên giảng viên]  
**Trường:** [Tên trường]  
**Năm:** 2025

## 📄 LICENSE

Dự án mã nguồn mở cho mục đích giáo dục.

---

**🌟 Nếu thấy hữu ích, hãy cho dự án 1 star! 🌟**

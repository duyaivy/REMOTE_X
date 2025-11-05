# 🔒 HƯỚNG DẪN SỬ DỤNG TÍNH NĂNG GIÁM SÁT BẢO MẬT

## 📋 GIỚI THIỆU

Tính năng giám sát bảo mật đã được tích hợp vào phần mềm Remote Desktop. Khi bật, hệ thống sẽ sử dụng AI để phát hiện các hoạt động nguy hiểm trên máy tính được chia sẻ.

## ⚙️ CÁCH SỬ DỤNG

### 1. Chuẩn bị (Chỉ cần làm 1 lần)

Trước khi sử dụng tính năng giám sát, cần cài đặt Sysmon và Winlogbeat:

**Bước 1:** Mở Command Prompt với quyền Administrator

**Bước 2:** Chạy lệnh setup:

```bash
cd remote
java -jar remote.jar --setup
```

**Lưu ý:** Setup chỉ cần chạy 1 lần duy nhất. Sau đó các service sẽ tự động chạy khi khởi động Windows.

### 2. Sử dụng Giám Sát

#### Bên Máy Cho Phép Điều Khiển (Server):

1. Mở ứng dụng RemoteX
2. Nhập ID và Mật khẩu
3. **✅ TICK vào checkbox "Bật giám sát bảo mật (AI)"**
4. Click "Cho phép điều khiển"

![image](https://github.com/user-attachments/assets/screenshot-checkbox.png)

#### Bên Máy Điều Khiển (Viewer):

- Kết nối như bình thường
- Nếu phát hiện hoạt động nguy hiểm, sẽ nhận được cảnh báo

## 🚨 CÁC MỨC ĐỘ CẢNH BÁO

### 1. CRITICAL (Cực kỳ nguy hiểm) 🔴

**Hành động:** Tự động ngắt kết nối NGAY LẬP TỨC

**Ví dụ:**

- Ransomware đang mã hóa file
- Malware đang cài đặt backdoor
- Tấn công từ xa được phát hiện

**Thông báo:**

```
⚠️  CẢNH BÁO BẢO MẬT - CRITICAL SEVERITY

Phát hiện hoạt động nguy hiểm!

📋 Chi tiết:
   • Process: suspicious.exe
   • User: Administrator
   • Mức độ nguy hiểm: 9/10
   • Thời gian: 2025-11-03 14:30:15

⚠️  Kết nối sẽ BỊ NGẮT vì mức độ nguy hiểm cao!
```

### 2. HIGH (Nguy hiểm cao) 🟠

**Hành động:** Tự động ngắt kết nối NGAY LẬP TỨC

**Ví dụ:**

- PowerShell script đáng ngờ
- Truy cập registry nhạy cảm
- Network scanning

### 3. MEDIUM (Nguy hiểm trung bình) 🟡

**Hành động:** Hiển thị cảnh báo, tự động đóng sau 10 giây

**Ví dụ:**

- CMD commands bất thường
- File access patterns lạ
- Unusual process spawning

**Thông báo sẽ tự động biến mất sau 10 giây**, session vẫn tiếp tục.

### 4. LOW (Nguy hiểm thấp) 🟢

**Hành động:** Hiển thị thông báo, tự động đóng sau 10 giây

**Ví dụ:**

- Process startup bình thường
- Regular system activities
- Known safe operations

## 🔧 TROUBLESHOOTING

### Lỗi: "CHƯA CÀI ĐẶT AGENT"

**Giải pháp:**

1. Mở Command Prompt với quyền Administrator
2. Chạy: `java -jar remote.jar --setup`
3. Khởi động lại máy

### Lỗi: "Services không chạy"

**Giải pháp:**

1. Mở "Services" (services.msc)
2. Tìm "Sysmon" và "Winlogbeat"
3. Click chuột phải → Start

### Monitoring không hoạt động

**Kiểm tra:**

1. Đã tick checkbox "Bật giám sát" chưa?
2. Services Sysmon và Winlogbeat có đang chạy không?
3. File logs có được tạo không? (`C:\ProgramData\winlogbeat\logs`)

## 📊 KIẾN TRÚC HỆ THỐNG

```
┌─────────────────────────────────────────────────┐
│  Máy được chia sẻ (Server)                      │
│                                                 │
│  1. Sysmon (giám sát system events)            │
│     ↓                                           │
│  2. Winlogbeat (thu thập logs)                 │
│     ↓                                           │
│  3. AI Model (phân tích logs)                  │
│     ↓                                           │
│  4. Alert Service                               │
│     ↓                                           │
│  5. Gửi cảnh báo qua chat socket                │
└─────────────────────────────────────────────────┘
                    ↓
        (qua mạng TCP/IP)
                    ↓
┌─────────────────────────────────────────────────┐
│  Máy điều khiển (Viewer)                        │
│                                                 │
│  1. Nhận alert message                          │
│  2. Parse và hiển thị MessageBox                │
│  3. Auto-disconnect nếu CRITICAL/HIGH           │
│  4. Auto-close sau 10s nếu MEDIUM/LOW           │
└─────────────────────────────────────────────────┘
```

## ⚡ PERFORMANCE

- **CPU Usage:** ~5-10% khi monitoring
- **RAM Usage:** ~100-200 MB
- **Disk Usage:** ~50-100 MB cho logs (auto-rotate)
- **Latency:** <100ms detection time

## 🔐 BẢO MẬT

- AI model chạy local, không gửi data ra internet
- Logs được lưu local trên máy
- Chỉ gửi alert messages qua encrypted socket
- Không thu thập thông tin cá nhân

## 📝 GHI CHÚ QUAN TRỌNG

1. **Chỉ dành cho môi trường giáo dục/test**

   - Đây là đồ án sinh viên, không phải sản phẩm thương mại
   - Không sử dụng cho môi trường production

2. **Yêu cầu quyền Administrator**

   - Setup cần quyền Admin để cài Sysmon/Winlogbeat
   - Chạy monitoring không cần Admin

3. **Tương thích**
   - Windows 10/11
   - Cần Java 8 trở lên
   - Cần ít nhất 4GB RAM

## 🆘 HỖ TRỢ

Nếu gặp vấn đề, kiểm tra:

1. Log file tại: `C:\ProgramData\winlogbeat\logs`
2. Console output của ứng dụng
3. Windows Event Viewer

---

**Phát triển bởi:** [Tên sinh viên]  
**Mục đích:** Đồ án tốt nghiệp  
**Version:** 1.0  
**Ngày:** November 2025

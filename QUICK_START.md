# ⚡ QUICK START - BUILD & TEST

## 🔨 BUILD PROJECT

### 1. Build Remote.server

```bash
cd remote.server
mvn clean package
```

### 2. Build Remote (client + monitoring)

```bash
cd remote
mvn clean package
```

## 🚀 CHẠY DEMO

### Bước 1: Setup Services (Chỉ lần đầu)

```bash
# Mở CMD với quyền Administrator
cd remote\target
java -jar remote-1.0.jar --setup
```

**Kết quả mong đợi:**

```
✓ Sysmon installed
✓ Winlogbeat installed
✓ Config files created
✓ Services started
```

### Bước 2: Start Server Relay

```bash
# Terminal 1
cd remote.server\target
java -jar remote.server-1.0.jar
```

**Kết quả:**

```
Listening for connections on port 5000
Listening for connections on port 6000
Listening for connections on port 7000
```

### Bước 3: Start Sharer (Máy cho điều khiển)

```bash
# Terminal 2
cd remote\target
java -jar remote-1.0.jar
```

**Làm:**

1. Nhập ID: `test123`
2. Nhập Password: `password`
3. ✅ **TICK checkbox "Bật giám sát bảo mật (AI)"**
4. Click "Cho phép điều khiển"

**Kết quả mong đợi:**

```
[MONITOR] Đang khởi động monitoring...
[ML] Initializing ML components...
[ML] ✓ ML components ready!
[MONITOR] ✓ Monitoring đã khởi động thành công
Kết nối thành công! Bắt đầu chia sẻ.
```

### Bước 4: Start Viewer (Máy điều khiển)

```bash
# Terminal 3
cd remote\target
java -jar remote-1.0.jar
```

**Làm:**

1. Nhập ID đối tác: `test123`
2. Nhập Password: `password`
3. Click "Bắt đầu điều khiển"

**Kết quả:**

```
Kết nối thành công! Bắt đầu điều khiển.
```

## 🧪 TEST CASES

### Test 1: Monitoring Tắt

- [ ] Không tick checkbox
- [ ] Kết nối thành công
- [ ] Không có monitoring messages
- [ ] Remote desktop hoạt động bình thường

### Test 2: Monitoring Bật

- [ ] Tick checkbox "Bật giám sát"
- [ ] Thấy message: "[MONITOR] ✓ Monitoring đã khởi động"
- [ ] Thấy message: "[ML] ✓ ML components ready!"

### Test 3: Tạo Alert Giả (CRITICAL)

**Trên máy Sharer, mở PowerShell và chạy:**

```powershell
# Tạo file nguy hiểm giả
echo "malicious" > C:\Windows\Temp\test_malware.exe
```

**Kết quả mong đợi:**

- [ ] Console hiển thị: "🚨 CRITICAL SEVERITY ANOMALY DETECTED"
- [ ] Viewer nhận alert
- [ ] MessageBox hiển thị cảnh báo
- [ ] Kết nối TỰ ĐỘNG NGẮT
- [ ] Thông báo "Kết nối đã bị NGẮT"

### Test 4: Alert MEDIUM/LOW

**Trên máy Sharer, chạy lệnh bình thường:**

```bash
dir
ipconfig
```

**Kết quả mong đợi:**

- [ ] Có thể có alert MEDIUM/LOW
- [ ] MessageBox hiển thị
- [ ] **Tự động đóng sau 10 giây**
- [ ] Kết nối KHÔNG ngắt
- [ ] Tiếp tục điều khiển bình thường

### Test 5: Chat Window

**Trên Viewer:**

- [ ] Click menu "Công cụ" → "Mở Chat"
- [ ] Chat window hiển thị
- [ ] Gửi message → Thấy ở Sharer
- [ ] Chat hoạt động song song với monitoring

### Test 6: Cleanup

**Đóng Viewer:**

- [ ] Console hiển thị statistics
- [ ] Monitoring dừng
- [ ] No memory leaks

## 📊 EXPECTED OUTPUT

### Console Output - Sharer (với monitoring)

```
[MONITOR] Đang khởi động monitoring...

[ML] Initializing ML components...
[ML] Loading preprocessing artifacts...
[ML] ✓ Scaler loaded (42 features)
[ML] ✓ Label encoders loaded (15 encoders)
[ML] ✓ TF-IDF models loaded
[ML] Loading ONNX model...
[ML] ✓ Model loaded: IsolationForest (42 features)
[ML] ✓ ML components ready!

[MONITOR] ✓ Monitoring đã khởi động thành công
Kết nối thành công! Bắt đầu chia sẻ.
ip: /127.0.0.1

FULL 1 (150 KB) cho /127.0.0.1
DELTA 2 (50 KB) cho /127.0.0.1
...

═══════════════════════════════════════════════════════════════════
🚨 CRITICAL SEVERITY ANOMALY DETECTED
═══════════════════════════════════════════════════════════════════
⏰ Time:        2025-11-03 15:30:45
📊 Score:       0.9234 (Confidence: 87%, Risk: 9/10)

📋 Event Details:
  • Event Code:   1
  • User:         Administrator
  • Process:      powershell.exe
  • Parent:       explorer.exe
  • Command:      -enc JABlAHgAZQBjAC...

💡 Recommended Actions:
  🔴 1. IMMEDIATE ACTION REQUIRED - Critical threat detected!
  🔴 2. Isolate the system from network immediately
  🔴 3. Terminate suspicious process if safe to do so
  ...
═══════════════════════════════════════════════════════════════════

[SHARE] Auto disconnect triggered!

════════════════════════════════════════════════════════════════════
📊 ANOMALY DETECTION STATISTICS
════════════════════════════════════════════════════════════════════
Total Alerts:    5
  🔴 Critical:   1
  🟠 High:       2
  🟡 Medium:     1
  🟢 Low:        1
════════════════════════════════════════════════════════════════════

[MONITOR] ✓ Đã dừng monitoring
```

### Console Output - Viewer (với alert)

```
Kết nối thành công! Bắt đầu điều khiển.
Chat connection established

[ALERT] Received security alert: CRITICAL
```

## 🐛 TROUBLESHOOTING

### Lỗi: "Cannot find ONNX model"

**Giải pháp:**

```bash
# Kiểm tra file resources
ls remote/src/main/resources/*.onnx
ls remote/target/classes/*.onnx

# Rebuild nếu thiếu
cd remote
mvn clean package
```

### Lỗi: "Services not running"

**Giải pháp:**

1. Mở services.msc
2. Tìm "Sysmon" và "Winlogbeat"
3. Start manually
4. Hoặc chạy lại setup

### Alert không hiển thị

**Kiểm tra:**

1. Checkbox có được tick không?
2. Console có message "[MONITOR] ✓" không?
3. Logs có được tạo không? (`C:\ProgramData\winlogbeat\logs`)
4. Sysmon có chạy không?

### Monitoring không hoạt động

**Debug steps:**

1. Check console output
2. Xem có error message không
3. Kiểm tra file logs
4. Verify services status
5. Re-run setup nếu cần

## ✅ ACCEPTANCE CRITERIA

Dự án được coi là hoàn thành khi:

- [ ] Build thành công không lỗi
- [ ] Server relay chạy ổn định
- [ ] Remote desktop hoạt động
- [ ] Chat window hoạt động
- [ ] **Checkbox giám sát hiển thị đúng**
- [ ] **Monitoring bật/tắt theo checkbox**
- [ ] **Alert CRITICAL/HIGH → Auto disconnect**
- [ ] **Alert MEDIUM/LOW → Auto close 10s**
- [ ] Không có memory leaks
- [ ] Cleanup đúng cách

## 📸 SCREENSHOTS CẦN CHỤP CHO BÁO CÁO

1. **MainStart với checkbox**
2. **Console output khi monitoring bật**
3. **Alert CRITICAL với auto disconnect**
4. **Alert MEDIUM với auto close 10s**
5. **Statistics khi đóng**
6. **Services running (services.msc)**

## 🎓 DEMO FLOW CHO GIẢNG VIÊN

```
1. Giới thiệu tổng quan (2 phút)
   → Show README.md
   → Giải thích kiến trúc

2. Start các components (3 phút)
   → Server relay
   → Sharer (WITH monitoring checkbox)
   → Viewer

3. Demo remote desktop bình thường (2 phút)
   → Điều khiển chuột, bàn phím
   → Chat window

4. Demo monitoring (5 phút)
   → Tạo alert CRITICAL
   → Show auto disconnect
   → Tạo alert MEDIUM
   → Show auto close 10s

5. Giải thích code (5 phút)
   → MonitoringManager
   → AlertService
   → ReceiveScreen alert handling

6. Q&A (3 phút)
```

---

**TOTAL TIME: ~20 phút**

**KEY POINTS:**
✅ Đơn giản, dễ hiểu
✅ Tự động hóa tối đa
✅ Bảo mật tốt
✅ Code clean, maintainable

**GOOD LUCK! 🍀**

<div align="center">

<img src="docs/logo.png" alt="Lume logo" width="128" height="128">

# Lume

**Bộ công cụ Material 3 cho ROM tùy biến: lịch âm Việt Nam trên màn hình khóa, tự động hóa DIY, quét QR và chế độ lái xe.**

[English](README.md) · **Tiếng Việt**

![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)
![Material 3](https://img.shields.io/badge/Material-3-6750A4?logo=materialdesign&logoColor=white)
![Xposed](https://img.shields.io/badge/LSPosed-module-4F378B)
![License: MIT](https://img.shields.io/badge/License-MIT-FFC857)

</div>

---

Lume là bộ công cụ Android dành cho ROM tùy biến. Dự án gồm một ứng dụng giao diện Material 3
và một module Xposed/LSPosed. Module thêm lịch âm Việt Nam lên màn hình khóa. Ngoài ra còn có tự
động hóa, quét mã QR, chế độ lái xe và vài tinh chỉnh hệ thống.

## ✨ Tính năng

- **Lịch âm Việt Nam**: xem cả tháng, tra cứu ngày tốt, và hiển thị ngày âm cạnh dòng ngày trên
  màn hình khóa (cần module Xposed).
- **Tự động hóa DIY**: tạo quy tắc từ trigger và chuỗi hành động có thứ tự (sạc, pin, kết nối,
  ứng dụng, thời gian).
- **Chế độ lái xe**: bản đồ MapLibre, HUD tốc độ GPS, cảnh báo camera và tìm đường. Cũng chạy được
  trên Android Auto qua Car App Library.
- **Quét QR và mã vạch**: dùng camera trực tiếp hoặc chọn ảnh từ thư viện (URL, Wi-Fi, văn bản),
  có lịch sử.
- **Font UI hệ thống**: dùng file `.ttf` / `.otf` riêng làm phông chữ System UI (cần module Xposed).
- **Thời gian sáng màn hình**: cài đặt riêng cho màn hình chính và màn hình khóa (cần module
  Xposed).
- **Hình nền động**: hình nền màn hình khóa có lịch âm.

## 📋 Yêu cầu

- Android 10 trở lên (`minSdk` 29).
- Để dùng lịch âm trên màn hình khóa, font hệ thống và thời gian sáng màn hình: cần framework
  tương thích Xposed, ví dụ [LSPosed](https://github.com/LSPosed/LSPosed).
- Chế độ lái xe cần quyền vị trí; muốn hiển thị trên xe cần Android Auto.
- Một số hành động tự động hóa chạy lệnh shell qua `su`, nên cần root.

## 📲 Cài đặt

1. Tải APK ở mục Releases của repo, hoặc [tự build](#-build).
2. Cài APK lên máy.
3. Muốn dùng các tính năng module: mở LSPosed, bật **Lume**, chọn phạm vi **System UI** và
   **System Framework**. Khởi động lại máy, hoặc khởi động lại System UI.
4. Mở Lume và cấp các quyền mà từng tính năng yêu cầu.

Nếu bạn đang dùng bản cũ tên *Toolkit*, hãy gỡ nó trước. Tên gói đã đổi thành
`labs.hongducdev.lume`, nên Android coi Lume là một ứng dụng khác. Cài đặt cũ không được chuyển
sang, và bạn cần bật lại module trong LSPosed.

## 🔨 Build

Cần JDK 17 và Android SDK (compile SDK 36).

```bash
./gradlew testDebugUnitTest   # chạy unit test
./gradlew assembleDebug       # APK ở app/build/outputs/apk/debug/
```

Thư viện giả lập Xposed API nằm sẵn ở `app/libs/api-82.jar`, chỉ dùng khi biên dịch.

## 🗂️ Cấu trúc dự án

- `app/src/main/java/labs/hongducdev/lume/xposed/`: các hook Xposed và provider cấu hình mà hook
  đọc. Hook tự bắt lỗi của mình để System UI không bao giờ bị crash vì module.
- `app/src/main/java/labs/hongducdev/lume/domain/diy/` và `services/automation/`: mô hình và
  runtime của tự động hóa.
- `app/src/main/java/labs/hongducdev/lume/car/`: màn hình lái xe và Android Auto.
- `app/src/main/java/labs/hongducdev/lume/lunar/`: logic lịch âm.
- `app/src/test/`: unit test.

## ⚠️ Lưu ý và giới hạn

- Hook nhắm vào các class cụ thể của System UI, nên có thể không chạy trên ROM có màn hình khóa
  khác bản mà hook được viết cho. Hook lỗi sẽ được ghi log và bỏ qua.
- Lume còn sớm (phiên bản 0.1.0), có thể còn lỗi.

## 📄 Giấy phép

[MIT](LICENSE)

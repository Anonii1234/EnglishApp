# 📚 LEARN ENGLISH - Ứng dụng học từ vựng thông minh

Ứng dụng Android giúp người dùng học và ghi nhớ từ vựng tiếng Anh thông qua các phương pháp tương tác hiện đại, theo dõi tiến độ và hệ thống thành tích hấp dẫn.

## ✨ Tính năng chính

- **📖 Học theo chủ đề:** Danh sách từ vựng phong phú được chia theo nhiều chủ đề (Giao tiếp, Công việc, Du lịch...).
- **🗂️ Flashcards tương tác:** Lật thẻ để xem nghĩa, ví dụ và nghe phát âm (TTS). Hỗ trợ chế độ Anh-Việt và Việt-Anh.
- **📝 Kiểm tra (Quiz):** Hệ thống bài tập trắc nghiệm giúp củng cố kiến thức sau mỗi bài học.
- **📊 Theo dõi tiến độ:** 
    - Ghi nhận từ đã thuộc/chưa thuộc.
    - Thuật toán nhắc nhở ôn tập (Spaced Repetition).
    - Thống kê thời gian học và chuỗi ngày học (Streak).
- **🏆 Hệ thống Thành tích:** Mở khóa các huy hiệu khi đạt được các cột mốc học tập.
- **🔍 Tìm kiếm thông minh:** Tra cứu từ vựng nhanh chóng trong toàn bộ kho dữ liệu.
- **📥 Nhập dữ liệu:** Cho phép người dùng tự thêm chủ đề mới bằng cách dán văn bản định dạng đơn giản.
- **🖼️ App Widget:** Hiển thị mỗi ngày một từ vựng mới ngay trên màn hình chính, chạm để vào học ngay.

## 🛠️ Công nghệ sử dụng

- **Ngôn ngữ:** Kotlin
- **Cơ sở dữ liệu:** Room Database (Lưu trữ từ vựng, tiến độ, lịch sử)
- **Xử lý bất đồng bộ:** Coroutines & Flow
- **UI/UX:** Material Design Components, View Animation, Gesture Detector
- **Tiện ích:** Text-to-Speech (TTS), SharedPreferences, Gson

## 📂 Cấu trúc thư mục chính

- `com.tenban.learnenglish`
    - `MainActivity.kt`: Màn hình chính hiển thị danh sách chủ đề.
    - `LearnActivity.kt`: Trình học Flashcard.
    - `QuizActivity.kt`: Xử lý bài kiểm tra.
    - `WordWidgetProvider.kt`: Quản lý Widget ngoài màn hình chính.
    - `Progress.kt`: Định nghĩa các thực thể Database (Room).
    - `VocabularyData.kt`: Quản lý và xử lý dữ liệu từ vựng.
    - `StatsActivity.kt`: Hiển thị biểu đồ và số liệu học tập.

## 🚀 Cách cài đặt

1. Clone project hoặc tải mã nguồn.
2. Mở bằng **Android Studio (Ladybug hoặc mới hơn)**.
3. Chạy lệnh `Sync Project with Gradle Files`.
4. Run ứng dụng trên Emulator hoặc thiết bị thật.

---
*Phát triển bởi Nguyễn Văn An - Chúc bạn học tốt!*

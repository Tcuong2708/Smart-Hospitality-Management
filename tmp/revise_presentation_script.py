from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

OUT = r"D:\Document\KhoaLuanCuNhan\Projects\Smart-Hospitality-Management\tmp\Kich_Ban_Thuyet_Trinh_Khoa_Luan.docx"
NAVY, GOLD, PALE, WHITE, GRAY, BORDER = "12324A", "D9AE2B", "F3F6F8", "FFFFFF", "666666", "D9D9D9"

doc = Document()
sec = doc.sections[0]
sec.top_margin, sec.bottom_margin = Cm(1.6), Cm(1.6)
sec.left_margin, sec.right_margin = Cm(1.8), Cm(1.8)

normal = doc.styles["Normal"]
normal.font.name, normal.font.size = "Arial", Pt(10.5)
normal.paragraph_format.space_after, normal.paragraph_format.line_spacing = Pt(5), 1.1
for name, size in (("Title", 21), ("Heading 1", 16), ("Heading 2", 13)):
    st = doc.styles[name]
    st.font.name, st.font.size, st.font.bold = "Arial", Pt(size), True
    st.paragraph_format.space_before, st.paragraph_format.space_after = Pt(8), Pt(5)

def shade(cell, fill):
    pr = cell._tc.get_or_add_tcPr()
    shd = pr.find(qn("w:shd")) or OxmlElement("w:shd")
    if shd.getparent() is None: pr.append(shd)
    shd.set(qn("w:fill"), fill)

def borders(cell):
    pr = cell._tc.get_or_add_tcPr()
    box = pr.first_child_found_in("w:tcBorders") or OxmlElement("w:tcBorders")
    if box.getparent() is None: pr.append(box)
    for edge in ("top", "left", "bottom", "right"):
        node = box.find(qn(f"w:{edge}")) or OxmlElement(f"w:{edge}")
        if node.getparent() is None: box.append(node)
        node.set(qn("w:val"), "single"); node.set(qn("w:sz"), "4"); node.set(qn("w:color"), BORDER)

def format_table(table, widths):
    table.alignment, table.autofit = WD_TABLE_ALIGNMENT.CENTER, False
    for ri, row in enumerate(table.rows):
        for ci, cell in enumerate(row.cells):
            cell.width, cell.vertical_alignment = Cm(widths[ci]), WD_CELL_VERTICAL_ALIGNMENT.CENTER
            borders(cell); shade(cell, NAVY if ri == 0 else (WHITE if ri % 2 else PALE))
            for p in cell.paragraphs:
                p.paragraph_format.space_after = Pt(0)
                if ri == 0: p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                for run in p.runs:
                    run.font.name, run.font.size = "Arial", Pt(9.2)
                    if ri == 0:
                        run.font.bold = True; run.font.color.rgb = RGBColor.from_string(WHITE)

def bullet(text, level=0):
    p = doc.add_paragraph(style="List Bullet" if level == 0 else "List Bullet 2")
    p.paragraph_format.left_indent = Cm(0.55 + level * 0.4)
    p.paragraph_format.first_line_indent = Cm(-0.25)
    p.add_run(text)

def time(label, topic):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(2)
    r = p.add_run(label); r.bold = True; r.font.color.rgb = RGBColor.from_string(NAVY)
    p.add_run(" | ").font.color.rgb = RGBColor.from_string(GOLD)
    r = p.add_run(topic); r.italic = True; r.font.color.rgb = RGBColor.from_string(GRAY)

def speech(text):
    p = doc.add_paragraph()
    r = p.add_run("Lời nói: "); r.bold = True; r.font.color.rgb = RGBColor.from_string(NAVY)
    p.add_run(text)

def demo(items):
    p = doc.add_paragraph(); r = p.add_run("Thao tác demo"); r.bold = True; r.font.color.rgb = RGBColor.from_string(NAVY)
    for item in items: bullet(item)

title = doc.add_paragraph(style="Title"); title.alignment = WD_ALIGN_PARAGRAPH.CENTER
title.add_run("Kịch bản thuyết trình khóa luận tối đa 12 phút")
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("Hệ thống quản lý khách sạn thông minh May Hotel"); r.bold = True; r.font.size = Pt(13); r.font.color.rgb = RGBColor.from_string(NAVY)
p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("Thành viên 1 trình bày toàn bộ slide; thành viên 2 demo quản lý; thành viên 3 demo Web lễ tân và Mobile"); r.italic = True; r.font.color.rgb = RGBColor.from_string(GRAY)

doc.add_heading("Phân bổ thời gian", level=1)
t = doc.add_table(rows=1, cols=5)
for c, h in zip(t.rows[0].cells, ["Người trình bày", "Thời gian", "Phạm vi", "Trọng tâm", "Thời lượng"]): c.text = h
rows = [
    ["Thành viên 1", "00:00–03:55", "Toàn bộ 18 slide nội dung", "Bài toán, thiết kế, chức năng, kiểm thử, kết luận", "3 phút 55 giây"],
    ["Thành viên 2", "03:55–07:40", "Demo quản lý", "Danh mục, khuyến mãi, phân quyền, thống kê", "3 phút 45 giây"],
    ["Thành viên 3", "07:40–11:35", "Demo Web và Mobile", "Xác thực và nhận phòng", "3 phút 55 giây"],
    ["Dự phòng", "11:35–12:00", "Chuyển thiết bị", "Hoặc giới thiệu nhanh chức năng mở rộng", "25 giây"],
]
for vals in rows:
    cells = t.add_row().cells
    for c, v in zip(cells, vals): c.text = v
format_table(t, [2.8, 2.6, 3.7, 5.3, 2.6])

doc.add_heading("Nguyên tắc demo", level=1)
bullet("Không demo đăng nhập, đăng xuất, đổi mật khẩu hoặc nhập lại dữ liệu dài; các tài khoản và dữ liệu mẫu phải mở sẵn.")
bullet("Thành viên 1 trình bày liên tục toàn bộ slide, không chuyển người giữa phần lý thuyết.")
bullet("Luồng đặt phòng trực tuyến được giới thiệu bằng slide và kết quả chuẩn bị sẵn; ưu tiên thời gian cho hai demo nghiệp vụ chính.")
bullet("Nếu VNPay, NFC hoặc AI chậm quá 10 giây, chuyển sang dữ liệu mẫu đã chuẩn bị và tiếp tục lời thoại.")

doc.add_heading("Chuẩn bị trước khi báo cáo", level=1)
bullet("Bản trình chiếu mở sẵn từ slide nội dung đầu tiên; bỏ qua slide tiêu đề khi tính 18 slide nội dung.")
bullet("Web quản lý mở sẵn các tab Phòng, Loại phòng, Khuyến mãi, Tài khoản và Thống kê doanh thu.")
bullet("Web lễ tân mở sẵn Nhận phòng và Sơ đồ phòng; có một phiếu đặt hôm nay, đã cọc và đang chờ nhận phòng.")
bullet("Mobile mở đúng phiếu; chuẩn bị thẻ NFC mô phỏng, ảnh CCCD mẫu và một kết quả xác thực dự phòng.")

doc.add_page_break()
doc.add_heading("Thành viên 1 — Trình bày toàn bộ slide", level=1)
time("00:00–00:20", "Slide 1 — Nội dung")
speech("Kính thưa Hội đồng, nhóm trình bày hệ thống quản lý khách sạn thông minh May Hotel trên hai nền tảng Web và Mobile. Nội dung gồm khảo sát nghiệp vụ, phân tích thiết kế, các chức năng đã cài đặt, kiểm thử và kết luận.")
time("00:20–00:40", "Slide 2 — Công nghệ")
speech("Hệ thống sử dụng Spring Boot cho Backend, MongoDB Atlas cho dữ liệu, Thymeleaf cho Web, Flutter cho Mobile và Python cho các dịch vụ AI. Các nền tảng dùng chung API để đồng bộ nghiệp vụ.")
time("00:40–00:55", "Slide 3 — Biểu mẫu khảo sát")
speech("Nhóm số hóa các biểu mẫu thực tế như hóa đơn, danh mục dịch vụ, thông tin xuất hóa đơn và phiếu quà tặng; đây là cơ sở xây dựng cấu trúc dữ liệu và quy trình xử lý.")
time("00:55–01:30", "Slide 4 đến 7 — Use case và thiết kế")
speech("Các tác nhân chính gồm khách hàng, lễ tân, quản lý, kế toán, nhân viên dịch vụ và buồng phòng. Hệ thống bao quát đặt phòng, nhận và trả phòng, thanh toán, hóa đơn, quản lý danh mục, khuyến mãi, khách hàng thân thiết và báo cáo. Thiết kế được tổ chức theo lớp giao diện, điều khiển, nghiệp vụ và dữ liệu.")
time("01:30–02:35", "Slide 8 đến 12 — Chức năng cốt lõi")
speech("Khách hàng tra cứu phòng theo ngày và số người; kết quả chỉ công khai loại phòng còn khả dụng, không lộ số phòng vật lý. Luồng đặt phòng trực tuyến tính giá, thuế, khuyến mãi và yêu cầu đặt cọc 10 phần trăm qua VNPay. Khi nhận phòng, Mobile xác thực từng khách bằng khuôn mặt và NFC mô phỏng hoặc dữ liệu CCCD mẫu; Web lễ tân chọn phòng đúng loại và hoàn tất nhận phòng. Khối quản lý thực hiện quản lý phòng, loại phòng, dịch vụ, nhân viên, khách hàng, tài khoản, khuyến mãi và thống kê doanh thu.")
time("02:35–03:05", "Slide 13 đến 15 — Kiểm thử")
speech("Nhóm kiểm thử các luồng đặt phòng, thanh toán, nhận phòng, phân quyền, cập nhật trạng thái và dữ liệu không hợp lệ. Kết quả cho thấy các chức năng chính đáp ứng điều kiện nghiệp vụ và trả thông báo phù hợp khi có lỗi.")
time("03:05–03:40", "Slide 16 — Kết luận và hướng phát triển")
speech("Hệ thống đã hoàn thiện hai nền tảng dùng chung dữ liệu, số hóa các nghiệp vụ chính và bổ sung chatbot, dự đoán khách không đến cùng tự động hủy theo quy tắc. Hướng phát triển là nâng độ tin cậy khi đọc CCCD thật, tối ưu mô hình AI và mở rộng tự động hóa vận hành.")
time("03:40–03:55", "Slide 17 và 18 — Tài liệu tham khảo, chuyển demo")
speech("Các nguồn tham khảo được liệt kê trong báo cáo. Tiếp theo, nhóm trình bày hai phần demo chính: quản lý hệ thống và nhận phòng đồng bộ giữa Web với Mobile.")

doc.add_page_break()
doc.add_heading("Thành viên 2 — Demo khối quản lý", level=1)
time("03:55–04:10", "Giới thiệu phạm vi")
speech("Em trình bày vai trò quản lý với các chức năng danh mục, khuyến mãi, tài khoản, phân quyền và báo cáo. Các màn hình dùng chung nguyên tắc tìm kiếm, lọc, phân trang và kiểm tra dữ liệu đầu vào.")
time("04:10–07:20", "Demo quản lý")
demo([
    "Mở Quản lý phòng, dùng bộ lọc và xem một bản ghi để chứng minh dữ liệu thực từ MongoDB Atlas.",
    "Mở Loại phòng hoặc Dịch vụ, giới thiệu một lần thao tác thêm, sửa, xóa và phân trang; không lặp lại toàn bộ CRUD.",
    "Mở Khuyến mãi, chỉ ra thời gian hiệu lực, trạng thái tự động và phạm vi công khai hoặc gửi riêng cho khách.",
    "Mở Quản lý tài khoản và Phân quyền vai trò; chỉ ra quyền theo chức năng Web hoặc Mobile.",
    "Mở Thống kê doanh thu, chọn khoảng thời gian và giới thiệu nút xuất báo cáo.",
])
speech("Các ràng buộc được kiểm tra tại Backend. Khuyến mãi tự chuyển trạng thái theo thời gian; quyền truy cập không chỉ ẩn menu mà còn được kiểm tra tại API. Báo cáo tổng hợp theo khoảng ngày và hỗ trợ xuất phục vụ kế toán.")
time("07:20–07:40", "Chuyển phần demo")
speech("Sau khi dữ liệu đặt phòng được quản lý tập trung, em xin chuyển sang luồng vận hành nhận phòng và xác thực khách trên Mobile.")

doc.add_page_break()
doc.add_heading("Thành viên 3 — Demo nhận phòng trên Web và Mobile", level=1)
time("07:40–08:00", "Giới thiệu nghiệp vụ")
speech("Luồng nhận phòng gồm xác định phiếu đặt hôm nay, xác thực đủ số khách lưu trú, chọn phòng đúng loại đã đặt và duyệt nhận phòng. Mobile thực hiện xác thực; Web lễ tân theo dõi kết quả và hoàn tất nghiệp vụ.")
time("08:00–10:55", "Demo đồng bộ Web và Mobile")
demo([
    "Trên Web lễ tân, mở danh sách chờ nhận phòng và chọn phiếu đã chuẩn bị; chỉ ra số khách cần xác thực và loại phòng đã đặt.",
    "Trên Mobile, mở Nhận phòng thông minh, chụp khuôn mặt và quét NFC mô phỏng hoặc ảnh CCCD mẫu. Nếu có nhiều khách, xác thực lần lượt đến đủ số lượng.",
    "Nêu ngoại lệ: khách nước ngoài hoặc xác thực thất bại thì lễ tân nhập tay có kiểm soát.",
    "Sau khi Mobile báo thành công, quay lại Web, chọn phòng trống đúng loại và duyệt nhận phòng.",
    "Mở Sơ đồ phòng để chứng minh phòng chuyển sang trạng thái Đang ở theo thời gian đã chọn.",
])
speech("Backend ghi nhận kết quả của từng khách trong phiếu và chỉ cho duyệt khi đủ số người. Phòng phải đúng loại đã đặt và không bị trùng lịch lưu trú. Kết quả được đồng bộ về Web; trạng thái phòng thay đổi theo thời gian thực tế.")
time("10:55–11:20", "Giới thiệu nhanh chức năng Mobile liên quan")
speech("Mobile còn hỗ trợ sinh trắc học, lịch sử đặt phòng và hóa đơn, đặt dịch vụ, cập nhật trạng thái buồng phòng, in hóa đơn nhiệt 58 mm và kiểm tra Root, mở khóa bộ nạp khởi động hoặc USB gỡ lỗi.")
time("11:20–11:35", "Kết thúc")
speech("Nhóm xin trân trọng cảm ơn Hội đồng đã lắng nghe và sẵn sàng trả lời câu hỏi.")

doc.add_heading("Nếu còn thời gian: chức năng ngoài đề cương", level=1)
bullet("AI dự đoán khách không đến, lịch hủy tự động và ghi nhận khách báo đến trễ.")
bullet("Đổi phòng, trả phòng, lệnh dọn phòng và cập nhật bảo trì theo thời gian thực.")
bullet("Đặt nhiều dịch vụ trên một hóa đơn và in nhiệt Bluetooth 58 mm.")
bullet("Chatbot tư vấn loại phòng, tra cứu phòng trống và tạo liên kết đặt cọc.")
bullet("Chỉ giới thiệu mỗi chức năng từ 5 đến 10 giây; không mở thêm nếu đã đến phút 11:35.")

doc.add_page_break()
doc.add_heading("Bảng đối chiếu Use Case và phạm vi trình bày", level=1)
t = doc.add_table(rows=1, cols=4)
for c, h in zip(t.rows[0].cells, ["Nhóm tác nhân", "Chức năng chính", "Hình thức trình bày", "Người phụ trách"]): c.text = h
matrix = [
    ["Khách hàng", "Tra cứu, đặt phòng trực tuyến, thanh toán/cọc, lịch sử, đánh giá", "Thành viên 1 giới thiệu trên slide", "Thành viên 1"],
    ["Quản trị và quản lý", "Danh mục, khuyến mãi, tài khoản, phân quyền, thống kê", "Demo trực tiếp", "Thành viên 2"],
    ["Lễ tân", "Đặt trực tiếp, nhận phòng, xác thực, đổi/trả phòng, hóa đơn", "Demo nhận phòng; phần còn lại giới thiệu", "Thành viên 3"],
    ["Mobile", "NFC, khuôn mặt, đặt dịch vụ, lịch sử, hóa đơn, bảo vệ thiết bị", "Demo xác thực; chức năng khác giới thiệu", "Thành viên 3"],
    ["Kế toán, buồng phòng, dịch vụ", "Báo cáo; dọn phòng; dịch vụ và in nhiệt", "Chỉ giới thiệu nếu còn thời gian", "Thành viên 2 và 3"],
]
for vals in matrix:
    cells = t.add_row().cells
    for c, v in zip(cells, vals): c.text = v
format_table(t, [3.0, 6.4, 4.4, 3.0])

doc.add_heading("Kịch bản xử lý sự cố", level=1)
t = doc.add_table(rows=1, cols=3)
for c, h in zip(t.rows[0].cells, ["Sự cố", "Xử lý ngay", "Câu chuyển tiếp"]): c.text = h
fallbacks = [
    ["VNPay hoặc mạng chậm", "Dừng chờ sau 10 giây, mở đơn mẫu đã thanh toán", "Đây là đơn đã nhận phản hồi thành công và chuyển sang chờ nhận phòng."],
    ["NFC không đọc", "Dùng thẻ mô phỏng hoặc dữ liệu CCCD mẫu", "Trong môi trường nghiên cứu, nhóm dùng dữ liệu mô phỏng cùng cấu trúc xác thực."],
    ["AI xác thực chậm", "Mở kết quả đã lưu hoặc dùng nhập tay ngoại lệ", "Hệ thống có cơ chế dự phòng cho xác thực tự động thất bại."],
    ["Web chưa cập nhật", "Làm mới một lần hoặc mở Sơ đồ phòng dự phòng", "Backend đã lưu và giao diện đang tải trạng thái mới nhất."],
]
for vals in fallbacks:
    cells = t.add_row().cells
    for c, v in zip(cells, vals): c.text = v
format_table(t, [3.3, 5.3, 8.2])

for section in doc.sections:
    p = section.footer.paragraphs[0]; p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("Kịch bản thuyết trình May Hotel — tối đa 12 phút"); r.font.name = "Arial"; r.font.size = Pt(8); r.font.color.rgb = RGBColor.from_string(GRAY)

doc.save(OUT)
print(OUT)

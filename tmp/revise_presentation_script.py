from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

OUT = r"D:\Document\KhoaLuanCuNhan\Projects\Smart-Hospitality-Management\tmp\Kich_Ban_Thuyet_Trinh_Khoa_Luan_v2.docx"
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
r = p.add_run("Thành viên 1 trình bày toàn bộ slide và đặt phòng trực tuyến; thành viên 3 tiếp nối nhận phòng; thành viên 2 demo quản lý"); r.italic = True; r.font.color.rgb = RGBColor.from_string(GRAY)

doc.add_heading("Phân bổ thời gian", level=1)
t = doc.add_table(rows=1, cols=5)
for c, h in zip(t.rows[0].cells, ["Người trình bày", "Thời gian", "Phạm vi", "Trọng tâm", "Thời lượng"]): c.text = h
rows = [
    ["Thành viên 1", "00:00–04:00", "Toàn bộ slide và đặt phòng", "Tổng quan, chức năng và đặt phòng trực tuyến", "4 phút"],
    ["Thành viên 3", "04:00–07:55", "Demo Web và Mobile", "Tiếp nối đơn vừa tạo để xác thực, nhận phòng", "3 phút 55 giây"],
    ["Thành viên 2", "07:55–11:35", "Demo quản lý", "Danh mục, khuyến mãi, phân quyền, thống kê", "3 phút 40 giây"],
    ["Dự phòng", "11:35–12:00", "Chuyển thiết bị", "Hoặc giới thiệu nhanh chức năng mở rộng", "25 giây"],
]
for vals in rows:
    cells = t.add_row().cells
    for c, v in zip(cells, vals): c.text = v
format_table(t, [2.8, 2.6, 3.7, 5.3, 2.6])

doc.add_heading("Nguyên tắc demo", level=1)
bullet("Không demo đăng nhập, đăng xuất, đổi mật khẩu hoặc nhập lại dữ liệu dài; các tài khoản và dữ liệu mẫu phải mở sẵn.")
bullet("Thành viên 1 trình bày liên tục toàn bộ slide rồi demo một luồng đặt phòng trực tuyến ngắn gọn.")
bullet("Đơn vừa được tạo ở phần đặt phòng phải là chính đơn mà thành viên 3 dùng để demo xác thực và nhận phòng.")
bullet("Nếu VNPay, NFC hoặc AI chậm quá 10 giây, chuyển sang dữ liệu mẫu đã chuẩn bị và tiếp tục lời thoại.")

doc.add_heading("Chuẩn bị trước khi báo cáo", level=1)
bullet("Bản trình chiếu mở sẵn từ slide nội dung đầu tiên; bỏ qua slide tiêu đề khi tính 18 slide nội dung.")
bullet("Web quản lý mở sẵn các tab Phòng, Loại phòng, Khuyến mãi, Tài khoản và Thống kê doanh thu.")
bullet("Web lễ tân mở sẵn Nhận phòng và Sơ đồ phòng; có một phiếu đặt hôm nay, đã cọc và đang chờ nhận phòng.")
bullet("Mobile mở đúng phiếu; chuẩn bị thẻ NFC mô phỏng, ảnh CCCD mẫu và một kết quả xác thực dự phòng.")

doc.add_page_break()
doc.add_heading("Thành viên 1 — Toàn bộ slide và đặt phòng trực tuyến", level=1)
time("00:00–00:20", "Slide 1 đến 3 — Tổng quan, công nghệ và biểu mẫu")
speech("Kính thưa Hội đồng, nhóm trình bày hệ thống May Hotel trên Web và Mobile. Backend dùng Spring Boot, dữ liệu MongoDB Atlas, Web dùng Thymeleaf, Mobile dùng Flutter và dịch vụ AI dùng Python. Các biểu mẫu thực tế là cơ sở xây dựng dữ liệu và nghiệp vụ.")
time("00:20–00:55", "Slide 4 đến 7 — Use case và thiết kế")
speech("Các tác nhân gồm khách hàng, lễ tân, quản lý, kế toán, nhân viên dịch vụ và buồng phòng. Hệ thống bao quát đặt, nhận, đổi và trả phòng; thanh toán, hóa đơn, danh mục, khuyến mãi, khách hàng thân thiết và báo cáo. Thiết kế tách lớp giao diện, điều khiển, nghiệp vụ và dữ liệu.")
time("00:55–01:30", "Slide 8 đến 12 — Chức năng cốt lõi")
speech("Khách tra cứu theo ngày và số người, đặt phòng và cọc qua VNPay. Khi nhận phòng, Mobile xác thực từng khách; Web lễ tân chọn đúng loại phòng và duyệt nhận phòng. Khối quản lý phụ trách danh mục, tài khoản, khuyến mãi và thống kê.")
time("01:30–01:50", "Slide 13 đến 18 — Kiểm thử và kết luận")
speech("Nhóm đã kiểm thử các luồng chính, dữ liệu không hợp lệ và phân quyền. Hệ thống hoàn thiện hai nền tảng dùng chung dữ liệu, đồng thời bổ sung chatbot và dự đoán khách không đến. Hướng phát triển là nâng độ tin cậy khi đọc CCCD thật và tối ưu AI.")
time("01:50–03:50", "Demo vai trò khách hàng — Đặt phòng trực tuyến")
demo([
    "Chọn ngày nhận, ngày trả và số khách; nhấn Tìm phòng.",
    "Chọn một loại phòng còn trống; kiểm tra giá, thuế và khuyến mãi.",
    "Xác nhận thông tin và chọn đặt cọc 10% qua VNPay.",
    "Mở kết quả đơn đã xác nhận; đọc mã đơn để thành viên 3 tiếp tục nhận phòng.",
])
speech("Hệ thống kiểm tra phòng theo đúng khoảng lưu trú, chỉ công khai loại phòng, tính giá tại Backend và chỉ xác nhận đơn sau phản hồi thanh toán hợp lệ.")
time("03:50–04:00", "Chuyển trực tiếp sang nhận phòng")
speech("Đơn vừa tạo đã chuyển sang danh sách chờ nhận phòng. Em xin chuyển cho thành viên tiếp theo xác thực khách và hoàn tất nhận phòng trên chính đơn này.")

doc.add_page_break()
doc.add_heading("Thành viên 2 — Demo khối quản lý", level=1)
time("07:55–08:10", "Giới thiệu phạm vi")
speech("Em trình bày vai trò quản lý với các chức năng danh mục, khuyến mãi, tài khoản, phân quyền và báo cáo. Các màn hình dùng chung nguyên tắc tìm kiếm, lọc, phân trang và kiểm tra dữ liệu đầu vào.")
time("08:10–11:15", "Demo quản lý")
demo([
    "Mở Quản lý phòng, dùng bộ lọc và xem một bản ghi để chứng minh dữ liệu thực từ MongoDB Atlas.",
    "Mở Loại phòng hoặc Dịch vụ, giới thiệu một lần thao tác thêm, sửa, xóa và phân trang; không lặp lại toàn bộ CRUD.",
    "Mở Khuyến mãi, chỉ ra thời gian hiệu lực, trạng thái tự động và phạm vi công khai hoặc gửi riêng cho khách.",
    "Mở Quản lý tài khoản và Phân quyền vai trò; chỉ ra quyền theo chức năng Web hoặc Mobile.",
    "Mở Thống kê doanh thu, chọn khoảng thời gian và giới thiệu nút xuất báo cáo.",
])
speech("Các ràng buộc được kiểm tra tại Backend. Khuyến mãi tự chuyển trạng thái theo thời gian; quyền truy cập không chỉ ẩn menu mà còn được kiểm tra tại API. Báo cáo tổng hợp theo khoảng ngày và hỗ trợ xuất phục vụ kế toán.")
time("11:15–11:35", "Kết thúc")
speech("Qua phần demo, dữ liệu từ khách hàng, lễ tân và quản lý được xử lý thống nhất trên cùng hệ thống. Nhóm xin trân trọng cảm ơn Hội đồng và sẵn sàng trả lời câu hỏi.")

doc.add_page_break()
doc.add_heading("Thành viên 3 — Demo nhận phòng trên Web và Mobile", level=1)
time("04:00–04:20", "Tiếp nhận đúng đơn vừa đặt")
speech("Luồng nhận phòng gồm xác định phiếu đặt hôm nay, xác thực đủ số khách lưu trú, chọn phòng đúng loại đã đặt và duyệt nhận phòng. Mobile thực hiện xác thực; Web lễ tân theo dõi kết quả và hoàn tất nghiệp vụ.")
time("04:20–07:15", "Demo đồng bộ Web và Mobile")
demo([
    "Trên Web lễ tân, mở danh sách chờ nhận phòng và chọn phiếu đã chuẩn bị; chỉ ra số khách cần xác thực và loại phòng đã đặt.",
    "Trên Mobile, mở Nhận phòng thông minh, chụp khuôn mặt và quét NFC mô phỏng hoặc ảnh CCCD mẫu. Nếu có nhiều khách, xác thực lần lượt đến đủ số lượng.",
    "Nêu ngoại lệ: khách nước ngoài hoặc xác thực thất bại thì lễ tân nhập tay có kiểm soát.",
    "Sau khi Mobile báo thành công, quay lại Web, chọn phòng trống đúng loại và duyệt nhận phòng.",
    "Mở Sơ đồ phòng để chứng minh phòng chuyển sang trạng thái Đang ở theo thời gian đã chọn.",
])
speech("Backend ghi nhận kết quả của từng khách trong phiếu và chỉ cho duyệt khi đủ số người. Phòng phải đúng loại đã đặt và không bị trùng lịch lưu trú. Kết quả được đồng bộ về Web; trạng thái phòng thay đổi theo thời gian thực tế.")
time("07:15–07:40", "Giới thiệu nhanh chức năng Mobile liên quan")
speech("Mobile còn hỗ trợ sinh trắc học, lịch sử đặt phòng và hóa đơn, đặt dịch vụ, cập nhật trạng thái buồng phòng, in hóa đơn nhiệt 58 mm và kiểm tra Root, mở khóa bộ nạp khởi động hoặc USB gỡ lỗi.")
time("07:40–07:55", "Chuyển sang khối quản lý")
speech("Như vậy, một đơn được đi xuyên suốt từ khách đặt trực tuyến đến xác thực và nhận phòng. Em xin chuyển phần cuối cho thành viên 2 trình bày khối quản lý và báo cáo.")

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

# Đưa phần nhận phòng lên ngay sau phần đặt phòng để luồng demo liền mạch:
# Thành viên 1 -> Thành viên 3 -> Thành viên 2 -> phụ lục.
body = doc._body._element
children = list(body)
heading2 = next(p._element for p in doc.paragraphs if p.text == "Thành viên 2 — Demo khối quản lý")
heading3 = next(p._element for p in doc.paragraphs if p.text == "Thành viên 3 — Demo nhận phòng trên Web và Mobile")
appendix = next(p._element for p in doc.paragraphs if p.text == "Nếu còn thời gian: chức năng ngoài đề cương")
i2, i3, ia = children.index(heading2), children.index(heading3), children.index(appendix)
start2 = i2 - 1
start3 = i3 - 1
starta = ia
block2 = children[start2:start3]
block3 = children[start3:starta]
for element in block2 + block3:
    body.remove(element)
insert_at = start2
for element in block3 + block2:
    body.insert(insert_at, element)
    insert_at += 1

for section in doc.sections:
    p = section.footer.paragraphs[0]; p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("Kịch bản thuyết trình May Hotel — tối đa 12 phút"); r.font.name = "Arial"; r.font.size = Pt(8); r.font.color.rgb = RGBColor.from_string(GRAY)

doc.save(OUT)
print(OUT)

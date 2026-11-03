from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.section import WD_SECTION
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.style import WD_STYLE_TYPE

OUT = r"D:\Document\KhoaLuanCuNhan\Projects\Smart-Hospitality-Management\tmp\Kich_Ban_Thuyet_Trinh_Khoa_Luan.docx"

NAVY = "12324A"
GOLD = "D9AE2B"
PALE = "F3F6F8"
LIGHT_BLUE = "EAF2F8"
WHITE = "FFFFFF"
BLACK = "000000"
GRAY = "666666"
BORDER = "D9D9D9"

doc = Document()
sec = doc.sections[0]
sec.top_margin = Cm(1.7)
sec.bottom_margin = Cm(1.7)
sec.left_margin = Cm(1.8)
sec.right_margin = Cm(1.8)

styles = doc.styles
normal = styles["Normal"]
normal.font.name = "Arial"
normal.font.size = Pt(10.5)
normal.font.color.rgb = RGBColor.from_string(BLACK)
normal.paragraph_format.space_after = Pt(5)
normal.paragraph_format.line_spacing = 1.12

for name, size in [("Title", 21), ("Heading 1", 16), ("Heading 2", 13), ("Heading 3", 11)]:
    st = styles[name]
    st.font.name = "Arial"
    st.font.size = Pt(size)
    st.font.bold = True
    st.font.color.rgb = RGBColor.from_string(BLACK)
    st.paragraph_format.space_before = Pt(10 if name != "Title" else 0)
    st.paragraph_format.space_after = Pt(6)

if "Time" not in styles:
    time_style = styles.add_style("Time", WD_STYLE_TYPE.PARAGRAPH)
else:
    time_style = styles["Time"]
time_style.font.name = "Arial"
time_style.font.size = Pt(9)
time_style.font.bold = True
time_style.font.color.rgb = RGBColor.from_string(NAVY)
time_style.paragraph_format.space_after = Pt(2)

def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)

def set_cell_borders(cell, color=BORDER):
    tc_pr = cell._tc.get_or_add_tcPr()
    borders = tc_pr.first_child_found_in("w:tcBorders")
    if borders is None:
        borders = OxmlElement("w:tcBorders")
        tc_pr.append(borders)
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        node = borders.find(qn(f"w:{edge}"))
        if node is None:
            node = OxmlElement(f"w:{edge}")
            borders.append(node)
        node.set(qn("w:val"), "single")
        node.set(qn("w:sz"), "4")
        node.set(qn("w:color"), color)

def set_cell_margins(cell, top=90, start=110, bottom=90, end=110):
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for m, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{m}"))
        if node is None:
            node = OxmlElement(f"w:{m}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")

def format_table(table, widths=None, header=True):
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    for ri, row in enumerate(table.rows):
        for ci, cell in enumerate(row.cells):
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            set_cell_borders(cell)
            set_cell_margins(cell)
            if widths:
                cell.width = Cm(widths[ci])
            if header and ri == 0:
                set_cell_shading(cell, NAVY)
                for p in cell.paragraphs:
                    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                    for run in p.runs:
                        run.font.bold = True
                        run.font.color.rgb = RGBColor.from_string(WHITE)
                        run.font.size = Pt(9.5)
            else:
                set_cell_shading(cell, WHITE if ri % 2 else PALE)
                for p in cell.paragraphs:
                    p.paragraph_format.space_after = Pt(0)
                    for run in p.runs:
                        run.font.size = Pt(9.3)

def add_bullet(text, level=0, bold_lead=None):
    p = doc.add_paragraph(style="List Bullet" if level == 0 else "List Bullet 2")
    p.paragraph_format.left_indent = Cm(0.55 + level * 0.45)
    p.paragraph_format.first_line_indent = Cm(-0.25)
    if bold_lead and text.startswith(bold_lead):
        p.add_run(bold_lead).bold = True
        p.add_run(text[len(bold_lead):])
    else:
        p.add_run(text)
    return p

def add_time(label, slide_text):
    p = doc.add_paragraph(style="Time")
    p.add_run(label)
    p.add_run(" | ").font.color.rgb = RGBColor.from_string(GOLD)
    r = p.add_run(slide_text)
    r.font.color.rgb = RGBColor.from_string(GRAY)

def add_speech(text):
    p = doc.add_paragraph()
    lead = p.add_run("Lời nói: ")
    lead.bold = True
    lead.font.color.rgb = RGBColor.from_string(NAVY)
    p.add_run(text)
    return p

def add_demo(steps):
    p = doc.add_paragraph()
    r = p.add_run("Thao tác demo")
    r.bold = True
    r.font.color.rgb = RGBColor.from_string(NAVY)
    for step in steps:
        add_bullet(step)

title = doc.add_paragraph(style="Title")
title.alignment = WD_ALIGN_PARAGRAPH.CENTER
title.add_run("Kịch bản thuyết trình khóa luận tối đa 12 phút")
sub = doc.add_paragraph()
sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = sub.add_run("Hệ thống quản lý khách sạn thông minh May Hotel")
r.bold = True
r.font.size = Pt(13)
r.font.color.rgb = RGBColor.from_string(NAVY)

p = doc.add_paragraph()
p.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = p.add_run("Ba thành viên, tối đa 4 phút mỗi người, 18 slide nội dung sau slide tiêu đề")
r.italic = True
r.font.color.rgb = RGBColor.from_string(GRAY)

doc.add_heading("Nguyên tắc điều phối", level=1)
add_bullet("Tổng thời lượng mục tiêu 11 phút 30 giây, chừa 30 giây cho chuyển màn hình hoặc xử lý chậm.")
add_bullet("Không demo đăng nhập, đăng xuất, đổi mật khẩu hoặc nhập lại dữ liệu dài. Mỗi tài khoản và dữ liệu mẫu phải mở sẵn.")
add_bullet("Chỉ trình diễn một luồng chính cho mỗi vai trò. Các chức năng CRUD tương tự chỉ giới thiệu bằng lời.")
add_bullet("Nếu mạng, VNPay, NFC hoặc AI phản hồi chậm quá 10 giây, chuyển ngay sang dữ liệu mẫu đã chuẩn bị và tiếp tục lời thoại.")

table = doc.add_table(rows=1, cols=5)
headers = ["Người trình bày", "Thời gian", "Slide chính", "Demo trọng tâm", "Thời lượng"]
for c, h in zip(table.rows[0].cells, headers): c.text = h
rows = [
    ["Thành viên 1", "00:00-03:50", "1-9", "Khách hàng đặt phòng trực tuyến", "3 phút 50 giây"],
    ["Thành viên 2", "03:50-07:35", "11-15", "Quản trị và quản lý", "3 phút 45 giây"],
    ["Thành viên 3", "07:35-11:30", "10, 16-18", "Nhận phòng trên Web và Mobile", "3 phút 55 giây"],
]
for vals in rows:
    cells = table.add_row().cells
    for c, v in zip(cells, vals): c.text = v
format_table(table, [3.0, 2.7, 2.3, 6.1, 2.7])

doc.add_heading("Chuẩn bị trước khi báo cáo", level=1)
add_bullet("Web khách hàng mở sẵn trang tra cứu phòng; tài khoản khách hàng đã đăng nhập và có hồ sơ đầy đủ.")
add_bullet("Web quản trị mở sẵn các tab Phòng, Loại phòng, Khuyến mãi, Tài khoản và Thống kê doanh thu.")
add_bullet("Web lễ tân mở sẵn Nhận phòng và Sơ đồ phòng; có một phiếu đặt hôm nay, đã cọc và đang chờ nhận phòng.")
add_bullet("Mobile mở sẵn phiếu tương ứng; chuẩn bị thẻ NFC mô phỏng, ảnh CCCD mẫu và dữ liệu xác thực dự phòng.")
add_bullet("Chuẩn bị sẵn ảnh hoặc video ngắn cho máy in nhiệt, trạng thái phòng thời gian thực và kết quả AI nếu không kịp demo trực tiếp.")

doc.add_page_break()
doc.add_heading("Thành viên 1  Tổng quan và đặt phòng trực tuyến", level=1)
add_time("00:00-00:20", "Slide 1 - Nội dung")
add_speech("Kính thưa Hội đồng, nhóm trình bày hệ thống quản lý khách sạn thông minh trên hai nền tảng Web và Mobile. Hệ thống bao quát nghiệp vụ đặt phòng, nhận phòng, trả phòng, thanh toán, quản lý danh mục, báo cáo và các chức năng AI hỗ trợ vận hành.")

add_time("00:20-00:45", "Slide 2 - Công nghệ sử dụng")
add_speech("Hệ thống sử dụng Spring Boot cho Backend, MongoDB Atlas cho dữ liệu, Thymeleaf cho Web, Flutter cho Mobile và Python cho các dịch vụ AI. Các nền tảng dùng chung API nên dữ liệu được đồng bộ theo cùng nghiệp vụ.")

add_time("00:45-01:00", "Slide 3 - Biểu mẫu")
add_speech("Nhóm khảo sát và số hóa các biểu mẫu thực tế gồm hóa đơn giá trị gia tăng, danh mục dịch vụ, thông tin xuất hóa đơn và phiếu quà tặng. Các biểu mẫu này là cơ sở xây dựng dữ liệu và quy trình thao tác.")

add_time("01:00-01:35", "Slide 4 đến 7 - Use case và thiết kế")
add_speech("Các sơ đồ thể hiện nhóm tác nhân khách hàng, lễ tân, quản lý, kế toán, nhân viên dịch vụ và buồng phòng. Hệ thống được tổ chức theo lớp giao diện, điều khiển, thực thể và truy cập dữ liệu. Sau đây nhóm tập trung vào các chức năng đã cài đặt thay vì đi chi tiết từng lớp.")

add_time("01:35-01:50", "Slide 8 - Tra cứu phòng trống")
add_speech("Khách hàng tra cứu theo ngày nhận, ngày trả, số người lớn, trẻ em và em bé. Kết quả trả về loại phòng còn khả dụng theo đúng khoảng thời gian, không công khai số phòng vật lý.")

add_time("01:50-03:35", "Slide 9 và demo - Đặt phòng trực tuyến")
add_demo([
    "Chọn ngày nhận, ngày trả và số khách rồi nhấn Tìm phòng.",
    "Chọn một loại phòng còn trống, kiểm tra giá, thuế và ưu đãi đang áp dụng.",
    "Mở trang đặt phòng, xác nhận thông tin khách và chọn đặt cọc 10% qua VNPay.",
    "Chỉ ra kết quả tạo đơn và trạng thái thanh toán; không chờ thao tác ngân hàng nếu môi trường sandbox chậm.",
])
add_speech("Đây là luồng đặt phòng trực tuyến của khách hàng. Hệ thống kiểm tra tồn phòng theo thời gian, tính giá và thuế tại Backend, áp dụng khuyến mãi hợp lệ và yêu cầu cọc 10% qua VNPay. Sau khi cổng thanh toán phản hồi thành công, đơn được xác nhận và chuyển sang danh sách chờ nhận phòng.")

add_time("03:35-03:50", "Chuyển thành viên")
add_speech("Sau khi khách đặt phòng, dữ liệu được chuyển sang khối quản trị và vận hành. Em xin chuyển phần trình bày cho thành viên thứ hai.")

doc.add_page_break()
doc.add_heading("Thành viên 2  Quản trị và quản lý", level=1)
add_time("03:50-04:10", "Slide 11 và 12 - Quản lý phòng và người dùng")
add_speech("Khối quản trị quản lý phòng, loại phòng, dịch vụ, khách hàng, nhân viên, tài khoản, khuyến mãi, điểm và hạng thành viên. Dữ liệu phòng được gắn trạng thái và loại phòng để phục vụ tra cứu, nhận phòng và báo cáo.")

add_time("04:10-06:35", "Demo - Vai trò quản trị và quản lý")
add_demo([
    "Mở Quản lý phòng, dùng bộ lọc và xem một bản ghi để chứng minh dữ liệu thật từ MongoDB Atlas.",
    "Mở Loại phòng hoặc Dịch vụ, giới thiệu nhanh thao tác thêm, sửa, xóa và phân trang; không lặp lại toàn bộ CRUD.",
    "Mở Khuyến mãi, chỉ ra thời gian hiệu lực, trạng thái tự động và phạm vi công khai hoặc gửi riêng cho khách.",
    "Mở Quản lý tài khoản và phân quyền vai trò; nhấn mạnh Super Admin cấp quyền theo chức năng Web hoặc Mobile.",
    "Mở Thống kê doanh thu, chọn khoảng thời gian và giới thiệu chức năng xuất báo cáo.",
])
add_speech("Các màn hình quản lý dùng chung nguyên tắc tìm kiếm, lọc, phân trang và kiểm tra dữ liệu đầu vào. Trạng thái khuyến mãi thay đổi tự động theo ngày. Phân quyền được kiểm tra ở Backend nên việc ẩn menu không phải là lớp bảo vệ duy nhất. Báo cáo doanh thu được tổng hợp theo khoảng thời gian và có thể xuất phục vụ kế toán.")

add_time("06:35-07:10", "Slide 13 đến 15 - Kiểm thử")
add_speech("Nhóm kiểm thử các luồng chính gồm đặt phòng, thanh toán, nhận phòng, phân quyền, cập nhật trạng thái và xử lý dữ liệu không hợp lệ. Kết quả kiểm thử cho thấy các chức năng chính đáp ứng đúng điều kiện nghiệp vụ và thông báo lỗi được trả về tại giao diện.")

add_time("07:10-07:35", "Chuyển thành viên")
add_speech("Tiếp theo là luồng vận hành quan trọng nhất: xác thực khách, nhận phòng trên Mobile và đồng bộ kết quả về Web lễ tân.")

doc.add_page_break()
doc.add_heading("Thành viên 3  Nhận phòng và Mobile", level=1)
add_time("07:35-07:55", "Slide 10 - Làm thủ tục nhận phòng")
add_speech("Luồng nhận phòng gồm xác định phiếu đặt hôm nay, xác thực đủ số khách lưu trú, chọn phòng đúng loại đã đặt và duyệt nhận phòng. Mobile thực hiện xác thực; Web lễ tân theo dõi kết quả và hoàn tất nghiệp vụ.")

add_time("07:55-10:45", "Demo - Web lễ tân và Mobile")
add_demo([
    "Trên Web lễ tân, mở danh sách chờ nhận phòng và chọn phiếu vừa đặt; chỉ ra số khách cần xác thực và loại phòng đã đặt.",
    "Trên Mobile, mở Nhận phòng thông minh, chụp khuôn mặt và quét NFC mô phỏng hoặc ảnh CCCD mẫu. Với nhiều khách, lặp xác thực đến đủ số lượng.",
    "Giải thích ngoại lệ: khách nước ngoài hoặc xác thực thất bại thì lễ tân nhập thông tin thủ công có kiểm soát.",
    "Sau khi Mobile báo thành công, quay lại Web, chọn phòng trống đúng loại và duyệt nhận phòng.",
    "Mở Sơ đồ phòng để chứng minh phòng chuyển sang Đang ở theo thời gian đã chọn.",
])
add_speech("Mobile gửi ảnh và dữ liệu NFC đến API xác thực, sau đó Backend ghi nhận từng khách trong phiếu. Hệ thống chỉ cho duyệt khi đủ số người và phòng được chọn đúng loại đã đặt. Kết quả được đồng bộ về Web; trạng thái phòng thay đổi theo thời gian lưu trú. Nếu xác thực tự động không thực hiện được, hệ thống vẫn có luồng nhập tay dành cho ngoại lệ.")

add_time("10:45-11:05", "Giới thiệu nhanh chức năng Mobile liên quan")
add_speech("Ngoài luồng vừa demo, Mobile còn hỗ trợ sinh trắc học, tra cứu lịch sử đặt phòng và hóa đơn, đặt dịch vụ, cập nhật trạng thái buồng phòng, in hóa đơn nhiệt 58 mm và kiểm tra Root, mở khóa bộ nạp khởi động hoặc USB gỡ lỗi. Nhóm chỉ giới thiệu nhanh để đảm bảo thời gian.")

add_time("11:05-11:25", "Slide 16 - Kết luận và hướng phát triển")
add_speech("Hệ thống đã hoàn thiện hai nền tảng dùng chung dữ liệu, số hóa các nghiệp vụ chính của khách sạn và bổ sung AI hỗ trợ chatbot, dự đoán khách không đến cùng tự động hủy phòng theo quy tắc. Hướng phát triển là nâng độ tin cậy của xác thực CCCD thật và mở rộng tự động hóa vận hành.")

add_time("11:25-11:30", "Slide 17 và 18 - Tài liệu tham khảo và cảm ơn")
add_speech("Nhóm xin trân trọng cảm ơn quý Thầy Cô trong Hội đồng đã lắng nghe và sẵn sàng trả lời câu hỏi.")

doc.add_heading("Phần dự phòng khi còn thời gian", level=1)
add_bullet("AI dự đoán khách không đến: xem xác suất rủi ro, thời điểm hủy tự động và thao tác ghi nhận khách báo đến trễ.")
add_bullet("Đổi phòng: chọn phòng trống đúng điều kiện, tính chênh lệch và cập nhật phòng cũ sang chờ dọn.")
add_bullet("Trả phòng và buồng phòng: tạo lệnh dọn, Mobile cập nhật đã dọn hoặc bảo trì và Web nhận trạng thái mới.")
add_bullet("Nhân viên dịch vụ: tiếp nhận nhiều dịch vụ trên một hóa đơn và in nhiệt Bluetooth 58 mm.")
add_bullet("Chatbot AI: tư vấn loại phòng, tra cứu phòng trống và tạo liên kết đặt cọc.")
add_bullet("Chỉ mở từng chức năng từ 5 đến 10 giây; không thao tác nếu tổng thời gian đã vượt 11 phút 30 giây.")

doc.add_page_break()
doc.add_heading("Bảng đối chiếu chức năng chính theo Use Case", level=1)
table = doc.add_table(rows=1, cols=4)
for c, h in zip(table.rows[0].cells, ["Nhóm tác nhân", "Chức năng chính", "Có demo", "Chỉ giới thiệu"]): c.text = h
matrix = [
    ["Khách hàng", "Tra cứu phòng, đặt phòng trực tuyến, thanh toán/cọc, lịch sử đặt phòng", "Tra cứu và đặt phòng trực tuyến", "Lịch sử, đánh giá, điểm và hạng"],
    ["Quản trị và quản lý", "Quản lý phòng, loại phòng, dịch vụ, khách hàng, nhân viên, khuyến mãi, tài khoản", "Một số danh mục, khuyến mãi, phân quyền", "Các CRUD tương tự"],
    ["Lễ tân", "Đặt trực tiếp, nhận phòng, xác thực, đổi phòng, trả phòng, hóa đơn", "Nhận phòng và sơ đồ phòng", "Đặt trực tiếp, đổi/trả phòng"],
    ["Kế toán", "Thống kê doanh thu và xuất báo cáo", "Xem báo cáo", "Xuất tệp nếu thiếu thời gian"],
    ["Buồng phòng", "Nhận lệnh dọn, cập nhật phòng sạch hoặc bảo trì", "Không demo chính", "Giới thiệu dự phòng"],
    ["Nhân viên dịch vụ", "Tiếp nhận dịch vụ và in hóa đơn nhiệt", "Không demo chính", "Giới thiệu dự phòng"],
    ["AI và Mobile", "Chatbot, dự đoán khách không đến, NFC/ảnh CCCD, sinh trắc học, bảo vệ thiết bị", "Xác thực nhận phòng", "Các chức năng còn lại"],
]
for vals in matrix:
    cells = table.add_row().cells
    for c, v in zip(cells, vals): c.text = v
format_table(table, [3.1, 6.4, 3.6, 4.2])

doc.add_heading("Kịch bản xử lý sự cố khi demo", level=1)
table = doc.add_table(rows=1, cols=3)
for c, h in zip(table.rows[0].cells, ["Sự cố", "Cách xử lý ngay", "Câu nói chuyển tiếp"]): c.text = h
fallbacks = [
    ["VNPay hoặc mạng chậm", "Dừng chờ sau 10 giây, mở đơn mẫu đã thanh toán", "Đây là đơn đã nhận phản hồi thành công từ cổng thanh toán và được hệ thống chuyển sang chờ nhận phòng."],
    ["NFC không đọc", "Dùng thẻ mô phỏng hoặc ảnh CCCD mẫu; không quét lặp nhiều lần", "Trong môi trường nghiên cứu, nhóm dùng dữ liệu mô phỏng để trình bày cùng cấu trúc xác thực."],
    ["AI xác thực chậm", "Mở kết quả đã lưu hoặc dùng nhập tay ngoại lệ", "Hệ thống có cơ chế dự phòng cho khách nước ngoài hoặc trường hợp xác thực tự động thất bại."],
    ["Web chưa cập nhật", "Nhấn Làm mới một lần hoặc mở tab sơ đồ phòng dự phòng", "Backend đã lưu trạng thái; giao diện đang tải lại dữ liệu mới nhất."],
    ["Máy in chưa kết nối", "Dùng ảnh/video hóa đơn 58 mm đã chuẩn bị", "Đây là kết quả in thực tế từ ứng dụng qua Bluetooth."],
]
for vals in fallbacks:
    cells = table.add_row().cells
    for c, v in zip(cells, vals): c.text = v
format_table(table, [3.5, 5.5, 8.3])

for section in doc.sections:
    footer = section.footer
    p = footer.paragraphs[0]
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("Kịch bản thuyết trình May Hotel - tối đa 12 phút")
    r.font.name = "Arial"
    r.font.size = Pt(8)
    r.font.color.rgb = RGBColor.from_string(GRAY)

doc.save(OUT)
print(OUT)

$(document).ready(function () {
    // ==========================================
    // 1. PHẦN XỬ LÝ AJAX THÊM GIỎ HÀNG (Dành cho Java)
    // ==========================================
    $('.btn-add-cart').click(function (e) {
        e.preventDefault();
        var id = $(this).data('id');
        var btn = $(this);
        var originalText = btn.html();

        btn.html('<span class="spinner-border spinner-border-sm me-2 text-warning"></span>Đang xử lý...').prop('disabled', true);

        $.ajax({
            url: '/cart/add', // Sửa lại path theo Controller Java của bạn
            type: 'POST',
            data: { maPhong: id }, // Tên biến khớp với @RequestParam trong Java
            success: function (result) {
                // Giả sử Java trả về JSON: { "status": true, "soLuong": 5 }
                if (result.status == true) {
                    $('#cart-quantity').text(result.soLuong);
                    btn.html(originalText).prop('disabled', false);

                    var toastEl = document.getElementById("toastSuccessAdd");
                    if (toastEl) {
                        $('#toastMessage').text("Đã thêm phòng vào danh sách chọn!");
                        var toast = new bootstrap.Toast(toastEl);
                        toast.show();
                    }
                }
            },
            error: function () {
                btn.html(originalText).prop('disabled', false);
                alert('Có lỗi xảy ra, vui lòng thử lại.');
            }
        });
    });

    // --- PHẦN THÔNG BÁO TỪ SERVER ---
    // Lưu ý: Java không dùng TempData. Bạn sẽ dùng URL Parameter hoặc Header.
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('success')) {
        var toastEl = document.getElementById("toastSuccessAdd");
        if (toastEl) {
            $('#toastMessage').text("Thao tác thành công!");
            var toast = new bootstrap.Toast(toastEl);
            toast.show();
        }
    }

    // ==========================================
    // 2. PHẦN BỔ SUNG: XỬ LÝ CHATBOT (PHOBERT)
    // ==========================================
    $("#chat-circle").click(function() {
        $(this).hide();
        $("#chat-box").fadeIn().css("display", "flex");
    });

    $("#chat-box-close").click(function() {
        $("#chat-box").fadeOut(function() {
            $("#chat-circle").fadeIn();
        });
    });

    function sendMessage() {
        var msg = $("#chat-input").val();
        if(msg.trim() == "") return;

        $("#chat-logs").append('<div class="msg-user">' + msg + '</div>');
        $("#chat-input").val('');
        scrollToBottom();

        var loadingId = "bot-loading";
        $("#chat-logs").append('<div id="' + loadingId + '" class="msg-bot text-muted fst-italic">Đang suy nghĩ...</div>');
        scrollToBottom();

        // Cập nhật: Gửi kèm user_id để giữ luồng đặt phòng
        $.ajax({
            url: "http://127.0.0.1:5000/predict",
            type: "POST",
            contentType: "application/json",
            data: JSON.stringify({
                message: msg,
                user_id: "user_123" // Bạn có thể lấy ID từ Session của Java
            }),
            success: function(res) {
                $("#" + loadingId).remove();
                var botReply = res.answer.replace(/\n/g, "<br>");
                $("#chat-logs").append('<div class="msg-bot">' + botReply + '</div>');
                scrollToBottom();
            },
            error: function() {
                $("#" + loadingId).remove();
                $("#chat-logs").append('<div class="msg-bot text-danger">Lỗi: Server AI chưa chạy!</div>');
                scrollToBottom();
            }
        });
    }

    $("#chat-submit").click(sendMessage);
    $("#chat-input").keypress(function(e) {
        if(e.which == 13) sendMessage();
    });

    function scrollToBottom() {
        var chatLogs = document.getElementById("chat-logs");
        if(chatLogs) chatLogs.scrollTop = chatLogs.scrollHeight;
    }
});
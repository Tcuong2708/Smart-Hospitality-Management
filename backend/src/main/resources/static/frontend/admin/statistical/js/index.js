const API_URL = 'http://localhost:8080/api/statistics'; // Replace with actual backend endpoint

document.addEventListener('DOMContentLoaded', () => {
    // Set current year in title immediately
    const currentYear = new Date().getFullYear();
    document.getElementById('current-year').textContent = currentYear;
    
    fetchStatisticsData(currentYear);
});

async function fetchStatisticsData(year) {
    try {
        const mockData = {
            nam: year,
            tongDoanhThu: 1545000000,
            tongSoDon: 432,
            trungBinhThang: 128750000,
            doanhThuArr: [85000000, 92000000, 115000000, 140000000, 165000000, 180000000, 195000000, 160000000, 130000000, 110000000, 95000000, 78000000],
            soDonArr: [25, 28, 35, 42, 50, 55, 60, 48, 38, 30, 26, 22]
        };

        renderDashboard(mockData);
    } catch (error) {
        console.error('Error fetching statistics:', error);
        document.getElementById('loading-spinner').style.display = 'none';
        document.getElementById('error-message').style.display = 'block';
    }
}

function renderDashboard(data) {
    document.getElementById('loading-spinner').style.display = 'none';
    document.getElementById('dashboard-content').style.display = 'block';

    // Format numbers
    const formatCurrency = (value) => new Intl.NumberFormat('vi-VN').format(value || 0) + ' đ';
    
    document.getElementById('total-revenue').textContent = formatCurrency(data.tongDoanhThu);
    document.getElementById('total-orders').textContent = (data.tongSoDon || 0) + ' đơn';
    document.getElementById('average-month').textContent = formatCurrency(data.trungBinhThang);

    // Render Chart
    renderChart(data.doanhThuArr || Array(12).fill(0), data.soDonArr || Array(12).fill(0));
}

function renderChart(revenueData, orderData) {
    const ctxElement = document.getElementById('myChart');
    if (!ctxElement) return;

    new Chart(ctxElement, {
        data: {
            labels: ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'],
            datasets: [
                {
                    type: 'bar',
                    label: 'Doanh thu (VNĐ)',
                    data: revenueData,
                    backgroundColor: 'rgba(15, 41, 66, 0.85)',
                    borderColor: '#0F2942',
                    borderWidth: 1,
                    borderRadius: 4,
                    yAxisID: 'y'
                },
                {
                    type: 'line',
                    label: 'Số đơn đặt',
                    data: orderData,
                    borderColor: '#C5A017',
                    backgroundColor: '#C5A017',
                    borderWidth: 3,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    tension: 0.15,
                    yAxisID: 'y1'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false
            },
            plugins: {
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            let labelText = context.dataset.label || '';
                            if (labelText) { labelText += ': '; }
                            if (context.datasetIndex === 0) {
                                labelText += new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(context.parsed.y);
                            } else {
                                labelText += context.parsed.y + ' đơn';
                            }
                            return labelText;
                        }
                    }
                }
            },
            scales: {
                y: {
                    type: 'linear',
                    display: true,
                    position: 'left',
                    beginAtZero: true,
                    title: { display: true, text: 'Doanh thu (VNĐ)', font: { weight: 'bold' } },
                    ticks: {
                        callback: function(value) {
                            return new Intl.NumberFormat('vi-VN', { notation: 'compact' }).format(value) + ' đ';
                        }
                    }
                },
                y1: {
                    type: 'linear',
                    display: true,
                    position: 'right',
                    beginAtZero: true,
                    grid: { drawOnChartArea: false },
                    title: { display: true, text: 'Số lượng đơn đặt', font: { weight: 'bold' } },
                    ticks: {
                        stepSize: 1,
                        callback: function(value) { return value + ' đơn'; }
                    }
                }
            }
        }
    });
}

/**
 * Volcano Arts Center - Premium Admin Charts Initialization
 * Uses Chart.js to render animated, styled charts across dashboards.
 */

document.addEventListener('DOMContentLoaded', () => {
    // Shared Theme Configuration
    const chartTheme = {
        fontFamily: "'DM Sans', sans-serif",
        textColor: 'rgba(255, 255, 255, 0.6)',
        gridColor: 'rgba(255, 255, 255, 0.05)',
        colors: {
            primary: '#CBA86A',   // var(--vac-gold)
            secondary: '#2E7D32', // var(--vac-green-700)
            accent: '#00A651'     // var(--vac-green)
        }
    };

    // Global Chart.js Defaults
    Chart.defaults.font.family = chartTheme.fontFamily;
    Chart.defaults.color = chartTheme.textColor;
    Chart.defaults.plugins.tooltip.backgroundColor = 'rgba(10, 20, 15, 0.95)';
    Chart.defaults.plugins.tooltip.titleFont = { family: "'Syne', sans-serif", size: 14, weight: 'bold' };
    Chart.defaults.plugins.tooltip.bodyFont = { family: "'DM Sans', sans-serif", size: 13 };
    Chart.defaults.plugins.tooltip.padding = 12;
    Chart.defaults.plugins.tooltip.borderColor = 'rgba(203, 168, 106, 0.3)';
    Chart.defaults.plugins.tooltip.borderWidth = 1;

    // 1. Super Admin: Revenue Analytics (Line Chart)
    const revenueCtx = document.getElementById('revenueChart');
    if (revenueCtx) {
        new Chart(revenueCtx, {
            type: 'line',
            data: {
                labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'],
                datasets: [{
                    label: 'Platform Revenue ($)',
                    data: [12500, 15200, 14800, 18900, 22100, 24500, 23000, 26800, 31000, 29500, 34200, 38500],
                    borderColor: chartTheme.colors.primary,
                    backgroundColor: (context) => {
                        const ctx = context.chart.ctx;
                        const gradient = ctx.createLinearGradient(0, 0, 0, 300);
                        gradient.addColorStop(0, 'rgba(203, 168, 106, 0.3)');
                        gradient.addColorStop(1, 'rgba(203, 168, 106, 0.0)');
                        return gradient;
                    },
                    borderWidth: 3,
                    tension: 0.4,
                    fill: true,
                    pointBackgroundColor: '#fff',
                    pointBorderColor: chartTheme.colors.primary,
                    pointBorderWidth: 2,
                    pointRadius: 4,
                    pointHoverRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    x: { grid: { display: false, drawBorder: false } },
                    y: { 
                        grid: { color: chartTheme.gridColor, drawBorder: false },
                        ticks: { callback: (value) => '$' + (value/1000) + 'k' }
                    }
                },
                animation: {
                    y: { duration: 2000, easing: 'easeOutQuart' }
                }
            }
        });
    }

    // 2. Super Admin: Department Activity (Bar Chart)
    const deptCtx = document.getElementById('departmentChart');
    if (deptCtx) {
        new Chart(deptCtx, {
            type: 'bar',
            data: {
                labels: ['Tours', 'Art Store', 'Donations'],
                datasets: [{
                    label: 'Volume',
                    data: [420, 850, 150],
                    backgroundColor: [
                        chartTheme.colors.secondary,
                        chartTheme.colors.primary,
                        chartTheme.colors.accent
                    ],
                    borderRadius: 6,
                    borderWidth: 0,
                    barThickness: 32
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { grid: { display: false, drawBorder: false } },
                    y: { grid: { color: chartTheme.gridColor, drawBorder: false } }
                },
                animation: {
                    y: { duration: 1500, delay: 300, easing: 'easeOutQuart' }
                }
            }
        });
    }

    // 3. Ops Manager: Fulfillment Pipeline (Doughnut)
    const opsCtx = document.getElementById('fulfillmentChart');
    if (opsCtx) {
        new Chart(opsCtx, {
            type: 'doughnut',
            data: {
                labels: ['Pending', 'Processing', 'Shipped', 'Delivered'],
                datasets: [{
                    data: [15, 30, 45, 10],
                    backgroundColor: [
                        'rgba(255, 255, 255, 0.2)',
                        chartTheme.colors.primary,
                        chartTheme.colors.accent,
                        chartTheme.colors.secondary
                    ],
                    borderWidth: 0,
                    hoverOffset: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '75%',
                plugins: {
                    legend: { position: 'bottom', labels: { usePointStyle: true, padding: 20 } }
                },
                animation: { animateScale: true, animateRotate: true, duration: 2000 }
            }
        });
    }
    
    // 4. Content Manager: Content Engagement (Bar/Line Combo)
    const contentCtx = document.getElementById('contentEngagementChart');
    if (contentCtx) {
        new Chart(contentCtx, {
            type: 'line',
            data: {
                labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
                datasets: [
                    {
                        type: 'bar',
                        label: 'New Content Published',
                        data: [12, 19, 8, 15],
                        backgroundColor: chartTheme.colors.secondary,
                        borderRadius: 4,
                        order: 2
                    },
                    {
                        type: 'line',
                        label: 'Platform Views',
                        data: [5000, 7500, 6800, 9200],
                        borderColor: chartTheme.colors.primary,
                        borderWidth: 3,
                        tension: 0.4,
                        order: 1,
                        yAxisID: 'y1'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'top' }
                },
                scales: {
                    x: { grid: { display: false } },
                    y: { grid: { color: chartTheme.gridColor } },
                    y1: {
                        position: 'right',
                        grid: { display: false },
                        ticks: { callback: (value) => (value/1000) + 'k' }
                    }
                }
            }
        });
    }
});

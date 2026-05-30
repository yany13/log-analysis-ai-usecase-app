// Analytics JavaScript
document.addEventListener('DOMContentLoaded', function() {
    loadAnalytics();
    setInterval(loadAnalytics, 5000); // Refresh every 5 seconds
});

function loadAnalytics() {
    fetch('/bytebite/api/analytics?action=stats')
        .then(response => response.json())
        .then(data => {
            displayAnalytics(data);
        })
        .catch(error => console.error('Error loading analytics:', error));
}

function displayAnalytics(data) {
    // Update stat cards
    document.getElementById('totalEvents').textContent = Object.values(data).reduce((a, b) => a + b, 0) || '0';
    
    // Sample data - in production, this would come from the API
    const queueStats = {
        queueSize: Math.floor(Math.random() * 50),
        batchSize: 100,
    };
    
    document.getElementById('queueSize').textContent = queueStats.queueSize;
    document.getElementById('batchSize').textContent = queueStats.batchSize;
    
    // Update charts
    updateEventChart(data);
    updateHealthChart();
}

function updateEventChart(data) {
    const chart = document.getElementById('eventChart');
    const entries = Object.entries(data);
    
    if (entries.length === 0) {
        chart.innerHTML = '<p style="text-align: center; color: #999;">No events yet</p>';
        return;
    }

    let html = '<table class="table" style="margin: 0;">';
    html += '<thead><tr><th>Event Type</th><th>Count</th></tr></thead><tbody>';
    
    entries.forEach(([type, count]) => {
        html += `<tr><td>${type}</td><td>${count}</td></tr>`;
    });
    
    html += '</tbody></table>';
    chart.innerHTML = html;
}

function updateHealthChart() {
    const chart = document.getElementById('healthChart');
    
    fetch('/bytebite/health')
        .then(response => response.json())
        .then(data => {
            let html = '<div style="padding: 20px;">';
            html += `<p><strong>Status:</strong> <span style="color: ${data.status === 'UP' ? '#5cdb6f' : '#ff6b6b'};">${data.status}</span></p>`;
            html += `<p><strong>Database:</strong> <span style="color: ${data.database.status === 'UP' ? '#5cdb6f' : '#ff6b6b'};">${data.database.status}</span></p>`;
            html += `<p><strong>LDAP:</strong> <span style="color: ${data.ldap.status === 'UP' ? '#5cdb6f' : '#ffd93d'};">${data.ldap.status}</span></p>`;
            html += '</div>';
            chart.innerHTML = html;
        })
        .catch(error => {
            console.error('Health check failed:', error);
            chart.innerHTML = '<p style="color: #999; text-align: center;">Health check unavailable</p>';
        });
}

// KDS (Kitchen Display System) JavaScript
document.addEventListener('DOMContentLoaded', function() {
    loadOrders();
    setInterval(loadOrders, 3000); // Refresh every 3 seconds
});

let orders = {};

function loadOrders() {
    fetch('/bytebite/api/kds?action=orders')
        .then(response => response.json())
        .then(data => {
            displayOrders(data);
        })
        .catch(error => console.error('Error loading orders:', error));
    
    loadStats();
}

function loadStats() {
    fetch('/bytebite/api/kds?action=stats')
        .then(response => response.json())
        .then(data => {
            document.getElementById('totalOrders').textContent = data.totalOrders || '0';
            document.getElementById('newOrders').textContent = data.newOrders || '0';
            document.getElementById('inProgress').textContent = data.inProgress || '0';
            document.getElementById('readyOrders').textContent = data.ready || '0';
        })
        .catch(error => console.error('Error loading stats:', error));
}

function displayOrders(orderList) {
    const grid = document.getElementById('kdsGrid');
    
    if (!orderList || orderList.length === 0) {
        grid.innerHTML = '<p style="grid-column: 1/-1; text-align: center; color: #999; padding: 40px;">No active orders</p>';
        return;
    }

    grid.innerHTML = '';
    orderList.forEach(order => {
        const ticket = createOrderTicket(order);
        grid.appendChild(ticket);
    });
}

function createOrderTicket(order) {
    const ticket = document.createElement('div');
    ticket.className = `order-ticket ${getOrderClass(order.priority)}`;
    
    const timeAgo = getTimeAgo(order.createdAt);
    const statusBadge = getStatusBadge(order.status);
    
    ticket.innerHTML = `
        <div class="order-number">${order.orderNumber}</div>
        <div class="order-time">${timeAgo}</div>
        <div class="order-items">
            ${(order.items || []).map(item => `
                <div class="order-item">
                    <span class="qty">${item.quantity}x</span> ${item.name}
                </div>
            `).join('')}
        </div>
        <div class="order-status">
            <span class="status-badge ${getStatusClass(order.status)}">${order.status}</span>
        </div>
        <div class="order-actions">
            ${getActionButtons(order)}
        </div>
    `;
    
    return ticket;
}

function getOrderClass(priority) {
    switch(priority) {
        case 'high': return 'priority-high';
        case 'medium': return 'priority-medium';
        default: return '';
    }
}

function getStatusBadge(status) {
    const badges = {
        'NEW': '<span class="status-badge status-new">NEW</span>',
        'IN_PROGRESS': '<span class="status-badge status-in-progress">IN PROGRESS</span>',
        'READY': '<span class="status-badge status-ready">READY</span>'
    };
    return badges[status] || status;
}

function getStatusClass(status) {
    switch(status) {
        case 'NEW': return 'status-new';
        case 'IN_PROGRESS': return 'status-in-progress';
        case 'READY': return 'status-ready';
        default: return '';
    }
}

function getActionButtons(order) {
    let buttons = '';
    
    if (order.status === 'NEW') {
        buttons += `<button class="btn-small btn-start" onclick="updateStatus('${order.orderId}', 'IN_PROGRESS')">Start</button>`;
    }
    
    if (order.status === 'IN_PROGRESS' || order.status === 'NEW') {
        buttons += `<button class="btn-small btn-ready" onclick="updateStatus('${order.orderId}', 'READY')">Ready</button>`;
    }
    
    return buttons;
}

function updateStatus(orderId, newStatus) {
    fetch('/bytebite/api/kds', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            orderId: orderId,
            status: newStatus
        })
    })
    .then(response => response.json())
    .then(result => {
        loadOrders();
    })
    .catch(error => console.error('Error updating order:', error));
}

function getTimeAgo(timestamp) {
    if (!timestamp) return 'Just now';
    
    const date = new Date(timestamp);
    const now = new Date();
    const seconds = Math.floor((now - date) / 1000);
    
    if (seconds < 60) return `${seconds}s ago`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    return `${Math.floor(seconds / 3600)}h ago`;
}

window.updateStatus = updateStatus;

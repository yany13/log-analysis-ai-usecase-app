// Menu JavaScript
document.addEventListener('DOMContentLoaded', function() {
    loadMenu();
    loadCart();
    setupCartUpdater();
});

let cart = [];

function loadMenu() {
    fetch('/bytebite/api/menu')
        .then(response => response.json())
        .then(data => {
            displayMenu(data);
        })
        .catch(error => {
            console.error('Error loading menu:', error);
            document.getElementById('menuGrid').innerHTML = 
                '<p style="grid-column: 1/-1; text-align: center; color: #999;">Failed to load menu</p>';
        });
}

function displayMenu(items) {
    const grid = document.getElementById('menuGrid');
    grid.innerHTML = '';

    items.forEach(item => {
        const itemElement = document.createElement('div');
        itemElement.className = 'menu-item';
        itemElement.innerHTML = `
            <div class="menu-item-image">
                ${getEmoji(item.name)}
            </div>
            <div class="menu-item-content">
                <div class="menu-item-name">${escapeHtml(item.name)}</div>
                <div class="menu-item-description">
                    Fresh and delicious ${item.name.toLowerCase()}
                </div>
                <div class="menu-item-price">$${item.price.toFixed(2)}</div>
                <div class="menu-item-actions">
                    <button class="btn btn-add" onclick="addToCart(${item.id}, '${escapeHtml(item.name)}', ${item.price})">
                        Add to Cart
                    </button>
                </div>
            </div>
        `;
        grid.appendChild(itemElement);
    });
}

function addToCart(id, name, price) {
    const existingItem = cart.find(item => item.id === id);
    
    if (existingItem) {
        existingItem.quantity++;
    } else {
        cart.push({
            id: id,
            name: name,
            price: price,
            quantity: 1
        });
    }
    
    updateCart();
    showNotification(`${name} added to cart!`);
}

function removeFromCart(id) {
    cart = cart.filter(item => item.id !== id);
    updateCart();
}

function updateCart() {
    localStorage.setItem('cart', JSON.stringify(cart));
    updateCartCount();
}

function loadCart() {
    const saved = localStorage.getItem('cart');
    if (saved) {
        cart = JSON.parse(saved);
        updateCartCount();
    }
}

function setupCartUpdater() {
    // Update cart count every time it changes
    setInterval(updateCartCount, 1000);
}

function updateCartCount() {
    const count = cart.reduce((sum, item) => sum + item.quantity, 0);
    const badge = document.getElementById('cartCount');
    if (badge) {
        badge.textContent = count;
    }
}

function getEmoji(name) {
    const emojis = {
        'Pizza': '🍕',
        'Burger': '🍔',
        'Pasta': '🍝',
        'Salad': '🥗',
        'Dessert': '🍰',
        'Drink': '🥤'
    };
    return emojis[name] || '🍽️';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showNotification(message) {
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #5cdb6f;
        color: white;
        padding: 15px 20px;
        border-radius: 5px;
        box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        z-index: 1000;
        animation: slideIn 0.3s ease;
    `;
    notification.textContent = message;
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Add CSS for animations
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from { transform: translateX(400px); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOut {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(400px); opacity: 0; }
    }
`;
document.head.appendChild(style);

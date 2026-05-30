// Checkout JavaScript
let cartData = [];

document.addEventListener('DOMContentLoaded', function() {
    loadCart();
    setupForm();
});

function loadCart() {
    const saved = localStorage.getItem('cart');
    if (saved) {
        cartData = JSON.parse(saved);
        updateSummary();
    }
}

function setupForm() {
    const form = document.getElementById('checkoutForm');
    form.addEventListener('submit', handleCheckout);
}

function updateSummary() {
    const subtotal = cartData.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    const tax = subtotal * 0.1;
    const shipping = 5.00;
    const total = subtotal + tax + shipping;

    document.getElementById('subtotal').textContent = '$' + subtotal.toFixed(2);
    document.getElementById('tax').textContent = '$' + tax.toFixed(2);
    document.getElementById('total').textContent = '$' + total.toFixed(2);
}

function handleCheckout(e) {
    e.preventDefault();

    if (cartData.length === 0) {
        showAlert('Your cart is empty', 'warning');
        window.location.href = '/bytebite/menu';
        return;
    }

    const formData = new FormData(e.target);
    const data = {
        fullName: formData.get('fullName'),
        email: formData.get('email'),
        address: formData.get('address'),
        city: formData.get('city'),
        zipCode: formData.get('zipCode'),
        paymentMethod: formData.get('paymentMethod'),
        notes: formData.get('notes'),
        items: cartData,
        totalAmount: parseFloat(document.getElementById('total').textContent.substring(1))
    };

    // Track event
    trackEvent('CHECKOUT', {
        itemCount: cartData.length,
        total: data.totalAmount
    });

    fetch('/bytebite/api/orders', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
    .then(response => response.json())
    .then(result => {
        if (result.success) {
            localStorage.removeItem('cart');
            showAlert('Order placed successfully!', 'success');
            setTimeout(() => {
                window.location.href = '/bytebite/menu';
            }, 2000);
        } else {
            showAlert('Failed to place order: ' + result.error, 'danger');
        }
    })
    .catch(error => {
        console.error('Checkout error:', error);
        showAlert('Checkout failed: ' + error.message, 'danger');
    });
}

function trackEvent(eventType, eventData) {
    fetch('/bytebite/api/analytics', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            eventType: eventType,
            ...eventData
        })
    }).catch(err => console.error('Event tracking failed:', err));
}

function showAlert(message, type) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;
    alertDiv.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 1000; min-width: 300px;';
    document.body.appendChild(alertDiv);
    
    setTimeout(() => alertDiv.remove(), 5000);
}

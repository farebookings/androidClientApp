<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Taxi App Driver — Admin Panel</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100">
    <nav class="bg-blue-700 text-white px-6 py-4 shadow-lg">
        <div class="max-w-7xl mx-auto flex justify-between items-center">
            <h1 class="text-2xl font-bold">🚖 Taxi Admin</h1>
            <button onclick="logout()" class="bg-red-500 px-4 py-2 rounded hover:bg-red-600">Logout</button>
        </div>
    </nav>

    <div class="max-w-7xl mx-auto p-6">
        <!-- Filtros -->
        <div class="bg-white rounded-lg shadow p-4 mb-6 flex gap-4">
            <select id="filterType" class="border rounded px-3 py-2" onchange="loadBookings()">
                <option value="all">All bookings</option>
                <option value="immediate">Immediate</option>
                <option value="scheduled">Scheduled</option>
            </select>
            <select id="filterStatus" class="border rounded px-3 py-2" onchange="loadBookings()">
                <option value="all">All status</option>
                <option value="pending">Pending</option>
                <option value="confirmed">Confirmed</option>
                <option value="in_progress">In Progress</option>
                <option value="completed">Completed</option>
                <option value="cancelled">Cancelled</option>
            </select>
        </div>

        <!-- Stats -->
        <div class="grid grid-cols-4 gap-4 mb-6" id="stats"></div>

        <!-- Tabla de reservas -->
        <div class="bg-white rounded-lg shadow overflow-hidden">
            <div class="px-6 py-4 border-b bg-gray-50">
                <h2 class="text-lg font-semibold">📋 Booking Management</h2>
            </div>
            <div class="overflow-x-auto">
                <table class="w-full text-sm">
                    <thead class="bg-gray-100">
                        <tr>
                            <th class="px-4 py-3 text-left">ID</th>
                            <th class="px-4 py-3 text-left">Client</th>
                            <th class="px-4 py-3 text-left">Type</th>
                            <th class="px-4 py-3 text-left">Pickup</th>
                            <th class="px-4 py-3 text-left">Status</th>
                            <th class="px-4 py-3 text-left">Driver</th>
                            <th class="px-4 py-3 text-left">Fare</th>
                            <th class="px-4 py-3 text-left">Date</th>
                            <th class="px-4 py-3 text-center">Actions</th>
                        </tr>
                    </thead>
                    <tbody id="bookingsTable" class="divide-y"></tbody>
                </table>
            </div>
        </div>
    </div>

    <script>
        const API = '/api';
        let token = localStorage.getItem('adminToken');

        async function api(path, options = {}) {
            const res = await fetch(API + path, {
                ...options,
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token,
                    ...(options.headers || {}),
                },
            });
            return res.json();
        }

        function logout() {
            localStorage.removeItem('adminToken');
            showLogin();
        }

        function showLogin() {
            document.querySelector('nav').style.display = 'none';
            document.querySelector('.max-w-7xl').innerHTML = `
                <div class="max-w-md mx-auto mt-20 bg-white rounded-xl shadow-lg p-8">
                    <h2 class="text-2xl font-bold text-center mb-6">🔐 Admin Login</h2>
                    <input id="email" type="email" placeholder="Email" class="w-full border rounded px-4 py-3 mb-3">
                    <input id="password" type="password" placeholder="Password" class="w-full border rounded px-4 py-3 mb-6">
                    <button onclick="login()" class="w-full bg-blue-700 text-white py-3 rounded-lg hover:bg-blue-800">
                        Login
                    </button>
                    <p id="loginError" class="text-red-500 mt-3 text-center hidden"></p>
                </div>
            `;
        }

        async function login() {
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const res = await fetch(API + '/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password }),
            });
            const data = await res.json();

            if (data.token) {
                token = data.token;
                localStorage.setItem('adminToken', token);
                init();
            } else {
                document.getElementById('loginError').textContent = data.error || 'Login failed';
                document.getElementById('loginError').classList.remove('hidden');
            }
        }

        function statusBadge(status) {
            const colors = {
                pending: 'bg-yellow-100 text-yellow-800',
                confirmed: 'bg-blue-100 text-blue-800',
                in_progress: 'bg-green-100 text-green-800',
                completed: 'bg-gray-100 text-gray-600',
                cancelled: 'bg-red-100 text-red-800',
            };
            return `<span class="px-2 py-1 rounded text-xs font-medium ${colors[status] || 'bg-gray-100'}">${status}</span>`;
        }

        async function loadBookings() {
            const type = document.getElementById('filterType').value;
            const status = document.getElementById('filterStatus').value;

            let path = '/admin/bookings';
            const params = new URLSearchParams();
            if (type !== 'all') params.set('type', type);
            if (status !== 'all') params.set('status', status);
            if (params.toString()) path += '?' + params.toString();

            const data = await api(path);
            const tbody = document.getElementById('bookingsTable');

            if (!data.bookings || data.bookings.length === 0) {
                tbody.innerHTML = '<tr><td colspan="9" class="px-4 py-8 text-center text-gray-400">No bookings found</td></tr>';
                return;
            }

            tbody.innerHTML = data.bookings.map(b => `
                <tr class="hover:bg-gray-50">
                    <td class="px-4 py-3 font-mono">#${b.id}</td>
                    <td class="px-4 py-3">
                        <div class="font-medium">${b.client_name}</div>
                        <div class="text-xs text-gray-500">${b.client_phone || ''}</div>
                    </td>
                    <td class="px-4 py-3">
                        <span class="px-2 py-1 rounded text-xs font-medium ${b.type === 'immediate' ? 'bg-purple-100 text-purple-800' : 'bg-indigo-100 text-indigo-800'}">
                            ${b.type}
                        </span>
                    </td>
                    <td class="px-4 py-3 max-w-xs truncate" title="${b.pickup_address}">${b.pickup_address}</td>
                    <td class="px-4 py-3">${statusBadge(b.status)}</td>
                    <td class="px-4 py-3">${b.driver_name || '<span class="text-gray-400">—</span>'}</td>
                    <td class="px-4 py-3 font-medium">${b.fare ? b.fare + '€' : '—'}</td>
                    <td class="px-4 py-3 text-xs">${new Date(b.created_at).toLocaleString()}</td>
                    <td class="px-4 py-3 text-center">
                        <select onchange="updateBookingStatus(${b.id}, this.value)" class="text-xs border rounded px-2 py-1">
                            <option value="pending" ${b.status === 'pending' ? 'selected' : ''}>Pending</option>
                            <option value="confirmed" ${b.status === 'confirmed' ? 'selected' : ''}>Confirm</option>
                            <option value="in_progress" ${b.status === 'in_progress' ? 'selected' : ''}>In Progress</option>
                            <option value="completed" ${b.status === 'completed' ? 'selected' : ''}>Complete</option>
                            <option value="cancelled" ${b.status === 'cancelled' ? 'selected' : ''}>Cancel</option>
                        </select>
                    </td>
                </tr>
            `).join('');
        }

        async function updateBookingStatus(id, status) {
            await api(`/bookings/${id}/status`, {
                method: 'PATCH',
                body: JSON.stringify({ status }),
            });
        }

        async function loadStats() {
            const data = await api('/admin/stats');
            if (data.stats) {
                document.getElementById('stats').innerHTML = Object.entries(data.stats).map(([key, val]) => `
                    <div class="bg-white rounded-lg shadow p-4 text-center">
                        <div class="text-2xl font-bold text-blue-700">${val}</div>
                        <div class="text-sm text-gray-500 capitalize">${key}</div>
                    </div>
                `).join('');
            }
        }

        async function init() {
            document.querySelector('nav').style.display = 'flex';
            // Check if admin
            const profile = await api('/auth/profile');
            if (profile.user && profile.user.role === 'admin') {
                loadStats();
                loadBookings();
                // Refresh every 30s
                setInterval(loadBookings, 30000);
            } else {
                alert('Admin access required');
                logout();
            }
        }

        // Start
        if (token) {
            init();
        } else {
            showLogin();
        }
    </script>
</body>
</html>

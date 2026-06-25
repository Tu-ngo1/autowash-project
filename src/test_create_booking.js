const http = require('http');

function postJSON(path, payload, token = null) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify(payload);
    const headers = {
      'Content-Type': 'application/json',
      'Content-Length': data.length
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const options = {
      hostname: 'localhost',
      port: 8080,
      path: path,
      method: 'POST',
      headers: headers
    };

    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        resolve({ statusCode: res.statusCode, body });
      });
    });

    req.on('error', reject);
    req.write(data);
    req.end();
  });
}

async function run() {
  try {
    // 1. Login
    console.log('Logging in...');
    const loginRes = await postJSON('/api/auth/login', {
      usernameOrPhone: 'customer01',
      password: 'password123'
    });
    
    console.log(`Login status: ${loginRes.statusCode}`);
    if (loginRes.statusCode !== 200) {
      console.log('Login failed:', loginRes.body);
      return;
    }

    const token = JSON.parse(loginRes.body).token;
    console.log('Obtained fresh token successfully.');

    // 2. Create Booking
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const dateString = tomorrow.toISOString().split('T')[0];
    const scheduledStartTime = `${dateString}T10:30:00`;

    const bookingPayload = {
      vehicleId: 1, // vehicle ID 1 from seed data
      scheduledStartTime: scheduledStartTime,
      serviceIds: [1], // Standard wash
      customerNote: "Rửa sạch mâm xe giúp tôi.",
      paymentMethod: "WALLET",
      voucherCode: null
    };

    console.log('Sending create booking request...');
    const bookingRes = await postJSON('/api/customer/bookings', bookingPayload, token);
    console.log(`Booking status: ${bookingRes.statusCode}`);
    console.log('Booking response:', bookingRes.body);

  } catch (error) {
    console.error('Error:', error);
  }
}

run();

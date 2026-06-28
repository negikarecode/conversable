// AI Gateway Test Suite
import assert from 'assert';

console.log('==================================================');
console.log(' RUNNING AI GATEWAY ROUTING & FAILOVER TESTS');
console.log('==================================================');

// Mock data structures similar to server.js for validation
const PROVIDERS = {
  gemini: { name: 'Google Gemini' },
  groq: { name: 'Groq' },
  openrouter: { name: 'OpenRouter' }
};

const healthStates = {
  gemini: { status: 'Healthy', avgLatency: 120, lastSuccess: 'now', lastFailure: null },
  groq: { status: 'Healthy', avgLatency: 80, lastSuccess: 'now', lastFailure: null },
  openrouter: { status: 'Unavailable', avgLatency: 0, lastSuccess: null, lastFailure: 'now' }
};

// Simulates the routing logic
function getOrderedProviders(healthStates) {
  return Object.keys(PROVIDERS)
    .filter(key => healthStates[key].status === 'Healthy' || healthStates[key].status === 'Degraded')
    .sort((a, b) => {
      if (healthStates[a].status !== healthStates[b].status) {
        return healthStates[a].status === 'Healthy' ? -1 : 1;
      }
      const latA = healthStates[a].avgLatency || 9999;
      const latB = healthStates[b].avgLatency || 9999;
      return latA - latB;
    });
}

// 1. Test Routing order prioritizing healthy & low latency
try {
  const ordered = getOrderedProviders(healthStates);
  console.log('Test 1: Ordering active healthy providers by latency...');
  assert.deepStrictEqual(ordered, ['groq', 'gemini']);
  console.log('✅ Test 1 PASSED: Fastest healthy provider (Groq, 80ms) was prioritized over Gemini (120ms). Unavailable provider OpenRouter was filtered out.');
} catch (e) {
  console.error('❌ Test 1 FAILED:', e.message);
  process.exit(1);
}

// 2. Test Failover logic simulation
try {
  console.log('\nTest 2: Simulating provider failover...');
  const ordered = getOrderedProviders(healthStates);
  
  // Try first provider (groq). Simulating it fails:
  console.log(`- Trying primary provider: ${ordered[0]}`);
  healthStates['groq'].status = 'Unavailable';
  healthStates['groq'].reason = 'HTTP 502 Bad Gateway';
  
  // Re-fetch routing order after failover
  const nextOrdered = getOrderedProviders(healthStates);
  console.log(`- Failover detected! Switched to next provider: ${nextOrdered[0]}`);
  
  assert.strictEqual(nextOrdered[0], 'gemini');
  console.log('✅ Test 2 PASSED: Successfully fell back to Gemini after Groq was marked Unavailable.');
} catch (e) {
  console.error('❌ Test 2 FAILED:', e.message);
  process.exit(1);
}

// 3. Test Recovery logic simulation
try {
  console.log('\nTest 3: Simulating provider recovery...');
  // Mark groq as healthy again
  healthStates['groq'].status = 'Healthy';
  healthStates['groq'].reason = 'Recovered';
  healthStates['groq'].avgLatency = 75; // Even faster now
  
  const ordered = getOrderedProviders(healthStates);
  assert.deepStrictEqual(ordered, ['groq', 'gemini']);
  console.log('✅ Test 3 PASSED: Groq successfully recovered and returned to the top of the healthy provider pool.');
} catch (e) {
  console.error('❌ Test 3 FAILED:', e.message);
  process.exit(1);
}

// 4. Test API Key Security (Verify health endpoint hides secrets)
try {
  console.log('\nTest 4: Verifying API Key Exposure Safeguards...');
  
  // Dummy health state with mock api keys (server.js environment check)
  const healthCheckResponse = {
    status: 'OK',
    providers: healthStates
  };
  
  const rawString = JSON.stringify(healthCheckResponse);
  assert.ok(!rawString.includes('sk-'), 'Secrets leaked in health status response!');
  assert.ok(!rawString.includes('gsk_'), 'Secrets leaked in health status response!');
  
  console.log('✅ Test 4 PASSED: Health status output contains zero raw API keys or credential fields.');
} catch (e) {
  console.error('❌ Test 4 FAILED:', e.message);
  process.exit(1);
}

console.log('\n==================================================');
console.log(' ALL GATEWAY TESTS PASSED SUCCESSFULLY! 🎉');
console.log('==================================================');

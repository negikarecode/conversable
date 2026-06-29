import { serve } from '@hono/node-server';
import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { logger } from 'hono/logger';

const app = new Hono();

app.use('*', cors());
app.use('*', logger());

// Providers Configuration
const PROVIDERS = {
  gemini: {
    name: 'Google Gemini',
    endpoint: 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent',
    apiKeyName: 'GEMINI_API_KEY',
    testPayload: {
      contents: [{ parts: [{ text: 'Hello' }] }]
    },
    parseResponse: (data) => {
      return data.candidates?.[0]?.content?.parts?.[0]?.text;
    },
    formatPayload: (systemPrompt, messages, temperature, maxTokens, forceJson, images) => {
      const contents = [];
      if (systemPrompt) {
        contents.push({ role: 'user', parts: [{ text: `System instruction: ${systemPrompt}` }] });
      }
      
      const userParts = [];
      messages.forEach(msg => {
        userParts.push({ text: msg.content });
      });
      
      if (images && images.length > 0) {
        images.forEach(img => {
          userParts.push({
            inlineData: {
              mimeType: img.mimeType || 'image/jpeg',
              data: img.data
            }
          });
        });
      }
      
      contents.push({
        role: 'user',
        parts: userParts
      });
      
      const config = {
        temperature: temperature || 0.7,
        maxOutputTokens: maxTokens || 150
      };
      if (forceJson) {
        config.responseMimeType = 'application/json';
      }
      
      return {
        contents,
        generationConfig: config
      };
    }
  },
  groq: {
    name: 'Groq',
    endpoint: 'https://api.groq.com/openai/v1/chat/completions',
    apiKeyName: 'GROQ_API_KEY',
    testPayload: {
      model: 'llama-3.3-70b-versatile',
      messages: [{ role: 'user', content: 'Hello' }],
      max_tokens: 5
    },
    parseResponse: (data) => {
      return data.choices?.[0]?.message?.content;
    },
    formatPayload: (systemPrompt, messages, temperature, maxTokens, forceJson) => {
      const formattedMessages = [];
      if (systemPrompt) {
        formattedMessages.push({ role: 'system', content: systemPrompt });
      }
      messages.forEach(msg => {
        formattedMessages.push({ role: msg.role, content: msg.content });
      });
      const payload = {
        model: 'llama-3.3-70b-versatile',
        messages: formattedMessages,
        temperature: temperature || 0.7,
        max_tokens: maxTokens || 150
      };
      if (forceJson) {
        payload.response_format = { type: 'json_object' };
      }
      return payload;
    }
  },
  openrouter: {
    name: 'OpenRouter',
    endpoint: 'https://openrouter.ai/api/v1/chat/completions',
    apiKeyName: 'OPENROUTER_API_KEY',
    testPayload: {
      model: 'meta-llama/llama-3.3-70b-instruct',
      messages: [{ role: 'user', content: 'Hello' }],
      max_tokens: 5
    },
    parseResponse: (data) => {
      return data.choices?.[0]?.message?.content;
    },
    formatPayload: (systemPrompt, messages, temperature, maxTokens) => {
      const formattedMessages = [];
      if (systemPrompt) {
        formattedMessages.push({ role: 'system', content: systemPrompt });
      }
      messages.forEach(msg => {
        formattedMessages.push({ role: msg.role, content: msg.content });
      });
      return {
        model: 'meta-llama/llama-3.3-70b-instruct',
        messages: formattedMessages,
        temperature: temperature || 0.7,
        max_tokens: maxTokens || 150
      };
    }
  },
  mistral: {
    name: 'Mistral',
    endpoint: 'https://api.mistral.ai/v1/chat/completions',
    apiKeyName: 'MISTRAL_API_KEY',
    testPayload: {
      model: 'mistral-large-latest',
      messages: [{ role: 'user', content: 'Hello' }],
      max_tokens: 5
    },
    parseResponse: (data) => {
      return data.choices?.[0]?.message?.content;
    },
    formatPayload: (systemPrompt, messages, temperature, maxTokens) => {
      const formattedMessages = [];
      if (systemPrompt) {
        formattedMessages.push({ role: 'system', content: systemPrompt });
      }
      messages.forEach(msg => {
        formattedMessages.push({ role: msg.role, content: msg.content });
      });
      return {
        model: 'mistral-large-latest',
        messages: formattedMessages,
        temperature: temperature || 0.7,
        max_tokens: maxTokens || 150
      };
    }
  },
  cerebras: {
    name: 'Cerebras',
    endpoint: 'https://api.cerebras.ai/v1/chat/completions',
    apiKeyName: 'CEREBRAS_API_KEY',
    testPayload: {
      model: 'gpt-oss-120b',
      messages: [{ role: 'user', content: 'Hello' }],
      max_tokens: 50
    },
    parseResponse: (data) => {
      return data.choices?.[0]?.message?.content || data.choices?.[0]?.message?.reasoning;
    },
    formatPayload: (systemPrompt, messages, temperature, maxTokens) => {
      const formattedMessages = [];
      if (systemPrompt) {
        formattedMessages.push({ role: 'system', content: systemPrompt });
      }
      messages.forEach(msg => {
        formattedMessages.push({ role: msg.role, content: msg.content });
      });
      return {
        model: 'gpt-oss-120b',
        messages: formattedMessages,
        temperature: temperature || 0.7,
        max_tokens: maxTokens || 150
      };
    }
  }
};

// Check if a provider has a valid key configured in process.env
function isProviderConfigured(key) {
  const config = PROVIDERS[key];
  const apiKey = process.env[config.apiKeyName];
  return apiKey && apiKey !== '' && !apiKey.includes('PLACEHOLDER') && !apiKey.includes('YOUR_') && !apiKey.includes('MY_');
}

// Health States tracking - Initialize configured providers as Healthy (awaiting verification)
// to prevent cold start routing blackouts.
const healthStates = {};
for (const key of Object.keys(PROVIDERS)) {
  const isConfigured = isProviderConfigured(key);
  healthStates[key] = {
    status: isConfigured ? 'Healthy' : 'Unavailable',
    reason: isConfigured ? 'Configured (Awaiting verification)' : `API Key ${PROVIDERS[key].apiKeyName} is missing or placeholder`,
    lastSuccess: null,
    lastFailure: null,
    avgLatency: isConfigured ? 1000 : 0,
    requestCount: 0,
    failureCount: 0
  };
}

// Update rolling latency
function updateLatency(key, latency) {
  const state = healthStates[key];
  state.requestCount += 1;
  if (state.avgLatency === 0) {
    state.avgLatency = latency;
  } else {
    state.avgLatency = Math.round((state.avgLatency * 9 + latency) / 10);
  }
}

// Friendly HTTP Error Parsers
function getFriendlyError(status, errText) {
  let message = errText;
  try {
    const parsed = JSON.parse(errText);
    message = parsed.error?.message || parsed.message || errText;
  } catch (e) {
    // Not JSON
  }

  const messageLower = message.toLowerCase();

  if (status === 401 || messageLower.includes('api key not valid') || messageLower.includes('invalid api key') || messageLower.includes('invalid_api_key') || messageLower.includes('unauthorized')) {
    return 'Invalid API key';
  }
  if (status === 403 || messageLower.includes('permission') || messageLower.includes('forbidden')) {
    return 'Missing permissions';
  }
  if (status === 404 || messageLower.includes('not found') || messageLower.includes('endpoint') || messageLower.includes('no route')) {
    return 'Incorrect endpoint';
  }
  if (status === 429 || messageLower.includes('rate limit') || messageLower.includes('quota') || messageLower.includes('too many requests')) {
    return 'Rate limited';
  }
  if (status >= 500) {
    return 'Provider internal server error';
  }
  return `HTTP ${status}: ${message.substring(0, 100)}`;
}

// Test Provider
async function testProvider(key) {
  const config = PROVIDERS[key];
  
  if (!isProviderConfigured(key)) {
    healthStates[key].status = 'Unavailable';
    healthStates[key].reason = `API Key ${config.apiKeyName} is missing or placeholder`;
    return false;
  }

  const apiKey = process.env[config.apiKeyName];
  const startTime = Date.now();
  try {
    let url = config.endpoint;
    const headers = { 'Content-Type': 'application/json' };
    
    if (key === 'gemini') {
      url += `?key=${apiKey}`;
    } else {
      headers['Authorization'] = `Bearer ${apiKey}`;
      if (key === 'openrouter') {
        headers['HTTP-Referer'] = 'https://conversable.app';
        headers['X-Title'] = 'Conversable';
      }
    }

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000); // 5s timeout for health validation

    const response = await fetch(url, {
      method: 'POST',
      headers,
      body: JSON.stringify(config.testPayload),
      signal: controller.signal
    });
    
    clearTimeout(timeoutId);
    const latency = Date.now() - startTime;

    if (response.ok) {
      const data = await response.json();
      const testResult = config.parseResponse(data);
      if (testResult) {
        healthStates[key].status = 'Healthy';
        healthStates[key].reason = 'Verification succeeded';
        healthStates[key].lastSuccess = new Date().toISOString();
        updateLatency(key, latency);
        return true;
      } else {
        healthStates[key].status = 'Degraded';
        healthStates[key].reason = 'Succeeded but failed to parse response content';
        healthStates[key].lastFailure = new Date().toISOString();
        return false;
      }
    } else {
      const errText = await response.text().catch(() => '');
      const friendlyErr = getFriendlyError(response.status, errText);
      healthStates[key].status = 'Unavailable';
      healthStates[key].reason = friendlyErr;
      healthStates[key].lastFailure = new Date().toISOString();
      return false;
    }
  } catch (err) {
    let reason = 'Network error';
    if (err.name === 'AbortError') {
      reason = 'Timeout';
    } else {
      reason = `Network error: ${err.message}`;
    }
    healthStates[key].status = 'Unavailable';
    healthStates[key].reason = reason;
    healthStates[key].lastFailure = new Date().toISOString();
    return false;
  }
}

// Send real test prompt to every healthy provider to verify AI response in parallel
async function verifyRealAiResponses() {
  console.log('\n==================================================');
  console.log(' RUNNING REAL AI RESPONSE VERIFICATION TESTS IN PARALLEL');
  console.log('==================================================');
  
  const promises = Object.keys(PROVIDERS).map(async (key) => {
    if (healthStates[key].status !== 'Healthy') {
      console.log(`- Skipping ${PROVIDERS[key].name} (not healthy / missing key)`);
      return;
    }
    
    console.log(`Testing real AI response from ${PROVIDERS[key].name}...`);
    try {
      const testMsg = [{ role: 'user', content: 'Reply with: Conversable AI is working.' }];
      const config = PROVIDERS[key];
      const apiKey = process.env[config.apiKeyName];
      let url = config.endpoint;
      const headers = { 'Content-Type': 'application/json' };
      if (key === 'gemini') {
        url += `?key=${apiKey}`;
      } else {
        headers['Authorization'] = `Bearer ${apiKey}`;
        if (key === 'openrouter') {
          headers['HTTP-Referer'] = 'https://conversable.app';
          headers['X-Title'] = 'Conversable';
        }
      }
      
      const payload = config.formatPayload(null, testMsg, 0.1, 50);
      
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 8000); // 8s timeout for real AI response
      
      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
        signal: controller.signal
      });
      
      clearTimeout(timeoutId);
      
      if (response.ok) {
        const data = await response.json();
        const text = config.parseResponse(data);
        if (text && text.toLowerCase().includes('conversable ai is working')) {
          console.log(`✓ ${config.name} returned valid response: "${text.trim()}"`);
          healthStates[key].status = 'Healthy';
          healthStates[key].reason = 'Verification succeeded & AI response verified';
          healthStates[key].lastSuccess = new Date().toISOString();
        } else if (text) {
          console.log(`⚠️ ${config.name} returned response but did not match prompt: "${text.trim()}"`);
          healthStates[key].status = 'Degraded';
          healthStates[key].reason = `AI returned invalid response contents: "${text.trim()}"`;
          healthStates[key].lastFailure = new Date().toISOString();
        } else {
          console.log(`✗ ${config.name} returned empty text`);
          healthStates[key].status = 'Degraded';
          healthStates[key].reason = 'AI returned empty response';
          healthStates[key].lastFailure = new Date().toISOString();
        }
      } else {
        const errText = await response.text().catch(() => '');
        const friendlyErr = getFriendlyError(response.status, errText);
        console.log(`✗ ${config.name} failed AI prompt verification: ${friendlyErr}`);
        healthStates[key].status = 'Unavailable';
        healthStates[key].reason = `Verification call failed: ${friendlyErr}`;
        healthStates[key].lastFailure = new Date().toISOString();
      }
    } catch (err) {
      const reason = err.name === 'AbortError' ? 'Timeout' : `Network error: ${err.message}`;
      console.log(`✗ ${config.name} failed during test request: ${reason}`);
      healthStates[key].status = 'Unavailable';
      healthStates[key].reason = `Verification call error: ${reason}`;
      healthStates[key].lastFailure = new Date().toISOString();
    }
  });

  await Promise.all(promises);
  console.log('==================================================\n');
}

// Startup checks - run in parallel
async function runAllHealthChecks() {
  // 1. Output Startup Configuration Report
  console.log('==================================================');
  console.log(' AI GATEWAY STARTUP CONFIGURATION REPORT');
  console.log('==================================================');
  for (const key of Object.keys(PROVIDERS)) {
    const displayName = key === 'openrouter' ? 'OpenRouter' : (key.charAt(0).toUpperCase() + key.slice(1));
    if (isProviderConfigured(key)) {
      console.log(`✓ ${displayName} configured`);
    } else {
      console.log(`✗ ${displayName} missing API key`);
    }
  }
  console.log('==================================================\n');

  // 2. Perform Validation Requests in Parallel
  console.log('[AI Gateway] Running startup validation health checks in parallel...');
  await Promise.all(Object.keys(PROVIDERS).map(async (key) => {
    await testProvider(key);
    console.log(`[AI Gateway] ${PROVIDERS[key].name}: ${healthStates[key].status} (${healthStates[key].reason})`);
  }));

  // 3. Verify Real AI Output Responses
  await verifyRealAiResponses();
}

// Background scheduler - run in parallel and more frequently to recover quickly
setInterval(async () => {
  console.log('[AI Gateway] Running periodic health checks in parallel...');
  await Promise.all(Object.keys(PROVIDERS).map(key => testProvider(key)));
}, 60 * 1000); // 1 min

// Intelligent routing & failover
async function routeChatRequest(systemPrompt, messages, temperature, maxTokens, forceJson, images) {
  let orderedProviders = Object.keys(PROVIDERS)
    .filter(key => healthStates[key].status === 'Healthy' || healthStates[key].status === 'Degraded')
    .sort((a, b) => {
      if (healthStates[a].status !== healthStates[b].status) {
        return healthStates[a].status === 'Healthy' ? -1 : 1;
      }
      const latA = healthStates[a].avgLatency || 9999;
      const latB = healthStates[b].avgLatency || 9999;
      return latA - latB;
    });

  // If there are images, we MUST use a vision-capable provider (Google Gemini)
  if (images && images.length > 0) {
    orderedProviders = ['gemini'].filter(key => healthStates[key].status === 'Healthy' || healthStates[key].status === 'Degraded');
    if (orderedProviders.length === 0) {
      orderedProviders = ['gemini']; // Force try gemini if it is the only vision provider
    }
  }

  if (orderedProviders.length === 0) {
    throw new Error('All configured AI providers are currently unavailable.');
  }

  let lastError = null;
  const maxTries = Math.min(orderedProviders.length, 3);
  
  for (let attempt = 0; attempt < maxTries; attempt++) {
    const providerKey = orderedProviders[attempt];
    const config = PROVIDERS[providerKey];
    const apiKey = process.env[config.apiKeyName];
    
    console.log(`[AI Gateway] Routing to ${config.name} (Attempt ${attempt + 1}/${maxTries})...`);
    const startTime = Date.now();
    
    try {
      let url = config.endpoint;
      const headers = { 'Content-Type': 'application/json' };
      
      if (providerKey === 'gemini') {
        url += `?key=${apiKey}`;
      } else {
        headers['Authorization'] = `Bearer ${apiKey}`;
        if (providerKey === 'openrouter') {
          headers['HTTP-Referer'] = 'https://conversable.app';
          headers['X-Title'] = 'Conversable';
        }
      }

      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 6000); // 6s request timeout for fast failover

      const payload = config.formatPayload(systemPrompt, messages, temperature, maxTokens, forceJson, images);

      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload),
        signal: controller.signal
      });
      
      clearTimeout(timeoutId);
      const latency = Date.now() - startTime;

      if (response.ok) {
        const data = await response.json();
        const text = config.parseResponse(data);
        if (text) {
          healthStates[providerKey].status = 'Healthy';
          healthStates[providerKey].lastSuccess = new Date().toISOString();
          updateLatency(providerKey, latency);
          return { text, provider: config.name, providerKey, latency };
        } else {
          throw new Error('Failed to parse text from provider response');
        }
      } else {
        const errText = await response.text().catch(() => '');
        throw new Error(`HTTP ${response.status}: ${errText.substring(0, 150)}`);
      }
    } catch (err) {
      console.error(`[AI Gateway] ${config.name} failed: ${err.message}`);
      
      // Update health state
      healthStates[providerKey].status = 'Unavailable';
      healthStates[providerKey].reason = err.message;
      healthStates[providerKey].lastFailure = new Date().toISOString();
      healthStates[providerKey].failureCount += 1;
      
      lastError = err;
    }
  }
  
  throw new Error(`AI request failed after trying ${maxTries} provider(s). Last error: ${lastError?.message}`);
}

// JSON Health API
app.get('/api/health', (c) => {
  const orderedProviders = Object.keys(PROVIDERS)
    .filter(key => healthStates[key].status === 'Healthy' || healthStates[key].status === 'Degraded')
    .sort((a, b) => {
      if (healthStates[a].status !== healthStates[b].status) {
        return healthStates[a].status === 'Healthy' ? -1 : 1;
      }
      const latA = healthStates[a].avgLatency || 9999;
      const latB = healthStates[b].avgLatency || 9999;
      return latA - latB;
    });

  const configured = Object.keys(PROVIDERS).filter(key => isProviderConfigured(key));
  const missing = Object.keys(PROVIDERS).filter(key => !isProviderConfigured(key));

  return c.json({
    status: Object.values(healthStates).some(p => p.status === 'Healthy') ? 'OK' : 'Degraded',
    activeProvider: orderedProviders.length > 0 ? PROVIDERS[orderedProviders[0]].name : 'None',
    configuredProviders: configured.map(k => PROVIDERS[k].name),
    missingProviders: missing.map(k => PROVIDERS[k].name),
    timestamp: new Date().toISOString(),
    providers: healthStates
  });
});

// Run Manual Diagnostics Endpoint
app.post('/api/health/test', async (c) => {
  console.log('[AI Gateway] Manual health diagnostics triggered...');
  for (const key of Object.keys(PROVIDERS)) {
    await testProvider(key);
  }
  await verifyRealAiResponses();

  const orderedProviders = Object.keys(PROVIDERS)
    .filter(key => healthStates[key].status === 'Healthy' || healthStates[key].status === 'Degraded')
    .sort((a, b) => {
      if (healthStates[a].status !== healthStates[b].status) {
        return healthStates[a].status === 'Healthy' ? -1 : 1;
      }
      const latA = healthStates[a].avgLatency || 9999;
      const latB = healthStates[b].avgLatency || 9999;
      return latA - latB;
    });

  const configured = Object.keys(PROVIDERS).filter(key => isProviderConfigured(key));
  const missing = Object.keys(PROVIDERS).filter(key => !isProviderConfigured(key));

  return c.json({
    status: Object.values(healthStates).some(p => p.status === 'Healthy') ? 'OK' : 'Degraded',
    activeProvider: orderedProviders.length > 0 ? PROVIDERS[orderedProviders[0]].name : 'None',
    configuredProviders: configured.map(k => PROVIDERS[k].name),
    missingProviders: missing.map(k => PROVIDERS[k].name),
    timestamp: new Date().toISOString(),
    providers: healthStates
  });
});

// HTML Dashboard
app.get('/health', (c) => {
  const orderedProviders = Object.keys(PROVIDERS)
    .filter(key => healthStates[key].status === 'Healthy' || healthStates[key].status === 'Degraded')
    .sort((a, b) => {
      if (healthStates[a].status !== healthStates[b].status) {
        return healthStates[a].status === 'Healthy' ? -1 : 1;
      }
      const latA = healthStates[a].avgLatency || 9999;
      const latB = healthStates[b].avgLatency || 9999;
      return latA - latB;
    });

  const activeProviderName = orderedProviders.length > 0 ? PROVIDERS[orderedProviders[0]].name : 'None (All offline)';
  const configuredCount = Object.keys(PROVIDERS).filter(k => isProviderConfigured(k)).length;
  const missingCount = Object.keys(PROVIDERS).filter(k => !isProviderConfigured(k)).length;
  const gatewayStatus = Object.values(healthStates).some(p => p.status === 'Healthy') ? 'ONLINE' : 'DEGRADED';
  const gatewayStatusClass = gatewayStatus.toLowerCase();

  const providerRows = Object.entries(healthStates).map(([key, state]) => {
    const config = PROVIDERS[key];
    const statusClass = state.status.toLowerCase();
    const isConfigured = isProviderConfigured(key);
    const lastSuccessStr = state.lastSuccess ? new Date(state.lastSuccess).toLocaleTimeString() : 'Never';
    const lastFailureStr = state.lastFailure ? new Date(state.lastFailure).toLocaleTimeString() : 'Never';
    const latencyStr = state.avgLatency > 0 ? `${state.avgLatency}ms` : 'N/A';
    const isActive = orderedProviders[0] === key;
    
    return `
      <div id="card-${key}" class="provider-card ${statusClass} ${isActive ? 'active-provider-card' : ''}">
        <div class="card-header">
          <div class="header-name-group">
            <span class="provider-name">${config.name}</span>
            ${isActive ? '<span class="active-badge"><i data-lucide="zap"></i> Active Router</span>' : ''}
          </div>
          <span id="status-${key}" class="status-pill status-${statusClass}">${state.status}</span>
        </div>
        <div class="card-body">
          <div class="config-line">
            <strong>Key Config:</strong> 
            <span class="code-span">${config.apiKeyName}</span>
            ${isConfigured ? '<span class="status-dot healthy">✓</span>' : '<span class="status-dot unavailable">✗</span>'}
          </div>
          <p><strong>Latency (rolling):</strong> <span id="latency-${key}" class="metric">${latencyStr}</span></p>
          <p><strong>Total Requests:</strong> <span id="requests-${key}" class="metric">${state.requestCount}</span></p>
          <p><strong>Failures:</strong> <span id="failures-${key}" class="metric">${state.failureCount}</span></p>
          <p><strong>Last Success:</strong> <span id="success-${key}">${lastSuccessStr}</span></p>
          <p><strong>Last Failure:</strong> <span id="failure-${key}">${lastFailureStr}</span></p>
          <div id="reason-container-${key}" class="reason-text" style="${state.reason ? '' : 'display: none;'}">
            <strong>Status Details:</strong> <span id="reason-${key}">${state.reason}</span>
          </div>
        </div>
      </div>
    `;
  }).join('');

  const html = `
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Conversable AI Gateway Dashboard</title>
      <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&display=swap" rel="stylesheet">
      <script src="https://unpkg.com/lucide@latest"></script>
      <style>
        :root {
          --bg-color: #08090d;
          --panel-bg: rgba(16, 18, 27, 0.85);
          --border-color: rgba(255, 255, 255, 0.06);
          --text-primary: #f3f4f6;
          --text-secondary: #9ca3af;
          --healthy-color: #10b981;
          --healthy-glow: rgba(16, 185, 129, 0.12);
          --degraded-color: #f59e0b;
          --degraded-glow: rgba(245, 158, 11, 0.12);
          --unavailable-color: #ef4444;
          --unavailable-glow: rgba(239, 68, 68, 0.12);
          --accent-gradient: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
          --card-radius: 16px;
        }
        body {
          margin: 0;
          padding: 0;
          background-color: var(--bg-color);
          color: var(--text-primary);
          font-family: 'Outfit', sans-serif;
          background-image: 
            radial-gradient(circle at 10% 20%, rgba(99, 102, 241, 0.08) 0%, transparent 40%),
            radial-gradient(circle at 90% 80%, rgba(16, 185, 129, 0.05) 0%, transparent 40%);
          min-height: 100vh;
        }
        .container {
          max-width: 1200px;
          margin: 0 auto;
          padding: 40px 20px;
        }
        header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 30px;
          border-bottom: 1px solid var(--border-color);
          padding-bottom: 24px;
        }
        .header-left h1 {
          font-size: 2.4rem;
          font-weight: 800;
          margin: 0;
          letter-spacing: -0.02em;
          background: linear-gradient(135deg, #ffffff 30%, #c7d2fe 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }
        .subtitle {
          color: var(--text-secondary);
          margin-top: 6px;
          font-size: 1.05rem;
          font-weight: 300;
          display: flex;
          align-items: center;
          gap: 6px;
        }
        .controls-group {
          display: flex;
          gap: 12px;
          align-items: center;
        }
        .back-btn {
          background: rgba(255, 255, 255, 0.04);
          color: var(--text-primary);
          border: 1px solid var(--border-color);
          padding: 12px 20px;
          border-radius: 30px;
          font-family: inherit;
          font-weight: 600;
          text-decoration: none;
          font-size: 0.95rem;
          display: flex;
          align-items: center;
          gap: 8px;
          transition: all 0.2s ease;
        }
        .back-btn:hover {
          background: rgba(255, 255, 255, 0.08);
          transform: translateY(-1px);
        }
        .diagnostics-btn {
          background: var(--accent-gradient);
          color: white;
          border: none;
          padding: 12px 24px;
          border-radius: 30px;
          font-family: 'Outfit', sans-serif;
          font-weight: 600;
          font-size: 0.95rem;
          cursor: pointer;
          display: flex;
          align-items: center;
          gap: 8px;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          box-shadow: 0 4px 14px rgba(79, 70, 229, 0.3);
        }
        .diagnostics-btn:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(79, 70, 229, 0.5);
        }
        .diagnostics-btn:disabled {
          background: rgba(255, 255, 255, 0.1);
          color: var(--text-secondary);
          cursor: not-allowed;
          box-shadow: none;
          transform: none;
        }
        
        /* Summary Banner */
        .summary-banner {
          background: var(--panel-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--card-radius);
          padding: 24px;
          margin-bottom: 30px;
          backdrop-filter: blur(16px);
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
          gap: 24px;
          box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
        }
        .summary-card {
          display: flex;
          flex-direction: column;
        }
        .summary-label {
          font-size: 0.85rem;
          text-transform: uppercase;
          letter-spacing: 0.05em;
          color: var(--text-secondary);
          margin-bottom: 8px;
          font-weight: 600;
        }
        .summary-val {
          font-size: 1.4rem;
          font-weight: 700;
          color: var(--text-primary);
          display: flex;
          align-items: center;
          gap: 8px;
        }
        
        /* Grid Layout */
        .grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(340px, 1fr));
          gap: 24px;
        }
        
        /* Provider Cards */
        .provider-card {
          background: var(--panel-bg);
          border: 1px solid var(--border-color);
          border-radius: var(--card-radius);
          padding: 24px;
          backdrop-filter: blur(12px);
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          position: relative;
          overflow: hidden;
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
        }
        .provider-card::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          width: 4px;
          height: 100%;
          background: transparent;
          transition: background-color 0.3s;
        }
        .provider-card:hover {
          transform: translateY(-4px);
          box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
        }
        
        /* Provider Status Coloring */
        .provider-card.healthy {
          border-color: rgba(16, 185, 129, 0.15);
        }
        .provider-card.healthy::before {
          background-color: var(--healthy-color);
        }
        .provider-card.degraded {
          border-color: rgba(245, 158, 11, 0.15);
        }
        .provider-card.degraded::before {
          background-color: var(--degraded-color);
        }
        .provider-card.unavailable {
          border-color: rgba(239, 68, 68, 0.15);
        }
        .provider-card.unavailable::before {
          background-color: var(--unavailable-color);
        }
        
        .active-provider-card {
          border-color: rgba(99, 102, 241, 0.35) !important;
          box-shadow: 0 10px 30px rgba(99, 102, 241, 0.1) !important;
        }
        
        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 16px;
        }
        .header-name-group {
          display: flex;
          flex-direction: column;
          gap: 4px;
        }
        .provider-name {
          font-size: 1.35rem;
          font-weight: 600;
          letter-spacing: -0.01em;
        }
        .active-badge {
          font-size: 0.75rem;
          font-weight: 600;
          background: rgba(99, 102, 241, 0.15);
          color: #a5b4fc;
          padding: 2px 8px;
          border-radius: 12px;
          display: inline-flex;
          align-items: center;
          gap: 4px;
          width: fit-content;
          border: 1px solid rgba(99, 102, 241, 0.2);
        }
        .active-badge svg {
          width: 10px;
          height: 10px;
          fill: currentColor;
        }
        
        .status-pill {
          padding: 5px 12px;
          border-radius: 20px;
          font-size: 0.75rem;
          font-weight: 600;
          letter-spacing: 0.05em;
          text-transform: uppercase;
          border: 1px solid transparent;
        }
        .status-healthy {
          background-color: rgba(16, 185, 129, 0.1);
          color: var(--healthy-color);
          border-color: rgba(16, 185, 129, 0.2);
        }
        .status-degraded {
          background-color: rgba(245, 158, 11, 0.1);
          color: var(--degraded-color);
          border-color: rgba(245, 158, 11, 0.2);
        }
        .status-unavailable {
          background-color: rgba(239, 68, 68, 0.1);
          color: var(--unavailable-color);
          border-color: rgba(239, 68, 68, 0.2);
        }
        
        .card-body p {
          margin: 8px 0;
          font-size: 0.92rem;
          color: var(--text-secondary);
        }
        .config-line {
          margin-bottom: 14px;
          font-size: 0.9rem;
          color: var(--text-secondary);
          display: flex;
          align-items: center;
          gap: 6px;
        }
        .code-span {
          font-family: monospace;
          background: rgba(255, 255, 255, 0.05);
          padding: 2px 6px;
          border-radius: 4px;
          color: var(--text-primary);
        }
        .status-dot {
          font-weight: bold;
          margin-left: 2px;
        }
        .status-dot.healthy {
          color: var(--healthy-color);
        }
        .status-dot.unavailable {
          color: var(--unavailable-color);
        }
        .metric {
          color: var(--text-primary);
          font-weight: 600;
        }
        
        .reason-text {
          font-size: 0.85rem;
          background-color: rgba(0, 0, 0, 0.25);
          border: 1px solid rgba(255, 255, 255, 0.04);
          padding: 10px 14px;
          border-radius: 8px;
          margin-top: 14px;
          word-break: break-all;
          color: var(--text-secondary);
          line-height: 1.4;
        }
        .reason-text strong {
          color: var(--text-primary);
        }
        
        /* Spinner */
        .spinner {
          width: 14px;
          height: 14px;
          border: 2.5px solid rgba(255, 255, 255, 0.3);
          border-radius: 50%;
          border-top-color: white;
          animation: spin 0.8s linear infinite;
          display: none;
        }
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      </style>
    </head>
    <body>
      <div class="container">
        <header>
          <div class="header-left">
            <h1>Conversable AI Gateway</h1>
            <div class="subtitle"><i data-lucide="activity"></i> Multi-Provider Intelligent Routing & Failover System</div>
          </div>
          <div class="controls-group">
            <a href="/index.html" class="back-btn"><i data-lucide="arrow-left"></i> Landing Page</a>
            <button id="run-diagnostics-btn" class="diagnostics-btn" onclick="runDiagnostics()">
              <span class="spinner" id="btn-spinner"></span>
              <span id="btn-text">Run Diagnostics</span>
            </button>
          </div>
        </header>
        
        <!-- Summary Banner -->
        <div class="summary-banner">
          <div class="summary-card">
            <span class="summary-label">Gateway Status</span>
            <span id="summary-overall-status" class="summary-val">
              <span class="status-pill status-${gatewayStatusClass}">${gatewayStatus}</span>
            </span>
          </div>
          <div class="summary-card">
            <span class="summary-label">Active Provider Target</span>
            <span id="summary-active-provider" class="summary-val" style="color: #c7d2fe;">
              ${activeProviderName}
            </span>
          </div>
          <div class="summary-card">
            <span class="summary-label">Providers Synthesis</span>
            <span class="summary-val">
              <span id="summary-configured-count" style="color: var(--healthy-color);">${configuredCount}</span>
              <span style="color: var(--text-secondary); font-size: 1rem; font-weight: 400; margin: 0 4px;">/</span>
              <span id="summary-total-count" style="color: var(--text-secondary); font-size: 1.1rem; font-weight: 400;">5</span>
              <span style="font-size: 0.85rem; color: var(--text-secondary); font-weight: 400; margin-left: 6px;">Configured</span>
            </span>
          </div>
        </div>

        <div class="grid" id="providers-grid">
          ${providerRows}
        </div>
      </div>

      <script>
        // Initialize Lucide Icons
        lucide.createIcons();

        async function runDiagnostics() {
          const btn = document.getElementById('run-diagnostics-btn');
          const btnText = document.getElementById('btn-text');
          const spinner = document.getElementById('btn-spinner');
          
          btn.disabled = true;
          btnText.textContent = 'Running diagnostics...';
          spinner.style.display = 'inline-block';
          
          try {
            const res = await fetch('/api/health/test', { method: 'POST' });
            if (!res.ok) throw new Error('Diagnostics execution failed.');
            const data = await res.json();
            
            // Dynamically update UI
            updateDashboardUI(data);
          } catch (err) {
            alert('Diagnostics failed: ' + err.message);
          } finally {
            btn.disabled = false;
            btnText.textContent = 'Run Diagnostics';
            spinner.style.display = 'none';
          }
        }

        function updateDashboardUI(data) {
          // 1. Update overall status
          const gatewayStatusVal = data.status === 'OK' ? 'ONLINE' : 'DEGRADED';
          const gatewayStatusClass = gatewayStatusVal.toLowerCase();
          document.getElementById('summary-overall-status').innerHTML = 
            \`<span class="status-pill status-\${gatewayStatusClass}">\${gatewayStatusVal}</span>\`;
          
          // 2. Update active provider
          document.getElementById('summary-active-provider').textContent = data.activeProvider;
          
          // 3. Update configured provider counts
          document.getElementById('summary-configured-count').textContent = data.configuredProviders.length;

          // 4. Update each provider card
          Object.entries(data.providers).forEach(([key, state]) => {
            const card = document.getElementById('card-' + key);
            const statusPill = document.getElementById('status-' + key);
            const latencyVal = document.getElementById('latency-' + key);
            const requestsVal = document.getElementById('requests-' + key);
            const failuresVal = document.getElementById('failures-' + key);
            const successVal = document.getElementById('success-' + key);
            const failureVal = document.getElementById('failure-' + key);
            const reasonContainer = document.getElementById('reason-container-' + key);
            const reasonVal = document.getElementById('reason-' + key);

            // Update classes for status colorings
            card.className = 'provider-card ' + state.status.toLowerCase();
            if (data.activeProvider === (key === 'gemini' ? 'Google Gemini' : (key === 'openrouter' ? 'OpenRouter' : key.charAt(0).toUpperCase() + key.slice(1)))) {
              card.classList.add('active-provider-card');
            }

            statusPill.className = 'status-pill status-' + state.status.toLowerCase();
            statusPill.textContent = state.status;

            latencyVal.textContent = state.avgLatency > 0 ? state.avgLatency + 'ms' : 'N/A';
            requestsVal.textContent = state.requestCount;
            failuresVal.textContent = state.failureCount;

            successVal.textContent = state.lastSuccess ? new Date(state.lastSuccess).toLocaleTimeString() : 'Never';
            failureVal.textContent = state.lastFailure ? new Date(state.lastFailure).toLocaleTimeString() : 'Never';

            if (state.reason) {
              reasonContainer.style.display = 'block';
              reasonVal.textContent = state.reason;
            } else {
              reasonContainer.style.display = 'none';
            }
          });

          // Refresh the lucide icons in case any badge was added/removed
          lucide.createIcons();
        }
      </script>
    </body>
    </html>
  `;
  return c.html(html);
});

// OpenAI compatible Chat Endpoint
app.post('/api/chat', async (c) => {
  try {
    const body = await c.req.json();
    const messages = body.messages || [];
    const temperature = body.temperature;
    const maxTokens = body.max_tokens;
    const forceJson = body.response_format?.type === 'json_object';
    const images = body.images; // Extract images from request body
    
    // Separate system prompt from messages if it exists
    let systemPrompt = null;
    const cleanMessages = [];
    messages.forEach(msg => {
      if (msg.role === 'system') {
        systemPrompt = msg.content;
      } else {
        cleanMessages.push(msg);
      }
    });

    const result = await routeChatRequest(systemPrompt, cleanMessages, temperature, maxTokens, forceJson, images);
    
    // Return standard OpenAI format response
    return c.json({
      choices: [
        {
          message: {
            role: 'assistant',
            content: result.text
          },
          finish_reason: 'stop',
          index: 0
        }
      ],
      usage: {
        prompt_tokens: 0,
        completion_tokens: 0,
        total_tokens: 0
      },
      model: result.providerKey,
      provider: result.provider
    });
  } catch (err) {
    console.error('[AI Gateway] Chat route failed:', err.message);
    return c.json({ error: err.message }, 500);
  }
});

// Audio Transcribe Endpoint
app.post('/api/transcribe', async (c) => {
  try {
    const body = await c.req.parseBody();
    const file = body.file;
    const model = body.model || 'whisper-large-v3-turbo';
    const language = body.language || 'en';
    
    if (!file) {
      return c.json({ error: 'No file uploaded' }, 400);
    }
    
    const apiKey = process.env.GROQ_API_KEY;
    if (!apiKey || apiKey.includes('PLACEHOLDER') || apiKey === '') {
      return c.json({ error: 'Groq API key not configured on backend' }, 500);
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('model', model);
    formData.append('language', language);
    formData.append('response_format', 'json');

    const response = await fetch('https://api.groq.com/openai/v1/audio/transcriptions', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${apiKey}`
      },
      body: formData
    });

    if (response.ok) {
      const data = await response.json();
      return c.json(data);
    } else {
      const errText = await response.text().catch(() => '');
      return c.json({ error: `Groq Whisper failed: ${errText}` }, response.status);
    }
  } catch (err) {
    return c.json({ error: err.message }, 500);
  }
});

// Trigger health checks on startup
runAllHealthChecks();

// Start Hono Node server on port 3000
const port = parseInt(process.env.PORT || '3000', 10);
console.log(`[AI Gateway] Starting server on port ${port}...`);
serve({
  fetch: app.fetch,
  port
});

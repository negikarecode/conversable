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
    formatPayload: (systemPrompt, messages, temperature, maxTokens) => {
      const contents = [];
      if (systemPrompt) {
        contents.push({ role: 'user', parts: [{ text: `System instruction: ${systemPrompt}` }] });
      }
      messages.forEach(msg => {
        contents.push({
          role: msg.role === 'assistant' ? 'model' : 'user',
          parts: [{ text: msg.content }]
        });
      });
      return {
        contents,
        generationConfig: {
          temperature: temperature || 0.7,
          maxOutputTokens: maxTokens || 150
        }
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
      model: 'llama3.1-8b',
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
        model: 'llama3.1-8b',
        messages: formattedMessages,
        temperature: temperature || 0.7,
        max_tokens: maxTokens || 150
      };
    }
  }
};

// Health States tracking
const healthStates = {
  gemini: { status: 'Unavailable', reason: 'Not verified', lastSuccess: null, lastFailure: null, avgLatency: 0, requestCount: 0, failureCount: 0 },
  groq: { status: 'Unavailable', reason: 'Not verified', lastSuccess: null, lastFailure: null, avgLatency: 0, requestCount: 0, failureCount: 0 },
  openrouter: { status: 'Unavailable', reason: 'Not verified', lastSuccess: null, lastFailure: null, avgLatency: 0, requestCount: 0, failureCount: 0 },
  mistral: { status: 'Unavailable', reason: 'Not verified', lastSuccess: null, lastFailure: null, avgLatency: 0, requestCount: 0, failureCount: 0 },
  cerebras: { status: 'Unavailable', reason: 'Not verified', lastSuccess: null, lastFailure: null, avgLatency: 0, requestCount: 0, failureCount: 0 }
};

// Check if a provider has a valid key configured in process.env
function isProviderConfigured(key) {
  const config = PROVIDERS[key];
  const apiKey = process.env[config.apiKeyName];
  return apiKey && apiKey !== '' && !apiKey.includes('PLACEHOLDER') && !apiKey.includes('YOUR_') && !apiKey.includes('MY_');
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
  if (status === 401 || status === 403) {
    return 'Invalid API key / Authentication failed';
  }
  if (status === 429) {
    return 'Rate limit exceeded';
  }
  if (status === 404) {
    return 'Model not found / Incorrect endpoint';
  }
  if (status >= 500) {
    return 'Provider internal server error (5xx)';
  }
  return `HTTP ${status}: ${errText.substring(0, 100)}`;
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
    const timeoutId = setTimeout(() => controller.abort(), 10000); // 10s timeout

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
    const reason = err.name === 'AbortError' ? 'Timeout' : `Network error: ${err.message}`;
    healthStates[key].status = 'Unavailable';
    healthStates[key].reason = reason;
    healthStates[key].lastFailure = new Date().toISOString();
    return false;
  }
}

// Send real test prompt to every healthy provider to verify AI response
async function verifyRealAiResponses() {
  console.log('\n==================================================');
  console.log(' RUNNING REAL AI RESPONSE VERIFICATION TESTS');
  console.log('==================================================');
  
  for (const key of Object.keys(PROVIDERS)) {
    if (healthStates[key].status !== 'Healthy') {
      console.log(`- Skipping ${PROVIDERS[key].name} (not healthy / missing key)`);
      continue;
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
      }
      
      const payload = config.formatPayload(null, testMsg, 0.1, 50);
      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(payload)
      });
      
      if (response.ok) {
        const data = await response.json();
        const text = config.parseResponse(data);
        if (text && text.toLowerCase().includes('conversable ai is working')) {
          console.log(`✓ ${config.name} returned valid response: "${text.trim()}"`);
        } else if (text) {
          console.log(`⚠️ ${config.name} returned response but did not match prompt: "${text.trim()}"`);
        } else {
          console.log(`✗ ${config.name} returned empty text`);
        }
      } else {
        console.log(`✗ ${config.name} returned HTTP ${response.status}`);
      }
    } catch (err) {
      console.log(`✗ ${config.name} failed during test request: ${err.message}`);
    }
  }
  console.log('==================================================\n');
}

// Startup checks
async function runAllHealthChecks() {
  // 1. Output Startup Configuration Report
  console.log('==================================================');
  console.log(' AI GATEWAY STARTUP CONFIGURATION REPORT');
  console.log('==================================================');
  for (const key of Object.keys(PROVIDERS)) {
    const config = PROVIDERS[key];
    if (isProviderConfigured(key)) {
      console.log(`✓ ${config.name} configured`);
    } else {
      console.log(`✗ ${config.name} missing API key`);
    }
  }
  console.log('==================================================\n');

  // 2. Perform Validation Requests
  console.log('[AI Gateway] Running startup validation health checks...');
  for (const key of Object.keys(PROVIDERS)) {
    await testProvider(key);
    console.log(`[AI Gateway] ${PROVIDERS[key].name}: ${healthStates[key].status} (${healthStates[key].reason})`);
  }

  // 3. Verify Real AI Output Responses
  await verifyRealAiResponses();
}

// Background scheduler
setInterval(async () => {
  console.log('[AI Gateway] Running periodic health checks...');
  for (const key of Object.keys(PROVIDERS)) {
    await testProvider(key);
  }
}, 3 * 60 * 1000); // 3 mins

// Intelligent routing & failover
async function routeChatRequest(systemPrompt, messages, temperature, maxTokens, forceJson) {
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
      const timeoutId = setTimeout(() => controller.abort(), 20000); // 20s request timeout

      const payload = config.formatPayload(systemPrompt, messages, temperature, maxTokens, forceJson);

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
          return { text, provider: config.name, latency };
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
  return c.json({
    status: Object.values(healthStates).some(p => p.status === 'Healthy') ? 'OK' : 'Degraded',
    timestamp: new Date().toISOString(),
    providers: healthStates
  });
});

// HTML Dashboard
app.get('/health', (c) => {
  const providerRows = Object.entries(healthStates).map(([key, state]) => {
    const config = PROVIDERS[key];
    const statusClass = state.status.toLowerCase();
    const lastSuccessStr = state.lastSuccess ? new Date(state.lastSuccess).toLocaleTimeString() : 'Never';
    const lastFailureStr = state.lastFailure ? new Date(state.lastFailure).toLocaleTimeString() : 'Never';
    const latencyStr = state.avgLatency > 0 ? `${state.avgLatency}ms` : 'N/A';
    
    return `
      <div class="provider-card ${statusClass}">
        <div class="card-header">
          <span class="provider-name">${config.name}</span>
          <span class="status-pill status-${statusClass}">${state.status}</span>
        </div>
        <div class="card-body">
          <p><strong>Latency (rolling):</strong> <span class="metric">${latencyStr}</span></p>
          <p><strong>Total Requests:</strong> <span class="metric">${state.requestCount}</span></p>
          <p><strong>Failures:</strong> <span class="metric">${state.failureCount}</span></p>
          <p><strong>Last Success:</strong> ${lastSuccessStr}</p>
          <p><strong>Last Failure:</strong> ${lastFailureStr}</p>
          <p class="reason-text"><strong>Details:</strong> ${state.reason}</p>
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
      <style>
        :root {
          --bg-color: #0d0e12;
          --panel-bg: rgba(22, 24, 33, 0.7);
          --border-color: rgba(255, 255, 255, 0.08);
          --text-primary: #f3f4f6;
          --text-secondary: #9ca3af;
          --healthy-color: #10b981;
          --healthy-glow: rgba(16, 185, 129, 0.15);
          --degraded-color: #f59e0b;
          --degraded-glow: rgba(245, 158, 11, 0.15);
          --unavailable-color: #ef4444;
          --unavailable-glow: rgba(239, 68, 68, 0.15);
        }
        body {
          margin: 0;
          padding: 0;
          background-color: var(--bg-color);
          color: var(--text-primary);
          font-family: 'Outfit', sans-serif;
          background-image: radial-gradient(circle at 50% 0%, rgba(99, 102, 241, 0.12) 0%, transparent 50%);
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
          margin-bottom: 40px;
          border-bottom: 1px solid var(--border-color);
          padding-bottom: 20px;
        }
        h1 {
          font-size: 2.2rem;
          font-weight: 800;
          margin: 0;
          background: linear-gradient(135deg, #fff 0%, #a5b4fc 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
        }
        .subtitle {
          color: var(--text-secondary);
          margin-top: 5px;
          font-size: 1rem;
        }
        .refresh-btn {
          background: linear-gradient(135deg, #4f46e5 0%, #3730a3 100%);
          color: white;
          border: none;
          padding: 12px 24px;
          border-radius: 30px;
          font-family: 'Outfit', sans-serif;
          font-weight: 600;
          cursor: pointer;
          transition: all 0.3s ease;
          box-shadow: 0 4px 14px rgba(79, 70, 229, 0.4);
        }
        .refresh-btn:hover {
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(79, 70, 229, 0.6);
        }
        .grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
          gap: 24px;
        }
        .provider-card {
          background: var(--panel-bg);
          border: 1px solid var(--border-color);
          border-radius: 20px;
          padding: 24px;
          backdrop-filter: blur(12px);
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        }
        .provider-card:hover {
          transform: translateY(-5px);
        }
        .provider-card.healthy {
          border-color: rgba(16, 185, 129, 0.3);
          box-shadow: 0 10px 30px var(--healthy-glow);
        }
        .provider-card.degraded {
          border-color: rgba(245, 158, 11, 0.3);
          box-shadow: 0 10px 30px var(--degraded-glow);
        }
        .provider-card.unavailable {
          border-color: rgba(239, 68, 68, 0.3);
          box-shadow: 0 10px 30px var(--unavailable-glow);
        }
        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 20px;
        }
        .provider-name {
          font-size: 1.4rem;
          font-weight: 600;
        }
        .status-pill {
          padding: 6px 14px;
          border-radius: 20px;
          font-size: 0.85rem;
          font-weight: 600;
          letter-spacing: 0.05em;
          text-transform: uppercase;
        }
        .status-healthy {
          background-color: rgba(16, 185, 129, 0.15);
          color: var(--healthy-color);
          border: 1px solid var(--healthy-color);
        }
        .status-degraded {
          background-color: rgba(245, 158, 11, 0.15);
          color: var(--degraded-color);
          border: 1px solid var(--degraded-color);
        }
        .status-unavailable {
          background-color: rgba(239, 68, 68, 0.15);
          color: var(--unavailable-color);
          border: 1px solid var(--unavailable-color);
          animation: pulse 2s infinite;
        }
        .card-body p {
          margin: 10px 0;
          font-size: 0.95rem;
          color: var(--text-secondary);
        }
        .metric {
          color: var(--text-primary);
          font-weight: 600;
        }
        .reason-text {
          font-size: 0.85rem !important;
          background-color: rgba(0, 0, 0, 0.2);
          padding: 10px;
          border-radius: 8px;
          word-break: break-all;
        }
        @keyframes pulse {
          0% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
          70% { box-shadow: 0 0 0 10px rgba(239, 68, 68, 0); }
          100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0); }
        }
      </style>
    </head>
    <body>
      <div class="container">
        <header>
          <div>
            <h1>Conversable AI Gateway</h1>
            <div class="subtitle">Multi-Provider Intelligent Routing & Failover System</div>
          </div>
          <button class="refresh-btn" onclick="location.reload()">Refresh Status</button>
        </header>
        <div class="grid">
          ${providerRows}
        </div>
      </div>
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

    const result = await routeChatRequest(systemPrompt, cleanMessages, temperature, maxTokens, forceJson);
    
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

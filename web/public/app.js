// CONVERSABLE LANDING PAGE LOGIC & ANALYTICS INTEGRATION

// 1. PRODUCTION ANALYTICS HELPER
function trackEvent(eventName, eventParams = {}) {
  // console.log(`[Analytics Event] ${eventName}`, eventParams); // Debug verify
  
  // Google Analytics 4 Integration
  if (typeof gtag === 'function') {
    gtag('event', eventName, eventParams);
  }
  
  // Microsoft Clarity Integration
  if (typeof clarity === 'function') {
    clarity("event", eventName);
  }
}

// 2. HERO DEMO SYSTEM
function triggerHeroDemo(choice) {
  const userMsgContainer = document.getElementById('hero-demo-user-msg');
  const userMsgText = document.getElementById('hero-demo-user-text');
  const feedbackCard = document.getElementById('hero-demo-feedback');
  const feedbackText = document.getElementById('hero-demo-feedback-text');
  const actionContainer = document.getElementById('hero-demo-actions');
  const scorePill = feedbackCard.querySelector('.score-pill');

  let userText = "";
  let feedback = "";
  let score = "";

  if (choice === 'defensive') {
    userText = "Our technology is 10x better, so competitor X's advertising budget doesn't matter.";
    score = "Assertiveness: 95% | Empathy: 12%";
    feedback = "Defensive response. By dismissing competitor strengths and their investor's concern rather than providing a structured explanation, you raise defensiveness. Investor interest drops.";
  } else if (choice === 'empathetic') {
    userText = "Competitor X relies on paid ads. We grow organically via developer word-of-mouth. Let me share the CAC metrics.";
    score = "Assertiveness: 88% | Empathy: 92%";
    feedback = "Excellent response. You validated their market query, demonstrated objective understanding of Competitor X's growth loop, and pivoted to your organic developer moat backed by hard data.";
  }

  // Track event
  trackEvent('hero_demo_choice', { choice_type: choice });

  // Display user response
  userMsgText.textContent = `"${userText}"`;
  userMsgContainer.style.display = 'block';
  actionContainer.style.display = 'none';

  // Wait, show typing simulation, then show feedback
  setTimeout(() => {
    scorePill.textContent = score;
    feedbackText.textContent = feedback;
    feedbackCard.style.display = 'block';
  }, 600);
}


// 3. PLAYGROUND SIMULATOR DATA
const simulatorData = {
  negotiation: {
    partnerName: "Marcus Vance",
    partnerTitle: "VP of Operations",
    promptText: `"Thanks for setting aside time, Alex. I reviewed your compensation review request. While we really value your work, a 15% raise is significantly higher than our standard yearly adjustments. How do you justify this step up?"`,
    avatarClass: "avatar-marcus",
    options: [
      {
        num: "Option A",
        text: `"I understand the guidelines, but I've delivered three times my target revenue this year, and my market value has risen. I believe my contributions justify the difference."`,
        dna: { confidence: 90, empathy: 60, clarity: 75 },
        tone: "Direct & Firm",
        score: 85,
        archetype: "Assertive Achiever",
        verdict: "Strong logical justification based on performance. High assertiveness, but slightly low empathy for corporate constraints. Overall, very solid and professional.",
        improvements: [
          "Acknowledge the company's budget guidelines first to establish common ground.",
          "Frame the 15% raise as a shared business investment rather than an individual cost."
        ]
      },
      {
        num: "Option B",
        text: `"If you can't match this, I'll have to start looking elsewhere. I'm already underpaid compared to my peers in other tech companies."`,
        dna: { confidence: 95, empathy: 15, clarity: 50 },
        tone: "Combative & Ultimatums",
        score: 30,
        archetype: "Ultimatum / Combative",
        verdict: "Ultimatums trigger immediate defensive barriers. Threatening to leave breaks trust in a negotiation unless you have a counter-offer in hand, and even then, it is highly risky.",
        improvements: [
          "Reframe the request as a collaborative future-proofing conversation rather than a threat.",
          "Anchor your request on your own specific metrics rather than peer comparisons."
        ]
      },
      {
        num: "Option C",
        text: `"I see. That's okay, I understand budgets are tight. Let's just do whatever standard adjustment is easiest for you."`,
        dna: { confidence: 20, empathy: 90, clarity: 60 },
        tone: "Passive & Compliant",
        score: 45,
        archetype: "Submissive Pleaser",
        verdict: "Too submissive. You immediately folded, which signals that you do not value your own market worth or contributions. You left money on the table.",
        improvements: [
          "Do not apologize or fold instantly when a concern is raised; it is a normal business discussion.",
          "Practice stating your metrics and holding a firm posture without apologizing."
        ]
      }
    ]
  },
  criticism: {
    partnerName: "Lars Sorensen",
    partnerTitle: "Creative Director",
    promptText: `"The latest design delivery is completely off-brand. It feels cluttered and represents none of our Scandinavian roots. Frankly, I'm disappointed. We need to redo this, but I'm worried we will miss the launch."`,
    avatarClass: "avatar-lars",
    options: [
      {
        num: "Option A",
        text: `"I'm sorry you feel that way. We followed the brief you provided. If we redo it, we will have to bill you for the extra hours and delay the launch."`,
        dna: { confidence: 80, empathy: 30, clarity: 50 },
        tone: "Defensive & Transgressing",
        score: 50,
        archetype: "Defensive Vendor",
        verdict: "Deflecting blame onto the client's brief immediately stalls collaboration. Mentioning extra billing in the same breath as their disappointment escalates conflict.",
        improvements: [
          "First establish alignment on the goal (a successful launch) and validate their concern.",
          "Establish clarity on the specific visual issues before discussing budget and timelines."
        ]
      },
      {
        num: "Option B",
        text: `"I hear your concern about the launch date, Lars. Let's schedule a 15-minute call in one hour to pinpoint the exact off-brand elements, and we'll dedicate our team tonight to resolve them."`,
        dna: { confidence: 85, empathy: 95, clarity: 90 },
        tone: "Collaborative & Structured",
        score: 96,
        archetype: "Collaborative Leader",
        verdict: "Outstanding. You validated their anxiety (the launch deadline), took immediate accountability for resolving the issue, and proposed a structured, quick path forward.",
        improvements: [
          "None needed. This response builds immense client trust under pressure.",
          "Ensure you follow up immediately with the calendar invite for the feedback call."
        ]
      },
      {
        num: "Option C",
        text: `"Oh no, I'm so sorry! We'll delete everything and start from scratch right away. Please don't be disappointed, we will do whatever it takes."`,
        dna: { confidence: 30, empathy: 85, clarity: 60 },
        tone: "Apologetic & Panic",
        score: 55,
        archetype: "Panic Pleaser",
        verdict: "While highly empathetic, this response lacks assertiveness. Deleting everything without understanding why it failed is inefficient and signals panic rather than expert leadership.",
        improvements: [
          "Anchor your response with a structured feedback process instead of immediately offering to scrap the work.",
          "Manage client expectations calmly rather than agreeing to rebuild blindly."
        ]
      }
    ]
  },
  networking: {
    partnerName: "Hana Hayashi",
    partnerTitle: "General Partner, Shibuya Ventures",
    promptText: `"So, I see your app teaches social skills. Honestly, isn't that something people just learn naturally? Why is this a startup business?"`,
    avatarClass: "avatar-hana",
    options: [
      {
        num: "Option A",
        text: `"Sure, people learn naturally, but they learn slowly and make costly mistakes. We accelerate that training by 10x using interactive psychology loops."`,
        dna: { confidence: 85, empathy: 75, clarity: 80 },
        tone: "Direct & Informative",
        score: 88,
        archetype: "Solution Architect",
        verdict: "Very good response. You reframed 'natural learning' as a slow, expensive process, positioning Conversable as an efficiency engine. Clear value proposition.",
        improvements: [
          "Add a concrete case study or growth metric to back up your 10x acceleration claim.",
          "Prompt Hana to test our simulator cards to immediately demonstrate the value."
        ]
      },
      {
        num: "Option B",
        text: `"Actually, look around. Remote work, Zoom fatigue, and social anxiety are at all-time highs. The old way of learning by trial and error is broken."`,
        dna: { confidence: 80, empathy: 50, clarity: 65 },
        tone: "Macro-focused & Theoretical",
        score: 72,
        archetype: "Problem Evacuee",
        verdict: "Good macro problem alignment. However, it feels slightly preachy and general. It would be stronger if focused on the customer's specific ROI.",
        improvements: [
          "Connect the macro trends of remote work directly back to professional performance metrics (e.g. negotiation, retention).",
          "Ensure your value proposition doesn't sound too lecture-heavy."
        ]
      },
      {
        num: "Option C",
        text: `"Social skills are actually highly complex algorithms. Most people are terrible at them, and it costs businesses billions of dollars annually."`,
        dna: { confidence: 90, empathy: 30, clarity: 45 },
        tone: "Cynical & Analytical",
        score: 60,
        archetype: "Rational Specialist",
        verdict: "High assertiveness but low empathy. Calling people 'terrible' at social skills makes the value proposition feel hostile rather than supportive.",
        improvements: [
          "Focus on 'deliberate training for high-performance' rather than framing the general population as socially deficient.",
          "Reframe the cost of poor communication into the growth potential of skilled teams."
        ]
      }
    ]
  }
};

let currentScenario = 'negotiation';

// 4. SWITCH SCENARIOS
function selectScenario(scenarioKey, tabElement) {
  currentScenario = scenarioKey;
  
  // Update Active Tab
  const tabs = document.querySelectorAll('.scenario-tab');
  tabs.forEach(tab => {
    tab.classList.remove('active');
    tab.setAttribute('aria-selected', 'false');
  });
  tabElement.classList.add('active');
  tabElement.setAttribute('aria-selected', 'true');

  trackEvent('simulator_scenario_select', { scenario: scenarioKey });

  // Reset simulator state
  resetSimulator();
}

// 5. RESET SIMULATOR
function resetSimulator() {
  const data = simulatorData[currentScenario];
  
  // Reset partner details
  document.getElementById('sim-partner-name').textContent = data.partnerName;
  document.getElementById('sim-partner-title').textContent = data.partnerTitle;
  document.getElementById('sim-prompt-text').textContent = data.promptText;

  // Reset User message
  document.getElementById('sim-user-message').style.display = 'none';
  document.getElementById('sim-user-text').textContent = '';

  // Show Option Buttons & hide reset container
  document.getElementById('sim-options-container').style.display = 'block';
  document.getElementById('sim-reset-container').style.display = 'none';

  // Load option texts
  document.getElementById('opt-1-text').textContent = data.options[0].text;
  document.getElementById('opt-2-text').textContent = data.options[1].text;
  document.getElementById('opt-3-text').textContent = data.options[2].text;

  // Reset option button highlights
  const buttons = document.querySelectorAll('.option-btn');
  buttons.forEach(btn => {
    btn.classList.remove('selected', 'disabled');
  });

  // Reset Stats View
  const statsPanel = document.querySelector('.stats-panel');
  if (statsPanel) {
    statsPanel.classList.remove('show');
  }
  document.getElementById('empty-stats-view').style.display = 'flex';
  document.getElementById('active-stats-view').style.display = 'none';

  // Reset Bar widths & values
  document.getElementById('dna-confidence-bar').style.width = '0%';
  document.getElementById('dna-confidence-val').textContent = '0%';
  document.getElementById('dna-empathy-bar').style.width = '0%';
  document.getElementById('dna-empathy-val').textContent = '0%';
  document.getElementById('dna-clarity-bar').style.width = '0%';
  document.getElementById('dna-clarity-val').textContent = '0%';
}

// 6. SELECT SIMULATOR OPTION
function chooseOption(index) {
  const optionIndex = index - 1;
  const data = simulatorData[currentScenario];
  const choice = data.options[optionIndex];

  trackEvent('simulator_option_click', { scenario: currentScenario, choice: choice.num });

  // Highlight selected option and disable others using CSS classes
  const buttons = document.querySelectorAll('.option-btn');
  buttons.forEach((btn, idx) => {
    if (idx === optionIndex) {
      btn.classList.add('selected');
    } else {
      btn.classList.add('disabled');
    }
  });

  // Show User Message
  const userMsgContainer = document.getElementById('sim-user-message');
  const userMsgText = document.getElementById('sim-user-text');
  userMsgText.textContent = choice.text;
  userMsgContainer.style.display = 'block';

  // Show Typing Indicator
  const typing = document.getElementById('sim-typing');
  typing.style.display = 'inline-flex';

  // Scroll to bottom of chat
  const chatContainer = document.getElementById('chat-messages-container');
  chatContainer.scrollTop = chatContainer.scrollHeight;

  // Immediately display style analysis panel in loading/analyzing state
  const statsPanel = document.querySelector('.stats-panel');
  if (statsPanel) {
    statsPanel.classList.add('show');
  }
  document.getElementById('empty-stats-view').style.display = 'none';
  document.getElementById('active-stats-view').style.display = 'block';

  // Set Tone Analysis and verdict text to "Analyzing speaking style..."
  document.getElementById('dna-badge').textContent = "Analyzing...";
  document.getElementById('dna-score').textContent = "--/100";
  document.getElementById('dna-tone-val').textContent = "Calculating metrics...";
  document.getElementById('dna-verdict-text').textContent = "AI coach is processing your response structure, assertiveness levels, and empathy anchors...";

  // Reset bars to 0% for start of animation
  document.getElementById('dna-confidence-bar').style.width = '0%';
  document.getElementById('dna-confidence-val').textContent = '0%';
  document.getElementById('dna-empathy-bar').style.width = '0%';
  document.getElementById('dna-empathy-val').textContent = '0%';
  document.getElementById('dna-clarity-bar').style.width = '0%';
  document.getElementById('dna-clarity-val').textContent = '0%';

  // Empty improvements list with placeholder loading state
  const improvementsList = document.getElementById('dna-improvements-list');
  improvementsList.innerHTML = '<li>Analyzing speech DNA...</li>';

  setTimeout(() => {
    // Hide Typing Indicator
    typing.style.display = 'none';

    // Set DNA scores with animation delay
    setTimeout(() => {
      document.getElementById('dna-confidence-bar').style.width = `${choice.dna.confidence}%`;
      document.getElementById('dna-confidence-val').textContent = `${choice.dna.confidence}%`;
      
      document.getElementById('dna-empathy-bar').style.width = `${choice.dna.empathy}%`;
      document.getElementById('dna-empathy-val').textContent = `${choice.dna.empathy}%`;
      
      document.getElementById('dna-clarity-bar').style.width = `${choice.dna.clarity}%`;
      document.getElementById('dna-clarity-val').textContent = `${choice.dna.clarity}%`;
    }, 100);

    // Set Verdict Text
    document.getElementById('dna-badge').textContent = choice.archetype;
    document.getElementById('dna-score').textContent = `${choice.score}/100`;
    document.getElementById('dna-tone-val').textContent = choice.tone;
    document.getElementById('dna-verdict-text').textContent = choice.verdict;

    // Load Suggested Improvements List
    improvementsList.innerHTML = '';
    choice.improvements.forEach(imp => {
      const li = document.createElement('li');
      li.textContent = imp;
      improvementsList.appendChild(li);
    });

    // Swap option selector with Reset Button
    document.getElementById('sim-options-container').style.display = 'none';
    document.getElementById('sim-reset-container').style.display = 'block';

    // Scroll chat again
    chatContainer.scrollTop = chatContainer.scrollHeight;
  }, 1000);
}

// 7. APK DOWNLOAD HANDLER (Direct programmatic trigger)
function handleApkDownload(event, buttonElement) {
  if (event) event.preventDefault();
  
  if (!buttonElement) return;
  if (buttonElement.classList.contains('loading')) return;
  
  trackEvent('apk_download_click', { source_id: buttonElement.id || 'inline' });

  // Save original styles/text
  const originalText = buttonElement.innerHTML;
  buttonElement.classList.add('loading');
  buttonElement.style.pointerEvents = 'none';
  
  const textSpan = buttonElement.querySelector('span') || buttonElement;
  let prevText = textSpan ? textSpan.textContent : 'Download APK';
  
  if (textSpan) {
    textSpan.textContent = 'Downloading...';
  }
  
  // Trigger direct download
  const a = document.createElement('a');
  a.href = '/conversable.apk';
  a.download = 'conversable.apk';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  
  setTimeout(() => {
    buttonElement.classList.remove('loading');
    buttonElement.style.pointerEvents = 'auto';
    if (textSpan) {
      textSpan.textContent = prevText;
    } else {
      buttonElement.innerHTML = originalText;
    }
    showToast('Download started successfully!');
    trackEvent('apk_download_success');
  }, 1200);
}

// 8. CUSTOM TOAST HELPER
function showToast(message) {
  let toast = document.getElementById('apk-toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'apk-toast';
    document.body.appendChild(toast);
  }
  toast.textContent = message;
  toast.offsetHeight; // trigger reflow
  toast.classList.add('show');
  
  if (window.toastTimeout) {
    clearTimeout(window.toastTimeout);
  }
  window.toastTimeout = setTimeout(() => {
    toast.classList.remove('show');
  }, 3000);
}

// 9. SCROLL DEPTH & SESSION DURATION TRACKING
let maxScroll = 0;
function trackScrollDepth() {
  const scrollTop = window.scrollY || document.documentElement.scrollTop;
  const docHeight = document.documentElement.scrollHeight - window.innerHeight;
  if (docHeight > 0) {
    const scrollPercent = Math.min(Math.round((scrollTop / docHeight) * 100), 100);
    if (scrollPercent > maxScroll) {
      maxScroll = scrollPercent;
      if (maxScroll % 25 === 0 && maxScroll > 0) {
        trackEvent('scroll_depth', { percent: maxScroll });
      }
    }
  }
}

// Initialize on DOM Load
window.addEventListener('DOMContentLoaded', () => {
  if (typeof lucide !== 'undefined') {
    lucide.createIcons();
  }
  resetSimulator();

  // Track session start
  trackEvent('session_start');

  // Track scroll depth
  window.addEventListener('scroll', trackScrollDepth);

  // Sticky CTA Bar scroll behavior
  const stickyBar = document.getElementById('sticky-cta-bar');
  if (stickyBar) {
    const handleScroll = () => {
      const playgroundSection = document.getElementById('playground');
      const footerSection = document.querySelector('.footer-section');
      const heroSection = document.querySelector('.hero-section');
      
      const scrollY = window.scrollY + window.innerHeight;
      const heroHeight = heroSection?.offsetHeight || 400;
      const isPastHero = window.scrollY > heroHeight;
      
      const hideZones = [playgroundSection, footerSection]
        .filter(Boolean)
        .map(el => el.offsetTop - 100);
      
      const shouldHide = hideZones.some(zone => scrollY >= zone + 100);
      
      if (isPastHero && !shouldHide && window.innerWidth <= 768) {
        stickyBar.style.transform = 'translateY(0)';
      } else {
        stickyBar.style.transform = 'translateY(100%)';
      }
    };
    window.addEventListener('scroll', handleScroll);
    window.addEventListener('resize', handleScroll);
    handleScroll();
  }
});

// Expose key functions globally to ensure HTML inline events work reliably
window.triggerHeroDemo = triggerHeroDemo;
window.selectScenario = selectScenario;
window.resetSimulator = resetSimulator;
window.chooseOption = chooseOption;
window.handleApkDownload = handleApkDownload;
window.showToast = showToast;

// Lucide setup
document.addEventListener('DOMContentLoaded', () => {
  if (typeof lucide !== 'undefined') {
    lucide.createIcons();
  }
  initializeRouter();
  initializeSearch();
});

// Hash Router configuration
function initializeRouter() {
  // Listen to hash shifts
  window.addEventListener('hashchange', handleRouting);
  
  // Initial routing pass
  handleRouting();
  
  // Sidebar click handler (for clicking already active hash links to re-trigger)
  document.querySelectorAll('.sidebar-link').forEach(link => {
    link.addEventListener('click', (e) => {
      const policyId = link.getAttribute('data-policy');
      window.location.hash = `#${policyId}`;
    });
  });
  
  // Footer page hooks
  document.querySelectorAll('.footer-links-col a[href^="#"]').forEach(link => {
    link.addEventListener('click', () => {
      const policyId = link.getAttribute('href').replace('#', '');
      window.location.hash = `#${policyId}`;
    });
  });
}

function handleRouting() {
  const hash = window.location.hash || '#privacy';
  const policyId = hash.replace('#', '');
  
  const validPolicies = ['privacy', 'terms', 'cookies', 'community', 'ai', 'deletion', 'security', 'accessibility', 'contact'];
  if (!validPolicies.includes(policyId)) {
    return;
  }
  
  // Toggle sidebar link states
  document.querySelectorAll('.sidebar-link').forEach(link => {
    link.classList.remove('active');
    if (link.getAttribute('data-policy') === policyId) {
      link.classList.add('active');
    }
  });
  
  // Toggle article pane visibility
  document.querySelectorAll('.policy-article').forEach(art => {
    art.classList.remove('active');
    if (art.id === policyId) {
      art.classList.add('active');
    }
  });
  
  // Refresh dynamic components
  generateTOC(policyId);
  calculateReadingTime(policyId);
  
  // Reset scroll position
  window.scrollTo({ top: 0, behavior: 'smooth' });
  
  // Update Mobile nav title
  const activeLink = document.querySelector(`.sidebar-link[data-policy="${policyId}"]`);
  if (activeLink) {
    document.querySelector('#mobile-nav-toggle span').textContent = activeLink.textContent;
  }
  
  // Close mobile dropdown if active
  document.getElementById('legal-sidebar-nav').classList.remove('open');
  document.getElementById('mobile-nav-toggle').setAttribute('aria-expanded', 'false');
  
  // Reset search inputs
  clearSearch();
}

// Dynamic TOC builder with Copy Anchor functionality
let currentObserver = null;
function generateTOC(policyId) {
  const article = document.getElementById(policyId);
  const tocList = document.getElementById('active-toc-list');
  tocList.innerHTML = '';
  
  if (!article) return;
  
  const headings = article.querySelectorAll('h2');
  if (headings.length === 0) {
    document.getElementById('legal-toc-nav').style.display = 'none';
    return;
  } else {
    document.getElementById('legal-toc-nav').style.display = 'block';
  }
  
  headings.forEach((heading) => {
    // Formulate ID if missing
    if (!heading.id) {
      heading.id = heading.textContent.toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/(^-|-$)/g, '');
    }
    
    // Add Copy Link helper inside the heading if absent
    if (!heading.querySelector('.anchor-link-btn')) {
      const anchorBtn = document.createElement('button');
      anchorBtn.className = 'anchor-link-btn';
      anchorBtn.setAttribute('aria-label', 'Copy link to this section');
      anchorBtn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-link"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path></svg>`;
      anchorBtn.onclick = (e) => {
        e.stopPropagation();
        const url = window.location.origin + window.location.pathname + '#' + policyId + ':' + heading.id;
        navigator.clipboard.writeText(url).then(() => {
          const origHTML = anchorBtn.innerHTML;
          anchorBtn.innerHTML = `<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-check" style="stroke: #1F8A4D;"><polyline points="20 6 9 17 4 12"></polyline></svg>`;
          setTimeout(() => { 
            anchorBtn.innerHTML = origHTML; 
          }, 2000);
        });
      };
      heading.style.position = 'relative';
      heading.insertBefore(anchorBtn, heading.firstChild);
    }
    
    // Assemble TOC link
    const tocLink = document.createElement('a');
    tocLink.href = `#${policyId}`;
    tocLink.textContent = heading.textContent.trim();
    tocLink.onclick = (e) => {
      e.preventDefault();
      heading.scrollIntoView({ behavior: 'smooth' });
      history.pushState(null, null, `#${policyId}`);
    };
    
    tocList.appendChild(tocLink);
  });
  
  setupScrollObserver(headings);
}

// Scroll tracker for sidebar TOC highlight
function setupScrollObserver(headings) {
  if (currentObserver) {
    currentObserver.disconnect();
  }
  
  const tocLinks = document.querySelectorAll('#active-toc-list a');
  
  currentObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const headingId = entry.target.id;
        tocLinks.forEach((link, idx) => {
          const matchingHeading = headings[idx];
          if (matchingHeading && matchingHeading.id === headingId) {
            link.classList.add('active');
          } else {
            link.classList.remove('active');
          }
        });
      }
    });
  }, {
    rootMargin: '-80px 0px -70% 0px'
  });
  
  headings.forEach(heading => currentObserver.observe(heading));
}

// Est. Reading Speed calculator
function calculateReadingTime(policyId) {
  const article = document.getElementById(policyId);
  if (!article) return;
  const text = article.textContent;
  const words = text.trim().split(/\s+/).length;
  const minutes = Math.max(1, Math.round(words / 220));
  document.getElementById('estimated-reading-time').textContent = `${minutes} min read`;
}

// Search inside policy content
let originalHTMLs = {};
function initializeSearch() {
  document.querySelectorAll('.policy-article').forEach(art => {
    originalHTMLs[art.id] = art.innerHTML;
  });
  
  const searchInput = document.getElementById('legal-search-input');
  const clearBtn = document.getElementById('clear-search-btn');
  
  searchInput.addEventListener('input', (e) => {
    const query = e.target.value.trim().toLowerCase();
    const activeArticle = document.querySelector('.policy-article.active');
    
    if (!activeArticle) return;
    
    if (query.length === 0) {
      clearSearch();
      return;
    }
    
    clearBtn.style.display = 'block';
    
    // Restore original HTML
    activeArticle.innerHTML = originalHTMLs[activeArticle.id];
    
    // Match term nodes
    highlightTextNodes(activeArticle, query);
    
    // Re-generate heading listeners
    generateTOC(activeArticle.id);
  });
  
  clearBtn.onclick = () => {
    clearSearch();
  };
}

function clearSearch() {
  const searchInput = document.getElementById('legal-search-input');
  searchInput.value = '';
  document.getElementById('clear-search-btn').style.display = 'none';
  
  document.querySelectorAll('.policy-article').forEach(art => {
    if (originalHTMLs[art.id]) {
      art.innerHTML = originalHTMLs[art.id];
    }
  });
  
  const activeArticle = document.querySelector('.policy-article.active');
  if (activeArticle) {
    generateTOC(activeArticle.id);
  }
}

function highlightTextNodes(element, query) {
  const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT, null, false);
  const nodes = [];
  
  while (walker.nextNode()) {
    nodes.push(walker.currentNode);
  }
  
  nodes.forEach(node => {
    const parent = node.parentNode;
    if (['SCRIPT', 'STYLE', 'MARK', 'CODE', 'BUTTON'].includes(parent.tagName)) return;
    
    const text = node.nodeValue;
    const lowerText = text.toLowerCase();
    const matchIndex = lowerText.indexOf(query);
    
    if (matchIndex >= 0) {
      const fragment = document.createDocumentFragment();
      let tempText = text;
      
      while (true) {
        const idx = tempText.toLowerCase().indexOf(query);
        if (idx < 0) {
          fragment.appendChild(document.createTextNode(tempText));
          break;
        }
        
        // Text before match
        fragment.appendChild(document.createTextNode(tempText.substring(0, idx)));
        
        // Highlighted mark
        const mark = document.createElement('mark');
        mark.className = 'search-highlight';
        mark.textContent = tempText.substring(idx, idx + query.length);
        fragment.appendChild(mark);
        
        tempText = tempText.substring(idx + query.length);
      }
      
      parent.replaceChild(fragment, node);
    }
  });
}

// Scroll helper actions (progress & back to top)
window.addEventListener('scroll', () => {
  const scrollTop = window.scrollY;
  const docHeight = document.documentElement.scrollHeight - window.innerHeight;
  const progress = docHeight > 0 ? (scrollTop / docHeight) * 100 : 0;
  document.getElementById('reading-progress').style.width = `${progress}%`;
  
  const backToTop = document.getElementById('back-to-top');
  if (scrollTop > 300) {
    backToTop.classList.add('visible');
  } else {
    backToTop.classList.remove('visible');
  }
});

document.getElementById('back-to-top').onclick = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' });
};

// Print & PDF Download triggers
document.getElementById('btn-print-doc').onclick = () => window.print();
document.getElementById('btn-download-pdf').onclick = () => window.print();

// Mobile Navbar navigation panel toggle
const mobileBtn = document.getElementById('mobile-nav-toggle');
const sidebarMenu = document.getElementById('legal-sidebar-nav');

mobileBtn.onclick = (e) => {
  e.stopPropagation();
  const open = sidebarMenu.classList.toggle('open');
  mobileBtn.setAttribute('aria-expanded', open ? 'true' : 'false');
};

document.addEventListener('click', (e) => {
  if (sidebarMenu.classList.contains('open') && !sidebarMenu.contains(e.target) && e.target !== mobileBtn) {
    sidebarMenu.classList.remove('open');
    mobileBtn.setAttribute('aria-expanded', 'false');
  }
});

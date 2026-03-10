// Header functionality and navigation scripts

document.addEventListener("DOMContentLoaded", function () {
  // Sticky header: fallback fixed khi position:sticky bị chặn bởi ancestor
  initStickyHeaderFallback();

  // Active navigation link highlighting
  highlightActiveNavLink();

  // Initialize tooltips (if using Bootstrap tooltips)
  initializeTooltips();

  // Handle form submissions
  initializeFormHandlers();
});

/**
 * Giữ header__top (logo, search, cart) dính trên cùng khi scroll.
 * Dùng position:fixed + placeholder nếu sticky không hoạt động (do overflow/transform ancestor).
 */
function initStickyHeaderFallback() {
  const wrapper = document.querySelector("header.tgdd-header-wrapper");
  const headerTop = document.querySelector("header.tgdd-header-wrapper .header__top");
  const topBar = document.querySelector("header.tgdd-header-wrapper .header-top-bar");
  if (!headerTop || !wrapper) return;

  const topBarHeight = topBar ? topBar.offsetHeight : 0;
  let placeholder = null;

  function updateSticky() {
    const scrollY = window.scrollY || window.pageYOffset;
    if (scrollY > topBarHeight) {
      if (!headerTop.classList.contains("header__top--fixed")) {
        const barHeight = headerTop.offsetHeight;
        if (!placeholder) {
          placeholder = document.createElement("div");
          placeholder.className = "header__top-placeholder";
          headerTop.parentNode.insertBefore(placeholder, headerTop.nextSibling);
        }
        placeholder.style.height = barHeight + "px";
        placeholder.classList.add("is-visible");
        headerTop.classList.add("header__top--fixed");
      }
    } else {
      if (headerTop.classList.contains("header__top--fixed")) {
        headerTop.classList.remove("header__top--fixed");
        if (placeholder) placeholder.classList.remove("is-visible");
      }
    }
  }

  let ticking = false;
  window.addEventListener("scroll", function () {
    if (!ticking) {
      requestAnimationFrame(function () {
        updateSticky();
        ticking = false;
      });
      ticking = true;
    }
  }, { passive: true });

  updateSticky();
}

/**
 * Highlight active navigation link based on current page (chỉ .nav-link trong navbar, không đụng tab khác)
 */
function highlightActiveNavLink() {
  const currentPath = window.location.pathname;
  const navLinks = document.querySelectorAll("nav .nav-link, .navbar .nav-link");

  navLinks.forEach((link) => {
    const href = link.getAttribute("href");
    if (!href || href === "#") return;
    if (currentPath === href || (href !== "/" && currentPath.startsWith(href))) {
      link.classList.add("active");
    } else {
      link.classList.remove("active");
    }
  });
}

/**
 * Initialize Bootstrap tooltips (bỏ qua nếu Bootstrap chưa load để tránh lỗi JS)
 */
function initializeTooltips() {
  if (typeof bootstrap === "undefined") return;
  try {
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggerList.forEach(function (tooltipTriggerEl) {
      new bootstrap.Tooltip(tooltipTriggerEl);
    });
  } catch (err) {
    console.warn("Tooltips init skipped:", err);
  }
}

/**
 * Initialize form handlers
 */
function initializeFormHandlers() {
  // Add form validation classes
  const forms = document.querySelectorAll("form");

  forms.forEach((form) => {
    if (form.closest("#cart-list")) return;
    form.addEventListener("submit", function (e) {
      const submitBtn = form.querySelector('button[type="submit"]');
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = "⏳ Processing...";
      }
    });
  });
}

/**
 * Show confirmation dialog
 */
function confirmDelete(message = "Are you sure you want to delete this item?") {
  return confirm(message);
}

/**
 * Close alert messages
 */
function closeAlert(alertElement) {
  alertElement.style.opacity = "0";
  alertElement.style.transition = "opacity 0.3s ease";

  setTimeout(() => {
    alertElement.remove();
  }, 300);
}

/**
 * Format currency
 */
function formatCurrency(value) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
  }).format(value);
}

/**
 * Debounce function for search inputs
 */
function debounce(func, delay) {
  let timeoutId;
  return function (...args) {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => func.apply(this, args), delay);
  };
}

// Export functions for global use
window.confirmDelete = confirmDelete;
window.closeAlert = closeAlert;
window.formatCurrency = formatCurrency;
window.debounce = debounce;

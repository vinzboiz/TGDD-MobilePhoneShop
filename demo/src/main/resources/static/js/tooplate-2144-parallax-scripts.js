// Parallax effect scripts

document.addEventListener("DOMContentLoaded", function () {
  initializeParallax();
});

/**
 * Initialize parallax effects for backgrounds
 */
function initializeParallax() {
  const parallaxElements = document.querySelectorAll("[data-parallax]");

  if (parallaxElements.length === 0) {
    return;
  }

  window.addEventListener(
    "scroll",
    function () {
      const scrollPosition = window.scrollY;

      parallaxElements.forEach((element) => {
        const speed = parseFloat(element.getAttribute("data-parallax")) || 0.5;
        const yPos = scrollPosition * speed;

        element.style.backgroundPosition = `center ${yPos}px`;
      });
    },
    false,
  );
}

/**
 * Apply parallax effect to specific element
 */
function applyParallax(element, speed = 0.5) {
  element.setAttribute("data-parallax", speed);
}

/**
 * Smooth scroll to element
 */
function smoothScrollTo(element, offset = 0) {
  const elementPosition = element.getBoundingClientRect().top + window.scrollY;
  const scrollTarget = elementPosition - offset;

  window.scrollTo({
    top: scrollTarget,
    behavior: "smooth",
  });
}

/**
 * Fade in animation on scroll
 */
function observeFadeInElements() {
  const observer = new IntersectionObserver(
    function (entries) {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("fade-in");
          observer.unobserve(entry.target);
        }
      });
    },
    {
      threshold: 0.1,
    },
  );

  const elements = document.querySelectorAll("[data-fade-in]");
  elements.forEach((el) => observer.observe(el));
}

// Initialize fade-in effects
observeFadeInElements();

// Export functions
window.applyParallax = applyParallax;
window.smoothScrollTo = smoothScrollTo;
window.observeFadeInElements = observeFadeInElements;

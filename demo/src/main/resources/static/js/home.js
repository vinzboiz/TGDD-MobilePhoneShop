/**
 * Trang chủ: tab active, timeline selected, countdown
 */
(function () {
  'use strict';

  function initHomePage() {
    // --- Slide trượt ngang: 2 ảnh/trang, nút < >, mỗi 3s tự chuyển ---
    var slideTrack = document.getElementById('home-slide-track');
    var btnPrev = document.getElementById('home-slide-prev');
    var btnNext = document.getElementById('home-slide-next');
    if (slideTrack) {
      var slideIndex = 0;
      var totalSteps = 3;
      var slideInterval;

      function applySlide(noTransition) {
        if (noTransition) {
          slideTrack.style.transition = 'none';
        } else {
          slideTrack.style.transition = 'transform 0.4s ease';
        }
        slideTrack.style.transform = 'translateX(' + (-slideIndex * (100 / totalSteps)) + '%)';
        if (noTransition) {
          slideTrack.offsetHeight;
          slideTrack.style.transition = '';
        }
      }

      function goNext() {
        if (slideIndex < totalSteps - 1) {
          slideIndex += 1;
          applySlide(false);
        } else {
          slideIndex = 0;
          applySlide(true);
        }
      }

      function goPrev() {
        if (slideIndex > 0) {
          slideIndex -= 1;
          applySlide(false);
        } else {
          slideIndex = totalSteps - 1;
          applySlide(true);
        }
      }

      function startInterval() {
        slideInterval = setInterval(goNext, 3000);
      }

      startInterval();
      if (btnPrev) {
        btnPrev.addEventListener('click', function () {
          goPrev();
          clearInterval(slideInterval);
          startInterval();
        });
      }
      if (btnNext) {
        btnNext.addEventListener('click', function () {
          goNext();
          clearInterval(slideInterval);
          startInterval();
        });
      }
    }

    // --- Tab: click tab → chuyển class active (nền #FFF6E3, gạch cam) ---
    var tablist = document.getElementById('promo-tabs');
    if (tablist) {
      tablist.addEventListener('click', function (e) {
        var link = e.target.closest('a.tgdd-promo-online__tab-link');
        if (!link) return;
        e.preventDefault();
        e.stopPropagation();
        tablist.querySelectorAll('.tgdd-promo-online__tab-link').forEach(function (a) {
          a.classList.remove('active');
          a.setAttribute('aria-selected', 'false');
        });
        link.classList.add('active');
        link.setAttribute('aria-selected', 'true');
      });
    }

    // --- Timeline: click ô → ô đó selected (nền #F79009, chữ trắng) ---
    var timeline = document.querySelector('.tgdd-flashsale-timeline');
    if (timeline) {
      timeline.addEventListener('click', function (e) {
        var item = e.target.closest('a.tgdd-flashsale-timeline__item');
        if (!item) return;
        e.preventDefault();
        e.stopPropagation();
        timeline.querySelectorAll('.tgdd-flashsale-timeline__item').forEach(function (el) {
          el.classList.remove('tgdd-flashsale-timeline__selected');
        });
        item.classList.add('tgdd-flashsale-timeline__selected');
      });
    }

    // --- Countdown ---
    var endEl = document.querySelector('[data-countdown-end]');
    if (endEl) {
      var raw = endEl.getAttribute('data-countdown-end');
      if (raw) {
        var target = new Date(raw).getTime();
        if (!isNaN(target)) {
          var h = document.getElementById('fs-hour');
          var m = document.getElementById('fs-minute');
          var s = document.getElementById('fs-second');
          if (h && m && s) {
            function pad(n) {
              return (n < 10 ? '0' : '') + n;
            }
            function tick() {
              var now = Date.now();
              var left = Math.max(0, Math.floor((target - now) / 1000));
              if (left <= 0) {
                h.textContent = '00';
                m.textContent = '00';
                s.textContent = '00';
                return;
              }
              var hh = Math.floor(left / 3600);
              var mm = Math.floor((left % 3600) / 60);
              var ss = left % 60;
              h.textContent = pad(hh);
              m.textContent = pad(mm);
              s.textContent = pad(ss);
            }
            tick();
            setInterval(tick, 1000);
          }
        }
      }
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initHomePage);
  } else {
    initHomePage();
  }
})();

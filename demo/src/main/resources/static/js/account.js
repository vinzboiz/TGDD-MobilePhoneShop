// JS thuần cho trang tài khoản
(function () {
  document.addEventListener('DOMContentLoaded', function () {
    console.log('[account.js] DOMContentLoaded fired');
    // 1. Chuyển section (Đơn hàng / Sổ địa chỉ / Mã giảm giá)
    var sectionButtons = document.querySelectorAll('[data-account-section-btn]');
    var sections = document.querySelectorAll('.tgdd-account-section');
    console.log('[account.js] sectionButtons length =', sectionButtons.length);
    console.log('[account.js] sections length =', sections.length);

    function activateSection(key) {
      if (!sectionButtons || !sections) {
        console.warn('[account.js] activateSection called but NodeLists not ready');
        return;
      }
      console.log('[account.js] activateSection ->', key);

      for (var i = 0; i < sectionButtons.length; i++) {
        var btn = sectionButtons[i];
        var targetBtn = btn.getAttribute('data-account-section-btn');
        if (targetBtn === key) {
          btn.classList.add('active');
        } else {
          btn.classList.remove('active');
        }
      }

      for (var j = 0; j < sections.length; j++) {
        var pane = sections[j];
        var targetPane = pane.getAttribute('data-account-section');
        if (targetPane === key) {
          pane.classList.add('tgdd-account-section--active');
        } else {
          pane.classList.remove('tgdd-account-section--active');
        }
      }
    }

    if (sectionButtons && sectionButtons.length > 0) {
      for (var k = 0; k < sectionButtons.length; k++) {
        (function (btn, index) {
          console.log('[account.js] bind click for sidebar btn', index, btn.getAttribute('data-account-section-btn'));
          btn.addEventListener('click', function () {
            var key = btn.getAttribute('data-account-section-btn') || 'orders';
            console.log('[account.js] sidebar click -> key =', key);
            activateSection(key);
          });
        })(sectionButtons[k], k);
      }
    }

    // Mặc định hiện section Đơn hàng đã mua
    activateSection('orders');

    // 2. Lọc đơn hàng theo trạng thái
    var filtersEl = document.getElementById('accountOrdersFilters');
    var listEl = document.getElementById('accountOrdersList');
    if (filtersEl && listEl) {
      var filterButtons = filtersEl.querySelectorAll('.tgdd-account-orders__filter');
      var orderCards = listEl.querySelectorAll('.tgdd-account-order-card');
      var allowedFilters = ['ALL', 'PENDING', 'SHIPPED', 'CANCELLED', 'PAID'];
      console.log('[account.js] filters found =', filterButtons.length, 'orders found =', orderCards.length);

      function applyOrderFilter(filter) {
        var effective = 'ALL';
        for (var a = 0; a < allowedFilters.length; a++) {
          if (allowedFilters[a] === filter) {
            effective = filter;
            break;
          }
        }

        if (!filterButtons || !orderCards) {
          console.warn('[account.js] applyOrderFilter: missing buttons or cards');
          return;
        }
        console.log('[account.js] applyOrderFilter ->', filter, 'effective =', effective);

        for (var i2 = 0; i2 < filterButtons.length; i2++) {
          var fb = filterButtons[i2];
          var f = (fb.getAttribute('data-filter') || 'ALL').toUpperCase();
          if (f === effective) {
            fb.classList.add('tgdd-account-orders__filter--active');
          } else {
            fb.classList.remove('tgdd-account-orders__filter--active');
          }
        }

        for (var j2 = 0; j2 < orderCards.length; j2++) {
          var card = orderCards[j2];
          var status = (card.getAttribute('data-status') || '').toUpperCase();
          var show = effective === 'ALL' || status === effective;
          card.style.display = show ? '' : 'none';
        }
      }

      if (filterButtons.length > 0) {
        for (var k2 = 0; k2 < filterButtons.length; k2++) {
          (function (btn, index) {
            console.log('[account.js] bind click for filter btn', index, btn.getAttribute('data-filter'));
            btn.addEventListener('click', function () {
              var filter = (btn.getAttribute('data-filter') || 'ALL').toUpperCase();
              console.log('[account.js] filter click ->', filter);
              applyOrderFilter(filter);
            });
          })(filterButtons[k2], k2);
        }
      }
    }

    // 3. Bật/tắt khung Sửa cho Sổ địa chỉ
    function toggleEdit(block, showEdit) {
      var personView = document.getElementById('accountPersonView');
      var personEdit = document.getElementById('accountPersonEdit');
      var addressView = document.getElementById('accountAddressView');
      var addressEdit = document.getElementById('accountAddressEdit');

      if (block === 'person') {
        if (personView) personView.hidden = showEdit;
        if (personEdit) personEdit.hidden = !showEdit;
      } else if (block === 'address') {
        if (addressView) addressView.hidden = showEdit;
        if (addressEdit) addressEdit.hidden = !showEdit;
      }
    }

    var editBtns = document.querySelectorAll('[data-edit-target]');
    if (editBtns && editBtns.length > 0) {
      for (var e = 0; e < editBtns.length; e++) {
        (function (btn, index) {
          console.log('[account.js] bind click for edit btn', index, btn.getAttribute('data-edit-target'));
          btn.addEventListener('click', function () {
            var target = btn.getAttribute('data-edit-target');
            console.log('[account.js] edit click ->', target);
            toggleEdit(target, true);
          });
        })(editBtns[e], e);
      }
    }

    var cancelBtns = document.querySelectorAll('[data-edit-cancel]');
    if (cancelBtns && cancelBtns.length > 0) {
      for (var c = 0; c < cancelBtns.length; c++) {
        (function (btn, index) {
          console.log('[account.js] bind click for cancel btn', index, btn.getAttribute('data-edit-cancel'));
          btn.addEventListener('click', function () {
            var target = btn.getAttribute('data-edit-cancel');
            console.log('[account.js] cancel click ->', target);
            toggleEdit(target, false);
          });
        })(cancelBtns[c], c);
      }
    }
  });
})();


const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function loadPublishFormPage(wxOverrides = {}) {
  const filePath = path.join(__dirname, '..', 'pages', 'publish-form', 'publish-form.js');
  const source = fs.readFileSync(filePath, 'utf8');
  let pageConfig;
  const wx = {
    chooseLocation() {},
    showToast() {},
    showModal() {},
    openSetting() {},
    ...wxOverrides
  };

  const context = vm.createContext({
    console,
    getApp: () => ({ globalData: {} }),
    Page: (config) => {
      pageConfig = config;
    },
    wx
  });

  vm.runInContext(source, context, { filename: filePath });
  return pageConfig;
}

function createPageInstance(wxOverrides = {}) {
  const config = loadPublishFormPage(wxOverrides);
  const instance = {
    data: structuredClone(config.data),
    setData(updates) {
      Object.entries(updates).forEach(([key, value]) => {
        const segments = key.split('.');
        let target = this.data;
        while (segments.length > 1) {
          const segment = segments.shift();
          target = target[segment];
        }
        target[segments[0]] = value;
      });
    }
  };

  Object.entries(config).forEach(([key, value]) => {
    if (typeof value === 'function') {
      instance[key] = value.bind(instance);
    }
  });

  return instance;
}

test('editing locationName after picking a map point clears stale coordinates', () => {
  const page = createPageInstance();
  page.setData({
    'form.locationName': '逸夫楼门口',
    'form.manualLocationName': '',
    'form.pickedLocationName': '逸夫楼门口',
    'form.longitude': 113.2931234,
    'form.latitude': 23.0961234
  });

  page.setField({
    currentTarget: { dataset: { field: 'locationName' } },
    detail: { value: '第二食堂二楼' }
  });

  assert.equal(page.data.form.locationName, '第二食堂二楼');
  assert.equal(page.data.form.manualLocationName, '第二食堂二楼');
  assert.equal(page.data.form.pickedLocationName, '');
  assert.equal(page.data.form.longitude, null);
  assert.equal(page.data.form.latitude, null);
});

test('canceling chooseLocation does not show the map reminder toast', () => {
  const calls = [];
  let chooseOptions;
  const page = createPageInstance({
    chooseLocation(options) {
      chooseOptions = options;
      options.fail({ errMsg: 'chooseLocation:fail cancel' });
    },
    showToast(payload) {
      calls.push(payload);
    }
  });

  page.chooseFoundLocation();

  assert.equal(chooseOptions.latitude, 23.06);
  assert.equal(chooseOptions.longitude, 113.39);
  assert.deepEqual(calls, []);
});

test('unexpected chooseLocation failure shows environment guidance', () => {
  const toastCalls = [];
  const page = createPageInstance({
    chooseLocation(options) {
      options.fail({ errMsg: 'chooseLocation:fail' });
    },
    showToast(payload) {
      toastCalls.push(payload);
    }
  });

  page.chooseFoundLocation();

  assert.equal(toastCalls.length, 1);
  assert.match(toastCalls[0].title, /真机预览/);
});

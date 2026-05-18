const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function loadMapPage(wxOverrides = {}, appOverrides = {}) {
  const filePath = path.join(__dirname, '..', 'pages', 'map', 'map.js');
  const source = fs.readFileSync(filePath, 'utf8');
  let pageConfig;
  const wx = {
    request() {},
    showToast() {},
    navigateTo() {},
    ...wxOverrides
  };

  const context = vm.createContext({
    console,
    getApp: () => ({
      globalData: {
        baseUrl: 'http://localhost:8080',
        ...appOverrides
      }
    }),
    Page: (config) => {
      pageConfig = config;
    },
    wx
  });

  vm.runInContext(source, context, { filename: filePath });
  return pageConfig;
}

function createPageInstance(wxOverrides = {}, appOverrides = {}) {
  const config = loadMapPage(wxOverrides, appOverrides);
  const instance = {
    data: structuredClone(config.data),
    setData(updates) {
      Object.entries(updates).forEach(([key, value]) => {
        this.data[key] = value;
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

test('map page keeps east campus as default center when no markers are returned', () => {
  const page = createPageInstance({
    request(options) {
      options.success({
        data: { code: 200, data: [] }
      });
      options.complete();
    }
  });

  page.loadMapPoints();

  assert.equal(page.data.latitude, 23.06);
  assert.equal(page.data.longitude, 113.39);
  assert.deepEqual(page.data.markers, []);
});

test('map page still starts from east campus even when markers exist', () => {
  const page = createPageInstance({
    request(options) {
      options.success({
        data: {
          code: 200,
          data: [
            {
              id: 1,
              itemName: '校园卡',
              itemCategory: '证件',
              campusArea: '东校区',
              locationName: '教学楼 A',
              latitude: 23.111,
              longitude: 113.411,
              eventTime: '2026-05-18T12:00:00'
            }
          ]
        }
      });
      options.complete();
    }
  });

  page.loadMapPoints();

  assert.equal(page.data.latitude, 23.06);
  assert.equal(page.data.longitude, 113.39);
  assert.equal(page.data.markers.length, 1);
});

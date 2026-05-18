const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function createSetData(instance) {
  return function setData(updates) {
    Object.entries(updates).forEach(([key, value]) => {
      const segments = key.split('.');
      let target = instance.data;
      while (segments.length > 1) {
        const segment = segments.shift();
        target = target[segment];
      }
      target[segments[0]] = value;
    });
  };
}

function loadPageConfig(filePath, { wxOverrides = {}, appOverrides = {} } = {}) {
  const source = fs.readFileSync(filePath, 'utf8');
  let pageConfig;
  const wx = {
    chooseMedia() {},
    uploadFile() {},
    previewImage() {},
    request() {},
    showToast() {},
    switchTab() {},
    setNavigationBarTitle() {},
    ...wxOverrides
  };

  const context = vm.createContext({
    console,
    getApp: () => ({
      globalData: {
        baseUrl: 'http://localhost:8080',
        token: 'token-123',
        ...appOverrides
      }
    }),
    Page: (config) => {
      pageConfig = config;
    },
    wx,
    setTimeout: (fn) => {
      fn();
      return 0;
    }
  });

  vm.runInContext(source, context, { filename: filePath });
  return pageConfig;
}

function createPublishFormPage({ wxOverrides = {}, appOverrides = {} } = {}) {
  const filePath = path.join(__dirname, '..', 'pages', 'publish-form', 'publish-form.js');
  const config = loadPageConfig(filePath, { wxOverrides, appOverrides });
  const instance = {
    data: structuredClone(config.data)
  };
  instance.setData = createSetData(instance);

  Object.entries(config).forEach(([key, value]) => {
    if (typeof value === 'function') {
      instance[key] = value.bind(instance);
    }
  });

  return instance;
}

function createDetailPage({ wxOverrides = {}, appOverrides = {} } = {}) {
  const filePath = path.join(__dirname, '..', 'pages', 'detail', 'detail.js');
  const config = loadPageConfig(filePath, { wxOverrides, appOverrides });
  const instance = {
    data: structuredClone(config.data)
  };
  instance.setData = createSetData(instance);

  Object.entries(config).forEach(([key, value]) => {
    if (typeof value === 'function') {
      instance[key] = value.bind(instance);
    }
  });

  return instance;
}

test('publish form limits images to at most three selections', () => {
  let chooseMediaOptions;
  const toastCalls = [];
  const page = createPublishFormPage({
    wxOverrides: {
      chooseMedia(options) {
        chooseMediaOptions = options;
        options.success({
          tempFiles: [
            { tempFilePath: '/tmp/2.jpg' },
            { tempFilePath: '/tmp/3.jpg' },
            { tempFilePath: '/tmp/4.jpg' }
          ]
        });
      },
      showToast(payload) {
        toastCalls.push(payload);
      }
    }
  });

  page.setData({
    'form.imageFiles': [{ tempFilePath: '/tmp/1.jpg' }]
  });

  page.chooseImages();

  assert.equal(chooseMediaOptions.count, 2);
  assert.deepEqual(
    page.data.form.imageFiles.map((file) => file.tempFilePath),
    ['/tmp/1.jpg', '/tmp/2.jpg', '/tmp/3.jpg']
  );
  assert.equal(toastCalls.length, 1);
  assert.match(toastCalls[0].title, /最多上传 3 张/);
});

test('removing an image keeps file and url arrays in sync', () => {
  const page = createPublishFormPage();
  page.setData({
    'form.imageFiles': [
      { tempFilePath: '/tmp/1.jpg' },
      { tempFilePath: '/tmp/2.jpg' },
      { tempFilePath: '/tmp/3.jpg' }
    ],
    'form.imageUrls': ['https://cdn/1.jpg', 'https://cdn/2.jpg', 'https://cdn/3.jpg']
  });

  page.removeImage({
    currentTarget: {
      dataset: { index: 1 }
    }
  });

  assert.deepEqual(
    page.data.form.imageFiles.map((file) => file.tempFilePath),
    ['/tmp/1.jpg', '/tmp/3.jpg']
  );
  assert.deepEqual(page.data.form.imageUrls, ['https://cdn/1.jpg', 'https://cdn/3.jpg']);
});

test('submit uploads images first and posts returned imageUrls in payload', async () => {
  const uploadCalls = [];
  const requestCalls = [];
  const page = createPublishFormPage({
    wxOverrides: {
      uploadFile(options) {
        uploadCalls.push({
          url: options.url,
          filePath: options.filePath,
          header: options.header,
          name: options.name
        });
        options.success({
          statusCode: 200,
          data: JSON.stringify({
            code: 200,
            data: {
              url: `https://cdn.example.com/${path.basename(options.filePath)}`
            }
          })
        });
      },
      request(options) {
        requestCalls.push(options);
        options.success({
          data: { code: 200 }
        });
        options.complete();
      },
      showToast() {},
      switchTab() {}
    }
  });

  page.setData({
    postType: 'FOUND',
    'form.itemName': '校园卡',
    'form.itemCategory': '校园卡',
    'form.campusArea': '东校区',
    'form.locationName': '教学楼 A',
    'form.eventTime': '2026-05-18',
    'form.longitude': 113.39,
    'form.latitude': 23.06,
    'form.imageFiles': [
      { tempFilePath: '/tmp/1.jpg' },
      { tempFilePath: '/tmp/2.jpg' }
    ]
  });

  await page.submit();

  assert.equal(uploadCalls.length, 2);
  assert.equal(uploadCalls[0].url, 'http://localhost:8080/api/files/upload');
  assert.equal(uploadCalls[0].header.satoken, 'token-123');
  assert.equal(uploadCalls[0].name, 'file');
  assert.equal(requestCalls.length, 1);
  assert.deepEqual(Array.from(requestCalls[0].data.imageUrls), [
    'https://cdn.example.com/1.jpg',
    'https://cdn.example.com/2.jpg'
  ]);
});

test('detail preview uses tapped image as current within full gallery', () => {
  const previewCalls = [];
  const page = createDetailPage({
    wxOverrides: {
      previewImage(payload) {
        previewCalls.push(payload);
      }
    }
  });

  page.setData({
    post: {
      imageUrls: ['https://cdn/1.jpg', 'https://cdn/2.jpg', 'https://cdn/3.jpg']
    }
  });

  page.previewImage({
    currentTarget: {
      dataset: { index: 1 }
    }
  });

  assert.deepEqual(JSON.parse(JSON.stringify(previewCalls)), [
    {
      current: 'https://cdn/2.jpg',
      urls: ['https://cdn/1.jpg', 'https://cdn/2.jpg', 'https://cdn/3.jpg']
    }
  ]);
});

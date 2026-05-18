USE shi_gui;

-- 测试管理员 (密码: admin123)
-- 存储格式: salt:SHA256(salt + SHA256(password))。前端先 SHA-256 再发送，避免明文传输。
-- 生成方法见 AdminUserServiceImpl.verifyPassword
INSERT INTO admin_user (username, password_hash)
VALUES ('admin', '1Kgtp+nNHV4Nignq9aOyjw==:cee2ba654e2a72de504be626be656b575bcef50fa834990fc097fd493c9f6584')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);

-- 固定的本地种子用户，可重复执行
INSERT INTO app_user (openid, nickname, role, status, deleted)
VALUES
('seed_post_user_001', '东校区失主', 'USER', 'NORMAL', 0),
('seed_post_user_002', '东校区拾主', 'USER', 'NORMAL', 0),
('seed_post_user_003', '南北校测试', 'USER', 'NORMAL', 0),
('seed_post_user_004', '珠海校区测试', 'USER', 'NORMAL', 0)
ON DUPLICATE KEY UPDATE
nickname = VALUES(nickname),
role = VALUES(role),
status = VALUES(status),
deleted = VALUES(deleted);

-- 清理这些种子用户之前插入过的单据，方便重复执行
DELETE FROM lost_found_post
WHERE user_id IN (
    SELECT id FROM app_user
    WHERE openid IN (
        'seed_post_user_001',
        'seed_post_user_002',
        'seed_post_user_003',
        'seed_post_user_004'
    )
);

-- 20 条寻物单 + 20 条招领单
INSERT INTO lost_found_post (
    user_id,
    post_type,
    title,
    item_name,
    item_category,
    description,
    private_feature,
    campus_area,
    location_name,
    longitude,
    latitude,
    storage_location,
    event_time,
    status,
    published_at,
    deleted
)
VALUES
((SELECT id FROM app_user WHERE openid = 'seed_post_user_001'), 'LOST', '丢失校园卡，卡套是绿色的', '校园卡', '校园卡', '中午在教学楼和食堂之间走动后发现不见了。', '卡号后四位 3812，绿色硅胶卡套', '东校区', '教学楼 A 与行政楼之间', NULL, NULL, '', '2026-05-18 11:20:00', 'MATCHING', '2026-05-18 11:35:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'LOST', '学生证可能落在图书馆自习区', '学生证', '学生证', '昨晚在图书馆三楼自习，回宿舍后发现学生证不见。', '证件照是蓝底，封套右下角有裂纹', '东校区', '图书馆三楼自习区', NULL, NULL, '', '2026-05-17 21:10:00', 'MATCHING', '2026-05-18 10:40:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_001'), 'LOST', '丢了一串宿舍和实验室钥匙', '钥匙', '钥匙', '一串三把钥匙，可能落在共享单车停车点附近。', '黑色伸缩钥匙扣，挂着一个小熊挂件', '东校区', '生活区共享单车停车点', NULL, NULL, '', '2026-05-18 08:40:00', 'MATCHING', '2026-05-18 09:00:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'LOST', 'AirPods 充电盒丢了', '耳机', '耳机', '只有白色充电盒，不含耳机本体。', '盒盖内侧贴着一张很小的课程表贴纸', '东校区', '操场看台下方长椅', NULL, NULL, '', '2026-05-16 18:25:00', 'MATCHING', '2026-05-18 08:45:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_001'), 'LOST', '蓝色保温杯遗失', '水杯', '水杯', '杯身是深蓝色，杯盖有磨损。', '杯底贴着姓名缩写 LQ', '东校区', '第二食堂二楼靠窗位置', NULL, NULL, '', '2026-05-18 12:15:00', 'MATCHING', '2026-05-18 12:50:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'LOST', '黑色长柄雨伞不见了', '雨伞', '雨伞', '今天早上下课后忘记带走。', '伞带上有一圈白色线头', '东校区', '公共教学楼 C 栋一楼门口', NULL, NULL, '', '2026-05-18 09:10:00', 'MATCHING', '2026-05-18 09:25:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_001'), 'LOST', '离散数学教材丢失', '教材', '书籍', '封面有透明书皮，里面夹着复习提纲。', '第 35 页有荧光笔重点和手写公式', '东校区', '教学楼 B 402', NULL, NULL, '', '2026-05-17 16:20:00', 'MATCHING', '2026-05-18 08:10:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'LOST', '白色充电宝找不到了', '充电宝', '其他', '一万毫安，侧边有轻微磕碰。', '机身贴有一张社团贴纸', '东校区', '宿舍楼自习室', NULL, NULL, '', '2026-05-17 22:30:00', 'MATCHING', '2026-05-18 07:50:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_001'), 'LOST', '银色 U 盘遗失', 'U 盘', '其他', '金属外壳，有伸缩扣。', '里面存有课设文件，外壳刻着 64G', '东校区', '实验楼一层打印店', NULL, NULL, '', '2026-05-18 14:05:00', 'MATCHING', '2026-05-18 14:20:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'LOST', '黑色双肩包落下了', '双肩包', '其他', '包里有笔记本和水杯，前袋放着纸巾。', '肩带处缝了一个红色姓名贴', '东校区', '教学楼 D 201 教室', NULL, NULL, '', '2026-05-18 15:00:00', 'MATCHING', '2026-05-18 15:20:00', 0),

((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'LOST', '南校区丢失校园卡', '校园卡', '校园卡', '午饭后从饭堂回宿舍路上丢失。', '卡套是透明的，内夹一张电影票', '南校区', '第三饭堂到宿舍路口', NULL, NULL, '', '2026-05-18 13:10:00', 'MATCHING', '2026-05-18 13:30:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_001'), 'LOST', '学生证可能掉在逸夫楼', '学生证', '学生证', '下午上完课离开时发现学生证不见。', '证件套是深蓝色，背后夹着一张快递单号', '南校区', '逸夫楼四楼走廊', NULL, NULL, '', '2026-05-17 17:40:00', 'MATCHING', '2026-05-18 09:50:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'LOST', '实验室钥匙丢失', '钥匙', '钥匙', '只有一把钥匙，挂着白色塑料标签。', '标签上写着 302-B', '南校区', '化学实验楼门前台阶', NULL, NULL, '', '2026-05-18 10:05:00', 'MATCHING', '2026-05-18 10:25:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_001'), 'LOST', '有线耳机遗失', '耳机', '耳机', '黑色有线耳机，收纳时可能落在桌洞。', '线控按键有裂痕', '南校区', '公共教学楼 208', NULL, NULL, '', '2026-05-16 19:15:00', 'MATCHING', '2026-05-18 08:20:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_004'), 'LOST', '珠海校区掉了米白色水杯', '水杯', '水杯', '杯子是米白色，带提手。', '杯身写着 Today is a good day', '珠海校区', '海琴楼一楼大厅', NULL, NULL, '', '2026-05-18 09:45:00', 'MATCHING', '2026-05-18 10:00:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_004'), 'LOST', '珠海校区黑胶伞遗失', '雨伞', '雨伞', '黑色折叠伞，伞柄是木质的。', '伞套侧边有一个小白点', '珠海校区', '图书馆门口寄存柜附近', NULL, NULL, '', '2026-05-17 18:00:00', 'MATCHING', '2026-05-18 08:05:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'LOST', '北校区药理学教材丢失', '教材', '书籍', '白底蓝字封面，里面夹着实验报告。', '扉页写着手机号尾号 0621', '北校区', '教学楼 E207', NULL, NULL, '', '2026-05-18 11:00:00', 'MATCHING', '2026-05-18 11:18:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_004'), 'LOST', '珠海校区 U 盘丢了', 'U 盘', '其他', '黑红配色，带挂绳。', '挂绳上串着一个银色小圈', '珠海校区', '实验楼打印区', NULL, NULL, '', '2026-05-18 14:40:00', 'MATCHING', '2026-05-18 14:55:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'LOST', '北校区白色充电宝遗失', '充电宝', '其他', '边角有使用痕迹，按键略松。', '外壳贴着卡通海豹贴纸', '北校区', '宿舍区快递站', NULL, NULL, '', '2026-05-18 16:10:00', 'MATCHING', '2026-05-18 16:20:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_004'), 'LOST', '珠海校区帆布袋找不到了', '帆布袋', '其他', '浅色帆布袋，里面有一本笔记本。', '袋口别着一枚绿色胸针', '珠海校区', '体育馆入口座椅区', NULL, NULL, '', '2026-05-18 17:30:00', 'MATCHING', '2026-05-18 17:45:00', 0),

((SELECT id FROM app_user WHERE openid = 'seed_post_user_002'), 'FOUND', '在东校区捡到一张校园卡', '校园卡', '校园卡', '在教学楼门口地面捡到，已擦拭收好。', '卡号后四位 3812，绿色硅胶卡套', '东校区', '教学楼 A 门口', 113.3981200, 23.0624100, '东校区保卫处前台', '2026-05-18 11:28:00', 'MATCHING', '2026-05-18 11:40:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_002'), 'FOUND', '图书馆拾到学生证一张', '学生证', '学生证', '在自习区靠窗位置发现，已先帮忙保管。', '证件照是蓝底，封套右下角有裂纹', '东校区', '图书馆三楼靠窗座位', 113.3974300, 23.0642800, '图书馆服务台', '2026-05-17 21:20:00', 'MATCHING', '2026-05-18 10:50:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_002'), 'FOUND', '捡到一串钥匙', '钥匙', '钥匙', '在共享单车集中停放区旁边看到。', '黑色伸缩钥匙扣，挂着一个小熊挂件', '东校区', '生活区共享单车停放点', 113.4012200, 23.0597300, '一站式服务中心失物柜', '2026-05-18 08:45:00', 'MATCHING', '2026-05-18 09:10:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_002'), 'FOUND', '看台下捡到白色耳机盒', '耳机', '耳机', '看起来像 AirPods 充电盒，里面没有耳机。', '盒盖内侧贴着一张很小的课程表贴纸', '东校区', '操场看台下方第二排长椅', 113.3958800, 23.0586400, '体育馆前台', '2026-05-16 18:40:00', 'MATCHING', '2026-05-18 08:55:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_002'), 'FOUND', '二食堂有人落下蓝色保温杯', '水杯', '水杯', '在靠窗餐桌上看到，暂时没有人回来找。', '杯底贴着姓名缩写 LQ', '东校区', '第二食堂二楼靠窗位', 113.4006400, 23.0618400, '第二食堂服务台', '2026-05-18 12:18:00', 'MATCHING', '2026-05-18 12:55:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_002'), 'FOUND', '教学楼门口拾到黑色雨伞', '雨伞', '雨伞', '放学时发现一把没人拿的黑色长柄伞。', '伞带上有一圈白色线头', '东校区', '公共教学楼 C 栋门口', 113.3969300, 23.0635200, '公共教学楼值班室', '2026-05-18 09:15:00', 'MATCHING', '2026-05-18 09:35:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_002'), 'FOUND', '教室里捡到一本离散数学教材', '教材', '书籍', '课后清理教室时发现，先放在讲台边。', '第 35 页有荧光笔重点和手写公式', '东校区', '教学楼 B 402 讲台旁', 113.3992000, 23.0609100, '教学楼 B 失物盒', '2026-05-17 16:35:00', 'MATCHING', '2026-05-18 08:18:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_002'), 'FOUND', '宿舍自习室有白色充电宝', '充电宝', '其他', '桌面清理时发现，无人认领。', '机身贴有一张社团贴纸', '东校区', '宿舍楼自习室 3 号桌', 113.4020100, 23.0589900, '宿管阿姨处', '2026-05-17 22:35:00', 'MATCHING', '2026-05-18 08:00:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_002'), 'FOUND', '打印店地上捡到银色 U 盘', 'U 盘', '其他', '体积不大，应该是学习资料用的。', '金属外壳刻着 64G', '东校区', '实验楼一层打印店门口', 113.3978600, 23.0612600, '打印店老板代管', '2026-05-18 14:10:00', 'MATCHING', '2026-05-18 14:25:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_002'), 'FOUND', '教室后排有黑色双肩包', '双肩包', '其他', '上完课后还留在位置上，里面没翻动。', '肩带处缝了一个红色姓名贴', '东校区', '教学楼 D 201 后排座位', 113.3987600, 23.0630400, '教学楼 D 值班室', '2026-05-18 15:05:00', 'MATCHING', '2026-05-18 15:30:00', 0),

((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'FOUND', '南校区捡到一张校园卡', '校园卡', '校园卡', '从饭堂出来时在地上看到。', '卡套是透明的，内夹一张电影票', '南校区', '第三饭堂到宿舍路口', 113.3008200, 23.0921700, '南校区保卫处', '2026-05-18 13:15:00', 'MATCHING', '2026-05-18 13:40:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'FOUND', '逸夫楼走廊拾到学生证', '学生证', '学生证', '四楼走廊靠窗台位置发现。', '证件套是深蓝色，背后夹着一张快递单号', '南校区', '逸夫楼四楼走廊', 113.2986400, 23.0940500, '逸夫楼值班室', '2026-05-17 17:50:00', 'MATCHING', '2026-05-18 10:05:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'FOUND', '化学实验楼前捡到钥匙', '钥匙', '钥匙', '只有一把，带白色标签。', '标签上写着 302-B', '南校区', '化学实验楼门前台阶', 113.2979100, 23.0932100, '实验楼门卫处', '2026-05-18 10:10:00', 'MATCHING', '2026-05-18 10:30:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'FOUND', '教室课桌里有黑色有线耳机', '耳机', '耳机', '下节课上课前发现的。', '线控按键有裂痕', '南校区', '公共教学楼 208 靠后排课桌', 113.3014600, 23.0916200, '教学楼失物箱', '2026-05-16 19:20:00', 'MATCHING', '2026-05-18 08:28:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_004'), 'FOUND', '海琴楼大厅拾到米白色水杯', '水杯', '水杯', '前台附近看到，已经放好。', '杯身写着 Today is a good day', '珠海校区', '海琴楼一楼大厅', 113.5729100, 22.3501800, '海琴楼服务台', '2026-05-18 09:48:00', 'MATCHING', '2026-05-18 10:08:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_004'), 'FOUND', '图书馆门口捡到黑胶折叠伞', '雨伞', '雨伞', '寄存柜旁边放着没人拿。', '伞柄是木质的，伞套侧边有小白点', '珠海校区', '图书馆门口寄存柜附近', 113.5738800, 22.3510400, '图书馆服务台', '2026-05-17 18:10:00', 'MATCHING', '2026-05-18 08:15:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'FOUND', '北校区教室发现一本药理学教材', '教材', '书籍', '下课后保洁阿姨交到值班处。', '扉页写着手机号尾号 0621', '北校区', '教学楼 E207', 113.3054200, 23.1283300, '北校区教学楼值班室', '2026-05-18 11:05:00', 'MATCHING', '2026-05-18 11:22:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_004'), 'FOUND', '实验楼打印区捡到黑红 U 盘', 'U 盘', '其他', '地上捡到，已经交给老师。', '挂绳上串着一个银色小圈', '珠海校区', '实验楼打印区门口', 113.5717300, 22.3495400, '实验楼辅导员办公室', '2026-05-18 14:45:00', 'MATCHING', '2026-05-18 15:00:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_003'), 'FOUND', '快递站有人落下白色充电宝', '充电宝', '其他', '快递架旁边找到的。', '外壳贴着卡通海豹贴纸', '北校区', '宿舍区快递站取件口', 113.3046500, 23.1269200, '快递站服务台', '2026-05-18 16:12:00', 'MATCHING', '2026-05-18 16:28:00', 0),
((SELECT id FROM app_user WHERE openid = 'seed_post_user_004'), 'FOUND', '体育馆座椅上有浅色帆布袋', '帆布袋', '其他', '袋里有笔记本，未翻看内容。', '袋口别着一枚绿色胸针', '珠海校区', '体育馆入口左侧座椅区', 113.5742100, 22.3488800, '体育馆前台', '2026-05-18 17:35:00', 'MATCHING', '2026-05-18 17:50:00', 0);

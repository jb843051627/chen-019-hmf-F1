-- hmf 住宅专项维修资金交存与使用监督管理 -- schema (chen-019)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_019

CREATE TABLE IF NOT EXISTS t_hmf_acct_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '分户账立户单号',
  node_no int DEFAULT NULL COMMENT '本单版次序',
  site_id int DEFAULT NULL COMMENT '所属分户底册',
  site_no varchar(64) DEFAULT NULL COMMENT '所属底册代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本户建筑面积(平方米)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本户应缴额(元)',
  content varchar(255) DEFAULT NULL COMMENT '校验串（随版次序走）',
  grade_level int DEFAULT NULL COMMENT '面积折到的收费档次',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已冻住',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分户账立户单';

CREATE TABLE IF NOT EXISTS t_hmf_arch (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '分户底册代号',
  site_name varchar(128) DEFAULT NULL COMMENT '小区（项目）全称',
  site_type varchar(32) DEFAULT NULL COMMENT '项目类别',
  road_name varchar(128) DEFAULT NULL COMMENT '落在哪一级辖区(市—区—街道)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '户建面低界(平方米)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '中界(平方米)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '高界(平方米)',
  status int DEFAULT NULL COMMENT '底册情形 0在册 1已迁出',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分户底册';

CREATE TABLE IF NOT EXISTS t_hmf_budget_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '拨付核签单号',
  site_id bigint DEFAULT NULL COMMENT '所属分户底册id',
  site_no varchar(64) DEFAULT NULL COMMENT '所属底册代号（立单时自底册誊定，不随底册改版）',
  bill_year int DEFAULT NULL COMMENT '核签年度（同一小区同一年再来的另起新单，不并单）',
  rule_code varchar(64) DEFAULT NULL COMMENT '拨付额取数所用支出审定额线代号',
  pay_amt decimal(12,2) DEFAULT NULL COMMENT '拨付额（自立单时一次性从审定额线算定，屏显与库存同源，禁手填）',
  drafter varchar(64) DEFAULT NULL COMMENT '立单经手人（起单到归档不变，重录不换；与update_by一格分开）',
  node_no int DEFAULT '0' COMMENT '当前所在口次 0业委会核 1事务所核 2中心核 3已封单',
  round_no int DEFAULT '1' COMMENT '签认拨次 首拨为1；每退一档+1，新旧拨署名分存',
  status int DEFAULT '0' COMMENT '报批情形 0在核 1已核讫 2已打回（重签落笔即回0）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者（谁实际操作续谁，跟着事务走）',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  UNIQUE KEY uk_bill_no (bill_no),
  KEY idx_site_year (site_id, bill_year),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算审定拨付核签单';

-- 每单三行：业委会(0)/事务所(1)/中心(2)，各档并签算法在建单时定死，不可改。
-- sign_mode：0一笔即可（头一笔落定本档即过）；1两笔凑齐（两笔都落才算过，只落一笔仍候签）。
-- sign_count 只记“当前拨次”已落的实笔数，永远由署名流水逐笔 COUNT 回填，不认人手自报数。
CREATE TABLE IF NOT EXISTS t_hmf_budget_node (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint NOT NULL COMMENT '所属拨付核签单',
  node_no int NOT NULL COMMENT '口次 0业委会核 1事务所核 2中心核',
  sign_mode int NOT NULL COMMENT '同口并签办法 0一笔即可 1两笔凑齐（建单定死）',
  need_count int NOT NULL COMMENT '本档应落名数（与点名册条数一致）',
  sign_count int NOT NULL DEFAULT '0' COMMENT '本档当前拨次已落名数（逐笔COUNT回填，仅参照用不作判定依据）',
  passed int NOT NULL DEFAULT '0' COMMENT '本档在当前拨次过没过 0候签 1已过',
  pass_round int DEFAULT NULL COMMENT '本档最近一次过档所在拨次',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  UNIQUE KEY uk_bill_node (bill_id, node_no),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拨付核签各档口径与状态';

-- 点名册：每档该哪几个人签，建单时点定，条数须与应落名数相等。
CREATE TABLE IF NOT EXISTS t_hmf_budget_roster (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint NOT NULL COMMENT '所属拨付核签单',
  node_no int NOT NULL COMMENT '口次 0业委会核 1事务所核 2中心核',
  cand_name varchar(64) NOT NULL COMMENT '被点名人姓名',
  seq_no int NOT NULL DEFAULT '1' COMMENT '本档点名次序',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  UNIQUE KEY uk_bill_node_cand (bill_id, node_no, cand_name),
  KEY idx_bill_node (bill_id, node_no),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拨付核签点名册';

-- 逐笔署名流水：只追加、不删改。退回不抹名字，靠 round_no 把新旧两拨分开存档。
-- action：1署名 2本档退回；create_by 记实际操作人（后一格，跟着事务走），signer 记落下的署名。
CREATE TABLE IF NOT EXISTS t_hmf_budget_sign (
  id bigint NOT NULL COMMENT '主键',
  bill_id bigint NOT NULL COMMENT '所属拨付核签单',
  node_no int NOT NULL COMMENT '落名/退回所在口次 0业委会核 1事务所核 2中心核',
  round_no int NOT NULL COMMENT '第几拨签的（首拨1，退一档+1）',
  signer varchar(64) NOT NULL COMMENT '署名（须在本档点名册内，逐笔对点名）',
  action int NOT NULL DEFAULT '1' COMMENT '流水动作 1署名 2退回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '实际操作人',
  create_time datetime DEFAULT NULL COMMENT '创建时间（几时补的）',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  KEY idx_bill_node_round (bill_id, node_no, round_no),
  UNIQUE KEY uk_bill_node_round_signer (bill_id, node_no, round_no, signer, action),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='拨付核签署名流水（分拨存档）';

CREATE TABLE IF NOT EXISTS t_hmf_cycle_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '结息催交单号',
  due_at datetime DEFAULT NULL COMMENT '该动手那一日的止点时刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '可提前几日开口催办',
  content varchar(255) DEFAULT NULL COMMENT '事由与被催分户底册记要',
  status int DEFAULT NULL COMMENT '条目情形 0候办 1已办结 2催不成',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='结息催交单';

CREATE TABLE IF NOT EXISTS t_hmf_pay_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '银行交存汇总册次号',
  row_no int DEFAULT NULL COMMENT '原册行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的分户账户号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行到帐金额',
  status int DEFAULT NULL COMMENT '行落地情形 0待销 1已销账 2挂起',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='银行交存汇总册核销行';

CREATE TABLE IF NOT EXISTS t_hmf_spend_line (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '支出审定额线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '低界金额',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '中界金额',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '高界金额',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '让线顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0现行 1已停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支出审定额线';

CREATE TABLE IF NOT EXISTS t_hmf_use_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '使用申请单号',
  stage int DEFAULT NULL COMMENT '当前段次 0..3（立项/公示/施工/结算）',
  status int DEFAULT NULL COMMENT '申领会落 0未起 1在办 2已作废',
  content varchar(255) DEFAULT NULL COMMENT '经办记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='维修资金使用申请单';

-- 初始档案数据（首条为启用态、第二条为停用态）
-- t_hmf_arch 两条种子底册：id=0 在册、id=1 已迁出（用于挂接与拦截的联动核对）。
-- 单户建面三线上限取 90/144/300 平方米（等于上限的情况计入上一层）。
INSERT IGNORE INTO t_hmf_arch (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'JZ00', '澜川市分户底册（梧桐里苑小区，在册）', '商品房', '澜川市—河湾区—长街上', 90.00, 144.00, 300.00, 0, 0, 'seed', NOW()),
       (1, 'JZ01', '澜川市分户底册（滨河北里项目，已迁出）', '保障性住房', '澜川市—河湾区—滨河北', 90.00, 144.00, 300.00, 1, 0, 'seed', NOW());

-- 拨付核签菜单与按钮权限（固定大ID，INSERT IGNORE 幂等；授给管理员角色 488243256161730560）
INSERT IGNORE INTO t_sys_permission
(id, name, descripion, url, is_blank, pid, perms, type, icon, order_num, visible, create_by, create_time)
VALUES
 (910000000000000001, '拨付核签', '拨付核签台账', '/hmfBudgetBill/view', 0, 0, 'hmfBudgetBill:view', 1, 'layui-icon layui-icon-read', 60, 0, 'seed', NOW()),
 (910000000000000002, '拨付核签台账集合', '拨付核签台账集合', '/hmfBudgetBill/list', 0, 910000000000000001, 'hmfBudgetBill:list', 2, '', NULL, 0, 'seed', NOW()),
 (910000000000000003, '拨付核签另立新单', '拨付核签另立新单', '/hmfBudgetBill/add', 0, 910000000000000001, 'hmfBudgetBill:add', 2, 'layui-icon layui-icon-add-1', NULL, 0, 'seed', NOW()),
 (910000000000000004, '拨付核签落名', '当前档落一笔署名', '/hmfBudgetBill/sign', 0, 910000000000000001, 'hmfBudgetBill:sign', 2, 'layui-icon layui-icon-ok', NULL, 0, 'seed', NOW()),
 (910000000000000005, '拨付核签退回', '当前档退回上一档', '/hmfBudgetBill/reject', 0, 910000000000000001, 'hmfBudgetBill:reject', 2, 'layui-icon layui-icon-return', NULL, 0, 'seed', NOW());

INSERT IGNORE INTO t_sys_permission_role (id, role_id, permission_id, create_by, create_time)
VALUES
 (910000000000000101, 488243256161730560, 910000000000000001, 'seed', NOW()),
 (910000000000000102, 488243256161730560, 910000000000000002, 'seed', NOW()),
 (910000000000000103, 488243256161730560, 910000000000000003, 'seed', NOW()),
 (910000000000000104, 488243256161730560, 910000000000000004, 'seed', NOW()),
 (910000000000000105, 488243256161730560, 910000000000000005, 'seed', NOW());


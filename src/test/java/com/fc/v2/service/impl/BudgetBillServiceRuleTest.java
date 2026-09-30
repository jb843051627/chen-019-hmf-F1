package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.mapper.auto.THmfArchMapper;
import com.fc.v2.mapper.auto.THmfBudgetBillMapper;
import com.fc.v2.mapper.auto.THmfBudgetNodeMapper;
import com.fc.v2.mapper.auto.THmfBudgetRosterMapper;
import com.fc.v2.mapper.auto.THmfBudgetSignMapper;
import com.fc.v2.model.auto.THmfArch;
import com.fc.v2.model.auto.THmfBudgetBill;
import com.fc.v2.model.auto.THmfBudgetNode;
import com.fc.v2.model.auto.THmfBudgetRoster;
import com.fc.v2.model.auto.THmfBudgetSign;
import com.fc.v2.model.custom.BudgetBillVo;
import com.fc.v2.service.ITHmfSpendLineService;
import com.fc.v2.service.impl.THmfBudgetBillServiceImpl.BudgetRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 拨付核签口径自测：不起容器，用内存假库把单据从头走到封，再退回重走一遍。
 * 核对《拨付核签口径提示》三手顺序、两笔凑齐/一笔即可、逐笔计数、退回分拨、封单、立单口径。
 */
public class BudgetBillServiceRuleTest {

    private THmfBudgetBillServiceImpl service;
    private FakeDb db;

    /** 业委会两个点名人 */
    private static final List<String> OWNERS = Arrays.asList("张业委", "李业委");
    private static final String OFFICER = "王事务";
    private static final String CENTER = "赵中心";

    @BeforeEach
    void setUp() {
        service = new THmfBudgetBillServiceImpl();
        db = new FakeDb();

        THmfBudgetBillMapper billMapper = Mockito.mock(THmfBudgetBillMapper.class);
        THmfBudgetNodeMapper nodeMapper = Mockito.mock(THmfBudgetNodeMapper.class);
        THmfBudgetRosterMapper rosterMapper = Mockito.mock(THmfBudgetRosterMapper.class);
        THmfBudgetSignMapper signMapper = Mockito.mock(THmfBudgetSignMapper.class);
        THmfArchMapper archMapper = Mockito.mock(THmfArchMapper.class);
        stub(billMapper, THmfBudgetBill.class, db);
        stub(nodeMapper, THmfBudgetNode.class, db);
        stub(rosterMapper, THmfBudgetRoster.class, db);
        stub(signMapper, THmfBudgetSign.class, db);
        stub(archMapper, THmfArch.class, db);

        THmfArch arch0 = new THmfArch();
        arch0.setId(0L);
        arch0.setSiteNo("JZ00");
        arch0.setSiteName("梧桐里苑");
        arch0.setStatus(0);
        arch0.setDelFlag(0);
        db.seed(arch0);
        THmfArch arch1 = new THmfArch();
        arch1.setId(1L);
        arch1.setSiteNo("JZ01");
        arch1.setStatus(1);
        arch1.setDelFlag(0);
        db.seed(arch1);

        ITHmfSpendLineService spendLineService = Mockito.mock(ITHmfSpendLineService.class);
        when(spendLineService.amountOf(any(), any(), any())).thenAnswer(inv -> {
            String code = inv.getArgument(0);
            return "NONE".equals(code) ? null : new BigDecimal("50000.00");
        });

        ReflectionTestUtils.setField(service, "billMapper", billMapper);
        ReflectionTestUtils.setField(service, "nodeMapper", nodeMapper);
        ReflectionTestUtils.setField(service, "rosterMapper", rosterMapper);
        ReflectionTestUtils.setField(service, "signMapper", signMapper);
        ReflectionTestUtils.setField(service, "archMapper", archMapper);
        ReflectionTestUtils.setField(service, "spendLineService", spendLineService);
    }

    private THmfBudgetBill newBill() {
        THmfBudgetBill bill = new THmfBudgetBill();
        bill.setSiteId(0L);
        bill.setBillYear(2026);
        bill.setRuleCode("LINE1");
        bill.setPayInput(new BigDecimal("100"));
        bill.setDrafter("钱立单");
        return bill;
    }

    private List<List<String>> roster() {
        return Arrays.asList(OWNERS, Arrays.asList(OFFICER), Arrays.asList(CENTER));
    }

    // ---------------------------------------------------------------- 立单口径

    @Test
    void create_fixedModesAndCountsAndPayAmount() {
        THmfBudgetBill bill = service.createBill(newBill(), roster());
        assertNotNull(bill.getId());
        assertEquals(0, bill.getNodeNo(), "从业委会档起");
        assertEquals(1, bill.getRoundNo(), "首拨为1");
        assertEquals(0, bill.getStatus());
        assertEquals("JZ00", bill.getSiteNo(), "底册代号立单誊定");
        assertEquals(new BigDecimal("50000.00"), bill.getPayAmt(), "拨付额只从审定额取这一回算");

        List<THmfBudgetNode> nodes = db.rows(THmfBudgetNode.class);
        assertEquals(3, nodes.size());
        assertEquals(1, nodes.get(0).getSignMode(), "业委会两笔凑齐");
        assertEquals(2, nodes.get(0).getNeedCount());
        assertEquals(0, nodes.get(1).getSignMode(), "事务所一笔即可");
        assertEquals(1, nodes.get(1).getNeedCount());
        assertEquals(0, nodes.get(2).getSignMode(), "中心一笔即可");
        assertEquals(1, nodes.get(2).getNeedCount());
        assertEquals(2 + 1 + 1, db.rows(THmfBudgetRoster.class).size(), "点名条数与应到数对齐");
    }

    @Test
    void create_ownerNeedsExactlyTwoDistinct() {
        BudgetRuleException ex = assertThrows(BudgetRuleException.class,
                () -> service.createBill(newBill(),
                        Arrays.asList(Arrays.asList("张业委"), Arrays.asList(OFFICER), Arrays.asList(CENTER))));
        assertTrue(ex.getMessage().contains("两笔凑齐"));
    }

    @Test
    void create_sameSiteSameYearRejected() {
        service.createBill(newBill(), roster());
        BudgetRuleException ex = assertThrows(BudgetRuleException.class,
                () -> service.createBill(newBill(), roster()));
        assertTrue(ex.getMessage().contains("不能再立"));
    }

    @Test
    void create_migratedArchRejected() {
        THmfBudgetBill b = newBill();
        b.setSiteId(1L);
        BudgetRuleException ex = assertThrows(BudgetRuleException.class, () -> service.createBill(b, roster()));
        assertTrue(ex.getMessage().contains("迁出"));
    }

    @Test
    void create_payAmountUnknownRejected() {
        THmfBudgetBill b = newBill();
        b.setRuleCode("NONE");
        BudgetRuleException ex = assertThrows(BudgetRuleException.class, () -> service.createBill(b, roster()));
        assertTrue(ex.getMessage().contains("算不出拨付额"));
        assertNull(b.getPayAmt(), "算不出额时不许手填金额落库");
    }

    // ---------------------------------------------------------------- 顺序与算法

    @Test
    void ownerTwoOfTwo_thenOfficeOne_thenCenterSeals() {
        Long id = service.createBill(newBill(), roster()).getId();

        // 业委会只落一笔：仍候签，不进档
        service.sign(id, 0, "张业委");
        THmfBudgetBill b1 = service.selectTHmfBudgetBillById(id);
        assertEquals(0, b1.getNodeNo());
        assertEquals(0, b1.getStatus());
        assertEquals(1, db.node(id, 0).getSignCount());
        assertEquals(0, db.node(id, 0).getPassed(), "只落一笔窗口仍显示候签");

        // 第二笔凑齐：进事务所档
        service.sign(id, 0, "李业委");
        assertEquals(1, service.selectTHmfBudgetBillById(id).getNodeNo());
        assertEquals(1, db.node(id, 0).getPassed());
        assertEquals(1, db.node(id, 0).getPassRound());

        // 事务所头一笔落地即过：进中心档
        service.sign(id, 1, OFFICER);
        assertEquals(2, service.selectTHmfBudgetBillById(id).getNodeNo());

        // 中心认下：封单
        service.sign(id, 2, CENTER);
        THmfBudgetBill sealed = service.selectTHmfBudgetBillById(id);
        assertEquals(3, sealed.getNodeNo());
        assertEquals(1, sealed.getStatus());

        // 封后谁都不能添名
        BudgetRuleException ex = assertThrows(BudgetRuleException.class, () -> service.sign(id, 2, CENTER));
        assertTrue(ex.getMessage().contains("封住"));
    }

    @Test
    void sign_wrongNodeIsNoop_andOutOfRosterRejected_andDuplicateNotCounted() {
        Long id = service.createBill(newBill(), roster()).getId();

        // 前一档没满，后一档签了白签：不落任何流水
        long signsBefore = db.rows(THmfBudgetSign.class).size();
        BudgetRuleException ex1 = assertThrows(BudgetRuleException.class, () -> service.sign(id, 1, OFFICER));
        assertTrue(ex1.getMessage().contains("白签"));
        assertEquals(signsBefore, db.rows(THmfBudgetSign.class).size(), "白签一笔都不留");

        // 署名逐条对点名册
        BudgetRuleException ex2 = assertThrows(BudgetRuleException.class, () -> service.sign(id, 0, "路人甲"));
        assertTrue(ex2.getMessage().contains("点名册里没有"));

        // 同人同拨重复落笔不重计
        service.sign(id, 0, "张业委");
        BudgetRuleException ex3 = assertThrows(BudgetRuleException.class, () -> service.sign(id, 0, "张业委"));
        assertTrue(ex3.getMessage().contains("不重计"));
        assertEquals(1, db.node(id, 0).getSignCount());
    }

    // ---------------------------------------------------------------- 退回口径

    @Test
    void reject_oneStepBackKeepOldNamesAndNewRound() {
        Long id = service.createBill(newBill(), roster()).getId();
        service.sign(id, 0, "张业委");
        service.sign(id, 0, "李业委");
        // 单到事务所档，事务所退回
        THmfBudgetBill back = service.reject(id, 1, "王事务");
        assertEquals(0, back.getNodeNo(), "只挪回紧挨上一档");
        assertEquals(2, back.getRoundNo(), "拨次+1，新旧分开");
        assertEquals(2, back.getStatus(), "已打回");
        assertEquals(2, db.signsOf(id, 0, 1).size(), "先前业委会旧名一个不抹");
        assertEquals(0, db.node(id, 0).getPassed(), "遭退链条在新拨重新候签");
        assertEquals(1, db.rejectsOf(id, 1, 1).size(), "退回动作本身存档，操作人留底");

        // 业委会自己已是头一档，无上一档可退
        BudgetRuleException ex = assertThrows(BudgetRuleException.class, () -> service.reject(id, 0, "张业委"));
        assertTrue(ex.getMessage().contains("无上一档可退"));

        // 重新走签：新拨两笔 → 事务所 → 中心封单
        service.sign(id, 0, "张业委");
        service.sign(id, 0, "李业委");
        service.sign(id, 1, OFFICER);
        service.sign(id, 2, CENTER);
        THmfBudgetBill sealed = service.selectTHmfBudgetBillById(id);
        assertEquals(3, sealed.getNodeNo());
        assertEquals(2, sealed.getRoundNo());
        assertEquals(2, db.node(id, 0).getPassRound(), "过档笔记在新拨");

        // 复核：新旧两拨分开放，各自笔数对得齐
        BudgetBillVo vo = service.detail(id);
        BudgetBillVo.NodeVo owner = vo.getNodes().get(0);
        assertEquals(2, owner.getRoster().size());
        assertEquals(2, owner.getSigns().size(), "当前有效署名是第2拨两笔");
        assertEquals(2, owner.getEffRound());
        boolean hasRound1 = owner.getRounds().stream().anyMatch(r -> r.getRoundNo() == 1);
        boolean hasRound2 = owner.getRounds().stream().anyMatch(r -> r.getRoundNo() == 2);
        assertTrue(hasRound1 && hasRound2, "新旧两拨各自存档");
    }

    // ---------------------------------------------------------------- 复核与台账

    @Test
    void detail_countsMatchRosterAndViewNumbers() {
        Long id = service.createBill(newBill(), roster()).getId();
        service.sign(id, 0, "张业委");
        // 前一档没满，后一档签了白签（这里就是要它抛，且不落库）
        assertThrows(BudgetRuleException.class, () -> service.sign(id, 1, OFFICER));
        BudgetBillVo vo = service.detail(id);
        for (BudgetBillVo.NodeVo n : vo.getNodes()) {
            assertEquals(n.getNeedCount(), n.getRoster().size(), "点名条数=应到数");
            assertTrue(n.getSignCount() <= n.getNeedCount(), "落笔数不得越过应到");
            assertEquals(n.getSigns().size(), n.getSignCount(), "屏上数与逐笔流水对齐");
        }
        BudgetBillVo.NodeVo office = vo.getNodes().get(1);
        assertEquals(0, office.getSignCount(), "越档白签不认，事务所仍是0笔");
        assertTrue(office.getRounds().isEmpty());

        // 台账当前档四处字
        THmfBudgetBill row = service.selectTHmfBudgetBillList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<THmfBudgetBill>())
                .stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        assertNotNull(row);
        assertEquals("业委会核", row.getCurNodeName());
        assertEquals(2, row.getCurNeed());
        assertEquals(1, row.getCurSigned());
        assertEquals(0, row.getCurPassed(), "只落一笔：这一档没过");
    }

    // ---------------------------------------------------------------- 内存假库

    /**
     * 极简内存库：解释 QueryWrapper 里 col = #{...} 条件（本服务只用 eq + last/orderBy），
     * insert 模拟雪花ID与 createBy/createTime 填充。
     */
    static class FakeDb {
        private final List<THmfBudgetBill> bills = new ArrayList<>();
        private final List<THmfBudgetNode> nodes = new ArrayList<>();
        private final List<THmfBudgetRoster> rosters = new ArrayList<>();
        private final List<THmfBudgetSign> signs = new ArrayList<>();
        private final List<THmfArch> archs = new ArrayList<>();
        private final AtomicLong seq = new AtomicLong(1000);

        void seed(THmfArch o) {
            archs.add(o);
        }

        Object insert(Object o) {
            if (getProp(o, "id") == null) {
                setProp(o, "id", seq.incrementAndGet());
            }
            if (getProp(o, "createTime") == null) {
                setProp(o, "createTime", new Date());
            }
            if (getProp(o, "createBy") == null) {
                setProp(o, "createBy", "测试操作人");
            }
            store(o);
            return 1;
        }

        Object updateById(Object o) {
            // 同一对象引用本就在列表里，状态已就地改掉，这里只兜底确保在库
            return 1;
        }

        private void store(Object o) {
            if (o instanceof THmfBudgetBill) {
                bills.add((THmfBudgetBill) o);
            } else if (o instanceof THmfBudgetNode) {
                nodes.add((THmfBudgetNode) o);
            } else if (o instanceof THmfBudgetRoster) {
                rosters.add((THmfBudgetRoster) o);
            } else if (o instanceof THmfBudgetSign) {
                signs.add((THmfBudgetSign) o);
            } else if (o instanceof THmfArch) {
                archs.add((THmfArch) o);
            } else {
                throw new IllegalArgumentException("未登记的实体: " + o.getClass());
            }
        }

        @SuppressWarnings("unchecked")
        <T> List<T> rows(Class<T> c) {
            if (c == THmfBudgetBill.class) {
                return (List<T>) bills;
            }
            if (c == THmfBudgetNode.class) {
                return (List<T>) nodes;
            }
            if (c == THmfBudgetRoster.class) {
                return (List<T>) rosters;
            }
            if (c == THmfBudgetSign.class) {
                return (List<T>) signs;
            }
            if (c == THmfArch.class) {
                return (List<T>) archs;
            }
            throw new IllegalArgumentException("未登记的实体: " + c);
        }

        @SuppressWarnings("unchecked")
        <T> List<T> selectList(Class<T> c, Wrapper<T> w) {
            Map<String, Object> eqs = eqs(w);
            List<T> hit = new ArrayList<>();
            for (Object o : rows(c)) {
                if (matches(o, eqs)) {
                    hit.add((T) o);
                }
            }
            return hit;
        }

        <T> T selectOne(Class<T> c, Wrapper<T> w) {
            List<T> list = selectList(c, w);
            return list.isEmpty() ? null : list.get(0);
        }

        <T> Integer selectCount(Class<T> c, Wrapper<T> w) {
            return selectList(c, w).size();
        }

        THmfBudgetNode node(Long billId, int nodeNo) {
            return nodes.stream()
                    .filter(n -> n.getBillId().equals(billId) && n.getNodeNo() == nodeNo)
                    .findFirst().orElse(null);
        }

        List<THmfBudgetSign> signsOf(Long billId, int nodeNo, int round) {
            List<THmfBudgetSign> out = new ArrayList<>();
            for (THmfBudgetSign s : signs) {
                if (s.getBillId().equals(billId) && s.getNodeNo() == nodeNo
                        && s.getRoundNo() == round && s.getAction() == THmfBudgetSign.ACTION_SIGN) {
                    out.add(s);
                }
            }
            return out;
        }

        List<THmfBudgetSign> rejectsOf(Long billId, int nodeNo, int round) {
            List<THmfBudgetSign> out = new ArrayList<>();
            for (THmfBudgetSign s : signs) {
                if (s.getBillId().equals(billId) && s.getNodeNo() == nodeNo
                        && s.getRoundNo() == round && s.getAction() == THmfBudgetSign.ACTION_REJECT) {
                    out.add(s);
                }
            }
            return out;
        }

        private boolean matches(Object o, Map<String, Object> eqs) {
            for (Map.Entry<String, Object> e : eqs.entrySet()) {
                Object v = columnGet(o, e.getKey());
                Object want = e.getValue();
                if (v == null ? want != null : !v.equals(want)) {
                    return false;
                }
            }
            return true;
        }

        private static final Map<Class<?>, Map<String, String>> COL2FIELD = new LinkedHashMap<>();
        static {
            Map<String, String> billCols = new LinkedHashMap<>();
            billCols.put("id", "id");
            billCols.put("bill_no", "billNo");
            billCols.put("site_id", "siteId");
            billCols.put("bill_year", "billYear");
            billCols.put("del_flag", "delFlag");
            billCols.put("node_no", "nodeNo");
            COL2FIELD.put(THmfBudgetBill.class, billCols);

            Map<String, String> nodeCols = new LinkedHashMap<>();
            nodeCols.put("id", "id");
            nodeCols.put("bill_id", "billId");
            nodeCols.put("node_no", "nodeNo");
            nodeCols.put("del_flag", "delFlag");
            COL2FIELD.put(THmfBudgetNode.class, nodeCols);

            Map<String, String> rosterCols = new LinkedHashMap<>();
            rosterCols.put("id", "id");
            rosterCols.put("bill_id", "billId");
            rosterCols.put("node_no", "nodeNo");
            rosterCols.put("cand_name", "candName");
            rosterCols.put("del_flag", "delFlag");
            COL2FIELD.put(THmfBudgetRoster.class, rosterCols);

            Map<String, String> signCols = new LinkedHashMap<>();
            signCols.put("id", "id");
            signCols.put("bill_id", "billId");
            signCols.put("node_no", "nodeNo");
            signCols.put("round_no", "roundNo");
            signCols.put("signer", "signer");
            signCols.put("action", "action");
            signCols.put("del_flag", "delFlag");
            COL2FIELD.put(THmfBudgetSign.class, signCols);

            Map<String, String> archCols = new LinkedHashMap<>();
            archCols.put("id", "id");
            archCols.put("del_flag", "delFlag");
            COL2FIELD.put(THmfArch.class, archCols);
        }

        private Object columnGet(Object o, String column) {
            Map<String, String> m = COL2FIELD.get(o.getClass());
            if (m == null || !m.containsKey(column)) {
                return null;
            }
            return getProp(o, m.get(column));
        }

        private static Object getProp(Object o, String field) {
            try {
                Method m = o.getClass().getMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
                return m.invoke(o);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static void setProp(Object o, String field, Object v) {
            for (Method mth : o.getClass().getMethods()) {
                if (mth.getName().equalsIgnoreCase("set" + field) && mth.getParameterCount() == 1) {
                    try {
                        mth.invoke(o, v);
                        return;
                    } catch (Exception ignore) {
                        // 试下一个同名重载
                    }
                }
            }
            throw new RuntimeException("找不到 set" + field);
        }

        private static final Pattern EQ =
                Pattern.compile("(\\w+)\\s*=\\s*#\\{ew\\.paramNameValuePairs\\.(\\w+)}");

        static <T> Map<String, Object> eqs(Wrapper<T> w) {
            Map<String, Object> out = new LinkedHashMap<>();
            if (w == null) {
                return out;
            }
            Matcher m = EQ.matcher(w.getSqlSegment());
            @SuppressWarnings("unchecked")
            Map<String, Object> vals = ((com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?>) w)
                    .getParamNameValuePairs();
            while (m.find()) {
                out.put(m.group(1), vals.get(m.group(2)));
            }
            return out;
        }
    }

    /** 把一个 BaseMapper 的常用方法接到内存库的指定实体表。 */
    private static <T> void stub(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper, Class<T> c, FakeDb db) {
        Mockito.when(mapper.insert(Mockito.<T>any())).thenAnswer(inv -> db.insert(inv.getArgument(0)));
        Mockito.when(mapper.updateById(Mockito.<T>any())).thenAnswer(inv -> db.updateById(inv.getArgument(0)));
        Mockito.when(mapper.selectOne(Mockito.<Wrapper<T>>any()))
                .thenAnswer(inv -> db.selectOne(c, inv.getArgument(0)));
        Mockito.when(mapper.selectList(Mockito.<Wrapper<T>>any()))
                .thenAnswer(inv -> db.selectList(c, inv.getArgument(0)));
        Mockito.when(mapper.selectCount(Mockito.<Wrapper<T>>any()))
                .thenAnswer(inv -> db.selectCount(c, inv.getArgument(0)));
    }
}

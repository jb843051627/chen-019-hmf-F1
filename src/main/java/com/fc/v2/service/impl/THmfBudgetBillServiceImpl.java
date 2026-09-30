package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.fc.v2.service.ITHmfBudgetBillService;
import com.fc.v2.service.ITHmfSpendLineService;

/**
 * 预算审定拨付核签单 Service业务层处理（approval-chain 形状：三手顺序核签）
 *
 * 口径：
 * - 三手口次定死 0业委会核（两笔凑齐 MODE_AND，2人）/1事务所核（一笔即可 MODE_OR，1人）/2中心核（MODE_OR，1人）；
 * - “齐没齐”绝不认人手数：本档是否达齐只由 t_hmf_budget_sign 当前拨次署名逐笔 COUNT 推出，
 *   node.signCount 只是 COUNT 后回填的参照；署名逐条对本档点名册；
 * - 顺序挨着走：只有 nodeNo==单据当前档才收名，后一档提前落名一律按没签处理（不落流水）；
 * - 退回只挪回紧挨着的上一档；旧署名一律不删，round_no+1 新旧分存；
 * - 中心过档即封单（nodeNo=3,status=1），此后谁都不能再加名。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class THmfBudgetBillServiceImpl implements ITHmfBudgetBillService {

    /** 口次：业委会核 */
    private static final int NODE_OWNER = 0;
    /** 口次：房管事务所核 */
    private static final int NODE_OFFICE = 1;
    /** 口次：中心核 */
    private static final int NODE_CENTER = 2;
    /** 全部核过的封单口次 */
    private static final int NODE_SEALED = 3;
    private static final int NODE_TOTAL = 3;

    /** 一笔即可 */
    private static final int MODE_OR = 0;
    /** 两笔凑齐 */
    private static final int MODE_AND = 1;

    /** 0在核 */
    private static final int STATUS_RUNNING = 0;
    /** 1已核讫 */
    private static final int STATUS_PASS = 1;
    /** 2已打回 */
    private static final int STATUS_VETO = 2;

    private static final String[] NODE_NAMES = {"业委会核", "事务所核", "中心核"};

    @Resource
    private THmfBudgetBillMapper billMapper;
    @Resource
    private THmfBudgetNodeMapper nodeMapper;
    @Resource
    private THmfBudgetRosterMapper rosterMapper;
    @Resource
    private THmfBudgetSignMapper signMapper;
    @Resource
    private THmfArchMapper archMapper;
    @Resource
    private ITHmfSpendLineService spendLineService;

    @Override
    public THmfBudgetBill selectTHmfBudgetBillById(Long id) {
        if (id == null) {
            return null;
        }
        return billMapper.selectOne(new QueryWrapper<THmfBudgetBill>().eq("id", id).eq("del_flag", 0));
    }

    @Override
    public List<THmfBudgetBill> selectTHmfBudgetBillList(Wrapper<THmfBudgetBill> queryWrapper) {
        List<THmfBudgetBill> bills = billMapper.selectList(queryWrapper);
        for (THmfBudgetBill bill : bills) {
            fillCurrentView(bill);
        }
        return bills;
    }

    /** 台账“四处字”：这一档该几个人签、怎么算签齐、此刻落了几笔、这一档过没过——全照库算，不取人手数 */
    private void fillCurrentView(THmfBudgetBill bill) {
        int current = bill.getNodeNo() == null ? NODE_OWNER : bill.getNodeNo();
        int round = bill.getRoundNo() == null ? 1 : bill.getRoundNo();
        if (current == NODE_SEALED) {
            bill.setCurNodeName("已封单");
            bill.setCurNeed(1);
            bill.setCurMode(MODE_OR);
            bill.setCurSigned(1);
            bill.setCurPassed(1);
            return;
        }
        THmfBudgetNode node = nodeMapper.selectOne(new QueryWrapper<THmfBudgetNode>()
                .eq("bill_id", bill.getId()).eq("node_no", current).eq("del_flag", 0).last("limit 1"));
        if (node == null) {
            return;
        }
        bill.setCurNodeName(NODE_NAMES[current]);
        bill.setCurNeed(node.getNeedCount());
        bill.setCurMode(node.getSignMode());
        bill.setCurSigned(countSigns(bill.getId(), current, round));
        // 这一档过没过也照实笔数重算，候签状态不能信库存里可能过期的参照
        bill.setCurPassed(reached(node, bill.getCurSigned()) ? 1 : 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public THmfBudgetBill createBill(THmfBudgetBill bill, List<List<String>> rosterNames) {
        if (bill == null || bill.getSiteId() == null || bill.getBillYear() == null
                || bill.getRuleCode() == null || bill.getRuleCode().trim().isEmpty()
                || bill.getDrafter() == null || bill.getDrafter().trim().isEmpty()) {
            throw new BudgetRuleException("底册、年度、审定额线代号、立单经手人都得给齐");
        }
        if (rosterNames == null || rosterNames.size() != NODE_TOTAL) {
            throw new BudgetRuleException("业委会、事务所、中心三档点名册都得给齐");
        }
        // 三档算法各自定死：业委会两笔凑齐（2人），事务所/中心一笔即可（各1人）
        int[] needFixed = {2, 1, 1};
        int[] modeFixed = {MODE_AND, MODE_OR, MODE_OR};
        List<List<String>> cleaned = new ArrayList<List<String>>();
        for (int n = 0; n < NODE_TOTAL; n++) {
            List<String> names = trimDistinct(rosterNames.get(n));
            if (names.size() != needFixed[n]) {
                throw new BudgetRuleException(NODE_NAMES[n] + "这一档定的是"
                        + (modeFixed[n] == MODE_AND ? "两笔凑齐" : "一笔即可")
                        + "，点名得是" + needFixed[n] + "个人（不能重名）");
            }
            cleaned.add(names);
        }

        // 底册须在册；代号立单时誊定，不随后续底册改版
        THmfArch arch = archMapper.selectOne(new QueryWrapper<THmfArch>()
                .eq("id", bill.getSiteId()).eq("del_flag", 0));
        if (arch == null) {
            throw new BudgetRuleException("所选分户底册不存在");
        }
        if (arch.getStatus() != null && arch.getStatus() == 1) {
            throw new BudgetRuleException("所选分户底册已迁出，不能立拨付单");
        }

        // 同一小区同一年再来一张要核签的，另起新单——不并单，也不允许重复立单
        Integer dup = billMapper.selectCount(new QueryWrapper<THmfBudgetBill>()
                .eq("site_id", bill.getSiteId())
                .eq("bill_year", bill.getBillYear())
                .eq("del_flag", 0));
        if (dup != null && dup > 0) {
            throw new BudgetRuleException("同一小区" + bill.getBillYear() + "年已有核签单，不能再立一张并进去");
        }

        // 拨付额只从预算审定额线取这一回算：入库存的与窗口显示的同源，没有手填的口子
        BigDecimal payAmt = spendLineService.amountOf(bill.getRuleCode().trim(), bill.getPayInput(), new Date());
        if (payAmt == null) {
            throw new BudgetRuleException("该审定额线此刻算不出拨付额，不能立单");
        }

        if (bill.getBillNo() == null || bill.getBillNo().trim().isEmpty()) {
            bill.setBillNo(arch.getSiteNo() + "-" + bill.getBillYear() + "-" + System.currentTimeMillis() % 100000L);
        } else {
            bill.setBillNo(bill.getBillNo().trim());
        }
        Integer noDup = billMapper.selectCount(new QueryWrapper<THmfBudgetBill>()
                .eq("bill_no", bill.getBillNo()).eq("del_flag", 0));
        if (noDup != null && noDup > 0) {
            throw new BudgetRuleException("单号已存在：" + bill.getBillNo());
        }

        bill.setSiteNo(arch.getSiteNo());
        bill.setPayAmt(payAmt);
        bill.setDrafter(bill.getDrafter().trim());
        bill.setNodeNo(NODE_OWNER);
        bill.setRoundNo(1);
        bill.setStatus(STATUS_RUNNING);
        bill.setDelFlag(0);
        billMapper.insert(bill);

        for (int n = 0; n < NODE_TOTAL; n++) {
            THmfBudgetNode node = new THmfBudgetNode();
            node.setBillId(bill.getId());
            node.setNodeNo(n);
            node.setSignMode(modeFixed[n]);
            node.setNeedCount(needFixed[n]);
            node.setSignCount(0);
            node.setPassed(0);
            node.setDelFlag(0);
            nodeMapper.insert(node);

            List<String> names = cleaned.get(n);
            for (int i = 0; i < names.size(); i++) {
                THmfBudgetRoster r = new THmfBudgetRoster();
                r.setBillId(bill.getId());
                r.setNodeNo(n);
                r.setCandName(names.get(i));
                r.setSeqNo(i + 1);
                r.setDelFlag(0);
                rosterMapper.insert(r);
            }
        }
        return bill;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public THmfBudgetBill sign(Long id, Integer nodeNo, String signer) {
        THmfBudgetBill bill = mustGet(id);
        if (nodeNo == null || signer == null || signer.trim().isEmpty()) {
            throw new BudgetRuleException("口次和署名都得给");
        }
        if ((bill.getStatus() != null && bill.getStatus() == STATUS_PASS)
                || (bill.getNodeNo() != null && bill.getNodeNo() == NODE_SEALED)) {
            throw new BudgetRuleException("中心认下后单已封住，谁都不能再添名");
        }
        // 次序两条：三档只能挨着往前。不是当前所在档，签了白签，按没签处理
        int current = bill.getNodeNo() == null ? NODE_OWNER : bill.getNodeNo();
        if (nodeNo.intValue() != current) {
            throw new BudgetRuleException("前一档还没满，" + NODE_NAMES[nodeNo] + "这一档签了也白签（按没签处理）");
        }
        String name = signer.trim();
        // 逐笔对点名：署名必须在这一档的点名册内，系统照单逐笔勾，不认自报
        THmfBudgetRoster onRoster = rosterMapper.selectOne(new QueryWrapper<THmfBudgetRoster>()
                .eq("bill_id", bill.getId()).eq("node_no", nodeNo).eq("cand_name", name)
                .eq("del_flag", 0).last("limit 1"));
        if (onRoster == null) {
            throw new BudgetRuleException(NODE_NAMES[nodeNo] + "点名册里没有“" + name + "”，这笔署名不认");
        }
        int round = bill.getRoundNo() == null ? 1 : bill.getRoundNo();
        // 同人同拨只算一笔，重复落笔不重计（库上另有唯一约束兜底）
        Integer already = signMapper.selectCount(new QueryWrapper<THmfBudgetSign>()
                .eq("bill_id", bill.getId()).eq("node_no", nodeNo).eq("round_no", round)
                .eq("signer", name).eq("action", THmfBudgetSign.ACTION_SIGN).eq("del_flag", 0));
        if (already != null && already > 0) {
            throw new BudgetRuleException("“" + name + "”这一拨已经签过，不重计");
        }

        THmfBudgetSign rec = new THmfBudgetSign();
        rec.setBillId(bill.getId());
        rec.setNodeNo(nodeNo);
        rec.setRoundNo(round);
        rec.setSigner(name);
        rec.setAction(THmfBudgetSign.ACTION_SIGN);
        rec.setDelFlag(0);
        signMapper.insert(rec);

        THmfBudgetNode node = getNode(bill.getId(), nodeNo);
        // 齐不齐只照署名流水逐笔 COUNT，不读任何人手记的数
        int landed = countSigns(bill.getId(), nodeNo, round);
        node.setSignCount(landed);
        boolean full = reached(node, landed);
        if (full) {
            node.setPassed(1);
            node.setPassRound(round);
            if (nodeNo == NODE_CENTER) {
                // 中心这一档认下，整张单封住
                bill.setNodeNo(NODE_SEALED);
                bill.setStatus(STATUS_PASS);
            } else {
                bill.setNodeNo(nodeNo + 1);
                bill.setStatus(STATUS_RUNNING);
            }
        } else {
            // 业委会两笔凑齐：只落一笔时窗口里仍然显示候签
            node.setPassed(0);
            bill.setStatus(STATUS_RUNNING);
        }
        nodeMapper.updateById(node);
        billMapper.updateById(bill);
        return bill;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public THmfBudgetBill reject(Long id, Integer nodeNo, String operator) {
        THmfBudgetBill bill = mustGet(id);
        if (nodeNo == null) {
            throw new BudgetRuleException("得指明是哪一档退的");
        }
        if (bill.getNodeNo() != null && bill.getNodeNo() == NODE_SEALED) {
            throw new BudgetRuleException("单已封讫，不能退回");
        }
        int current = bill.getNodeNo() == null ? NODE_OWNER : bill.getNodeNo();
        if (nodeNo.intValue() != current) {
            throw new BudgetRuleException("只有当前所在档能退这张单（中心越过事务所直盖章一样不认）");
        }
        if (nodeNo == NODE_OWNER) {
            // 退回只退一个手：业委会是头一档，没有再上一档可挪
            throw new BudgetRuleException("业委会档已是头一档，无上一档可退");
        }
        int round = bill.getRoundNo() == null ? 1 : bill.getRoundNo();

        // 退这一笔也入流水存档：谁退的、几时退的；signer 落实际操作人
        String who = (operator == null || operator.trim().isEmpty()) ? "" : operator.trim();
        THmfBudgetSign rec = new THmfBudgetSign();
        rec.setBillId(bill.getId());
        rec.setNodeNo(nodeNo);
        rec.setRoundNo(round);
        rec.setSigner(who.isEmpty() ? "(本档退回)" : who);
        rec.setAction(THmfBudgetSign.ACTION_REJECT);
        rec.setDelFlag(0);
        signMapper.insert(rec);

        // 整张只挪回紧挨着的上一档；拨次+1，新旧两拨署名分开放，旧名一个不抹
        int newRound = round + 1;
        bill.setRoundNo(newRound);
        bill.setNodeNo(nodeNo - 1);
        bill.setStatus(STATUS_VETO);
        billMapper.updateById(bill);

        // 遭退档及其以上各档在新拨重签：清的是“当前拨状态”，历史各拨署名原样留档
        for (int n = nodeNo - 1; n <= NODE_CENTER; n++) {
            THmfBudgetNode node = getNode(bill.getId(), n);
            node.setPassed(0);
            node.setSignCount(countSigns(bill.getId(), n, newRound));
            nodeMapper.updateById(node);
        }
        return bill;
    }

    @Override
    public BudgetBillVo detail(Long id) {
        THmfBudgetBill bill = mustGet(id);
        BudgetBillVo vo = new BudgetBillVo();
        vo.setBill(bill);
        int current = bill.getNodeNo() == null ? NODE_OWNER : bill.getNodeNo();
        int round = bill.getRoundNo() == null ? 1 : bill.getRoundNo();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int n = 0; n < NODE_TOTAL; n++) {
            THmfBudgetNode node = getNode(bill.getId(), n);
            BudgetBillVo.NodeVo nv = new BudgetBillVo.NodeVo();
            nv.setNodeNo(n);
            nv.setNodeName(NODE_NAMES[n]);
            nv.setSignMode(node.getSignMode());
            nv.setNeedCount(node.getNeedCount());
            // 抽任何一档单看也经得起复核：已过档的点它过档那一拨的笔数，候签的点当前拨
            int effRound = (node.getPassed() != null && node.getPassed() == 1 && node.getPassRound() != null)
                    ? node.getPassRound() : round;
            nv.setEffRound(effRound);
            // 屏上那个数照署名流水逐笔 COUNT 重算，不直接信库存参照值
            nv.setSignCount(countSigns(bill.getId(), n, effRound));
            nv.setPassed(node.getPassed());
            nv.setCurrent(n == current);

            List<THmfBudgetRoster> roster = rosterMapper.selectList(new QueryWrapper<THmfBudgetRoster>()
                    .eq("bill_id", bill.getId()).eq("node_no", n).eq("del_flag", 0)
                    .orderBy(true, true, "seq_no", "id"));
            for (THmfBudgetRoster r : roster) {
                nv.getRoster().add(r.getCandName());
            }

            List<THmfBudgetSign> all = signMapper.selectList(new QueryWrapper<THmfBudgetSign>()
                    .eq("bill_id", bill.getId()).eq("node_no", n).eq("del_flag", 0)
                    .orderBy(true, true, "round_no", "id"));
            Map<Integer, BudgetBillVo.RoundVo> roundMap = new LinkedHashMap<Integer, BudgetBillVo.RoundVo>();
            for (THmfBudgetSign s : all) {
                BudgetBillVo.SignVo sv = new BudgetBillVo.SignVo();
                sv.setSigner(s.getSigner());
                sv.setOperator(s.getCreateBy());
                sv.setSignTime(s.getCreateTime() == null ? "" : sdf.format(s.getCreateTime()));
                sv.setRoundNo(s.getRoundNo());
                sv.setAction(s.getAction());
                sv.setRemark(s.getRemark());
                if (s.getRoundNo() != null && s.getRoundNo() == effRound
                        && s.getAction() != null && s.getAction() == THmfBudgetSign.ACTION_SIGN) {
                    nv.getSigns().add(sv);
                }
                BudgetBillVo.RoundVo rv = roundMap.get(s.getRoundNo());
                if (rv == null) {
                    rv = new BudgetBillVo.RoundVo();
                    rv.setRoundNo(s.getRoundNo());
                    roundMap.put(s.getRoundNo(), rv);
                }
                rv.getSigns().add(sv);
            }
            nv.getRounds().addAll(roundMap.values());
            vo.getNodes().add(nv);
        }
        return vo;
    }

    // ------------------------------------------------------------------

    /** 本档算法定死判定：MODE_OR 头一笔即过；MODE_AND 须点齐应到数（应到数与点名册同数） */
    private boolean reached(THmfBudgetNode node, int landed) {
        if (node.getSignMode() != null && node.getSignMode() == MODE_AND) {
            return landed >= node.getNeedCount();
        }
        return landed >= 1;
    }

    private int countSigns(Long billId, int nodeNo, int round) {
        Integer c = signMapper.selectCount(new QueryWrapper<THmfBudgetSign>()
                .eq("bill_id", billId).eq("node_no", nodeNo).eq("round_no", round)
                .eq("action", THmfBudgetSign.ACTION_SIGN).eq("del_flag", 0));
        return c == null ? 0 : c;
    }

    private THmfBudgetNode getNode(Long billId, int nodeNo) {
        THmfBudgetNode node = nodeMapper.selectOne(new QueryWrapper<THmfBudgetNode>()
                .eq("bill_id", billId).eq("node_no", nodeNo).eq("del_flag", 0)
                .last("limit 1"));
        if (node == null) {
            throw new BudgetRuleException("这一档口径缺失，单据数据不完整");
        }
        return node;
    }

    private THmfBudgetBill mustGet(Long id) {
        THmfBudgetBill bill = selectTHmfBudgetBillById(id);
        if (bill == null) {
            throw new BudgetRuleException("拨付核签单不存在");
        }
        return bill;
    }

    private List<String> trimDistinct(List<String> raw) {
        List<String> out = new ArrayList<String>();
        if (raw == null) {
            return out;
        }
        for (String s : raw) {
            if (s == null) {
                continue;
            }
            String t = s.trim();
            if (!t.isEmpty() && !out.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    /** 口径不满足时抛出，由全局异常处理回中文事由，调用方不落任何脏数据 */
    public static class BudgetRuleException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        BudgetRuleException(String message) {
            super(message);
        }
    }

    /** 三档名，供控制器/页面复用 */
    public static List<String> nodeNames() {
        return Arrays.asList(NODE_NAMES);
    }
}

package com.fc.v2.service;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.model.auto.THmfBudgetBill;
import com.fc.v2.model.custom.BudgetBillVo;

/**
 * 预算审定拨付核签单 Service接口（approval-chain 形状：三手顺序核签，无改删入口）
 *
 * 口径要点：
 * 1. 三手——业委会(0，两笔凑齐)/事务所(1，一笔即可)/中心(2，一笔即可)，算法建单定死；
 * 2. 齐不齐只认逐笔署名流水 COUNT，不认人手自报数，署名逐条对点名册；
 * 3. 前一档没满，后一档落名按没签处理；中心认下即封单；
 * 4. 退回只退紧挨上一档，旧名不抹、拨次+1，新旧拨分存。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITHmfBudgetBillService {

    /** 按主键回查单据 */
    THmfBudgetBill selectTHmfBudgetBillById(Long id);

    /** 台账查询（del_flag=0） */
    List<THmfBudgetBill> selectTHmfBudgetBillList(Wrapper<THmfBudgetBill> queryWrapper);

    /**
     * 立单。bill 带 billNo(可空自动编号)/siteId/billYear/ruleCode/payInput(送审定额线的取数输入)/drafter；
     * rosterNames 三个元素依次为业委会(须2人)/事务所(须1人)/中心(须1人)点名人。
     * 拨付额由审定额线一次性算定写入，调用方传 payAmt 无效。
     */
    THmfBudgetBill createBill(THmfBudgetBill bill, List<List<String>> rosterNames);

    /**
     * 落一笔署名。nodeNo 必须恰为当前所在档，signer 必须在本档点名册内，同拨同人不重计；
     * 越档、封单、未点名一律拒绝（异常带中文事由）。落齐按本档算法自动过档，中心过即封单。
     */
    THmfBudgetBill sign(Long id, Integer nodeNo, String signer);

    /**
     * 当前档把单退回：只挪回紧挨上一档，拨次+1，先前各档名字一个不抹，
     * 遭退档及以上各档在新拨重签。业委会档(0)无上一档可退，拒绝。
     */
    THmfBudgetBill reject(Long id, Integer nodeNo, String operator);

    /** 复核视图：三档点名册、当前拨署名、历史各拨存档，逐档笔数对得齐。 */
    BudgetBillVo detail(Long id);
}

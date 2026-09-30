package com.fc.v2.controller.admin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.mapper.auto.THmfArchMapper;
import com.fc.v2.model.auto.THmfArch;
import com.fc.v2.model.auto.THmfBudgetBill;
import com.fc.v2.model.auto.THmfSpendLine;
import com.fc.v2.model.custom.BudgetBillVo;
import com.fc.v2.service.ITHmfBudgetBillService;
import com.fc.v2.service.ITHmfSpendLineService;
import com.fc.v2.shiro.util.ShiroUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

/**
 * 预算审定拨付核签单 Controller（approval-chain 形状）
 * 只有立单、台账、落名、退回、复核，没有改/删入口——封了的单谁也动不了。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Api(value = "预算审定拨付核签单")
@Controller
@RequestMapping("/hmfBudgetBill")
public class HmfBudgetBillController extends BaseController {

    private final String prefix = "admin/hmfBudgetBill";

    @Autowired
    private ITHmfBudgetBillService hmfBudgetBillService;
    @Autowired
    private ITHmfSpendLineService hmfSpendLineService;
    @Autowired
    private THmfArchMapper hmfArchMapper;

    @ApiOperation(value = "核签台账跳转", notes = "核签台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("hmfBudgetBill:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @Log(title = "拨付核签台账查询", action = "list")
    @ApiOperation(value = "分页查询", notes = "分页查询")
    @GetMapping("/list")
    @RequiresPermissions("hmfBudgetBill:list")
    @ResponseBody
    public ResultTable list(THmfBudgetBill record) {
        QueryWrapper<THmfBudgetBill> queryWrapper = new QueryWrapper<THmfBudgetBill>()
                .eq("del_flag", 0)
                .orderBy(true, false, "create_time");
        if (record.getBillNo() != null && !record.getBillNo().trim().isEmpty()) {
            queryWrapper.like("bill_no", record.getBillNo().trim());
        }
        if (record.getSiteNo() != null && !record.getSiteNo().trim().isEmpty()) {
            queryWrapper.like("site_no", record.getSiteNo().trim());
        }
        if (record.getStatus() != null) {
            queryWrapper.eq("status", record.getStatus());
        }
        startPage();
        com.github.pagehelper.PageInfo<THmfBudgetBill> page =
                new com.github.pagehelper.PageInfo<THmfBudgetBill>(
                        hmfBudgetBillService.selectTHmfBudgetBillList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @ApiOperation(value = "立单页跳转", notes = "立单页跳转")
    @GetMapping("/add")
    @RequiresPermissions("hmfBudgetBill:add")
    public String add(ModelMap model) {
        // 底册下拉只给在册的；拨付额线下拉只给此刻生效的
        List<THmfArch> archs = hmfArchMapper.selectList(new QueryWrapper<THmfArch>()
                .eq("del_flag", 0).eq("status", 0).orderBy(true, true, "site_no"));
        List<THmfSpendLine> lines = hmfSpendLineService.listAvailable(new Date());
        model.put("archs", archs);
        model.put("lines", lines);
        // 前一格：立单经手人默认就是当前登录人，起单到归档不换
        model.put("drafter", ShiroUtils.getLoginName());
        return prefix + "/add";
    }

    @Log(title = "拨付核签单立单", action = "add")
    @ApiOperation(value = "立单", notes = "三档算法与应到数建单即定死，拨付额由审定额线算定")
    @PostMapping("/add")
    @RequiresPermissions("hmfBudgetBill:add")
    @ResponseBody
    public AjaxResult add(@RequestParam(required = false) String billNo,
                          @RequestParam Long siteId,
                          @RequestParam Integer billYear,
                          @RequestParam String ruleCode,
                          @RequestParam BigDecimal payInput,
                          @RequestParam(required = false) String drafter,
                          @RequestParam String ownerA,
                          @RequestParam String ownerB,
                          @RequestParam String officeName,
                          @RequestParam String centerName) {
        THmfBudgetBill bill = new THmfBudgetBill();
        bill.setBillNo(billNo);
        bill.setSiteId(siteId);
        bill.setBillYear(billYear);
        bill.setRuleCode(ruleCode);
        // payInput 只是送审定额线的取数输入；payAmt 由服务端同一回算落库，页面传什么金额都不作数
        bill.setPayInput(payInput);
        bill.setDrafter((drafter == null || drafter.trim().isEmpty()) ? ShiroUtils.getLoginName() : drafter);
        List<List<String>> roster = new ArrayList<List<String>>();
        roster.add(Arrays.asList(ownerA, ownerB));
        roster.add(Arrays.asList(officeName));
        roster.add(Arrays.asList(centerName));
        return toAjax(hmfBudgetBillService.createBill(bill, roster) != null ? 1 : 0);
    }

    /** 立单页预览拨付额：与立单落库走同一个 amountOf，屏显与库存保证同一回算 */
    @ApiOperation(value = "拨付额取数预览", notes = "按审定额线当场算出拨付额，只显不录")
    @GetMapping("/payAmount")
    @RequiresPermissions("hmfBudgetBill:add")
    @ResponseBody
    public AjaxResult payAmount(@RequestParam String ruleCode, @RequestParam BigDecimal payInput) {
        BigDecimal amt = hmfSpendLineService.amountOf(ruleCode, payInput, new Date());
        if (amt == null) {
            return AjaxResult.error("该审定额线此刻算不出拨付额");
        }
        return AjaxResult.successData(200, amt);
    }

    @ApiOperation(value = "核签详情页跳转", notes = "核签详情页跳转")
    @GetMapping("/detail/{id}")
    @RequiresPermissions("hmfBudgetBill:view")
    public String detailPage(@PathVariable("id") Long id, ModelMap model) {
        model.put("billId", id);
        return prefix + "/detail";
    }

    @ApiOperation(value = "核签复核数据", notes = "三档点名/署名/各拨存档，倒序也对得齐")
    @GetMapping("/detailJson")
    @RequiresPermissions("hmfBudgetBill:list")
    @ResponseBody
    public AjaxResult detailJson(@RequestParam Long id) {
        BudgetBillVo vo = hmfBudgetBillService.detail(id);
        return AjaxResult.successData(200, vo);
    }

    @Log(title = "拨付核签落名", action = "sign")
    @ApiOperation(value = "落一笔署名", notes = "只收当前所在档；署名须在本档点名册内；齐则过档，中心过即封单")
    @PostMapping("/sign")
    @RequiresPermissions("hmfBudgetBill:sign")
    @ResponseBody
    public AjaxResult sign(@RequestParam Long id, @RequestParam Integer nodeNo, @RequestParam String signer) {
        return toAjax(hmfBudgetBillService.sign(id, nodeNo, signer.trim()) != null ? 1 : 0);
    }

    @Log(title = "拨付核签退回", action = "reject")
    @ApiOperation(value = "退回上一档", notes = "只挪回紧挨上一档，拨次+1，旧名不抹")
    @PostMapping("/reject")
    @RequiresPermissions("hmfBudgetBill:reject")
    @ResponseBody
    public AjaxResult reject(@RequestParam Long id, @RequestParam Integer nodeNo,
                             @RequestParam(required = false) String operator) {
        String who = (operator == null || operator.trim().isEmpty())
                ? ShiroUtils.getLoginName() : operator.trim();
        return toAjax(hmfBudgetBillService.reject(id, nodeNo, who) != null ? 1 : 0);
    }

    /** 三档定死的名目，供页面按钮/标题复用 */
    public static List<String> nodeNames() {
        return Arrays.asList("业委会核", "事务所核", "中心核");
    }
}

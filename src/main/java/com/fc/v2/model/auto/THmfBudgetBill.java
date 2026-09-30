package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 预算审定拨付核签单对象 t_hmf_budget_bill
 *
 * @author fuce
 * @date 2026-09-12
 */
@TableName("t_hmf_budget_bill")
@ApiModel(value = "THmfBudgetBill", description = "预算审定拨付核签单")
public class THmfBudgetBill implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 拨付核签单号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "拨付核签单号")
    private String billNo;

    /** 所属分户底册id */
    @TableField("site_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属分户底册id")
    private Long siteId;

    /** 所属底册代号（立单时自底册誊定，不随底册改版） */
    @TableField("site_no")
    @ApiModelProperty(value = "所属底册代号")
    private String siteNo;

    /** 核签年度（同一小区同一年再来的另起新单，不并单） */
    @TableField("bill_year")
    @ApiModelProperty(value = "核签年度")
    private Integer billYear;

    /** 拨付额取数所用支出审定额线代号 */
    @TableField("rule_code")
    @ApiModelProperty(value = "拨付额取数所用支出审定额线代号")
    private String ruleCode;

    /** 拨付额（自立单时一次性从审定额线算定，屏显与库存同源，禁手填） */
    @TableField("pay_amt")
    @ApiModelProperty(value = "拨付额（审定额取数，禁手填）")
    private BigDecimal payAmt;

    /** 送审定额线的取数输入（不落库；只用于这一回算 payAmt） */
    @TableField(exist = false)
    @ApiModelProperty(value = "审定额线取数输入（不落库）")
    private BigDecimal payInput;

    /** 当前档名目（台账回显，不落库） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当前档名目")
    private String curNodeName;
    /** 当前档应落名数（不落库） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当前档应落名数")
    private Integer curNeed;
    /** 当前档并签办法 0一笔即可 1两笔凑齐（不落库） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当前档并签办法")
    private Integer curMode;
    /** 当前档已落笔数（不落库，逐笔COUNT） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当前档已落笔数")
    private Integer curSigned;
    /** 当前档过没过 0候签 1已过（不落库） */
    @TableField(exist = false)
    @ApiModelProperty(value = "当前档过没过")
    private Integer curPassed;

    /** 立单经手人（起单到归档不变，重录不换；与update_by一格分开） */
    @TableField("drafter")
    @ApiModelProperty(value = "立单经手人")
    private String drafter;

    /** 当前所在口次 0业委会核 1事务所核 2中心核 3已封单 */
    @TableField("node_no")
    @ApiModelProperty(value = "当前所在口次 0业委会核 1事务所核 2中心核 3已封单")
    private Integer nodeNo;

    /** 签认拨次 首拨为1；每退一档+1，新旧拨署名分存 */
    @TableField("round_no")
    @ApiModelProperty(value = "签认拨次 首拨为1")
    private Integer roundNo;

    /** 报批情形 0在核 1已核讫 2已打回（重签落笔即回0） */
    @TableField("status")
    @ApiModelProperty(value = "报批情形 0在核 1已核讫 2已打回")
    private Integer status;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者（谁实际操作续谁，跟着事务走） */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getSiteNo() {
        return siteNo;
    }

    public void setSiteNo(String siteNo) {
        this.siteNo = siteNo;
    }

    public Integer getBillYear() {
        return billYear;
    }

    public void setBillYear(Integer billYear) {
        this.billYear = billYear;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public BigDecimal getPayAmt() {
        return payAmt;
    }

    public void setPayAmt(BigDecimal payAmt) {
        this.payAmt = payAmt;
    }

    public BigDecimal getPayInput() {
        return payInput;
    }

    public void setPayInput(BigDecimal payInput) {
        this.payInput = payInput;
    }

    public String getCurNodeName() {
        return curNodeName;
    }

    public void setCurNodeName(String curNodeName) {
        this.curNodeName = curNodeName;
    }

    public Integer getCurNeed() {
        return curNeed;
    }

    public void setCurNeed(Integer curNeed) {
        this.curNeed = curNeed;
    }

    public Integer getCurMode() {
        return curMode;
    }

    public void setCurMode(Integer curMode) {
        this.curMode = curMode;
    }

    public Integer getCurSigned() {
        return curSigned;
    }

    public void setCurSigned(Integer curSigned) {
        this.curSigned = curSigned;
    }

    public Integer getCurPassed() {
        return curPassed;
    }

    public void setCurPassed(Integer curPassed) {
        this.curPassed = curPassed;
    }

    public String getDrafter() {
        return drafter;
    }

    public void setDrafter(String drafter) {
        this.drafter = drafter;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

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
import java.util.Date;

/**
 * 拨付核签各档口径与状态对象 t_hmf_budget_node
 * 每单固定三行：0业委会核（两笔凑齐）/1事务所核（一笔即可）/2中心核（一笔即可）。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_hmf_budget_node")
@ApiModel(value = "THmfBudgetNode", description = "拨付核签各档口径与状态")
public class THmfBudgetNode implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属拨付核签单 */
    @TableField("bill_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属拨付核签单")
    private Long billId;

    /** 口次 0业委会核 1事务所核 2中心核 */
    @TableField("node_no")
    @ApiModelProperty(value = "口次 0业委会核 1事务所核 2中心核")
    private Integer nodeNo;

    /** 同口并签办法 0一笔即可 1两笔凑齐（建单定死） */
    @TableField("sign_mode")
    @ApiModelProperty(value = "同口并签办法 0一笔即可 1两笔凑齐")
    private Integer signMode;

    /** 本档应落名数（与点名册条数一致） */
    @TableField("need_count")
    @ApiModelProperty(value = "本档应落名数")
    private Integer needCount;

    /** 本档当前拨次已落名数（逐笔COUNT回填，仅参照用不作判定依据） */
    @TableField("sign_count")
    @ApiModelProperty(value = "本档当前拨次已落名数（参照）")
    private Integer signCount;

    /** 本档在当前拨次过没过 0候签 1已过 */
    @TableField("passed")
    @ApiModelProperty(value = "本档过没过 0候签 1已过")
    private Integer passed;

    /** 本档最近一次过档所在拨次 */
    @TableField("pass_round")
    @ApiModelProperty(value = "最近一次过档拨次")
    private Integer passRound;

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

    /** 更新者 */
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

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getSignMode() {
        return signMode;
    }

    public void setSignMode(Integer signMode) {
        this.signMode = signMode;
    }

    public Integer getNeedCount() {
        return needCount;
    }

    public void setNeedCount(Integer needCount) {
        this.needCount = needCount;
    }

    public Integer getSignCount() {
        return signCount;
    }

    public void setSignCount(Integer signCount) {
        this.signCount = signCount;
    }

    public Integer getPassed() {
        return passed;
    }

    public void setPassed(Integer passed) {
        this.passed = passed;
    }

    public Integer getPassRound() {
        return passRound;
    }

    public void setPassRound(Integer passRound) {
        this.passRound = passRound;
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

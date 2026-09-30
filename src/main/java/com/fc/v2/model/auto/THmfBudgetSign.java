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
 * 拨付核签署名流水对象 t_hmf_budget_sign
 * 只追加、不删改；退回不抹名字，新旧两拨靠 round_no 分开，谁几时补的各自存档。
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_hmf_budget_sign")
@ApiModel(value = "THmfBudgetSign", description = "拨付核签署名流水")
public class THmfBudgetSign implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流水动作：署名 */
    public static final int ACTION_SIGN = 1;
    /** 流水动作：退回 */
    public static final int ACTION_REJECT = 2;

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

    /** 落名/退回所在口次 0业委会核 1事务所核 2中心核 */
    @TableField("node_no")
    @ApiModelProperty(value = "口次 0业委会核 1事务所核 2中心核")
    private Integer nodeNo;

    /** 第几拨签的（首拨1，退一档+1） */
    @TableField("round_no")
    @ApiModelProperty(value = "签认拨次")
    private Integer roundNo;

    /** 署名（须在本档点名册内，逐笔对点名） */
    @TableField("signer")
    @ApiModelProperty(value = "署名")
    private String signer;

    /** 流水动作 1署名 2退回 */
    @TableField("action")
    @ApiModelProperty(value = "流水动作 1署名 2退回")
    private Integer action;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 实际操作人（谁实际操作续谁） */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "实际操作人")
    private String createBy;

    /** 创建时间（几时补的） */
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

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
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

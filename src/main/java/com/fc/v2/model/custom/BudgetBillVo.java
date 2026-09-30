package com.fc.v2.model.custom;

import java.util.ArrayList;
import java.util.List;

/**
 * 拨付核签复核视图：中心倒着点回业委会，逐档点名条数与署名条数对齐。
 * 每一档都可单独抽出来复核，整串笔数须与当日窗口签认名单咬合。
 *
 * @author fuce
 * @date 2026-09-30
 */
public class BudgetBillVo {

    /** 单据本身（拨付额、当前档、情形、立单经手人都在这） */
    private Object bill;

    /** 三档明细，按下标 0业委会 1事务所 2中心；复核时倒序遍历 */
    private List<NodeVo> nodes = new ArrayList<NodeVo>();

    public Object getBill() {
        return bill;
    }

    public void setBill(Object bill) {
        this.bill = bill;
    }

    public List<NodeVo> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeVo> nodes) {
        this.nodes = nodes;
    }

    /** 单档复核 */
    public static class NodeVo {
        /** 口次 0..2 */
        private Integer nodeNo;
        /** 档名 */
        private String nodeName;
        /** 并签办法 0一笔即可 1两笔凑齐 */
        private Integer signMode;
        /** 应落名数（点名册条数） */
        private Integer needCount;
        /** 当前拨次已落笔数（逐笔COUNT，仅参照） */
        private Integer signCount;
        /** 这一笔记数取自第几拨（已过档取过档拨次，候签取当前拨） */
        private Integer effRound;
        /** 当前拨次过没过 0候签 1已过 */
        private Integer passed;
        /** 该档是否就是当前所在档 */
        private boolean current;
        /** 点名册（系统逐笔勾署名的底账） */
        private List<String> roster = new ArrayList<String>();
        /** 当前拨次署名（谁） */
        private List<SignVo> signs = new ArrayList<SignVo>();
        /** 历史各拨（退回前的旧拨，名字一个不抹，按拨次存档） */
        private List<RoundVo> rounds = new ArrayList<RoundVo>();

        public Integer getNodeNo() {
            return nodeNo;
        }

        public void setNodeNo(Integer nodeNo) {
            this.nodeNo = nodeNo;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
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

        public Integer getEffRound() {
            return effRound;
        }

        public void setEffRound(Integer effRound) {
            this.effRound = effRound;
        }

        public Integer getPassed() {
            return passed;
        }

        public void setPassed(Integer passed) {
            this.passed = passed;
        }

        public boolean isCurrent() {
            return current;
        }

        public void setCurrent(boolean current) {
            this.current = current;
        }

        public List<String> getRoster() {
            return roster;
        }

        public void setRoster(List<String> roster) {
            this.roster = roster;
        }

        public List<SignVo> getSigns() {
            return signs;
        }

        public void setSigns(List<SignVo> signs) {
            this.signs = signs;
        }

        public List<RoundVo> getRounds() {
            return rounds;
        }

        public void setRounds(List<RoundVo> rounds) {
            this.rounds = rounds;
        }
    }

    /** 一笔署名 */
    public static class SignVo {
        private String signer;
        private String operator;
        private String signTime;
        /** 第几拨 */
        private Integer roundNo;
        /** 1署名 2退回 */
        private Integer action;
        private String remark;

        public String getSigner() {
            return signer;
        }

        public void setSigner(String signer) {
            this.signer = signer;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getSignTime() {
            return signTime;
        }

        public void setSignTime(String signTime) {
            this.signTime = signTime;
        }

        public Integer getRoundNo() {
            return roundNo;
        }

        public void setRoundNo(Integer roundNo) {
            this.roundNo = roundNo;
        }

        public Integer getAction() {
            return action;
        }

        public void setAction(Integer action) {
            this.action = action;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    /** 一拨存档（新旧分开） */
    public static class RoundVo {
        private Integer roundNo;
        private List<SignVo> signs = new ArrayList<SignVo>();

        public Integer getRoundNo() {
            return roundNo;
        }

        public void setRoundNo(Integer roundNo) {
            this.roundNo = roundNo;
        }

        public List<SignVo> getSigns() {
            return signs;
        }

        public void setSigns(List<SignVo> signs) {
            this.signs = signs;
        }
    }
}

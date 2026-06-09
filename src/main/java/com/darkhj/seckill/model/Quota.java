package com.darkhj.seckill.model;

import com.darkhj.seckill.common.BaseModel;

import java.io.Serializable;

public class Quota extends BaseModel implements Serializable {
    /**
     * Id
     */
    private Long ID;



    private Integer num;
    private Long goodsID;

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Long getID() {
        return ID;
    }

    public void setID(Long ID) {
        this.ID = ID;
    }

    @Override
    public String toString() {
        return "Quota{" +
                "ID=" + ID +
                ", num=" + num +
                ", goodsID=" + goodsID +
                '}';
    }
}

package com.darkhj.seckill.service;

import com.darkhj.seckill.model.Goods;
import com.darkhj.seckill.model.PreSecKillRecord;

import java.util.ArrayList;

public interface SecKillService {

    Goods getGoodsByNum(String traceID, Long userID, String goodsNum);

    ArrayList<Goods> getGoodsList(String traceID, Long userID, Integer offset, Integer limit);

    String secKillV1(String traceID, Long userID, String goodsNum, Integer num);

    String secKillV2(String traceID, Long userID, String goodsNum, Integer num);

    String secKillV3(String traceID, Long userID, String goodsNum, Integer num);

    String secKillInStore(String traceID, Long userID, Goods goods, String secNum, Integer num);

    void setSuccessInPreSecKill(String traceID, Long userID, Goods goods, String secNum, PreSecKillRecord record);
}

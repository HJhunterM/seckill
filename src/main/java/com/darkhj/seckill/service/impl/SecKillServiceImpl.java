package com.darkhj.seckill.service.impl;

import com.darkhj.seckill.common.ResponseEnum;
import com.darkhj.seckill.common.redis.RedisBase;
import com.darkhj.seckill.exception.BusinessException;
import com.darkhj.seckill.mapper.*;
import com.darkhj.seckill.model.*;
import com.darkhj.seckill.service.SecKillService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SecKillServiceImpl implements SecKillService {

    @Autowired
    private ExampleMapper exampleMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private QuotaMapper quotaMapper;

    @Autowired
    private UserQuotaMapper userQuotaMapper;

    @Autowired
    private SecKillRecordMapper recordMapper;

    @Autowired
    private SecKillStockMapper stockMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RedisBase redisBase;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ResourceLoader resourceLoader;

    private String secKillLua;
    private String setSecKillSuccessLua;

    @PostConstruct
    public void loadLuaScripts() throws IOException {
        secKillLua = loadScript("classpath:lua/secKill.lua");
        setSecKillSuccessLua = loadScript("classpath:lua/setSecKillSuccess.lua");
    }

    private String loadScript(String path) throws IOException {
        Resource resource = resourceLoader.getResource(path);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }


    @Override
    public Goods getGoodsByNum(String traceID, Long userID, String goodsNum) {
        return goodsMapper.getGoodsByNum(goodsNum);
    }

    @Override
    public ArrayList<Goods> getGoodsList(String traceID, Long userID, Integer offset, Integer limit) {
        return goodsMapper.getGoodsList(offset, limit);
    }

    @Override
    @Transactional
    public String secKillV1(String traceID, Long userID, String goodsNum, Integer num) {
        Goods goods = goodsMapper.getGoodsByNum(goodsNum);
        return secKillInStore(traceID, userID, goods, "", num);
    }

    @Override
    @Transactional
    public String secKillV2(String traceID, Long userID, String goodsNum, Integer num) {
        String cacheKey = "goodsInfo:" + goodsNum;
        Gson gson = new Gson();
        Object res = redisBase.get(cacheKey);
        // System.out.println(res);
        Goods goods;
        if (res == null) {
            //System.out.println("cache miss");
            goods = goodsMapper.getGoodsByNum(goodsNum);
            String redisValue = gson.toJson(goods);
            redisBase.set(cacheKey, redisValue, 30);
        } else {
            goods = gson.fromJson(res.toString(), Goods.class);
            //    System.out.println("cache hit");

        }
        //System.out.println(goods);
        String secNum = UUID.randomUUID().toString();
        try {
            preDescStock(traceID, userID, goods, num, secNum, "");
        } catch (Exception e) {
            //System.out.println("preDescStock err "+e.getMessage());
            return "";
        }
        String orderNum = secKillInStore(traceID, userID, goods, secNum, num);
        Date date = new Date();
        PreSecKillRecord psRecord = new PreSecKillRecord();
        psRecord.setGoodsID(goods.getID());
        psRecord.setPrice(goods.getPrice());
        psRecord.setSecNum(secNum);
        psRecord.setOrderNum(orderNum);
        psRecord.setPrice(goods.getPrice());
        psRecord.setStatus(SKStatusEnum.SK_STATUS_BEFORE_PAY.getValue());
        psRecord.setCreateTime(date);
        psRecord.setModifyTime(date);
        setSuccessInPreSecKill(traceID, userID, goods, secNum, psRecord);
        return orderNum;
    }

    @Override
    public String secKillV3(String traceID, Long userID, String goodsNum, Integer num) {
        //kafkaTemplate.send("tp-seckill","aaaaaa");
        Goods goods = goodsMapper.getGoodsByNum(goodsNum);
        String secNum = UUID.randomUUID().toString();
        try {
            preDescStock(traceID, userID, goods, num, secNum, "");
        } catch (Exception e) {
            System.out.println("preDescStock err " + e.getMessage());
            return "";
        }
        SecKillMsg skMsg = new SecKillMsg();
        skMsg.goods = goods;
        skMsg.secNum = secNum;
        skMsg.traceID = traceID;
        skMsg.userID = userID;
        skMsg.num = num;
        Gson gson = new Gson();
        String msg = gson.toJson(skMsg);
        kafkaTemplate.send("tp-seckill", msg);
        return secNum;
    }

    public void preDescStock(String traceID, Long userID, Goods goods, Integer num, String secNum, String secRecord) throws Exception {
        Date date = new Date();
        PreSecKillRecord psRecord = new PreSecKillRecord();
        psRecord.setGoodsID(goods.getID());
        psRecord.setPrice(goods.getPrice());
        psRecord.setSecNum(secNum);
        psRecord.setPrice(goods.getPrice());
        psRecord.setStatus(SKStatusEnum.SK_STATUS_BEFORE_ORDER.getValue());
        psRecord.setCreateTime(date);
        psRecord.setModifyTime(date);
        Gson gson = new Gson();
        String recordStr = gson.toJson(psRecord);
        List<String> result = redisBase.executeLuaReturnString(secKillLua, Arrays.asList("" + userID, "" + goods.getID(), "" + num, secNum, recordStr));
        Long y = Long.valueOf(String.valueOf(result.get(0))).longValue();
        Integer x = y.intValue();
        switch (x) {
            case -1:
                throw new Exception("already in sec kill");
            case -2:
                throw new Exception("user out of limit on this goods");
            case -3:
                throw new Exception("stock not enough");
            case -4:
                throw new Exception("killed out");
            default:
                break;
        }
    }

    public Boolean judgeQuota(long userID, long goodsID, Integer num) {
        Integer userKilledNum = 0;
        Integer userQuotaNum = 0;
        UserQuota userQuota = userQuotaMapper.getUserGoodsUserQuota(userID, goodsID);
        if (userQuota != null) {
            userKilledNum = userQuota.getKilledNum();
            userQuotaNum = userQuota.getNum();
        }
        if (userQuotaNum == 0) {
            Quota globalQuota = quotaMapper.getGoodsQuota(goodsID);
            if (globalQuota != null) {
                userQuotaNum = globalQuota.getNum();
            }
        }
        if (userQuotaNum == 0) {
            return true;
        }
        Integer leftQuota = userQuotaNum - userKilledNum;
        if (leftQuota >= num) {
            return true;
        }
        return false;
    }

    public String secKillInStore(String traceID, Long userID, Goods goods, String secNum, Integer num) {
        boolean quotaFlag = judgeQuota(userID, goods.getID(), num);
        if (!quotaFlag) {
            throw new BusinessException(ResponseEnum.ERR_GOODS_STOCK_NOT_ENOUGH.code(), "");
        }
        return secKillDB(traceID, userID, goods, secNum, num);
    }

    public void setSuccessInPreSecKill(String traceID, Long userID, Goods goods, String secNum, PreSecKillRecord record) {
        Gson gson = new Gson();
        String recordStr = gson.toJson(record);
        List<String> result = redisBase.executeLuaReturnString(setSecKillSuccessLua, Arrays.asList("" + userID, "" + goods.getID(), secNum, recordStr));
    }

    @Transactional
    public String secKillDB(String traceID, Long userID, Goods goods, String secNum, Integer num) {
        Integer affectedRows = userQuotaMapper.incrKilledNum(userID, goods.getID(), num);
        if (affectedRows == 0) {
            UserQuota userQuota = new UserQuota();
            userQuota.setGoodsID(goods.getID());
            userQuota.setKilledNum(num);
            userQuota.setNum(0);
            userQuota.setUserID(userID);
            userQuotaMapper.save(userQuota);
        }
        if (secNum == "") {
            secNum = UUID.randomUUID().toString();
        }
        String orderNum = UUID.randomUUID().toString();
        affectedRows = stockMapper.descStock(goods.getID(), num);
        if (affectedRows == 0) {
            throw new BusinessException(ResponseEnum.ERR_GOODS_STOCK_NOT_ENOUGH.code(), "Stock not enouth");
        }
        Order order = new Order();
        order.setBuyer(userID);
        order.setSeller(goods.getSeller());
        order.setGoodsID(goods.getID());
        order.setGoodsNum(goods.getGoodsNum());
        order.setPrice(goods.getPrice());
        order.setOrderNum(orderNum);
        order.setStatus(SKStatusEnum.SK_STATUS_BEFORE_PAY.getValue());
        orderMapper.save(order);
        SecKillRecord record = new SecKillRecord();
        record.setUserID(userID);
        record.setGoodsID(goods.getID());
        record.setPrice(goods.getPrice());
        record.setOrderNum(order.getOrderNum());
        record.setSecNum(secNum);
        record.setStatus(SKStatusEnum.SK_STATUS_BEFORE_PAY.getValue());
        recordMapper.save(record);
        return orderNum;
    }

    public static class SecKillMsg {
        public String traceID;
        public Goods goods;
        public String secNum;
        public Long userID;
        public Integer num;
    }

    public String secKillTopic = "tp-seckill";
}

-- 秒杀系统数据库初始化脚本

USE `dark-miaosha`;

-- 商品表
CREATE TABLE IF NOT EXISTS t_goods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_num VARCHAR(64) NOT NULL UNIQUE,
    goods_name VARCHAR(128),
    price FLOAT,
    pic_url VARCHAR(256),
    seller BIGINT,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 秒杀库存表
CREATE TABLE IF NOT EXISTS t_seckill_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_id BIGINT NOT NULL,
    stock INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 全局限额表
CREATE TABLE IF NOT EXISTS t_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goods_id BIGINT,
    num INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户限额表
CREATE TABLE IF NOT EXISTS t_user_quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    goods_id BIGINT,
    num INT,
    killed_num INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_goods (user_id, goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_num VARCHAR(64) NOT NULL UNIQUE,
    buyer BIGINT,
    seller BIGINT,
    goods_id BIGINT,
    goods_num VARCHAR(64),
    price FLOAT,
    status INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 秒杀记录表
CREATE TABLE IF NOT EXISTS t_seckill_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sec_num VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT,
    goods_id BIGINT,
    order_num VARCHAR(64),
    price FLOAT,
    status INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入测试数据
INSERT INTO t_goods (goods_num, goods_name, price, pic_url, seller) VALUES 
('abc123', 'iPhone 15', 5999.0, 'https://example.com/iphone.jpg', 1);

INSERT INTO t_seckill_stock (goods_id, stock) VALUES (1, 100);

INSERT INTO t_quota (goods_id, num) VALUES (1, 2);

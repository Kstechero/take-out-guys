package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.context.BaseContext;
import com.sky.dto.CouponDTO;
import com.sky.dto.CouponPageQueryDTO;
import com.sky.entity.Coupon;
import com.sky.entity.UserCoupon;
import com.sky.exception.BaseException;
import com.sky.mapper.CouponMapper;
import com.sky.mapper.UserCouponMapper;
import com.sky.result.PageResult;
import com.sky.service.CouponService;
import com.sky.vo.UserCouponVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CouponServiceImpl implements CouponService {
    @Autowired private CouponMapper couponMapper;
    @Autowired private UserCouponMapper userCouponMapper;

    @Override
    public void save(CouponDTO dto) {
        validate(dto);
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(dto, coupon);
        LocalDateTime now = LocalDateTime.now();
        coupon.setRemainingCount(dto.getTotalCount());
        coupon.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        coupon.setCreateTime(now); coupon.setUpdateTime(now);
        coupon.setCreateUser(BaseContext.getCurrentId()); coupon.setUpdateUser(BaseContext.getCurrentId());
        couponMapper.insert(coupon);
    }

    @Override
    public PageResult pageQuery(CouponPageQueryDTO query) {
        if (query.getPage() < 1 || query.getPageSize() < 1) throw new BaseException("分页参数不正确");
        PageHelper.startPage(query.getPage(), query.getPageSize());
        Page<Coupon> result = couponMapper.pageQuery(query);
        return new PageResult(result.getTotal(), result.getResult());
    }

    @Override
    @Transactional
    public void update(Long id, CouponDTO dto) {
        validate(dto);
        Coupon old = couponMapper.getByIdForUpdate(id);
        if (old == null) throw new BaseException("优惠券不存在");
        int received = old.getTotalCount() - old.getRemainingCount();
        if (dto.getTotalCount() < received) throw new BaseException("发行总量不能小于已领取数量 " + received);
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(dto, coupon);
        coupon.setId(id); coupon.setRemainingCount(dto.getTotalCount() - received);
        coupon.setStatus(dto.getStatus() == null ? old.getStatus() : dto.getStatus());
        coupon.setUpdateTime(LocalDateTime.now()); coupon.setUpdateUser(BaseContext.getCurrentId());
        couponMapper.update(coupon);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Coupon coupon = couponMapper.getByIdForUpdate(id);
        if (coupon == null) throw new BaseException("优惠券不存在");
        if (userCouponMapper.countByCouponId(id) > 0) throw new BaseException("优惠券已被领取，不能删除，可将其停用");
        couponMapper.deleteById(id);
    }

    @Override
    public PageResult available(int page, int pageSize) {
        if (page < 1 || pageSize < 1) throw new BaseException("分页参数不正确");
        PageHelper.startPage(page, pageSize);
        Page<Coupon> result = couponMapper.availableForReceive(BaseContext.getCurrentId());
        return new PageResult(result.getTotal(), result.getResult());
    }

    @Override
    @Transactional
    public void receive(Long couponId) {
        Coupon coupon = couponMapper.getByIdForUpdate(couponId);
        if (coupon == null) throw new BaseException("优惠券不存在");
        LocalDateTime now = LocalDateTime.now();
        if (!Integer.valueOf(1).equals(coupon.getStatus())) throw new BaseException("优惠券已停用");
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) throw new BaseException("优惠券不在领取有效期内");
        if (coupon.getRemainingCount() == null || coupon.getRemainingCount() <= 0) throw new BaseException("优惠券已领完");
        Long userId = BaseContext.getCurrentId();
        if (userCouponMapper.countByUserAndCoupon(userId, couponId) >= coupon.getPerUserLimit()) throw new BaseException("已达到该优惠券领取上限");
        if (couponMapper.decreaseStock(couponId) != 1) throw new BaseException("优惠券已领完");
        userCouponMapper.insert(UserCoupon.builder().userId(userId).couponId(couponId).status(0)
                .receiveTime(now).expireTime(coupon.getValidUntil()).build());
    }

    @Override
    public List<UserCouponVO> myCoupons(Integer status) {
        if (status != null && (status < 0 || status > 2)) throw new BaseException("优惠券状态不正确");
        return userCouponMapper.listByUser(BaseContext.getCurrentId(), status);
    }

    @Override
    public List<Coupon> availableForOrder(BigDecimal amount, Long orderId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) throw new BaseException("订单金额不正确");
        return couponMapper.availableForOrder(BaseContext.getCurrentId(), amount);
    }

    @Override
    public Coupon getById(Long id) {
        return id == null ? null : couponMapper.getById(id);
    }

    @Override
    @Transactional
    public void changeStatusByAgent(Long id, Integer expectedStatus, Integer targetStatus) {
        if (id == null || expectedStatus == null || targetStatus == null
                || (targetStatus != 0 && targetStatus != 1)) {
            throw new BaseException("优惠券状态变更参数不正确");
        }
        if (couponMapper.updateStatusIfExpected(
                id, expectedStatus, targetStatus, BaseContext.getCurrentId()) != 1) {
            throw new BaseException("优惠券状态已变化，请重新查询后确认");
        }
    }

    private void validate(CouponDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getName())) throw new BaseException("优惠券名称不能为空");
        if (dto.getType() == null || dto.getType() < 1 || dto.getType() > 3) throw new BaseException("优惠券类型不正确");
        if (dto.getDiscountAmount() == null || dto.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) throw new BaseException("优惠金额必须大于0");
        if (dto.getMinimumAmount() == null || dto.getMinimumAmount().compareTo(BigDecimal.ZERO) < 0) throw new BaseException("使用门槛不能小于0");
        if (dto.getMinimumAmount().compareTo(BigDecimal.ZERO) > 0 && dto.getDiscountAmount().compareTo(dto.getMinimumAmount()) > 0) throw new BaseException("优惠金额不能高于使用门槛");
        if (dto.getTotalCount() == null || dto.getTotalCount() < 1) throw new BaseException("发行数量必须大于0");
        if (dto.getPerUserLimit() == null || dto.getPerUserLimit() < 1 || dto.getPerUserLimit() > dto.getTotalCount()) throw new BaseException("每人限领数量不正确");
        if (dto.getValidFrom() == null || dto.getValidUntil() == null || !dto.getValidUntil().isAfter(dto.getValidFrom())) throw new BaseException("优惠券有效期不正确");
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) throw new BaseException("优惠券状态不正确");
    }
}

package com.mall.demo.module.logistic.service;





import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.demo.common.enums.ErrorCodeEnum;
import com.mall.demo.common.exception.BizException;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.logistic.dto.req.CreateAddressReqDto;
import com.mall.demo.module.logistic.dto.req.UpdateAddressReqDto;
import com.mall.demo.module.logistic.dto.resp.GetAddressListRespDto;
import com.mall.demo.module.logistic.entity.Address;
import com.mall.demo.module.logistic.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GetAddressListRespDto createAddress(CreateAddressReqDto reqDto) {

        String userId = SecurityUtils.getId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), "用户未登录");
        }

        log.info("【地址管理】创建新地址, userId: {}", userId);

        Address address = new Address();
        address.setUserId(userId);
        address.setAddressName(reqDto.getAddressName());
        address.setPhone(reqDto.getPhone());
        address.setReceiver(reqDto.getReceiver());

        int rows = addressMapper.insert(address);

        GetAddressListRespDto dto = new GetAddressListRespDto();


        if (rows > 0) {
            log.info("【地址管理】地址创建成功, addressId: {}", address.getAddressId());
            dto.setAddressId(address.getAddressId());
            dto.setUserId(userId);
            dto.setAddressName(address.getAddressName());
            dto.setPhone(address.getPhone());
            dto.setReceiver(address.getReceiver());
            return dto;

        } else {
            log.error("【地址管理】地址创建失败");
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR.getCode(), "地址创建失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GetAddressListRespDto updateAddress(UpdateAddressReqDto dto) {

        String userId = SecurityUtils.getId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), "用户未登录");
        }

        if (dto.getAddressId() == null || dto.getAddressId().isEmpty()) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR.getCode(), "地址ID不能为空");
        }

        log.info("【地址管理】更新地址, userId: {}, addressId: {}", userId, dto.getAddressId());

        Address existAddress = addressMapper.selectById(dto.getAddressId());
        if (existAddress == null) {
            throw new BizException(ErrorCodeEnum.ADDRESS_NOT_EXIST.getCode(), ErrorCodeEnum.ADDRESS_NOT_EXIST.getMessage());
        }

        if (!userId.equals(existAddress.getUserId())) {
            throw new BizException(ErrorCodeEnum.NOT_PERMISSION.getCode(), ErrorCodeEnum.NOT_PERMISSION.getMessage());
        }

        Address address = new Address();
        address.setAddressId(dto.getAddressId());

        if (dto.getAddressName() != null && !dto.getAddressName().isEmpty()) {
            address.setAddressName(dto.getAddressName());
        }
        if (dto.getPhone() != null && !dto.getPhone().isEmpty()) {
            address.setPhone(dto.getPhone());
        }
        if (dto.getReceiver() != null && !dto.getReceiver().isEmpty()) {
            address.setReceiver(dto.getReceiver());
        }

        int rows = addressMapper.updateById(address);
        GetAddressListRespDto Respdto = new GetAddressListRespDto();
        if (rows > 0) {
            log.info("【地址管理】地址更新成功, addressId: {}", dto.getAddressId());
            Respdto.setAddressId(address.getAddressId());
            Respdto.setUserId(userId);
            Respdto.setAddressName(address.getAddressName());
            Respdto.setPhone(address.getPhone());
            Respdto.setReceiver(address.getReceiver());
            return Respdto;
        } else {
            log.error("【地址管理】地址更新失败");
            throw new BizException(ErrorCodeEnum.UPDATE_ERROR.getCode(), ErrorCodeEnum.UPDATE_ERROR.getMessage());
        }
    }





@Override
public void deleteAddress(String addressId) {
        String userId = SecurityUtils.getId();
        if (userId == null) {
            throw new BizException(
                    ErrorCodeEnum.USER_NOT_EXIST.getCode(),
                    ErrorCodeEnum.USER_NOT_EXIST.getMessage()
            );
        }

        Address existAddress = addressMapper.selectById(addressId);
        if (existAddress == null){
            throw new BizException(
                    ErrorCodeEnum.ADDRESS_NOT_EXIST.getCode(),
                    ErrorCodeEnum.ADDRESS_NOT_EXIST.getMessage()
            );
        }
        if (!userId.matches(existAddress.getUserId())){
            throw new BizException(
                    ErrorCodeEnum.NOT_PERMISSION.getCode(),
                    ErrorCodeEnum.NOT_PERMISSION.getMessage()
            );
        }

    // 执行删除
    int rows = addressMapper.deleteById(addressId);

    if (rows > 0) {
        log.info("【地址管理】地址删除成功, addressId: {}", addressId);
    } else {
        log.error("【地址管理】地址删除失败, addressId: {}", addressId);
        throw new BizException(ErrorCodeEnum.SYSTEM_ERROR.getCode(), "地址删除失败");
    }

}







    @Override
    public List<Address> getAddressList() {
        String userId = SecurityUtils.getId();
        if (userId == null) {
            throw new BizException(ErrorCodeEnum.USER_NOT_EXIST.getCode(), "用户未登录");
        }
        log.info("【地址管理】获取地址列表, userId: {}", userId);
        return addressMapper.selectList(new LambdaQueryWrapper<Address>().eq(Address::getUserId, userId));

    }

}

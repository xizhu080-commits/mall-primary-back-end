package com.mall.demo.module.logistic.controller;

import com.mall.demo.common.result.RestResp;
import com.mall.demo.common.util.SecurityUtils;
import com.mall.demo.module.logistic.dto.req.CreateAddressReqDto;
import com.mall.demo.module.logistic.dto.req.UpdateAddressReqDto;
import com.mall.demo.module.logistic.dto.resp.GetAddressListRespDto;
import com.mall.demo.module.logistic.entity.Address;
import com.mall.demo.module.logistic.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    /*
    * 新建地址
    * */
    @PostMapping("/create")
    public RestResp<GetAddressListRespDto> createAddress(@Valid @RequestBody CreateAddressReqDto dto) {
        return RestResp.ok(addressService.createAddress(dto));
    }


    /*
    * 修改地址
    * */
    @PatchMapping("/update")
    public RestResp<GetAddressListRespDto> updateAddress(@Valid @RequestBody UpdateAddressReqDto dto) {
        return RestResp.ok(addressService.updateAddress(dto));
    }

    @DeleteMapping("/delete")
@Operation(summary = "删除地址", description = "删除指定的收货地址")
public RestResp<Void> deleteAddress(@RequestParam String addressId) {
    addressService.deleteAddress(addressId);
    return RestResp.ok("删除成功", null);
}


    /*
    * 获取地址列表
    *
    * */
    @GetMapping("/list")
    public RestResp<List<Address>> getAddressList() {
        return RestResp.ok(addressService.getAddressList());
    }

}

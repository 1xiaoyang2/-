package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.utils.thread.WmThreadLocalUtils;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;


@Slf4j
@Service
@Transactional
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    public ResponseResult uploadPicture(MultipartFile multipartFile) {
        //检查参数
        if(multipartFile == null || multipartFile.getSize() == 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //上传图片到minio
        String prefix = UUID.randomUUID().toString().replace("-", "");
        String postfix = multipartFile.getOriginalFilename().substring(multipartFile.getOriginalFilename().lastIndexOf("."));
        String fileName = prefix + postfix;
        String fileId = null;
        try {
            fileId = fileStorageService.uploadImgFile("", fileName, multipartFile.getInputStream());
            log.info("上传图片到minio中，fileId：{}",fileId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //保存到数据库中
        WmMaterial wmMaterial = new WmMaterial();
        wmMaterial.setUserId(WmThreadLocalUtils.getUser().getId());
        wmMaterial.setUrl(fileId);
        wmMaterial.setIsCollection((short)0);
        wmMaterial.setType((short)0);
        wmMaterial.setCreatedTime(new LocalDateTime().toDate(TimeZone.getTimeZone("GMT+8")));
        save(wmMaterial);

        return ResponseResult.okResult(wmMaterial);
    }

    @Override
    public ResponseResult getList(WmMaterialDto dto) {
        //检查
        dto.checkParam();

        IPage page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmMaterial> wrapper = new LambdaQueryWrapper<>();
        //是否收藏
        if(dto.getIsCollection() != null && dto.getIsCollection() != 1){
            wrapper.eq(WmMaterial::getIsCollection,dto.getIsCollection());
        }
        wrapper.eq(WmMaterial::getUserId, WmThreadLocalUtils.getUser().getId());

        wrapper.orderByDesc(WmMaterial::getCreatedTime);
        page = page(page, wrapper);

        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());
        return responseResult;
    }
}

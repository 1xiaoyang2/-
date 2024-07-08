package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constans.WemediaConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.thread.WmThreadLocalUtils;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl  extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {
    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;
    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;
    @Override
    public ResponseResult getList(WmNewsPageReqDto dto) {
        //检查参数
        dto.checkParam();

        IPage iPage = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmNews> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus());
        wrapper.eq(dto.getChannelId() != null, WmNews::getChannelId, dto.getChannelId());
        wrapper.like(dto.getKeyword() != null, WmNews::getTitle, dto.getKeyword());
        wrapper.between(dto.getBeginPubDate() != null && dto.getEndPubDate() != null,
                WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate());
        wrapper.eq(WmNews::getUserId, WmThreadLocalUtils.getUser().getId());
        wrapper.orderByDesc(WmNews::getPublishTime);

        IPage page = page(iPage, wrapper);

        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    @Override
    public ResponseResult submitNews(WmNewsDto dto) {
        //参数校验
        if(dto == null || dto.getContent() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //删除之前素材与文章的中间关系表
        List<Integer> integers = wmNewsMaterialMapper
                .selectList(new LambdaQueryWrapper<WmNewsMaterial>().eq(WmNewsMaterial::getNewsId, dto.getId()))
                .stream().map(WmNewsMaterial::getId).collect(Collectors.toList());
        wmNewsMaterialMapper.delete(new LambdaQueryWrapper<WmNewsMaterial>()
                .in(!CollectionUtils.isEmpty(integers), WmNewsMaterial::getNewsId, dto.getId()));
        //添加文案
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto, wmNews);
        wmNews.setUserId(WmThreadLocalUtils.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short) 1); //默认上架
        if(!CollectionUtils.isEmpty(dto.getImages())){
            wmNews.setImages(StringUtils.join(dto.getImages(),","));
        }
        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            wmNews.setType(null);
        }
        if(wmNews.getId() == null){
            save(wmNews);
        } else {
            wmNews.setId(null);
            updateById(wmNews);
        }

        //从文案内容中拿到image的url
        List<String> materials = JSON.parseArray(dto.getContent(), Map.class).stream()
                .filter(map -> map.get("type").equals("image"))
                .map(map -> (String)map.get("value")).collect(Collectors.toList());

        savaRelativeInfo(wmNews, materials, WemediaConstants.WM_CONTENT_REFERENCE);

        //添加文章封面图片与文章的关系
        //封面类型为自动
        List<String> images = dto.getImages();
        if(dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            if(materials.size() >= 3){
                //多图
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images = materials.stream().limit(3).collect(Collectors.toList());
            } else if (materials.size() >= 1 && materials.size() < 3) {
                //单图
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = materials.stream().limit(1).collect(Collectors.toList());
            }else {
                //无图
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }
            if(!CollectionUtils.isEmpty(images)){
                wmNews.setImages(StringUtils.join(images,","));
            }
            updateById(wmNews);
        }
        //添加文章封面图片与文章的关系
        savaRelativeInfo(wmNews, images, WemediaConstants.WM_COVER_REFERENCE);

        wmNewsTaskService.addNewsToTask(wmNews.getId(),wmNews.getPublishTime());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    private void savaRelativeInfo(WmNews wmNews, List<String> materials, Short type) {
        //获取image的id
        List<Integer> ids = wmMaterialMapper
                .selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, materials))
                .stream().map(WmMaterial::getId).collect(Collectors.toList());
        //判断素材是否有效
        if(CollectionUtils.isEmpty(ids)){
            throw new CustomException(AppHttpCodeEnum.MATERIALS_REFERENCE_FAIL);
        }
        if(ids.size() != materials.size()){
            throw new CustomException(AppHttpCodeEnum.MATERIALS_REFERENCE_FAIL);
        }
        //添加文章内容图片与文章的关系
        wmNewsMaterialMapper.saveRelations(ids, wmNews.getId(), type);
    }

    /**
     * 文章的上下架
     * @param dto
     * @return
     */
    @Override
    public ResponseResult downOrUp(WmNewsDto dto) {
        //1.检查参数
        if(dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询文章
        WmNews wmNews = getById(dto.getId());
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"文章不存在");
        }

        //3.判断文章是否已发布
        if(!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"当前文章不是发布状态，不能上下架");
        }

        //4.修改文章enable
        if(dto.getEnable() != null && dto.getEnable() > -1 && dto.getEnable() < 2){
            update(Wrappers.<WmNews>lambdaUpdate().set(WmNews::getEnable,dto.getEnable())
                    .eq(WmNews::getId,wmNews.getId()));
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}

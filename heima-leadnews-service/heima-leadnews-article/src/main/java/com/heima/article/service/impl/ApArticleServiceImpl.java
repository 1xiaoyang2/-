package com.heima.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.ApArticleService;
import com.heima.common.constans.ArticleConstants;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.nntp.Article;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {

    @Autowired
    private ApArticleMapper apArticleMapper;
    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;
    @Autowired
    private ApArticleContentMapper apArticleContentMapper;
    @Override
    public ResponseResult load(ArticleHomeDto dto, Short type) {
        //1.参数检验
        // size
        Integer size = dto.getSize();
        if (size == null || size == 0) {
            dto.setSize(10);
        }
        size = Math.min(size, 50);
        dto.setSize(size);
        //type
        if (!type.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstants.LOADTYPE_LOAD_NEW)) {
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //tag
        if (StringUtils.isEmpty(dto.getTag())) {
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        //time
        if (dto.getMaxBehotTime() == null) dto.setMaxBehotTime(new Date());
        if (dto.getMinBehotTime() == null) dto.setMinBehotTime(new Date());

        //2.查询
        List<ApArticle> apArticleList = apArticleMapper.loadArticleList(dto, type);
        return ResponseResult.okResult(apArticleList);
    }

    @Override
    public ResponseResult savaArticle(ArticleDto dto) {
        //参数校验
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //属性拷贝
        ApArticle article = new ApArticle();
        BeanUtils.copyProperties(dto,article);

        //判断文章是否为修改还是保存
        if(dto.getId() == null){
            //保存文章
            save(article);

            //保存文章配置
            ApArticleConfig apArticleConfig = new ApArticleConfig(article.getId());
            apArticleConfigMapper.insert(apArticleConfig);

            //保存文章内容
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setContent(dto.getContent());
            apArticleContent.setArticleId(article.getId());
            apArticleContentMapper.insert(apArticleContent);
        }else {
            //修改文章
            updateById(article);

            //修改文章内容
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setContent(dto.getContent());
            apArticleContent.setArticleId(article.getId());
            apArticleContentMapper.update(apArticleContent,
                    new LambdaUpdateWrapper<ApArticleContent>().eq(ApArticleContent::getArticleId,article.getId()));
        }


        return ResponseResult.okResult(article.getId());
    }
}

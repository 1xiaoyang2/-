package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.apis.wemedia.IWemediaClient;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.constans.ArticleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vos.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class HotArticleServiceImpl implements HotArticleService {
    @Autowired
    private ApArticleMapper apArticleMapper;
    @Autowired
    private IWemediaClient wemediaClient;
    @Autowired
    private CacheService cacheService;

    @Override
    public void computeHotArticle() {
        //查询前5天的文章
        Date date = DateTime.now().minusDays(5).toDate();
        List<ApArticle> apArticleList = apArticleMapper.findArticleListByLast5days(date);

        //计算文章分值
        List<HotArticleVo> hotArticleVoList = computeHotArticleScore(apArticleList);

        //为每一个频道缓存30条分值最高的文章
        cacheTagToRedis(hotArticleVoList);
    }

    private void cacheTagToRedis(List<HotArticleVo> hotArticleVoList) {
        //为每一个频道缓存30条分值最高的文章
        ResponseResult result = wemediaClient.getChannels();
        if (result.getCode().equals(200)) {
            String jsonData = JSON.toJSONString(result.getData());
            List<WmChannel> wmChannels = JSON.parseArray(jsonData, WmChannel.class);
            if (!CollectionUtils.isEmpty(wmChannels)) {
                for (WmChannel wmChannel : wmChannels) {
                    List<HotArticleVo> collect = hotArticleVoList
                            .stream()
                            .filter(h -> h.getChannelId().equals(wmChannel.getId()))
                            .sorted(Comparator.comparing(HotArticleVo::getScore).reversed())
                            .limit(30).collect(Collectors.toList());
                    cacheService.set(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + wmChannel.getId(),JSON.toJSONString(collect));
                }
            }
        }

        //设置推荐数据
        List<HotArticleVo> collect = hotArticleVoList
                .stream()
                .sorted(Comparator.comparing(HotArticleVo::getScore).reversed())
                .limit(30).collect(Collectors.toList());
        cacheService.set(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG,JSON.toJSONString(collect));
    }


    private List<HotArticleVo> computeHotArticleScore(List<ApArticle> apArticleList) {
        List<HotArticleVo> hotArticleVoList = new ArrayList<>();

        if (apArticleList != null && apArticleList.size() > 0) {
            for (ApArticle apArticle : apArticleList) {
                HotArticleVo hot = new HotArticleVo();
                BeanUtils.copyProperties(apArticle, hot);
                Integer score = computeScore(apArticle);
                hot.setScore(score);
                hotArticleVoList.add(hot);
            }
        }
        return hotArticleVoList;
    }

    /**
     * 计算文章的具体分值
     *
     * @param apArticle
     * @return
     */
    private Integer computeScore(ApArticle apArticle) {
        Integer scere = 0;
        if (apArticle.getLikes() != null) {
            scere += apArticle.getLikes() * ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }
        if (apArticle.getViews() != null) {
            scere += apArticle.getViews();
        }
        if (apArticle.getComment() != null) {
            scere += apArticle.getComment() * ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }
        if (apArticle.getCollection() != null) {
            scere += apArticle.getCollection() * ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }

        return scere;
    }
}

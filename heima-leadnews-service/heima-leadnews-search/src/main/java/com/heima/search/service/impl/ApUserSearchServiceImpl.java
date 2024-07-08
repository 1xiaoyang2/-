package com.heima.search.service.impl;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.common.user.pojos.ApUser;
import com.heima.model.search.dtos.HistorySearchDto;
import com.heima.search.pojos.ApUserSearch;
import com.heima.search.service.ApUserSearchService;
import com.heima.utils.thread.AppThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;


@Service
@Slf4j
public class ApUserSearchServiceImpl implements ApUserSearchService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    @Async
    public void insert(String keyword, Integer userId) {
        //查询mongodb中是否存在
        ApUserSearch apUserSearch = mongoTemplate.findOne(
                Query.query(Criteria.where("keyword").is(keyword).and("userId").is(userId))
                , ApUserSearch.class);

        //存在，更新时间
        if(apUserSearch != null){
            apUserSearch.setCreatedTime(new Date());
            mongoTemplate.save(apUserSearch);
            return;
        }

        //不存在，插入数据(数据上限为10条)
        apUserSearch = new ApUserSearch();
        apUserSearch.setUserId(userId);
        apUserSearch.setKeyword(keyword);
        apUserSearch.setCreatedTime(new Date());
        List<ApUserSearch> apUserSearchList = mongoTemplate.find(Query.query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "createdTime")), ApUserSearch.class);

        if(apUserSearchList.size() >= 10){
            //替换
            ApUserSearch lastUserSearch = apUserSearchList.get(apUserSearchList.size() - 1);
            mongoTemplate.findAndReplace(Query.query(Criteria.where("id").is(lastUserSearch.getId())), apUserSearch);
        }else {
            //插入
            mongoTemplate.save(apUserSearch);
        }
    }

    @Override
    public ResponseResult findUserSearch() {
        //获取用户
        ApUser user = AppThreadLocalUtils.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //查询搜索记录
        List<ApUserSearch> apUserSearchList = mongoTemplate.find(
                Query.query(Criteria.where("userId").is(user.getId()))
                        .with(Sort.by(Sort.Direction.DESC, "createdTime")), ApUserSearch.class);

        return ResponseResult.okResult(apUserSearchList);
    }

    @Override
    public ResponseResult delUserSearch(HistorySearchDto dto) {
        //1.检查参数
        if(dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.判断是否登录
        ApUser user = AppThreadLocalUtils.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //3.删除
        mongoTemplate.remove(Query.query(Criteria.where("userId").is(user.getId()).and("id").is(dto.getId())),ApUserSearch.class);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}

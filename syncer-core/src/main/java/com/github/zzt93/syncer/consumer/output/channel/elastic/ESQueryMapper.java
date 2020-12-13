package com.github.zzt93.syncer.consumer.output.channel.elastic;

import com.github.zzt93.syncer.common.data.ExtraQuery;
import com.github.zzt93.syncer.consumer.output.channel.mapper.ExtraQueryMapper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.support.AbstractClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author zzt
 */
public class ESQueryMapper implements ExtraQueryMapper {

  private final AbstractClient client;
  private final Logger logger = LoggerFactory.getLogger(ESQueryMapper.class);

  public ESQueryMapper(AbstractClient client) {
    this.client = client;
  }

  @Override
  public Map<String, Object> map(ExtraQuery extraQuery) {
    String[] select = extraQuery.getSelect();
    Optional<QueryBuilder> filter = extraQuery.getEsFilter();
    if (!filter.isPresent()) {
      return Collections.emptyMap();
    }
    SearchResponse response;
    try {
      response = client.prepareSearch(extraQuery.getIndexName())
          .setTypes(extraQuery.getTypeName())
          .setSearchType(SearchType.DEFAULT)
          .setFetchSource(select, null)
          .setQuery(filter.get())
          .execute()
          .actionGet();
    } catch (Exception e) {
      logger.error("Fail to do the extra query {}", extraQuery, e);
      return Collections.emptyMap();
    }
    SearchHits hits = response.getHits();
    if (hits.totalHits > 1) {
      // todo toList
      logger.warn("Multiple query results exists, only use the first");
    } else if (hits.totalHits == 0) {
      logger.warn("Fail to find any match by {}", extraQuery);
      return Collections.emptyMap();
    }
    Map<String, Object> hit = hits.getAt(0).getSource();
    Map<String, Object> res = new HashMap<>();
    for (int i = 0; i < select.length; i++) {
      Object value = hit.get(select[i]);
      if (value == null) {
        logger.warn("No {} in {} by {}", select[i], hit, extraQuery);
      }
      res.put(extraQuery.getAs(i), value);
    }
    extraQuery.addQueryResult(res);
    return res;
  }


  /**
   * @param str special placeholder: $userId$
   * means this key is the result of previous query
   */
  private Optional<String> getPlaceholderValue(String str) {
    if (str.startsWith("$") && str.endsWith("$")) {
      return Optional.of(str.substring(1, str.length() - 1));
    }
    return Optional.empty();
  }

}

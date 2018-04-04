package com.github.zzt93.syncer.consumer.filter.impl;

import com.github.zzt93.syncer.common.data.SyncData;
import com.github.zzt93.syncer.common.thread.ThreadSafe;
import com.github.zzt93.syncer.consumer.filter.ExprFilter;
import java.util.List;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author zzt
 */
public class Statement implements ExprFilter, IfBodyAction {

  private final FilterActions filterActions;

  public Statement(SpelExpressionParser parser, List<String> statement) {
    this.filterActions = new FilterActions(parser, statement);
  }

  @ThreadSafe(safe = {FilterActions.class, SpelExpressionParser.class})
  @Override
  public Void decide(List<SyncData> dataList) {
    for (SyncData syncData : dataList) {
      StandardEvaluationContext context = syncData.getContext();
      filterActions.execute(context);
    }
    return null;
  }

  @Override
  public Object execute(SyncData data) {
    filterActions.execute(data.getContext());
    return FilterRes.ACCEPT;
  }
}

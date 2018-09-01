/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.core.rewrite;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.optimizer.condition.ShardingConditions;
import io.shardingsphere.core.parsing.lexer.token.DefaultKeyword;
import io.shardingsphere.core.parsing.parser.context.OrderItem;
import io.shardingsphere.core.parsing.parser.context.limit.Limit;
import io.shardingsphere.core.parsing.parser.sql.SQLStatement;
import io.shardingsphere.core.parsing.parser.sql.dml.insert.InsertStatement;
import io.shardingsphere.core.parsing.parser.sql.dql.select.SelectStatement;
import io.shardingsphere.core.parsing.parser.token.IndexToken;
import io.shardingsphere.core.parsing.parser.token.InsertColumnToken;
import io.shardingsphere.core.parsing.parser.token.InsertValuesToken;
import io.shardingsphere.core.parsing.parser.token.ItemsToken;
import io.shardingsphere.core.parsing.parser.token.OffsetToken;
import io.shardingsphere.core.parsing.parser.token.OrderByToken;
import io.shardingsphere.core.parsing.parser.token.RowCountToken;
import io.shardingsphere.core.parsing.parser.token.SQLToken;
import io.shardingsphere.core.parsing.parser.token.SchemaToken;
import io.shardingsphere.core.parsing.parser.token.TableToken;
import io.shardingsphere.core.metadata.datasource.ShardingDataSourceMetaData;
import io.shardingsphere.core.rewrite.placeholder.IndexPlaceholder;
import io.shardingsphere.core.rewrite.placeholder.InsertValuesPlaceholder;
import io.shardingsphere.core.rewrite.placeholder.SchemaPlaceholder;
import io.shardingsphere.core.rewrite.placeholder.TablePlaceholder;
import io.shardingsphere.core.routing.SQLUnit;
import io.shardingsphere.core.routing.type.RoutingTable;
import io.shardingsphere.core.routing.type.TableUnit;
import io.shardingsphere.core.rule.BindingTableRule;
import io.shardingsphere.core.rule.ShardingRule;
import io.shardingsphere.core.util.SQLUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * SQL rewrite engine.
 * 
 * <p>Rewrite logic SQL to actual SQL, should rewrite table name and optimize something.</p>
 *
 * @author zhangliang
 * @author maxiaoguang
 * @author panjuan
 */
public final class SQLRewriteEngine {
    
    private final ShardingRule shardingRule;
    
    private final String originalSQL;
    
    private final DatabaseType databaseType;
    
    private final List<SQLToken> sqlTokens = new LinkedList<>();
    
    private final SQLStatement sqlStatement;
    
    private final ShardingConditions shardingConditions;
    
    private final List<Object> parameters;
    
    /**
     * Constructs SQL rewrite engine.
     * 
     * @param shardingRule databases and tables sharding rule
     * @param originalSQL original SQL
     * @param databaseType database type
     * @param sqlStatement SQL statement
     * @param shardingConditions sharding conditions
     * @param parameters parameters
     */
    public SQLRewriteEngine(final ShardingRule shardingRule, final String originalSQL, final DatabaseType databaseType,
                            final SQLStatement sqlStatement, final ShardingConditions shardingConditions, final List<Object> parameters) {
        this.shardingRule = shardingRule;
        this.originalSQL = originalSQL;
        this.databaseType = databaseType;
        this.sqlStatement = sqlStatement;
        this.shardingConditions = shardingConditions;
        this.parameters = parameters;
        sqlTokens.addAll(sqlStatement.getSqlTokens());
    }
    
    /**
     * rewrite SQL.
     *
     * @param isRewriteLimit is rewrite limit
     * @return SQL builder
     */
    public SQLBuilder rewrite(final boolean isRewriteLimit) {
        SQLBuilder result = new SQLBuilder(parameters);
        if (sqlTokens.isEmpty()) {
            result.appendLiterals(originalSQL);
            return result;
        }
        int count = 0;
        sortByBeginPosition();
        for (SQLToken each : sqlTokens) {
            if (0 == count) {
                result.appendLiterals(originalSQL.substring(0, each.getBeginPosition()));
            }
            if (each instanceof TableToken) {
                appendTablePlaceholder(result, (TableToken) each, count, sqlTokens);
            } else if (each instanceof SchemaToken) {
                appendSchemaPlaceholder(result, (SchemaToken) each, count, sqlTokens);
            } else if (each instanceof IndexToken) {
                appendIndexPlaceholder(result, (IndexToken) each, count, sqlTokens);
            } else if (each instanceof ItemsToken) {
                appendItemsToken(result, (ItemsToken) each, count, sqlTokens);
            } else if (each instanceof InsertValuesToken) {
                appendInsertValuesToken(result, (InsertValuesToken) each, count, sqlTokens);
            } else if (each instanceof RowCountToken) {
                appendLimitRowCount(result, (RowCountToken) each, count, sqlTokens, isRewriteLimit);
            } else if (each instanceof OffsetToken) {
                appendLimitOffsetToken(result, (OffsetToken) each, count, sqlTokens, isRewriteLimit);
            } else if (each instanceof OrderByToken) {
                appendOrderByToken(result, count, sqlTokens);
            } else if (each instanceof InsertColumnToken) {
                appendSymbolToken(result, (InsertColumnToken) each, count, sqlTokens);
            }
            count++;
        }
        return result;
    }
    
    private void sortByBeginPosition() {
        Collections.sort(sqlTokens, new Comparator<SQLToken>() {
            
            @Override
            public int compare(final SQLToken o1, final SQLToken o2) {
                return o1.getBeginPosition() - o2.getBeginPosition();
            }
        });
    }
    
    private void appendTablePlaceholder(final SQLBuilder sqlBuilder, final TableToken tableToken, final int count, final List<SQLToken> sqlTokens) {
        sqlBuilder.appendPlaceholder(new TablePlaceholder(tableToken.getTableName().toLowerCase(), tableToken.getOriginalLiterals()));
        int beginPosition = tableToken.getBeginPosition() + tableToken.getSkippedSchemaNameLength() + tableToken.getOriginalLiterals().length();
        appendRest(sqlBuilder, count, sqlTokens, beginPosition);
    }
    
    private void appendSchemaPlaceholder(final SQLBuilder sqlBuilder, final SchemaToken schemaToken, final int count, final List<SQLToken> sqlTokens) {
        sqlBuilder.appendPlaceholder(new SchemaPlaceholder(schemaToken.getSchemaName().toLowerCase(), schemaToken.getTableName().toLowerCase()));
        int beginPosition = schemaToken.getBeginPosition() + schemaToken.getOriginalLiterals().length();
        appendRest(sqlBuilder, count, sqlTokens, beginPosition);
    }
    
    private void appendIndexPlaceholder(final SQLBuilder sqlBuilder, final IndexToken indexToken, final int count, final List<SQLToken> sqlTokens) {
        String indexName = indexToken.getIndexName().toLowerCase();
        String logicTableName = indexToken.getTableName().toLowerCase();
        if (Strings.isNullOrEmpty(logicTableName)) {
            logicTableName = shardingRule.getLogicTableName(indexName);
        }
        sqlBuilder.appendPlaceholder(new IndexPlaceholder(indexName, logicTableName));
        int beginPosition = indexToken.getBeginPosition() + indexToken.getOriginalLiterals().length();
        appendRest(sqlBuilder, count, sqlTokens, beginPosition);
    }
    
    private void appendItemsToken(final SQLBuilder sqlBuilder, final ItemsToken itemsToken, final int count, final List<SQLToken> sqlTokens) {
        for (int i = 0; i < itemsToken.getItems().size(); i++) {
            if (itemsToken.isFirstOfItemsSpecial() && 0 == i) {
                sqlBuilder.appendLiterals(SQLUtil.getOriginalValue(itemsToken.getItems().get(i), databaseType));
            } else {
                sqlBuilder.appendLiterals(", ");
                sqlBuilder.appendLiterals(SQLUtil.getOriginalValue(itemsToken.getItems().get(i), databaseType));
            }
        }
        appendRest(sqlBuilder, count, sqlTokens, itemsToken.getBeginPosition());
    }
    
    private void appendInsertValuesToken(final SQLBuilder sqlBuilder, final InsertValuesToken insertValuesToken, final int count, final List<SQLToken> sqlTokens) {
        sqlBuilder.appendPlaceholder(new InsertValuesPlaceholder(insertValuesToken.getTableName().toLowerCase(), shardingConditions));
        appendRest(sqlBuilder, count, sqlTokens, ((InsertStatement) sqlStatement).getInsertValuesListLastPosition());
    }
    
    private void appendLimitRowCount(final SQLBuilder sqlBuilder, final RowCountToken rowCountToken, final int count, final List<SQLToken> sqlTokens, final boolean isRewrite) {
        SelectStatement selectStatement = (SelectStatement) sqlStatement;
        Limit limit = selectStatement.getLimit();
        if (!isRewrite) {
            sqlBuilder.appendLiterals(String.valueOf(rowCountToken.getRowCount()));
        } else if ((!selectStatement.getGroupByItems().isEmpty() || !selectStatement.getAggregationSelectItems().isEmpty()) && !selectStatement.isSameGroupByAndOrderByItems()) {
            sqlBuilder.appendLiterals(String.valueOf(Integer.MAX_VALUE));
        } else {
            sqlBuilder.appendLiterals(String.valueOf(limit.isNeedRewriteRowCount() ? rowCountToken.getRowCount() + limit.getOffsetValue() : rowCountToken.getRowCount()));
        }
        int beginPosition = rowCountToken.getBeginPosition() + String.valueOf(rowCountToken.getRowCount()).length();
        appendRest(sqlBuilder, count, sqlTokens, beginPosition);
    }
    
    private void appendLimitOffsetToken(final SQLBuilder sqlBuilder, final OffsetToken offsetToken, final int count, final List<SQLToken> sqlTokens, final boolean isRewrite) {
        sqlBuilder.appendLiterals(isRewrite ? "0" : String.valueOf(offsetToken.getOffset()));
        int beginPosition = offsetToken.getBeginPosition() + String.valueOf(offsetToken.getOffset()).length();
        appendRest(sqlBuilder, count, sqlTokens, beginPosition);
    }
    
    private void appendOrderByToken(final SQLBuilder sqlBuilder, final int count, final List<SQLToken> sqlTokens) {
        SelectStatement selectStatement = (SelectStatement) sqlStatement;
        StringBuilder orderByLiterals = new StringBuilder();
        orderByLiterals.append(" ").append(DefaultKeyword.ORDER).append(" ").append(DefaultKeyword.BY).append(" ");
        int i = 0;
        for (OrderItem each : selectStatement.getOrderByItems()) {
            String columnLabel = Strings.isNullOrEmpty(each.getColumnLabel()) ? String.valueOf(each.getIndex()) : SQLUtil.getOriginalValue(each.getColumnLabel(), databaseType);
            if (0 == i) {
                orderByLiterals.append(columnLabel).append(" ").append(each.getOrderDirection().name());
            } else {
                orderByLiterals.append(",").append(columnLabel).append(" ").append(each.getOrderDirection().name());
            }
            i++;
        }
        orderByLiterals.append(" ");
        sqlBuilder.appendLiterals(orderByLiterals.toString());
        int beginPosition = ((SelectStatement) sqlStatement).getGroupByLastPosition();
        appendRest(sqlBuilder, count, sqlTokens, beginPosition);
    }
    
    private void appendSymbolToken(final SQLBuilder sqlBuilder, final InsertColumnToken insertColumnToken, final int count, final List<SQLToken> sqlTokens) {
        sqlBuilder.appendLiterals(insertColumnToken.getColumnName());
        appendRest(sqlBuilder, count, sqlTokens, insertColumnToken.getBeginPosition());
    }
    
    private void appendRest(final SQLBuilder sqlBuilder, final int count, final List<SQLToken> sqlTokens, final int beginPosition) {
        int endPosition = sqlTokens.size() - 1 == count ? originalSQL.length() : sqlTokens.get(count + 1).getBeginPosition();
        sqlBuilder.appendLiterals(originalSQL.substring(beginPosition, endPosition));
    }
    
    /**
     * Generate SQL string.
     * 
     * @param tableUnit route table unit
     * @param sqlBuilder SQL builder
     * @param shardingDataSourceMetaData sharding data source meta data
     * @return SQL unit
     */
    public SQLUnit generateSQL(final TableUnit tableUnit, final SQLBuilder sqlBuilder, final ShardingDataSourceMetaData shardingDataSourceMetaData) {
        return sqlBuilder.toSQL(tableUnit, getTableTokens(tableUnit), shardingRule, shardingDataSourceMetaData);
    }
   
    private Map<String, String> getTableTokens(final TableUnit tableUnit) {
        Map<String, String> result = new HashMap<>();
        for (RoutingTable routingTable : tableUnit.getRoutingTables()) {
            String logicTableName = routingTable.getLogicTableName().toLowerCase();
            result.put(logicTableName, routingTable.getActualTableName());
            Optional<BindingTableRule> bindingTableRule = shardingRule.findBindingTableRule(logicTableName);
            if (bindingTableRule.isPresent()) {
                result.putAll(getBindingTableTokens(tableUnit.getDataSourceName(), routingTable, bindingTableRule.get()));
            }
        }
        return result;
    }
    
    private Map<String, String> getBindingTableTokens(final String dataSourceName, final RoutingTable routingTable, final BindingTableRule bindingTableRule) {
        Map<String, String> result = new HashMap<>();
        for (String eachTable : sqlStatement.getTables().getTableNames()) {
            String tableName = eachTable.toLowerCase();
            if (!tableName.equals(routingTable.getLogicTableName().toLowerCase()) && bindingTableRule.hasLogicTable(tableName)) {
                result.put(tableName, bindingTableRule.getBindingActualTable(dataSourceName, tableName, routingTable.getActualTableName()));
            }
        }
        return result;
    }
}

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

package io.shardingsphere.core.parsing.parser.dialect.mysql.sql;

import io.shardingsphere.core.parsing.lexer.LexerEngine;
import io.shardingsphere.core.parsing.lexer.dialect.mysql.MySQLKeyword;
import io.shardingsphere.core.parsing.lexer.token.DefaultKeyword;
import io.shardingsphere.core.parsing.parser.clause.TableReferencesClauseParser;
import io.shardingsphere.core.parsing.parser.dialect.mysql.statement.ShowColumnsStatement;
import io.shardingsphere.core.parsing.parser.dialect.mysql.statement.ShowCreateTableStatement;
import io.shardingsphere.core.parsing.parser.dialect.mysql.statement.ShowDatabasesStatement;
import io.shardingsphere.core.parsing.parser.dialect.mysql.statement.ShowIndexStatement;
import io.shardingsphere.core.parsing.parser.dialect.mysql.statement.ShowOtherStatement;
import io.shardingsphere.core.parsing.parser.dialect.mysql.statement.ShowTableStatusStatement;
import io.shardingsphere.core.parsing.parser.dialect.mysql.statement.ShowTablesStatement;
import io.shardingsphere.core.parsing.parser.sql.dal.DALStatement;
import io.shardingsphere.core.parsing.parser.sql.dal.show.AbstractShowParser;
import io.shardingsphere.core.parsing.parser.token.RemoveToken;
import io.shardingsphere.core.parsing.parser.token.SchemaToken;
import io.shardingsphere.core.rule.ShardingRule;
import lombok.RequiredArgsConstructor;

/**
 * Show parser for MySQL.
 *
 * @author zhangliang
 */
@RequiredArgsConstructor
public final class MySQLShowParser extends AbstractShowParser {
    
    private final LexerEngine lexerEngine;
    
    private final TableReferencesClauseParser tableReferencesClauseParser;
    
    public MySQLShowParser(final ShardingRule shardingRule, final LexerEngine lexerEngine) {
        this.lexerEngine = lexerEngine;
        tableReferencesClauseParser = new TableReferencesClauseParser(shardingRule, lexerEngine);
    }
    
    @Override
    public DALStatement parse() {
        lexerEngine.nextToken();
        lexerEngine.skipIfEqual(DefaultKeyword.FULL);
        if (lexerEngine.equalAny(MySQLKeyword.DATABASES)) {
            return showDatabases();
        }
        if (lexerEngine.skipIfEqual(DefaultKeyword.TABLE, MySQLKeyword.STATUS)) {
            return parseShowTableStatus();
        }
        if (lexerEngine.skipIfEqual(MySQLKeyword.TABLES)) {
            return parseShowTables();
        }
        if (lexerEngine.skipIfEqual(MySQLKeyword.COLUMNS, MySQLKeyword.FIELDS)) {
            return parseShowColumnsFields();
        }
        if (lexerEngine.skipIfEqual(DefaultKeyword.CREATE) && lexerEngine.skipIfEqual(DefaultKeyword.TABLE)) {
            return parseShowCreateTable();
        }
        if (lexerEngine.skipIfEqual(DefaultKeyword.INDEX, MySQLKeyword.INDEXES, MySQLKeyword.KEYS)) {
            return parseShowIndex();
        }
        return new ShowOtherStatement();
    }
    
    private DALStatement showDatabases() {
        return new ShowDatabasesStatement();
    }
    
    private DALStatement parseShowTableStatus() {
        return new ShowTableStatusStatement();
    }
    
    private DALStatement parseShowTables() {
        DALStatement result = new ShowTablesStatement();
        if (lexerEngine.equalAny(DefaultKeyword.FROM, DefaultKeyword.IN)) {
            int beginPosition = lexerEngine.getCurrentToken().getEndPosition() - lexerEngine.getCurrentToken().getLiterals().length();
            lexerEngine.nextToken();
            lexerEngine.nextToken();
            result.getSqlTokens().add(new RemoveToken(beginPosition, lexerEngine.getCurrentToken().getEndPosition()));
        }
        return result;
    }
    
    private DALStatement parseShowColumnsFields() {
        DALStatement result = new ShowColumnsStatement();
        lexerEngine.skipIfEqual(DefaultKeyword.FROM, DefaultKeyword.IN);
        tableReferencesClauseParser.parseSingleTableWithoutAlias(result);
        if (lexerEngine.skipIfEqual(DefaultKeyword.FROM, DefaultKeyword.IN)) {
            int beginPosition = lexerEngine.getCurrentToken().getEndPosition() - lexerEngine.getCurrentToken().getLiterals().length();
            result.getSqlTokens().add(new SchemaToken(beginPosition, lexerEngine.getCurrentToken().getLiterals(), result.getTables().getSingleTableName()));
        }
        return result;
    }
    
    private DALStatement parseShowCreateTable() {
        DALStatement result = new ShowCreateTableStatement();
        tableReferencesClauseParser.parseSingleTableWithoutAlias(result);
        return result;
    }
    
    private DALStatement parseShowIndex() {
        DALStatement result = new ShowIndexStatement();
        lexerEngine.skipIfEqual(DefaultKeyword.FROM, DefaultKeyword.IN);
        tableReferencesClauseParser.parseSingleTableWithoutAlias(result);
        if (lexerEngine.skipIfEqual(DefaultKeyword.FROM, DefaultKeyword.IN)) {
            int beginPosition = lexerEngine.getCurrentToken().getEndPosition() - lexerEngine.getCurrentToken().getLiterals().length();
            result.getSqlTokens().add(new SchemaToken(beginPosition, lexerEngine.getCurrentToken().getLiterals(), result.getTables().getSingleTableName()));
        }
        return result;
    }
}

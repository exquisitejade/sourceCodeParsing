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

package io.shardingsphere.core.parsing.parser.sql.tcl;

import io.shardingsphere.core.constant.SQLType;
import io.shardingsphere.core.parsing.lexer.token.DefaultKeyword;
import io.shardingsphere.core.parsing.lexer.token.Keyword;
import io.shardingsphere.core.parsing.lexer.token.TokenType;
import io.shardingsphere.core.parsing.parser.sql.AbstractSQLStatement;
import lombok.ToString;

import java.util.Arrays;
import java.util.Collection;

/**
 * Transaction Control Language statement.
 *
 * @author zhangliang
 */
@ToString(callSuper = true)
public final class TCLStatement extends AbstractSQLStatement {
    
    private static final Collection<Keyword> STATEMENT_PREFIX = Arrays.<Keyword>asList(
            DefaultKeyword.SET, DefaultKeyword.COMMIT, DefaultKeyword.ROLLBACK, DefaultKeyword.SAVEPOINT, DefaultKeyword.BEGIN);
    
    public TCLStatement() {
        super(SQLType.TCL);
    }
    
    /**
     * Is TCL statement.
     *
     * @param tokenType token type
     * @return is TCL or not
     */
    public static boolean isTCL(final TokenType tokenType) {
        return STATEMENT_PREFIX.contains(tokenType);
    }
}

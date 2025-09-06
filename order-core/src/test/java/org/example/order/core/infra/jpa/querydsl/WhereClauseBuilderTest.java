package org.example.order.core.infra.jpa.querydsl;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import org.example.order.core.infra.jpa.querydsl.dsl.WhereClauseBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhereClauseBuilderTest {

    @Test
    @DisplayName("optionalAnd(V!=null)일 때 and가 적용된다")
    void optionalAnd_valueNotNull() {
        BooleanExpression base = Expressions.asBoolean(true).isTrue();
        BooleanExpression cond = Expressions.asBoolean(true).isTrue();

        var where = new WhereClauseBuilder(base)
                .optionalAnd("value", () -> cond);

        assertThat(where).isNotNull();
    }

    @Test
    @DisplayName("optionalAnd(V==null)일 때 and가 적용되지 않는다")
    void optionalAnd_valueNull() {
        BooleanExpression base = Expressions.asBoolean(true).isTrue();
        BooleanExpression cond = Expressions.asBoolean(false).isTrue();

        var where = new WhereClauseBuilder(base)
                .optionalAnd(null, () -> cond);

        assertThat(where).isNotNull();
    }

    @Test
    @DisplayName("optionalAnd(boolean) true일 때 and 적용")
    void optionalAnd_booleanTrue() {
        BooleanExpression base = Expressions.asBoolean(true).isTrue();
        BooleanExpression cond = Expressions.asBoolean(true).isTrue();

        var where = new WhereClauseBuilder(base)
                .optionalAnd(true, () -> cond);

        assertThat(where).isNotNull();
    }

    @Test
    @DisplayName("optionalOr(V!=null)일 때 or가 적용된다")
    void optionalOr_valueNotNull() {
        BooleanExpression base = Expressions.asBoolean(true).isTrue();
        BooleanExpression cond = Expressions.asBoolean(false).isTrue();

        var where = new WhereClauseBuilder(base)
                .optionalOr("x", () -> cond);

        assertThat(where).isNotNull();
    }
}

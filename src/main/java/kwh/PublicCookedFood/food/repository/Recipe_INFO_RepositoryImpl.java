package kwh.PublicCookedFood.food.repository;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.PublicCookedFood.food.entity.QRecipe_INFO;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class Recipe_INFO_RepositoryImpl implements Recipe_INFO_RepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Autowired
    public Recipe_INFO_RepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Recipe_INFO> findRecipesByKeywordAndSearch(String keyword, String search, Pageable pageable) {
        QRecipe_INFO recipeInfo = QRecipe_INFO.recipe_INFO;

        BooleanBuilder builder = new BooleanBuilder();

        if (keyword != null && !keyword.isEmpty()) {
            builder.and(
                    recipeInfo.tyNM.containsIgnoreCase(keyword)
                            .or(recipeInfo.nationNM.containsIgnoreCase(keyword))
                            .or(recipeInfo.irdntCODE.containsIgnoreCase(keyword))
            );
        }
        if (search != null && !search.isEmpty()) {
            builder.and(recipeInfo.recipeNMKO.containsIgnoreCase(search));
        }
        List<Recipe_INFO> results = queryFactory
                .selectFrom(recipeInfo)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(recipeInfo)
                .where(builder)
                .fetch().size();

        return new PageImpl<>(results, pageable, total);
    }
}

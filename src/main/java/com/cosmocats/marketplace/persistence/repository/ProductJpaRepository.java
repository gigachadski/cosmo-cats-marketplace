package com.cosmocats.marketplace.persistence.repository;

import com.cosmocats.marketplace.persistence.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    /** Loads the category in the same query, so a page of products is one select, not N+1. */
    @Override
    @EntityGraph(attributePaths = "category")
    Page<ProductEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findWithCategoryById(Long id);

    /** Named query method: WHERE category_id = ? ORDER BY name, returning a projection. */
    List<ProductSummaryView> findByCategoryIdOrderByNameAsc(Long categoryId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ProductEntity p where p.id = :id")
    int deleteByIdReturningCount(@Param("id") Long id);
}

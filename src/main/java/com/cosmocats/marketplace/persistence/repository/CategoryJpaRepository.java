package com.cosmocats.marketplace.persistence.repository;

import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findAllByOrderByNameAsc();

    /** Deletes in one statement and returns how many rows were removed (0 or 1). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CategoryEntity c where c.id = :id")
    int deleteByIdReturningCount(@Param("id") Long id);
}

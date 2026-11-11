package com.cosmocats.marketplace.persistence.adapter;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.persistence.repository.CategoryJpaRepository;
import com.cosmocats.marketplace.repository.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository categories;

    public JpaCategoryRepositoryAdapter(CategoryJpaRepository categories) {
        this.categories = categories;
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categories.findById(id).map(EntityMapper::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return categories.findAllByOrderByNameAsc().stream().map(EntityMapper::toDomain).toList();
    }

    @Override
    public Category save(Category category) {
        if (category.id() != null) {
            throw new IllegalArgumentException("A new category must not have an id");
        }
        // flush now, so a unique-constraint violation surfaces here and not at commit time
        return EntityMapper.toDomain(categories.saveAndFlush(new CategoryEntity(category.name())));
    }

    @Override
    public Optional<Category> update(Category category) {
        return categories.findById(category.id()).map(entity -> {
            entity.setName(category.name());
            categories.flush();
            return EntityMapper.toDomain(entity);
        });
    }

    @Override
    public boolean deleteById(Long id) {
        return categories.deleteByIdReturningCount(id) > 0;
    }
}

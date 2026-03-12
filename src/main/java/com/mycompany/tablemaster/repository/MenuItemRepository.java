package com.mycompany.tablemaster.repository;

import com.mycompany.tablemaster.entity.MenuCategory;
import com.mycompany.tablemaster.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByCategory(MenuCategory category);

    List<MenuItem> findByIsAvailableTrue();
}

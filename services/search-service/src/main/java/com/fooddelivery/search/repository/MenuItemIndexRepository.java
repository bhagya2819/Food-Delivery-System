package com.fooddelivery.search.repository;

import com.fooddelivery.search.entity.MenuItemIndex;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MenuItemIndexRepository extends JpaRepository<MenuItemIndex, UUID> {
}

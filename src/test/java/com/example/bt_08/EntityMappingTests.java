package com.example.bt_08;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.bt_08.entity.Category;
import com.example.bt_08.entity.Product;
import com.example.bt_08.entity.User;
import com.example.bt_08.repository.CategoryRepository;
import com.example.bt_08.repository.ProductRepository;
import com.example.bt_08.service.CategoryService;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
class EntityMappingTests {

    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CategoryService categoryService;
    @Autowired
    EntityManager em;

    @Test
    void category_manyToMany_users() {
        Category phone = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Điện thoại")).findFirst().orElseThrow();

        assertThat(phone.getUsers()).extracting(User::getEmail)
                .containsExactlyInAnyOrder("an.nguyen@example.com", "binh.tran@example.com");
    }

    @Test
    void user_manyToMany_categories_inverseSide() {
        User an = em.createQuery("select u from User u where u.email = :e", User.class)
                .setParameter("e", "an.nguyen@example.com").getSingleResult();

        assertThat(an.getFullname()).isEqualTo("Nguyễn Văn An");
        assertThat(an.getCategories()).extracting(Category::getName)
                .containsExactlyInAnyOrder("Điện thoại", "Laptop", "Máy tính bảng");
    }

    @Test
    void product_hasOptionalUser_andQuotedDescColumn() {
        Category laptop = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Laptop")).findFirst().orElseThrow();

        Product saved = productRepository.saveAndFlush(Product.builder()
                .title("Không có user").price(new BigDecimal("1000.50")).quantity(1)
                .desc("cột [desc] là từ khoá T-SQL").category(laptop).build());
        em.clear();

        Product reloaded = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getUser()).isNull();
        assertThat(reloaded.getDesc()).isEqualTo("cột [desc] là từ khoá T-SQL");
        assertThat(reloaded.getPrice()).isEqualByComparingTo("1000.50");
    }

    @Test
    void deletingCategory_removesItsCategoryUserLinks() {
        User an = em.createQuery("select u from User u where u.email = :e", User.class)
                .setParameter("e", "an.nguyen@example.com").getSingleResult();
        Category cat = Category.builder().name("Tạm").images("x.jpg").build();
        cat.getUsers().add(an);
        Long id = categoryRepository.saveAndFlush(cat).getId();

        Number linksBefore = (Number) em.createNativeQuery("select count(*) from Category_User where categoryid = :id")
                .setParameter("id", id).getSingleResult();
        assertThat(linksBefore.intValue()).isEqualTo(1);

        categoryService.delete(id);
        em.flush();

        Number linksAfter = (Number) em.createNativeQuery("select count(*) from Category_User where categoryid = :id")
                .setParameter("id", id).getSingleResult();
        assertThat(linksAfter.intValue()).isZero();
        assertThat(categoryRepository.findById(id)).isEmpty();
    }

    @Test
    void seededProducts_haveUserAndCategory() {
        Product p = productRepository.findAllByOrderByPriceAsc().get(0);

        assertThat(p.getTitle()).isEqualTo("Sạc nhanh Anker 65W");
        assertThat(p.getCategory().getName()).isEqualTo("Phụ kiện");
        assertThat(p.getUser().getEmail()).isEqualTo("binh.tran@example.com");
    }
}

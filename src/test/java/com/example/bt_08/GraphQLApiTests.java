package com.example.bt_08;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureGraphQlTester
@Transactional
class GraphQLApiTests {

    @Autowired
    GraphQlTester graphQl;

    private static final String CREATE_PRODUCT = """
            mutation($input: ProductInput!) {
              createProduct(input: $input) { id title price quantity category { id name } }
            }""";

    @Test
    void products_areSortedByPriceAscending() {
        List<Double> prices = graphQl.document("{ products { price } }").execute()
                .path("products[*].price").entityList(Double.class).get();

        assertThat(prices).hasSize(12).isSorted();
    }

    @Test
    void products_resolveCategoryField() {
        graphQl.document("{ products { title category { name } } }").execute()
                .path("products[*].category.name").entityList(String.class)
                .satisfies(names -> assertThat(names).hasSize(12).doesNotContainNull());
    }

    @Test
    void productsByCategory_returnsOnlyThatCategory_sortedByPrice() {
        var response = graphQl.document("{ productsByCategory(categoryId: 2) { price category { name } } }").execute();

        response.path("productsByCategory[*].category.name").entityList(String.class)
                .satisfies(names -> assertThat(names).hasSize(3).containsOnly("Laptop"));
        response.path("productsByCategory[*].price").entityList(Double.class)
                .satisfies(prices -> assertThat(prices).isSorted());
    }

    @Test
    void productsByCategoryName_ignoresCase() {
        graphQl.document("{ productsByCategoryName(categoryName: \"lAPtop\") { title } }").execute()
                .path("productsByCategoryName").entityList(Object.class).hasSize(3);
    }

    @Test
    void productsByCategory_unknownCategory_isNotFound() {
        graphQl.document("{ productsByCategory(categoryId: 999) { id } }").execute()
                .errors().expect(e -> e.getErrorType() == ErrorType.NOT_FOUND).verify();
    }

    @Test
    void product_unknownId_returnsNull() {
        graphQl.document("{ product(id: 999) { id } category(id: 999) { id } }").execute()
                .path("product").valueIsNull()
                .path("category").valueIsNull();
    }

    @Test
    void product_crudLifecycle() {
        Map<String, Object> input = Map.of("title", "  Test SP  ", "price", 1500000, "quantity", 4,
                "desc", "mô tả", "categoryId", 1);

        String id = graphQl.document(CREATE_PRODUCT).variable("input", input).execute()
                .path("createProduct.title").entity(String.class).isEqualTo("Test SP")
                .path("createProduct.category.name").entity(String.class).isEqualTo("Điện thoại")
                .path("createProduct.id").entity(String.class).get();

        graphQl.document("{ products { id } }").execute()
                .path("products").entityList(Object.class).hasSize(13);

        Map<String, Object> updated = Map.of("title", "Test SP 2", "price", 999000, "quantity", 9, "categoryId", 2);
        graphQl.document("""
                mutation($id: ID!, $input: ProductInput!) {
                  updateProduct(id: $id, input: $input) { title price quantity category { name } }
                }""").variable("id", id).variable("input", updated).execute()
                .path("updateProduct.title").entity(String.class).isEqualTo("Test SP 2")
                .path("updateProduct.quantity").entity(Integer.class).isEqualTo(9)
                .path("updateProduct.category.name").entity(String.class).isEqualTo("Laptop");

        graphQl.document("mutation($id: ID!) { deleteProduct(id: $id) }").variable("id", id).execute()
                .path("deleteProduct").entity(Boolean.class).isEqualTo(true);

        graphQl.document("mutation($id: ID!) { deleteProduct(id: $id) }").variable("id", id).execute()
                .errors().expect(e -> e.getErrorType() == ErrorType.NOT_FOUND).verify();
    }

    @Test
    void createProduct_invalidInput_isBadRequestWithFieldErrors() {
        Map<String, Object> input = Map.of("title", "", "price", -5, "quantity", -1, "categoryId", 1);

        graphQl.document(CREATE_PRODUCT).variable("input", input).execute()
                .errors().satisfy(errors -> {
                    assertThat(errors).hasSize(1);
                    assertThat(errors.get(0).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    assertThat(errors.get(0).getExtensions().get("fieldErrors"))
                            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                            .containsKeys("title", "price", "quantity");
                });
    }

    @Test
    void createProduct_unknownCategory_isNotFound() {
        Map<String, Object> input = Map.of("title", "X", "price", 1000, "quantity", 1, "categoryId", 999);

        graphQl.document(CREATE_PRODUCT).variable("input", input).execute()
                .errors().expect(e -> e.getErrorType() == ErrorType.NOT_FOUND).verify();
    }

    @Test
    void category_duplicateNameIsRejected_caseInsensitive() {
        graphQl.document("mutation { createCategory(input: {name: \"laptop\"}) { id } }").execute()
                .errors().satisfy(errors -> {
                    assertThat(errors.get(0).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    assertThat(errors.get(0).getMessage()).contains("đã tồn tại");
                });
    }

    @Test
    void category_crudLifecycle_andCannotDeleteWhenHasProducts() {
        String id = graphQl.document("mutation { createCategory(input: {name: \"Test Cat\", images: \"images/test.jpg\"}) { id } }")
                .execute().path("createCategory.id").entity(String.class).get();

        graphQl.document("mutation($id: ID!) { updateCategory(id: $id, input: {name: \"Test Cat 2\"}) { name } }")
                .variable("id", id).execute()
                .path("updateCategory.name").entity(String.class).isEqualTo("Test Cat 2");

        graphQl.document("mutation { deleteCategory(id: 1) }").execute()
                .errors().satisfy(errors -> {
                    assertThat(errors.get(0).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    assertThat(errors.get(0).getMessage()).contains("Không thể xoá");
                });

        graphQl.document("mutation($id: ID!) { deleteCategory(id: $id) }").variable("id", id).execute()
                .path("deleteCategory").entity(Boolean.class).isEqualTo(true);
    }
}

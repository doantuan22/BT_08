"use strict";

const GRAPHQL_URL = "/graphql";
const MAX_INT = 2147483647;
const MAX_PRICE = Number.MAX_SAFE_INTEGER;

class GraphQLError extends Error {
    constructor(message, fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors || {};
    }
}

async function gql(query, variables = {}) {
    let response;
    try {
        response = await fetch(GRAPHQL_URL, {
            method: "POST",
            headers: {"Content-Type": "application/json", "Accept": "application/json"},
            body: JSON.stringify({query, variables})
        });
    } catch (e) {
        throw new GraphQLError("Không kết nối được tới server. Hãy kiểm tra lại ứng dụng đang chạy.");
    }

    let payload;
    try {
        payload = await response.json();
    } catch (e) {
        throw new GraphQLError("Server trả về dữ liệu không hợp lệ (HTTP " + response.status + ")");
    }

    if (payload.errors && payload.errors.length) {
        const first = payload.errors[0];
        const message = payload.errors.map(err => err.message).join("; ");
        const fieldErrors = (first.extensions && first.extensions.fieldErrors) || {};
        throw new GraphQLError(message, fieldErrors);
    }
    return payload.data;
}

const Q = {
    categories: `query { categories { id name images } }`,

    products: `query { products { id title price quantity desc category { id name } } }`,

    productsByCategory: `query($categoryId: ID!) {
        productsByCategory(categoryId: $categoryId) { id title price quantity desc category { id name } }
    }`,

    createProduct: `mutation($input: ProductInput!) { createProduct(input: $input) { id } }`,
    updateProduct: `mutation($id: ID!, $input: ProductInput!) { updateProduct(id: $id, input: $input) { id } }`,
    deleteProduct: `mutation($id: ID!) { deleteProduct(id: $id) }`,

    createCategory: `mutation($input: CategoryInput!) { createCategory(input: $input) { id } }`,
    updateCategory: `mutation($id: ID!, $input: CategoryInput!) { updateCategory(id: $id, input: $input) { id } }`,
    deleteCategory: `mutation($id: ID!) { deleteCategory(id: $id) }`
};

const state = {
    categories: [],
    products: []
};

const $ = (id) => document.getElementById(id);

const priceFormat = new Intl.NumberFormat("vi-VN");

function el(tag, props = {}, ...children) {
    const node = document.createElement(tag);
    Object.entries(props).forEach(([k, v]) => {
        if (k === "className") node.className = v;
        else if (k === "dataset") Object.assign(node.dataset, v);
        else node[k] = v;
    });
    children.forEach(c => node.append(c));
    return node;
}

let toastTimer;

function showToast(message, isError = false) {
    const toast = $("toast");
    toast.textContent = message;
    toast.classList.toggle("error", isError);
    toast.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => (toast.hidden = true), isError ? 6000 : 3000);
}

function showError(err) {
    console.error(err);
    showToast(err.message || "Có lỗi xảy ra", true);
}

async function run(action) {
    try {
        await action();
        return true;
    } catch (err) {
        showError(err);
        return false;
    }
}

document.querySelectorAll(".tab").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".tab").forEach(b => b.classList.toggle("active", b === btn));
        document.querySelectorAll(".tab-panel").forEach(p => (p.hidden = p.id !== "tab-" + btn.dataset.tab));
    });
});

async function loadCategories() {
    const data = await gql(Q.categories);
    state.categories = data.categories;
    renderCategoryTable();
    renderCategoryOptions();
}

function renderCategoryTable() {
    const tbody = document.querySelector("#categoryTable tbody");
    tbody.replaceChildren(...state.categories.map(c =>
        el("tr", {},
            el("td", {textContent: c.id}),
            el("td", {textContent: c.name}),
            el("td", {textContent: c.images || ""}),
            el("td", {className: "actions"},
                el("button", {className: "btn small", textContent: "Sửa", dataset: {action: "edit", id: c.id}}),
                " ",
                el("button", {className: "btn small danger", textContent: "Xoá", dataset: {action: "delete", id: c.id}})
            )
        )
    ));
    $("categoryEmpty").hidden = state.categories.length > 0;
}

function renderCategoryOptions() {
    const filter = $("categoryFilter");
    const previous = filter.value;
    filter.replaceChildren(
        el("option", {value: "", textContent: "-- Tất cả --"}),
        ...state.categories.map(c => el("option", {value: c.id, textContent: c.name}))
    );
    filter.value = state.categories.some(c => c.id === previous) ? previous : "";

    $("productCategory").replaceChildren(
        el("option", {value: "", textContent: "-- Chọn danh mục --"}),
        ...state.categories.map(c => el("option", {value: c.id, textContent: c.name}))
    );
}

function openCategoryModal(category) {
    clearFieldErrors($("categoryForm"));
    $("categoryModalTitle").textContent = category ? "Sửa danh mục #" + category.id : "Thêm danh mục";
    $("categoryId").value = category ? category.id : "";
    $("categoryName").value = category ? category.name : "";
    $("categoryImages").value = category ? (category.images || "") : "";
    openModal("categoryModal");
    $("categoryName").focus();
}

$("btnAddCategory").addEventListener("click", () => openCategoryModal(null));

$("categoryTable").addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const category = state.categories.find(c => c.id === btn.dataset.id);
    if (btn.dataset.action === "edit") {
        openCategoryModal(category);
    } else if (confirm(`Xoá danh mục "${category.name}"?`)) {
        const ok = await run(async () => {
            await gql(Q.deleteCategory, {id: category.id});
            await refreshAll();
        });
        if (ok) showToast("Đã xoá danh mục");
    }
});

$("categoryForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.currentTarget;
    clearFieldErrors(form);

    const id = $("categoryId").value;
    const input = {
        name: $("categoryName").value.trim(),
        images: $("categoryImages").value.trim() || null
    };
    if (!input.name) {
        return setFieldError(form, "name", "Tên danh mục không được để trống");
    }

    await submitForm(form, async () => {
        if (id) await gql(Q.updateCategory, {id, input});
        else await gql(Q.createCategory, {input});
        closeModal("categoryModal");
        await refreshAll();
        showToast(id ? "Đã cập nhật danh mục" : "Đã thêm danh mục");
    });
});

async function loadProducts() {
    const categoryId = $("categoryFilter").value;
    const data = categoryId
        ? await gql(Q.productsByCategory, {categoryId})
        : await gql(Q.products);
    state.products = categoryId ? data.productsByCategory : data.products;
    renderProductTable();
}

function renderProductTable() {
    const tbody = document.querySelector("#productTable tbody");
    tbody.replaceChildren(...state.products.map(p =>
        el("tr", {},
            el("td", {textContent: p.id}),
            el("td", {textContent: p.title}),
            el("td", {className: "num", textContent: priceFormat.format(p.price)}),
            el("td", {className: "num", textContent: p.quantity}),
            el("td", {textContent: p.category.name}),
            el("td", {className: "actions"},
                el("button", {className: "btn small", textContent: "Sửa", dataset: {action: "edit", id: p.id}}),
                " ",
                el("button", {className: "btn small danger", textContent: "Xoá", dataset: {action: "delete", id: p.id}})
            )
        )
    ));
    $("productEmpty").hidden = state.products.length > 0;
}

function openProductModal(product) {
    clearFieldErrors($("productForm"));
    $("productModalTitle").textContent = product ? "Sửa sản phẩm #" + product.id : "Thêm sản phẩm";
    $("productId").value = product ? product.id : "";
    $("productName").value = product ? product.title : "";
    $("productPrice").value = product ? product.price : "";
    $("productQuantity").value = product ? product.quantity : 0;
    $("productDesc").value = product ? (product.desc || "") : "";
    $("productCategory").value = product ? product.category.id : $("categoryFilter").value;
    openModal("productModal");
    $("productName").focus();
}

$("btnAddProduct").addEventListener("click", () => openProductModal(null));

$("categoryFilter").addEventListener("change", () => run(loadProducts));

$("productTable").addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-action]");
    if (!btn) return;
    const product = state.products.find(p => p.id === btn.dataset.id);
    if (btn.dataset.action === "edit") {
        openProductModal(product);
    } else if (confirm(`Xoá sản phẩm "${product.title}"?`)) {
        const ok = await run(async () => {
            await gql(Q.deleteProduct, {id: product.id});
            await loadProducts();
        });
        if (ok) showToast("Đã xoá sản phẩm");
    }
});

$("productForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.currentTarget;
    clearFieldErrors(form);

    const id = $("productId").value;
    const priceText = $("productPrice").value;
    const quantityText = $("productQuantity").value;
    const input = {
        title: $("productName").value.trim(),
        price: priceText === "" ? null : Number(priceText),
        quantity: quantityText === "" ? null : parseInt(quantityText, 10),
        desc: $("productDesc").value.trim() || null,
        categoryId: $("productCategory").value
    };

    let invalid = false;
    if (!input.title) invalid = !setFieldError(form, "title", "Tiêu đề không được để trống") || invalid;
    if (input.price === null || !(input.price > 0)) invalid = !setFieldError(form, "price", "Giá phải lớn hơn 0") || invalid;
    if (input.price !== null && input.price > MAX_PRICE) invalid = !setFieldError(form, "price", "Giá quá lớn") || invalid;
    if (input.quantity === null || input.quantity < 0) invalid = !setFieldError(form, "quantity", "Số lượng không được âm") || invalid;
    if (input.quantity !== null && input.quantity > MAX_INT) invalid = !setFieldError(form, "quantity", "Số lượng quá lớn (tối đa " + MAX_INT + ")") || invalid;
    if (!input.categoryId) invalid = !setFieldError(form, "categoryId", "Vui lòng chọn danh mục") || invalid;
    if (invalid) return;

    await submitForm(form, async () => {
        if (id) await gql(Q.updateProduct, {id, input});
        else await gql(Q.createProduct, {input});
        closeModal("productModal");
        await loadProducts();
        showToast(id ? "Đã cập nhật sản phẩm" : "Đã thêm sản phẩm");
    });
});

async function submitForm(form, action) {
    const submitBtn = form.querySelector("button[type=submit]");
    submitBtn.disabled = true;
    try {
        await action();
    } catch (err) {
        Object.entries(err.fieldErrors || {}).forEach(([field, msg]) => setFieldError(form, field, msg));
        showError(err);
    } finally {
        submitBtn.disabled = false;
    }
}

function setFieldError(form, field, message) {
    const holder = form.querySelector(`[data-error-for="${field}"]`);
    if (holder) {
        holder.textContent = message;
        holder.closest("label").classList.add("has-error");
    }
    return false;
}

function clearFieldErrors(form) {
    form.querySelectorAll(".field-error").forEach(s => (s.textContent = ""));
    form.querySelectorAll(".has-error").forEach(l => l.classList.remove("has-error"));
}

function openModal(id) {
    $(id).hidden = false;
}

function closeModal(id) {
    $(id).hidden = true;
}

document.querySelectorAll(".modal").forEach(modal => {
    modal.addEventListener("click", (e) => {
        if (e.target === modal || e.target.closest("[data-close]")) closeModal(modal.id);
    });
});
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") document.querySelectorAll(".modal:not([hidden])").forEach(m => closeModal(m.id));
});

async function refreshAll() {
    await loadCategories();
    await loadProducts();
}

run(refreshAll);

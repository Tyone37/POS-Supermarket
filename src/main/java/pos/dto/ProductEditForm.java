package pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ProductEditForm {
    @NotBlank(message = "Vui lòng nhập mã vạch.")
    @Size(max = 50, message = "Mã vạch tối đa 50 ký tự.")
    private String barcode;
    @NotBlank(message = "Vui lòng nhập tên sản phẩm.")
    @Size(max = 200, message = "Tên sản phẩm tối đa 200 ký tự.")
    private String name;
    @NotNull(message = "Vui lòng chọn nhóm hàng.")
    @Positive(message = "Nhóm hàng không hợp lệ.")
    private Long categoryId;
    @NotNull(message = "Vui lòng chọn đơn vị.")
    @Positive(message = "Đơn vị không hợp lệ.")
    private Long unitId;
    @NotBlank(message = "Vui lòng nhập đơn giá.")
    private String price;
    @NotBlank(message = "Vui lòng nhập tồn kho.")
    private String stock;
    @Size(max = 500, message = "URL ảnh tối đa 500 ký tự.")
    private String imageUrl;
    @NotBlank(message = "Vui lòng tải lại sản phẩm trước khi lưu.")
    private String expectedUpdatedAt;

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public String getStock() { return stock; }
    public void setStock(String stock) { this.stock = stock; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getExpectedUpdatedAt() { return expectedUpdatedAt; }
    public void setExpectedUpdatedAt(String expectedUpdatedAt) { this.expectedUpdatedAt = expectedUpdatedAt; }

    public Forms.Product product() {
        return new Forms.Product(null, barcode, name, categoryId, unitId, price, stock, imageUrl);
    }
}

package kernel.ddd;

import kernel.common.UuidV7;

import java.util.Objects;
import java.util.UUID;

/**
 * Lớp cơ sở cho entity: đối tượng được nhận diện bằng định danh thay vì thuộc tính.
 *
 * <p>Hai entity bằng nhau khi cùng lớp và cùng định danh.
 *
 * @param <ID> kiểu định danh của entity
 */
public abstract class Entity<ID> {
    private final ID id;

    /**
     * Khởi tạo entity với định danh cho trước.
     *
     * @param id định danh của entity
     * @throws NullPointerException nếu {@code id} là {@code null}
     */
    protected Entity(ID id) {
        this.id = Objects.requireNonNull(id, "ID của entity không được null");
    }

    /**
     * Trả về định danh của entity.
     *
     * @return định danh, không bao giờ {@code null}
     */
    public ID getId() {
        return id;
    }

    /**
     * So sánh theo lớp và định danh.
     *
     * @param o đối tượng cần so sánh
     * @return {@code true} nếu {@code o} cùng lớp và cùng định danh với entity này
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> other = (Entity<?>) o;
        return id.equals(other.id);
    }

    /**
     * Tính mã băm dựa trên định danh.
     *
     * @return mã băm của định danh
     */
    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    public UUID nextId(){
        return UuidV7.generate();
    }

}

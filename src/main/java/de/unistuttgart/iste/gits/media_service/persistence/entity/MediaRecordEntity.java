package de.unistuttgart.iste.gits.media_service.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.*;

@Entity(name = "MediaRecord")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(columnDefinition = "UUID[]")
    @Type(value = CustomUUIDArrayType.class )
    private UUID[] courseIds;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private UUID creatorId;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private MediaType type;

    @ElementCollection
    private List<UUID> contentIds;

    @Column(length = 500)
    private String uploadUrl;

    @Column(length = 500)
    private String downloadUrl;

    @OneToMany(mappedBy = "primaryKey.mediaRecordId", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    private List<MediaRecordProgressDataEntity> progressData = new ArrayList<>();

    public enum MediaType {
        AUDIO,
        VIDEO,
        IMAGE,
        PRESENTATION,
        DOCUMENT,
        URL
    }

    static class CustomUUIDArrayType implements UserType<UUID[]> {

        @Override
        public int getSqlType() {
            return Types.ARRAY;
        }

        @Override
        public Class<UUID[]> returnedClass() {
            return UUID[].class;
        }

        @Override
        public boolean equals(final UUID[] x, final UUID[] y) {
            if (x == null) {
                return y == null;
            }
            return Arrays.equals(x, y);
        }

        @Override
        public int hashCode(final UUID[] x) {
            return Arrays.hashCode(x);
        }

        @Override
        public UUID[] nullSafeGet(final ResultSet rs, final int position, final SharedSessionContractImplementor session, final Object owner) throws SQLException {
            final Array array = rs.getArray(position);
            return array != null ? (UUID[]) array.getArray(): null;
        }

        @Override
        public void nullSafeSet(final PreparedStatement st, final UUID[] value, final int index, final SharedSessionContractImplementor session) throws SQLException {
            if (st != null) {
                if (value != null) {
                    final Array array = session.getJdbcConnectionAccess().obtainConnection().createArrayOf("UUID", value);
                    st.setArray(index, array);
                } else {
                    st.setNull(index, Types.ARRAY);
                }
            }

        }

        @Override
        public UUID[] deepCopy(final UUID[] value) {
            return value;
        }

        @Override
        public boolean isMutable() {
            return false;
        }

        @Override
        public Serializable disassemble(final UUID[] value) {
            return this.deepCopy(value);
        }

        @Override
        public UUID[] assemble(final Serializable cached, final Object owner) {
            return this.deepCopy((UUID[]) cached);
        }

        @Override
        public UUID[] replace(final UUID[] detached, final UUID[] managed, final Object owner) {
            return detached;
        }
    }
}

# Data 모듈 가이드

Data 모듈은 엔티티 정의를 위해 사용됩니다.

## 엔티티 정의

- `entity` 패키지에 도메인별로 패키지를 생성 후 엔티티를 정의합니다.
- 엔티티는 `BaseEntity`를 상속받아 정의합니다.
- 엔티티에는 메서드를 정의하지 않습니다.

```java
@Entity
@Getter
@ToString(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "user") // 테이블 명 지정
@SQLDelete(sql = "UPDATE user SET deleted=true where id=?") // 논리 삭제
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;
}
```

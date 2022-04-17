package blog.surapong.example.testtx.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@Accessors(chain = true)
@EqualsAndHashCode
@ToString
@Entity
@Table(name = "account")
public class Account {

    @Id
    @Column(name = "accountId")
    private String accountId;

    /* In production please use another type
     I use this type for test transactional only */
    private Integer balance;

    private Integer endBy;

}

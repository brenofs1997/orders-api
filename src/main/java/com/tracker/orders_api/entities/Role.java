package com.tracker.orders_api.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleID;

    private String name;

    public Long getRoleID() {
        return roleID;
    }

    public void setRoleID(Long roleID) {
        this.roleID = roleID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public  enum Values{
        ADMIN(1L),
        BASIC(2L);


        long roleId;

        Values(long roleId){
            this.roleId = roleId;
        }

        public long getRoleId() {
            return roleId;
        }
    }


}

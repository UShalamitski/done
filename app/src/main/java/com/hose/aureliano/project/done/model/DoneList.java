package com.hose.aureliano.project.done.model;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Entity represents a list of items to do.
 * <p>
 * Date: 11.02.2018.
 *
 * @author evere
 */
@Entity(tableName = "lists")
public class DoneList {

    @NonNull
    @PrimaryKey
    private String id;
    private String name;

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoneList doneList = (DoneList) o;
        return new EqualsBuilder()
                .append(id, doneList.id)
                .append(name, doneList.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .toHashCode();
    }
}

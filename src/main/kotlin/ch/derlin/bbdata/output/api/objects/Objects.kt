package ch.derlin.bbdata.output.api.objects

import ch.derlin.bbdata.output.api.object_groups.ObjectGroup
import ch.derlin.bbdata.output.api.types.Unit
import ch.derlin.bbdata.output.api.user_groups.UserGroup
import org.hibernate.annotations.Generated
import org.hibernate.annotations.GenerationTime
import org.joda.time.DateTime
import javax.persistence.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size


/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */


@Entity
@Table(name = "objects")
data class Objects(

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Basic(optional = false)
        @Column(name = "id")
        val id: Long? = null,

        @Basic(optional = false)
        @NotNull
        @Size(min = 1, max = 60)
        @Column(name = "name")
        var name: String? = null,

        @Size(max = 255)
        @Column(name = "description")
        var description: String? = null,

        @JoinColumn(name = "unit_symbol", referencedColumnName = "symbol")
        @ManyToOne(optional = false, fetch = FetchType.EAGER)
        val unit: Unit,

        @Column(name = "disabled")
        var disabled: Boolean = false,

        @Column(name = "creationdate", insertable = false, updatable = false)
        @Generated(GenerationTime.INSERT)
        val creationdate: DateTime? = null,

        @JoinColumn(name = "ugrp_id", referencedColumnName = "id")
        @ManyToOne(optional = false, fetch = FetchType.LAZY)
        val owner: UserGroup,

        @OneToMany(cascade = arrayOf(CascadeType.ALL), fetch = FetchType.EAGER, orphanRemoval = true)
        @JoinColumn(name = "object_id", updatable = false)
        var tags: MutableSet<Tag> = mutableSetOf(),

        @ManyToMany(mappedBy = "objects", fetch = FetchType.LAZY)
        private val objectGroups: List<ObjectGroup>? = null,

        @OneToMany(fetch = FetchType.LAZY, cascade = arrayOf())
        @JoinColumn(name = "object_id", insertable = false, updatable = false)
        protected val userPerms: List<ObjectsPerms>? = listOf()

) {
    fun addTag(tag: String): Boolean = this.tags.add(Tag(tag, this.id!!))
    fun removeTag(tag: String): Boolean = this.tags.removeIf { it.name == tag }
}
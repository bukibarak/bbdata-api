package ch.derlin.bbdata.output.api.object_groups

/**
 * date: 20.11.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */

import ch.derlin.bbdata.output.Beans
import ch.derlin.bbdata.output.api.objects.ObjectRepository
import ch.derlin.bbdata.output.api.objects.Objects
import ch.derlin.bbdata.output.api.user_groups.UserGroupRepository
import ch.derlin.bbdata.output.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotNull

@RestController
@RequestMapping("/objectGroups")
@Tag(name = "ObjectGroups", description = "Manage object groups")
class ObjectGroupsController(private val objectGroupsRepository: ObjectGroupsRepository,
                             private val userGroupRepository: UserGroupRepository) {

    class NewObjectGroup : Beans.NameDescription() {
        @NotNull
        val owner: Int? = null
    }

    @Protected
    @GetMapping("")
    //@ApiResponse(
    //        responseCode = "200",
    //        description = "default resposne. Note: depending on the withObject parameter, the objects array can be missing.",
    //        content = arrayOf(Content(
    //                schema = ArraySchema(schema = Schema(type= "ObjectGroup", implementation = ObjectGroup::class)))
    //        ))
    fun getAll(@UserId userId: Int,
               @RequestParam("writable", required = false, defaultValue = "false") writable: Boolean)
            : List<ObjectGroup.ObjectGroupSimple> {

        val ogrpList =
                if (writable) objectGroupsRepository.findAllWritable(userId)
                else objectGroupsRepository.findAll(userId)

        return ogrpList.map { ObjectGroup.ObjectGroupSimple.fromObjectGroup(it) }
    }

    @Protected
    @PutMapping("")
    fun createOne(@UserId userId: Int,
                  @Valid @RequestBody newOgrp: NewObjectGroup): ObjectGroup.ObjectGroupSimple {

        val owner = userGroupRepository.findMine(userId, newOgrp.owner!!).orElseThrow {
            ItemNotFoundException("userGroup (${newOgrp.owner})")
        }

        val obj = objectGroupsRepository.saveAndFlush(
                ObjectGroup(
                        name = newOgrp.name,
                        description = newOgrp.description,
                        owner = owner
                )
        )

        return ObjectGroup.ObjectGroupSimple.fromObjectGroup(obj)
    }

    @Protected
    @GetMapping("/{id}")
    fun getOneById(@UserId userId: Int,
                   @PathVariable(value = "id") id: Long,
                   @RequestParam("writable", required = false, defaultValue = "false") writable: Boolean): ObjectGroup {
        val obj =
                if (writable) objectGroupsRepository.findOneWritable(userId, id)
                else objectGroupsRepository.findOne(userId, id)

        return obj.orElseThrow { ItemNotFoundException("object group ($id)") }
    }

    @Protected
    @DeleteMapping("/{id}")
    fun deleteOneById(@UserId userId: Int, @PathVariable(value = "id") id: Long): ResponseEntity<Unit> {
        var ret = HttpStatus.NOT_MODIFIED
        objectGroupsRepository.findOne(userId, id).ifPresent {
            objectGroupsRepository.delete(it)
            ret = HttpStatus.OK
        }
        return ResponseEntity(ret)
    }
}

@RestController
@RequestMapping("/objectGroups")
@Tag(name = "ObjectGroups", description = "Manage object groups")
class ObjectGroupsObjectController(
        private val objectGroupsRepository: ObjectGroupsRepository,
        private val objectRepository: ObjectRepository) {

    @Protected
    @GetMapping("/{id}/objects")
    fun getObjectsOfGroup(@UserId userId: Int, @PathVariable(value = "id") id: Long): MutableList<Objects> {

        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ItemNotFoundException("object group ($id)")
        }
        return ogrp.objects
    }

    @Protected
    @PutMapping("/{id}/objects")
    fun addObjectToGroup(@UserId userId: Int,
                         @PathVariable(value = "id") id: Long,
                         @RequestParam("objectId", required = true) objectId: Long): ResponseEntity<Unit> {

        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ItemNotFoundException("object group ($id)")
        }

        if (ogrp.objects.find { it.id == objectId } != null) {
            return ResponseEntity(HttpStatus.NOT_MODIFIED)
        }

        val obj = objectRepository.findById(objectId, userId, writable = true).orElseThrow {
            ItemNotFoundException("object ($objectId)")
        }

        ogrp.objects.add(obj)
        objectGroupsRepository.save(ogrp)
        return ResponseEntity(HttpStatus.OK)
    }

    @Protected
    @DeleteMapping("/{id}/objects")
    fun removeObjectFromGroup(@UserId userId: Int,
                              @PathVariable(value = "id") id: Long,
                              @RequestParam("objectId", required = true) objectId: Long): ResponseEntity<Unit> {

        val ogrp = objectGroupsRepository.findOneWritable(userId, id).orElseThrow {
            ItemNotFoundException("object group ($id)")
        }

        var ret = HttpStatus.NOT_MODIFIED
        ogrp.objects.find { it.id == objectId }?.let {
            ogrp.objects.remove(it)
            objectGroupsRepository.save(ogrp)
            ret = HttpStatus.OK

        }
        return ResponseEntity(ret)
    }
}
package ch.derlin.bbdata.output.api.user_groups

import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.users.UserRepository
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@Tag(name = "UserGroups", description = "Manage user groups")
class UserGroupMappingController(
        private val userGroupMappingRepository: UserGroupMappingRepository,
        private val userRepository: UserRepository) {


    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Get the details of a user part of the user group (such as his role, admin or not).")
    @GetMapping("/userGroups/{userGroupId}/users/{userId}")
    fun getUserInGroup(@UserId userId: Int,
                       @PathVariable(value = "userGroupId") userGroupId: Int,
                       @PathVariable(name = "userId") userIdToGet: Int): UsergroupMapping {
        ensureUserCanAccessGroup(userId, userGroupId, requireAdminRight = false)
        return userGroupMappingRepository.findById(UserUgrpMappingId(userIdToGet, userGroupId)).orElseThrow {
            ItemNotFoundException("user ($userIdToGet) for userGroup ($userGroupId)")
        }
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Add a user to a user group you are admin of, or change his role. " +
            "If the user is already present with the given role, it simply returns NOT MODIFIED. " +
            "If the user has another role (admin/regular), it will be updated.<br>" +
            "_NOTE_: user with id=1 cannot be touched.")
    @SimpleModificationStatusResponse
    @PutMapping("/userGroups/{userGroupId}/users/{userId}")
    fun addUserToGroup(@UserId userId: Int,
                       @PathVariable(value = "userGroupId") id: Int,
                       @PathVariable(name = "userId") newUserId: Int,
                       @RequestParam(name = "admin", required = false, defaultValue = "false") admin: Boolean
    ): ResponseEntity<String> {
        // ensure the user has the right to update members of this group
        ensureUserCanAccessGroup(userId, id, requireAdminRight = true)
        // ensure the user we want to update exists
        userRepository.findById(newUserId).orElseThrow { ItemNotFoundException("user ($newUserId)") }
        // do the deed, either updating or creating a new mapping
        val optional = userGroupMappingRepository.findById(UserUgrpMappingId(newUserId, id))
        if (optional.isPresent) {
            // mapping exists, this is an update
            val mapping = optional.get()
            if (mapping.isAdmin == admin) {
                return CommonResponses.notModifed()
            } else {
                mapping.isAdmin = admin
                userGroupMappingRepository.save(mapping)
                return CommonResponses.ok()
            }
        }
        // mapping doesn't exist, create a new one
        userGroupMappingRepository.save(UsergroupMapping(userId = newUserId, groupId = id, isAdmin = admin))
        return CommonResponses.ok()
    }

    @Protected(SecurityConstants.SCOPE_WRITE)
    @Operation(description = "Remove a user from a user group you are admin of. " +
            "If the user is not part of it, it simply returns NOT MODIFIED.<br>" +
            "_NOTE_: user with id=1 cannot be touched.")
    @SimpleModificationStatusResponse
    @DeleteMapping("/userGroups/{userGroupId}/users/{userId}")
    fun removeUserFromGroup(@UserId userId: Int,
                            @PathVariable(value = "userGroupId") id: Int,
                            @PathVariable(name = "userId") userIdToDelete: Int
    ): ResponseEntity<String> {
        // ensure the user has the right to update members of this group
        ensureUserCanAccessGroup(userId, id, requireAdminRight = true)
        // only remove if exists
        val optional = userGroupMappingRepository.findById(UserUgrpMappingId(userIdToDelete, id))
        if (optional.isPresent) {
            userGroupMappingRepository.delete(optional.get())
            return CommonResponses.ok()
        }
        return CommonResponses.notModifed()
    }

    /*
    @Protected(SecurityConstants.SCOPE_WRITE)
    @PutMapping("/userGroups/{userGroupId}/users/new")
    fun createUserInGroup(@UserId userId: Int,
                   @Valid @RequestBody newUser: UserController.NewUser,
                   @PathVariable(value = "userGroupId") id: Int,
                   @RequestParam(name = "admin", required = false, defaultValue = "false") admin: Boolean): User {
        // ensure the user has the right to add a member to the group
        val mapping = canUserModifyGroup(userId, id)
        // create both user and mapping (group permission)
        if (!mapping.isAdmin) throw ForbiddenException("You must be admin to add users.")
        val user = userRepository.saveAndFlush(newUser.toUser()) // use flush to get the generated ID
        userGroupMappingRepository.save(UsergroupMapping(userId = user.id!!, groupId = id, isAdmin = admin))
        return user
    }
    */

    fun ensureUserCanAccessGroup(userId: Int, groupId: Int, requireAdminRight: Boolean = false) {
        // ensure the user has the right to add a member to the group
        if (userGroupMappingRepository.isSuperAdmin(userId)) return
        val mapping = userGroupMappingRepository.findById(UserUgrpMappingId(userId, groupId)).orElseThrow {
            ItemNotFoundException("usergroup (${groupId})")
        }
        if (requireAdminRight && !mapping.isAdmin)
            throw ForbiddenException("You must be admin to execute this action.")
    }
}
package ch.derlin.bbdata.output.api.users

import ch.derlin.bbdata.common.exceptions.ForbiddenException
import ch.derlin.bbdata.common.exceptions.ItemNotFoundException
import ch.derlin.bbdata.output.api.CommonResponses
import ch.derlin.bbdata.output.api.SimpleModificationStatusResponse
import ch.derlin.bbdata.output.api.user_groups.UserGroupMappingController
import ch.derlin.bbdata.output.api.user_groups.UserGroupMappingRepository
import ch.derlin.bbdata.output.security.Protected
import ch.derlin.bbdata.output.security.SecurityConstants
import ch.derlin.bbdata.output.security.UserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

/**
 * date: 28.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@RestController
@Tag(name = "Users", description = "Get or create users")
class UserController(private val userRepository: UserRepository,
                     private val userGroupMappingRepository: UserGroupMappingRepository,
                     private val userGroupMappingController: UserGroupMappingController) {

    // TODO: where to put NewX classes ? controller or model ?
    class NewUser {
        @NotNull
        @Size(min = User.NAME_MIN, max = User.NAME_MAX)
        val name: String? = null

        @NotNull
        @Size(min = User.PASSWORD_MIN, max = User.PASSWORD_MAX)
        val password: String? = null

        @NotEmpty
        @Size(max = User.EMAIL_MAX)
        @Pattern(regexp = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message = "Invalid email")
        val email: String? = null

        fun toUser(): User = User(name = name!!, password = User.hashPassword(password!!), email = email!!)

    }

    @Protected // TODO: permissions ?
    @Operation(description = "Get the list of all users registered.")
    @GetMapping("/users")
    fun getUsers(): List<User> = userRepository.findAll() // TODO: try to comment this endpoint... HATEOAS ! /search

    @Protected
    @Operation(description = "Get details about a user.")
    @GetMapping("/users/{userId}")
    fun getUser(@PathVariable("userId") userId: Int): User = userRepository.findById(userId).orElseThrow {
        ItemNotFoundException("user ($userId)")
    }

    @Operation(description = "Create a new user. You can use the parameters `userGroupId` and `admin` to " +
            "also add the created user to a user group you own (see PUT /userGroups/ugID/users/uID for more details).")
    @Protected(SecurityConstants.SCOPE_WRITE)
    @Transactional(rollbackFor = [ItemNotFoundException::class])
    @PutMapping("/users")
    fun createUser(
            @UserId userId: Int,
            @RequestParam(name = "userGroupId", required = false) userGroupId: Int?,
            @RequestParam(name = "admin", required = false) admin: Boolean,
            @Valid @RequestBody newUser: NewUser): User {
        // Note: the user won't be part of any userGroup, so he has to be added through another call
        // to the ObjectGroupsPermissionController
        val user = userRepository.saveAndFlush(newUser.toUser())
        userGroupId?.let {
            // the controller will take care of the permissions
            userGroupMappingController.addUserToGroup(userId, it, newUserId = user.id!!, admin = admin)
        }
        return user
    }

    @SimpleModificationStatusResponse
    @Operation(description = "Delete a user. This functionality is only available to SUPERADMIN users.")
    @Protected(SecurityConstants.SCOPE_WRITE)
    @DeleteMapping("/users/{userId}")
    fun deleteUser(
            @UserId userId: Int,
            @PathVariable("userId") userToDelete: Int): ResponseEntity<String> {
        if (!userGroupMappingRepository.isSuperAdmin(userId))
            throw ForbiddenException("Deleting users is only available to SUPERADMIN users")
        val optionalUser = userRepository.findById(userToDelete)
        return if (optionalUser.isPresent) {
            userRepository.delete(optionalUser.get())
            CommonResponses.ok()
        } else {
            CommonResponses.notModifed()
        }
    }
}
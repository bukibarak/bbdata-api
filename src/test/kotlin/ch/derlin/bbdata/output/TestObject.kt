package ch.derlin.bbdata.output

import ch.derlin.bbdata.Profiles
import ch.derlin.bbdata.postQueryString
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.beans.factory.annotation.Autowired


/**
 * date: 18.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObject {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val id = 1
    }

    @Test
    @Throws(Exception::class)
    fun `1-1 object disable`() {
        val response1 = restTemplate.postQueryString("/objects/$id/disable")
        assertEquals(HttpStatus.OK, response1.statusCode)

        val response2 = restTemplate.postQueryString("/objects/$id/disable")
        assertEquals(HttpStatus.NOT_MODIFIED, response2.statusCode)
    }

    @Test
    @Throws(Exception::class)
    fun `1-2 object enable`() {
        val response1 = restTemplate.postQueryString("/objects/$id/enable")
        assertEquals(HttpStatus.OK, response1.statusCode)

        val response2 = restTemplate.postQueryString("/objects/$id/enable")
        assertEquals(HttpStatus.NOT_MODIFIED, response2.statusCode)
    }
}
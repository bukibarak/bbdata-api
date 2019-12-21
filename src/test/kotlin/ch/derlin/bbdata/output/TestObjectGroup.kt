package ch.derlin.bbdata.output

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.random.Random

/**
 * date: 20.12.19
 * @author Lucy Linder <lucy.derlin@gmail.com>
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Profiles.UNSECURED, Profiles.NO_CASSANDRA)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestObjectGroup {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    companion object {
        val name = "test-${Random.nextInt(10000)}"
        var id: Int? = -1
        var tpl: TestRestTemplate? = null

        @AfterAll
        @JvmStatic
        fun cleanup() {
            tpl?.let { tpl ->
                id?.let {
                    try {
                        tpl.deleteQueryString("/objectGroups/$it")
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    @Test
    fun `1-0 test create object group fail`() {
        // == create no name
        var putResponse = restTemplate.putForEntity("/objectGroups",
                """{"name": "", "owner": 1, "description": "test"}""", String::class.java)
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create no owner
        putResponse = restTemplate.putForEntity("/objectGroups",
                """{"name": "$name", "description": "test"}""", String::class.java)
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, putResponse.statusCode)

        // == create wrong owner
        putResponse = restTemplate.putForEntity("/objectGroups",
                """{"name": "$name", "owner": -1, "description": "test"}""", String::class.java)
        Assertions.assertEquals(HttpStatus.NOT_FOUND, putResponse.statusCode)
    }

    @Test
    fun `1-1 test create object group`() {
        // == create
        val putResponse = restTemplate.putForEntity("/objectGroups",
                """{"name": "$name", "owner": 1, "description": "test"}""", String::class.java)
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)

        // == store variables
        id = JsonPath.parse(putResponse.body).read<Int>("$.id")
        tpl = restTemplate

        // == get
        val getResponse = restTemplate.getForEntity("/objectGroups/${id}", String::class.java)
        JSONAssert.assertEquals(putResponse.body, getResponse.body, false)

        // check some json variables
        val json = JsonPath.parse(getResponse.body)
        Assertions.assertTrue(json.read<String>("$.owner.name").equals("admin"))
        Assertions.assertTrue(json.read<String>("$.description").equals("test"))
    }

    @Test
    fun `1-2 test add object`() {
        val putResponse = restTemplate.putQueryString("/objectGroups/$id/objects?objectId=1")
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)

        val json = restTemplate.getQueryJson("/objectGroups/${id}", String::class.java).second
        Assertions.assertTrue(json.read<List<Any>>("$.objects[?(@.id == 1)]").size > 0)
    }

    @Test
    fun `1-3 test get objects`() {
        val json1 = restTemplate.getQueryJson("/objectGroups/${id}", String::class.java).second
        val objs1 = json1.read<List<String>>("$.objects")
        Assertions.assertTrue(objs1.size > 0)

        val objs2 = restTemplate.getQueryString("/objectGroups/${id}/objects", String::class.java).body!!
        JSONAssert.assertEquals(objs1.toString(), objs2, false)
    }

    @Test
    fun `1-4 test remove object`() {
        val putResponse = restTemplate.deleteQueryString("/objectGroups/$id/objects?objectId=1")
        Assertions.assertEquals(HttpStatus.OK, putResponse.statusCode)

        val putResponse2 = restTemplate.deleteQueryString("/objectGroups/$id/objects?objectId=1")
        Assertions.assertEquals(HttpStatus.NOT_MODIFIED, putResponse2.statusCode)
    }

    @Test
    fun `1-5 test remove object group`() {
        val putResponse = restTemplate.deleteQueryString("/objectGroups/$id")
        Assertions.assertEquals(putResponse.statusCode, HttpStatus.OK)

        val putResponse2 = restTemplate.deleteQueryString("/objectGroups/$id")
        Assertions.assertEquals(putResponse2.statusCode, HttpStatus.NOT_MODIFIED)

        val getStatus = restTemplate.getQueryJson("/objectGroups/$id").first
        Assertions.assertEquals(HttpStatus.NOT_FOUND, getStatus)
    }
}
package de.datenwissen.util.groovyrdf.jena;

import static org.junit.Assert.*

import org.junit.Before
import org.junit.Test

import de.datenwissen.util.groovyrdf.core.RdfData
import de.datenwissen.util.groovyrdf.core.RdfNamespace;
import de.datenwissen.util.groovyrdf.core.RdfResource

@SuppressWarnings ("GroovyAssignabilityCheck")
class JenaRdfDataTest {

    RdfNamespace vocab = new RdfNamespace ("http://example.com/vocab/")

    RdfData rdfData

    @Before
    public void setUp () throws Exception {
        def rdfBuilder = new JenaRdfBuilder ()
        rdfData = rdfBuilder {
            "http://example.com/resource/alice" {
                a vocab.Person
                "$vocab.name" "Alice"
                "$vocab.nick" (["Ally", "Licy"])
                "$vocab.age" 23
                "$vocab.title" "Mrs.", [lang: "en"]
                "$vocab.title" "Frau", [lang: "de"]
                "$vocab.hobby" "Reading", [lang: "en"]
                "$vocab.friend" {
                    "http://example.com/resource/bob" {
                        "$vocab.name" "Bob"
                        "$vocab.age" 25
                    }
                }
                "$vocab.knows" {
                    "http://example.com/resource/bob" {
                        "$vocab.name" "Bob"
                    }
                    "http://example.com/resource/trudy" {
                        "$vocab.name" "Trudy"
                    }
                    "http://example.com/resource/carl" {
                        "$vocab.name" "Carl"
                    }
                }
            }
        }
    }

    @Test
    void testGetResource () {
        RdfResource alice = rdfData."http://example.com/resource/alice"
        assertEquals ("http://example.com/resource/alice", alice.uri)
    }

    @Test
    void testGetResourceViaCall () {
        RdfResource alice = rdfData "http://example.com/resource/alice"
        assertEquals ("http://example.com/resource/alice", alice.uri)
    }

    @Test
    void testGetType () {
        RdfResource alice = rdfData "http://example.com/resource/alice"
        assertEquals vocab.Person, alice.type
    }

    @Test
    void testGetTypeWithoutType () {
        RdfResource bob = rdfData "http://example.com/resource/bob"
        assertNull 'Bob should have no type', bob.type
    }

    @Test
    void testGetLiteralProperty () {
        RdfResource alice = rdfData."http://example.com/resource/alice"
        String name = alice[vocab.name]
        assertEquals ("Alice", name)
    }

    @Test
    void testGetLiteralPropertyViaCall () {
        RdfResource alice = rdfData "http://example.com/resource/alice"
        String name = alice vocab.name
        assertEquals "Alice", name
    }

    @Test
    void testGetTypedLiteralProperty () {
        RdfResource alice = rdfData."http://example.com/resource/alice"
        int age = alice[vocab.age]
        assertEquals (23, age)
    }

    @Test
    void testGetTypedLiteralPropertyViaCall () {
        RdfResource alice = rdfData ("http://example.com/resource/alice")
        int age = alice vocab.age
        assertEquals (23, age)
    }

    @Test
    void testGetResourceProperty () {
        RdfResource alice = rdfData."http://example.com/resource/alice"
        RdfResource bob = alice[vocab.friend]
        assertEquals ("http://example.com/resource/bob", bob.uri)
        assertEquals ("Bob", bob[vocab.name])
        assertEquals (25, bob[vocab.age])
    }

    @Test
    void testGetResourcePropertyViaCall () {
        RdfResource alice = rdfData ("http://example.com/resource/alice")
        RdfResource bob = alice vocab.friend
        assertEquals ("http://example.com/resource/bob", bob.uri)
        assertEquals ("Bob", bob (vocab.name))
        assertEquals (25, bob (vocab.age))
    }

    @Test
    void testGetResourcePropertyCascading () {
        RdfResource alice = rdfData "http://example.com/resource/alice"
        String bobsName = alice (vocab.friend) (vocab.name)
        assertEquals "Bob", bobsName
    }

    @Test
    void testGetManyLiteralProperties () {
        RdfResource alice = rdfData."http://example.com/resource/alice"
        def nickSet = alice[vocab.nick]

        assertTrue (nickSet instanceof Set)
        assertEquals (2, nickSet.size ())

        assertTrue (nickSet.contains ("Ally"))
        assertTrue (nickSet.contains ("Licy"))

    }

    @Test
    void testGetManyResourceProperties () {
        RdfResource alice = rdfData."http://example.com/resource/alice"
        def knowsSet = alice[vocab.knows]

        assertTrue (knowsSet instanceof Set)
        assertEquals (3, knowsSet.size ())

        def expectedKnowns = [
                "http://example.com/resource/bob": "Bob",
                "http://example.com/resource/trudy": "Trudy",
                "http://example.com/resource/carl": "Carl"
        ]

        expectedKnowns.each { uri, name ->
            def known = knowsSet.find {
                it.uri == uri
            }
            assertNotNull (known)
            assertEquals (name, known (vocab.name))
        }

    }

    @Test
    void testGetLiteralPropertyWithLanguage () {
        RdfResource alice = rdfData."http://example.com/resource/alice"
        String value = alice vocab.title, "en"
        assertEquals "Mrs.", value
        value = alice vocab.title, "de"
        assertEquals "Frau", value
        value = alice vocab.hobby, "en"
        assertEquals "Reading", value
        value = alice vocab.hobby, "de"
        assertNull value
    }

    @Test
    void testGetLiteralPropertySetOfAllLanguages () {
        RdfResource alice = rdfData."http://example.com/resource/alice"
        Set<String> titles = alice vocab.title
        assertEquals (['Mrs.', 'Frau'] as Set, titles)
    }

    @Test
    void testGetLiteralPropertyEvenIfLanguageIsNotPresent () {
        RdfResource alice = rdfData."http://example.com/resource/alice"
        String name = alice (vocab.name, 'en')
        assertEquals ('Alice', name)
    }

    @Test
    void testListProperties () {
        def expectedProperties = [
                vocab.name,
                vocab.nick,
                vocab.age,
                vocab.title,
                vocab.hobby,
                vocab.friend,
                vocab.knows
        ] as Set
        RdfResource alice = rdfData."http://example.com/resource/alice"
        assertEquals (expectedProperties, alice.listProperties ())
    }

    @Test
    void testListSubjects () {
        assertEquals ([
                'http://example.com/resource/alice',
                'http://example.com/resource/carl',
                'http://example.com/resource/bob',
                'http://example.com/resource/trudy'
        ] as Set, rdfData.listSubjects ()*.uri.toSet ())
    }

    @Test
    void testListSubjectsOfType () {
        assertEquals (['http://example.com/resource/alice'], rdfData.listSubjects (vocab.Person)*.uri)
    }

    @Test
    void testEquals () {
        assertTrue (rdfData.equals (rdfData))
        assertTrue (new JenaRdfData ().equals (new JenaRdfData ()))
        assertFalse (rdfData.equals (null))
        assertFalse (rdfData.equals (new JenaRdfData ()))
        assertFalse (rdfData.equals (new Date ()))
    }

}

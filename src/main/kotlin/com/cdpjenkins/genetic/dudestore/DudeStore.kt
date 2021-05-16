package com.cdpjenkins.genetic.dudestore

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.postgres.PostgresPlugin

fun main(args: Array<String>) {
    val jdbi: Jdbi = Jdbi.create("jdbc:postgresql://localhost/dude_db?user=test_user&password=BUuVrC0QTe9A")
        .installPlugin(PostgresPlugin())

    jdbi.withHandle<Int, Exception> {
        it.execute("DROP TABLE IF EXISTS Dudes")
        it.execute("CREATE TABLE Dudes(name VARCHAR(50), generation INT)")
        it.createUpdate("INSERT INTO Dudes VALUES(:name, :generation)")
            .bind("name", "Mr Cow")
            .bind("generation", 1)
            .execute();
    }

    val results: List<Dude> = jdbi.withHandle<MutableList<Dude>, Exception> {
        it.createQuery("SELECT * FROM Dudes")
            .mapToBean(Dude::class.java)
            .list()
    }

    println(results)

}

data class Dude(
    var name: String,
    var generation: Int = -1
)


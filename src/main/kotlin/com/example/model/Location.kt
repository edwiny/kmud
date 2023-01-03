package com.example.model

class Exit(
    val id: Int,
    val key: String,
    val description: String,
    val fromDirection: String,    //east, west, up, down, etc
    val fromLocation: Location,
    val toDirection: String,
    val toLocation: Location
)


class Location(
    val id: Int,
    val name: String,
    val description: String,
    val exits: List<Exit>,
    var characters: MutableList<Character>
)


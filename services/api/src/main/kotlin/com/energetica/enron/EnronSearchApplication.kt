package com.energetica.enron

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class EnronSearchApplication

fun main(args: Array<String>) {
	runApplication<EnronSearchApplication>(*args)
}

package com.ludoscity.findmybikes.common

class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}

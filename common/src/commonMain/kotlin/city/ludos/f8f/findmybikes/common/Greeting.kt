package city.ludos.f8f.findmybikes.common

class Greeting {
    fun greeting(): String {
        return "Hello, ${Platform().platform}!"
    }
}
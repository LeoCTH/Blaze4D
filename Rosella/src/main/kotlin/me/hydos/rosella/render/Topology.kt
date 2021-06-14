package me.hydos.rosella.render

import org.lwjgl.vulkan.VK10

enum class Topology(val vkType: Int) {
	TRIANGLES(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST),
	TRIANGLE_STRIP(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
}
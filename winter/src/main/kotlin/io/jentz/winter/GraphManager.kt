package io.jentz.winter

object GraphManager {

    private class Node(val parent: Node?, val qualifier: Any, val graph: Graph) {
        val children = mutableMapOf<Any, Node>()
    }

    @JvmStatic
    var rootComponent: Component? = null

    @JvmStatic
    var root: Graph?
        get() = rootNode?.graph
        set(value) {
            rootNode = if (value != null) {
                Node(null, Any(), value)
            } else {
                null
            }
        }

    private var rootNode: Node? = null

    @JvmStatic
    fun open(vararg qualifiers: Any, builderBlock: ComponentBuilderBlock? = null): Graph {
        val rootComponent = rootComponent
        val rootNode = rootNode

        if (rootComponent == null && rootNode == null) {
            throw WinterException("Set root-graph or root-component before calling open.")
        }

        if (qualifiers.isEmpty()) return lazyInitRoot(builderBlock).graph

        var node = lazyInitRoot()

        for (i in 0 until qualifiers.size - 1) {
            val qualifier = qualifiers[i]
            node = node.children[qualifiers[i]] ?: let {
                try {
                    Node(node, qualifier, node.graph.initSubcomponent(qualifier)).also { node.children[qualifier] = it }
                } catch (e: EntryNotFoundException) {
                    val path = qualifiers.slice(0..i).joinToString(" -> ")
                    throw WinterException("Subcomponent with path '$path' does not exist.")
                }
            }
        }

        val qualifier = qualifiers.last()
        node.children[qualifier]?.let { return it.graph }

        try {
            return node.graph
                    .initSubcomponent(qualifier, builderBlock)
                    .also { node.children[qualifier] = Node(node, qualifier, it) }
        } catch (e: EntryNotFoundException) {
            val path = qualifiers.joinToString(" -> ")
            throw WinterException("Subcomponent with path '$path' does not exist.")
        }
    }

    @JvmStatic
    fun close(vararg qualifiers: Any) {
        val root = rootNode ?: throw WinterException("Close called but nothing is open.")
        rootNode = null
        
        val node = qualifiers.fold(root) { node, qualifier ->
            node.children[qualifier] ?: let {
                val path = qualifiers.joinToString(" -> ")
                throw WinterException("Subcomponent with path '$path' is not initialized.")
            }
        }
        disposeNode(node)
        node.parent?.children?.remove(node.qualifier)
    }

    private fun disposeNode(node: Node) {
        node.children.values.forEach { disposeNode(it) }
        node.graph.dispose()
    }

    @JvmStatic
    fun reset() {
        rootNode = null
        rootComponent = null
    }

    private fun lazyInitRoot(builderBlock: ComponentBuilderBlock? = null): Node {
        rootNode?.let { return it }

        val component = rootComponent ?: throw WinterException("Set root-graph or root-component before calling open.")
        return Node(null, Any(), component.init(builderBlock)).also { this.rootNode = it }
    }

}
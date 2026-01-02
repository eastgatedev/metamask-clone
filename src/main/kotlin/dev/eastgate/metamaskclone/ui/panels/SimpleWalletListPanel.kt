package dev.eastgate.metamaskclone.ui.panels

import com.intellij.ui.components.JBList
import dev.eastgate.metamaskclone.models.Wallet
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class SimpleWalletListPanel : JPanel(BorderLayout()) {
    private val listModel = DefaultListModel<Wallet>()
    private val walletList = JBList(listModel)

    var onWalletSelected: ((Wallet) -> Unit)? = null

    init {
        setupUI()
    }

    private fun setupUI() {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Wallets"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        )
        preferredSize = Dimension(0, 200)
        minimumSize = Dimension(0, 150)

        // Setup wallet list
        walletList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        walletList.cellRenderer = SimpleWalletRenderer()

        walletList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selected = walletList.selectedValue
                if (selected != null) {
                    onWalletSelected?.invoke(selected)
                }
            }
        }

        val scrollPane = JScrollPane(walletList)
        scrollPane.preferredSize = Dimension(0, 170)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER

        add(scrollPane, BorderLayout.CENTER)
    }

    fun updateWallets(wallets: List<Wallet>) {
        listModel.clear()
        wallets.forEach { listModel.addElement(it) }

        // Select first wallet if available and nothing is selected
        if (wallets.isNotEmpty() && walletList.selectedIndex == -1) {
            walletList.selectedIndex = 0
        }
    }

    private class SimpleWalletRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): java.awt.Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

            if (value is Wallet) {
                text = value.name
                if (value.isImported) {
                    text += " (imported)"
                }
            }

            border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
            return this
        }
    }
}

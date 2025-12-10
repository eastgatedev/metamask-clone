package dev.eastgate.metamaskclone.ui.panels

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import dev.eastgate.metamaskclone.models.Token
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel

class TokenListPanel : JPanel() {

    var onTokenSelected: ((Token) -> Unit)? = null
    var onAddTokenClick: (() -> Unit)? = null

    private val tokenListContainer = JPanel()
    private var tokens: List<Token> = emptyList()

    init {
        setupUI()
    }

    private fun setupUI() {
        layout = BorderLayout()
        border = JBUI.Borders.empty(0, 10)

        // Token list container
        tokenListContainer.layout = BoxLayout(tokenListContainer, BoxLayout.Y_AXIS)

        val scrollPane = JBScrollPane(tokenListContainer)
        scrollPane.border = null
        scrollPane.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER

        // Add token button at bottom
        val addTokenButton = JButton("+ Add Token")
        addTokenButton.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addTokenButton.addActionListener {
            onAddTokenClick?.invoke()
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 8))
        buttonPanel.add(addTokenButton)

        add(scrollPane, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)

        // Default empty state
        updateTokens(emptyList())
    }

    fun updateTokens(tokenList: List<Token>) {
        tokens = tokenList
        tokenListContainer.removeAll()

        if (tokens.isEmpty()) {
            val emptyLabel = JBLabel("No tokens added")
            emptyLabel.foreground = JBColor.gray
            emptyLabel.alignmentX = LEFT_ALIGNMENT
            emptyLabel.border = JBUI.Borders.empty(20, 0)
            tokenListContainer.add(emptyLabel)
        } else {
            for (token in tokens) {
                tokenListContainer.add(createTokenRow(token))
                tokenListContainer.add(Box.createVerticalStrut(4))
            }
        }

        tokenListContainer.add(Box.createVerticalGlue())
        tokenListContainer.revalidate()
        tokenListContainer.repaint()
    }

    private fun createTokenRow(token: Token): JPanel {
        val row = JPanel(BorderLayout())
        row.alignmentX = LEFT_ALIGNMENT
        row.maximumSize = Dimension(Int.MAX_VALUE, 60)
        row.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBUI.Borders.empty(10, 5)
        )
        row.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        // Left side: token icon and symbol
        val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 10, 0))
        leftPanel.isOpaque = false

        // Token icon (first letter)
        val iconLabel = JBLabel(token.symbol.firstOrNull()?.uppercase() ?: "?")
        iconLabel.font = iconLabel.font.deriveFont(Font.BOLD, 14f)
        iconLabel.horizontalAlignment = JBLabel.CENTER
        iconLabel.preferredSize = Dimension(36, 36)
        iconLabel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1, true),
            JBUI.Borders.empty(8)
        )
        leftPanel.add(iconLabel)

        // Token name/symbol
        val namePanel = JPanel()
        namePanel.layout = BoxLayout(namePanel, BoxLayout.Y_AXIS)
        namePanel.isOpaque = false

        val symbolLabel = JBLabel(token.symbol)
        symbolLabel.font = symbolLabel.font.deriveFont(Font.BOLD, 13f)
        namePanel.add(symbolLabel)

        if (token.name.isNotEmpty() && token.name != token.symbol) {
            val nameLabel = JBLabel(token.name)
            nameLabel.font = nameLabel.font.deriveFont(Font.PLAIN, 11f)
            nameLabel.foreground = JBColor.gray
            namePanel.add(nameLabel)
        }

        leftPanel.add(namePanel)

        // Right side: balance
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        rightPanel.isOpaque = false

        val balanceLabel = JBLabel("${token.getFormattedBalance()} ${token.symbol}")
        balanceLabel.font = balanceLabel.font.deriveFont(Font.PLAIN, 12f)
        rightPanel.add(balanceLabel)

        row.add(leftPanel, BorderLayout.WEST)
        row.add(rightPanel, BorderLayout.EAST)

        // Click listener
        row.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onTokenSelected?.invoke(token)
            }

            override fun mouseEntered(e: MouseEvent) {
                row.background = JBColor(0xF5F5F5.toInt(), 0x3C3F41.toInt())
            }

            override fun mouseExited(e: MouseEvent) {
                row.background = null
            }
        })

        return row
    }

    fun getTokens(): List<Token> = tokens
}

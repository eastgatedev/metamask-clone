package dev.eastgate.metamaskclone.ui.panels

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import dev.eastgate.metamaskclone.models.Token
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.SwingConstants

class MainTabPanel : JPanel() {

    var onAddTokenClick: (() -> Unit)? = null
    var onTokenSelected: ((Token) -> Unit)? = null

    private val tabsPanel = JPanel(FlowLayout(FlowLayout.CENTER, 20, 0))
    private val contentPanel = JPanel(CardLayout())
    private val tokenListPanel = TokenListPanel()
    private val activityPanel = createActivityPlaceholder()

    private val tokensTab = createTabLabel("Tokens", true)
    private val activityTab = createTabLabel("Activity", false)

    private var selectedTab = "Tokens"

    init {
        setupUI()
    }

    private fun setupUI() {
        layout = BorderLayout()

        // Tabs row
        tabsPanel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border())
        tabsPanel.add(tokensTab)
        tabsPanel.add(activityTab)

        // Content cards
        contentPanel.add(tokenListPanel, "Tokens")
        contentPanel.add(activityPanel, "Activity")

        // Wire up token list events
        tokenListPanel.onAddTokenClick = { onAddTokenClick?.invoke() }
        tokenListPanel.onTokenSelected = { token -> onTokenSelected?.invoke(token) }

        add(tabsPanel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)

        // Set initial tab
        selectTab("Tokens")
    }

    private fun createTabLabel(text: String, selected: Boolean): JPanel {
        val panel = JPanel(BorderLayout())
        panel.isOpaque = false
        panel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

        val label = JBLabel(text)
        label.horizontalAlignment = SwingConstants.CENTER
        label.font = label.font.deriveFont(Font.PLAIN, 13f)
        label.border = JBUI.Borders.empty(10, 15)

        val underline = JPanel()
        underline.preferredSize = java.awt.Dimension(0, 2)
        underline.background = if (selected) JBColor(0x6366F1.toInt(), 0x818CF8.toInt()) else JBColor.PanelBackground

        panel.add(label, BorderLayout.CENTER)
        panel.add(underline, BorderLayout.SOUTH)

        panel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                selectTab(text)
            }

            override fun mouseEntered(e: MouseEvent) {
                if (selectedTab != text) {
                    label.foreground = JBColor(0x6366F1.toInt(), 0x818CF8.toInt())
                }
            }

            override fun mouseExited(e: MouseEvent) {
                if (selectedTab != text) {
                    label.foreground = JBColor.foreground()
                }
            }
        })

        return panel
    }

    private fun selectTab(tabName: String) {
        selectedTab = tabName

        // Update tab styles
        updateTabStyle(tokensTab, tabName == "Tokens")
        updateTabStyle(activityTab, tabName == "Activity")

        // Show corresponding content
        val cardLayout = contentPanel.layout as CardLayout
        cardLayout.show(contentPanel, tabName)
    }

    private fun updateTabStyle(tabPanel: JPanel, selected: Boolean) {
        val label = tabPanel.getComponent(0) as JBLabel
        val underline = tabPanel.getComponent(1) as JPanel

        if (selected) {
            label.foreground = JBColor(0x6366F1.toInt(), 0x818CF8.toInt())
            label.font = label.font.deriveFont(Font.BOLD)
            underline.background = JBColor(0x6366F1.toInt(), 0x818CF8.toInt())
        } else {
            label.foreground = JBColor.foreground()
            label.font = label.font.deriveFont(Font.PLAIN)
            underline.background = JBColor.PanelBackground
        }
    }

    private fun createActivityPlaceholder(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = JBUI.Borders.empty(40, 10)

        val label = JBLabel("No activity yet")
        label.horizontalAlignment = SwingConstants.CENTER
        label.foreground = JBColor.gray

        val subLabel = JBLabel("Transaction history will appear here")
        subLabel.horizontalAlignment = SwingConstants.CENTER
        subLabel.foreground = JBColor.gray
        subLabel.font = subLabel.font.deriveFont(Font.PLAIN, 11f)

        val centerPanel = JPanel(BorderLayout())
        centerPanel.isOpaque = false
        centerPanel.add(label, BorderLayout.CENTER)
        centerPanel.add(subLabel, BorderLayout.SOUTH)

        panel.add(centerPanel, BorderLayout.NORTH)

        return panel
    }

    fun updateTokens(tokens: List<Token>) {
        tokenListPanel.updateTokens(tokens)
    }

    fun getTokenListPanel(): TokenListPanel = tokenListPanel
}

package com.github.yazdipour.jetplugin.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.*
import com.intellij.ui.content.ContentFactory
import com.github.yazdipour.jetplugin.MyBundle
import com.github.yazdipour.jetplugin.services.MyProjectService
import java.awt.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.*


class MyToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = FeedbackPanel()
        val content = ContentFactory.getInstance().createContent(myToolWindow, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}
class FeedbackPanel : JPanel() {
    private val cardLayout = CardLayout()
    private val mainPanel = JPanel(cardLayout)

    private val startPanel = JPanel()
    private val imagePanel = JPanel()
    private val questionPanels = mutableListOf<JPanel>()

    private val startButton = JButton("Start")
    private val imageLabel = JLabel()

    private val questions = listOf(
            "How cute do you find this hedgehog?",
            "What's the first word that comes to mind when you see this image?",
            "If you could give this hedgehog a name, what would it be?",
            "On a scale of 1-10, how likely are you to share this image with a friend?"
    )

    private var currentQuestionIndex = 0
    private val answers = mutableListOf<String>()

    init {
        layout = BorderLayout()
        setupPanels()
        setupListeners()
        add(mainPanel, BorderLayout.CENTER)
    }

    private fun setupPanels() {
        // Start Panel
        startPanel.layout = BoxLayout(startPanel, BoxLayout.Y_AXIS)
        startButton.alignmentX = Component.CENTER_ALIGNMENT
        startPanel.add(Box.createVerticalGlue())
        startPanel.add(startButton)
        startPanel.add(Box.createVerticalGlue())
        mainPanel.add(startPanel, "Start")

        // Image Panel
        imagePanel.layout = BoxLayout(imagePanel, BoxLayout.Y_AXIS)
        imageLabel.alignmentX = Component.CENTER_ALIGNMENT
        val imageNextButtonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
        imageNextButtonPanel.add(JButton("Next").apply {
            addActionListener { showNextQuestion() }
        })
        imagePanel.add(Box.createVerticalGlue())
        imagePanel.add(imageLabel)
        imagePanel.add(Box.createVerticalStrut(20))
        imagePanel.add(imageNextButtonPanel)
        imagePanel.add(Box.createVerticalGlue())
        mainPanel.add(imagePanel, "Image")

        // Question Panels
        questions.forEachIndexed { index, question ->
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

            val questionLabel = JLabel(question)
            questionLabel.alignmentX = Component.CENTER_ALIGNMENT

            val answerField = JTextField(20)
            answerField.maximumSize = Dimension(300, 30)

            val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER))
            val button = if (index == questions.size - 1) {
                JButton("Submit").apply {
                    addActionListener {
                        saveAnswer()
                        saveFeedback()
                        resetUI()
                    }
                }
            } else {
                JButton("Next").apply {
                    addActionListener {
                        saveAnswer()
                        showNextQuestion()
                    }
                }
            }
            buttonPanel.add(button)

            panel.add(Box.createVerticalGlue())
            panel.add(questionLabel)
            panel.add(Box.createVerticalStrut(10))
            panel.add(answerField)
            panel.add(Box.createVerticalStrut(20))
            panel.add(buttonPanel)
            panel.add(Box.createVerticalGlue())

            questionPanels.add(panel)
            mainPanel.add(panel, "Question$index")
        }

        cardLayout.show(mainPanel, "Start")
    }

    private fun setupListeners() {
        startButton.addActionListener {
            showImage()
        }
    }

    private fun showImage() {
        val imageUrl = FeedbackPanel::class.java.getResource("/hedgehog.jpg")
        val image = ImageIO.read(imageUrl)
        val scaledImage = image.getScaledInstance(300, 300, Image.SCALE_SMOOTH)
        imageLabel.icon = ImageIcon(scaledImage)
        cardLayout.show(mainPanel, "Image")
    }

    private fun showNextQuestion() {
        cardLayout.show(mainPanel, "Question$currentQuestionIndex")
        currentQuestionIndex++
    }

    private fun saveAnswer() {
        val currentPanel = questionPanels[currentQuestionIndex - 1]
        val answerField = currentPanel.components.filterIsInstance<JTextField>().firstOrNull()
        answerField?.text?.let { answers.add(it) }
    }

    private fun saveFeedback() {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val feedback = buildString {
            appendLine("Feedback collected on: $timestamp")
            questions.zip(answers).forEach { (question, answer) ->
                appendLine("Q: $question")
                appendLine("A: $answer")
            }
            appendLine("------------------------")
        }

        val homeDir = System.getProperty("user.home")
        val feedbackDir = File(homeDir, "FeedbackPlugin")
        feedbackDir.mkdirs()
        val feedbackFile = File(feedbackDir, "feedback.txt")
        feedbackFile.appendText(feedback)

        JOptionPane.showMessageDialog(this, "Feedback saved. Thank you!\nFile location: ${feedbackFile.absolutePath}")
    }

    private fun resetUI() {
        currentQuestionIndex = 0
        answers.clear()
        questionPanels.forEach { panel ->
            panel.components.filterIsInstance<JTextField>().forEach { it.text = "" }
        }
        cardLayout.show(mainPanel, "Start")
    }
}
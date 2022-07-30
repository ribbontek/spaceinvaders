package com.ribbontek.spaceinvaders.examples

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.SplitPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Tree
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport

class GlassyUIExampleScreen : Screen {
    private lateinit var stage: Stage
    private lateinit var skin: Skin

    private class NodeKt(actor: Actor) : Tree.Node<NodeKt, Any, Actor>(actor)
    private class TreeKt(skin: Skin) : Tree<NodeKt, Actor>(skin)

    override fun show() {
        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage
        skin = Skin(Gdx.files.internal("glassy-ui/glassy-ui.json"))
        val root = Table()
        root.setFillParent(true)
        stage.addActor(root)

        root.add(Label("Glassy UI", skin, "big")).colspan(3)

        root.row()
        var table = Table()
        table.defaults().pad(10.0f)
        table.add(TextButton("Story", skin))
        table.row()
        table.add(TextButton("Options", skin))
        table.row()
        table.add(TextButton("Quit", skin))
        root.add(table).expandX()

        table = Table()
        table.defaults().pad(10.0f)
        table.add(TextButton("Story", skin, "small"))
        table.row()
        table.add(TextButton("Options", skin, "small"))
        table.row()
        table.add(TextButton("Quit", skin, "small"))
        root.add(table).expandX()

        table = Table()
        table.add(Label("Difficulty", skin)).colspan(2)
        table.row()
        val selectBox = SelectBox<Any?>(skin)
        selectBox.setItems("Easy", "Difficult", "Extreme")
        table.add(selectBox).colspan(2)
        table.row()
        table.add(Label("Name: ", skin)).padTop(15.0f)
        table.add(TextField("Nameo", skin)).padTop(15.0f)
        root.add(table).expandX()

        root.row()
        root.add(Label("reticulating splines...", skin)).colspan(3).padTop(5.0f)
        root.row()
        val progressBar = ProgressBar(0f, 100.0f, 1f, false, skin)
        progressBar.value = 50.0f
        progressBar.setAnimateDuration(.2f)
        root.add(progressBar).colspan(3).growX()

        root.row()
        root.add(Label("VOLUME", skin)).colspan(3).padTop(5.0f)
        root.row()
        val slider = Slider(0.0f, 100.0f, 1.0f, false, skin)
        slider.value = 50.0f
        slider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                progressBar.value = slider.value
            }
        })
        root.add(slider).colspan(3).growX().expandY().top()

        val window = Window("Inventory", skin)
        table = Table()
        window.add(table)
        window.setSize(400.0f, 200.0f)
        window.setPosition(stage.width / 2.0f, 25.0f, Align.bottom)
        stage.addActor(window)

        val tree = TreeKt(skin)
        var parent = NodeKt(Label("Backpack", skin, "black"))
        tree.add(parent)
        var child = NodeKt(Label("Kitty Snacks", skin, "black"))
        parent.add(child)
        child = NodeKt(Label("Dripping Bastard Sword of Misfortune", skin, "black"))
        parent.add(child)
        child = NodeKt(Label("Redeeming Pencil of Noteworthiness", skin, "black"))
        parent.add(child)
        parent = NodeKt(Label("Belt", skin, "black"))
        tree.add(parent)
        child = NodeKt(Label("Soda Brand Soda", skin, "black"))
        parent.add(child)
        child = NodeKt(Label("Bundle of Pocket Lint", skin, "black"))
        parent.add(child)
        child = NodeKt(Label("Horadric Dodecahedron", skin, "black"))
        parent.add(child)
        parent = child
        child = NodeKt(Label("Void Boogers", skin, "black"))
        parent.add(child)
        child = NodeKt(Label("Void Boogers", skin, "black"))
        parent.add(child)
        child = NodeKt(Label("Void Boogers", skin, "black"))
        parent.add(child)

        val scrollPane = ScrollPane(tree, skin)
        scrollPane.fadeScrollBars = false

        table = Table(skin)
        table.setBackground("black")
        table.defaults().expandX().left().padLeft(10.0f)
        table.add(CheckBox("CheckBox", skin))
        table.row()
        table.add(CheckBox("CheckBox", skin))
        table.row()
        val buttonGroup = ButtonGroup<CheckBox>()
        var checkBox = CheckBox("Radio Button", skin, "radio")
        buttonGroup.add(checkBox)
        table.add(checkBox)
        table.row()
        checkBox = CheckBox("Radio Button", skin, "radio")
        buttonGroup.add(checkBox)
        table.add(checkBox)

        val splitPane = SplitPane(scrollPane, table, false, skin)
        window.add(splitPane).grow()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(.5f, .5f, .5f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(Gdx.graphics.deltaTime)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
    }

    override fun dispose() {
        stage.dispose()
    }
}

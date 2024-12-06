(ns clojure.vis-ui
  (:refer-clojure :exclude [load])
  (:import (com.badlogic.gdx.graphics.g2d TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.utils Drawable)
           (com.badlogic.gdx.utils Align)
           (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip
                                      Menu
                                      MenuBar
                                      MenuItem
                                      Separator
                                      VisImage
                                      VisTextButton
                                      VisCheckBox
                                      VisSelectBox
                                      VisImageButton
                                      VisTextField
                                      VisLabel
                                      VisScrollPane
                                      VisTree
                                      VisWindow)
           (com.kotcrab.vis.ui.widget.tabbedpane Tab
                                                 TabbedPane)))

(defn tab-widget [{:keys [title content savable? closable-by-user?]}]
  (proxy [Tab] [(boolean savable?) (boolean closable-by-user?)]
    (getTabTitle [] title)
    (getContentTable [] content)))

(defn tabbed-pane []
  (TabbedPane.))

(defn menu [label]
  (Menu. label))

(defn menu-bar []
  (MenuBar.))

(def menu-bar->table MenuBar/.getTable)
(def add-menu        MenuBar/.addMenu)

(defn menu-item [text]
  (MenuItem. text))

(defn configure-tooltips [{:keys [default-appear-delay-time]}]
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float default-appear-delay-time)))

(defn loaded? [] (VisUI/isLoaded))
(defn dispose [] (VisUI/dispose))
(defn skin    [] (VisUI/getSkin))

(defn load [skin-scale]
  (VisUI/load (case skin-scale
                :skin-scale/x1 VisUI$SkinScale/X1
                :skin-scale/x2 VisUI$SkinScale/X2)))

(defn window? [actor]
  (instance? VisWindow actor))

(defn tree []
  (VisTree.))

(defn separator [type]
  (Separator. (case type
                :default "default"
                :vertical "vertial")))

(defn label ^VisLabel [text]
  (VisLabel. ^CharSequence text))

(defn scroll-pane ^VisScrollPane [actor]
  (VisScrollPane. actor))

(defmulti image type)

(defmethod image Drawable [^Drawable drawable]
  (VisImage. drawable))

(defmethod image TextureRegion [^TextureRegion tr]
  (VisImage. tr))

(defn image-button ^VisImageButton [^Drawable drawable]
  (VisImageButton. drawable))

(defn text-button [text]
  (VisTextButton. (str text)))

(defn check-box [text]
  (VisCheckBox. (str text)))

(def checked? VisCheckBox/.isChecked)

(defn text-field [text]
  (VisTextField. (str text)))

(def text-field->text VisTextField/.getText)

(defn select-box [{:keys [items selected]}]
  (doto (VisSelectBox.)
    (.setItems ^"[Lcom.badlogic.gdx.scenes.scene2d.Actor;" (into-array items))
    (.setSelected selected)))

(def selected VisSelectBox/.getSelected)

(defn add-tooltip!
  "tooltip-text is a (fn []) or a string. If it is a function will be-recalculated every show.
  Returns the actor."
  [^Actor actor tooltip-text]
  (let [text? (string? tooltip-text)
        label (VisLabel. (if text? tooltip-text ""))
        tooltip (proxy [Tooltip] []
                  ; hooking into getWidth because at
                  ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                  ; when tooltip position gets calculated we setText (which calls pack) before that
                  ; so that the size is correct for the newly calculated text.
                  (getWidth []
                    (let [^Tooltip this this]
                      (when-not text?
                        (.setText this (str (tooltip-text))))
                      (proxy-super getWidth))))]
    (.setAlignment label Align/center)
    (.setTarget  tooltip actor)
    (.setContent tooltip label))
  actor)

(defn remove-tooltip! [^Actor actor]
  (Tooltip/removeTooltip actor))

(ns gdl.scene2d.vis-ui
  (:require [gdl.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage]
            [com.badlogic.gdx.utils.align :as align]
            [gdl.utils.disposable :as disposable])
  (:import (clojure.lang MultiFn)
           (com.badlogic.gdx.scenes.scene2d Actor)
           (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Separator
                                      Tooltip
                                      VisLabel
                                      VisScrollPane)))

(doseq [[k method-sym] '{:actor.type/menu-bar     gdl.scene2d.vis-ui.menu/create
                         :actor.type/select-box   gdl.scene2d.vis-ui.select-box/create
                         :actor.type/label        gdl.scene2d.vis-ui.label/create
                         :actor.type/text-field   gdl.scene2d.vis-ui.text-field/create
                         :actor.type/check-box    gdl.scene2d.vis-ui.check-box/create
                         :actor.type/table        gdl.scene2d.vis-ui.table/create
                         :actor.type/image-button gdl.scene2d.vis-ui.image-button/create
                         :actor.type/text-button  gdl.scene2d.vis-ui.text-button/create
                         :actor.type/window       gdl.scene2d.vis-ui.window/create
                         :actor.type/image        gdl.scene2d.vis-ui.image/create}
        :let [method-var (requiring-resolve method-sym)]]
  (assert (keyword? k))
  (MultiFn/.addMethod gdl.scene2d/build k method-var))

(defmethod scene2d/build :actor.type/separator-horizontal [_]
  (Separator. "default"))

(defmethod scene2d/build :actor.type/separator-vertical [_]
  (Separator. "vertical"))

(defmethod scene2d/build :actor.type/scroll-pane
  [{:keys [scroll-pane/actor
           actor/name]}]
  (doto (VisScrollPane. actor)
    (.setFlickScroll false)
    (.setFadeScrollBars false)
    (actor/set-name! name)))

(defn load! [{:keys [skin-scale]}]
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case skin-scale
                :x1 VisUI$SkinScale/X1
                :x2 VisUI$SkinScale/X2))
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0))
  (reify disposable/Disposable
    (dispose! [_]
      (VisUI/dispose))))

(let [update-fn (fn [tooltip-text]
                  (fn [tooltip]
                    (when-not (string? tooltip-text)
                      (let [actor (Tooltip/.getTarget tooltip)
                            ; acturs might be initialized without a stage yet so we do when-let
                            ; FIXME double when-let
                            ctx (when-let [stage (actor/get-stage actor)]
                                  (stage/get-ctx stage))]
                        (when ctx ; ctx is only set later for update!/draw! ... not at starting of initialisation
                          (Tooltip/.setText tooltip (str (tooltip-text ctx))))))))]
  (extend-type Actor
    actor/Tooltip
    (add-tooltip! [actor tooltip-text]
      (let [text? (string? tooltip-text)
            label (doto (VisLabel. ^CharSequence (str (if text? tooltip-text "")))
                    (.setAlignment (align/k->value :center)))
            update-text! (update-fn tooltip-text)]
        (doto (proxy [Tooltip] []
                ; hooking into getWidth because at
                ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                ; when tooltip position gets calculated we setText (which calls pack) before that
                ; so that the size is correct for the newly calculated text.
                (getWidth []
                  (update-text! this)
                  (let [^Tooltip this this]
                    (proxy-super getWidth))))
          (.setTarget  actor)
          (.setContent label)))
      actor)

    (remove-tooltip! [actor]
      (Tooltip/removeTooltip actor))))

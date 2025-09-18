(ns com.kotcrab.vis.ui.vis-ui
  (:require clojure.config
            [clojure.walk :as walk]
            [com.badlogic.gdx.utils.align :as align]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]
            [gdl.scene2d.stage :as stage])
  (:import (clojure.lang MultiFn)
           (com.badlogic.gdx.scenes.scene2d Actor)
           (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip
                                      VisLabel
                                      VisScrollPane)))

; in cdq referenced:
; * com.kotcrab.vis.ui.widget.separator

(defmethod scene2d/build :actor.type/scroll-pane
  [{:keys [scroll-pane/actor
           actor/name]}]
  (doto (VisScrollPane. actor)
    (.setFlickScroll false)
    (.setFadeScrollBars false)
    (actor/set-name! name)))

(def impls (walk/postwalk
            clojure.config/require-resolve-symbols
            '[
              [gdl.scene2d/build
               :actor.type/menu-bar
               com.kotcrab.vis.ui.widget.menu/create]

              [gdl.scene2d/build
               :actor.type/select-box
               com.kotcrab.vis.ui.widget.select-box/create]

              [gdl.scene2d/build
               :actor.type/label
               com.kotcrab.vis.ui.widget.label/create]

              [gdl.scene2d/build
               :actor.type/text-field
               com.kotcrab.vis.ui.widget.text-field/create]

              [gdl.scene2d/build
               :actor.type/check-box
               com.kotcrab.vis.ui.widget.check-box/create]

              [gdl.scene2d/build
               :actor.type/table
               com.kotcrab.vis.ui.widget.table/create]

              [gdl.scene2d/build
               :actor.type/image-button
               com.kotcrab.vis.ui.widget.image-button/create]

              [gdl.scene2d/build
               :actor.type/text-button
               com.kotcrab.vis.ui.widget.text-button/create]

              [gdl.scene2d/build
               :actor.type/window
               com.kotcrab.vis.ui.widget.window/create]

              [gdl.scene2d/build
               :actor.type/image
               com.kotcrab.vis.ui.widget.image/create]]))

(defn load! [{:keys [skin-scale]}]
  (doseq [[defmulti-var k method-fn-var] impls]
    (assert (var? defmulti-var))
    (assert (keyword? k))
    (assert (var? method-fn-var))
    (MultiFn/.addMethod @defmulti-var k method-fn-var))
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
  (reify com.badlogic.gdx.utils.Disposable
    (dispose [_]
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
                  (proxy-super getWidth)))
          (.setTarget  actor)
          (.setContent label)))
      actor)

    (remove-tooltip! [actor]
      (Tooltip/removeTooltip actor))))

(ns clojure.scene2d.vis-ui
  (:require [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [com.badlogic.gdx.utils.align :as align]
            [gdl.disposable :as disposable]
            [com.kotcrab.vis.ui.widget.tooltip :as tooltip]
            [com.kotcrab.vis.ui.widget.separator :as separator]
            [com.kotcrab.vis.ui.widget.vis-label :as vis-label]
            [com.kotcrab.vis.ui.widget.vis-scroll-pane :as vis-scroll-pane]
            [com.kotcrab.vis.ui.vis-ui :as vis-ui]))

(doseq [[k method-sym] '{:actor.type/actor            com.badlogic.gdx.scenes.scene2d.actor/create
                         :actor.type/group            com.badlogic.gdx.scenes.scene2d.group/create
                         :actor.type/horizontal-group com.badlogic.gdx.scenes.scene2d.ui.horizontal-group/create
                         :actor.type/stack            com.badlogic.gdx.scenes.scene2d.ui.stack/create
                         :actor.type/widget           com.badlogic.gdx.scenes.scene2d.ui.widget/create
                         :actor.type/menu-bar     clojure.scene2d.vis-ui.menu/create
                         :actor.type/select-box   com.kotcrab.vis.ui.widget.vis-select-box/create
                         :actor.type/label        clojure.scene2d.vis-ui.label/create
                         :actor.type/text-field   clojure.scene2d.vis-ui.text-field/create
                         :actor.type/check-box    clojure.scene2d.vis-ui.check-box/create
                         :actor.type/table        clojure.scene2d.vis-ui.table/create
                         :actor.type/image-button clojure.scene2d.vis-ui.image-button/create
                         :actor.type/text-button  clojure.scene2d.vis-ui.text-button/create
                         :actor.type/window       clojure.scene2d.vis-ui.window/create
                         :actor.type/image        clojure.scene2d.vis-ui.image/create}
        :let [method-var (requiring-resolve method-sym)]]
  (assert (keyword? k))
  (clojure.lang.MultiFn/.addMethod com.badlogic.gdx.scenes.scene2d/build k method-var))

(defmethod scene2d/build :actor.type/separator-horizontal [_]
  (separator/horizontal))

(defmethod scene2d/build :actor.type/separator-vertical [_]
  (separator/vertical))

(defmethod scene2d/build :actor.type/scroll-pane
  [{:keys [scroll-pane/actor
           actor/name]}]
  (doto (vis-scroll-pane/create actor
                                {:flick-scroll? false
                                 :fade-scroll-bars? false})
    (actor/set-name! name)))

(defn load! [{:keys [skin-scale]}]
  ; app crashes during startup before vis-ui/dispose!
  ; and we do clojure.tools.namespace.refresh -> gui elements not showing.
  (when (vis-ui/loaded?)
    (vis-ui/dispose!))
  (vis-ui/load! skin-scale)
  (-> (vis-ui/skin)
      (.getFont "default-font") ; FIXME SKIN !
      .getData
      .markupEnabled
      (set! true))
  (tooltip/set-default-appear-delay-time! 0)
  (reify disposable/Disposable
    (dispose! [_]
      (vis-ui/dispose!))))

(let [update-fn (fn [tooltip-text]
                  (fn [tooltip]
                    (when-not (string? tooltip-text)
                      (let [actor (tooltip/target tooltip)
                            ctx (when-let [stage (actor/get-stage actor)]
                                  (stage/get-ctx stage))]
                        (when ctx
                          (tooltip/set-text! tooltip (tooltip-text ctx)))))))]
  (extend-type com.badlogic.gdx.scenes.scene2d.Actor
    actor/Tooltip
    (add-tooltip! [actor tooltip-text]
      (let [text? (string? tooltip-text)
            label (doto (vis-label/create (if text? tooltip-text ""))
                    (.setAlignment (align/k->value :center)))
            update-text! (update-fn tooltip-text)]
        (tooltip/create {:update-fn update-text!
                         :target actor
                         :content label}))
      actor)

    (remove-tooltip! [actor]
      (tooltip/remove! actor))))

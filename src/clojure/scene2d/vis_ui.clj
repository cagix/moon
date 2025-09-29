(ns clojure.scene2d.vis-ui
  (:require [com.kotcrab.vis.ui.vis-ui :as vis-ui]
            [com.kotcrab.vis.ui.widget.tooltip :as tooltip]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [com.badlogic.gdx.utils.align :as align]
            [clojure.disposable :as disposable])
  (:import (clojure.lang MultiFn)
           (com.badlogic.gdx.scenes.scene2d Actor)
           (com.kotcrab.vis.ui.widget Separator
                                      VisLabel
                                      VisScrollPane)))

(doseq [[k method-sym] '{:actor.type/menu-bar     clojure.scene2d.vis-ui.menu/create
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
  (MultiFn/.addMethod clojure.scene2d/build k method-var))

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
  (extend-type Actor
    actor/Tooltip
    (add-tooltip! [actor tooltip-text]
      (let [text? (string? tooltip-text)
            label (doto (VisLabel. ^CharSequence (str (if text? tooltip-text "")))
                    (.setAlignment (align/k->value :center)))
            update-text! (update-fn tooltip-text)]
        (tooltip/create {:update-fn update-text!
                         :target actor
                         :content label}))
      actor)

    (remove-tooltip! [actor]
      (tooltip/remove! actor))))

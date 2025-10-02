(ns clojure.scene2d.vis-ui
  (:require [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.group :as group]
            [com.badlogic.gdx.scenes.scene2d.ui.stack :as stack]
            [com.badlogic.gdx.scenes.scene2d.ui.horizontal-group :as horizontal-group]
            [gdl.scene2d.actor]
            [clojure.scene2d.actor]
            [clojure.scene2d.group]
            [clojure.scene2d.widget-group :as widget-group]
            [gdl.disposable :as disposable]
            [com.kotcrab.vis.ui.widget.tooltip :as tooltip]
            [com.kotcrab.vis.ui.widget.separator :as separator]
            [com.kotcrab.vis.ui.widget.vis-scroll-pane :as vis-scroll-pane]
            [com.kotcrab.vis.ui.vis-ui :as vis-ui]))

(defmethod scene2d/build :actor.type/horizontal-group [opts]
  (doto (horizontal-group/create opts)
    (clojure.scene2d.group/set-opts! opts)))

(defmethod scene2d/build :actor.type/stack [opts]
  (doto (stack/create)
    (widget-group/set-opts! opts)))

(defmethod scene2d/build :actor.type/group [opts]
  (doto (group/create)
    (clojure.scene2d.group/set-opts! opts)))

(defmethod scene2d/build :actor.type/actor [opts]
  (doto (actor/create opts)
    (clojure.scene2d.actor/set-opts! opts)))

(doseq [[k method-sym] '{:actor.type/widget       com.badlogic.gdx.scenes.scene2d.ui.widget/create
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

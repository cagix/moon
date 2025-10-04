(ns clojure.scene2d.vis-ui
  (:require [clojure.gdx.scenes.scene2d.vis-ui.core :as vis-ui]
            [clojure.gdx.scenes.scene2d.vis-ui.widget.tooltip :as tooltip]
            [clojure.disposable :as disposable]))

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

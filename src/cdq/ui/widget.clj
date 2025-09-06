(ns cdq.ui.widget
  (:require [clojure.gdx.scenes.scene2d :as scene2d]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.ui.widget :as widget]))

(defn create [opts]
  (widget/create
    (fn [this _batch _parent-alpha]
      (when-let [f (:draw opts)]
        (scene2d/try-draw this f)))))

(defn set-opts! [widget opts]
  (actor/set-opts! widget opts))

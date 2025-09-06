(ns cdq.ui.widget
  (:require [cdq.ui.actor :as actor]
            [clojure.gdx.scenes.scene2d.ui.widget :as widget]))

(defn create [opts]
  (widget/create
    (fn [this _batch _parent-alpha]
      (when-let [f (:draw opts)]
        (actor/try-draw this f)))))

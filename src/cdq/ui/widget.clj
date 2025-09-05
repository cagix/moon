(ns cdq.ui.widget
  (:require [cdq.ui :as ui]
            cdq.construct
            [clojure.gdx.scenes.scene2d.ui.widget :as widget]))

(defmethod cdq.construct/construct :actor.type/widget [opts]
  (widget/create
    (fn [this _batch _parent-alpha]
      (when-let [f (:draw opts)]
        (ui/try-draw this f)))))

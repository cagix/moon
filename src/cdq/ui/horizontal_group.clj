(ns cdq.ui.horizontal-group
  (:require [cdq.ui :as ui]
            cdq.construct
            [clojure.gdx.scenes.scene2d.ui.horizontal-group :as horizontal-group]))

(defmethod cdq.construct/construct :actor.type/horizontal-group [{:keys [space pad] :as opts}]
  (doto (horizontal-group/create space pad)
    (ui/set-opts! opts)))

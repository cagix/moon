(ns cdq.ui.stack
  (:require [clojure.gdx.scenes.scene2d.ui :as ui]
            [cdq.ui.widget-group :as widget-group]))

(defn create [opts]
  (doto (ui/stack)
    (widget-group/set-opts! opts)))

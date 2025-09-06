(ns cdq.ui.stack
  (:require [clojure.gdx.scenes.scene2d.ui.stack :as stack]
            [cdq.ui.widget-group :as widget-group]))

(defn create [opts]
  (doto (stack/create)
    (widget-group/set-opts! opts)))

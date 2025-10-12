(ns cdq.ui.build.stack
  (:require [cdq.ui.build.widget-group :as widget-group]
            [clojure.gdx.scene2d.ui.stack :as stack]))

(defn create [opts]
  (doto (stack/create)
    (widget-group/set-opts! opts)))

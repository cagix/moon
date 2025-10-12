(ns cdq.ui.build.stack
  (:require [clojure.gdx.scene2d.ui.stack :as stack]
            [clojure.scene2d.widget-group :as widget-group]))

(defn create [opts]
  (doto (stack/create)
    (widget-group/set-opts! opts)))

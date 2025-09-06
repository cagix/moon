(ns cdq.ui.stack
  (:require [cdq.ui :as ui]
            [clojure.gdx.scenes.scene2d.ui.stack :as stack]))

(defn create [opts]
  (doto (stack/create)
    (ui/set-opts! opts)))
